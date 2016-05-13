package se.ltu.emapal.compute.util.media

/**
 * Some object used for encoding some set of data into a single media entity.
 *
 * The interface can only be used to create either one list or one map. The object created has no
 * theoretical limit on the amount of nested values it contains. If attempting to encode a second
 * set of data, an exception will be thrown.
 */
interface MediaEncoder {
    /** Encodes list of values.  */
    fun encodeList(encoder: (MediaEncoderList) -> Unit)

    /** Encodes map of values.  */
    fun encodeMap(encoder: (MediaEncoderMap) -> Unit)
}
