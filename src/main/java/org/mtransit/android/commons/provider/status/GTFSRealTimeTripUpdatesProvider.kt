package org.mtransit.android.commons.provider.status

import android.content.Context
import android.util.Log
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SecurityUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.isIGNORE_DIRECTION
import org.mtransit.android.commons.provider.gtfs.GtfsRealTimeStorage
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optArrival
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDelay
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDeparture
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDirectionId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optScheduleRelationship
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopSequence
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopTimeUpdateList
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimeInstant
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toTripUpdates
import org.mtransit.android.commons.provider.gtfs.agencyTag
import org.mtransit.android.commons.provider.gtfs.getRDS
import org.mtransit.android.commons.provider.gtfs.getRDSSchedule
import org.mtransit.android.commons.provider.gtfs.getTargetUUIDs
import org.mtransit.android.commons.provider.gtfs.getTripIds
import org.mtransit.android.commons.provider.gtfs.makeRequest
import org.mtransit.android.commons.provider.gtfs.parseRouteId
import org.mtransit.android.commons.provider.gtfs.parseStopId
import org.mtransit.android.commons.provider.gtfs.parseTripId
import org.mtransit.android.commons.provider.status.StatusProvider.cacheAllStatusesBulkLockDB
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import com.google.transit.realtime.GtfsRealtime.FeedMessage as GFeedMessage
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship as GTDScheduleRelationship
import com.google.transit.realtime.GtfsRealtime.TripUpdate as GTripUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent as GTUStopTimeEvent
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate as GTUStopTimeUpdate

object GTFSRealTimeTripUpdatesProvider {

    val PROVIDER_PRECISION_IN_MS = 10.seconds.inWholeMilliseconds

    val TRIP_UPDATE_MAX_VALIDITY_IN_MS = 1.hours.inWholeMilliseconds

    val TRIP_UPDATE_VALIDITY_IN_MS = 10.minutes.inWholeMilliseconds
    val TRIP_UPDATE_VALIDITY_IN_FOCUS_IN_MS = 10.seconds.inWholeMilliseconds

    @Suppress("unused")
    val TRIP_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = 3.minutes.inWholeMilliseconds

    @Suppress("unused")
    val TRIP_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = 1.minutes.inWholeMilliseconds

    @Suppress("unused")
    @JvmStatic
    fun GTFSRealTimeProvider.getMinDurationBetweenRefreshInMs(inFocus: Boolean) =
        if (inFocus) TRIP_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS.adaptForCachedAPI(this.context)
        else TRIP_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS.adaptForCachedAPI(this.context)

    @JvmStatic
    fun GTFSRealTimeProvider.getValidityInMs(inFocus: Boolean) =
        if (inFocus) TRIP_UPDATE_VALIDITY_IN_FOCUS_IN_MS.adaptForCachedAPI(this.context)
        else TRIP_UPDATE_VALIDITY_IN_MS.adaptForCachedAPI(this.context)

    @JvmStatic
    val GTFSRealTimeProvider.maxValidityInMs: Long get() = TRIP_UPDATE_MAX_VALIDITY_IN_MS.adaptForCachedAPI(this.context)

    private fun Long.adaptForCachedAPI(context: Context?) =
        if (context?.let { GTFSRealTimeProvider.getAGENCY_TRIP_UPDATES_URL_CACHED(it) }?.isNotBlank() == true) {
            this * 2L // fewer calls to Cached API $$
        } else this

