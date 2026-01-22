package org.mtransit.android.commons.provider.vehiclelocations

import android.content.Context
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.provider.NextBusProvider
import org.mtransit.android.commons.provider.NextBusProvider.getAGENCY_TAG
import org.mtransit.android.commons.provider.NextBusProvider.getAgencyRouteTagTargetUUID
import org.mtransit.android.commons.provider.NextBusProvider.isAPPEND_HEAD_SIGN_VALUE_TO_ROUTE_TAG
import org.mtransit.android.commons.provider.nextbus.NextBusStorage
import org.mtransit.android.commons.provider.nextbus.api.NextBusApi
import org.mtransit.android.commons.provider.nextbus.api.VehicleLocationsResponse
import org.mtransit.android.commons.provider.vehiclelocations.VehicleLocationProvider.Companion.getCachedVehicleLocationsS
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.UnknownHostException
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object NextBusVehicleLocationsProvider {

    val VEHICLE_LOCATION_MAX_VALIDITY_IN_MS = 1.hours.inWholeMilliseconds

    val VEHICLE_LOCATION_VALIDITY_IN_MS = 10.minutes.inWholeMilliseconds
    val VEHICLE_LOCATION_VALIDITY_IN_FOCUS_IN_MS = 5.seconds.inWholeMilliseconds

    @Suppress("unused")
    val VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_MS = 3.minutes.inWholeMilliseconds

    @Suppress("unused")
    val VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = 1.minutes.inWholeMilliseconds

    @Suppress("unused")
    @JvmStatic
    fun getMinDurationBetweenRefreshInMs(inFocus: Boolean) =
        if (inFocus) VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS
        else VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_MS

    @JvmStatic
    fun getValidityInMs(inFocus: Boolean) =
        if (inFocus) VEHICLE_LOCATION_VALIDITY_IN_FOCUS_IN_MS
        else VEHICLE_LOCATION_VALIDITY_IN_MS

    @JvmStatic
    val maxValidityInMs: Long get() = VEHICLE_LOCATION_MAX_VALIDITY_IN_MS

    @JvmStatic
    fun NextBusProvider.getCached(vehicleLocationFilter: VehicleLocationProviderContract.Filter): List<VehicleLocation>? =
        ((vehicleLocationFilter.poi as? RouteDirectionStop)?.getTargetUUIDs(this)
            ?: vehicleLocationFilter.routeDirection?.getTargetUUIDs(this)
            ?: vehicleLocationFilter.route?.getTargetUUIDs(this))
            ?.let { targetUUIDs ->
                getCached(targetUUIDs, tripIds = null) // NO GTFS trip.id information available
            }

    private fun RouteDirectionStop.getTargetUUIDs(provider: NextBusProvider) = buildMap {
        if (!provider.isAppendHeadSignValueToRouteTag) {
            put(getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)), route.uuid)
        } else { // STLaval
            put(getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)), routeDirectionUUID)
        }
    }

    private fun RouteDirection.getTargetUUIDs(provider: NextBusProvider) = buildMap {
        if (!provider.isAppendHeadSignValueToRouteTag) {
            put(getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)), route.uuid)
        } else { // STLaval
            put(getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)), uuid)
        }
    }

    private fun Route.getTargetUUIDs(provider: NextBusProvider) = buildMap {
        if (!provider.isAppendHeadSignValueToRouteTag) {
            put(getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)), uuid)
        } // ELSE // STLaval
    }

    fun NextBusProvider.getCached(targetUUIDs: Map<String, String>, tripIds: List<String>? = null) = buildList {
        getCachedVehicleLocationsS(targetUUIDs.keys, tripIds)?.let {
            addAll(it)
        }
    }.map { it.copy(targetUUID = targetUUIDs[it.targetUUID] ?: it.targetUUID) }

    @JvmStatic
    fun NextBusProvider.getNew(vehicleLocationFilter: VehicleLocationProviderContract.Filter): List<VehicleLocation>? {
        updateAgencyDataIfRequired(vehicleLocationFilter.inFocusOrDefault)
        return getCached(vehicleLocationFilter)
    }

    private fun NextBusProvider.updateAgencyDataIfRequired(inFocus: Boolean) {
        val context = requireContextCompat()
        var inFocus = inFocus
        val lastUpdateInMs = NextBusStorage.getVehicleLocationLastUpdateMs(context, 0L)
        val lastUpdateCode = NextBusStorage.getVehicleLocationLastUpdateCode(context, -1).takeIf { it >= 0 }
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
    private fun NextBusProvider.updateAgencyDataIfRequiredSync(lastUpdateInMs: Long, inFocus: Boolean) {
        val context = requireContextCompat()
        if (NextBusStorage.getVehicleLocationLastUpdateMs(context, 0L) > lastUpdateInMs) {
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

    private fun NextBusProvider.updateAllAgencyDataFromWWW(context: Context, deleteAllRequired: Boolean) {
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

    private fun NextBusProvider.loadAgencyDataFromWWW(context: Context): List<VehicleLocation>? {
        try {
            MTLog.i(this, "Loading from '%s' for agency '%s'...", NextBusApi.BASE_HOST_URL, agencyTag)
            val response = getNextBusApi(context)
                .getVehicleLocations(agencyTag = agencyTag)
                .execute()
            NextBusStorage.saveVehicleLocationLastUpdateCode(context, response.code())
            val newLastUpdate = TimeUtils.currentTimeMillis().milliseconds
            NextBusStorage.saveVehicleLocationLastUpdateMs(context, newLastUpdate.inWholeMilliseconds)
            when (response.code()) {
                HttpURLConnection.HTTP_OK -> {
                    val vehicleLocations = mutableListOf<VehicleLocation>()
                    try {
                        response.body()?.let { vehicleLocationsResponse ->
                            vehicleLocationsResponse.vehicle?.forEach { nVehicle ->
                                if (Constants.DEBUG) {
                                    MTLog.d(
                                        this@NextBusVehicleLocationsProvider,
                                        "loadAgencyDataFromWWW() > NextBus nVehicle: ${nVehicle}."
                                    )
                                }
                                processVehiclePositions(newLastUpdate, nVehicle)
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.let {
                                        vehicleLocations.addAll(it)
                                    }
                            }
                        }
                    } catch (e: Exception) {
                        MTLog.w(this@NextBusVehicleLocationsProvider, e, "loadAgencyDataFromWWW() > error while parsing NextBus Real Time data!")
                    }
                    MTLog.i(this@NextBusVehicleLocationsProvider, "Found %d vehicle locations.", vehicleLocations.size)
                    if (Constants.DEBUG) {
                        for (serviceUpdate in vehicleLocations) {
                            MTLog.d(this@NextBusVehicleLocationsProvider, "loadAgencyDataFromWWW() > vehicle location: $serviceUpdate.")
                        }
                    }
                    return vehicleLocations
                }

                else -> {
                    MTLog.w(
                        this@NextBusVehicleLocationsProvider,
                        "ERROR: HTTP URL-Connection Response Code ${response.code()} (Message: ${response.message()})"
                    )
                    return null
                }
            }
        } catch (uhe: UnknownHostException) {
            if (MTLog.isLoggable(android.util.Log.DEBUG)) {
                MTLog.w(this@NextBusVehicleLocationsProvider, uhe, "No Internet Connection!")
            } else {
                MTLog.w(this@NextBusVehicleLocationsProvider, "No Internet Connection!")
            }
            return null
        } catch (se: SocketException) {
            MTLog.w(this@NextBusVehicleLocationsProvider, se, "No Internet Connection!")
            return null
        } catch (e: Exception) { // Unknown error
            MTLog.e(this@NextBusVehicleLocationsProvider, e, "INTERNAL ERROR: Unknown Exception")
            return null
        }
    }

    private fun NextBusProvider.processVehiclePositions(
        newLastUpdate: Duration,
        nVehicle: VehicleLocationsResponse.Vehicle,
    ): Set<VehicleLocation>? {
        val targetUUIDs = parseProviderTargetUUID(nVehicle)?.takeIf { it.isNotBlank() } ?: return null
        return setOf(
            VehicleLocation(
                authority = this.authority,
                targetUUID = targetUUIDs,
                targetTripId = null, // no GTFS trip.id info returned
                lastUpdateInMs = newLastUpdate.inWholeMilliseconds,
                maxValidityInMs = this@processVehiclePositions.vehicleLocationMaxValidityInMs,
                //
                vehicleId = nVehicle.id,
                vehicleLabel = null,
                reportTimestamp = nVehicle.secsSinceReport?.seconds?.let { newLastUpdate - it },
                latitude = nVehicle.lat?.toFloat() ?: return null,
                longitude = nVehicle.lon?.toFloat() ?: return null,
                bearingDegrees = nVehicle.heading, // in degrees
                speedMetersPerSecond = nVehicle.speedKmHr?.div(3.6)?.toInt(), // in km/h (m/s = km/h * 1000 meters / 3600 seconds)
            )
        )
    }

    private fun NextBusProvider.parseProviderTargetUUID(nVehicle: VehicleLocationsResponse.Vehicle) =
        nVehicle.routeTag?.let { getAgencyRouteTagTargetUUID(agencyTag, it) }

    private val NextBusProvider.agencyTag get() = getAGENCY_TAG(requireContextCompat())
    private val NextBusProvider.isAppendHeadSignValueToRouteTag get() = isAPPEND_HEAD_SIGN_VALUE_TO_ROUTE_TAG(requireContextCompat())

    private fun Route.getRouteTag(provider: NextBusProvider) = provider.getRouteTag(this, null)
    private fun RouteDirection.getRouteTag(provider: NextBusProvider) = provider.getRouteTag(this)
    private fun RouteDirectionStop.getRouteTag(provider: NextBusProvider) = provider.getRouteTag(this)
}
