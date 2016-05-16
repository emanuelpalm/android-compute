package se.ltu.emapal.compute.util.media

/**
 * Some exception that may be encoded or decoded.
 */
abstract class MediaEncodableException(
        message: String? = null,
        cause: Throwable? = null
) : RuntimeException(message, cause), MediaEncodable {

    companion object {
        /** Wraps [throwable] in a [MediaEncodableException]. */
        fun wrap(throwable: Throwable) = object : MediaEncodableException(
                throwable.message,
                throwable
        ) {
            override val encodable: (MediaEncoder) -> Unit
                get() = { it.encodeMap { it.add("error", message) } }
        }
    }
}