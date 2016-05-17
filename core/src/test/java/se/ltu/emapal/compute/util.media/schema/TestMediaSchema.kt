package se.ltu.emapal.compute.util.media.schema

import org.junit.Test

import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.HashMap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import se.ltu.emapal.compute.util.Result
import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.MediaDecoders
import se.ltu.emapal.compute.util.media.jackson.JacksonMediaConverter

class TestMediaSchema {
    @Test
    fun shouldGenerateCorrectReports() {
        val schema = MediaSchema.typeMap()
                .schemaEntry("name", MediaSchema.typeText())
                .schemaEntry("title", MediaSchema.typeText()
                        .setOptional()
                        .setRegex("mr|mrs|miss|dr"))
                .schemaEntry("age", MediaSchema.typeNumber()
                        .setMinimum(0)
                        .setMaximum(99))
                .schemaEntry("colors", MediaSchema.typeList()
                        .schemaDefault(MediaSchema.typeAny()
                                .add(MediaRequirement({
                                    val regex = Regex("[0-9a-fA-F]{6}")
                                    when (it.type) {
                                        MediaDecoder.Type.TEXT -> it.toText().matches(regex)
                                        MediaDecoder.Type.NUMBER -> it.toInt() >= 0x000000 && it.toInt() <= 0xffffff
                                        else -> false
                                    }
                                }, "is_color"))))

        assertEquals(
                Result.Failure<MediaDecoder, MediaSchemaException>(MediaSchemaException(
                        MediaViolation("", MediaRequirement("type", "MAP"))
                )),
                schema.verify(MediaDecoders.ofText("string")))

        assertEquals(
                Result.Failure<MediaDecoder, MediaSchemaException>(MediaSchemaException(
                        MediaViolation("age", MediaRequirement("optional", "false")),
                        MediaViolation("colors", MediaRequirement("optional", "false")),
                        MediaViolation("name", MediaRequirement("optional", "false"))
                )),
                schema.verify(MediaDecoders.ofMap(HashMap())))

        assertEquals(
                Result.Failure<MediaDecoder, MediaSchemaException>(MediaSchemaException(
                        MediaViolation("name", MediaRequirement("type", "TEXT")),
                        MediaViolation("title", MediaRequirement("type", "TEXT")),
                        MediaViolation("age", MediaRequirement("type", "NUMBER")),
                        MediaViolation("colors", MediaRequirement("type", "LIST"))
                )),
                schema.verify(MediaDecoders.ofMap(object : HashMap<String, MediaDecoder>() {
                    init {
                        put("name", MediaDecoders.ofNumber(10))
                        put("title", MediaDecoders.ofBoolean(false))
                        put("age", MediaDecoders.ofText("old"))
                        put("colors", MediaDecoders.ofNull())
                    }
                })))

        assertEquals(
                Result.Failure<MediaDecoder, MediaSchemaException>(MediaSchemaException(
                        MediaViolation("title", MediaRequirement("regex", "mr|mrs|miss|dr")),
                        MediaViolation("age", MediaRequirement("maximum", "99")),
                        MediaViolation("colors[0]", MediaRequirement("is_color")),
                        MediaViolation("colors[1]", MediaRequirement("is_color")),
                        MediaViolation("party", MediaRequirement("expected", "false"))
                )),
                schema.verify(MediaDecoders.ofMap(object : HashMap<String, MediaDecoder>() {
                    init {
                        put("name", MediaDecoders.ofText("Jones"))
                        put("title", MediaDecoders.ofText("phd"))
                        put("age", MediaDecoders.ofNumber(120))
                        put("colors", MediaDecoders.ofList(Arrays.asList(
                                MediaDecoders.ofNumber(0xff0000ff.toInt()),
                                MediaDecoders.ofText("red"))))
                        put("party", MediaDecoders.ofText("Yellow"))
                    }
                })))

        assertTrue(
                schema.verify(MediaDecoders.ofMap(object : HashMap<String, MediaDecoder>() {
                    init {
                        put("name", MediaDecoders.ofText("Jones"))
                        put("title", MediaDecoders.ofText("dr"))
                        put("age", MediaDecoders.ofNumber(42))
                        put("colors", MediaDecoders.ofList(Arrays.asList(
                                MediaDecoders.ofNumber(0x0000ff),
                                MediaDecoders.ofText("ff0000"))))
                    }
                })) is Result.Success<MediaDecoder, MediaSchemaException>
        )
    }

    @Test
    @Throws(Throwable::class)
    fun shouldEncodeCorrectly() {
        val schema = MediaSchema.typeList()
                .setSize(5)
                .schemaElement(0, MediaSchema.typeNull())
                .schemaElement(1, MediaSchema.typeBoolean())
                .schemaElement(2, MediaSchema.typeNumber()
                        .setMinimum(10.0)
                        .setMaximum(20.0))
                .schemaElement(3, MediaSchema.typeNumber()
                        .setMinimum(BigInteger.valueOf(100))
                        .setMaximum(BigInteger.valueOf(200)))
                .schemaElement(4, MediaSchema.typeNumber()
                        .setMinimum(BigDecimal.valueOf(1000))
                        .setMaximum(BigDecimal.valueOf(2000)))

        val json = JacksonMediaConverter.JSON.encode(schema.encodable).unwrap().toString(StandardCharsets.UTF_8)
        assertEquals("{\"type\":\"LIST\",\"optional\":false,\"requirements\":[{\"size\":[\"5\"]}],\"elements\":{\"0\":{\"type\":\"NULL\",\"optional\":false},\"1\":{\"type\":\"BOOLEAN\",\"optional\":false},\"2\":{\"type\":\"NUMBER\",\"optional\":false,\"requirements\":[{\"maximum\":[\"20.0\"]},{\"minimum\":[\"10.0\"]}]},\"3\":{\"type\":\"NUMBER\",\"optional\":false,\"requirements\":[{\"maximum\":[\"200\"]},{\"minimum\":[\"100\"]}]},\"4\":{\"type\":\"NUMBER\",\"optional\":false,\"requirements\":[{\"maximum\":[\"2000\"]},{\"minimum\":[\"1000\"]}]}}}", json)
    }
}
