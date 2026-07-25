package org.mtransit.android.commons.provider.status

import androidx.annotation.VisibleForTesting
import com.google.transit.realtime.copy
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.arrivalDiff
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.data.getTripTimestamps
import org.mtransit.android.commons.data.providerPrecision
import org.mtransit.android.commons.data.setTripCancelled
import org.mtransit.android.commons.data.setStopTimeCancelled
import org.mtransit.android.commons.data.setTripDeleted
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
    rdSchedules: Collection<Schedule>,
    sortedRDS: List<RouteDirectionStop>,
    feedReadFromSourceMs: Long,
    includeCancelledTimestamps: Boolean = false,
) {
    val rdSchedulesByUUID = rdSchedules.associateBy { it.targetUUID }
    val rdsUUIDsByTripId = rdSchedules
        .flatMap { schedule ->
            schedule.timestamps
                .mapNotNull { it.tripId }
                .map { tripId -> tripId to schedule.targetUUID }
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, uuids) -> uuids.toSet() }
    rdTripUpdates.forEach { (td, gTripUpdate) ->
        val gTripId = td.optTripIdNotEmpty ?: return@forEach
        val tripId = parseTripId(gTripId)
        val tripRdsUUIDs = rdsUUIDsByTripId[tripId]
            ?: return@forEach // trip ID not in static data
        val tripSchedules = tripRdsUUIDs.mapNotNull { rdSchedulesByUUID[it] }
        val tripSortedRDS = sortedRDS
            .filter { rds -> rds.uuid in tripRdsUUIDs }
            .takeIf { it.isNotEmpty() }
            ?: return@forEach
        val sortedTargetUuidAndSequence = makeTargetUuidAndSequenceList(tripId, tripSchedules, tripSortedRDS)
        processRDTripUpdate(
            tripId, gTripUpdate, tripSortedRDS, sortedTargetUuidAndSequence, rdSchedulesByUUID,
            isSameStop = this::isSameStop,
            feedReadFromSourceMs = feedReadFromSourceMs,
            fixStopSequence = {
                it?.fixStopSequence(
                    tripId = tripId,
                    tripSortedRDS = tripSortedRDS,
                    sortedTargetUuidAndSequence = sortedTargetUuidAndSequence,
                    isSameStop = this::isSameStop,
                    parseStopId = this::parseStopId,
                )
            },
            includeCancelledTimestamps = includeCancelledTimestamps,
        )
    }
}

internal fun makeTargetUuidAndSequenceList(
    tripId: String,
    tripSchedules: Collection<Schedule>,
    tripSortedRDS: List<RouteDirectionStop>,
): List<Pair<String, Int>> {
    if (tripSchedules.any { schedule -> schedule.timestamps.any { it.tripId == tripId && it.stopSequenceOrNull == null } }) {
        /** should not happen if FF is turned ON [org.mtransit.commons.FeatureFlags.F_EXPORT_STOP_SEQUENCE] */
        return tripSortedRDS
            .mapIndexed { index, rds ->
                rds.uuid to index + 1 // generated stop sequence
            }
            .sortedBy { (_, stopSequence) -> stopSequence }
    }
    return buildSet { // unicity of uuid+sequence
        tripSchedules.forEach { schedule ->
            schedule.getTripTimestamps(tripId).forEach { timestamp ->
                timestamp.stopSequenceOrNull?.let { stopSequence ->
                    add(schedule.targetUUID to stopSequence)
                }
            }
        }
    }.sortedBy { (_, stopSequence) -> stopSequence }
}

