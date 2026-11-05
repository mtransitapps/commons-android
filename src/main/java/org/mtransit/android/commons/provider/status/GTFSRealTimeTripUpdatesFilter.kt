package org.mtransit.android.commons.provider.status

import androidx.annotation.VisibleForTesting
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.ALLOW_IGNORE_TRIP_DESCRIPTOR_DIRECTION_ID
import org.mtransit.android.commons.provider.gtfs.GTFSDateTimeUtils
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.hasStopTimeUpdateList
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.hasTimeOrScheduledTime
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optArrival
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDeparture
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDirectionIdValid
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optScheduleRelationship
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStartDateOrModifiedTripStartDate
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStartTimeOrModifiedTripStartTime
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopTimeUpdateList
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimeOrScheduledTimeMs
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimestampMs
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripIdNotEmpty
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optVehicle
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.scheduleRelationshipOrDefault
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.status.GTFSRealTimeTripUpdatesProvider.DEBUG_STATIC_RT_MATCH
import org.mtransit.android.commons.provider.status.GTFSRealTimeTripUpdatesProvider.LOG_TAG
import org.mtransit.android.commons.provider.status.GTFSRealTimeTripUpdatesProvider.TRIP_UPDATE_MAX_AGE_MS
import org.mtransit.android.commons.toDurationLog
import org.mtransit.android.commons.toMillis
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import com.google.transit.realtime.GtfsRealtime.TripDescriptor as GTripDescriptor
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship as GTDScheduleRelationship
import com.google.transit.realtime.GtfsRealtime.TripUpdate as GTripUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate as GTUStopTimeUpdate
import kotlinx.datetime.TimeZone as KtTimeZone

internal fun List<GTripUpdate>.selectRouteDirectionTripUpdates(
    nowMs: Long,
    targetRouteIdHash: String,
    targetDirectionOriginalId: Int?,
    agencyTimeZone: KtTimeZone,
    findOutOfSyncTripId: (departure: Instant, stuList: List<GTUStopTimeUpdate>?) -> String?,
    staticRDTripIds: Set<String>,
    tripIdOutOfSync: Boolean,
    ignoreDirection: Boolean,
    parseRouteId: (GTripDescriptor) -> String?,
    parseTripId: (GTripDescriptor) -> String?,
) = this
    .mapNotNull { gTripUpdate ->
        gTripUpdate
            .match(
                nowMs = nowMs,
                targetRouteIdHash = targetRouteIdHash,
                targetDirectionOriginalId = targetDirectionOriginalId,
                agencyTimeZone = agencyTimeZone,
                findOutOfSyncTripId = findOutOfSyncTripId,
                staticRDTripIds = staticRDTripIds,
                tripIdOutOfSync = tripIdOutOfSync,
                ignoreDirection = ignoreDirection,
                parseRouteId = parseRouteId,
                parseTripId = parseTripId,
            )
            ?.let { staticTripId ->
                staticTripId to gTripUpdate
            }
    }
    .filter { (_, gTripUpdate) -> gTripUpdate.isUseful(nowMs, agencyTimeZone) }
    .filterDuplicatesTrips()

// Montréal-based competitor: 4.5 hours (?)
private val FUTURE_TRIP_UPDATE_MAX_DIFF_MS = 90.minutes.times(3).inWholeMilliseconds

internal fun GTripUpdate.isUseful(nowMs: Long, agencyTimeZone: KtTimeZone): Boolean {
    val td = optTrip ?: return false // not useful
    td.optScheduleRelationship?.let { scheduleRelationship ->
        if (scheduleRelationship == GTDScheduleRelationship.DELETED
            || scheduleRelationship == GTDScheduleRelationship.CANCELED
        ) {
            return true // useful (no max age)
        }
    }
    optTimestampMs?.let { tuTimestamp ->
        if (tuTimestamp + TRIP_UPDATE_MAX_AGE_MS < nowMs) {
            MTLog.d(LOG_TAG, "isUseful() > IGNORE ${(nowMs - tuTimestamp).toDurationLog()} old: ${td.toStringExt(true)}")
            return false // not useful (too old to display)
        }
    }
    val hasVehicleInfo = optVehicle?.let { it.hasId() || it.hasLabel() || it.hasLicensePlate() } == true
    val startDateTimeOrFirstTimeMs: Long? = if (hasVehicleInfo) {
        null
    } else {
        GTFSDateTimeUtils.parseToDateTime(td.optStartDateOrModifiedTripStartDate, td.optStartTimeOrModifiedTripStartTime, agencyTimeZone)?.toMillis()
            ?: optStopTimeUpdateList // not sorting because GTFS spec requires it to be already sorted & it's a fallback
                ?.firstOrNull { it.optDeparture?.hasTimeOrScheduledTime() == true || it.optArrival?.hasTimeOrScheduledTime() == true }
                ?.let {
                    it.optDeparture?.optTimeOrScheduledTimeMs ?: it.optArrival?.optTimeOrScheduledTimeMs
                }
    }
    startDateTimeOrFirstTimeMs?.let { timeMs ->
        if (nowMs + FUTURE_TRIP_UPDATE_MAX_DIFF_MS < timeMs) {
            MTLog.d(LOG_TAG, "isUseful() > IGNORE ${((timeMs - nowMs)).toDurationLog()} in the future: ${td.toStringExt(true)}")
            return false // not useful (too far in advance to display)
        }
    }
    if (!hasDelay() && !hasStopTimeUpdateList() && td.scheduleRelationshipOrDefault == GTDScheduleRelationship.SCHEDULED) {
        MTLog.d(LOG_TAG, "isUseful() > IGNORE (no delay, stop time update & scheduled): ${this.toStringExt()}")
        return false // not useful
    }
    return true // useful (contains delay or STUs or not a SCHEDULED trip)
}

