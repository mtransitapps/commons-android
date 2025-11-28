package org.mtransit.android.commons

import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

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
