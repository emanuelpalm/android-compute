package se.ltu.emapal.compute

/**
 * Signifies a failure to perform some compute action.
 */
class ComputeError(val code: Int, message: String) : RuntimeException(message)