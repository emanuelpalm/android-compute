package se.ltu.emapal.compute

import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncoder
import se.ltu.emapal.compute.util.media.schema.MediaSchema
import se.ltu.emapal.compute.util.media.schema.MediaSchemaException

/**
 * Signifies a failure to perform some compute action.
 */
class ComputeError(val code: Int, message: String) : RuntimeException(message), MediaEncodable {
    override val encodable: (MediaEncoder) -> Unit
        get() = {
            it.encodeMap {
                it
                        .add("cod", code)
                        .add("msg", message ?: "")
            }
        }

    companion object {
        private val decoderSchema = MediaSchema.typeMap()
                .schemaEntry("cod", MediaSchema.typeNumber())
                .schemaEntry("msg", MediaSchema.typeText())

        /** Attempts to produce [ComputeError] using provided decoder. */
        fun decode(decoder: MediaDecoder): Result<ComputeError, MediaSchemaException> {
            return decoderSchema.verify(decoder)
                    .map {
                        val decoderMap = it.toMap()
                        ComputeError(
                                decoderMap["cod"]!!.toInt(),
                                decoderMap["msg"]!!.toText()
                        )
                    }
        }
    }
}