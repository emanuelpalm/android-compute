package se.ltu.emapal.compute.util.media.jackson

import com.fasterxml.jackson.core.JsonGenerator
import se.ltu.emapal.compute.util.media.MediaEncoder
import se.ltu.emapal.compute.util.media.MediaEncoderList
import se.ltu.emapal.compute.util.media.MediaEncoderMap

import java.io.Closeable
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Encodes encodable objects as JSON.
 */
internal class JacksonMediaEncoder(
        private val generator: JsonGenerator
) : MediaEncoder, MediaEncoderList, MediaEncoderMap, Closeable {

    private fun writeNumber(value: Number?) {
        if (value != null)
            when (value) {
                is BigDecimal -> generator.writeNumber(value)
                is Double -> generator.writeNumber(value)
                is Float -> generator.writeNumber(value)

                is BigInteger -> generator.writeNumber(value)
                is Long -> generator.writeNumber(value)
                is Int -> generator.writeNumber(value)
                is Short -> generator.writeNumber(value)
            }
        else
            generator.writeNull()
    }

    override fun encodeList(encoder: (MediaEncoderList) -> Unit) {
        generator.writeStartArray()
        encoder.invoke(this)
        generator.writeEndArray()
    }

    override fun encodeMap(encoder: (MediaEncoderMap) -> Unit) {
        generator.writeStartObject()
        encoder.invoke(this)
        generator.writeEndObject()
    }

    override fun addNull(): MediaEncoderList {
        generator.writeNull()
        return this
    }

    override fun add(value: Boolean?): MediaEncoderList {
        if (value != null)
            generator.writeBoolean(value)
        else
            generator.writeNull()
        return this
    }

    override fun add(value: Number?): MediaEncoderList {
        writeNumber(value)
        return this
    }

    override fun add(value: String?): MediaEncoderList {
        generator.writeString(value)
        return this
    }

    override fun add(value: ByteArray?): MediaEncoderList {
        if (value != null)
            generator.writeBinary(value)
        else
            generator.writeNull()
        return this
    }

    override fun add(value: (MediaEncoder) -> Unit): MediaEncoderList {
        value.invoke(this)
        return this
    }

    override fun addList(encoder: (MediaEncoderList) -> Unit): MediaEncoderList {
        generator.writeStartArray()
        encoder.invoke(this)
        generator.writeEndArray()
        return this
    }

    override fun addMap(encoder: (MediaEncoderMap) -> Unit): MediaEncoderList {
        generator.writeStartObject()
        encoder.invoke(this)
        generator.writeEndObject()
        return this
    }

    override fun addNull(key: String): MediaEncoderMap {
        generator.writeNullField(key)
        return this
    }

    override fun add(key: String, value: Boolean?): MediaEncoderMap {
        generator.writeFieldName(key)
        if (value != null)
            generator.writeBoolean(value)
        else
            generator.writeNull()
        return this
    }

    override fun add(key: String, value: Number?): MediaEncoderMap {
        generator.writeFieldName(key)
        writeNumber(value)
        return this
    }

    override fun add(key: String, value: String?): MediaEncoderMap {
        generator.writeStringField(key, value)
        return this
    }

    override fun add(key: String, value: ByteArray?): MediaEncoderMap {
        if (value != null)
            generator.writeBinaryField(key, value)
        else
            generator.writeNullField(key)

        return this
    }

    override fun add(key: String, value: (MediaEncoder) -> Unit): MediaEncoderMap {
        generator.writeFieldName(key)
        value.invoke(this)
        return this
    }

    override fun addList(key: String, encoder: (MediaEncoderList) -> Unit): MediaEncoderMap {
        generator.writeFieldName(key)
        generator.writeStartArray()
        encoder.invoke(this)
        generator.writeEndArray()
        return this
    }

    override fun addMap(key: String, encoder: (MediaEncoderMap) -> Unit): MediaEncoderMap {
        generator.writeFieldName(key)
        generator.writeStartObject()
        encoder.invoke(this)
        generator.writeEndObject()
        return this
    }

    override fun close() {
        generator.close()
    }
}
