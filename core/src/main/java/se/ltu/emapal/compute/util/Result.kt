package se.ltu.emapal.compute.util

/**
 * Data type used for error handling.
 *
 * In places where it is expected that functions fail frequently, it may be inappropriate to use
 * regular exceptions, as these are easy to ignore and are expensive in terms of performance. The
 * [Result] type is constructed either as a success, wrapping an arbitrary value, or a failure,
 * wrapping an arbitrary error, and can later be destructed using a when expression.
 */
@Suppress("unused")
sealed class Result<out T, out E> {
    class Success<T, E>(val value: T) : Result<T, E>()
    class Failure<T, E>(val error: E) : Result<T, E>()
}