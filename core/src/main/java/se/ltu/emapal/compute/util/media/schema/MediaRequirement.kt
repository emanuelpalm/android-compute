package se.ltu.emapal.compute.util.media.schema

import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncoder

import java.util.Objects

/**
 * Some requirement imposed by some [MediaSchema].
 *
 * Requirements are, in essence, simple predicate functions used to test [MediaDecoder]s. For the
 * sake of representing requirements, each such also provide a formal name and formal parameters.
 * The name and parameters have no impact on the execution of the predicate, and thus don't effect
 * the outcome of requirement evaluation.
 */
class MediaRequirement private constructor(
        val predicate: (MediaDecoder) -> Boolean,
        val name: String,
        val parameters: List<String>
) : MediaEncodable {
    constructor(name: String, vararg parameters: Any?) : this({ true }, name, listOf(*parameters).map { it.toString() })

    constructor(predicate: (MediaDecoder) -> Boolean, name: String, vararg parameters: Any?) : this(predicate, name, listOf(*parameters).map { it.toString() })

    override val encodable: (MediaEncoder) -> Unit
        get() = {
            it.encodeMap {
                it.addList(name, { list ->
                    parameters.forEach { parameter ->
                        list.add(parameter.toString())
                    }
                })
            }
        }

    override fun toString() = "MediaRequirement(name=$name, parameters=[${parameters.joinToString()}])"

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MediaRequirement) {
            return false
        }
        return name == other.name && parameters == other.parameters
    }

    override fun hashCode() = Objects.hash(name, parameters)
}
