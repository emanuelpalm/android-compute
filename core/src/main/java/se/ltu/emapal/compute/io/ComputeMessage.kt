package se.ltu.emapal.compute.io

/**
 * Represents a message that may be sent or received using a [ComputeChannel].
 *
 * @param version Message protocol version.
 * @param type Message type identifier.
 */
data class ComputeMessage(
        val version: Int,
        val type: Int
)