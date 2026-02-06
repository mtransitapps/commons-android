package org.mtransit.android.commons.provider.vehiclelocations

import android.content.Context
import com.google.transit.realtime.GtfsRealtime
import com.google.transit.realtime.GtfsRealtime.FeedMessage
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyTagTargetUUID
import org.mtransit.android.commons.provider.gtfs.GtfsRealTimeStorage
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optBearing
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optLabel
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optLatitude
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optLongitude
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optPosition
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optSpeed
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimestamp
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optVehicle
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.originalIdToHash
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.originalIdToId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.sortVehicles
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toVehicles
import org.mtransit.android.commons.provider.gtfs.getTripsIds
import org.mtransit.android.commons.provider.gtfs.makeRequest
import org.mtransit.android.commons.provider.vehiclelocations.VehicleLocationProvider.Companion.getCachedVehicleLocationsS
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.commons.FeatureFlags
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.UnknownHostException
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object GTFSRealTimeVehiclePositionsProvider {

    val VEHICLE_LOCATION_MAX_VALIDITY_IN_MS = 1.hours.inWholeMilliseconds

    val VEHICLE_LOCATION_VALIDITY_IN_MS = 10.minutes.inWholeMilliseconds
    val VEHICLE_LOCATION_VALIDITY_IN_FOCUS_IN_MS = 10.seconds.inWholeMilliseconds

    @Suppress("unused")
    val VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_MS = 3.minutes.inWholeMilliseconds

    @Suppress("unused")
    val VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = 1.minutes.inWholeMilliseconds

    @Suppress("unused")
    @JvmStatic
    fun GTFSRealTimeProvider.getMinDurationBetweenRefreshInMs(inFocus: Boolean) =
        if (inFocus) VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS.adaptForCachedAPI(this.context)
        else VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_MS.adaptForCachedAPI(this.context)

    @JvmStatic
    fun GTFSRealTimeProvider.getValidityInMs(inFocus: Boolean) =
        if (inFocus) VEHICLE_LOCATION_VALIDITY_IN_FOCUS_IN_MS.adaptForCachedAPI(this.context)
        else VEHICLE_LOCATION_VALIDITY_IN_MS.adaptForCachedAPI(this.context)

    @JvmStatic
    val GTFSRealTimeProvider.maxValidityInMs: Long get() = VEHICLE_LOCATION_MAX_VALIDITY_IN_MS.adaptForCachedAPI(this.context)

    private fun Long.adaptForCachedAPI(context: Context?) =
        if (context?.let { GTFSRealTimeProvider.getAGENCY_VEHICLE_POSITIONS_URL_CACHED(it) }?.isNotBlank() == true) {
            this * 2L // less calls to Cached API $$
        } else this

    @JvmStatic
    fun GTFSRealTimeProvider.getCached(filter: VehicleLocationProviderContract.Filter) =
        ((filter.poi as? RouteDirectionStop)?.getTargetUUIDs(this)
            ?: filter.routeDirection?.getTargetUUIDs(this)
            ?: filter.route?.getTargetUUIDs(this))
            ?.let { targetUUIDs ->
                //noinspection DiscouragedApi TODO enable F_PROVIDER_READS_TRIP_ID_DIRECTLY
                var tripIds: List<String>? = filter.tripIds
                if (FeatureFlags.F_PROVIDER_READS_TRIP_ID_DIRECTLY) {
                    tripIds = filter.targetAuthority?.let { targetAuthority ->
                        filter.routeId?.let { routeId ->
                            context?.getTripsIds(targetAuthority, routeId, filter.directionId)
                        }
                    }
                }
                tripIds
                    ?.takeIf { tripIds -> tripIds.isNotEmpty() } // trip IDs REQUIRED for GTFS Vehicle locations
                    ?.let { tripIds -> targetUUIDs to tripIds }
            }?.let { (targetUUIDs, tripIds) ->
                getCached(targetUUIDs, tripIds)
            }

    private fun RouteDirectionStop.getTargetUUIDs(provider: GTFSRealTimeProvider) = buildMap {
        put(getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)), route.uuid)
        getAgencyRouteDirectionTagTargetUUID(provider.agencyTag, getRouteTag(provider), getDirectionTag(provider))?.let { put(it, routeDirectionUUID) }
    }

    private fun RouteDirection.getTargetUUIDs(provider: GTFSRealTimeProvider) = buildMap {
        put(getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)), route.uuid)
        getAgencyRouteDirectionTagTargetUUID(provider.agencyTag, getRouteTag(provider), getDirectionTag(provider))?.let { put(it, uuid) }
    }

    private fun Route.getTargetUUIDs(provider: GTFSRealTimeProvider) = mapOf(
        getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)) to uuid,
    )

    fun GTFSRealTimeProvider.getCached(targetUUIDs: Map<String, String>, tripIds: List<String>) = buildList {
        getCachedVehicleLocationsS(targetUUIDs.keys, tripIds)?.let {
            addAll(it)
        }
    }.map { it.copy(targetUUID = targetUUIDs[it.targetUUID] ?: it.targetUUID) }

    @JvmStatic
    fun GTFSRealTimeProvider.getNew(filter: VehicleLocationProviderContract.Filter): List<VehicleLocation>? {
        updateAgencyDataIfRequired(filter.inFocusOrDefault)
        return getCached(filter)
    }

    private fun GTFSRealTimeProvider.updateAgencyDataIfRequired(inFocus: Boolean) {
        val context = requireContextCompat()
        var inFocus = inFocus
        val lastUpdateInMs = GtfsRealTimeStorage.getVehicleLocationLastUpdateMs(context, 0L)
        val lastUpdateCode = GtfsRealTimeStorage.getVehicleLocationLastUpdateCode(context, -1).takeIf { it >= 0 }
        if (lastUpdateCode != null && lastUpdateCode != HttpURLConnection.HTTP_OK) {
            inFocus = true // force earlier retry if last fetch returned HTTP error
        }
        val minUpdateMs = min(vehicleLocationMaxValidityInMs, getVehicleLocationValidityInMs(inFocus))
        val nowInMs = TimeUtils.currentTimeMillis()
        if (lastUpdateInMs + minUpdateMs > nowInMs) {
            return
        }
        updateAgencyDataIfRequiredSync(lastUpdateInMs, inFocus)
    }

    @Synchronized
    private fun GTFSRealTimeProvider.updateAgencyDataIfRequiredSync(lastUpdateInMs: Long, inFocus: Boolean) {
        val context = requireContextCompat()
        if (GtfsRealTimeStorage.getVehicleLocationLastUpdateMs(context, 0L) > lastUpdateInMs) {
            return  // too late, another thread already updated
        }
        val nowInMs = TimeUtils.currentTimeMillis()
        var deleteAllRequired = false
        if (lastUpdateInMs + vehicleLocationMaxValidityInMs < nowInMs) {
            deleteAllRequired = true // too old to display
        }
        val minUpdateMs = min(vehicleLocationMaxValidityInMs, getVehicleLocationValidityInMs(inFocus))
        if (deleteAllRequired || lastUpdateInMs + minUpdateMs < nowInMs) {
            updateAllAgencyDataFromWWW(context, deleteAllRequired) // try to update
        }
    }

    private fun GTFSRealTimeProvider.updateAllAgencyDataFromWWW(context: Context, deleteAllRequired: Boolean) {
        var deleteAllDone = false
        if (deleteAllRequired) {
            deleteAllCachedVehicleLocations()
            deleteAllDone = true
        }
        val newVehicleLocations = loadAgencyDataFromWWW(context)
        if (newVehicleLocations != null) { // empty is OK
            if (!deleteAllDone) {
                deleteAllCachedVehicleLocations()
            }
            cacheVehicleLocations(newVehicleLocations)
        } // else keep whatever we have until max validity reached
    }

    private fun GTFSRealTimeProvider.loadAgencyDataFromWWW(context: Context): List<VehicleLocation>? {
        try {
            val urlRequest = makeRequest(
                context,
                urlCachedString = GTFSRealTimeProvider.getAGENCY_VEHICLE_POSITIONS_URL_CACHED(context),
                getUrlString = { token -> GTFSRealTimeProvider.getAgencyVehiclePositionsUrlString(context, token) }
            ) ?: return null
            getOkHttpClient(context).newCall(urlRequest).execute().use { response ->
                GtfsRealTimeStorage.saveVehicleLocationLastUpdateCode(context, response.code)
                GtfsRealTimeStorage.saveVehicleLocationLastUpdateMs(context, TimeUtils.currentTimeMillis())
                when (response.code) {
                    HttpURLConnection.HTTP_OK -> {
                        val newLastUpdateInMs = TimeUtils.currentTimeMillis()
                        val vehicleLocations = mutableListOf<VehicleLocation>()
                        val ignoreDirection = GTFSRealTimeProvider.isIGNORE_DIRECTION(context)
                        try {
                            val gFeedMessage = FeedMessage.parseFrom(response.body.bytes())
                            val gVehiclePositions = gFeedMessage.entityList.toVehicles()
                            for (gVehiclePosition in gVehiclePositions.sortVehicles(newLastUpdateInMs)) {
                                if (Constants.DEBUG) {
                                    MTLog.d(
                                        this@GTFSRealTimeVehiclePositionsProvider,
                                        "loadAgencyDataFromWWW() > GTFS vehicle: ${gVehiclePosition.toStringExt()}."
                                    )
                                }
                                processVehiclePositions(newLastUpdateInMs, gVehiclePosition, ignoreDirection)
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.let {
                                        vehicleLocations.addAll(it)
                                    }
                            }
                        } catch (e: Exception) {
                            MTLog.w(this@GTFSRealTimeVehiclePositionsProvider, e, "loadAgencyDataFromWWW() > error while parsing GTFS Real Time data!")
                        }
                        MTLog.i(this@GTFSRealTimeVehiclePositionsProvider, "Found %d vehicle locations.", vehicleLocations.size)
                        if (Constants.DEBUG) {
                            for (vehicleLocation in vehicleLocations) {
                                MTLog.d(this@GTFSRealTimeVehiclePositionsProvider, "loadAgencyDataFromWWW() > - new ${vehicleLocation.toStringShort()}.")
                            }
                        }
                        return vehicleLocations
                    }

                    else -> {
                        MTLog.w(
                            this@GTFSRealTimeVehiclePositionsProvider,
                            "ERROR: HTTP URL-Connection Response Code ${response.code} (Message: ${response.message})"
                        )
                        return null
                    }
                }
            }
        } catch (uhe: UnknownHostException) {
            if (MTLog.isLoggable(android.util.Log.DEBUG)) {
                MTLog.w(this@GTFSRealTimeVehiclePositionsProvider, uhe, "No Internet Connection!")
            } else {
                MTLog.w(this@GTFSRealTimeVehiclePositionsProvider, "No Internet Connection!")
            }
            return null
        } catch (se: SocketException) {
            MTLog.w(this@GTFSRealTimeVehiclePositionsProvider, se, "No Internet Connection!")
            return null
        } catch (e: Exception) { // Unknown error
            MTLog.e(this@GTFSRealTimeVehiclePositionsProvider, e, "INTERNAL ERROR: Unknown Exception")
            return null
        }
    }

    private fun GTFSRealTimeProvider.processVehiclePositions(
        newLastUpdateInMs: Long,
        gVehiclePosition: GtfsRealtime.VehiclePosition,
        ignoreDirection: Boolean,
    ): Set<VehicleLocation>? {
        val targetUUIDs = parseProviderTargetUUID(gVehiclePosition, ignoreDirection)?.takeIf { it.isNotBlank() } ?: return null
        return setOf(
            VehicleLocation(
                authority = this.authority,
                targetUUID = targetUUIDs,
                targetTripId = gVehiclePosition.optTrip?.optTripId?.originalIdToId(tripIdCleanupPattern),
                lastUpdateInMs = newLastUpdateInMs,
                maxValidityInMs = this@processVehiclePositions.vehicleLocationMaxValidityInMs,
                //
                vehicleId = gVehiclePosition.optVehicle?.optId,
                vehicleLabel = gVehiclePosition.optVehicle?.optLabel,
                reportTimestamp = gVehiclePosition.optTimestamp?.seconds,
                latitude = gVehiclePosition.optPosition?.optLatitude ?: return null,
                longitude = gVehiclePosition.optPosition?.optLongitude ?: return null,
                bearingDegrees = gVehiclePosition.optPosition?.optBearing?.toInt(), // in degrees
                speedMetersPerSecond = gVehiclePosition.optPosition?.optSpeed?.toInt(), // in meters per second
            )
        )
    }

    private fun GTFSRealTimeProvider.parseProviderTargetUUID(gVehiclePosition: GtfsRealtime.VehiclePosition, ignoreDirection: Boolean): String? {
        val tripDescriptor = gVehiclePosition.optTrip ?: return null
        if (tripDescriptor.hasModifiedTrip() || tripDescriptor.hasStartTime() || tripDescriptor.hasStartDate()) {
            MTLog.d(this, "parseTargetUUID() > unhandled values: ${tripDescriptor.toStringExt()}")
        }
        when (tripDescriptor.scheduleRelationship) {
            GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED -> {} // handled
            GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED,
            GtfsRealtime.TripDescriptor.ScheduleRelationship.UNSCHEDULED,
            GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED,
            GtfsRealtime.TripDescriptor.ScheduleRelationship.REPLACEMENT,
            GtfsRealtime.TripDescriptor.ScheduleRelationship.DUPLICATED,
            GtfsRealtime.TripDescriptor.ScheduleRelationship.DELETED,
            GtfsRealtime.TripDescriptor.ScheduleRelationship.NEW,
                -> MTLog.d(this, "parseTargetUUID() > unhandled schedule relationship: ${tripDescriptor.scheduleRelationship}")
        }
        return if (tripDescriptor.hasRouteId()) {
            if (tripDescriptor.hasDirectionId() && !ignoreDirection) {
                getAgencyRouteDirectionTagTargetUUID(
                    agencyTag,
                    tripDescriptor.routeId.originalIdToHash(routeIdCleanupPattern),
                    tripDescriptor.directionId,
                )
            } else {
                getAgencyRouteTagTargetUUID(
                    agencyTag,
                    tripDescriptor.routeId.originalIdToHash(routeIdCleanupPattern),
                )
            }
        } else {
            getAgencyTagTargetUUID(
                agencyTag
            )
        }
    }

    private val GTFSRealTimeProvider.routeIdCleanupPattern get() = getRouteIdCleanupPattern(requireContextCompat())
    private val GTFSRealTimeProvider.tripIdCleanupPattern get() = getTripIdCleanupPattern(requireContextCompat())

    private val GTFSRealTimeProvider.agencyTag get() = getAgencyTag(requireContextCompat())

    private fun Route.getRouteTag(provider: GTFSRealTimeProvider) = provider.getRouteTag(this)
    private fun Direction.getDirectionTag(provider: GTFSRealTimeProvider) = provider.getDirectionTag(this)

    private fun RouteDirection.getRouteTag(provider: GTFSRealTimeProvider) = this.route.getRouteTag(provider)
    private fun RouteDirection.getDirectionTag(provider: GTFSRealTimeProvider) = this.direction.getDirectionTag(provider)

    private fun RouteDirectionStop.getRouteTag(provider: GTFSRealTimeProvider) = this.route.getRouteTag(provider)
    private fun RouteDirectionStop.getDirectionTag(provider: GTFSRealTimeProvider) = this.direction.getDirectionTag(provider)
}
