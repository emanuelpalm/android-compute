package se.ltu.emapal.compute.util.media.schema

import se.ltu.emapal.compute.util.media.MediaEncodableException
import se.ltu.emapal.compute.util.media.MediaEncoder

/**
 * Signifies that some media decoder fails to comply to some [MediaSchema].
 *
 * @param violations A list of violated [MediaRequirement]s.
 */
class MediaSchemaException(
        val violations: List<MediaViolation>
) : MediaEncodableException("Media schema violated.") {

    /** Constructs new media schema error from given violations. */
    constructor(vararg violations: MediaViolation) : this (listOf(*violations))

    override val encodable: (MediaEncoder) -> Unit
        get() = {
            it.encodeMap {
                it.addList("violations", violations)
            }
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other?.javaClass != javaClass) {
            return false
        }
        other as MediaSchemaException
        return violations == other.violations
    }

    override fun hashCode() = violations.hashCode()
    override fun toString() = "MediaSchemaException(violations=$violations)"
}