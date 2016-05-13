package se.ltu.emapal.compute.util.media.schema

import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncoder

/**
 * A [MediaSchema] violation.
 *
 * @param entity Identifies violating entity.
 * @param requirement Violated requirement.
 */
data class MediaViolation(
        val entity: String,
        val requirement: MediaRequirement
) : MediaEncodable {

    override val encodable: (MediaEncoder) -> Unit = {
        it.encodeMap({
            it
                    .add("entity", entity)
                    .add("requirement", requirement)
        })
    }
}
