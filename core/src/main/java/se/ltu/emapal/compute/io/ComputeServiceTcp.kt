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
import java.net.SocketTimeoutException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Manages client communication via TCP on behalf of some compute service.
 *
 * Instances of this class are created by [ComputeServiceTcpListener].
 *
 * All methods ought to be thread safe.
 */
internal class ComputeServiceTcp : ComputeService {
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
    private val whenErrorSubject = PublishSubject<ComputeError>()
    private val whenExceptionSubject = PublishSubject<Throwable>()
    private val whenLogEntrySubject = PublishSubject<ComputeLogEntry>()
    private val whenStatusSubject = ReplaySubject.createWithSize<ComputeServiceStatus>(1)

    /**
     * Creates new TCP [ComputeService], scheduling its work using [executor].
     *
     * @param socket Socket through which some client is available. The socket must be connected.
     * @param timeout Duration after which a stale connection times out.
     * @param executor Executor service to use for sending and receiving messages.
     * @param executorDelay Delay after which socket polling is started.
     * @param executorInterval Interval at which socket polling is scheduled.
     */
    constructor(
            socket: SocketChannel,
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
        this.socket = socket
        this.timeout = timeout

        selector = Selector.open()

        socket.configureBlocking(false)
        socket.register(selector, SelectionKey.OP_READ)
        whenStatusSubject.onNext(ComputeServiceStatus.CONNECTED)

        channel = ComputeChannel(socket)

        deadline = AtomicReference(UnixTime.now() + timeout)

        // Schedule polling.
        this.executor.scheduleAtFixedRate(
                {
                    try {
                        poll()
                    } catch (e: Throwable) {
                        whenExceptionSubject.onNext(e)
                        whenStatusSubject.onNext(ComputeServiceStatus.DISRUPTED)
                        close()
                    }
                },
                executorDelay.toMilliseconds(),
                executorInterval.toMilliseconds(),
                TimeUnit.MILLISECONDS)

        // Schedule refreshing.
        this.executor.scheduleAtFixedRate(
                {
                    try {
                        refresh()
                    } catch (e: Throwable) {
                        whenExceptionSubject.onNext(e)
                        whenStatusSubject.onNext(ComputeServiceStatus.DISRUPTED)
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
                    if (key.isValid && key.isReadable) {
                        receive(key)
                    }
                    true
                }
                if (socket.isConnected) {
                    send();
                }
            }
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
                    is ComputeMessage.ClientBatch -> whenBatchSubject.onNext(it.batch)
                    is ComputeMessage.ClientError -> whenErrorSubject.onNext(it.error)
                    is ComputeMessage.ClientExit -> {
                        whenStatusSubject.onNext(ComputeServiceStatus.TERMINATED)
                        close()
                    }
                    is ComputeMessage.ClientImAlive -> Unit
                    is ComputeMessage.ClientLogEntry -> whenLogEntrySubject.onNext(it.logEntry)
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
                channel.write(ComputeMessage.ServiceImAlive(sendCount.incrementAndGet()))
            }
        }
    }

    override fun submit(batch: ComputeBatch) {
        sendQueue.add(ComputeMessage.ServiceBatch(sendCount.incrementAndGet(), batch))
    }

    override fun submit(lambda: ComputeLambda) {
        sendQueue.add(ComputeMessage.ServiceLambda(sendCount.incrementAndGet(), lambda))
    }

    override val whenBatch: Observable<ComputeBatch>
            get() = whenBatchSubject
    override val whenError: Observable<ComputeError>
            get() = whenErrorSubject
    override val whenException: Observable<Throwable>
            get() = whenExceptionSubject
    override val whenLogEntry: Observable<ComputeLogEntry>
            get() = whenLogEntrySubject
    override val whenStatus: Observable<ComputeServiceStatus>
            get() = whenStatusSubject

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                if (socket.isConnected) {
                    channel.write(ComputeMessage.ServiceExit(sendCount.incrementAndGet()))
                }
                whenBatchSubject.onCompleted()
                whenErrorSubject.onCompleted()
                whenExceptionSubject.onCompleted()
                whenLogEntrySubject.onCompleted()
                whenStatusSubject.onCompleted()

            } finally {
                if (isOwningExecutor) {
                    executor.shutdown()
                }
                selector.close()
                socket.close()
            }
        }
    }
}