package se.ltu.emapal.compute.io

import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncodableException
import se.ltu.emapal.compute.util.media.jackson.JacksonMediaConverter
import se.ltu.emapal.compute.util.nio.ByteBufferInputStream
import se.ltu.emapal.compute.util.nio.ByteBufferOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.ByteChannel
import java.nio.channels.Channel

/**
 * Manages communication between one compute client and one compute service.
 *
 * All messages are copied on and off a single 32 Mb buffer. No message may exceed that size, or an
 * exception is thrown.
 *
 * @param encoder Function used to encode messages.
 * @param decoder Function used to decode messages.
 * @param byteChannel Channel used for communication.
 */
class ComputeChannel(
        private val encoder: (MediaEncodable, OutputStream) -> Unit,
        private val decoder: (InputStream) -> Result<MediaDecoder, IOException>,
        private val byteChannel: ByteChannel
) : Channel {
    private val buffer = ByteBuffer.allocate(BUFFER_SIZE)

    /** Creates [ComputeChannel] using JSON as message encoding. */
    constructor(byteChannel: ByteChannel) : this(
            { encodable, output -> JacksonMediaConverter.encode(encodable, output) },
            { JacksonMediaConverter.decode(it) },
            byteChannel
    )

    init {
        buffer.order(ByteOrder.BIG_ENDIAN)
    }

    /** Sends [message] over channel and awaits response. */
    fun write(message: ComputeMessage): Result<ComputeMessage, MediaEncodableException> {
        synchronized(buffer) {
            writeUnsafe(message)
            return readUnsafe()
        }
    }

    private fun writeUnsafe(message: ComputeMessage) {
        ByteBufferOutputStream(buffer).use {
            it.buffer.position(4)
            encoder(message, it)

            val size = it.buffer.position() - 4
            it.buffer.position(0)
            it.buffer.putInt(size)

            it.buffer.rewind()
            byteChannel.write(it.buffer)
        }
    }

    /** Waits for [ComputeMessage] to be received over channel. */
    fun read(): Result<ComputeMessage, MediaEncodableException> {
        synchronized(buffer) {
            return readUnsafe()
        }
    }

    private fun readUnsafe(): Result<ComputeMessage, MediaEncodableException> {
        val size = buffer.limit(4).let {
            byteChannel.read(it as ByteBuffer)
            it.int
        }
        buffer.rewind()
        buffer.limit(size).let {
            byteChannel.read(it as ByteBuffer)
        }
        buffer.rewind()
        ByteBufferInputStream(buffer).use {
            return decoder(it)
                    .mapError { MediaEncodableException.wrap(it) }
                    .apply {
                        ComputeMessage.decode(it)
                                .mapError { it as MediaEncodableException }
                    }
        }
    }

    override fun isOpen(): Boolean {
        synchronized(buffer) {
            return byteChannel.isOpen
        }
    }

    override fun close() {
        synchronized(buffer) {
            byteChannel.close()
        }
    }

    companion object {
        /** The size of the internal [ByteBuffer] used when reading and writing messages. */
        val BUFFER_SIZE = 32 * 1024 * 1024
    }
}