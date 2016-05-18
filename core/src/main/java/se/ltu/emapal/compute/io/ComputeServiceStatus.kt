package se.ltu.emapal.compute.io

/**
 * Service status.
 */
enum class ComputeServiceStatus {
    /** Service is connected to some compute client. */
    CONNECTED,

    /** The compute service has been terminated. */
    TERMINATED
}