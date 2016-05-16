package se.ltu.emapal.compute.util.nio

import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * An [OutputStream] backed by a [ByteBuffer].
 */
class ByteBufferOutputStream(val buffer: ByteBuffer) : OutputStream() {

    override fun write(byte: Int) {
        buffer.put(byte.toByte())
    }

    override fun write(bytes: ByteArray) = write(bytes, 0, bytes.size)

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        buffer.put(bytes, offset, length)
    }
}