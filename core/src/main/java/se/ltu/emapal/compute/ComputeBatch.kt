package se.ltu.emapal.compute

import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncoder
import se.ltu.emapal.compute.util.media.schema.MediaSchema
import se.ltu.emapal.compute.util.media.schema.MediaSchemaException
import java.util.*

/**
 * Some arbitrary byte array to be processed by some identified lambda function.
 *
 * @param lambdaId Identifies lambda responsible for processing batch.
 * @param batchId Batch identifier unique within the application.
 * @param data Arbitrary byte array to process.
 */
class ComputeBatch(
        val lambdaId: Int,
        val batchId: Int,
        val data: ByteArray
) : MediaEncodable {
    override val encodable: (MediaEncoder) -> Unit
        get() = {
            it.encodeMap {
                it
                        .add("lid", lambdaId)
                        .add("bid", batchId)
                        .add("dat", data)
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other?.javaClass != javaClass) {
            return false
        }
        other as ComputeBatch
        return lambdaId == other.lambdaId
                && batchId == other.batchId
                && Arrays.equals(data, other.data);
    }

    override fun hashCode(): Int {
        var result = lambdaId
        result += 31 * result + batchId
        result += 31 * result + Arrays.hashCode(data)
        return result
    }

    override fun toString(): String {
        return "ComputeBatch(lambdaId=$lambdaId, batchId=$batchId, data=${Arrays.toString(data)})"
    }

    companion object {
        private val decoderSchema = MediaSchema.typeMap()
                .schemaEntry("lid", MediaSchema.typeNumber())
                .schemaEntry("bid", MediaSchema.typeNumber())
                .schemaEntry("dat", MediaSchema.typeBlob())

        /** Attempts to produce [ComputeBatch] using provided decoder. */
        fun decode(decoder: MediaDecoder): Result<ComputeBatch, MediaSchemaException> {
            return decoderSchema.verify(decoder)
                    .map {
                        val decoderMap = decoder.toMap()
                        ComputeBatch(
                                decoderMap["lid"]!!.toInt(),
                                decoderMap["bid"]!!.toInt(),
                                decoderMap["dat"]!!.toBlob()
                        )
                    }
        }
    }
}