@VisibleForTesting
internal fun processRDTripUpdate(
    tripId: String,
    gTripUpdate: GTripUpdate,
    tripSortedRDS: List<RouteDirectionStop>,
    sortedTargetUuidAndSequence: List<Pair<String, Int>>,
    rdSchedulesByUUID: Map<String, Schedule>,
    isSameStop: (GTUStopTimeUpdate?, RouteDirectionStop, Int) -> Boolean,
    feedReadFromSourceMs: Long,
    fixStopSequence: (List<GTUStopTimeUpdate>?) -> List<GTUStopTimeUpdate>? = { it },
    includeCancelledTimestamps: Boolean = false,
) {
    val gTripUpdateReadFromSourceMs = gTripUpdate.optTimestampMs ?: feedReadFromSourceMs
    if (gTripUpdate.optTrip?.optScheduleRelationship == GTDScheduleRelationship.DELETED) {
        rdSchedulesByUUID.values.setTripDeleted(tripId, gTripUpdateReadFromSourceMs)
        return
    }
    if (gTripUpdate.optTrip?.optScheduleRelationship == GTDScheduleRelationship.CANCELED) {
        rdSchedulesByUUID.values.setTripCancelled(tripId, includeCancelledTimestamps, gTripUpdateReadFromSourceMs)
        return
    }
    if (gTripUpdate.optDelay == null && gTripUpdate.stopTimeUpdateCount == 0) {
        MTLog.d(LOG_TAG, "processRDTripUpdate($tripId) > SKIP (useless trip update: ${gTripUpdate.toStringExt()})")
        return // nothing to do
    }
    var stuIdx = 0
    var uuidAndSeqIdx = 0
    var currentDelay = gTripUpdate.optDelayDuration // initial delay valid until 1st stop time update
    val gStopTimeUpdates = fixStopSequence(gTripUpdate.optStopTimeUpdateList)
        ?.sortedBy { it.optStopSequence }
    var currentStopTimeUpdate: GTUStopTimeUpdate?
    var nextStopTimeUpdate = gStopTimeUpdates?.getOrNull(stuIdx)
    var currentUuidAndSeq = sortedTargetUuidAndSequence.getOrNull(uuidAndSeqIdx)
        ?: return // no more stop
    var currentRDS = tripSortedRDS.firstOrNull { it.uuid == currentUuidAndSeq.uuid }
        ?: return // stop not found!
    while (uuidAndSeqIdx <= sortedTargetUuidAndSequence.size) {
        while (!isSameStop(nextStopTimeUpdate, currentRDS, currentUuidAndSeq.stopSequence)
            && uuidAndSeqIdx <= sortedTargetUuidAndSequence.size // allow null currentRDS to signify end of trip
        ) {
            currentDelay = applyDelay(
                tripId = tripId,
                stopSequence = currentUuidAndSeq.stopSequence,
                rdsSchedule = rdSchedulesByUUID[currentRDS.uuid],
                currentDelay = currentDelay,
                readFromSourceMs = gTripUpdateReadFromSourceMs
            )
            currentUuidAndSeq = sortedTargetUuidAndSequence.getOrNull(++uuidAndSeqIdx) ?: return // no more stop
            currentRDS = tripSortedRDS.firstOrNull { it.uuid == currentUuidAndSeq.uuid } ?: return // stop not found!
        }
        if (uuidAndSeqIdx >= sortedTargetUuidAndSequence.size) return // no more stop
        currentStopTimeUpdate = nextStopTimeUpdate ?: return // no more stop time update
        nextStopTimeUpdate = gStopTimeUpdates?.getOrNull(++stuIdx)
        currentDelay = applyDelaySTU(
            tripId = tripId,
            stopSequence = currentUuidAndSeq.stopSequence,
            rdsSchedule = rdSchedulesByUUID[currentRDS.uuid],
            gStopTimeUpdate = currentStopTimeUpdate,
            readFromSourceMs = gTripUpdateReadFromSourceMs,
            currentDelay = currentDelay,
            includeCancelledTimestamps = includeCancelledTimestamps,
        )
        currentUuidAndSeq = sortedTargetUuidAndSequence.getOrNull(++uuidAndSeqIdx) ?: break // no more stop
        currentRDS = tripSortedRDS.firstOrNull { it.uuid == currentUuidAndSeq.uuid } ?: break // stop not found!
    }
}

