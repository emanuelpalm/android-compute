package se.ltu.emapal.compute.util.nio

import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.channels.ClosedChannelException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [ByteChannel] backed by a [ByteBuffer].
 */
class ByteBufferChannel(val buffer: ByteBuffer) : ByteChannel {
    val atomicIsOpen = AtomicBoolean(true)

    override fun write(source: ByteBuffer): Int {
        if (!atomicIsOpen.get()) {
            throw ClosedChannelException()
        }
        buffer.limit(source.limit())
        buffer.position().let { startPosition ->
            return buffer.put(source).position() - startPosition
        }
    }

    override fun isOpen() = atomicIsOpen.get()

    override fun close() {
        atomicIsOpen.set(false)
    }

    override fun read(destination: ByteBuffer): Int {
        if (!atomicIsOpen.get()) {
            throw ClosedChannelException()
        }
        buffer.limit(buffer.position() + destination.limit())
        buffer.position().let { startPosition ->
            return destination.put(buffer).position() - startPosition
        }
    }
}