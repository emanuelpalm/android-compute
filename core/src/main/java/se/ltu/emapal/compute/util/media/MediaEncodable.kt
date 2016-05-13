package se.ltu.emapal.compute.util.media

/**
 * Some object that may be encoded using a [MediaEncoder].
 */
interface MediaEncodable {
    /** Encodable value, useful for encoding object. */
    val encodable: (MediaEncoder) -> Unit
}