    @JvmStatic
    fun GTFSRealTimeProvider.getCached(statusFilter: StatusProviderContract.Filter): POIStatus? {
        val filter = statusFilter as? Schedule.ScheduleStatusFilter ?: run {
            MTLog.w(this, "getNewStatus() > Can't find new schedule without schedule filter!")
            return null
        }
        // return (statusFilter as? Schedule.ScheduleStatusFilter)?.let { filter ->
        // (
        return filter.routeDirectionStop.getTargetUUIDs(this, includeStopTags = true)
            // ?: filter.routeDirection?.getTargetUUIDs(this, includeAgencyTag = INCLUDE_AGENCY_TAG)
            // ?: filter.route?.getTargetUUIDs(this, includeAgencyTag = INCLUDE_AGENCY_TAG))
            .let { targetUUIDs ->
                val tripIds = filter.targetAuthority.let { targetAuthority ->
                    filter.routeId.let { routeId ->
                        context?.getTripIds(targetAuthority, routeId, filter.directionId)
                    }
                }
                tripIds
                    ?.takeIf { tripIds -> tripIds.isNotEmpty() } // trip IDs REQUIRED for GTFS Vehicle locations
                    ?.let { tripIds -> targetUUIDs to tripIds }
            }?.let { (targetUUIDs, tripIds) ->
                getCached(targetUUIDs, tripIds)
                    ?: makeCachedStatusFromAgencyData(filter, tripIds)
            }
    }

    val GTFSRealTimeProvider.ignoreDirection get() = isIGNORE_DIRECTION(this.requireContextCompat())

