package se.ltu.emapal.compute.util.media.schema

import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncoder

/**
 * A [MediaSchema] report, reporting on the validity of some media decoder.
 *
 * @param violations All schema violations discovered while verifying some media decoder.
 */
data class MediaReport(
        val violations: Collection<MediaViolation>
) : MediaEncodable {
    /** Whether or not the media decoder reported on is valid.  */
    val isValid: Boolean
        get() = violations.size == 0

    override val encodable: (MediaEncoder) -> Unit
        get() = {
            it.encodeMap {
                it
                        .addList("violations", violations)
                        .add("is_valid", isValid)
            }
        }

}