package se.ltu.emapal.compute.util.media

/**
 * Some object that may be encoded using a [MediaEncoder].
 */
interface MediaEncodable {
    fun encode(encoder: MediaEncoder)
}