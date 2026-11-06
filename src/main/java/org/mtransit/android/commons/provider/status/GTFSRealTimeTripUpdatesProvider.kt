package org.mtransit.android.commons.provider.status

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.Context
import com.google.transit.realtime.headerOrNull
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SecurityUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.ScheduleStatusFilter
import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.data.makeSchedule
import org.mtransit.android.commons.data.toNoData
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.agency.AgencyUtils
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopIdNotEmpty
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopTimeUpdateList
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimestampMs
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.sortTripUpdates
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toTripUpdates
import org.mtransit.android.commons.provider.gtfs.getRDS
import org.mtransit.android.commons.provider.gtfs.getRDSSchedules
import org.mtransit.android.commons.provider.gtfs.getTripIds
import org.mtransit.android.commons.provider.gtfs.ignoreDirection
import org.mtransit.android.commons.provider.gtfs.isSameStop
import org.mtransit.android.commons.provider.gtfs.makeRequest
import org.mtransit.android.commons.provider.gtfs.match
import org.mtransit.android.commons.provider.gtfs.parseRouteId
import org.mtransit.android.commons.provider.gtfs.parseStopId
import org.mtransit.android.commons.provider.gtfs.parseTripId
import org.mtransit.android.commons.provider.gtfs.storage
import org.mtransit.android.commons.toDateTimeLog
import org.mtransit.commons.SourceUtils
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import com.google.transit.realtime.GtfsRealtime.FeedMessage as GFeedMessage
import com.google.transit.realtime.GtfsRealtime.TripUpdate as GTripUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate as GTUStopTimeUpdate
import kotlinx.datetime.TimeZone as KtTimeZone

object GTFSRealTimeTripUpdatesProvider : MTLog.Loggable {

    internal val LOG_TAG: String = GTFSRealTimeTripUpdatesProvider::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    val PROVIDER_PRECISION = 10.seconds
    val PROVIDER_PRECISION_IN_MS = PROVIDER_PRECISION.inWholeMilliseconds

    @JvmStatic
    fun Long.adaptForCachedAPI(context: Context?) =
        if (context?.let { GTFSRealTimeProvider.getAGENCY_TRIP_UPDATES_URL_CACHED(it) }?.isNotBlank() == true) {
            this.coerceAtLeast(1.minutes.inWholeMilliseconds) // fewer calls to Cached API $$
        } else {
            this
        }

    @JvmStatic
    fun GTFSRealTimeProvider.getCached(statusFilter: StatusProviderContract.Filter): POIStatus? {
        val filter = statusFilter as? ScheduleStatusFilter ?: run {
            MTLog.w(LOG_TAG, "getCached() > Can't find new schedule without schedule filter!")
            return null
        }
        val staticRDTripIds = filter.targetAuthority.let { targetAuthority ->
            filter.routeId.let { routeId ->
                context?.getTripIds(targetAuthority, routeId, filter.directionId)
            }
        }?.takeIf { tripIds -> tripIds.isNotEmpty() } // static trip IDs REQUIRED for GTFS Trip Updates
            ?: return null
        return getCachedStatusS(filter.targetUUID, staticRDTripIds)
            ?: makeCachedStatusFromAgencyDataLock(filter, staticRDTripIds)
    }

    private val tripUpdateLock = mutableMapOf<String, Any>()

    private fun GTFSRealTimeProvider.makeCachedStatusFromAgencyDataLock(
        filter: ScheduleStatusFilter,
        staticRDTripIds: Set<String>
    ): POIStatus? {
        val context = context ?: return null
        if (storage.getTripUpdateLastUpdateMs(0L) <= 0L) return null // never loaded
        gTripUpdates ?: return null
        synchronized(tripUpdateLock.getOrPut(filter.routeDirectionStop.routeDirectionUUID) { Any() }) {
            return getCachedStatusS(filter.targetUUID, staticRDTripIds) // try another time
                ?: makeCachedStatusFromAgencyData(context, filter, staticRDTripIds)
        }
    }

    internal const val DEBUG_STATIC_RT_MATCH = false
    // internal const val DEBUG_STATIC_RT_MATCH = true // DEBUG

    private const val PRINT_ALL_RD_STOP_TIME_UPDATES = false
    // private const val PRINT_ALL_RD_STOP_TIME_UPDATES = true // DEBUG

    // Best practices: 90 seconds
    // https://gtfs.org/documentation/realtime/realtime-best-practices/#feed-publishing-general-practices
    // Montréal-based competitor: 10 minutes
    internal val TRIP_UPDATE_MAX_AGE_MS = 90.times(7).seconds.inWholeMilliseconds

