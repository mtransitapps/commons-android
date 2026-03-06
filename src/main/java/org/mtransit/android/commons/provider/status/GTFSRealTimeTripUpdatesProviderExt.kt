package org.mtransit.android.commons.provider.status

import com.google.transit.realtime.arrivalOrNull
import com.google.transit.realtime.departureOrNull
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.arrivalDiff
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDelay
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopSequence
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopTimeUpdateList
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimeInstant
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripId
import org.mtransit.android.commons.provider.gtfs.parseStopId
import org.mtransit.android.commons.provider.gtfs.parseTripId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import com.google.transit.realtime.GtfsRealtime.TripDescriptor as GTripDescriptor
import com.google.transit.realtime.GtfsRealtime.TripUpdate as GTripUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent as GTUStopTimeEvent
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate as GTUStopTimeUpdate


fun GTFSRealTimeProvider.wip(
    rdTripUpdates: List<Pair<GTripDescriptor, GTripUpdate>>,
    targetUuidSchedule: Map<String, Schedule?>,
    sortedRDS: List<RouteDirectionStop>
) {
    rdTripUpdates.forEach { (td, gTripUpdate) ->
        val gTripId = td.optTripId ?: return@forEach
        val tripId = parseTripId(gTripId)
        val tripTargetUuidSchedule = targetUuidSchedule
            .filter { (_, schedule) -> schedule?.timestamps?.any { it.tripId == tripId } == true }
            .takeIf { it.isNotEmpty() }
            ?: return@forEach
        val tripSortedRDS = sortedRDS
            .filter { rds -> tripTargetUuidSchedule.contains(rds.uuid) }
            .takeIf { it.isNotEmpty() }
            ?: return@forEach
        wipTripUpdate(tripId, gTripUpdate, tripSortedRDS, tripTargetUuidSchedule)
    }
}

private fun GTFSRealTimeProvider.wipTripUpdate(
    tripId: String,
    gTripUpdate: GTripUpdate,
    tripSortedRDS: List<RouteDirectionStop>,
    tripTargetUuidSchedule: Map<String, Schedule?>
) {
    var currentDelay = gTripUpdate.optDelay?.seconds // initial delay valid until 1st stop time update
    val gStopTimeUpdates = gTripUpdate.optStopTimeUpdateList?.sortedBy { it.optStopSequence }

    var stuIdx = 0
    var currentStopTimeUpdate: GTUStopTimeUpdate? = gStopTimeUpdates?.getOrNull(stuIdx)
    var nextStopTimeUpdate: GTUStopTimeUpdate? = gStopTimeUpdates?.getOrNull(stuIdx + 1)
    var rdsTripTimestamp: Schedule.Timestamp? = null
    var rdsIdx = 0
    var currentRDS: RouteDirectionStop = tripSortedRDS.getOrNull(rdsIdx)
        ?: return // no more stop
    // ### Iterate on initial stops before 1st stop time update
    while (!isSameStop(currentStopTimeUpdate, currentRDS)
        && rdsIdx <= tripSortedRDS.size // allow null currentRDS to signify end of trip
    ) {
        rdsTripTimestamp = tripTargetUuidSchedule[currentRDS.uuid]?.timestamps?.singleOrNull { it.tripId == tripId }
        currentDelay = wipApplyDelay(rdsTripTimestamp, currentDelay)
        currentRDS = tripSortedRDS.getOrNull(++rdsIdx) ?: break
    }
    if (rdsIdx >= tripSortedRDS.size) return // no more stop
    currentStopTimeUpdate ?: return // no more stop time update
    // ### use stop time update
    rdsTripTimestamp = tripTargetUuidSchedule[currentRDS.uuid]?.timestamps?.singleOrNull { it.tripId == tripId }
    currentDelay = wipApplyDelaySTU(rdsTripTimestamp, currentStopTimeUpdate, currentDelay)
    TODO()
}

internal fun wipApplyDelaySTU(
    rdsTripTimestamp: Schedule.Timestamp?,
    currentStopTimeUpdate: GTUStopTimeUpdate,
    currentDelay: Duration? = null,
): Duration? {
    rdsTripTimestamp ?: return null // impossible to handle
    val timestampOriginalArrival = rdsTripTimestamp.arrival
    val timestampOriginalDeparture = rdsTripTimestamp.departure
    val timestampOriginalArrivalDiff = rdsTripTimestamp.arrivalDiff ?: Duration.ZERO
    val stuArrivalDelay = currentStopTimeUpdate.arrivalOrNull
        .wipMakeDelay(timestampOriginalArrival)
        ?: currentDelay
    val stuDepartureDelay = currentStopTimeUpdate.departureOrNull
        .wipMakeDelay(timestampOriginalDeparture, stuArrivalDelay, timestampOriginalArrivalDiff)
    stuArrivalDelay?.let { rdsTripTimestamp.arrival += it; rdsTripTimestamp.realTime = true }
    stuDepartureDelay?.let { rdsTripTimestamp.departure += it; rdsTripTimestamp.realTime = true }
    return stuDepartureDelay
}

internal fun GTUStopTimeEvent?.wipMakeDelay(
    originalTime: Instant,
    previousDelay: Duration? = null,
    previousOriginalDiff: Duration? = null,
): Duration? {
    return this?.optDelay?.seconds
        ?: this?.optTimeInstant?.let { time -> time - originalTime }
        ?: previousDelay?.let {
            previousOriginalDiff?.let {
                (previousDelay - previousOriginalDiff).coerceAtLeast(Duration.ZERO)
            }
        }
}

internal fun wipApplyDelay(
    rdsTripTimestamp: Schedule.Timestamp?,
    currentDelay: Duration?
): Duration? {
    currentDelay ?: return null
    rdsTripTimestamp ?: return currentDelay
    val currentDiffBetweenArrivalAndDeparture = rdsTripTimestamp.departure - rdsTripTimestamp.arrival
    if (currentDelay < Duration.ZERO) {
        rdsTripTimestamp.arrival += currentDelay
        rdsTripTimestamp.departure += currentDelay
        rdsTripTimestamp.realTime = true
        return currentDelay // do not consume negative delay
    } else if (currentDiffBetweenArrivalAndDeparture <= currentDelay) {
        rdsTripTimestamp.arrival += currentDelay
        val newDelay = (currentDelay - currentDiffBetweenArrivalAndDeparture).coerceAtLeast(Duration.ZERO)
        rdsTripTimestamp.departure += newDelay
        rdsTripTimestamp.realTime = true
        return newDelay
    } else {
        rdsTripTimestamp.arrival += currentDelay
        rdsTripTimestamp.realTime = true
        return Duration.ZERO // all delay consumed
    }
}


private fun GTFSRealTimeProvider.isSameStop(
    stopTimeUpdate: GTUStopTimeUpdate?,
    rds: RouteDirectionStop?,
    @Suppress("unused") currentStopSequence: Int? = null,
): Boolean {
    stopTimeUpdate ?: return false
    rds ?: return false
    // TODO check stop sequence as well?
    // TODO what about stop present multiple times in same trip?
    return rds.stop.isSameOriginalId(parseStopId(stopTimeUpdate))
}
