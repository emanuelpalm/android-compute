package se.ltu.emapal.compute.io

import rx.Observable
import rx.lang.kotlin.PublishSubject
import rx.subjects.ReplaySubject
import se.ltu.emapal.compute.ComputeBatch
import se.ltu.emapal.compute.ComputeError
import se.ltu.emapal.compute.ComputeLambda
import se.ltu.emapal.compute.ComputeLogEntry
import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.time.Duration
import se.ltu.emapal.compute.util.time.UnixTime
import java.net.InetSocketAddress
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
    private val isClosed = AtomicBoolean(false)

    private val selector: Selector
    private val socket: SocketChannel
    private val channel: ComputeChannel
    private val atomicTimeout: AtomicReference<UnixTime>

    private val whenBatchSubject = PublishSubject<ComputeBatch>()
    private val whenErrorSubject = PublishSubject<Throwable>()
    private val whenLambdaSubject = PublishSubject<ComputeLambda>()
    private val whenStatusSubject = ReplaySubject.createWithSize<ComputeClient.Status>(1)

    /**
     * Creates new TCP [ComputeClient], connecting to host at [address], scheduling its work using
     * [executor].
     *
     * @param address Address of compute service.
     * @param socketTimeout Duration, in [timeUnit]s, after which a stale connection times out.
     * @param executor Executor service to use for sending and receiving messages.
     * @param executorDelay Delay, in [timeUnit]s, after which socket polling is started.
     * @param executorInterval Interval, in [timeUnit]s, at which socket polling is scheduled.
     * @param timeUnit Time unit used for [executorDelay] and [executorInterval].
     */
    constructor(
            address: InetSocketAddress,
            socketTimeout: Long = 30000,
            executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
            executorDelay: Long = 10,
            executorInterval: Long = 250,
            timeUnit: TimeUnit = TimeUnit.MILLISECONDS
    ) {
        this.executor = executor

        selector = Selector.open()

        socket = SocketChannel.open()
        socket.configureBlocking(false)
        socket.register(selector, SelectionKey.OP_CONNECT)
        socket.connect(address)

        channel = ComputeChannel(socket)

        val timeoutDuration = Duration.of(socketTimeout, timeUnit)
        atomicTimeout = AtomicReference(UnixTime.now() + timeoutDuration)

        executor.scheduleAtFixedRate({
            try {
                if (selector.isOpen) {
                    if ((UnixTime.now() - atomicTimeout.get()).toSeconds() > socketTimeout) {
                        throw SocketTimeoutException()
                    }

                    selector.select()
                    selector.selectedKeys().removeAll { key ->
                        if (key.isValid) {
                            if (key.isConnectable) {
                                if (socket.finishConnect()) {
                                    socket.register(selector, SelectionKey.OP_READ)
                                    whenStatusSubject.onNext(ComputeClient.Status.CONNECTED)
                                }
                            } else {
                                if (key.isReadable) {
                                    channel.read().let {
                                        it.ifValue {
                                            when (it) {
                                                is ComputeMessage.ServiceBatch -> whenBatchSubject.onNext(it.batch)
                                                is ComputeMessage.ServiceExit -> whenStatusSubject.onNext(ComputeClient.Status.TERMINATED)
                                                is ComputeMessage.ServiceLambda -> whenLambdaSubject.onNext(it.lambda)
                                                is ComputeMessage.ServiceImAlive -> Unit
                                                else -> whenErrorSubject.onNext(IllegalStateException("Unhandled message: $it"))
                                            }
                                        }
                                        it.ifError { whenErrorSubject.onNext(it) }
                                        atomicTimeout.set(UnixTime.now() + timeoutDuration)
                                    }
                                }
                                if (sendQueue.isNotEmpty()) {
                                    sendQueue.removeAll {
                                        val result = channel.write(it)
                                        if (result is Result.Failure) {
                                            whenErrorSubject.onNext(result.error)
                                            false
                                        } else {
                                            true
                                        }
                                    }
                                }
                            }
                        }
                        true
                    }
                }
            } catch (e: Throwable) {
                whenErrorSubject.onNext(e)
                close()
            }
        }, executorDelay, executorInterval, timeUnit)

        whenStatusSubject.onNext(ComputeClient.Status.CONNECTING)
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

    override fun whenBatch(): Observable<ComputeBatch> = whenBatchSubject
    override fun whenError(): Observable<Throwable> = whenErrorSubject
    override fun whenLambda(): Observable<ComputeLambda> = whenLambdaSubject
    override fun whenStatus(): Observable<ComputeClient.Status> = whenStatusSubject

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            whenStatusSubject.onNext(ComputeClient.Status.DISCONNECTED)
            try {
                whenBatchSubject.onCompleted()
                whenErrorSubject.onCompleted()
                whenLambdaSubject.onCompleted()
                whenStatusSubject.onCompleted()

            } finally {
                executor.shutdown()
                selector.close()
                socket.close()
            }
        }
    }
}