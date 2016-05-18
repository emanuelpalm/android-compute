package se.ltu.emapal.compute.util.time

import java.util.concurrent.TimeUnit

/**
 * A duration, which is a difference between two points in time.
 */
class Duration private constructor(private val milliseconds: Long) {
    /** Converts duration to a, possibly negative amount of milliseconds. */
    fun toMilliseconds() = milliseconds

    /** Converts duration to a, possibly negative, amount of seconds. */
    fun toSeconds() = milliseconds / 1000

    companion object {
        /** Creates new [Duration] from provided amount of milliseconds. */
        fun ofMilliseconds(milliseconds: Long) = Duration(milliseconds)

        /** Creates new [Duration] from provided amount of seconds. */
        fun ofSeconds(seconds: Long) = ofMilliseconds(seconds * 1000)

        /** Creates new [Duration] from provided [units], being of [timeUnit]. */
        fun of(units: Long, timeUnit: TimeUnit) = ofMilliseconds(TimeUnit.MILLISECONDS.convert(units, timeUnit))
    }
}