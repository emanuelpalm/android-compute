package se.ltu.emapal.compute.io

import org.junit.Assert
import org.junit.Test
import se.ltu.emapal.compute.ComputeBatch
import se.ltu.emapal.compute.ComputeError
import se.ltu.emapal.compute.ComputeLambda
import se.ltu.emapal.compute.ComputeLogEntry
import se.ltu.emapal.compute.util.nio.ByteBufferChannel
import java.nio.ByteBuffer

class TestComputeChannel {
    val buffer = ByteBuffer.allocate(ComputeChannel.BUFFER_SIZE)
    val channel = ComputeChannel(ByteBufferChannel(buffer))

    @Test
    fun shouldTransceiveClientMessages() {
        transceive(ComputeMessage.ClientBatch(1, ComputeBatch(100, 200, "HELLO".toByteArray())))
        transceive(ComputeMessage.ClientError(2, ComputeError(1234, "Noes!")))
        transceive(ComputeMessage.ClientExit(3))
        transceive(ComputeMessage.ClientLogEntry(4, ComputeLogEntry(111, 222, "Surprise!")))
        transceive(ComputeMessage.ClientImAlive(5))
    }

    @Test
    fun shouldTransceiveServiceMessages() {
        transceive(ComputeMessage.ServiceBatch(1, ComputeBatch(111, 222, "hello".toByteArray())))
        transceive(ComputeMessage.ServiceExit(2))
        transceive(ComputeMessage.ServiceLambda(3, ComputeLambda(111, "lcm:register(lambda)")))
        transceive(ComputeMessage.ServiceImAlive(4))
    }

    private fun transceive(message: ComputeMessage) {
        buffer.rewind()
        channel.write(message)
                .unwrap()

        buffer.rewind()
        val response = channel.read()
                .unwrap()

        Assert.assertEquals(message, response)
    }
}