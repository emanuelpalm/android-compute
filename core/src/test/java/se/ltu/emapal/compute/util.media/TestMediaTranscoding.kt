package se.ltu.emapal.compute.util.media

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

import java.math.BigDecimal
import java.math.BigInteger

import org.junit.Assert.*
import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.media.jackson.JacksonMediaConverter
import java.io.*
import java.nio.charset.StandardCharsets

/**
 * Tests encoder/decoder pairs by encoding, decoding and verifying results.
 */
@RunWith(Parameterized::class)
class TestMediaTranscoding(
        private val decoder: (ByteArray) -> Result<MediaDecoder, IOException>,
        private val encoder: ((MediaEncoder) -> Unit) -> Result<ByteArray, IOException>
) {

    @Test
    fun shouldTranscodeNull() {
        transcode(
                {
                    it.encodeList({
                        it
                                .addNull()
                                .add(null as Boolean?)
                                .add(null as Number?)
                                .add(null as String?)
                                .add(null as ByteArray?)
                                .add(null as MediaEncodable?)
                    })
                },
                {
                    val list = it.toList()
                    assertEquals(6, list.size)

                    list.forEach {
                        assertNotNull(it)
                        assertEquals(MediaDecoder.Type.NULL, it.type)
                    }
                })
        transcode(
                {
                    it.encodeMap({
                        it
                                .addNull("K0")
                                .add("K1", null as Boolean?)
                                .add("K2", null as Number?)
                                .add("K3", null as String?)
                                .add("K4", null as ByteArray?)
                                .add("K5", null as MediaEncodable?)
                    })
                },
                {
                    val map = it.toMap()
                    assertEquals(6, map.size)

                    map.forEach {
                        assertEquals(MediaDecoder.Type.NULL, it.value.type)
                    }
                })
    }

    @Test
    fun shouldTranscodeBooleans() {
        transcode(
                { it.encodeList({ it.add(true) }) },
                {
                    val value = it.toList()[0]
                    assertNotNull(value)
                    assertEquals(MediaDecoder.Type.BOOLEAN, value.type)
                    assertTrue(value.toBoolean())
                })
        transcode(
                { it.encodeMap({ it.add("key", false) }) },
                {
                    val value = it.toMap()["key"]
                    assertNotNull(value)
                    assertEquals(MediaDecoder.Type.BOOLEAN, value!!.type)
                    assertFalse(value.toBoolean())
                })
    }

    @Test
    fun shouldTranscodeIntegers() {
        transcode(
                {
                    it.encodeList({
                        it
                                .add(Short.MAX_VALUE.toLong())
                                .add(Integer.MAX_VALUE.toLong())
                                .add(Long.MAX_VALUE)
                                .add(BigInteger.TEN)
                    })
                },
                {
                    val list = it.toList()
                    assertEquals(4, list.size)

                    assertEquals(Short.MAX_VALUE, list[0].toShort())
                    assertEquals(Integer.MAX_VALUE, list[1].toInt())
                    assertEquals(Long.MAX_VALUE, list[2].toLong())
                    assertEquals(BigInteger.TEN, list[3].toBigInteger())

                    assertTrue(list.all({ it.type === MediaDecoder.Type.NUMBER }))
                })
        transcode(
                {
                    it.encodeMap({
                        it
                                .add("a", Short.MAX_VALUE.toLong())
                                .add("b", Integer.MAX_VALUE.toLong())
                                .add("c", Long.MAX_VALUE)
                                .add("d", BigInteger.TEN)
                                .add("e", 10.0)
                    })
                },
                {
                    val map = it.toMap()
                    assertEquals(5, map.size)

                    assertEquals(Short.MAX_VALUE, map["a"]!!.toShort())
                    assertEquals(Integer.MAX_VALUE, map["b"]!!.toInt())
                    assertEquals(BigInteger.valueOf(Long.MAX_VALUE), map["c"]!!.toBigInteger())
                    assertEquals(BigInteger.TEN.toShort(), map["d"]!!.toShort())
                    assertEquals(10.toShort(), map["e"]!!.toShort())
                })
    }

    @Test
    fun shouldTranscodeDecimals() {
        transcode(
                {
                    it.encodeList({
                        it
                                .add(Float.MAX_VALUE.toDouble())
                                .add(Double.MAX_VALUE)
                                .add(BigDecimal.valueOf(105, 1))
                    })
                },
                {
                    val list = it.toList()
                    assertEquals(3, list.size)
                    assertEquals(Float.MAX_VALUE, list[0].toFloat(), 0.01f)
                    assertEquals(Double.MAX_VALUE, list[1].toDouble(), 0.01)
                    assertEquals(BigDecimal.valueOf(105, 1), list[2].toBigDecimal())

                    assertTrue(list.all({ it.type === MediaDecoder.Type.NUMBER }))
                })
        transcode(
                {
                    it.encodeMap({
                        it
                                .add("a", Float.MAX_VALUE.toDouble())
                                .add("b", Double.MAX_VALUE)
                                .add("c", BigDecimal.valueOf(105, 1))
                                .add("d", 10)
                    })
                },
                {
                    val map = it.toMap()
                    assertEquals(4, map.size)
                    assertEquals(Float.MAX_VALUE.toDouble(), map["a"]!!.toDouble(), 0.01)
                    assertEquals(BigDecimal.valueOf(Double.MAX_VALUE), map["b"]!!.toBigDecimal())
                    assertEquals(BigDecimal.valueOf(105, 1).toFloat(), map["c"]!!.toFloat(), 0.01f)
                    assertEquals(10f, map["d"]!!.toFloat(), 0.01f)
                })
    }

    @Test
    fun shouldTranscodeTexts() {
        transcode(
                { it.encodeList({ it.add("hello") }) },
                {
                    val value = it.toList()[0]
                    assertNotNull(value)
                    assertEquals(MediaDecoder.Type.TEXT, value.type)
                    assertEquals("hello", value.toText())
                })
        transcode(
                { it.encodeMap({ it.add("key", "bye") }) },
                {
                    val value = it.toMap()["key"]
                    assertNotNull(value)
                    assertEquals(MediaDecoder.Type.TEXT, value!!.type)
                    assertEquals("bye", value.toText())
                })
    }

    @Test
    fun shouldTranscodeBlobs() {
        transcode(
                { it.encodeList({ it.add("hello".toByteArray()) }) },
                {
                    val value = it.toList()[0]
                    assertNotNull(value)
                    //assertEquals(MediaDecoder.Type.BLOB, value.type)
                    assertEquals("hello", value.toBlob().toString(StandardCharsets.UTF_8))
                })
        transcode(
                { it.encodeMap({ it.add("key", "bye".toByteArray()) }) },
                {
                    val value = it.toMap()["key"]
                    assertNotNull(value)
                    //assertEquals(MediaDecoder.Type.BLOB, value!!.type)
                    assertEquals("bye", value!!.toBlob().toString(StandardCharsets.UTF_8))
                })
    }

    @Test
    fun shouldTranscodeEncodables() {
        transcode(
                {
                    it.encodeList({
                        it
                                .add({ it.encodeMap({ it.add("x", "y") }) })
                                .add({ it.encodeList({ it.add("z") }) })
                    })
                },
                {
                    val list = it.toList()
                    assertEquals(2, list.size)
                    assertEquals(MediaDecoder.Type.MAP, list[0].type)
                    assertEquals(MediaDecoder.Type.LIST, list[1].type)

                    val map0 = list[0].toMap()
                    assertEquals(1, map0.size)
                    assertEquals("y", map0["x"]!!.toText())

                    val list1 = list[1].toList()
                    assertEquals(1, list1.size)
                    assertEquals("z", list1[0].toText())
                })
        transcode(
                {
                    it.encodeMap({
                        it
                                .add("a", { it.encodeMap({ it.add("x", "y") }) })
                                .add("b", { it.encodeList({ it.add("z") }) })
                    })
                },
                {
                    val map = it.toMap()
                    assertEquals(2, map.size)
                    assertEquals(MediaDecoder.Type.MAP, map["a"]!!.type)
                    assertEquals(MediaDecoder.Type.LIST, map["b"]!!.type)

                    val mapA = map["a"]!!.toMap()
                    assertEquals(1, mapA.size)
                    assertEquals("y", mapA["x"]!!.toText())

                    val listB = map["b"]!!.toList()
                    assertEquals(1, listB.size)
                    assertEquals("z", listB[0].toText())
                })
    }

    @Test
    fun shouldTranscodeLists() {
        transcode(
                {
                    it.encodeList({
                        it
                                .addList({ it.add(1).add(2) })
                                .addList({ it.add("a").add("b") })
                    })
                },
                {
                    val list = it.toList()
                    assertEquals(2, list.size)
                    assertEquals(MediaDecoder.Type.LIST, list[0].type)
                    assertEquals(MediaDecoder.Type.LIST, list[1].type)

                    val list0 = list[0].toList()
                    assertEquals(2, list0.size)
                    assertEquals(1, list0[0].toInt())
                    assertEquals(2, list0[1].toInt())

                    val list1 = list[1].toList()
                    assertEquals(2, list1.size)
                    assertEquals("a", list1[0].toText())
                    assertEquals("b", list1[1].toText())
                })
        transcode(
                {
                    it.encodeMap({
                        it
                                .addList("A", { it.add(true).add(false) })
                                .addList("B", { it.add("x").add("y") })
                    })
                },
                {
                    val map = it.toMap()
                    assertEquals(2, map.size)
                    assertEquals(MediaDecoder.Type.LIST, map["A"]!!.type)
                    assertEquals(MediaDecoder.Type.LIST, map["B"]!!.type)

                    val listA = map["A"]!!.toList()
                    assertEquals(2, listA.size)
                    assertEquals(true, listA[0].toBoolean())
                    assertEquals(false, listA[1].toBoolean())

                    val listB = map["B"]!!.toList()
                    assertEquals(2, listB.size)
                    assertEquals("x", listB[0].toText())
                    assertEquals("y", listB[1].toText())
                })
    }

    @Test
    fun shouldTranscodeMaps() {
        transcode(
                {
                    it.encodeList({
                        it
                                .addMap({ it.add("k0", 1).add("k1", 2) })
                                .addMap({ it.add("k0", "a").add("k1", "b") })
                    })
                },
                {
                    val list = it.toList()
                    assertEquals(2, list.size)
                    assertEquals(MediaDecoder.Type.MAP, list[0].type)
                    assertEquals(MediaDecoder.Type.MAP, list[1].type)

                    val map0 = list[0].toMap()
                    assertEquals(2, map0.size)
                    assertEquals(1, map0["k0"]!!.toInt())
                    assertEquals(2, map0["k1"]!!.toInt())

                    val map1 = list[1].toMap()
                    assertEquals(2, map1.size)
                    assertEquals("a", map1["k0"]!!.toText())
                    assertEquals("b", map1["k1"]!!.toText())
                })
        transcode(
                { it.encodeMap({ it.addMap("A", { mapEncoderA -> mapEncoderA.add("k0", true).add("k1", false) }).addMap("B", { mapEncoderB -> mapEncoderB.add("k0", "x").add("k1", "y") }) }) },
                { decoder ->
                    val map = decoder.toMap()
                    assertEquals(2, map.size)
                    assertEquals(MediaDecoder.Type.MAP, map["A"]!!.type)
                    assertEquals(MediaDecoder.Type.MAP, map["B"]!!.type)

                    val mapA = map["A"]!!.toMap()
                    assertEquals(2, mapA.size)
                    assertEquals(true, mapA["k0"]!!.toBoolean())
                    assertEquals(false, mapA["k1"]!!.toBoolean())

                    val mapB = map["B"]!!.toMap()
                    assertEquals(2, mapB.size)
                    assertEquals("x", mapB["k0"]!!.toText())
                    assertEquals("y", mapB["k1"]!!.toText())
                })
    }

    private fun transcode(input: (MediaEncoder) -> Unit, output: (MediaDecoder) -> Unit) {
        encoder(input)
                .apply { decoder(it) }
                .map { output(it) }
                .unwrap()
    }

    companion object {
        /**
         * Provides an encoder/decoder pair for each type of encoding/decoding supported.
         */
        @JvmStatic
        @Parameterized.Parameters
        @Suppress("unused")
        fun parameters(): Collection<Array<Any>> {
            return listOf(
                    arrayOf<Any>(
                            { bytes: ByteArray -> JacksonMediaConverter.JSON.decode(bytes) },
                            { value: (MediaEncoder) -> Unit -> JacksonMediaConverter.JSON.encode(value) }
                    )
            )
        }
    }
}
