package se.ltu.emapal.compute

import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.UnixTime
import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncoder
import se.ltu.emapal.compute.util.media.schema.MediaSchema
import se.ltu.emapal.compute.util.media.schema.MediaSchemaException

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
) : MediaEncodable {
    /** Creates new log entry with the current time as timestamp. */
    constructor(
            lambdaId: Int,
            batchId: Int,
            message: String
    ) : this(UnixTime.now(), lambdaId, batchId, message)

    override val encodable: (MediaEncoder) -> Unit
        get() = {
            it.encodeMap {
                it
                        .add("tim", timestamp.toMilliseconds())
                        .add("lid", lambdaId)
                        .add("bid", batchId)
                        .add("msg", message)
            }
        }

    companion object {
        private val decoderSchema = MediaSchema.typeMap()
                .schemaEntry("tim", MediaSchema.typeNumber())
                .schemaEntry("lid", MediaSchema.typeNumber())
                .schemaEntry("bid", MediaSchema.typeNumber())
                .schemaEntry("msg", MediaSchema.typeText())

        /** Attempts to produce [ComputeLambda] using provided decoder. */
        fun decode(decoder: MediaDecoder): Result<ComputeLogEntry, MediaSchemaException> {
            return decoderSchema.verify(decoder)
                    .map {
                        val decoderMap = it.toMap()
                        ComputeLogEntry(
                                UnixTime.ofMilliseconds(decoderMap["tim"]!!.toLong()),
                                decoderMap["lid"]!!.toInt(),
                                decoderMap["lid"]!!.toInt(),
                                decoderMap["lua"]!!.toText()
                        )
                    }
        }
    }
}
