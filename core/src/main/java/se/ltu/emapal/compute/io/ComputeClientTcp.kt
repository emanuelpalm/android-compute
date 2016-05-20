package se.ltu.emapal.compute.io

import rx.Observable
import rx.lang.kotlin.PublishSubject
import rx.subjects.ReplaySubject
import se.ltu.emapal.compute.ComputeBatch
import se.ltu.emapal.compute.ComputeError
import se.ltu.emapal.compute.ComputeLambda
import se.ltu.emapal.compute.ComputeLogEntry
import se.ltu.emapal.compute.util.time.Duration
import se.ltu.emapal.compute.util.time.UnixTime
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Manages service communication via TCP on behalf of some compute client.
 *
 * Upon creation, immediately attempts to connect to some identified service, publishing any
 * progress via its observables.
 *
 * All methods ought to be thread safe.
 */
class ComputeClientTcp : ComputeClient {
    private val sendCount = AtomicInteger(0)
    private val sendQueue = ConcurrentLinkedQueue<ComputeMessage>()

    private val executor: ScheduledExecutorService
    private val isOwningExecutor: Boolean
    private val isClosed = AtomicBoolean(false)

    private val selector: Selector
    private val socket: SocketChannel
    private val channel: ComputeChannel

    private val deadline: AtomicReference<UnixTime>
    private val timeout: Duration

    private val whenBatchSubject = PublishSubject<ComputeBatch>()
    private val whenExceptionSubject = PublishSubject<Throwable>()
    private val whenLambdaSubject = PublishSubject<ComputeLambda>()
    private val whenStatusSubject = ReplaySubject.createWithSize<ComputeClientStatus>(1)

    /**
     * Creates new TCP [ComputeClient], connecting to host at [address], scheduling its work using
     * [executor].
     *
     * @param address Address of compute service.
     * @param timeout Duration after which a stale connection times out.
     * @param executor Executor service to use for sending and receiving messages.
     * @param executorDelay Delay after which socket polling is started.
     * @param executorInterval Interval at which socket polling is scheduled.
     */
    constructor(
            address: InetSocketAddress,
            timeout: Duration = Duration.ofSeconds(30),
            executor: ScheduledExecutorService? = null,
            executorDelay: Duration = Duration.ofMilliseconds(10),
            executorInterval: Duration = Duration.ofMilliseconds(250)
    ) {
        if (executor != null) {
            this.executor = executor
            isOwningExecutor = false
        } else {
            this.executor = Executors.newSingleThreadScheduledExecutor()
            isOwningExecutor = true
        }
        this.timeout = timeout

        selector = Selector.open()

        socket = SocketChannel.open()
        socket.configureBlocking(false)
        socket.register(selector, SelectionKey.OP_CONNECT)
        socket.connect(address)
        whenStatusSubject.onNext(ComputeClientStatus.CONNECTING)

        channel = ComputeChannel(socket)

        deadline = AtomicReference(UnixTime.now() + timeout)

        // Schedule polling.
        this.executor.scheduleAtFixedRate(
                {
                    if (isClosed.get()) {
                        return@scheduleAtFixedRate
                    }
                    try {
                        poll()

                    } catch (e: Throwable) {
                        if (e !is InterruptedException) {
                            whenExceptionSubject.onNext(e)
                            whenStatusSubject.onNext(ComputeClientStatus.DISRUPTED)
                        }
                        close()
                    }
                },
                executorDelay.toMilliseconds(),
                executorInterval.toMilliseconds(),
                TimeUnit.MILLISECONDS)

        // Schedule refreshing.
        this.executor.scheduleAtFixedRate(
                {
                    if (isClosed.get()) {
                        return@scheduleAtFixedRate
                    }
                    try {
                        refresh()

                    } catch (e: Throwable) {
                        if (e !is InterruptedException) {
                            whenExceptionSubject.onNext(e)
                            whenStatusSubject.onNext(ComputeClientStatus.DISRUPTED)
                        }
                        close()
                    }
                },
                0,
                (timeout.toMilliseconds() * 0.9).toLong(),
                TimeUnit.MILLISECONDS)
    }