// Google Maps > Transit: https://support.google.com/transitpartners/answer/6159841
// - stop_times.txt based: trip_id+start_date || route_id+direction_id+start_date(+start_time)
// - frequencies.txt based: trip_id+start_date+start_time || route_id+direction_id+start_date+start_time
// Montréal-based competitor: https://resources.transitapp.com/article/462-trip-updates#Trip-Descriptor-68xQI
// - trip_id || route_id+direction_id+start_date+start_time

@VisibleForTesting
internal const val IGNORE_TRIP_ID = false
// internal const val IGNORE_TRIP_ID = true // DEBUG

internal fun GTripUpdate.match(
    nowMs: Long,
    targetRouteIdHash: String,
    targetDirectionOriginalId: Int?,
    agencyTimeZone: KtTimeZone,
    findOutOfSyncTripId: (departure: Instant, stuList: List<GTUStopTimeUpdate>?) -> String?,
    staticRDTripIds: Set<String>,
    tripIdOutOfSync: Boolean,
    ignoreDirection: Boolean,
    parseRouteId: (GTripDescriptor) -> String?,
    parseTripId: (GTripDescriptor) -> String?,
): String? {
    optTrip?.apply {
        val tripId = parseTripId(this)
        if (tripId in staticRDTripIds) {
            if (IGNORE_TRIP_ID) {
                MTLog.d(LOG_TAG, "match() > IGNORE FOUND trip ID '$tripId' for ${toStringExt(short = true)}")
            } else {
                // if (DEBUG_STATIC_RT_MATCH) { // too much log
                MTLog.d(LOG_TAG, "match() > FOUND: trip ID '$tripId' for ${toStringExt(short = true)}")
                // }
                return tripId // MATCH
            }
        }
        @Suppress("SimplifyBooleanWithConstants")
        if (!tripIdOutOfSync && tripId != null && !IGNORE_TRIP_ID) return null // NOT a match
        parseRouteId(this).let { rtRouteIdHash ->
            if (rtRouteIdHash != targetRouteIdHash) {
                // if (DEBUG_STATIC_RT_MATCH) { // too much log
                // MTLog.d(LOG_TAG, "match() > IGNORE: wrong route ID '$rtRouteIdHash' (t:$targetRouteIdHash)")
                // }
                return null // NOT A MATCH
            }
        }
        @Suppress("SimplifyBooleanWithConstants")
        if (!ALLOW_IGNORE_TRIP_DESCRIPTOR_DIRECTION_ID || optTripIdNotEmpty == null) {
            optDirectionIdValid?.takeIf { !ignoreDirection }?.let { directionId ->
                if (directionId != targetDirectionOriginalId) {
                    if (DEBUG_STATIC_RT_MATCH) {
                        MTLog.d(LOG_TAG, "match() > IGNORE: wrong direction ID '$directionId' for ${toStringExt(short = true)}")
                    }
                    return null // NOT A MATCH
                }
            }
        }
        optStartTimeOrModifiedTripStartTime?.let { startTime -> // start time required when trip ID do not match
            val tuDeparture = optStartDateOrModifiedTripStartDate?.let { startDate ->
                GTFSDateTimeUtils.parseToDateTime(startDate, startTime, agencyTimeZone)
            } ?: run {
                GTFSDateTimeUtils.parseToTime(nowMs, startTime, agencyTimeZone)
            }
            tuDeparture?.let { tuDeparture ->
                findOutOfSyncTripId(tuDeparture, this@match.optStopTimeUpdateList)?.let { staticTripId ->
                    // if (DEBUG_STATIC_RT_MATCH) {
                    MTLog.i(LOG_TAG, "match() > FOUND: out-of-sync trip ID '$staticTripId' for ${toStringExt(short = true)}")
                    // }
                    @Suppress("SimplifyBooleanWithConstants")
                    if (IGNORE_TRIP_ID && staticTripId != parseTripId(this)) {
                        MTLog.w(
                            LOG_TAG,
                            "match() > WARNING: found out-of-sync trip ID '$staticTripId' has different trip ID than expected (${parseTripId(this)})!"
                        )
                    }
                    return staticTripId
                }
            }
        }
    }
    // if (DEBUG_STATIC_RT_MATCH) { // too much log
    // MTLog.d(this@match, "match() > IGNORE (NOT A MATCH): ${toStringExt(short = true)}")
    // }
    return null // NOT A MATCH
}

