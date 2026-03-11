package org.mtransit.android.commons.provider.status

import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.arrivalDiff
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optArrival
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDelay
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDeparture
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optScheduleRelationship
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopSequence
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopTimeUpdateList
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimeInstant
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.parseStopId
import org.mtransit.android.commons.provider.gtfs.parseTripId
import org.mtransit.android.commons.provider.status.GTFSRealTimeTripUpdatesProvider.LOG_TAG
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import com.google.transit.realtime.GtfsRealtime.TripDescriptor as GTripDescriptor
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship as GTDScheduleRelationship
import com.google.transit.realtime.GtfsRealtime.TripUpdate as GTripUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent as GTUStopTimeEvent
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate as GTUStopTimeUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship as GTUSTUScheduleRelationship

fun GTFSRealTimeProvider.processRDTripUpdates(
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
        val sortedTargetUuidAndSequence = makeTargetUuidAndSequenceList(tripId, tripTargetUuidSchedule, tripSortedRDS)
        processRDTripUpdate(
            tripId, gTripUpdate, tripSortedRDS, sortedTargetUuidAndSequence, tripTargetUuidSchedule,
            isSameStop = { stu, rds, stopSeq -> isSameStop(stu, rds, stopSeq) },
        )
    }
}

internal fun makeTargetUuidAndSequenceList(
    tripId: String,
    tripTargetUuidSchedule: Map<String, Schedule?>,
    tripSortedRDS: List<RouteDirectionStop>,
): List<Pair<String, Int>> {
    if (tripTargetUuidSchedule.values.any {
            it?.timestamps?.filter { timestamp -> timestamp.tripId == tripId }?.any { timestamp -> timestamp.stopSequenceOrNull == null } == true
        }) {
        // should not happen if FF is turned ON
        return tripSortedRDS
            .mapIndexed { index, rds ->
                rds.uuid to index + 1 // generated stop sequence
            }
            .sortedBy { (_, stopSequence) -> stopSequence }
    }
    var generatedStopSequence = 1
    return buildSet { // unicity of uuid+sequence
        tripTargetUuidSchedule.forEach { (targetUuid, schedule) ->
            schedule?.timestamps?.filter { it.tripId == tripId }?.forEach { timestamp ->
                val stopSequence = timestamp.stopSequenceOrNull ?: generatedStopSequence
                add(targetUuid to stopSequence)
                generatedStopSequence = stopSequence + 1 // next probable stop sequence
            }
        }
    }.sortedBy { (_, stopSequence) -> stopSequence }
}

internal fun processRDTripUpdate(
    tripId: String,
    gTripUpdate: GTripUpdate,
    tripSortedRDS: List<RouteDirectionStop>,
    sortedTargetUuidAndSequence: List<Pair<String, Int>>,
    tripTargetUuidSchedule: Map<String, Schedule?>,
    isSameStop: (GTUStopTimeUpdate?, RouteDirectionStop, Int) -> Boolean,
) {
    if (gTripUpdate.optTrip?.optScheduleRelationship == GTDScheduleRelationship.CANCELED
        || gTripUpdate.optTrip?.optScheduleRelationship == GTDScheduleRelationship.DELETED
    ) {
        tripTargetUuidSchedule.values.forEach { schedule ->
            schedule ?: return@forEach
            schedule.timestamps.filter { it.tripId == tripId }.forEach {
                schedule.removeTimestamp(it)
            }
        }
        return
    }
    if (gTripUpdate.optDelay == null && gTripUpdate.stopTimeUpdateCount == 0) {
        MTLog.d(LOG_TAG, "processRDTripUpdate() > SKIP (useless trip update: ${gTripUpdate.toStringExt()})")
        return // nothing to do
    }
    var stuIdx = 0
    var uuidAndSeqIdx = 0
    var currentDelay = gTripUpdate.optDelay?.seconds // initial delay valid until 1st stop time update
    val gStopTimeUpdates = gTripUpdate.optStopTimeUpdateList?.sortedBy { it.optStopSequence }
    var currentStopTimeUpdate: GTUStopTimeUpdate?
    var nextStopTimeUpdate: GTUStopTimeUpdate? = gStopTimeUpdates?.getOrNull(stuIdx)
    var currentUuidAndSeq = sortedTargetUuidAndSequence.getOrNull(uuidAndSeqIdx)
        ?: return // no more stop
    var currentRDS: RouteDirectionStop = tripSortedRDS.singleOrNull { it.uuid == currentUuidAndSeq.first }
        ?: return // stop not found!
    while (uuidAndSeqIdx <= sortedTargetUuidAndSequence.size) {
        while (!isSameStop(nextStopTimeUpdate, currentRDS, currentUuidAndSeq.second)
            && uuidAndSeqIdx <= sortedTargetUuidAndSequence.size // allow null currentRDS to signify end of trip
        ) {
            currentDelay = applyDelay(tripId, currentUuidAndSeq.second, tripTargetUuidSchedule[currentRDS.uuid], currentDelay)
            currentUuidAndSeq = sortedTargetUuidAndSequence.getOrNull(++uuidAndSeqIdx) ?: break // no more stop
            currentRDS = tripSortedRDS.singleOrNull { it.uuid == currentUuidAndSeq.first } ?: break // stop not found!
        }
        if (uuidAndSeqIdx >= sortedTargetUuidAndSequence.size) break // no more stop
        currentStopTimeUpdate = nextStopTimeUpdate ?: break // no more stop time update
        nextStopTimeUpdate = gStopTimeUpdates?.getOrNull(++stuIdx)
        currentDelay = applyDelaySTU(tripId, currentUuidAndSeq.second, tripTargetUuidSchedule[currentRDS.uuid], currentStopTimeUpdate, currentDelay)
        currentUuidAndSeq = sortedTargetUuidAndSequence.getOrNull(++uuidAndSeqIdx) ?: break // no more stop
        currentRDS = tripSortedRDS.singleOrNull { it.uuid == currentUuidAndSeq.first } ?: break // stop not found!
    }
}