    /** Polls socket, looking for opportunity to finish connecting, reading and writing. */
    private fun poll() {
        if ((deadline.get() - UnixTime.now()) < Duration.ZERO) {
            throw SocketTimeoutException()
        }
        synchronized(socket) {
            if (selector.isOpen) {
                selector.selectNow()
                selector.selectedKeys().removeAll { key ->
                    if (key.isValid) {
                        if (key.isConnectable) {
                            connect(key)

                        } else if (key.isReadable) {
                            receive(key)
                        }
                    }
                    true
                }
                if (socket.isConnected) {
                    send();
                }
            }
        }
    }

    /** Finishes on-going socket connection. */
    private fun connect(key: SelectionKey) {
        if (key.channel() !== socket) {
            throw IllegalStateException("key.channel() !== socket")
        }
        if (socket.finishConnect()) {
            socket.register(selector, SelectionKey.OP_READ)
            whenStatusSubject.onNext(ComputeClientStatus.CONNECTED)
        }
    }

    /** Reads any pending incoming socket messages. */
    private fun receive(key: SelectionKey) {
        if (key.channel() !== socket) {
            throw IllegalStateException("key.channel() !== socket")
        }
        channel.read().let {
            it.ifValue {
                when (it) {
                    is ComputeMessage.ServiceBatch -> whenBatchSubject.onNext(it.batch)
                    is ComputeMessage.ServiceExit -> {
                        whenStatusSubject.onNext(ComputeClientStatus.TERMINATED)
                        close()
                    }
                    is ComputeMessage.ServiceLambda -> whenLambdaSubject.onNext(it.lambda)
                    is ComputeMessage.ServiceImAlive -> Unit
                    else -> whenExceptionSubject.onNext(IllegalStateException("Unhandled message: $it"))
                }
                deadline.set(UnixTime.now() + timeout)
            }
            it.ifError { whenExceptionSubject.onNext(it) }
        }
    }

    /** Sends any pending outgoing messages. */
    private fun send() {
        sendQueue.removeAll {
            channel.write(it)
                    .ifError { whenExceptionSubject.onNext(it) }
            true
        }
    }

    /** Refreshes connection by sending "I'm alive!" message. */
    private fun refresh() {
        synchronized(socket) {
            if (socket.isConnected) {
                channel.write(ComputeMessage.ClientImAlive(sendCount.incrementAndGet()))
            }
        }
    }

    /** Waits synchronously for compute client to become connected for given duration. */
    fun awaitConnectionFor(duration: Duration) {
        when (whenStatusSubject.value) {
            ComputeClientStatus.CONNECTED -> return
            ComputeClientStatus.CONNECTING -> Unit
            else -> throw IllegalStateException("Cannot await connection while in state ${whenStatusSubject.value}.")
        }

        val semaphore = Semaphore(0)
        val atomicException = AtomicReference<Throwable>(null)
        whenStatusSubject.subscribe {
            if (it == ComputeClientStatus.CONNECTED) {
                semaphore.release()
            }
        }
        whenExceptionSubject.subscribe {
            atomicException.set(it)
            semaphore.release()
        }
        if (!semaphore.tryAcquire(duration.toMilliseconds(), TimeUnit.MILLISECONDS)) {
            throw SocketTimeoutException()
        }
        val exception = atomicException.get()
        if (exception != null) {
            throw exception
        }
    }

    override fun submit(batch: ComputeBatch) {
        sendQueue.add(ComputeMessage.ClientBatch(sendCount.incrementAndGet(), batch))
    }

    override fun submit(error: ComputeError) {
        sendQueue.add(ComputeMessage.ClientError(sendCount.incrementAndGet(), error))
    }

    override fun submit(logEntry: ComputeLogEntry) {
        sendQueue.add(ComputeMessage.ClientLogEntry(sendCount.incrementAndGet(), logEntry))
    }

    override val whenBatch: Observable<ComputeBatch>
            get() = whenBatchSubject
    override val whenException: Observable<Throwable>
            get() = whenExceptionSubject
    override val whenLambda: Observable<ComputeLambda>
            get() = whenLambdaSubject
    override val whenStatus: Observable<ComputeClientStatus>
            get() = whenStatusSubject

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                if (socket.isConnected) {
                    channel.write(ComputeMessage.ClientExit(sendCount.incrementAndGet()))
                }
                whenBatchSubject.onCompleted()
                whenExceptionSubject.onCompleted()
                whenLambdaSubject.onCompleted()
                whenStatusSubject.onCompleted()

            } finally {
                if (isOwningExecutor) {
                    executor.awaitTermination(1, TimeUnit.SECONDS)
                }
                selector.close()
                socket.close()
            }
        }
    }
}