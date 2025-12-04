package org.mtransit.android.commons.provider.vehiclelocations

import android.content.Context
import com.google.transit.realtime.GtfsRealtime
import com.google.transit.realtime.GtfsRealtime.FeedMessage
import okhttp3.Request
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
import org.mtransit.android.commons.provider.gtfs.GtfsRealTimeStorage
import org.mtransit.android.commons.provider.gtfs.GtfsRealTimeStorage.saveServiceUpdateLastUpdateMs
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.originalIdToHash
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.sortVehiclesPair
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toVehiclesWithIdPair
import org.mtransit.android.commons.provider.vehiclelocations.VehicleLocationProvider.Companion.getCachedVehicleLocationsS
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.URL
import java.net.UnknownHostException
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object GTFSRealTimeVehiclePositionsProvider {

    val VEHICLE_LOCATION_MAX_VALIDITY_IN_MS = 1.hours.inWholeMilliseconds
    val VEHICLE_LOCATION_VALIDITY_IN_MS = 30.minutes.inWholeMilliseconds
    val VEHICLE_LOCATION_VALIDITY_IN_FOCUS_IN_MS = 1.minutes.inWholeMilliseconds
    val VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_MS = 10.minutes.inWholeMilliseconds
    val VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = 1.minutes.inWholeMilliseconds

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
    fun GTFSRealTimeProvider.getCached(vehicleLocationFilter: VehicleLocationProviderContract.Filter) =
        ((vehicleLocationFilter.poi as? RouteDirectionStop)?.getTargetUUIDs(this)
            ?: vehicleLocationFilter.routeDirection?.getTargetUUIDs(this)
            ?: vehicleLocationFilter.route?.getTargetUUIDs(this))
            ?.let { targetUUIDs ->
                getCached(targetUUIDs)
            }

    private fun RouteDirectionStop.getTargetUUIDs(provider: GTFSRealTimeProvider) = buildMap {
        put(getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)), route.getUUID())
        getAgencyRouteDirectionTagTargetUUID(provider.agencyTag, getRouteTag(provider), getDirectionTag(provider))?.let { put(it, routeDirectionUUID) }
    }

    private fun RouteDirection.getTargetUUIDs(provider: GTFSRealTimeProvider) = buildMap {
        put(getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)), route.getUUID())
        getAgencyRouteDirectionTagTargetUUID(provider.agencyTag, getRouteTag(provider), getDirectionTag(provider))?.let { put(it, uuid) }
    }

    private fun Route.getTargetUUIDs(provider: GTFSRealTimeProvider) = mapOf(
        getAgencyRouteTagTargetUUID(provider.agencyTag, getRouteTag(provider)) to uuid,
    )

    fun GTFSRealTimeProvider.getCached(targetUUIDs: Map<String, String>) = buildList {
        getCachedVehicleLocationsS(targetUUIDs.keys)?.let {
            addAll(it)
        }
    }.map { it.copy(targetUUID = targetUUIDs[it.targetUUID] ?: it.targetUUID) }

    @JvmStatic
    fun GTFSRealTimeProvider.getNew(vehicleLocationFilter: VehicleLocationProviderContract.Filter): List<VehicleLocation>? {
        updateAgencyDataIfRequired(vehicleLocationFilter.inFocusOrDefault)
        return getCached(vehicleLocationFilter)
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
        val newVehicleLocations: List<VehicleLocation>? = loadAgencyDataFromWWW(context)
        if (newVehicleLocations != null) { // empty is OK
            if (!deleteAllDone) {
                deleteAllCachedVehicleLocations()
            }
            cacheVehicleLocations(newVehicleLocations)
        } // else keep whatever we have until max validity reached
    }

    private fun GTFSRealTimeProvider.loadAgencyDataFromWWW(context: Context): List<VehicleLocation>? {
        try {
            val url: URL
            val urlCachedString = GTFSRealTimeProvider.getAGENCY_VEHICLE_POSITIONS_URL_CACHED(context)
            if (urlCachedString.isBlank()) {
                val token = GTFSRealTimeProvider.getAGENCY_URL_TOKEN(context) // use local token 1st for new/updated API URL & tokens
                    .takeIf { it.isNotBlank() } ?: this.providedAgencyUrlToken
                ?: "" // compat w/ API w/o token
                var urlString = GTFSRealTimeProvider.getAgencyVehiclePositionsUrlString(context, token)
                if (GTFSRealTimeProvider.isUSE_URL_HASH_SECRET_AND_DATE(context)) {
                    getHashSecretAndDate(context)?.let { hash ->
                        urlString = urlString.replace(GTFSRealTimeProvider.MT_HASH_SECRET_AND_DATE.toRegex(), hash.trim())
                    }
                }
                url = URL(urlString)
                MTLog.i(this, "Loading from '%s'...", url.host)
                MTLog.d(this, "Using token '%s' (length: %d)", if (!token.isEmpty()) "***" else "(none)", token.length)
            } else {
                url = URL(urlCachedString)
                MTLog.i(this, "Loading from cached API (length: %d) '***'...", urlCachedString.length)
            }
            val urlRequest = Request.Builder().url(url).build()
            getOkHttpClient(context).newCall(urlRequest).execute().use { response ->
                GtfsRealTimeStorage.saveVehicleLocationLastUpdateCode(context, response.code)
                saveServiceUpdateLastUpdateMs(context, TimeUtils.currentTimeMillis())
                when (response.code) {
                    HttpURLConnection.HTTP_OK -> {
                        val newLastUpdateInMs = TimeUtils.currentTimeMillis()
                        val vehicleLocations = mutableListOf<VehicleLocation>()
                        try {
                            val gFeedMessage = FeedMessage.parseFrom(response.body.bytes())
                            val vehiclePositionsWithIdPair = gFeedMessage.entityList.toVehiclesWithIdPair()
                            for (gVehiclePositionAndId in vehiclePositionsWithIdPair.sortVehiclesPair(newLastUpdateInMs)) {
                                val gVehiclePosition = gVehiclePositionAndId.first
                                if (Constants.DEBUG) {
                                    MTLog.d(
                                        this@GTFSRealTimeVehiclePositionsProvider,
                                        "loadAgencyDataFromWWW() > GTFS vehicle: ${gVehiclePosition.toStringExt()}."
                                    )
                                }
                                processVehiclePositions(newLastUpdateInMs, gVehiclePosition)
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.let {
                                        vehicleLocations.addAll(it)
                                    }
                            }
                        } catch (e: Exception) {
                            MTLog.w(this@GTFSRealTimeVehiclePositionsProvider, e, "loadAgencyDataFromWWW() > error while parsing GTFS Real Time data!")
                        }
                        MTLog.i(this@GTFSRealTimeVehiclePositionsProvider, "Found %d service updates.", vehicleLocations.size)
                        if (Constants.DEBUG) {
                            for (serviceUpdate in vehicleLocations) {
                                MTLog.d(this@GTFSRealTimeVehiclePositionsProvider, "loadAgencyDataFromWWW() > service update: $serviceUpdate.")
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
        gVehiclePosition: GtfsRealtime.VehiclePosition
    ): Set<VehicleLocation>? {
        val targetUUIDs = parseProviderTargetUUID(gVehiclePosition)?.takeIf { it.isNotBlank() } ?: return null
        return setOf(VehicleLocation(
            targetUUID = targetUUIDs,
            targetTripId = gVehiclePosition.trip.tripId.originalIdToHash(tripIdCleanupPattern),
            lastUpdateInMs = newLastUpdateInMs,
            maxValidityInMs = this@processVehiclePositions.vehicleLocationMaxValidityInMs,
            //
            vehicleId = gVehiclePosition.vehicle.id,
            vehicleLabel = gVehiclePosition.vehicle.label,
            latitude = gVehiclePosition.position.latitude,
            longitude = gVehiclePosition.position.longitude,
            bearing = gVehiclePosition.position.bearing,
            speed = gVehiclePosition.position.speed,
        ))
    }

    private fun GTFSRealTimeProvider.parseProviderTargetUUID(gVehiclePosition: GtfsRealtime.VehiclePosition): String? {
        val tripDescriptor = gVehiclePosition.trip
        if (tripDescriptor.hasRouteId()) {
            return if (tripDescriptor.hasDirectionId()) {
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
        }
        MTLog.w(this, "parseTargetUUID() > unexpected trip selector: ${tripDescriptor.toStringExt()} (IGNORED)")
        return null
    }
}

private val GTFSRealTimeProvider.routeIdCleanupPattern get() = getRouteIdCleanupPattern(requireContextCompat())
private val GTFSRealTimeProvider.tripIdCleanupPattern get() = getRouteIdCleanupPattern(requireContextCompat())

private val GTFSRealTimeProvider.agencyTag get() = getAgencyTag(requireContextCompat())

private fun Route.getRouteTag(provider: GTFSRealTimeProvider) = provider.getRouteTag(this)
private fun Direction.getDirectionTag(provider: GTFSRealTimeProvider) = provider.getDirectionTag(this)

private fun RouteDirection.getRouteTag(provider: GTFSRealTimeProvider) = this.route.getRouteTag(provider)
private fun RouteDirection.getDirectionTag(provider: GTFSRealTimeProvider) = this.direction.getDirectionTag(provider)

private fun RouteDirectionStop.getRouteTag(provider: GTFSRealTimeProvider) = this.route.getRouteTag(provider)
private fun RouteDirectionStop.getDirectionTag(provider: GTFSRealTimeProvider) = this.direction.getDirectionTag(provider)
