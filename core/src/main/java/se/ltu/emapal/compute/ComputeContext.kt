package se.ltu.emapal.compute

import rx.Observable
import se.ltu.emapal.compute.util.Result
import java.io.Closeable

/**
 * Some context useful for performing computation.
 */
interface ComputeContext : Closeable {
    /** Uses context to process provided batch, returning output batch if successful. */
    fun process(batch: ComputeBatch): Result<ComputeBatch, ComputeError>

    /** Registers given lambda in compute context, allowing it to later be used to process batches. */
    fun register(lambda: ComputeLambda): Result<Void?, ComputeError>

    /** Observable pushing new log entries whenever executed lambda calls the log function. */
    fun whenLogEntry(): Observable<ComputeLogEntry>
}