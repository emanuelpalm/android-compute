package se.ltu.emapal.compute

import se.ltu.emapal.compute.util.UnixTime

/**
 * A compute log entry.
 *
 * @param timestamp Date and time at which the log entry was generated.
 * @param lambdaId Identifies lambda executed when entry was logged.
 * @param batchId Identifies batch processed when entry was logged.
 * @param message Logged message.
 */
data class ComputeLogEntry(
        val timestamp: UnixTime,
        val lambdaId: Int,
        val batchId: Int,
        val message: String
) {
    /** Creates new log entry with the current time as timestamp. */
    constructor(
            lambdaId: Int,
            batchId: Int,
            message: String
    ) : this(UnixTime.now(), lambdaId, batchId, message)
}
