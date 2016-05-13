package se.ltu.emapal.compute.util.media.json

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
internal class JsonMediaEncoder(
        private val generator: JsonGenerator
) : MediaEncoder, MediaEncoderList, MediaEncoderMap, Closeable {

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

    override fun add(value: Boolean): MediaEncoderList {
        generator.writeBoolean(value)
        return this
    }

    override fun add(value: Long): MediaEncoderList {
        generator.writeNumber(value)
        return this
    }

    override fun add(value: BigInteger): MediaEncoderList {
        generator.writeNumber(value)
        return this
    }

    override fun add(value: Double): MediaEncoderList {
        generator.writeNumber(value)
        return this
    }

    override fun add(value: BigDecimal): MediaEncoderList {
        generator.writeNumber(value)
        return this
    }

    override fun add(value: String): MediaEncoderList {
        generator.writeString(value)
        return this
    }

    override fun add(value: ByteArray): MediaEncoderList {
        generator.writeBinary(value)
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

    override fun add(key: String, value: Boolean): MediaEncoderMap {
        generator.writeBooleanField(key, value)
        return this
    }

    override fun add(key: String, value: Long): MediaEncoderMap {
        generator.writeNumberField(key, value)
        return this
    }

    override fun add(key: String, value: BigInteger): MediaEncoderMap {
        generator.writeNumberField(key, BigDecimal(value))
        return this
    }

    override fun add(key: String, value: Double): MediaEncoderMap {
        generator.writeNumberField(key, value)
        return this
    }

    override fun add(key: String, value: BigDecimal): MediaEncoderMap {
        generator.writeNumberField(key, value)
        return this
    }

    override fun add(key: String, value: String): MediaEncoderMap {
        generator.writeStringField(key, value)
        return this
    }

    override fun add(key: String, value: ByteArray): MediaEncoderMap {
        generator.writeBinaryField(key, value)
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
