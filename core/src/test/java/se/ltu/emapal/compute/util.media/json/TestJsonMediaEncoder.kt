package se.ltu.emapal.compute.util.media.json

import org.junit.Test

import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets

import org.junit.Assert.assertEquals
import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncoder
import java.io.ByteArrayOutputStream

/**
 * Only briefly tests JSON encoding as a complement to the tests in TestMediaTranscoding.
 */
class TestJsonMediaEncoder {
    @Test
    fun shouldEncodeNull() {
        encode({
            it.encodeList { it.addNull() }
        }, "[null]")
    }

    @Test
    fun shouldEncodeBooleans() {
        encode({
            it.encodeMap({
                it
                        .add("a", true)
                        .add("b", false)
            })
        }, "{\"a\":true,\"b\":false}")
    }

    @Test
    fun shouldEncodeIntegers() {
        encode({
            it.encodeList({
                it
                        .add(-1)
                        .add(2L)
                        .add(BigInteger.valueOf(3))
                        .add(BigDecimal.valueOf(4))
            })
        }, "[-1,2,3,4]")
    }

    @Test
    fun shouldEncodeDecimals() {
        encode({
            it.encodeList({
                it
                        .add(1.0)
                        .add(-2.2)
                        .add(BigDecimal.valueOf(100.5))
            })
        }, "[1.0,-2.2,100.5]")
    }

    @Test
    fun shouldEncodeTexts() {
        encode({
            it.encodeList({
                it
                        .add("hello")
                        .add("bye")
            })
        }, "[\"hello\",\"bye\"]")
    }


    @Test
    fun shouldEncodeBlobs() {
        encode({
            it.encodeList({
                it
                        .add("hej".toByteArray(StandardCharsets.UTF_8))
                        .add("hello".toByteArray(StandardCharsets.UTF_8))
            })
        }, "[\"aGVq\",\"aGVsbG8=\"]")
    }

    @Test
    fun shouldEncodeLists() {
        encode({
            it.encodeList({
                it
                        .addList({
                            it
                                    .add(1)
                                    .add(2)
                        })
                        .addList({
                            it
                                    .add(2)
                                    .add(3)
                        })
            })
        }, "[[1,2],[2,3]]")
    }

    @Test
    fun shouldEncodeMaps() {
        encode({
            it.encodeList({
                it
                        .addMap({
                            it
                                    .add("a", 1)
                                    .add("b", 2)
                        })
                        .addMap({
                            it
                                    .add("c", 2)
                                    .add("b", 3)
                        })
            })
        }, "[{\"a\":1,\"b\":2},{\"c\":2,\"b\":3}]")
    }

    private fun encode(encoder: (MediaEncoder) -> Unit, expectedOutput: String) {
        val outputStream = ByteArrayOutputStream()
        val result = JsonMediaConverter.encode(encoder, outputStream)
        when (result) {
            is Result.Success -> assertEquals(expectedOutput, outputStream.toByteArray().toString(StandardCharsets.UTF_8))
            is Result.Failure -> throw result.error
        }
    }
}
