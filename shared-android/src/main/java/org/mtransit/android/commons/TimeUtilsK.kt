package org.mtransit.android.commons

import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

object TimeUtilsK {

    @JvmOverloads
    @JvmStatic
    fun formatSimpleDuration(durationInMs: Long, short: Boolean = true) = buildString {
        val negative = durationInMs < 0L
        abs(durationInMs).milliseconds.toComponents { days, hours, minutes, seconds, nanoseconds ->
            days.takeIf { it > 0L }?.let { append(it).append(if (short) "d " else " day ") }
            hours.takeIf { it > 0 }?.let { append(it).append(if (short) "h " else " hour ") }
            minutes.takeIf { it > 0 }?.let { append(it).append(if (short) "m " else " min ") }
            seconds.takeIf { it > 0 }?.let { append(it).append(if (short) "s " else " sec ") }
            nanoseconds.nanoseconds.inWholeMilliseconds.takeIf { it > 0 }?.let { append(it).append(if (short) "ms " else " milli ") }
        }
        if (negative) insert(0, "-")
        if (durationInMs == 0L) append("0")
    }.trim()

    @JvmStatic
    fun currentInstant() = TimeUtils.currentTimeMillis().millisToInstant()

    @JvmStatic
    val EPOCH_TIME_0: Instant = 0L.millisToInstant()
}

@Suppress("unused")
fun Duration.formatSimpleDuration(short: Boolean = true) = this.inWholeMilliseconds.let { TimeUtilsK.formatSimpleDuration(it, short) }

fun Long.millisToInstant() = Instant.fromEpochMilliseconds(this)

fun Long.secsToInstant() = Instant.fromEpochSeconds(this)

fun Instant.toMillis() = this.toEpochMilliseconds()

fun Instant.toSecs() = this.epochSeconds

fun Instant.roundToNearest(interval: Duration): Instant {
    val intervalMillis = interval.inWholeMilliseconds
        .takeUnless { it == 0L } ?: return this
    return ((this.toMillis() + (intervalMillis / 2L)) / intervalMillis * intervalMillis).millisToInstant()
}

fun Instant.floorBy(period: Duration, down: Boolean = true) =
    (this % period).let { rem ->
        when {
            rem == Duration.ZERO -> this
            down -> this - rem
            else -> this + (period - rem)
        }
    }

operator fun Instant.rem(period: Duration): Duration = (this.toMillis() % period.inWholeMilliseconds).milliseconds
