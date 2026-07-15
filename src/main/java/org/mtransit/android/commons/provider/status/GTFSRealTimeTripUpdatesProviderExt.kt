package org.mtransit.android.commons.provider.status

import androidx.annotation.VisibleForTesting
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.arrivalDiff
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.data.getTripTimestamps
import org.mtransit.android.commons.data.hasTripTimestamps
import org.mtransit.android.commons.data.providerPrecision
import org.mtransit.android.commons.data.setCancelled
import org.mtransit.android.commons.data.setDeleted
import org.mtransit.android.commons.data.setReadFromSourceAtInMsKeepMostRecent
import org.mtransit.android.commons.data.updateArrivalForRealTime
import org.mtransit.android.commons.data.updateDepartureForRealTime
import org.mtransit.android.commons.data.updateForRealTime
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optArrival
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDelay
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDelayDuration
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDeparture
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optScheduleRelationship
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopIdNotEmpty
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopSequence
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopTimeUpdateList
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimeInstant
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimestampMs
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripIdNotEmpty
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.parseStopId
import org.mtransit.android.commons.provider.gtfs.parseTripId
import org.mtransit.android.commons.provider.status.GTFSRealTimeTripUpdatesProvider.LOG_TAG
import org.mtransit.android.commons.provider.status.GTFSRealTimeTripUpdatesProvider.PROVIDER_PRECISION
import org.mtransit.android.toDateTimeLog
import org.mtransit.android.toDurationLog
import kotlin.time.Duration
import kotlin.time.Instant
import com.google.transit.realtime.GtfsRealtime.TripDescriptor as GTripDescriptor
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship as GTDScheduleRelationship
import com.google.transit.realtime.GtfsRealtime.TripUpdate as GTripUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent as GTUStopTimeEvent
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate as GTUStopTimeUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship as GTUSTUScheduleRelationship

fun GTFSRealTimeProvider.processRDTripUpdates(
    rdTripUpdates: List<Pair<GTripDescriptor, GTripUpdate>>,
    targetUuidSchedule: Map<String, Schedule>,
    sortedRDS: List<RouteDirectionStop>,
    feedReadFromSourceMs: Long,
    includeCancelledTimestamps: Boolean = false,
) {
    rdTripUpdates.forEach { (td, gTripUpdate) ->
        val gTripId = td.optTripIdNotEmpty ?: return@forEach
        val tripId = parseTripId(gTripId)
        val tripTargetUuidSchedule = targetUuidSchedule
            .filter { (_, schedule) -> schedule.hasTripTimestamps(tripId) }
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
            feedReadFromSourceMs = feedReadFromSourceMs,
            includeCancelledTimestamps = includeCancelledTimestamps,
        )
    }
}

