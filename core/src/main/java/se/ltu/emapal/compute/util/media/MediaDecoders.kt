package se.ltu.emapal.compute.util.media

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Various [MediaDecoder] utilities.
 */
object MediaDecoders {
    private val NULL = TypeNull()
    private val TRUE = TypeBoolean(true)
    private val FALSE = TypeBoolean(false)

    /** Creates new media decoder from `null`. */
    fun ofNull(): MediaDecoder = NULL

    /** Creates new media decoder from `boolean`. */
    fun ofBoolean(isTrue: Boolean): MediaDecoder = if (isTrue) TRUE else FALSE

    /** Creates new media decoder from [Number]. */
    fun ofNumber(number: Number): MediaDecoder = TypeNumber(number)

    /** Creates new media decoder from [String]. */
    fun ofText(string: String): MediaDecoder = TypeText(string)

    /** Creates new media decoder from [ByteArray]. */
    fun ofBlob(byteArray: ByteArray): MediaDecoder = TypeBlob(byteArray)

    /** Creates new media decoder from [List]. */
    fun ofList(list: List<MediaDecoder>): MediaDecoder = TypeList(list)

    /** Creates new media decoder from [Map]. */
    fun ofMap(map: Map<String, MediaDecoder>): MediaDecoder = TypeMap(map)

    /**
     * Null type.
     */
    class TypeNull : MediaDecoder {
        override val type: MediaDecoder.Type
            get() = MediaDecoder.Type.NULL

        override fun toString(): String = "NULL"
    }

    /**
     * Boolean type.
     */
    private class TypeBoolean(private val value: Boolean) : MediaDecoder {
        override val type: MediaDecoder.Type
            get() = MediaDecoder.Type.BOOLEAN

        override fun toBoolean(): Boolean = value

        override fun toString(): String {
            return if (value) "true" else "false"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is MediaDecoder) {
                return false
            }
            return other.type === MediaDecoder.Type.BOOLEAN
                    && value == other.toBoolean()
        }

        override fun hashCode(): Int = value.hashCode()
    }

    /**
     * Number type.
     */
    private class TypeNumber(private val value: Number) : MediaDecoder {
        override val type: MediaDecoder.Type
            get() = MediaDecoder.Type.NUMBER

        override fun toLong(): Long = value.toLong()

        override fun toBigInteger(): BigInteger {
            if (value is BigInteger) {
                return value
            }
            if (value is BigDecimal) {
                return value.toBigInteger()
            }
            return BigInteger.valueOf(value.toLong())
        }

        override fun toDouble(): Double = value.toDouble()

        override fun toBigDecimal(): BigDecimal {
            if (value is BigDecimal) {
                return value
            }
            if (value is BigInteger) {
                return BigDecimal(value)
            }
            return BigDecimal.valueOf(value.toDouble())
        }

        override fun toString(): String = value.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is MediaDecoder) {
                return false
            }
            return other.type === MediaDecoder.Type.NUMBER
                    && toBigDecimal() == other.toBigDecimal()
        }

        override fun hashCode(): Int = value.hashCode()
    }

    /**
     * Text type.
     */
    private class TypeText(private val value: String) : MediaDecoder {
        override val type: MediaDecoder.Type
            get() = MediaDecoder.Type.TEXT

        override fun toText(): String = value

        override fun toString(): String = "\"${toText()}\""

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is MediaDecoder) {
                return false
            }
            return other.type === MediaDecoder.Type.NUMBER
                    && value == other.toText()
        }

        override fun hashCode(): Int = value.hashCode()
    }

    /**
     * BLOB type.
     */
    private class TypeBlob(private val value: ByteArray) : MediaDecoder {
        override val type: MediaDecoder.Type
            get() = MediaDecoder.Type.TEXT

        override fun toBlob(): ByteArray = value

        override fun toString(): String = toBlob().toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is MediaDecoder) {
                return false
            }
            return other.type === MediaDecoder.Type.NUMBER
                    && value == other.toBlob()
        }

        override fun hashCode(): Int = value.hashCode()
    }

    /**
     * List type.
     */
    private class TypeList(private val list: List<MediaDecoder>) : MediaDecoder {
        override val type: MediaDecoder.Type
            get() = MediaDecoder.Type.LIST

        override fun toList(): List<MediaDecoder> = list

        override fun toString(): String = list.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is MediaDecoder) {
                return false
            }
            return other.type === MediaDecoder.Type.LIST
                    && list == other.toList()
        }

        override fun hashCode(): Int = list.hashCode()
    }

    /**
     * Map type.
     */
    private class TypeMap(private val map: Map<String, MediaDecoder>) : MediaDecoder {
        override val type: MediaDecoder.Type
            get() = MediaDecoder.Type.MAP

        override fun toMap(): Map<String, MediaDecoder> = map

        override fun toString(): String = map.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other !is MediaDecoder) {
                return false
            }
            return other.type === MediaDecoder.Type.MAP
                    && map == other.toMap()
        }

        override fun hashCode(): Int = map.hashCode()
    }
}
