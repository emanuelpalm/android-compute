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
            { encodable, output -> JacksonMediaConverter.SMILE.encode(encodable, output) },
            { JacksonMediaConverter.SMILE.decode(it) },
            byteChannel
    )

    init {
        buffer.order(ByteOrder.BIG_ENDIAN)
    }

    /** Sends [message] over channel and awaits response. */
    fun write(message: ComputeMessage): Result<Void?, IOException> {
        try {
            synchronized(buffer) {
                buffer.rewind()
                buffer.limit(buffer.capacity())
                ByteBufferOutputStream(buffer).use {
                    it.buffer.position(4)
                    encoder(message, it)

                    val size = it.buffer.position()
                    val messageSize = size - 4
                    it.buffer.position(0)
                    it.buffer.limit(4)
                    it.buffer.putInt(messageSize)

                    it.buffer.rewind()
                    it.buffer.limit(size)
                    byteChannel.write(it.buffer)
                }
            }
            return Result.Success(null)

        } catch (e: IOException) {
            return Result.Failure(e)
        }
    }

    /** Waits for [ComputeMessage] to be received over channel. */
    fun read(): Result<ComputeMessage, MediaEncodableException> {
        try {
            synchronized(buffer) {
                buffer.rewind()
                val size = buffer.limit(4).let {
                    byteChannel.read(it as ByteBuffer)
                    it.rewind()
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
                                        .mapError {
                                            @Suppress("USELESS_CAST")
                                            (it as MediaEncodableException)
                                        }
                            }
                }
            }
        } catch (e: IOException) {
            return Result.Failure(MediaEncodableException.wrap(e))
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