    private fun GTFSRealTimeProvider.makeCachedStatusFromAgencyData(filter: Schedule.ScheduleStatusFilter, tripIds: List<String>): POIStatus? {
        val context = context ?: return null
        try {
            val rds = filter.routeDirectionStop
            val targetAuthority = filter.targetAuthority
            val routeId = rds.route.id
            val directionId = rds.direction.id
            var sortedRDS: List<RouteDirectionStop>? = null
            var uuidSchedule: Map<String, Schedule?>? = null
            val gFeedMessage = GFeedMessage.parseFrom(File(context.cacheDir, GTFS_RT_TRIP_UPDATE_PB_FILE_NAME).inputStream())
            val gTripUpdates = gFeedMessage.entityList.toTripUpdates()
            val rdTripUpdates = gTripUpdates.mapNotNull { gTripUpdate ->
                gTripUpdate.optTrip?.let { it to gTripUpdate }
            }.filter { (trip, _) ->
                parseTripId(trip)?.let { tripId ->
                    if (tripId !in tripIds) return@filter false
                }
                parseRouteId(trip)?.let { routeIdHash ->
                    if (routeIdHash != rds.route.originalIdHash.toString()) return@filter false
                }
                trip.optDirectionId?.takeIf { !ignoreDirection }?.let { directionId ->
                    if (directionId != rds.direction.originalDirectionIdOrNull) return@filter false
                }
                return@filter true
            }.takeIf { it.isNotEmpty() }
            rdTripUpdates ?: return null
            if (sortedRDS == null) {
                sortedRDS = context.getRDS(this.authority, routeId, directionId)
            }
            if (uuidSchedule == null) {
                uuidSchedule =
                    sortedRDS
                        ?.let { rdsList ->
                            context
                                .getRDSSchedule(targetAuthority, rdsList.map { it.uuid })
                                ?.associate {
                                    it.targetUUID to it
                                }
                        }
            }
            if (true) {
                uuidSchedule ?: return null
                sortedRDS ?: return null
                wip(rdTripUpdates, uuidSchedule, sortedRDS)
                return null
            }
            rdTripUpdates.forEach { (trip, gTripUpdate) ->
                val updatedTripID = parseTripId(trip) ?: return@forEach
                val stopTimeUpdates = gTripUpdate.optStopTimeUpdateList?.sortedBy { it.optStopSequence }
                    ?: return@forEach
                val targetUuidOnThisTrip = uuidSchedule
                    ?.filter { (_, schedule) -> schedule?.timestamps?.any { it.tripId == updatedTripID } == true }
                    ?: return@forEach
                val sortedRDSOnThisTrip = sortedRDS
                    ?.filter { rds -> targetUuidOnThisTrip.contains(rds.uuid) }
                    ?: return@forEach
                var currentStopIdHash: String? = null
                var currentStopSequence: Int? = null
                var currentStopTimeIndex: Int = 0
                var currentStopTimeUpdate = stopTimeUpdates?.getOrNull(currentStopTimeIndex)
                var nextStopTimeUpdate = stopTimeUpdates?.getOrNull(currentStopTimeIndex + 1)
                if (true) {
                    var rdsI = 0
                    var stuI = 0
                    var currentRDS: RouteDirectionStop? = sortedRDSOnThisTrip.getOrNull(rdsI) ?: return@forEach
                    var currentStopTimeUpdate: GTUStopTimeUpdate = stopTimeUpdates.getOrNull(stuI) ?: return@forEach
                    var nextStopTimeUpdate = stopTimeUpdates.getOrNull(stuI + 1)
                    while (currentRDS != null
                        && !isSameStop(currentRDS, currentStopTimeUpdate)
                        && rdsI <= sortedRDSOnThisTrip.size // we do want NULL
                    ) {
                        currentRDS = sortedRDSOnThisTrip.getOrNull(++rdsI)
                    }
                    currentRDS ?: return@forEach // no match
                    // 1st trip stop matching 1st stop time update found
                    var currentRDSTripTimestamp =
                        targetUuidOnThisTrip[currentRDS.uuid]?.timestamps?.singleOrNull { it.tripId == updatedTripID }
                            ?: return@forEach
                    var (currentArrivalDelay, currentDepartureDelay) = getDelay(currentStopTimeUpdate, currentRDSTripTimestamp)
                    applyDelay(currentRDSTripTimestamp, currentArrivalDelay, currentDepartureDelay)
                    currentArrivalDelay = null // only once for the matching stop
                    currentRDS = sortedRDSOnThisTrip.getOrNull(++rdsI) // NEXT ONE
                        ?: return@forEach // no more stop
                    while (currentRDS != null && nextStopTimeUpdate != null
                        && !isSameStop(currentRDS, nextStopTimeUpdate)
                    ) {
                        // keep using current
                        currentRDSTripTimestamp =
                            targetUuidOnThisTrip[currentRDS.uuid]?.timestamps?.singleOrNull { it.tripId == updatedTripID }
                                ?: continue // FIXME???
                        applyDelay(currentRDSTripTimestamp, currentArrivalDelay, currentDepartureDelay)
                        currentRDS = sortedRDSOnThisTrip.getOrNull(++rdsI) // NEXT ONE
                    }
                    currentRDS ?: return@forEach // no more RDS

                    currentStopTimeUpdate = stopTimeUpdates.getOrNull(++stuI) ?: return@forEach
                    nextStopTimeUpdate = stopTimeUpdates.getOrNull(stuI + 1)
                    getDelay(currentStopTimeUpdate, currentRDSTripTimestamp).let {
                        currentArrivalDelay = it.first
                        currentDepartureDelay = it.second
                    }
                    return@forEach
                }
                var generatedStopSequence = 1
                sortedRDSOnThisTrip.forEach { rds ->
                    generatedStopSequence++
                    currentStopIdHash = rds.stop.originalIdHashString
                    currentStopSequence = generatedStopSequence
                    if (false) {
                        findCurrentNextStopTimeUpdate(sortedRDSOnThisTrip, stopTimeUpdates, currentStopIdHash, currentStopSequence, currentStopTimeIndex).let {
                            currentStopTimeUpdate = it.first
                            nextStopTimeUpdate = it.second
                        }
                        currentStopTimeUpdate?.optStopSequence?.let { currentStopTimeUpdateStopSequence ->
                            when {
                                currentStopSequence < currentStopTimeUpdateStopSequence -> return@forEach // no real-time info yet
                                currentStopSequence > currentStopTimeUpdateStopSequence -> {
                                    nextStopTimeUpdate?.optStopSequence?.let { nextStopTimeUpdateStopSequence ->
                                        if (currentStopSequence < nextStopTimeUpdateStopSequence) {
                                            // keep current stop time update
                                        } else {
                                            currentStopTimeIndex++
                                            currentStopTimeUpdate = stopTimeUpdates?.getOrNull(currentStopTimeIndex)
                                            nextStopTimeUpdate = stopTimeUpdates?.getOrNull(currentStopTimeIndex + 1)
                                        }
                                    } // ELSE keep current stop time update
                                }

                                else -> currentStopTimeIndex = 0
                            }
                        }
                    }


                }
            }
            return null
        } catch (e: Exception) {
            MTLog.w(this, e, "makeCachedStatusFromAgencyData() > error!")
            return null
        }
    }

