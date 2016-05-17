package se.ltu.emapal.compute.io

import se.ltu.emapal.compute.ComputeBatch
import se.ltu.emapal.compute.ComputeError
import se.ltu.emapal.compute.ComputeLambda
import se.ltu.emapal.compute.ComputeLogEntry
import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncoder
import se.ltu.emapal.compute.util.media.schema.MediaRequirement
import se.ltu.emapal.compute.util.media.schema.MediaSchema
import se.ltu.emapal.compute.util.media.schema.MediaSchemaException
import se.ltu.emapal.compute.util.media.schema.MediaViolation

/**
 * Represents a message that may be sent or received using a [ComputeChannel].
 */
abstract class ComputeMessage : MediaEncodable {
    /** From client: Processed batch. */
    class ClientBatch(override val id: Int, val batch: ComputeBatch) : ComputeMessage() {
        override val type = TYPE_CLIENT_BATCH

        override val body: MediaEncodable?
            get() = batch
    }

    /** From client: Error message. */
    class ClientError(override val id: Int, val error: ComputeError) : ComputeMessage() {
        override val type = TYPE_CLIENT_ERROR

        override val body: MediaEncodable?
            get() = error
    }

    /** From client: Exit message. */
    class ClientExit(override val id: Int) : ComputeMessage() {
        override val type = TYPE_CLIENT_EXIT

        override val body: MediaEncodable?
            get() = null
    }

    /** From client: Log entry. */
    class ClientLogEntry(override val id: Int, val logEntry: ComputeLogEntry) : ComputeMessage() {
        override val type = TYPE_CLIENT_LOG_ENTRY

        override val body: MediaEncodable?
            get() = logEntry
    }

    /** From client: Request confirmation. */
    class ClientOK(override val id: Int) : ComputeMessage() {
        override val type = TYPE_CLIENT_OK

        override val body: MediaEncodable?
            get() = null
    }

    /** From service: Batch to process. */
    class ServiceBatch(override val id: Int, val batch: ComputeBatch) : ComputeMessage() {
        override val type = TYPE_SERVICE_BATCH

        override val body: MediaEncodable?
            get() = batch
    }

    /** From service: Exit message. */
    class ServiceExit(override val id: Int) : ComputeMessage() {
        override val type = TYPE_SERVICE_EXIT

        override val body: MediaEncodable?
            get() = null
    }

    /** From service: Lambda to be registered by receiving client. */
    class ServiceLambda(override val id: Int, val lambda: ComputeLambda) : ComputeMessage() {
        override val type = TYPE_SERVICE_LAMBDA

        override val body: MediaEncodable?
            get() = lambda
    }

    /** From service: Request confirmation. */
    class ServiceOK(override val id: Int) : ComputeMessage() {
        override val type = TYPE_SERVICE_OK

        override val body: MediaEncodable?
            get() = null
    }


    /** Message ID. Used when matching requests to responses. */
    abstract val id: Int

    /** Message type. */
    abstract val type: Int

    /** Message payload. */
    abstract val body: MediaEncodable?

    override val encodable: (MediaEncoder) -> Unit
        get() = {
            it.encodeMap {
                it
                        .add("mid", id)
                        .add("typ", type)
                        .add("bdy", body)
            }
        }


    override fun toString(): String {
        return "${javaClass.simpleName}(id=$id, type=$type, body=$body)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other?.javaClass != javaClass) {
            return false
        }
        other as ComputeMessage
        return id == other.id
                && type == other.type
                && body == other.body
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + type
        result = 31 * result + (body?.hashCode() ?: 0)
        return result
    }

    companion object {
        private val TYPE_CLIENT_BATCH = 1
        private val TYPE_CLIENT_ERROR = 2
        private val TYPE_CLIENT_EXIT = 3
        private val TYPE_CLIENT_LOG_ENTRY = 4
        private val TYPE_CLIENT_OK = 5
        private val TYPE_SERVICE_BATCH = 6
        private val TYPE_SERVICE_EXIT = 7
        private val TYPE_SERVICE_LAMBDA = 8
        private val TYPE_SERVICE_OK = 9

        private val decoderSchema = MediaSchema.typeMap()
                .schemaEntry("mid", MediaSchema.typeNumber())
                .schemaEntry("typ", MediaSchema.typeNumber()
                        .setMinimum(1)
                        .setMaximum(9))
                .schemaEntry("bdy", MediaSchema.typeAny())

        /** Attempts to produce [ComputeMessage] using provided decoder. */
        fun decode(decoder: MediaDecoder): Result<ComputeMessage, MediaSchemaException> {
            return decoderSchema.verify(decoder)
                    .apply<ComputeMessage> {
                        val decoderMap = it.toMap()
                        val id = decoderMap["mid"]!!.toInt()
                        val type = decoderMap["typ"]!!.toInt()
                        val body = decoderMap["body"]!!

                        when (type) {
                            TYPE_CLIENT_BATCH -> ComputeBatch.decode(body).map { ClientBatch(id, it) }
                            TYPE_CLIENT_ERROR -> ComputeError.decode(body).map { ClientError(id, it) }
                            TYPE_CLIENT_EXIT -> Result.Success(ClientExit(id))
                            TYPE_CLIENT_LOG_ENTRY -> ComputeLogEntry.decode(body).map { ClientLogEntry(id, it) }
                            TYPE_CLIENT_OK -> Result.Success(ClientOK(id))

                            TYPE_SERVICE_BATCH -> ComputeBatch.decode(body).map { ServiceBatch(id, it) }
                            TYPE_SERVICE_EXIT -> Result.Success(ServiceExit(id))
                            TYPE_SERVICE_LAMBDA -> ComputeLambda.decode(body).map { ServiceLambda(id, it) }
                            TYPE_SERVICE_OK -> Result.Success(ServiceOK(id))

                            else -> Result.Failure(MediaSchemaException(
                                    MediaViolation("body", MediaRequirement("type_is_valid"))
                            ))
                        }
                    }
        }
    }
}