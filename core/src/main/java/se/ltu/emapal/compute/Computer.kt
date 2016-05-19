package se.ltu.emapal.compute

import rx.Observable
import rx.lang.kotlin.PublishSubject
import se.ltu.emapal.compute.io.ComputeClient
import java.io.Closeable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * A client computer.
 *
 * Receives compute instructions via some [ComputeClient], executes them using internally managed
 * threads, and reports any results of relevance to its connected compute service.
 *
 * @param client Client used to communication with some compute service, providing tasks.
 * @param contextFactory Function used to instantiate new [ComputeContext]s.
 * @param contextThreads The amount of [ComputeContext]s to create, each given its own thread.
 */
class Computer(
        val client: ComputeClient,
        contextFactory: () -> ComputeContext,
        contextThreads: Int = 1
) : Closeable {
    private val threadPool = Executors.newFixedThreadPool(contextThreads)
    private val threadQueues = Array(contextThreads, { ConcurrentLinkedQueue<(ComputeContext) -> Unit>() })
    private val sharedQueue = ConcurrentLinkedQueue<(ComputeContext) -> Unit>()
    private val isClosed = AtomicBoolean(false)

    private val whenExceptionSubject = PublishSubject<Pair<Int, Throwable>>()
    private val whenLambdaCountSubject = PublishSubject<Int>()
    private val whenBatchPendingCountSubject = PublishSubject<Int>()
    private val whenBatchProcessedCountSubject = PublishSubject<Int>()

    private val lambdaCount = AtomicInteger(0)
    private val batchPendingCount = AtomicInteger(0)
    private val batchProcessedCount = AtomicInteger(0)

    init {
        // Creates one thread and work queue for each created context.
        (0..(contextThreads - 1)).forEach { index ->

            val context = contextFactory()
            val contextQueue = threadQueues[index]

            threadPool.execute({

                while (!isClosed.get()) {
                    try {
                        val contextTask = contextQueue.poll()
                        if (contextTask != null) {
                            contextTask.invoke(context)
                            continue
                        }
                        val stolenTask = sharedQueue.poll()
                        if (stolenTask != null) {
                            stolenTask.invoke(context)
                            continue
                        }
                        Thread.sleep(250)
                    } catch (e: Throwable) {
                        whenExceptionSubject.onNext(Pair(index, e))
                    }
                }
                context.close()

            })

        }
        // Provides received lambdas via personal queues to each context thread.
        client.whenLambda.subscribe { lambda ->
            threadQueues.forEach { queue ->
                queue.add { context ->
                    context.register(lambda).let {
                        it.ifValue { whenLambdaCountSubject.onNext(lambdaCount.incrementAndGet()) }
                        it.ifError { client.submit(it) }
                    }
                }
            }
        }
        // Provides received batches via shared queue to first available context thread.
        client.whenBatch.subscribe { batch ->
            whenBatchPendingCountSubject.onNext(batchPendingCount.incrementAndGet())
            sharedQueue.add { context ->
                context.process(batch).let {
                    it.ifValue {
                        client.submit(it)
                        whenBatchProcessedCountSubject.onNext(batchProcessedCount.incrementAndGet())
                    }
                    it.ifError { client.submit(it) }
                    whenBatchPendingCountSubject.onNext(batchPendingCount.decrementAndGet())
                }
            }
        }
    }

    /** Publishes context thread exceptions, with the first pair member being context ID. */
    val whenException: Observable<Pair<Int, Throwable>>
        get() = whenExceptionSubject

    /** Publishes the amount of registered lambda functions. */
    val whenLambdaCount: Observable<Int>
        get() = whenLambdaCountSubject

    /** Publishes the amount of received batches pending for processing. */
    val whenBatchPendingCount: Observable<Int>
        get() = whenBatchPendingCountSubject

    /** Publishes the amount of received batches that have been processed and returned. */
    val whenBatchProcessedCount: Observable<Int>
        get() = whenBatchProcessedCountSubject

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            threadPool.shutdown()

            whenExceptionSubject.onCompleted()
            whenLambdaCountSubject.onCompleted()
            whenBatchPendingCountSubject.onCompleted()
            whenBatchProcessedCountSubject.onCompleted()
        }
    }
}