    fun getDelay(
        stopTimeUpdate: GTUStopTimeUpdate?,
        timestamp: Schedule.Timestamp,
        previousDelays: Pair<Duration?, Duration?> = null to null,
    ): Pair<Duration?, Duration?> {
        stopTimeUpdate ?: return null to null // no delay info // show static schedule info
        when (stopTimeUpdate.optScheduleRelationship) {
            null, // DEFAULT
            GTUStopTimeUpdate.ScheduleRelationship.SCHEDULED -> {
            } // DO NOTHING
            GTUStopTimeUpdate.ScheduleRelationship.NO_DATA -> {
                // keep static, forget current stop time update
                return null to null // no delay info // show static schedule info
            }

            GTUStopTimeUpdate.ScheduleRelationship.SKIPPED -> {
                // TODO remove trip timestamp (stop will not be stopped ad)
                MTLog.w(this@GTFSRealTimeTripUpdatesProvider, "Unexpected stop time schedule relationship: SKIPPED")
                // return null // stop will be skipped
                return previousDelays
            }

            GTUStopTimeUpdate.ScheduleRelationship.UNSCHEDULED -> { // only with frequency based schedule
                MTLog.w(this@GTFSRealTimeTripUpdatesProvider, "Unexpected stop time schedule relationship: UNSCHEDULED")
            }
        }
        val timestampOriginalArrival = timestamp.arrival
        val timestampOriginalDeparture = timestamp.departure
        var departureDelay: Duration? = stopTimeUpdate.optDeparture?.makeDelay(timestampOriginalDeparture)
        val arrivalDelay: Duration? = stopTimeUpdate.optArrival?.makeDelay(timestampOriginalArrival)
        if (departureDelay == null && arrivalDelay != null) {
            departureDelay = timestampOriginalDeparture.coerceAtLeast(timestampOriginalArrival + arrivalDelay) - timestampOriginalDeparture
        }
        return arrivalDelay to departureDelay
    }

    fun applyDelay(
        timestamp: Schedule.Timestamp,
        arrivalDelay: Duration?,
        departureDelay: Duration?,
    ) = timestamp.apply {
        departureDelay?.let { departure += it } // 1st
        arrivalDelay?.let { arrival += it } // 2nd
    }

    fun applyUpdate(
        timestamp: Schedule.Timestamp,
        currentStopTimeUpdate: GTUStopTimeUpdate?
    ): Schedule.Timestamp? {
        currentStopTimeUpdate ?: return timestamp // no change
        when (currentStopTimeUpdate.optScheduleRelationship) {
            null, // DEFAULT
            GTUStopTimeUpdate.ScheduleRelationship.SCHEDULED -> {
            } // DO NOTHING
            GTUStopTimeUpdate.ScheduleRelationship.NO_DATA -> {
                // keep static, forget current stop time update
                return timestamp // no change
            }

            GTUStopTimeUpdate.ScheduleRelationship.SKIPPED -> {
                // TODO remove trip timestamp (stop will not be stopped ad)
                MTLog.w(this@GTFSRealTimeTripUpdatesProvider, "Unexpected stop time schedule relationship: SKIPPED")
                return null // stop will be skipped
            }

            GTUStopTimeUpdate.ScheduleRelationship.UNSCHEDULED -> { // only with frequency based schedule
                MTLog.w(this@GTFSRealTimeTripUpdatesProvider, "Unexpected stop time schedule relationship: UNSCHEDULED")
            }
        }
        val timestampOriginalArrival = timestamp.arrivalT.millisToInstant()
        val timestampOriginalDeparture = timestamp.departureT.millisToInstant()
        val departureDelay: Duration? = currentStopTimeUpdate.optDeparture?.makeDelay(timestamp.departureT.millisToInstant())
        val arrivalDelay: Duration? = currentStopTimeUpdate.optArrival?.makeDelay(timestamp.arrivalT.millisToInstant())

        TODO()
    }

