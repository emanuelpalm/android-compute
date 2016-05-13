package se.ltu.emapal.compute.util.media.schema

import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncoder

import java.util.Objects

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

    override fun encode(encoder: MediaEncoder) = encoder.encodeMap({
        it
                .add("entity", entity)
                .add("requirement", requirement)
    })
}
