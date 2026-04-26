package org.mtransit.android.commons.provider.vehiclelocations

import android.content.Context
import androidx.annotation.VisibleForTesting
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SecurityUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyTagTargetUUID
import org.mtransit.android.commons.provider.gtfs.GtfsRealTimeStorage
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optBearing
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDirectionIdValid
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optLabel
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optLatitude
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optLongitude
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optPosition
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optSpeed
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimestamp
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optVehicle
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.sortVehicles
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toVehicles
import org.mtransit.android.commons.provider.gtfs.agencyTag
import org.mtransit.android.commons.provider.gtfs.getTargetUUIDs
import org.mtransit.android.commons.provider.gtfs.getTripIds
import org.mtransit.android.commons.provider.gtfs.ignoreDirection
import org.mtransit.android.commons.provider.gtfs.makeRequest
import org.mtransit.android.commons.provider.gtfs.parseRouteId
import org.mtransit.android.commons.provider.gtfs.parseTripId
import org.mtransit.android.commons.provider.gtfs.setTripIdsOutOfSync
import org.mtransit.android.commons.provider.vehiclelocations.VehicleLocationProvider.Companion.getCachedVehicleLocationsS
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.android.commons.secsToInstant
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import com.google.transit.realtime.GtfsRealtime.FeedMessage as GFeedMessage
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship as GTDScheduleRelationship
import com.google.transit.realtime.GtfsRealtime.VehiclePosition as GVehiclePosition

object GTFSRealTimeVehiclePositionsProvider : MTLog.Loggable {

    internal val LOG_TAG: String = GTFSRealTimeVehiclePositionsProvider::class.java.simpleName

