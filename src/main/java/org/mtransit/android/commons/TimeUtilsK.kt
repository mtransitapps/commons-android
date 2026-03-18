package org.mtransit.android.commons

import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

object TimeUtilsK {

    @JvmStatic
    fun formatSimpleDuration(durationInMs: Long) = buildString {
        val negative = durationInMs < 0
        abs(durationInMs).milliseconds.toComponents { days, hours, minutes, seconds, nanoseconds ->
            days.takeIf { it > 0 }?.let { append(it).append(" days ") }
            hours.takeIf { it > 0 }?.let { append(it).append(" h ") }
            minutes.takeIf { it > 0 }?.let { append(it).append(" min ") }
            seconds.takeIf { it > 0 }?.let { append(it).append(" sec ") }
            nanoseconds.takeIf { it > 0 }?.nanoseconds?.inWholeMilliseconds?.let { append(it).append(" ms ") }
        }
        if (negative) insert(0, "-")
    }.trim()

    fun currentInstant() = TimeUtils.currentTimeMillis().millisToInstant()

    val EPOCH_TIME_0: Instant = 0L.millisToInstant()
}

fun Duration.formatSimpleDuration() = this.inWholeMilliseconds.let { TimeUtilsK.formatSimpleDuration(it) }

fun Long.millisToInstant() = Instant.fromEpochMilliseconds(this)

fun Long.secsToInstant() = Instant.fromEpochSeconds(this)

@Suppress("unused")
fun Int.secsToInstant() = this.toLong().secsToInstant()

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