    private fun GTUStopTimeEvent.makeDelay(originalTime: Instant): Duration? =
        optDelay?.seconds
            ?: optTimeInstant?.let { time -> time - originalTime }

    fun GTFSRealTimeProvider.findCurrentNextStopTimeUpdate(
        sortedRDS: List<RouteDirectionStop>,
        stopTimeUpdates: List<GTUStopTimeUpdate>?,
        currentStopIdHash: String?,
        currentStopSequence: Int,
        currentStopTimeIndex: Int,
    ): Pair<GTUStopTimeUpdate?, GTUStopTimeUpdate?> {
        var currentStopTimeIndex = currentStopTimeIndex
        var currentStopTimeUpdate = stopTimeUpdates?.getOrNull(currentStopTimeIndex)
        var nextStopTimeUpdate = stopTimeUpdates?.getOrNull(currentStopTimeIndex + 1)
        currentStopTimeUpdate?.let {

        }
        if (false) { // TODO later stop sequence
            currentStopTimeUpdate?.optStopSequence?.let { currentStopTimeUpdateStopSequence ->
                while (true) {
                    if (currentStopSequence < currentStopTimeUpdateStopSequence) {
                        return null to null // no real-time info yet
                    }
                    if (currentStopSequence == currentStopTimeUpdateStopSequence) {
                        return currentStopTimeUpdate to nextStopTimeUpdate // use
                    }
                    if (currentStopSequence > currentStopTimeUpdateStopSequence) {
                        nextStopTimeUpdate?.optStopSequence?.let { nextStopTimeUpdateStopSequence ->
                            if (currentStopSequence < nextStopTimeUpdateStopSequence) {
                                return currentStopTimeUpdate to nextStopTimeUpdate // keep same
                            } else if (currentStopSequence == nextStopTimeUpdateStopSequence) {
                                currentStopTimeIndex++
                                currentStopTimeUpdate = stopTimeUpdates?.getOrNull(currentStopTimeIndex)
                                nextStopTimeUpdate = stopTimeUpdates?.getOrNull(currentStopTimeIndex + 1)
                                // continue
                                return currentStopTimeUpdate to nextStopTimeUpdate // use next
                            } else {
                                currentStopTimeIndex++
                            }
                        }
                    }
                }
            }
        }
        TODO("Not yet implemented")
    }

    fun GTFSRealTimeProvider.getStopTimeUpdateSequence(
        sortedRDS: List<RouteDirectionStop>,
        stopTimeUpdate: GTUStopTimeUpdate
    ): Int? {
        val providedStopSequence = stopTimeUpdate.optStopSequence
        val providedStopIdHash = parseStopId(stopTimeUpdate)
        // .optStopId?.originalIdToHash(stopIdCleanupPattern)
            ?: return providedStopSequence
        if (providedStopSequence == null) {
            return sortedRDS.indexOfFirst { rds -> isSameStop(rds, stopTimeUpdate) }
        }
        // providedStopSequence?.let {
        // return it
        // }
        // TODO HERE NOW, it's complicated, trip stop sequence can be a mess
        // TODO: only guarantee is stop order... if stop not repeated in same trip
        // TODO -> maybe start simple first by using stop ID if available then stop sequence and ignore complex use case data for now
        var iRDS = 0
        var generatedStopSequence = 1
        while (iRDS < sortedRDS.size) {
            // for (; iRDS < sortedRDS.size; iRDS++) {
            val currentRDS = sortedRDS[iRDS]
            if (isSameStop(currentRDS, stopTimeUpdate)) {
                if (generatedStopSequence == providedStopSequence) {
                    return generatedStopSequence
                }
                generatedStopSequence++
            } else {
                break
            }
        }
        return null
    }