internal fun makeTargetUuidAndSequenceList(
    tripId: String,
    tripTargetUuidSchedule: Map<String, Schedule>,
    tripSortedRDS: List<RouteDirectionStop>,
): List<Pair<String, Int>> {
    if (tripTargetUuidSchedule.values.any { schedule ->
            schedule.getTripTimestamps(tripId).any { timestamp -> timestamp.stopSequenceOrNull == null }
        }) {
        /** should not happen if FF is turned ON [org.mtransit.commons.FeatureFlags.F_EXPORT_STOP_SEQUENCE] */
        return tripSortedRDS
            .mapIndexed { index, rds ->
                rds.uuid to index + 1 // generated stop sequence
            }
            .sortedBy { (_, stopSequence) -> stopSequence }
    }
    var generatedStopSequence = 1
    return buildSet { // unicity of uuid+sequence
        tripTargetUuidSchedule.forEach { (targetUuid, schedule) ->
            schedule.getTripTimestamps(tripId).forEach { timestamp ->
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
    tripTargetUuidSchedule: Map<String, Schedule>,
    isSameStop: (GTUStopTimeUpdate?, RouteDirectionStop, Int) -> Boolean,
    feedReadFromSourceMs: Long,
    includeCancelledTimestamps: Boolean = false,
) {
    val gTripUpdateReadFromSourceMs = gTripUpdate.optTimestampMs ?: feedReadFromSourceMs
    if (gTripUpdate.optTrip?.optScheduleRelationship == GTDScheduleRelationship.DELETED) {
        tripTargetUuidSchedule.values.setDeleted(tripId, gTripUpdateReadFromSourceMs)
        return
    }
    if (gTripUpdate.optTrip?.optScheduleRelationship == GTDScheduleRelationship.CANCELED) {
        tripTargetUuidSchedule.values.setCancelled(tripId, includeCancelledTimestamps, gTripUpdateReadFromSourceMs)
        return
    }
    if (gTripUpdate.optDelay == null && gTripUpdate.stopTimeUpdateCount == 0) {
        MTLog.d(LOG_TAG, "processRDTripUpdate($tripId) > SKIP (useless trip update: ${gTripUpdate.toStringExt()})")
        return // nothing to do
    }
    var stuIdx = 0
    var uuidAndSeqIdx = 0
    var currentDelay = gTripUpdate.optDelayDuration // initial delay valid until 1st stop time update
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
            currentDelay = applyDelay(
                tripId = tripId,
                stopSequence = currentUuidAndSeq.second,
                rdsSchedule = tripTargetUuidSchedule[currentRDS.uuid],
                currentDelay = currentDelay,
                readFromSourceMs = gTripUpdateReadFromSourceMs
            )
            currentUuidAndSeq = sortedTargetUuidAndSequence.getOrNull(++uuidAndSeqIdx) ?: break // no more stop
            currentRDS = tripSortedRDS.singleOrNull { it.uuid == currentUuidAndSeq.first } ?: break // stop not found!
        }
        if (uuidAndSeqIdx >= sortedTargetUuidAndSequence.size) break // no more stop
        currentStopTimeUpdate = nextStopTimeUpdate ?: break // no more stop time update
        nextStopTimeUpdate = gStopTimeUpdates?.getOrNull(++stuIdx)
        currentDelay = applyDelaySTU(
            tripId = tripId,
            stopSequence = currentUuidAndSeq.second,
            rdsSchedule = tripTargetUuidSchedule[currentRDS.uuid],
            gStopTimeUpdate = currentStopTimeUpdate,
            readFromSourceMs = gTripUpdate.optTimestampMs ?: feedReadFromSourceMs,
            currentDelay = currentDelay,
            includeCancelledTimestamps = includeCancelledTimestamps,
        )
        currentUuidAndSeq = sortedTargetUuidAndSequence.getOrNull(++uuidAndSeqIdx) ?: break // no more stop
        currentRDS = tripSortedRDS.singleOrNull { it.uuid == currentUuidAndSeq.first } ?: break // stop not found!
    }
}

fun Iterable<Schedule.Timestamp>.findClosestTripTimestamp(tripId: String, filterStopSequence: Int? = null) =
    this.filter { it.tripId == tripId }
        .filter { timestamp ->
            timestamp.stopSequenceOrNull == null // should never happen -> FF: ON since March 2026
                    || filterStopSequence == null
                    || timestamp.stopSequenceOrNull == filterStopSequence
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
    readFromSourceMs: Long,
    currentDelay: Duration? = null,
    includeCancelledTimestamps: Boolean = false,
): Duration? {
    val rdsTripTimestamp = rdsSchedule?.timestamps?.findClosestTripTimestamp(tripId, stopSequence)
        ?: return null // impossible to handle
    val timestampOriginalArrival = rdsTripTimestamp.arrival
    val timestampOriginalDeparture = rdsTripTimestamp.departure
    val timestampOriginalArrivalDiff = rdsTripTimestamp.arrivalDiff
    var updated = false
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
    stuArrivalTime?.let { rdsTripTimestamp.updateArrivalForRealTime(newArrival = it); updated = true }
        ?: stuArrivalDelay?.let { rdsTripTimestamp.updateArrivalForRealTime(it, rdsSchedule.providerPrecision, PROVIDER_PRECISION); updated = true }
    stuDepartureTime?.let { rdsTripTimestamp.updateDepartureForRealTime(newDeparture = it); updated = true }
        ?: stuDepartureDelay?.let { rdsTripTimestamp.updateDepartureForRealTime(it, rdsSchedule.providerPrecision, PROVIDER_PRECISION); updated = true }
    if (gStopTimeUpdate.scheduleRelationship == GTUSTUScheduleRelationship.SKIPPED) {
        rdsSchedule.setCancelled(rdsTripTimestamp, includeCancelledTimestamps)
        updated = true
    }
    if (updated) rdsSchedule.setReadFromSourceAtInMsKeepMostRecent(readFromSourceMs)
    return stuDepartureDelay
        .takeIf { gStopTimeUpdate.scheduleRelationship != GTUSTUScheduleRelationship.NO_DATA }
}

internal fun GTUStopTimeEvent?.makeDelay(
    originalTime: Instant,
    previousSTEDelay: Duration? = null,
    previousCurrentDiff: Duration? = null,
): Duration? {
    return this?.optDelayDuration
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
    currentDelay: Duration?,
    readFromSourceMs: Long
): Duration? {
    currentDelay ?: return null
    val rdsTripTimestamp = rdsSchedule?.timestamps?.findClosestTripTimestamp(tripId, stopSequence)
        ?: return currentDelay
    val currentDiffBetweenArrivalAndDeparture = rdsTripTimestamp.arrivalDiff
    val newDelay = if (currentDelay < Duration.ZERO) {
        rdsTripTimestamp.updateForRealTime(delay = currentDelay, rdsSchedule.providerPrecision, PROVIDER_PRECISION)
        currentDelay // do not consume negative delay
    } else if (currentDiffBetweenArrivalAndDeparture <= currentDelay) {
        val newDelay = (currentDelay - currentDiffBetweenArrivalAndDeparture).coerceAtLeast(Duration.ZERO)
        rdsTripTimestamp.updateForRealTime(arrivalDelay = currentDelay, departureDelay = newDelay, rdsSchedule.providerPrecision, PROVIDER_PRECISION)
        newDelay
    } else {
        rdsTripTimestamp.updateArrivalForRealTime(currentDelay, rdsSchedule.providerPrecision, PROVIDER_PRECISION)
        Duration.ZERO // all delay consumed
    }
    rdsSchedule.setReadFromSourceAtInMsKeepMostRecent(readFromSourceMs)
    return newDelay
}

private fun GTFSRealTimeProvider.isSameStop(stopTimeUpdate: GTUStopTimeUpdate?, rds: RouteDirectionStop?, stopSequence: Int) =
    stopTimeUpdate?.isSameStop(rds, stopSequence, this::parseStopId) == true

@VisibleForTesting
internal fun GTUStopTimeUpdate.isSameStop(
    rds: RouteDirectionStop?,
    stopSequence: Int,
    parseStopId: (String) -> String,
): Boolean {
    rds ?: return false
    val sameOrUnspecifiedStopSequence = this.optStopSequence?.let {
        it == stopSequence
    }
    val sameOrUnspecifiedStopId = this.optStopIdNotEmpty?.let {
        rds.stop.isSameOriginalId(parseStopId(it))
    }
    if (sameOrUnspecifiedStopSequence == null && sameOrUnspecifiedStopId == null) return false
    return (sameOrUnspecifiedStopSequence ?: true) && (sameOrUnspecifiedStopId ?: true)
}
