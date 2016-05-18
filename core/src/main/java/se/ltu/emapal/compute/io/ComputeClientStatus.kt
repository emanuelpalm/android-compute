package se.ltu.emapal.compute.io

/**
 * Client status.
 */
enum class ComputeClientStatus {
    /** Client is currently connected to service. */
    CONNECTED,

    /** Client is currently attempting to connect to service. */
    CONNECTING,

    /** Client connection was disrupted due to some error. */
    DISRUPTED,

    /** Client was connected to a service, which terminated the connection. */
    TERMINATED
}