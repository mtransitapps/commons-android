package org.mtransit.android.commons.data

import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.toMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

fun Instant.toScheduleTimestamp(localTimeZoneId: String, arrival: Instant? = null, tripId: String? = null, stopSequence: Int? = null) =
    Schedule.Timestamp(this.toMillis(), localTimeZoneId).apply {
        arrival?.let { this.arrival = it }
        tripId?.let { this.tripId = it }
        stopSequence?.let { this.setStopSequence(it) }
    }

var Schedule.Timestamp.departure: Instant
    get() = departureT.millisToInstant()
    set(value) {
        departureT = value.toMillis()
    }

@Suppress("unused")
val Schedule.Timestamp.arrivalDiff: Duration? get() = this.arrivalDiffMs?.milliseconds?.coerceAtLeast(Duration.ZERO)

var Schedule.Timestamp.arrival: Instant
    get() = arrivalT.millisToInstant()
    set(value) {
        arrivalT = value.toMillis()
    }
