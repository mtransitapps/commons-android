package org.mtransit.android.commons.data

import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.toMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

fun Instant.toScheduleTimestamp(localTimeZoneId: String, arrival: Instant? = null, tripId: String? = null, stopSequence: Int? = null) =
    Schedule.Timestamp(this.toMillis(), localTimeZoneId).apply {
        arrival?.let { this.arrivalT = it.toMillis() }
        tripId?.let { this.tripId = it }
        stopSequence?.let { this.setStopSequence(it) }
    }

fun Schedule.Timestamp.isDepartureLate(minDelay: Duration = 30.seconds) =
    originalDepartureDelay > minDelay

fun Schedule.Timestamp.isDepartureEarly(maxDelay: Duration = 30.seconds) =
    originalDepartureDelay < -maxDelay

fun Schedule.Timestamp.isArrivalLate(minDelay: Duration = 30.seconds) =
    originalArrivalDelay > minDelay

fun Schedule.Timestamp.isArrivalEarly(maxDelay: Duration = 30.seconds) =
    originalArrivalDelay < -maxDelay

val Schedule.Timestamp.departure get() = departureT.millisToInstant()

var Schedule.Timestamp.originalDepartureDelay: Duration
    get() = originalDepartureDelayMs.milliseconds
    set(value) {
        originalDepartureDelayMs = value.inWholeMilliseconds
    }

val Schedule.Timestamp.originalDeparture get() = departure - originalDepartureDelay

fun Schedule.Timestamp.updateDepartureForRealTime(departureDelay: Duration) = updateDepartureForRealTime(departure + departureDelay)

fun Schedule.Timestamp.updateDepartureForRealTime(newDeparture: Instant) {
    val departureDelay = newDeparture - departure
    originalDepartureDelay = departureDelay
    departureT = newDeparture.toMillis()
    realTime = true
}

fun Schedule.Timestamp.updateForRealTime(arrivalDelay: Duration?, departureDelay: Duration) {
    updateDepartureForRealTime(departureDelay)
    arrivalDelay?.let { updateArrivalForRealTime(it) }
}

fun Schedule.Timestamp.updateForRealTime(newArrival: Instant?, newDeparture: Instant) {
    updateDepartureForRealTime(newDeparture)
    newArrival?.let { updateArrivalForRealTime(it) }
}

val Schedule.Timestamp.arrivalDiff get() = this.arrivalDiffMs?.milliseconds?.coerceAtLeast(Duration.ZERO) ?: Duration.ZERO

val Schedule.Timestamp.arrival get() = arrivalT.millisToInstant()

var Schedule.Timestamp.originalArrivalDelay: Duration
    get() = originalArrivalDelayMs.milliseconds
    set(value) {
        originalArrivalDelayMs = value.inWholeMilliseconds
    }

val Schedule.Timestamp.originalArrival get() = arrival - originalArrivalDelay

fun Schedule.Timestamp.updateArrivalForRealTime(arrivalDelay: Duration) = updateArrivalForRealTime(arrival + arrivalDelay)

fun Schedule.Timestamp.updateArrivalForRealTime(newArrival: Instant) {
    val arrivalDelay = newArrival - arrival
    originalArrivalDelay = arrivalDelay
    arrivalT = newArrival.toMillis()
    realTime = true
}