    fun GTFSRealTimeProvider.getCached(targetUUIDs: Map<String, String>, tripIds: List<String>): POIStatus? {
        return getCachedStatusS(targetUUIDs.keys, tripIds)
    }

    @JvmStatic
    fun GTFSRealTimeProvider.getNew(statusFilter: StatusProviderContract.Filter): POIStatus? {
        val filter = statusFilter as? Schedule.ScheduleStatusFilter ?: run {
            MTLog.w(this, "getNewStatus() > Can't find new schedule without schedule filter!")
            return null
        }
        updateAgencyDataIfRequired(filter.isInFocusOrDefault)
        return getCached(filter)
    }

    private fun GTFSRealTimeProvider.updateAgencyDataIfRequired(inFocus: Boolean) {
        val context = requireContextCompat()
        var inFocus = inFocus
        val lastUpdateInMs = GtfsRealTimeStorage.getTripUpdateLastUpdateMs(context, 0L)
        val lastUpdateCode = GtfsRealTimeStorage.getTripUpdateLastUpdateCode(context, -1).takeIf { it >= 0 }
        if (lastUpdateCode != null && lastUpdateCode != HttpURLConnection.HTTP_OK) {
            inFocus = true // force earlier retry if last fetch returned HTTP error
        }
        val minUpdateMs = min(statusMaxValidityInMs, getStatusValidityInMs(inFocus))
        val nowInMs = TimeUtils.currentTimeMillis()
        if (lastUpdateInMs + minUpdateMs > nowInMs) {
            return
        }
        updateAgencyDataIfRequiredSync(lastUpdateInMs, inFocus)
    }

    @Synchronized
    private fun GTFSRealTimeProvider.updateAgencyDataIfRequiredSync(lastUpdateInMs: Long, inFocus: Boolean) {
        val context = requireContextCompat()
        if (GtfsRealTimeStorage.getTripUpdateLastUpdateMs(context, 0L) > lastUpdateInMs) {
            return  // too late, another thread already updated
        }
        val nowInMs = TimeUtils.currentTimeMillis()
        var deleteAllRequired = false
        if (lastUpdateInMs + statusMaxValidityInMs < nowInMs) {
            deleteAllRequired = true // too old to display
        }
        val minUpdateMs = min(statusMaxValidityInMs, getStatusValidityInMs(inFocus))
        if (deleteAllRequired || lastUpdateInMs + minUpdateMs < nowInMs) {
            updateAllAgencyDataFromWWW(context, deleteAllRequired) // try to update
        }
    }

    private fun GTFSRealTimeProvider.updateAllAgencyDataFromWWW(context: Context, deleteAllRequired: Boolean) {
        var deleteAllDone = false
        if (deleteAllRequired) {
            deleteAllCachedStatus()
            deleteAllDone = true
        }
        val newStatuses = loadAgencyDataFromWWW(context)
        if (newStatuses != null) { // empty is OK
            if (!deleteAllDone) {
                deleteAllCachedStatus()
            }
            cacheAllStatusesBulkLockDB(this, newStatuses)
        } // else keep whatever we have until max validity reached
    }

    private const val GTFS_RT_TRIP_UPDATE_PB_FILE_NAME = "gtfs_rt_trip_update.pb"

