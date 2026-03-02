package org.mtransit.android.commons.provider.status

import android.content.Context
import android.util.Log
import com.google.transit.realtime.GtfsRealtime
import com.google.transit.realtime.GtfsRealtime.FeedMessage
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SecurityUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyTagTargetUUID
import org.mtransit.android.commons.provider.gtfs.GtfsRealTimeStorage
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optBearing
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDirectionId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optLabel
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optLatitude
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optLongitude
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optPosition
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optRouteId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optSpeed
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimestamp
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optVehicle
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.originalIdToHash
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.originalIdToId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.sortTripUpdates
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toTripUpdates
import org.mtransit.android.commons.provider.gtfs.agencyTag
import org.mtransit.android.commons.provider.gtfs.getTargetUUIDs
import org.mtransit.android.commons.provider.gtfs.getTripIds
import org.mtransit.android.commons.provider.gtfs.makeRequest
import org.mtransit.android.commons.provider.gtfs.routeIdCleanupPattern
import org.mtransit.android.commons.provider.gtfs.tripIdCleanupPattern
import org.mtransit.android.commons.provider.status.StatusProvider.cacheAllStatusesBulkLockDB
import org.mtransit.android.commons.secsToInstant
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object GTFSRealTimeTripUpdatesProvider {

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
        return filter.routeDirectionStop.getTargetUUIDs(this)
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
            }
    }

    fun GTFSRealTimeProvider.getCached(targetUUIDs: Map<String, String>, tripIds: List<String>): POIStatus? =
        // buildList {
        getCachedStatusS(this, targetUUIDs.keys, tripIds)
            //     ?.let {
            //     add(it)
            // }
            // }
            ?.let { it.apply { targetUUID = targetUUIDs[it.targetUUID] ?: targetUUID } }

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
                        val newLastUpdateInMs = TimeUtils.currentTimeMillis()
                        val statuses = mutableListOf<POIStatus>()
                        val ignoreDirection = GTFSRealTimeProvider.isIGNORE_DIRECTION(context)
                        try {
                            val gFeedMessage = FeedMessage.parseFrom(response.body.bytes())
                            val gTripUpdates = gFeedMessage.entityList.toTripUpdates()
                            for (gTripUpdate in gTripUpdates.sortTripUpdates(newLastUpdateInMs)) {
                                if (Constants.DEBUG) {
                                    MTLog.d(
                                        this@GTFSRealTimeTripUpdatesProvider,
                                        "loadAgencyDataFromWWW() > GTFS trip updates: ${gTripUpdate.toStringExt()}."
                                    )
                                }
                                processTripUpdates(newLastUpdateInMs, gTripUpdate, ignoreDirection)
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.let {
                                        statuses.addAll(it)
                                    }
                            }
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
        gTripUpdate: GtfsRealtime.TripUpdate,
        ignoreDirection: Boolean,
    ): Set<POIStatus>? {
        val targetUUIDs = parseProviderTargetUUID(gTripUpdate, ignoreDirection)?.takeIf { it.isNotBlank() } ?: return null
        return setOf(
            Schedule(
                authority = this.authority,
                targetUUID = targetUUIDs,
                targetTripId = gTripUpdate.optTrip?.optTripId?.originalIdToId(tripIdCleanupPattern),
                lastUpdateInMs = newLastUpdateInMs,
                maxValidityInMs = this@processVehiclePositions.vehicleLocationMaxValidityInMs,
                //
                vehicleId = gTripUpdate.optVehicle?.optId,
                vehicleLabel = gTripUpdate.optVehicle?.optLabel,
                reportTimestamp = gTripUpdate.optTimestamp?.secsToInstant(),
                latitude = gTripUpdate.optPosition?.optLatitude ?: return null,
                longitude = gTripUpdate.optPosition?.optLongitude ?: return null,
                bearingDegrees = gTripUpdate.optPosition?.optBearing?.toInt(), // in degrees
                speedMetersPerSecond = gTripUpdate.optPosition?.optSpeed?.toInt(), // in meters per second
            )
        )
    }

    private fun GTFSRealTimeProvider.parseProviderTargetUUID(gTripUpdate: GtfsRealtime.TripUpdate, ignoreDirection: Boolean): String? {
        val gTripDescriptor = gTripUpdate.optTrip ?: return null
        if (gTripDescriptor.hasModifiedTrip()) {
            MTLog.d(this, "parseTargetUUID() > unhandled modified trip: ${gTripDescriptor.toStringExt()}")
        }
        if (gTripDescriptor.hasStartTime() || gTripDescriptor.hasStartDate()) {
            MTLog.d(this, "parseTargetUUID() > unhandled start date & time: ${gTripDescriptor.toStringExt()}")
        }
        when (gTripDescriptor.scheduleRelationship) {
            GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED -> {} // handled
            GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED,
            GtfsRealtime.TripDescriptor.ScheduleRelationship.UNSCHEDULED,
            GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED,
            GtfsRealtime.TripDescriptor.ScheduleRelationship.REPLACEMENT,
            GtfsRealtime.TripDescriptor.ScheduleRelationship.DUPLICATED,
            GtfsRealtime.TripDescriptor.ScheduleRelationship.DELETED,
            GtfsRealtime.TripDescriptor.ScheduleRelationship.NEW,
                -> MTLog.d(this, "parseTargetUUID() > unhandled schedule relationship: ${gTripDescriptor.scheduleRelationship}")
        }
        gTripDescriptor.optRouteId?.originalIdToHash(routeIdCleanupPattern)?.let { routeId ->
            gTripDescriptor.optDirectionId?.takeIf { !ignoreDirection }?.let { directionId ->
                return getAgencyRouteDirectionTagTargetUUID(agencyTag, routeId, directionId)
            }
            return getAgencyRouteTagTargetUUID(agencyTag, routeId)
        }
        return getAgencyTagTargetUUID(agencyTag)
    }
}
