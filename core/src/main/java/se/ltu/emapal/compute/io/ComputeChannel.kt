package se.ltu.emapal.compute.io

import java.nio.channels.ByteChannel

/**
 * Manages communication between compute clients and services.
 *
 * @param byteChannel Channel used for communication.
 */
class ComputeChannel(
        private val byteChannel: ByteChannel
) {

}