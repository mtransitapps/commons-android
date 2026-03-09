package org.mtransit.android.commons.provider.status

import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.arrivalDiff
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optArrival
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDelay
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDeparture
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
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship as GTUSTUScheduleRelationship

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
        wipTripUpdate(
            tripId, gTripUpdate, tripSortedRDS, tripTargetUuidSchedule,
            isSameStop = { stu, rds -> isSameStop(stu, rds) },
        )
    }
}

internal fun wipTripUpdate(
    tripId: String,
    gTripUpdate: GTripUpdate,
    tripSortedRDS: List<RouteDirectionStop>,
    tripTargetUuidSchedule: Map<String, Schedule?>,
    isSameStop: (GTUStopTimeUpdate?, RouteDirectionStop?) -> Boolean,
) {
    var currentDelay = gTripUpdate.optDelay?.seconds // initial delay valid until 1st stop time update
    val gStopTimeUpdates = gTripUpdate.optStopTimeUpdateList?.sortedBy { it.optStopSequence }

    var stuIdx = 0
    var currentStopTimeUpdate: GTUStopTimeUpdate? = null
    var nextStopTimeUpdate: GTUStopTimeUpdate? = gStopTimeUpdates?.getOrNull(stuIdx)
    var rdsTripTimestamp: Schedule.Timestamp? = null
    var rdsIdx = 0
    var currentRDS: RouteDirectionStop = tripSortedRDS.getOrNull(rdsIdx)
        ?: return // no more stop
    while (rdsIdx <= tripSortedRDS.size) {
        while (!isSameStop(nextStopTimeUpdate, currentRDS)
            && rdsIdx <= tripSortedRDS.size // allow null currentRDS to signify end of trip
        ) {
            rdsTripTimestamp = tripTargetUuidSchedule[currentRDS.uuid]?.timestamps?.singleOrNull { it.tripId == tripId }
            currentDelay = wipApplyDelay(rdsTripTimestamp, currentDelay)
            currentRDS = tripSortedRDS.getOrNull(++rdsIdx) ?: break
        }
        if (rdsIdx >= tripSortedRDS.size) break // no more stop
        currentStopTimeUpdate = nextStopTimeUpdate ?: break // no more stop time update
        nextStopTimeUpdate = gStopTimeUpdates?.getOrNull(++stuIdx)
        rdsTripTimestamp = tripTargetUuidSchedule[currentRDS.uuid]?.timestamps?.singleOrNull { it.tripId == tripId }
        currentDelay = wipApplyDelaySTU(rdsTripTimestamp, currentStopTimeUpdate, currentDelay)
        currentRDS = tripSortedRDS.getOrNull(++rdsIdx) ?: break
    }
}

internal fun wipApplyDelaySTU(
    rdsTripTimestamp: Schedule.Timestamp?,
    gStopTimeUpdate: GTUStopTimeUpdate,
    currentDelay: Duration? = null,
): Duration? {
    rdsTripTimestamp ?: return null // impossible to handle
    val timestampOriginalArrival = rdsTripTimestamp.arrival
    val timestampOriginalDeparture = rdsTripTimestamp.departure
    val timestampOriginalArrivalDiff = rdsTripTimestamp.arrivalDiff ?: Duration.ZERO
    val stuArrivalDelay = gStopTimeUpdate.optArrival
        .takeIf { gStopTimeUpdate.scheduleRelationship != GTUSTUScheduleRelationship.NO_DATA }
        .wipMakeDelay(timestampOriginalArrival)
        ?: currentDelay
            .takeIf { gStopTimeUpdate.scheduleRelationship != GTUSTUScheduleRelationship.NO_DATA }
    val stuDepartureDelay = gStopTimeUpdate.optDeparture
        .takeIf { gStopTimeUpdate.scheduleRelationship != GTUSTUScheduleRelationship.NO_DATA }
        .wipMakeDelay(timestampOriginalDeparture, stuArrivalDelay, timestampOriginalArrivalDiff)
    stuArrivalDelay?.let { rdsTripTimestamp.arrival += it; rdsTripTimestamp.realTime = true }
    stuDepartureDelay?.let { rdsTripTimestamp.departure += it; rdsTripTimestamp.realTime = true }
    return stuDepartureDelay
        .takeIf { gStopTimeUpdate.scheduleRelationship != GTUSTUScheduleRelationship.NO_DATA }
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
