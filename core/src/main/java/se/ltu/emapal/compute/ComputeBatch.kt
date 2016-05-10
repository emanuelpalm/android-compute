package se.ltu.emapal.compute

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
) {
    override fun equals(other: Any?): Boolean{
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

    override fun hashCode(): Int{
        var result = lambdaId
        result += 31 * result + batchId
        result += 31 * result + Arrays.hashCode(data)
        return result
    }

    override fun toString(): String{
        return "ComputeBatch(lambdaId=$lambdaId, batchId=$batchId, data=${Arrays.toString(data)})"
    }
}