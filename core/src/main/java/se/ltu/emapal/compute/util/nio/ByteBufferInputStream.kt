package se.ltu.emapal.compute.util.nio

import java.io.InputStream
import java.nio.ByteBuffer

/**
 * An [InputStream] backed by a [ByteBuffer].
 */
class ByteBufferInputStream(val buffer: ByteBuffer) : InputStream() {

    override fun read(): Int {
        val remaining = buffer.remaining()
        if (remaining == 0) {
            return -1
        }
        return buffer.get().toInt()
    }

    override fun read(bytes: ByteArray): Int = read(bytes, 0, bytes.size)

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        val remaining = buffer.remaining()
        if (remaining == 0) {
            return -1
        }
        val position = buffer.position()
        buffer.get(bytes, offset, Math.min(remaining, length))
        return buffer.position() - position
    }
}