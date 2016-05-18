package se.ltu.emapal.compute.io

import rx.lang.kotlin.PublishSubject
import se.ltu.emapal.compute.util.time.Duration
import java.io.Closeable
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Listens for incoming TCP connections and turns any received into [ComputeServiceTcp] objects.
 */
class ComputeServiceTcpListener : Closeable {
    private val executor: ScheduledExecutorService
    private val isClosed = AtomicBoolean(false)

    private val selector: Selector
    private val serverChannel: ServerSocketChannel

    private val whenConnectSubject = PublishSubject<ComputeService>()
    private val whenExceptionSubject = PublishSubject<Throwable>()

    /**
     * Creates new TCP [ComputeService], scheduling its work using [executor].
     *
     * @param hostAddress The local address at which incoming connections will be accepted.
     * @param executor Executor service to use for accepting incoming connections.
     * @param executorDelay Delay after which socket listening is started.
     * @param executorInterval Interval at which socket listening is scheduled.
     */
    constructor(
            hostAddress: InetSocketAddress,
            executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
            executorDelay: Duration = Duration.ofMilliseconds(10),
            executorInterval: Duration = Duration.ofMilliseconds(250)
    ) {
        this.executor = executor

        selector = Selector.open()

        serverChannel = ServerSocketChannel.open()
        serverChannel.configureBlocking(false)
        serverChannel.socket().bind(hostAddress)
        serverChannel.register(selector, SelectionKey.OP_ACCEPT)

        // Schedule accepting.
        executor.scheduleAtFixedRate(
                {
                    try {
                        poll()
                    } catch (e: Throwable) {
                        whenExceptionSubject.onNext(e)
                        close()
                    }
                },
                executorDelay.toMilliseconds(),
                executorInterval.toMilliseconds(),
                TimeUnit.MILLISECONDS)
    }

    private fun poll() {
        if (selector.isOpen) {
            selector.select()
            selector.selectedKeys().removeAll { key ->
                if (key.isValid && key.isAcceptable) {
                    accept(key)
                }
                true
            }
        }
    }

    private fun accept(key: SelectionKey) {
        if (key.channel() !== serverChannel) {
            throw IllegalStateException("key.channel() !== serverChannel")
        }
        val socket = serverChannel.accept()
        val service = ComputeServiceTcp(
                socket = socket,
                executor = executor
        )
        whenConnectSubject.onNext(service)
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            try {
                whenConnectSubject.onCompleted()
                whenExceptionSubject.onCompleted()

            } finally {
                executor.shutdown()
                selector.close()
                serverChannel.close()
            }
        }
    }
}