package se.ltu.emapal.compute.io

import se.ltu.emapal.compute.ComputeBatch
import se.ltu.emapal.compute.ComputeError
import se.ltu.emapal.compute.ComputeLambda
import se.ltu.emapal.compute.ComputeLogEntry
import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.schema.MediaRequirement
import se.ltu.emapal.compute.util.media.schema.MediaSchema
import se.ltu.emapal.compute.util.media.schema.MediaSchemaException
import se.ltu.emapal.compute.util.media.schema.MediaViolation

/**
 * Represents a message that may be sent or received using a [ComputeChannel].
 */
abstract class ComputeMessage {
    /** From client: Processed batch. */
    class ClientBatch(override val id: Int, val batch: ComputeBatch) : ComputeMessage()

    /** From client: Error message. */
    class ClientError(override val id: Int, val error: ComputeError) : ComputeMessage()

    /** From client: Exit message. */
    class ClientExit(override val id: Int) : ComputeMessage()

    /** From client: Log entry. */
    class ClientLogEntry(override val id: Int, val logEntry: ComputeLogEntry) : ComputeMessage()

    /** From client: Request confirmation. */
    class ClientOK(override val id: Int) : ComputeMessage()

    /** From service: Batch to process. */
    class ServiceBatch(override val id: Int, val batch: ComputeBatch) : ComputeMessage()

    /** From service: Exit message. */
    class ServiceExit(override val id: Int) : ComputeMessage()

    /** From service: Lambda to be registered by receiving client. */
    class ServiceLambda(override val id: Int, val lambda: ComputeLambda) : ComputeMessage()

    /** From service: Request confirmation. */
    class ServiceOK(override val id: Int) : ComputeMessage()

    companion object {
        private val decoderSchema = MediaSchema.typeMap()
                .schemaEntry("mid", MediaSchema.typeNumber())
                .schemaEntry("typ", MediaSchema.typeNumber()
                        .setMinimum(1)
                        .setMaximum(9))
                .schemaEntry("bdy", MediaSchema.typeAny())

        /** Attempts to produce [ComputeMessage] using provided decoder. */
        fun decode(decoder: MediaDecoder): Result<ComputeMessage, MediaSchemaException> {
            return decoderSchema.verify(decoder)
                    .apply {
                        val decoderMap = it.toMap()
                        val id = decoderMap["mid"]!!.toInt()
                        val type = decoderMap["typ"]!!.toInt()
                        val body = decoderMap["body"]!!

                        when (type) {
                            1 -> ComputeBatch.decode(body).map { ClientBatch(id, it) }
                            2 -> ComputeError.decode(body).map { ClientError(id, it) }
                            3 -> Result.Success(ClientExit(id))
                            4 -> ComputeLogEntry.decode(body).map { ClientLogEntry(id, it) }
                            5 -> Result.Success(ClientOK(id))

                            6 -> ComputeBatch.decode(body).map { ServiceBatch(id, it) }
                            7 -> Result.Success(ServiceExit(id))
                            8 -> ComputeLambda.decode(body).map { ServiceLambda(id, it) }
                            9 -> Result.Success(ServiceOK(id))

                            else -> Result.Failure(MediaSchemaException(
                                    MediaViolation("body", MediaRequirement("type_in_range", "1", "9"))
                            ))
                        }
                    }
        }
    }

    /** Message ID. Used when matching requests to responses. */
    abstract val id: Int
}