    private fun GTFSRealTimeProvider.loadAgencyDataFromWWW(context: Context): List<POIStatus>? {
        try {
            val urlRequest = makeRequest(
                context,
                urlCachedString = GTFSRealTimeProvider.getAGENCY_VEHICLE_POSITIONS_URL_CACHED(context),
                getUrlString = { token -> GTFSRealTimeProvider.getAgencyTripUpdatesUrlString(context, token) }
            ) ?: return null
            getOkHttpClient(context).newCall(urlRequest).execute().use { response ->
                GtfsRealTimeStorage.saveTripUpdateLastUpdateCode(context, response.code)
                GtfsRealTimeStorage.saveTripUpdateLastUpdateMs(context, TimeUtils.currentTimeMillis())
                when (response.code) {
                    HttpURLConnection.HTTP_OK -> {
                        val statuses = mutableListOf<POIStatus>()
                        try {
                            try {
                                File(context.cacheDir, GTFS_RT_TRIP_UPDATE_PB_FILE_NAME).writeBytes(response.body.bytes())
                            } catch (e: IOException) {
                                MTLog.w(this@GTFSRealTimeTripUpdatesProvider, e, "loadAgencyDataFromWWW() > error while saving GTFS RT Trip Updates data!")
                            }
                            return null
                        } catch (e: Exception) {
                            MTLog.w(this@GTFSRealTimeTripUpdatesProvider, e, "loadAgencyDataFromWWW() > error while parsing GTFS Real Time data!")
                        }
                        MTLog.i(this@GTFSRealTimeTripUpdatesProvider, "Found %d vehicle locations.", statuses.size)
                        if (Constants.DEBUG) {
                            for (schedule in statuses) {
                                MTLog.d(this@GTFSRealTimeTripUpdatesProvider, "loadAgencyDataFromWWW() > - new $schedule.")
                            }
                        }
                        return statuses
                    }

                    else -> {
                        MTLog.w(
                            this@GTFSRealTimeTripUpdatesProvider,
                            "ERROR: HTTP URL-Connection Response Code ${response.code} (Message: ${response.message})"
                        )
                        return null
                    }
                }
            }
        } catch (sslhe: SSLHandshakeException) {
            MTLog.w(this, sslhe, "SSL error!")
            SecurityUtils.logCertPathValidatorException(sslhe)
            GtfsRealTimeStorage.saveTripUpdateLastUpdateCode(context, 567) // SSL certificate not trusted (on this device)
            GtfsRealTimeStorage.saveTripUpdateLastUpdateMs(context, TimeUtils.currentTimeMillis())
            return null
        } catch (uhe: UnknownHostException) {
            if (MTLog.isLoggable(Log.DEBUG)) {
                MTLog.w(this@GTFSRealTimeTripUpdatesProvider, uhe, "No Internet Connection!")
            } else {
                MTLog.w(this@GTFSRealTimeTripUpdatesProvider, "No Internet Connection!")
            }
            return null
        } catch (se: SocketException) {
            MTLog.w(this@GTFSRealTimeTripUpdatesProvider, se, "No Internet Connection!")
            return null
        } catch (e: Exception) { // Unknown error
            MTLog.e(this@GTFSRealTimeTripUpdatesProvider, e, "INTERNAL ERROR: Unknown Exception")
            return null
        }
    }

