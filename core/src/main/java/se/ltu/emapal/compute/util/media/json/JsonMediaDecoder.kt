package se.ltu.emapal.compute.util.media.json

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeType
import se.ltu.emapal.compute.util.media.MediaDecoder

import java.util.*

/**
 * Decodes JSON objects.
 */
internal class JsonMediaDecoder(private val node: JsonNode) : MediaDecoder {
    override val type: MediaDecoder.Type
        get() = when (node.nodeType) {
            JsonNodeType.NULL -> MediaDecoder.Type.NULL
            JsonNodeType.BOOLEAN -> MediaDecoder.Type.BOOLEAN
            JsonNodeType.NUMBER -> MediaDecoder.Type.NUMBER
            JsonNodeType.STRING -> MediaDecoder.Type.TEXT
            JsonNodeType.BINARY -> MediaDecoder.Type.BLOB
            JsonNodeType.ARRAY -> MediaDecoder.Type.LIST
            JsonNodeType.OBJECT -> MediaDecoder.Type.MAP
            else -> MediaDecoder.Type.UNDEFINED
        }

    override fun toBoolean() = if (node.isBoolean) node.booleanValue() else throw ClassCastException("Not a boolean.")
    override fun toLong() = if (node.isNumber) node.longValue() else throw ClassCastException("Not a number.")
    override fun toBigInteger() = if (node.isNumber) node.bigIntegerValue() else throw ClassCastException("Not a number.")
    override fun toDouble() = if (node.isNumber) node.doubleValue() else throw ClassCastException("Not a number.")
    override fun toBigDecimal() = if (node.isNumber) node.decimalValue() else throw ClassCastException("Not a number.")
    override fun toText() = if (node.isTextual) node.textValue() else throw ClassCastException("Not a text.")
    override fun toBlob() = if (node.isBinary || node.isTextual) node.binaryValue() else throw ClassCastException("Not a BLOB.")
    override fun toList(): List<MediaDecoder> = if (node.isArray) DecoderList(node) else throw ClassCastException("Not a list.")
    override fun toMap(): Map<String, MediaDecoder> = if (node.isObject) DecoderMap(node) else throw ClassCastException("Not a map.")

    override fun toString() = node.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other?.javaClass != javaClass) {
            return false
        }
        other as JsonMediaDecoder
        return node == other.node
    }

    override fun hashCode() = node.hashCode()

    private class DecoderList(private val jsonArray: JsonNode) : AbstractList<MediaDecoder>() {
        override fun get(index: Int): MediaDecoder? = jsonArray.get(index).let {
            if (it != null) JsonMediaDecoder(it) else null
        }

        override val size: Int
            get() = jsonArray.size()

    }

    private class DecoderMap(private val jsonObject: JsonNode) : AbstractMap<String, MediaDecoder>() {
        override val entries: MutableSet<MutableMap.MutableEntry<String, MediaDecoder>> by lazy {
            jsonObject.fields()
                    .asSequence()
                    .fold(HashMap<String, MediaDecoder>(), { map, entry ->
                        map.put(entry.key, JsonMediaDecoder(entry.value))
                        map
                    })
                    .entries
        }
    }
}