    // internal const val PRE_FILTER_TRIP_UPDATES = true
    internal const val PRE_FILTER_TRIP_UPDATES = false // WIP

    private fun GTFSRealTimeProvider.makeCachedStatusFromAgencyData(
        context: Context,
        filter: ScheduleStatusFilter,
        staticRDTripIds: Set<String>,
    ): POIStatus? {
        MTLog.d(LOG_TAG, "makeCachedStatusFromAgencyData(${filter.targetUUID}, ${staticRDTripIds.size})")
        val lastUpdateInMs = storage.getTripUpdateLastUpdateMs(0L)
            .takeIf { it > 0L } ?: return null // never loaded
        val nowMs = TimeUtils.currentTimeMillis()
        val feedReadFromSourceMs = storage.getTripUpdateReadFromSourceMs(0L)
            .takeIf { it > 0L }
            ?.also { feedTimestamp ->
                if (feedTimestamp + TRIP_UPDATE_MAX_AGE_MS < nowMs) {
                    MTLog.w(LOG_TAG, "makeCachedStatusFromAgencyData(${filter.targetUUID}) > IGNORE cached feed (too old: ${feedTimestamp.toDateTimeLog()})")
                    return null
                }
            } ?: lastUpdateInMs
        val gTripUpdates = gTripUpdates ?: return null
        val sourceLabel = SourceUtils.getSourceLabel( // always use source from official API
            GTFSRealTimeProvider.getAgencyTripUpdatesUrlString(context, "T")
        )
        try {
            val (targetRoute, targetDirection) = filter.routeDirectionStop.let { it.route to it.direction }
            val rdID = "${targetRoute.id}-${targetDirection.id}"
            val targetAuthority = filter.targetAuthority
            val targetRouteIdHash = targetRoute.originalIdHash.toString()
            val targetDirectionOriginalId = targetDirection.originalDirectionIdOrNull
            if (DEBUG_STATIC_RT_MATCH) {
                MTLog.d(LOG_TAG, "makeCachedStatusFromAgencyData($rdID) > target trip IDs [${staticRDTripIds.size}]:")
                staticRDTripIds.chunked(9).forEach {
                    MTLog.d(LOG_TAG, "makeCachedStatusFromAgencyData($rdID) > - ${it.joinToString(",")}")
                }
            }
            val sortedRDS = // needed all the time: generate real-time OR no-date schedule
                context.getRDS(targetAuthority, targetRoute.id, targetDirection.id)
                    ?.takeIf { it.isNotEmpty() }
                    ?: run {
                        MTLog.d(
                            LOG_TAG,
                            "makeCachedStatusFromAgencyData($rdID) > SKIP " +
                                "(no stops for this route direction: $targetAuthority, ${targetRoute.id}, ${targetDirection.id})."
                        )
                        return null
                    }
            val rdSchedules: Collection<Schedule> by lazy { // delay/avoid DB read
                context.getRDSSchedules(targetAuthority, sortedRDS, filter.isIncludeCancelledTimestampsOrDefault)
            }
            val agencyTimeZoneId = AgencyUtils.getAgencyTimeZoneId(context)
            val agencyTimeZone = runCatching { KtTimeZone.of(agencyTimeZoneId) }.getOrElse { e ->
                MTLog.w(LOG_TAG, e, "makeCachedStatusFromAgencyData($rdID) > error getting timezone from '$agencyTimeZoneId'!")
                KtTimeZone.currentSystemDefault()
            }
            val rdSchedulesByUUID by lazy { rdSchedules.associateBy { it.targetUUID } }
            val rdsUUIDsByTripId by lazy {
                rdSchedules
                    .flatMap { schedule ->
                        schedule.timestamps
                            .mapNotNull { it.tripId }
                            .map { staticTripId -> staticTripId to schedule.targetUUID }
                    }
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, uuids) -> uuids.toSet() }
            }
            // region Out Of Sync Trip IDs
            val outOfSyncTripIdToTargetUUIDStopSeqTimestamps by lazy {
                rdSchedules
                    .asSequence()
                    // 1 uuid can have multiple timestamp for 1 trip
                    .flatMap { schedule ->
                        schedule.timestamps.asSequence().map { (schedule.targetUUID to it.stopSequenceOrNull) to it }
                    }
                    .groupBy { (_, timestamp) -> timestamp.tripId.orEmpty() }
                    .filterKeys { it.isNotEmpty() }
            }
            val departuresToOutOfSyncTripIds by lazy {
                outOfSyncTripIdToTargetUUIDStopSeqTimestamps
                    .mapNotNull { (staticTripId, targetUUIDStopSeqToTimestamps) ->
                        // might not contain the actual 1st trip stop departure
                        // since we are loading last X hours instead of all trips started and not finished + near future trips
                        // -> good-enough for now as it's already a fall-back for out-of-sync trip IDs
                        targetUUIDStopSeqToTimestamps
                            .minByOrNull { (_, timestamp) -> timestamp.departure }
                            ?.let { (_, timestamp) ->
                                timestamp.departure to staticTripId
                            }
                    }
                    .groupBy({ it.first }, { it.second })
            }
            val stuListMatch: (tripId: String, stuList: List<GTUStopTimeUpdate>?, all: Boolean) -> Boolean = stuListMatch@{ tripId, stuList, all ->
                stuList?.takeIf { it.isNotEmpty() } ?: return@stuListMatch all
                val tripTargetUUIDStopSeqTimestamps by lazy { outOfSyncTripIdToTargetUUIDStopSeqTimestamps[tripId] }
                stuList.forEach { gSTU ->
                    val match = match(
                        gSTU,
                        rdsCandidates = sortedRDS,
                        findStopTimestamp = { rdsUUID, gSTUStopSeq ->
                            tripTargetUUIDStopSeqTimestamps?.find { (targetUUIDAndStopSeq, _) ->
                                targetUUIDAndStopSeq.first == rdsUUID
                                    && targetUUIDAndStopSeq.second.let { stopSeq -> stopSeq == null || stopSeq == gSTUStopSeq }
                                /** stop seq should not be null if FF is turned ON [org.mtransit.commons.FeatureFlags.F_EXPORT_STOP_SEQUENCE] */
                            }?.second != null
                        }
                    )
                    if (all) {
                        if (!match) {
                            MTLog.d(LOG_TAG, "stuListMatch() > trip ID '$tripId' does NOT match! (${gSTU.toStringExt(short = true)})")
                            return@stuListMatch false
                        }
                    } else { // any
                        if (match) {
                            return@stuListMatch true
                        }
                    }
                }
                return@stuListMatch all
            }
            val findOutOfSyncTripId: (departure: Instant, stuList: List<GTUStopTimeUpdate>?) -> String? = findTripId@{ departure, gSTUList ->
                if (gSTUList.isNullOrEmpty()) {
                    MTLog.d(LOG_TAG, "findOutOfSyncTripId() > out-of-sync trip ID NOT found at ${departure.toDateTimeLog()} (no stop time updates).")
                    // could be CANCELLED or DELETED trips (w/o stop time updates)
                    // -> good-enough for now as it's already a fall-back for out-of-sync trip IDs
                    return@findTripId null // NO MATCH
                }
                val staticTripIdsWithSameDepartures = departuresToOutOfSyncTripIds[departure]
                if (staticTripIdsWithSameDepartures.isNullOrEmpty()) {
                    MTLog.d(LOG_TAG, "findOutOfSyncTripId() > out-of-sync trip ID NOT found at ${departure.toDateTimeLog()} (no candidates).")
                    return@findTripId null // NO MATCH
                }
                MTLog.d(LOG_TAG, "findOutOfSyncTripId() > out-of-sync trip ID candidate(s) found: ${staticTripIdsWithSameDepartures.joinToString()}.")
                staticTripIdsWithSameDepartures.forEach candidates@{ staticTripId ->
                    val tripRdsUUIDs = rdsUUIDsByTripId[staticTripId]
                        ?: return@candidates // trip ID not in static data
                    val tripSchedules = tripRdsUUIDs.mapNotNull { rdSchedulesByUUID[it] }
                    val tripSortedRDS = sortedRDS
                        .filter { rds -> rds.uuid in tripRdsUUIDs }
                        .takeIf { it.isNotEmpty() }
                        ?: return@candidates
                    gSTUList.forEach { stu ->
                        stu.optStopIdNotEmpty?.let { parseStopId(it) }?.let { stopId ->
                            if (tripSortedRDS.none { it.stop.isSameOriginalId(stopId) }) {
                                MTLog.d(
                                    LOG_TAG,
                                    "findOutOfSyncTripId() > out-of-sync trip ID NOT found at ${departure.toDateTimeLog()} " +
                                        "(stop ID '${stu.optStopId}' not in this route direction)."
                                )
                                return@candidates // NO MATCH
                            }
                        }
                    }
                    val sortedTargetUuidAndSequence = makeTargetUuidAndSequenceList(staticTripId, tripSchedules, tripSortedRDS)
                    val gSTUStopSeqFixed = gSTUList.fixStopSequence(
                        tripId = staticTripId,
                        tripSortedRDS = tripSortedRDS,
                        sortedTargetUuidAndSequence = sortedTargetUuidAndSequence,
                        isSameStop = { stu, rds, stopSeq -> stu?.isSameStop(rds, stopSeq, this::parseStopId) == true },
                        parseStopId = ::parseStopId,
                    ).takeIf { it.isNotEmpty() } ?: return@candidates // NO MATCH
                    // need to match as many as possible as same stop ID + sequence can be in both directions
                    val stopTimeUpdatesMatch = stuListMatch(staticTripId, gSTUStopSeqFixed, true)
                    if (!stopTimeUpdatesMatch) {
                        MTLog.d(LOG_TAG, "findOutOfSyncTripId() > ignore wrong out-of-sync trip ID '$staticTripId' at ${departure.toDateTimeLog()}.")
                        return@candidates // NO MATCH
                    }
                    MTLog.d(LOG_TAG, "findOutOfSyncTripId() > out-of-sync trip ID '$staticTripId' found at ${departure.toDateTimeLog()}.")
                    return@findTripId staticTripId // MATCH
                }
                MTLog.d(LOG_TAG, "findOutOfSyncTripId() > out-of-sync trip ID NOT found at ${departure.toDateTimeLog()}.")
                return@findTripId null // NO MATCH
            }
            // endregion Out Of Sync Trip IDs
            val tripIdsMatch by lazy {
                val rdTripUpdatesCandidates =
                    gTripUpdates.filter { gTripUpdate ->
                        val gTripDescriptor = gTripUpdate.optTrip ?: return@filter false
                        val tripId = parseTripId(gTripDescriptor) ?: return@filter false
                        val rtRouteIdHash = parseRouteId(gTripDescriptor)
                        (rtRouteIdHash == null || rtRouteIdHash == targetRouteIdHash) // keep route ID unknown or match
                            && gTripUpdate.isUseful(nowMs, agencyTimeZone)
                            && stuListMatch(tripId, gTripUpdate.optStopTimeUpdateList, false)
                    }.takeIf { it.isNotEmpty() } ?: run {
                        return@lazy true // no trip updates for this route -> match
                    }
                rdTripUpdatesCandidates.any { gTripUpdate ->
                    gTripUpdate.optTrip?.let { parseTripId(it) }?.let { tripId -> tripId in staticRDTripIds } == true
                }.also { tripIdsMatch ->
                    if (!tripIdsMatch) {
                        MTLog.w(LOG_TAG, "makeCachedStatusFromAgencyData($rdID) > no trip IDs matches!")
                        if (Constants.DEBUG) {
                            MTLog.w(
                                LOG_TAG,
                                "makeCachedStatusFromAgencyData($rdID) > - static trip IDs: [${staticRDTripIds.size}] ${staticRDTripIds.joinToString()}"
                            )
                            if (PRINT_ALL_RD_STOP_TIME_UPDATES) {
                                MTLog.w(LOG_TAG, "makeCachedStatusFromAgencyData($rdID) > - real-time trip IDs: [${rdTripUpdatesCandidates.size}]")
                                rdTripUpdatesCandidates.forEach {
                                    MTLog.w(LOG_TAG, "makeCachedStatusFromAgencyData($rdID) > - real-time trip IDs: - ${it.toStringExt()}")
                                }
                            } else {
                                MTLog.w(
                                    LOG_TAG,
                                    "makeCachedStatusFromAgencyData($rdID) > - real-time trip IDs: [${rdTripUpdatesCandidates.size}] ${
                                        rdTripUpdatesCandidates.joinToString { tu ->
                                            tu.optTrip?.let { parseTripId(it) }.orEmpty()
                                        }
                                    }"
                                )
                            }
                        }
                    }
                    MTLog.d(LOG_TAG, "makeCachedStatusFromAgencyData($rdID) > tripIdsMatch > $tripIdsMatch")
                }
            }
            val rdTripIdsToTripUpdates = if (!PRE_FILTER_TRIP_UPDATES) {
                null
            } else {
                gTripUpdates.selectRouteDirectionTripUpdates(
                    nowMs = nowMs,
                    targetRouteIdHash = targetRouteIdHash,
                    targetDirectionOriginalId = targetDirectionOriginalId,
                    agencyTimeZone = agencyTimeZone,
                    findOutOfSyncTripId = findOutOfSyncTripId,
                    staticRDTripIds = staticRDTripIds,
                    getTripIdsMatch = { tripIdsMatch },
                    ignoreDirection = ignoreDirection,
                    parseRouteId = ::parseRouteId,
                    parseTripId = ::parseTripId,
                )
            }
            if (PRE_FILTER_TRIP_UPDATES) {
                rdTripIdsToTripUpdates?.takeIf { it.isNotEmpty() && rdSchedules.isNotEmpty() } ?: run {
                    sortedRDS.forEach { rds ->
                        rds.makeSchedule(
                            lastUpdateInMs = lastUpdateInMs,
                            maxValidityInMs = statusMaxValidityInMs,
                            readFromSourceAtInMs = feedReadFromSourceMs,
                            providerPrecisionInMs = PROVIDER_PRECISION_IN_MS,
                            localTimeZoneId = agencyTimeZoneId,
                            sourceLabel = sourceLabel,
                            noData = true, // NO DATA
                        ).let { noDataStatus ->
                            cacheStatus(noDataStatus) // avoid re-run
                        }
                    }
                    MTLog.i(
                        LOG_TAG,
                        "makeCachedStatusFromAgencyData($rdID) > No trip updates or static schedule found " +
                            "for route '${targetRoute.shortestName}' direction '${targetDirection.headsignValue}'."
                    )
                    return null
                }
                MTLog.i(
                    LOG_TAG,
                    "makeCachedStatusFromAgencyData($rdID) > Using ${rdTripIdsToTripUpdates.size} trip updates " +
                        "for route '${targetRoute.shortestName}' direction '${targetDirection.headsignValue}': " +
                        "${rdTripIdsToTripUpdates.map { it.first }.distinct()}."
                )
                if (Constants.DEBUG) {
                    MTLog.d(
                        LOG_TAG,
                        "makeCachedStatusFromAgencyData($rdID) > GTFS " +
                            "{R:'${targetRoute.shortestName}'|D:${targetDirection.headsignValue}} [${gTripUpdates.size}]: "
                    )
                    rdTripIdsToTripUpdates.forEach { (_, gTripUpdate) ->
                        MTLog.d(LOG_TAG, "makeCachedStatusFromAgencyData($rdID) > GTFS - ${gTripUpdate.toStringExt()}.")
                        if (PRINT_ALL_RD_STOP_TIME_UPDATES) {
                            gTripUpdate.optStopTimeUpdateList?.forEachIndexed { idx, stu ->
                                MTLog.d(LOG_TAG, "makeCachedStatusFromAgencyData($rdID) > GTFS - [$idx] ${stu.toStringExt()}")
                            }
                        }
                    }
                }
            }
            val gTripUpdatesByTripId by lazy {
                gTripUpdates
                    .mapIndexed { index, gTripUpdate ->
                        val tripId = gTripUpdate.optTrip?.let { parseTripId(it) }
                            ?: "no_trip_id_$index" // keep all trip updates since we can match w/o trip ID
                        tripId to gTripUpdate
                    }
                    .filter { (_, gTripUpdate) -> gTripUpdate.isUseful(nowMs, agencyTimeZone) }
                    .filterDuplicatesTrips()
            }
            val hasMatchingTripUpdates = processRDTripUpdates(
                gTripUpdates = if (PRE_FILTER_TRIP_UPDATES) rdTripIdsToTripUpdates.orEmpty() else gTripUpdatesByTripId,
                nowMs = nowMs,
                targetRouteIdHash = targetRouteIdHash,
                targetDirectionOriginalId = targetDirectionOriginalId,
                staticRDTripIds = staticRDTripIds,
                getTripIdsMatch = { tripIdsMatch },
                agencyTimeZone = agencyTimeZone,
                findOutOfSyncTripId = findOutOfSyncTripId,
                getRDSchedulesByUUID = { rdSchedulesByUUID },
                getRDSUUIDsByTripId = { rdsUUIDsByTripId },
                sortedRDS = sortedRDS,
                feedReadFromSourceMs = feedReadFromSourceMs,
                includeCancelledTimestamps = filter.isIncludeCancelledTimestampsOrDefault,
            )
            @Suppress("SimplifyBooleanWithConstants")
            if (!PRE_FILTER_TRIP_UPDATES && !hasMatchingTripUpdates) {
                sortedRDS.forEach { rds ->
                    rds.makeSchedule(
                        lastUpdateInMs = lastUpdateInMs,
                        maxValidityInMs = statusMaxValidityInMs,
                        readFromSourceAtInMs = feedReadFromSourceMs,
                        providerPrecisionInMs = PROVIDER_PRECISION_IN_MS,
                        localTimeZoneId = agencyTimeZoneId,
                        sourceLabel = sourceLabel,
                        noData = true, // NO DATA
                    ).let { noDataStatus ->
                        cacheStatus(noDataStatus) // avoid re-run
                    }
                }
                MTLog.d(
                    LOG_TAG,
                    "makeCachedStatusFromAgencyData($rdID) > SKIP " +
                        "(no matching trip updates or static schedule for this route direction: $targetAuthority, ${targetRoute.id}, ${targetDirection.id})."
                )
                return null
            }
            cacheRealTimeSchedules(rdSchedules = rdSchedules, sourceLabel = sourceLabel, lastUpdateInMs = lastUpdateInMs)
            return getCachedStatusS(filter.targetUUID, staticRDTripIds)
        } catch (e: Exception) {
            MTLog.w(LOG_TAG, e, "makeCachedStatusFromAgencyData(${filter.targetUUID}) > error!")
            return null
        }
    }

    private val OLDEST_FOR_REAL_TIME = 1.minutes
    private val MAX_FUTURE_FOR_REAL_TIME = 12.hours
    private const val MIN_NUMBER_OF_FUTURE_FOR_REAL_TIME = 10 // need to keep multiple RT timestamps to be able to show the next non-RT is in a long time

    private fun GTFSRealTimeProvider.cacheRealTimeSchedules(
        rdSchedules: Collection<Schedule>,
        sourceLabel: String,
        lastUpdateInMs: Long,
        ignorePastRealTime: Boolean = false,
        tripsWithRealTime: Set<String> = rdSchedules
            .asSequence()
            .mapNotNull { schedule -> schedule.timestamps.takeIf { it.isNotEmpty() } }
            .flatten()
            .filter { it.isRealTimeOrCancelled }
            .mapNotNull { it.tripId }
            .toSet() // distinct
    ) {
        rdSchedules.forEach { rdsSchedule ->
            rdsSchedule.sourceLabel = sourceLabel
            rdsSchedule.lastUpdateInMs = lastUpdateInMs
            rdsSchedule.providerPrecisionInMs = PROVIDER_PRECISION_IN_MS
            rdsSchedule.maxValidityInMs = statusMaxValidityInMs
            val now = TimeUtilsK.currentInstant()
            if (rdsSchedule.timestamps.none { it.isRealTimeOrCancelled || (it.tripId in tripsWithRealTime && it.departure < now) }) {
                cacheStatus(rdsSchedule.toNoData()) // avoid re-run
                return@forEach
            }
            var oldestDateForRealTime = now - OLDEST_FOR_REAL_TIME
            var maxFutureDateForRealTime = now + MAX_FUTURE_FOR_REAL_TIME
            val (sortedPastTimestamps, sortedFutureTimestamps) = rdsSchedule.timestamps.partition { it.departure < now }
            if (!ignorePastRealTime) {
                oldestDateForRealTime = sortedPastTimestamps.filter { it.isRealTimeOrCancelled }.minOfOrNull { it.arrival } // all real-time
                    ?: oldestDateForRealTime
            }
            sortedFutureTimestamps.take(MIN_NUMBER_OF_FUTURE_FOR_REAL_TIME).maxOfOrNull { it.departure }
                ?.takeIf { it > maxFutureDateForRealTime }
                ?.let {
                    maxFutureDateForRealTime = it
                }
            sortedFutureTimestamps.filter { it.isRealTimeOrCancelled }.maxOfOrNull { it.departure } // all real-time
                ?.takeIf { it > maxFutureDateForRealTime }
                ?.let {
                    maxFutureDateForRealTime = it
                }
            // remove timestamps that are not real-time & outside of min/max date for real-time
            rdsSchedule.timestamps
                .filterNot {
                    it.isRealTimeOrCancelled || (oldestDateForRealTime < it.arrival && it.departure < maxFutureDateForRealTime)
                }
                .forEach { rdsSchedule.removeTimestamp(it) }
            cacheStatus(rdsSchedule)
        }
    }

    @JvmStatic
    fun GTFSRealTimeProvider.getNew(statusFilter: StatusProviderContract.Filter): POIStatus? {
        val filter = statusFilter as? ScheduleStatusFilter ?: run {
            MTLog.w(LOG_TAG, "getNew() > Can't find new schedule without schedule filter!")
            return null
        }
        updateAgencyDataIfRequired(filter.isInFocusOrDefault)
        return getCached(filter)
    }

    private fun GTFSRealTimeProvider.updateAgencyDataIfRequired(inFocus: Boolean) {
        val context = requireContextCompat()
        var inFocus = inFocus
        val lastUpdateCode = storage.getTripUpdateLastUpdateCode(-1).takeIf { it >= 0 }
        if (lastUpdateCode != null && lastUpdateCode != HttpURLConnection.HTTP_OK) {
            inFocus = true // force earlier retry if last fetch returned HTTP error
        }
        val minUpdateMs = min(statusMaxValidityInMs, getStatusValidityInMs(inFocus))
        val lastUpdateInMs = storage.getTripUpdateLastUpdateMs(0L)
        if (lastUpdateInMs + minUpdateMs > TimeUtils.currentTimeMillis()) {
            return
        }
        updateAgencyDataIfRequiredSync(context, lastUpdateInMs, inFocus)
    }

    @Synchronized
    private fun GTFSRealTimeProvider.updateAgencyDataIfRequiredSync(context: Context, lastUpdateInMs: Long, inFocus: Boolean) {
        if (storage.getTripUpdateLastUpdateMs(0L) > lastUpdateInMs) return // too late, another thread already updated
        val nowInMs = TimeUtils.currentTimeMillis()
        var deleteAllRequired = false
        if (lastUpdateInMs + statusMaxValidityInMs < nowInMs) {
            deleteAllRequired = true // too old to display
        }
        val minUpdateMs = min(statusMaxValidityInMs, getStatusValidityInMs(inFocus))
        if (!deleteAllRequired && nowInMs <= lastUpdateInMs + minUpdateMs) return
        updateAllAgencyDataFromWWW(context, deleteAllRequired) // try to update
    }

    private fun GTFSRealTimeProvider.updateAllAgencyDataFromWWW(context: Context, deleteAllRequired: Boolean) {
        var deleteAllDone = false
        if (deleteAllRequired) {
            deleteAllCachedStatus()
            deleteAllDone = true
        }
        val newStatusesLoaded = loadAgencyDataFromWWW(context)
        if (newStatusesLoaded) { // empty is OK
            if (!deleteAllDone) {
                deleteAllCachedStatus()
            }
            // no caching, will make as requested from cached file
        } // else keep whatever we have until max validity reached
    }

    private const val GTFS_RT_TRIP_UPDATE_PB_FILE_NAME = "gtfs_rt_trip_update.pb"

    @JvmStatic
    fun onLowMemory() {
        _gTripUpdates = null
    }

    @JvmStatic
    fun onTrimMemory(level: Int) {
        if (level >= TRIM_MEMORY_BACKGROUND) {
            _gTripUpdates = null
        }
    }

    @Volatile
    private var _gTripUpdates: List<GTripUpdate>? = null

    private var GTFSRealTimeProvider.gTripUpdates: List<GTripUpdate>?
        get() {
            if (_gTripUpdates == null) {
                synchronized(this@GTFSRealTimeTripUpdatesProvider) {
                    if (_gTripUpdates != null) return@synchronized
                    _gTripUpdates = context?.let { context ->
                        File(context.cacheDir, GTFS_RT_TRIP_UPDATE_PB_FILE_NAME)
                            .takeIf { file -> file.exists() }
                            ?.inputStream()
                            ?.use { inputStream ->
                                try {
                                    val gFeedMessage = GFeedMessage.parseFrom(inputStream)
                                    storage.saveTripUpdateReadFromSourceMs(gFeedMessage.headerOrNull?.optTimestampMs)
                                    gFeedMessage
                                        .entityList
                                        .toTripUpdates()
                                } catch (e: IOException) {
                                    MTLog.w(LOG_TAG, e, "gTripUpdates.get() > error while reading GTFS RT Trip Updates data!")
                                    null
                                }
                            }
                    }
                }
            }
            return _gTripUpdates
        }
        set(value) {
            synchronized(this@GTFSRealTimeTripUpdatesProvider) {
                _gTripUpdates = value
            }
        }

    private const val PRINT_ALL_LOADED_TRIP_UPDATES = false
    // private const val PRINT_ALL_LOADED_TRIP_UPDATES = true // DEBUG

    private fun GTFSRealTimeProvider.loadAgencyDataFromWWW(context: Context): Boolean {
        try {
            val urlRequest = makeRequest(
                loggable = this@GTFSRealTimeTripUpdatesProvider,
                context = context,
                urlCachedString = GTFSRealTimeProvider.getAGENCY_TRIP_UPDATES_URL_CACHED(context),
                getUrlString = { token -> GTFSRealTimeProvider.getAgencyTripUpdatesUrlString(context, token) }
            ) ?: return false
            getOkHttpClient(context).newCall(urlRequest).execute().use { response ->
                storage.saveTripUpdateLastUpdateCode(response.code)
                storage.saveTripUpdateLastUpdateMs(TimeUtils.currentTimeMillis())
                when (response.code) {
                    HttpURLConnection.HTTP_OK -> {
                        try {
                            try {
                                val responseBodyByes = response.body.bytes()
                                File(context.cacheDir, GTFS_RT_TRIP_UPDATE_PB_FILE_NAME).writeBytes(responseBodyByes)
                                val gFeedMessage = GFeedMessage.parseFrom(responseBodyByes)
                                storage.saveTripUpdateReadFromSourceMs(gFeedMessage.headerOrNull?.optTimestampMs)
                                gTripUpdates = gFeedMessage.entityList.toTripUpdates() // will be used soon
                                MTLog.i(LOG_TAG, "Found ${gTripUpdates?.size} statuses.")
                                @Suppress("SimplifyBooleanWithConstants")
                                if (Constants.DEBUG && PRINT_ALL_LOADED_TRIP_UPDATES) {
                                    MTLog.d(LOG_TAG, "loadAgencyDataFromWWW() > GTFS trip updates[${gTripUpdates?.size}]: ")
                                    gTripUpdates?.sortTripUpdates()?.forEach { gTripUpdate ->
                                        MTLog.d(LOG_TAG, "loadAgencyDataFromWWW() > - GTFS ${gTripUpdate.toStringExt()}")
                                    }
                                }
                                return true
                            } catch (e: IOException) {
                                MTLog.w(LOG_TAG, e, "loadAgencyDataFromWWW() > error while saving GTFS RT Trip Updates data!")
                                return false
                            }
                        } catch (e: Exception) {
                            MTLog.w(LOG_TAG, e, "loadAgencyDataFromWWW() > error while parsing GTFS Real Time data!")
                            return false
                        }
                    }

                    else -> {
                        MTLog.w(
                            LOG_TAG,
                            "ERROR: HTTP URL-Connection Response Code ${response.code} (Message: ${response.message})"
                        )
                        return false
                    }
                }
            }
        } catch (sslhe: SSLHandshakeException) {
            MTLog.w(LOG_TAG, sslhe, "SSL error!")
            SecurityUtils.logCertPathValidatorException(sslhe)
            storage.saveTripUpdateLastUpdateCode(567) // SSL certificate not trusted (on this device)
            storage.saveTripUpdateLastUpdateMs(TimeUtils.currentTimeMillis())
            return false
        } catch (iioe: InterruptedIOException) {
            MTLog.w(LOG_TAG, iioe, "Connection timeout!")
            storage.saveTripUpdateLastUpdateCode(567)
            storage.saveTripUpdateLastUpdateMs(TimeUtils.currentTimeMillis())
            return false
        } catch (uhe: UnknownHostException) {
            if (MTLog.isLoggable(android.util.Log.DEBUG)) {
                MTLog.w(LOG_TAG, uhe, "No Internet Connection!")
            } else {
                MTLog.w(LOG_TAG, "No Internet Connection!")
            }
            storage.saveTripUpdateLastUpdateCode(567)
            storage.saveTripUpdateLastUpdateMs(TimeUtils.currentTimeMillis())
            return false
        } catch (se: SocketException) {
            MTLog.w(LOG_TAG, se, "No Internet Connection!")
            storage.saveTripUpdateLastUpdateCode(567)
            storage.saveTripUpdateLastUpdateMs(TimeUtils.currentTimeMillis())
            return false
        } catch (ioe: IOException) {
            MTLog.w(LOG_TAG, ioe, "I/O error!")
            storage.saveTripUpdateLastUpdateCode(567)
            storage.saveTripUpdateLastUpdateMs(TimeUtils.currentTimeMillis())
            return false
        } catch (e: Exception) { // Unknown error
            MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception")
            return false
        }
    }
}