    override fun getLogTag() = LOG_TAG

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
            this * 2L // fewer calls to Cached API $$
        } else this

    private var _tripIdsOutOfSync: Boolean? = null

    private fun GTFSRealTimeProvider.getTripIdOutOfSync() = _tripIdsOutOfSync
        ?: context?.let { GtfsRealTimeStorage.getVehicleLocationTripIdsOutOfSync(it, false) }.also {
            _tripIdsOutOfSync = it
        }

    private const val INCLUDE_AGENCY_TAG = true // some transit agencies only use the trip IDs to target #TransitWindsor

    @JvmStatic
    fun GTFSRealTimeProvider.getCached(filter: VehicleLocationProviderContract.Filter) =
        getCached(
            filter,
            tripIdsOutOfSync = getTripIdOutOfSync(),
            getTripIds = { authority, routeId, directionId ->
                context?.getTripIds(authority, routeId, directionId)
            },
            getCachedVehicleLocations = { targetUUIDs, tripIds ->
                getCachedVehicleLocationsS(targetUUIDs, tripIds)
            }
        )

    @VisibleForTesting
    internal fun GTFSRealTimeProvider.getCached(
        filter: VehicleLocationProviderContract.Filter,
        tripIdsOutOfSync: Boolean?,
        getTripIds: (authority: String, routeId: Long, directionId: Long?) -> List<String>?,
        getCachedVehicleLocations: (targetUUIDs: Collection<String>, tripIds: List<String>?) -> List<VehicleLocation>?,
    ): List<VehicleLocation>? {
        val tripIdsOutOfSync = tripIdsOutOfSync == true
        @Suppress("SimplifyBooleanWithConstants")
        return filter.getTargetUUIDs(this, includeAgencyTag = INCLUDE_AGENCY_TAG && !tripIdsOutOfSync)
            ?.let { targetUUIDs ->
                val tripIds = if (tripIdsOutOfSync) null
                else filter.targetAuthority?.let { targetAuthority ->
                    filter.routeId?.let { routeId ->
                        getTripIds(targetAuthority, routeId, filter.directionId)
                    }
                }
                targetUUIDs to tripIds?.takeIf { it.isNotEmpty() } // no trip IDS == fallback to primary target UUID only
            }?.let { (targetUUIDs, tripIds) ->
                getCached(targetUUIDs, tripIds, getCachedVehicleLocations)
            }
    }

    private fun getCached(
        targetUUIDs: Map<String, String>,
        tripIds: List<String>?,
        getCachedVehicleLocations: (targetUUIDs: Collection<String>, tripIds: List<String>?) -> List<VehicleLocation>?,
    ) = buildList {
        getCachedVehicleLocations(targetUUIDs.keys, tripIds)?.takeIf { it.isNotEmpty() }
            ?.let {
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
                loggable = this@GTFSRealTimeVehiclePositionsProvider,
                context = context,
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
                        try {
                            val gFeedMessage = GFeedMessage.parseFrom(response.body.bytes())
                            val gVehiclePositions = gFeedMessage.entityList.toVehicles()
                            if (Constants.DEBUG) {
                                MTLog.d(LOG_TAG, "loadAgencyDataFromWWW() > GTFS vehicles[${gVehiclePositions.size}]: ")
                            }
                            val ignoreDirection = ignoreDirection
                            for (gVehiclePosition in gVehiclePositions.sortVehicles()) {
                                if (Constants.DEBUG) {
                                    MTLog.d(LOG_TAG, "loadAgencyDataFromWWW() > GTFS - ${gVehiclePosition.toStringExt()}.")
                                }
                                processVehiclePositions(newLastUpdateInMs, gVehiclePosition, ignoreDirection)
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.let {
                                        vehicleLocations.addAll(it)
                                    }
                            }
                        } catch (e: Exception) {
                            MTLog.w(LOG_TAG, e, "loadAgencyDataFromWWW() > error while parsing GTFS Real Time data!")
                        }
                        MTLog.i(LOG_TAG, "Found %d vehicle locations.", vehicleLocations.size)
                        if (Constants.DEBUG) {
                            for (vehicleLocation in vehicleLocations) {
                                MTLog.d(LOG_TAG, "loadAgencyDataFromWWW() > - new ${vehicleLocation.toStringShort()}.")
                            }
                        }
                        setTripIdsOutOfSync(vehicleLocations)
                        return vehicleLocations
                    }

                    else -> {
                        MTLog.w(LOG_TAG, "ERROR: HTTP URL-Connection Response Code ${response.code} (Message: ${response.message})")
                        return null
                    }
                }
            }
        } catch (sslhe: SSLHandshakeException) {
            MTLog.w(LOG_TAG, sslhe, "SSL error!")
            SecurityUtils.logCertPathValidatorException(sslhe)
            GtfsRealTimeStorage.saveVehicleLocationLastUpdateCode(context, 567) // SSL certificate not trusted (on this device)
            GtfsRealTimeStorage.saveVehicleLocationLastUpdateMs(context, TimeUtils.currentTimeMillis())
            return null
        } catch (uhe: UnknownHostException) {
            if (MTLog.isLoggable(android.util.Log.DEBUG)) {
                MTLog.w(LOG_TAG, uhe, "No Internet Connection!")
            } else {
                MTLog.w(LOG_TAG, "No Internet Connection!")
            }
            return null
        } catch (se: SocketException) {
            MTLog.w(LOG_TAG, se, "No Internet Connection!")
            return null
        } catch (e: Exception) { // Unknown error
            MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception")
            return null
        }
    }

    private fun GTFSRealTimeProvider.setTripIdsOutOfSync(vehicleLocations: MutableList<VehicleLocation>) {
        setTripIdsOutOfSync(
            getOneTripId = { vehicleLocations.firstOrNull { it.targetTripId != null }?.targetTripId },
            saveTripIdsOutOfSync = { context, tripIdsOutOfSync ->
                GtfsRealTimeStorage.saveVehicleLocationTripIdsOutOfSync(context, tripIdsOutOfSync)
                _tripIdsOutOfSync = tripIdsOutOfSync
            }
        )
    }

    private fun GTFSRealTimeProvider.processVehiclePositions(
        newLastUpdateInMs: Long,
        gVehiclePosition: GVehiclePosition,
        ignoreDirection: Boolean,
    ): Set<VehicleLocation>? {
        val targetUUIDs = parseProviderTargetUUID(gVehiclePosition, ignoreDirection)?.takeIf { it.isNotBlank() } ?: return null
        return setOf(
            VehicleLocation(
                authority = this.authority,
                targetUUID = targetUUIDs,
                targetTripId = gVehiclePosition.optTrip?.let { parseTripId(it) },
                lastUpdateInMs = newLastUpdateInMs,
                maxValidityInMs = this@processVehiclePositions.vehicleLocationMaxValidityInMs,
                //
                vehicleId = gVehiclePosition.optVehicle?.optId,
                vehicleLabel = gVehiclePosition.optVehicle?.optLabel,
                reportTimestamp = gVehiclePosition.optTimestamp?.secsToInstant(),
                latitude = gVehiclePosition.optPosition?.optLatitude ?: return null,
                longitude = gVehiclePosition.optPosition?.optLongitude ?: return null,
                bearingDegrees = gVehiclePosition.optPosition?.optBearing?.toInt(), // in degrees
                speedMetersPerSecond = gVehiclePosition.optPosition?.optSpeed?.toInt(), // in meters per second
            )
        )
    }

    @VisibleForTesting
    internal fun GTFSRealTimeProvider.parseProviderTargetUUID(gVehiclePosition: GVehiclePosition, ignoreDirection: Boolean): String? {
        val gTripDescriptor = gVehiclePosition.optTrip ?: return null
        if (gTripDescriptor.hasModifiedTrip()) {
            MTLog.d(LOG_TAG, "parseTargetUUID() > unhandled modified trip: ${gTripDescriptor.toStringExt()}")
        }
        if (gTripDescriptor.hasStartTime() || gTripDescriptor.hasStartDate()) {
            MTLog.d(LOG_TAG, "parseTargetUUID() > unhandled start date & time: ${gTripDescriptor.toStringExt()}")
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
                -> MTLog.d(LOG_TAG, "parseTargetUUID() > unhandled schedule relationship: ${gTripDescriptor.scheduleRelationship}")
        }
        parseRouteId(gTripDescriptor)?.let { routeId ->
            gTripDescriptor.optDirectionIdValid?.takeIf { !ignoreDirection }?.let { directionId ->
                return getAgencyRouteDirectionTagTargetUUID(agencyTag, routeId, directionId)
            }
            return getAgencyRouteTagTargetUUID(agencyTag, routeId)
        }
        return getAgencyTagTargetUUID(agencyTag)
    }
}
