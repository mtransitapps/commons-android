package org.mtransit.android.commons.provider.status

import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDelay
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopSequence
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopTimeUpdateList
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripId
import org.mtransit.android.commons.provider.gtfs.parseStopId
import org.mtransit.android.commons.provider.gtfs.parseTripId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import com.google.transit.realtime.GtfsRealtime.TripDescriptor as GTripDescriptor
import com.google.transit.realtime.GtfsRealtime.TripUpdate as GTripUpdate
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

    var rdsIdx = 0
    var currentRDS: RouteDirectionStop = tripSortedRDS.getOrNull(rdsIdx)
        ?: return // no more stop
    // ### Iterate on initial stops before 1st stop time update
    while (!isSameStop(currentStopTimeUpdate, currentRDS)
        && rdsIdx <= tripSortedRDS.size // allow null currentRDS to signify end of trip
    ) {
        val rdsTripTimestamp =
            tripTargetUuidSchedule[currentRDS.uuid]?.timestamps?.singleOrNull { it.tripId == tripId }
        currentDelay = wipApplyDelay(rdsTripTimestamp, currentDelay)
        currentRDS = tripSortedRDS.getOrNull(++rdsIdx) ?: break
    }
    currentRDS ?: return // no more stop
    // ### use stop time update
    TODO()
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
