package se.ltu.emapal.compute.util.media

/**
 * Media entity map encoder.
 */
interface MediaEncoderMap {
    /** Adds named `null` value to map. */
    fun addNull(key: String): MediaEncoderMap

    /** Adds named `boolean` value to map. */
    fun add(key: String, value: Boolean?): MediaEncoderMap

    /** Adds named [Number] value to map. */
    fun add(key: String, value: Number?): MediaEncoderMap

    /** Adds named [String] value to map. */
    fun add(key: String, value: String?): MediaEncoderMap

    /** Adds named [ByteArray] value to list. */
    fun add(key: String, value: ByteArray?): MediaEncoderMap

    /** Adds value to list via provided consumer lambda. */
    fun add(key: String, value: (MediaEncoder) -> Unit): MediaEncoderMap

    /** Adds [MediaEncodable] value to list. */
    fun add(key: String, value: MediaEncodable?): MediaEncoderMap = if (value != null)
        add(key, { value.encodable(it) })
    else
        addNull(key)

    /** Adds named list of values to map. */
    fun addList(key: String, encoder: (MediaEncoderList) -> Unit): MediaEncoderMap

    /** Adds named list of [MediaEncodable]s to map. */
    fun addList(key: String, list: Collection<MediaEncodable>): MediaEncoderMap {
        return addList(key, { encoder ->
            list.forEach { element ->
                encoder.add { element.encodable(it) }
            }
        })
    }

    /** Adds named map of values to map. */
    fun addMap(key: String, encoder: (MediaEncoderMap) -> Unit): MediaEncoderMap

    /** Adds named map of [MediaEncodable]s to map. */
    fun addMap(key: String, map: Map<String, MediaEncodable>): MediaEncoderMap {
        return addMap(key, { encoder ->
            map.entries.forEach { entry ->
                encoder.add(entry.key, { entry.value.encodable(it) })
            }
        })
    }
}