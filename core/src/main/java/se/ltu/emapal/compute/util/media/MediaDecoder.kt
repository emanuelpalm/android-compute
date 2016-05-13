package se.ltu.emapal.compute.util.media

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Some decodable media value.
 *
 * Decoding is fail-fast, meaning that conversions that would require type coercion causes
 * [ClassCastException]s to be thrown. Some notable exceptions to this rule apply, however, when
 * converting values with [Type.NUMBER]. Number conversions adhere to the same, or at least
 * similar, mechanics as they do when casted normally. Converting a number with decimals to an
 * integer type causes the decimals to be dropped, and converting a number too large to be held by
 * the target type causes information loss.
 */
interface MediaDecoder {
    /** Media entity type.  */
    val type: Type

    /** Decodes media entity as boolean.  */
    fun toBoolean(): Boolean {
        throw ClassCastException("Not a boolean: " + toString())
    }

    /** Decodes media value as byte.  */
    fun toByte() = toLong().toByte()

    /** Decodes media value as short.  */
    fun toShort() = toLong().toShort()

    /** Decodes media value as integer.  */
    fun toInt() = toLong().toInt()

    /** Decodes media value as long.  */
    fun toLong(): Long {
        throw ClassCastException("Not a number: " + toString())
    }

    /** Decodes media value as big integer.  */
    fun toBigInteger(): BigInteger {
        throw ClassCastException("Not a number: " + toString())
    }

    /** Decodes media value as 32-bit IEEE floating point number.  */
    fun toFloat() = toDouble().toFloat()

    /** Decodes media value as 64-bit IEEE floating point number.  */
    fun toDouble(): Double {
        throw ClassCastException("Not a number: " + toString())
    }

    /** Decodes media value as big decimal.  */
    fun toBigDecimal(): BigDecimal {
        throw ClassCastException("Not a number: " + toString())
    }

    /** Decodes media value as a string.  */
    fun toText(): String {
        throw ClassCastException("Not a text: " + toString())
    }

    /** Decodes media value as byte array. */
    fun toBlob(): ByteArray {
        throw ClassCastException("Not a BLOB: " + toString())
    }

    /** Decodes media value as list of entities.  */
    fun toList(): List<MediaDecoder> {
        throw ClassCastException("Not a list: " + toString())
    }

    /** Decodes media value as map of string keys and arbitrary values.  */
    fun toMap(): Map<String, MediaDecoder> {
        throw ClassCastException("Not a map: " + toString())
    }

    /**
     * Indicates the concrete type of some entity represented by a [MediaDecoder].
     */
    enum class Type {
        UNDEFINED,
        NULL,
        BOOLEAN,
        NUMBER,
        TEXT,
        BLOB,
        LIST,
        MAP
    }
}
