package se.ltu.emapal.compute.io

/**
 * Service status.
 */
enum class ComputeServiceStatus {
    /** Service is connected to some compute client. */
    CONNECTED,

    /** Service connection was disrupted due to some error. */
    DISRUPTED,

    /** The compute service has been terminated. */
    TERMINATED
}