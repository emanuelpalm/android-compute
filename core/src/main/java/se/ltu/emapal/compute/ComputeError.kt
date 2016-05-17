package se.ltu.emapal.compute

import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncodableException
import se.ltu.emapal.compute.util.media.MediaEncoder
import se.ltu.emapal.compute.util.media.schema.MediaSchema
import se.ltu.emapal.compute.util.media.schema.MediaSchemaException

/**
 * Signifies a failure to perform some compute action.
 */
class ComputeError(val code: Int, message: String) : MediaEncodableException(message) {
    override val encodable: (MediaEncoder) -> Unit
        get() = {
            it.encodeMap {
                it
                        .add("cod", code)
                        .add("msg", message ?: "")
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ComputeError

        if (code != other.code) return false

        return true
    }

    override fun hashCode(): Int {
        return code
    }

    override fun toString(): String {
        return "ComputeError(code=$code)"
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