package se.ltu.emapal.compute.util.media.json

import org.junit.Test

import se.ltu.emapal.compute.util.media.MediaDecoder

import org.junit.Assert.*
import se.ltu.emapal.compute.util.Result
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

/**
 * Only briefly tests JSON decoding as a complement to the tests in TestMediaTranscoding.
 */
class TestJsonMediaDecoder {
    @Test
    fun shouldDecodeNull() {
        decode("[null]", { decoder ->
            val value = decoder.toList()[0]
            assertNotNull(value)
            assertEquals(MediaDecoder.Type.NULL, value.type)
        })
    }

    @Test
    fun shouldDecodeBooleans() {
        decode("{\"a\":true,\"b\":false}", { decoder ->
            val map = decoder.toMap()
            assertEquals(2, map.size)
            assertTrue(map["a"]!!.toBoolean())
            assertFalse(map["b"]!!.toBoolean())
        })
    }

    @Test
    fun shouldDecodeIntegers() {
        decode("[-1,2,3,4]", { decoder ->
            val list = decoder.toList()
            assertEquals(4, list.size)
            assertEquals((-1).toShort(), list[0].toShort())
            assertEquals(2, list[1].toInt())
            assertEquals(3L, list[2].toLong())
            assertEquals(4, list[3].toBigInteger().intValueExact())
        })
    }

    @Test
    fun shouldDecodeDecimals() {
        decode("[1.1,-2.2,1.005e2]", { decoder ->
            val list = decoder.toList()
            assertEquals(3, list.size)
            assertEquals(1.1f, list[0].toFloat(), 0.001f)
            assertEquals(-2.2, list[1].toDouble(), 0.001)
            assertEquals(100.5, list[2].toBigDecimal().toDouble(), 0.001)
        })
    }

    @Test
    fun shouldDecodeStrings() {
        decode("[\"hello\",\"bye\"]", { decoder ->
            val list = decoder.toList()
            assertEquals(2, list.size)
            assertEquals("hello", list[0].toText())
            assertEquals("bye", list[1].toText())
        })
    }

    @Test
    fun shouldDecodeBlobs() {
        decode("[\"aGVq\",\"aGVsbG8=\"]", { decoder ->
            val list = decoder.toList()
            assertEquals(2, list.size)
            assertEquals("hej", list[0].toBlob().toString(StandardCharsets.UTF_8))
            assertEquals("hello", list[1].toBlob().toString(StandardCharsets.UTF_8))
        })
    }

    @Test
    fun shouldDecodeLists() {
        decode("[[1,2],[2,3]]", { decoder ->
            val list = decoder.toList()
            assertEquals(2, list.size)

            val listA = list[0].toList()
            assertEquals(1, listA[0].toInt())
            assertEquals(2, listA[1].toInt())

            val listB = list[1].toList()
            assertEquals(2, listB[0].toInt())
            assertEquals(3, listB[1].toInt())
        })
    }

    @Test
    fun shouldDecodeMaps() {
        decode("[{\"a\":1,\"b\":2},{\"c\":2,\"b\":3}]", { decoder ->
            val list = decoder.toList()
            assertEquals(2, list.size)

            val mapA = list[0].toMap()
            assertEquals(1, mapA["a"]!!.toInt())
            assertEquals(2, mapA["b"]!!.toInt())

            val mapB = list[1].toMap()
            assertEquals(2, mapB["c"]!!.toInt())
            assertEquals(3, mapB["b"]!!.toInt())
        })
    }

    private fun decode(input: String, decoderConsumer: (MediaDecoder) -> Unit) {
        val result = JsonMediaConverter.decode(ByteArrayInputStream(input.toByteArray()))
        decoderConsumer(when (result) {
            is Result.Success -> result.value
            is Result.Failure -> throw result.error
        })
    }
}
