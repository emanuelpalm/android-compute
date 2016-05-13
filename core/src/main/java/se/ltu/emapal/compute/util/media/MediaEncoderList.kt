package se.ltu.emapal.compute.util.media

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Media entity list encoder.
 */
interface MediaEncoderList {
    /** Adds `null` value to list. */
    fun addNull(): MediaEncoderList

    /** Adds `boolean` value to list. */
    fun add(value: Boolean): MediaEncoderList

    /** Adds `long` value to list. */
    fun add(value: Long): MediaEncoderList

    /** Adds [BigInteger] value to list. */
    fun add(value: BigInteger): MediaEncoderList

    /** Adds `double` value to list. */
    fun add(value: Double): MediaEncoderList

    /** Adds [BigDecimal] value to list. */
    fun add(value: BigDecimal): MediaEncoderList

    /** Adds [String] value to list. */
    fun add(value: String): MediaEncoderList

    /** Adds [ByteArray] value to list. */
    fun add(value: ByteArray): MediaEncoderList

    /** Adds value to list via provided lambda. */
    fun add(value: (MediaEncoder) -> Unit): MediaEncoderList

    /** Adds [MediaEncodable] value to list. */
    fun add(value: MediaEncodable) = add { value.encodable(it) }

    /** Adds list of values to list. */
    fun addList(encoder: (MediaEncoderList) -> Unit): MediaEncoderList

    /** Adds list of [MediaEncodable]s to list. */
    fun addList(list: Collection<MediaEncodable>): MediaEncoderList {
        return addList({ encoder ->
            list.forEach { element ->
                encoder.add { element.encodable(it) }
            }
        })
    }

    /** Adds map of values to list. */
    fun addMap(encoder: (MediaEncoderMap) -> Unit): MediaEncoderList

    /** Adds map of [MediaEncodable]s to list. */
    fun addMap(map: Map<String, MediaEncodable>): MediaEncoderList {
        return addMap({ encoder ->
            map.entries.forEach { entry ->
                encoder.add(entry.key, { entry.value.encodable(it) })
            }
        })
    }
}