@VisibleForTesting
internal fun List<GTUStopTimeUpdate>.fixStopSequence(
    tripId: String,
    tripSortedRDS: List<RouteDirectionStop>,
    sortedTargetUuidAndSequence: List<Pair<String, Int>>,
    isSameStop: (GTUStopTimeUpdate?, RouteDirectionStop, Int) -> Boolean,
    parseStopId: (String) -> String,
): List<GTUStopTimeUpdate> {
    return this.mapNotNull { stu ->
        val match = tripSortedRDS.any { rds ->
            sortedTargetUuidAndSequence
                .any { (uuid, staticStopSeq) ->
                    uuid == rds.uuid && isSameStop(stu, rds, staticStopSeq)
                }
        }
        if (match) return@mapNotNull stu // keep valid STU
        val wrongStopSeq = stu.optStopSequence
        stu.optStopIdNotEmpty?.let { wrongStuStopId ->
            val parsedWrongStopId = parseStopId(wrongStuStopId)
            val rdsUuid = tripSortedRDS.firstOrNull { it.stop.isSameOriginalId(parsedWrongStopId) }?.uuid ?: return@let
            sortedTargetUuidAndSequence.singleOrNull { it.uuid == rdsUuid } // stop passed only a SINGLE time per trip
                ?.let { (_, stopSeq) ->
                    return@mapNotNull stu.copy { // use STU w fixed stop sequence
                        stopSequence = stopSeq
                    }
                        .also {
                            MTLog.d(LOG_TAG, "fixStopSequence($tripId) > KEEP fixed wrong stop sequence (!$wrongStopSeq): ${it.toStringExt(short = true)}")
                        }
                }
        }
        MTLog.w(LOG_TAG, "fixStopSequence($tripId) > IGNORE (no stop ID/sequence match): ${stu.toStringExt()}")
        return@mapNotNull null // remove invalid STU
    }
}

private val Pair<String, Int>.uuid get() = this.first
private val Pair<String, Int>.stopSequence get() = this.second

// TODO use `trip` descriptor `start_date` & `start_time` to compare with original departure Date/Time
fun Iterable<Schedule.Timestamp>.findClosestTripTimestamp(tripId: String, filterStopSequence: Int? = null) =
    filter { timestamp ->
        timestamp.tripId == tripId
                && timestamp.stopSequenceMatch(filterStopSequence)
    }.let { rdsTripTimestamps ->
        if (rdsTripTimestamps.size > 1) {
            val now = TimeUtilsK.currentInstant()
            rdsTripTimestamps.sortedBy { (it.departure - now).absoluteValue }
        } else {
            rdsTripTimestamps
        }.firstOrNull()
    }

private fun Schedule.Timestamp.stopSequenceMatch(filterStopSequence: Int? = null): Boolean =
    this.stopSequenceOrNull == null // should never happen -> FF: ON since March 2026
            || filterStopSequence == null
            || this.stopSequenceOrNull == filterStopSequence

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
    if (stuArrivalTime != null) {
        rdsTripTimestamp.updateArrivalForRealTime(newArrival = stuArrivalTime)
        updated = true
    } else if (stuArrivalDelay != null) {
        rdsTripTimestamp.updateArrivalForRealTime(stuArrivalDelay, rdsSchedule.providerPrecision, PROVIDER_PRECISION)
        updated = true
    }
    if (stuDepartureTime != null) {
        rdsTripTimestamp.updateDepartureForRealTime(newDeparture = stuDepartureTime)
        updated = true
    } else if (stuDepartureDelay != null) {
        rdsTripTimestamp.updateDepartureForRealTime(stuDepartureDelay, rdsSchedule.providerPrecision, PROVIDER_PRECISION)
        updated = true
    }
    if (gStopTimeUpdate.optScheduleRelationship == GTUSTUScheduleRelationship.SKIPPED) {
        rdsSchedule.setStopTimeCancelled(rdsTripTimestamp, includeCancelledTimestamps)
        updated = true
    }
    if (updated) rdsSchedule.setReadFromSourceAtInMsKeepMostRecent(readFromSourceMs)
    return stuDepartureDelay
        .takeIf { gStopTimeUpdate.optScheduleRelationship != GTUSTUScheduleRelationship.NO_DATA }
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
