package se.ltu.emapal.compute.io

import rx.Observable
import se.ltu.emapal.compute.ComputeBatch
import se.ltu.emapal.compute.ComputeError
import se.ltu.emapal.compute.ComputeLambda
import se.ltu.emapal.compute.ComputeLogEntry
import java.io.Closeable

/**
 * Manages communication with a single client on behalf of some compute service.
 */
interface ComputeService : Closeable {
    /** Submits batch for eventual transmission to connected compute client. */
    fun submit(batch: ComputeBatch)

    /** Submits compute lambda for eventual transmission to connected compute service. */
    fun submit(lambda: ComputeLambda)

    /** Publishes received processed batches. */
    fun whenBatch(): Observable<ComputeBatch>

    /** Publishes errors reported by client. */
    fun whenError(): Observable<ComputeError>

    /** Publishes generated errors. */
    fun whenException(): Observable<Throwable>

    /** Publishes received client log entries. */
    fun whenLogEntry(): Observable<ComputeLogEntry>

    /** Publishes service status changes. */
    fun whenStatus(): Observable<ComputeServiceStatus>

}