    private fun GTFSRealTimeProvider.processTripUpdates(
        newLastUpdateInMs: Long,
        gTripUpdate: GTripUpdate,
        ignoreDirection: Boolean,
    ): Set<POIStatus>? {
        val updateRouteId = gTripUpdate.optTrip?.let { parseRouteId(it) }
        val updateDirectionId = gTripUpdate.optTrip?.optDirectionId
            ?.takeIf { !ignoreDirection }
        val updatedTripId = gTripUpdate.optTrip?.let { parseTripId(it) }
        gTripUpdate.optDelay?.let {
            // experimental field, means all stop times are delayed
            // -> fetch all trips stops static schedule and generate real-time schedule with delay
        }
        gTripUpdate.optStopTimeUpdateList?.forEach { stopTimeUpdate ->
            stopTimeUpdate.optStopId?.let { stopId ->
                // val targetUUIDs = RouteDirectionStop.makeUUID()
            } ?: run {
                // NO STOP ID provided > not supported, original trip ID "stop sequence" is not in the local DB!
            }
        }
        val targetUUIDs = parseProviderTargetUUID(gTripUpdate, ignoreDirection)?.takeIf { it.isNotBlank() } ?: return null
        return setOf(
            Schedule(
                null,
                targetUUIDs,
                newLastUpdateInMs,
                maxValidityInMs,
                newLastUpdateInMs,
                PROVIDER_PRECISION_IN_MS,
                false, // noPickup
                null, // sourceLabel
                false // no data
                //
                // authority = this.authority,
                // targetUUID = targetUUIDs,
                // targetTripId = gTripUpdate.optTrip?.optTripId?.originalIdToId(tripIdCleanupPattern),
                // lastUpdateInMs = newLastUpdateInMs,
                // maxValidityInMs = this@processTripUpdates.vehicleLocationMaxValidityInMs,
                // //
                // vehicleId = gTripUpdate.optVehicle?.optId,
                // vehicleLabel = gTripUpdate.optVehicle?.optLabel,
                // reportTimestamp = gTripUpdate.optTimestamp?.secsToInstant(),
                // latitude = gTripUpdate.optPosition?.optLatitude ?: return null,
                // longitude = gTripUpdate.optPosition?.optLongitude ?: return null,
                // bearingDegrees = gTripUpdate.optPosition?.optBearing?.toInt(), // in degrees
                // speedMetersPerSecond = gTripUpdate.optPosition?.optSpeed?.toInt(), // in meters per second
            )
        )
    }

    private fun GTFSRealTimeProvider.parseProviderTargetUUID(gTripUpdate: GTripUpdate, ignoreDirection: Boolean): String? {
        val gTripDescriptor = gTripUpdate.optTrip ?: return null
        if (gTripDescriptor.hasModifiedTrip()) {
            MTLog.d(this, "parseTargetUUID() > unhandled modified trip: ${gTripDescriptor.toStringExt()}")
        }
        if (gTripDescriptor.hasStartTime() || gTripDescriptor.hasStartDate()) {
            MTLog.d(this, "parseTargetUUID() > unhandled start date & time: ${gTripDescriptor.toStringExt()}")
        }
        when (gTripDescriptor.scheduleRelationship) {
            GTDScheduleRelationship.SCHEDULED -> {} // handled
            GTDScheduleRelationship.ADDED,
            GTDScheduleRelationship.UNSCHEDULED,
            GTDScheduleRelationship.CANCELED,
            GTDScheduleRelationship.REPLACEMENT,
            GTDScheduleRelationship.DUPLICATED,
            GTDScheduleRelationship.DELETED,
            GTDScheduleRelationship.NEW,
                -> MTLog.d(this, "parseTargetUUID() > unhandled schedule relationship: ${gTripDescriptor.scheduleRelationship}")
        }
        parseRouteId(gTripDescriptor)?.let { routeId ->
            gTripDescriptor.optDirectionId?.takeIf { !ignoreDirection }?.let { directionId ->
                return getAgencyRouteDirectionTagTargetUUID(agencyTag, routeId, directionId)
            }
            return getAgencyRouteTagTargetUUID(agencyTag, routeId)
        }
        return getAgencyTagTargetUUID(agencyTag)
    }

    private fun GTFSRealTimeProvider.isSameStop(rds: RouteDirectionStop, stopTimeUpdate: GTUStopTimeUpdate) =
        rds.stop.isSameOriginalId(parseStopId(stopTimeUpdate))
}
