package se.ltu.emapal.compute

import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncoder
import se.ltu.emapal.compute.util.media.schema.MediaSchema
import se.ltu.emapal.compute.util.media.schema.MediaSchemaException

/**
 * A lambda, containing some Lua program able to process arbitrary byte array batches.
 *
 * @param lambdaId Lambda identifier unique within the application.
 * @param program Lua program.
 */
data class ComputeLambda(
        val lambdaId: Int,
        val program: String
) : MediaEncodable {
    override val encodable: (MediaEncoder) -> Unit
        get() = {
            it.encodeMap {
                it
                        .add("lid", lambdaId)
                        .add("lua", program)
            }
        }

    companion object {
        private val decoderSchema = MediaSchema.typeMap()
                .schemaEntry("lid", MediaSchema.typeNumber())
                .schemaEntry("lua", MediaSchema.typeText())

        /** Attempts to produce [ComputeLambda] using provided decoder. */
        fun decode(decoder: MediaDecoder): Result<ComputeLambda, MediaSchemaException> {
            return decoderSchema.verify(decoder)
                    .map {
                        val decoderMap = it.toMap()
                        ComputeLambda(
                                decoderMap["lid"]!!.toInt(),
                                decoderMap["lua"]!!.toText()
                        )
                    }
        }
    }
}