internal fun List<Pair<String, GTripUpdate>>.filterDuplicatesTrips() = buildList<Pair<String, GTripUpdate>> {
    this@filterDuplicatesTrips
        .filter { (_, gTripUpdate) ->
            gTripUpdate.optTrip?.optScheduleRelationship?.let {
                @Suppress("DEPRECATION")
                it == GTDScheduleRelationship.DUPLICATED || it == GTDScheduleRelationship.ADDED || it == GTDScheduleRelationship.NEW
            } != true // schedule relationships not supported YET
        }
        .groupBy { (tripId, gTripUpdate) ->
            val td = gTripUpdate.optTrip
            tripId to (td?.optStartDateOrModifiedTripStartDate to td?.optStartTimeOrModifiedTripStartTime)
        }
        .forEach { (_, tripIdAndTripUpdates) ->
            if (tripIdAndTripUpdates.isEmpty()) return@forEach
            if (tripIdAndTripUpdates.size == 1) {
                val gTripUpdate1 = tripIdAndTripUpdates.firstOrNull() ?: return@forEach
                add(gTripUpdate1)
                return@forEach
            }
            // PICK ONE
            // 1 - pick one w/o ModifiedTrip AND w/ Vehicle descriptor
            tripIdAndTripUpdates
                .filter { (_, tu) -> tu.optTrip?.hasModifiedTrip() == false && tu.hasVehicle() }
                .takeIf { it.isNotEmpty() }
                ?.let { woModifiedTripAndWVehicle ->
                    val picked = woModifiedTripAndWVehicle.first()
                    MTLog.d(
                        LOG_TAG,
                        "filterDuplicatesTrips() > pick 1st out of ${woModifiedTripAndWVehicle.size} w/o ModifiedTrip and w/ Vehicle: " +
                            "[${picked.first}] ${picked.second.toStringExt()}"
                    )
                    add(picked)
                    return@forEach
                }
            // 2 - pick one w/o ModifiedTrip
            tripIdAndTripUpdates
                .filter { (_, tu) -> tu.optTrip?.hasModifiedTrip() == false }
                .takeIf { it.isNotEmpty() }
                ?.let { woModifiedTrip ->
                    val picked = woModifiedTrip.first()
                    MTLog.d(
                        LOG_TAG,
                        "filterDuplicatesTrips() > pick 1st out of ${woModifiedTrip.size} w/o ModifiedTrip: " +
                            "[${picked.first}] ${picked.second.toStringExt()}"
                    )
                    add(picked)
                    return@forEach
                }
            // 3 - pick one w/ Vehicle descriptor (even if it has ModifiedTrip)
            tripIdAndTripUpdates
                .filter { (_, tu) -> tu.hasVehicle() }
                .takeIf { it.isNotEmpty() }
                ?.let { wVehicle ->
                    val picked = wVehicle.first()
                    MTLog.d(
                        LOG_TAG,
                        "filterDuplicatesTrips() > pick 1st out of ${wVehicle.size} w/ Vehicle: " +
                            "[${picked.first}] ${picked.second.toStringExt()}"
                    )
                    add(picked)
                    return@forEach
                }
            // ELSE -> pick 1st one in the list
            val gTripUpdate1 = tripIdAndTripUpdates.firstOrNull() ?: return@forEach
            MTLog.d(
                LOG_TAG,
                "filterDuplicatesTrips() > use 1st one out of ${tripIdAndTripUpdates.size}: " +
                    "[${gTripUpdate1.first}] ${gTripUpdate1.second.toStringExt()}"
            )
            add(gTripUpdate1)
        }
}
