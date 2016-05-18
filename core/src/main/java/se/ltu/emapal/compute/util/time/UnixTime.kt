package se.ltu.emapal.compute.util.time

import java.util.*

/**
 * Keeps track of time as an amount of milliseconds since the Unix 1970 epoch.
 *
 * This class represents the preferred way to store all times that are not directly presented to
 * human users, or where conditions make its use nonviable.
 */
class UnixTime private constructor(private val milliseconds: Long) {
    /** Converts time into [Calendar] object. */
    fun toCalendar() = Calendar.getInstance().apply {
        this.timeZone = TimeZone.getTimeZone("GMT")
        this.timeInMillis = toMilliseconds()
    }

    /** Converts time into [Date] object. */
    fun toDate() = Date(toMilliseconds())

    /** Converts time into the amount of milliseconds since the Unix 1970 epoch. */
    fun toMilliseconds() = milliseconds

    /** Creates new time of provided duration added to this time. */
    operator fun plus(duration: Duration) = ofMilliseconds(toMilliseconds() + duration.toMilliseconds())

    /** Removes provided time from this time and returns duration, which may be negative. */
    operator fun minus(time: UnixTime) = Duration.ofMilliseconds(toMilliseconds() - time.toMilliseconds())

    override fun equals(other: Any?): Boolean{
        if (this === other) {
            return true
        }
        if (other?.javaClass != javaClass) {
            return false
        }
        other as UnixTime
        return milliseconds == other.milliseconds
    }

    override fun hashCode(): Int{
        return milliseconds.hashCode()
    }

    override fun toString(): String = toDate().toString()

    companion object {
        /** Gets current time. */
        fun now() = ofMilliseconds(System.currentTimeMillis())

        /** Creates new unix time object from given amount of milliseconds. */
        fun ofMilliseconds(milliseconds: Long) = UnixTime(milliseconds)
    }
}