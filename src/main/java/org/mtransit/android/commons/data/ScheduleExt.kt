package org.mtransit.android.commons.data

import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.floorBy
import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.roundToNearest
import org.mtransit.android.commons.toMillis
import org.mtransit.android.toDateTimeLog
import org.mtransit.android.toDurationLog
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

fun makeSchedule(
    id: Int? = null,
    targetUUID: String,
    lastUpdateInMs: Long,
    validityInMs: Long,
    readFromSourceAtInMs: Long,
    providerPrecisionInMs: Long,
    isNoPickup: Boolean = false,
    sourceLabel: String? = null,
    noData: Boolean = false
) = Schedule(
    id,
    targetUUID,
    lastUpdateInMs,
    validityInMs,
    readFromSourceAtInMs,
    providerPrecisionInMs,
    isNoPickup,
    sourceLabel,
    noData,
)

fun RouteDirectionStop.makeSchedule(
    lastUpdateInMs: Long,
    validityInMs: Long,
    readFromSourceAtInMs: Long,
    providerPrecisionInMs: Long,
    sourceLabel: String,
    noData: Boolean,
) = makeSchedule(
    targetUUID = uuid,
    lastUpdateInMs = lastUpdateInMs,
    validityInMs = validityInMs,
    readFromSourceAtInMs = readFromSourceAtInMs,
    providerPrecisionInMs = providerPrecisionInMs,
    sourceLabel = sourceLabel,
    noData = noData
).apply {
    isNoPickup = this@makeSchedule.isNoPickup
}

fun Schedule.toNoData() = makeSchedule(
    id = id,
    targetUUID = targetUUID,
    lastUpdateInMs = lastUpdateInMs,
    validityInMs = validityInMs,
    readFromSourceAtInMs = readFromSourceAtInMs,
    providerPrecisionInMs = providerPrecisionInMs,
    isNoPickup = isNoPickup,
    sourceLabel = sourceLabel,
    noData = true // NO DATA
)

val Schedule.providerPrecision get() = providerPrecisionInMs.milliseconds

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

val Schedule.Timestamp.maxDate get() = maxOf(originalDeparture, departure, originalArrival, arrival)
val Schedule.Timestamp.minDate get() = minOf(originalDeparture, departure, originalArrival, arrival)

/**
 * It's better to be early at the stop, than late and miss the vehicle departure -> truncate (floor by) to early w/ precision
 */
fun Schedule.Timestamp.updateDepartureForRealTime(departureDelay: Duration, currentPrecision: Duration, delayPrecision: Duration) {
    val maxPrecision = currentPrecision.coerceAtLeast(delayPrecision)
    updateDepartureForRealTime(computeInstant(departure, departureDelay, maxPrecision))
}

fun Schedule.Timestamp.updateDepartureForRealTime(newDeparture: Instant) {
    val departureDelay = newDeparture - originalDeparture
    originalDepartureDelay = departureDelay
    departureT = newDeparture.toMillis()
    realTime = true
}

fun Schedule.Timestamp.updateForRealTime(delay: Duration, currentPrecision: Duration, delayPrecision: Duration) =
    updateForRealTime(arrivalDelay = delay, departureDelay = delay, currentPrecision = currentPrecision, delayPrecision = delayPrecision)

fun Schedule.Timestamp.updateForRealTime(arrivalDelay: Duration?, departureDelay: Duration, currentPrecision: Duration, delayPrecision: Duration) {
    updateDepartureForRealTime(departureDelay, currentPrecision, delayPrecision)
    arrivalDelay?.let { updateArrivalForRealTime(it, currentPrecision, delayPrecision) }
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

private fun computeInstant(
    initialInstant: Instant,
    delay: Duration,
    precision: Duration,
    canRoundToNearest: Boolean = false,
    canRoundUp: Boolean = false
): Instant {
    val newInstant = initialInstant + delay
    val roundedNewInstant = if (canRoundToNearest && delay.absoluteValue > precision.div(2)) {
        newInstant.roundToNearest(precision)
    } else {
        newInstant.floorBy(precision, down = !canRoundUp || delay.isPositive())
    }
    return roundedNewInstant
}

/**
 * Arrival is almost never shown in UI, has to be before departure -> same rule as departure
 */
fun Schedule.Timestamp.updateArrivalForRealTime(arrivalDelay: Duration, currentPrecision: Duration, delayPrecision: Duration) {
    val maxPrecision = currentPrecision.coerceAtLeast(delayPrecision)
    updateArrivalForRealTime(computeInstant(arrival, arrivalDelay, maxPrecision))
}

fun Schedule.Timestamp.updateArrivalForRealTime(newArrival: Instant) {
    val arrivalDelay = newArrival - originalArrival
    originalArrivalDelay = arrivalDelay
    arrivalT = newArrival.toMillis()
    realTime = true
}

@Suppress("unused")
val Schedule.hasRealTime get() = this.timestamps.any { it.isRealTime }

fun Schedule.toStringK() = buildString {
    append("S{")
    append("uuid:").append(targetUUID)
    append(",")
    append("proPre:").append(providerPrecisionInMs.toDurationLog())
    append(",")
    append("useUtl:").append(usefulUntilInMs.toDateTimeLog())
    append(",")
    if (isNoPickup) {
        append("noPickup")
        append(",")
    }
    timestamps.takeIf { it.isNotEmpty() }?.let { timestamps ->
        append("t[").append(timestamps.size).append("]")
        if (Constants.DEBUG) {
            append(timestamps.joinToString(separator = ",", prefix = ":{", postfix = "}") { it.toStringShort() })
        }
        append(",")
    }
    frequencies.takeIf { it.isNotEmpty() }?.let { frequencies ->
        append("f[").append(frequencies.size).append("]")
        if (Constants.DEBUG) {
            append(frequencies.joinToString(separator = ",", prefix = ":{", postfix = "}") { it.toString() })
        }
        append(",")
    }
    append("}")
}

@Suppress("unused")
fun Schedule.Timestamp.toStringShort() = buildString {
    append("T{")
    arrivalTIfDifferent?.let {
        append("a=").append(if (Constants.DEBUG) arrivalT.toDateTimeLog() else arrivalT)
        if (originalArrivalDelayMs != 0L) {
            append("[+/-:").append(if (Constants.DEBUG) originalArrivalDelayMs.toDurationLog() else originalArrivalDelayMs).append("]")
        }
        append(",")
    }
    append("d=").append(if (Constants.DEBUG) departureT.toDateTimeLog() else departureT)
    if (originalDepartureDelayMs != 0L) {
        append("[+/-:").append(if (Constants.DEBUG) originalDepartureDelayMs.toDurationLog() else originalDepartureDelayMs).append("]")
    }
    if (tripId != null) {
        append("[tId:").append(tripId).append("]")
    }
    if (isRealTime) {
        append("[RT]")
    }
    if (isOldSchedule) {
        append("[OLD]")
    }
    append("}")
}