private fun Schedule.findClosestTripTimestamp(tripId: String, stopSequence: Int) =
    timestamps.filter { it.tripId == tripId }
        .filter { timestamp ->
            (timestamp.stopSequenceOrNull == null || timestamp.stopSequenceOrNull == stopSequence)
        }.let { rdsTripTimestamps ->
            if (rdsTripTimestamps.size > 1) {
                val now = TimeUtilsK.currentInstant()
                rdsTripTimestamps.sortedBy { (it.departure - now).absoluteValue }
            } else {
                rdsTripTimestamps
            }.firstOrNull()
        }

internal fun applyDelaySTU(
    tripId: String,
    stopSequence: Int,
    rdsSchedule: Schedule?,
    gStopTimeUpdate: GTUStopTimeUpdate,
    currentDelay: Duration? = null,
): Duration? {
    val rdsTripTimestamp = rdsSchedule?.findClosestTripTimestamp(tripId, stopSequence)
        ?: return null // impossible to handle
    val timestampOriginalArrival = rdsTripTimestamp.arrival
    val timestampOriginalDeparture = rdsTripTimestamp.departure
    val timestampOriginalArrivalDiff = rdsTripTimestamp.arrivalDiff
    val stuArrivalTime = gStopTimeUpdate.optArrival
        .takeIf { gStopTimeUpdate.scheduleRelationship != GTUSTUScheduleRelationship.NO_DATA }
        ?.optTimeInstant
    val stuArrivalDelay = gStopTimeUpdate.optArrival
        .takeIf { gStopTimeUpdate.scheduleRelationship != GTUSTUScheduleRelationship.NO_DATA }
        .makeDelay(timestampOriginalArrival)
        ?: currentDelay
            .takeIf { gStopTimeUpdate.scheduleRelationship != GTUSTUScheduleRelationship.NO_DATA }
    val stuDepartureTime = gStopTimeUpdate.optDeparture
        .takeIf { gStopTimeUpdate.scheduleRelationship != GTUSTUScheduleRelationship.NO_DATA }
        ?.optTimeInstant
    val stuDepartureDelay = gStopTimeUpdate.optDeparture
        .takeIf { gStopTimeUpdate.scheduleRelationship != GTUSTUScheduleRelationship.NO_DATA }
        .makeDelay(timestampOriginalDeparture, stuArrivalDelay, timestampOriginalArrivalDiff)
    stuArrivalTime?.let { rdsTripTimestamp.arrival = it; rdsTripTimestamp.realTime = true }
        ?: stuArrivalDelay?.let { rdsTripTimestamp.arrival += it; rdsTripTimestamp.realTime = true }
    stuDepartureTime?.let { rdsTripTimestamp.departure = it; rdsTripTimestamp.realTime = true }
        ?: stuDepartureDelay?.let { rdsTripTimestamp.departure += it; rdsTripTimestamp.realTime = true }
    if (gStopTimeUpdate.scheduleRelationship == GTUSTUScheduleRelationship.SKIPPED) {
        rdsSchedule.removeTimestamp(rdsTripTimestamp)
    }
    return stuDepartureDelay
        .takeIf { gStopTimeUpdate.scheduleRelationship != GTUSTUScheduleRelationship.NO_DATA }
}

internal fun GTUStopTimeEvent?.makeDelay(
    originalTime: Instant,
    previousSTEDelay: Duration? = null,
    previousCurrentDiff: Duration? = null,
): Duration? {
    return this?.optDelay?.seconds
        ?: this?.optTimeInstant?.let { time -> time - originalTime }
        ?: previousSTEDelay?.let {
            previousCurrentDiff?.let {
                (previousSTEDelay - previousCurrentDiff).coerceAtLeast(Duration.ZERO)
            }
        }
}

internal fun applyDelay(
    tripId: String,
    stopSequence: Int,
    rdsSchedule: Schedule?,
    currentDelay: Duration?
): Duration? {
    currentDelay ?: return null
    val rdsTripTimestamp = rdsSchedule?.findClosestTripTimestamp(tripId, stopSequence)
        ?: return currentDelay
    val currentDiffBetweenArrivalAndDeparture = rdsTripTimestamp.arrivalDiff
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

private fun GTFSRealTimeProvider.isSameStop(stopTimeUpdate: GTUStopTimeUpdate?, rds: RouteDirectionStop?, stopSequence: Int) =
    stopTimeUpdate?.isSameStop(rds, stopSequence, this::parseStopId) == true

internal fun GTUStopTimeUpdate.isSameStop(
    rds: RouteDirectionStop?,
    stopSequence: Int,
    parseStopId: (String) -> String,
): Boolean {
    rds ?: return false
    val sameOrUnspecifiedStopSequence = this.optStopSequence
        ?.let { it == stopSequence }
    val sameOrUnspecifiedStopId = this.optStopId?.let {
        rds.stop.isSameOriginalId(parseStopId(it))
    }
    return sameOrUnspecifiedStopSequence == true || sameOrUnspecifiedStopId == true
}
