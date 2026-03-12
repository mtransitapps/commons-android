package org.mtransit.android.commons.provider.status

import android.content.Context
import android.util.Log
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SecurityUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.isIGNORE_DIRECTION
import org.mtransit.android.commons.provider.gtfs.GtfsRealTimeStorage
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDirectionId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toTripUpdates
import org.mtransit.android.commons.provider.gtfs.getRDS
import org.mtransit.android.commons.provider.gtfs.getRDSSchedule
import org.mtransit.android.commons.provider.gtfs.getTripIds
import org.mtransit.android.commons.provider.gtfs.makeRequest
import org.mtransit.android.commons.provider.gtfs.parseRouteId
import org.mtransit.android.commons.provider.gtfs.parseTripId
import org.mtransit.commons.SourceUtils
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import com.google.transit.realtime.GtfsRealtime.FeedMessage as GFeedMessage

object GTFSRealTimeTripUpdatesProvider : MTLog.Loggable {

    internal val LOG_TAG: String = GTFSRealTimeTripUpdatesProvider::class.java.simpleName

    override fun getLogTag() = LOG_TAG

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
            MTLog.w(this@getCached, "getCached() > Can't find new schedule without schedule filter!")
            return null
        }
        val tripIds = filter.targetAuthority.let { targetAuthority ->
            filter.routeId.let { routeId ->
                context?.getTripIds(targetAuthority, routeId, filter.directionId)
            }
        }?.takeIf { tripIds -> tripIds.isNotEmpty() } // trip IDs REQUIRED for GTFS Trip Updates
            ?: return null
        return getCachedStatusS(filter.targetUUID, tripIds)
            ?: makeCachedStatusFromAgencyData(filter, tripIds)
    }

    val GTFSRealTimeProvider.ignoreDirection get() = isIGNORE_DIRECTION(this.requireContextCompat())

    private fun GTFSRealTimeProvider.makeCachedStatusFromAgencyData(
        filter: Schedule.ScheduleStatusFilter,
        tripIds: List<String>
    ): POIStatus? {
        val context = context ?: return null
        val readFromSourceMs = GtfsRealTimeStorage.getTripUpdateLastUpdateMs(context, 0L)
        if (readFromSourceMs <= 0L) return null // never loaded
        val sourceLabel = SourceUtils.getSourceLabel( // always use source from official API
            GTFSRealTimeProvider.getAgencyTripUpdatesUrlString(context, "T")
        )
        try {
            val rds = filter.routeDirectionStop
            val targetAuthority = rds.authority
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
            if (Constants.DEBUG) {
                rdTripUpdates.forEach { (_, gTripUpdate) ->
                    MTLog.d(
                        this@GTFSRealTimeTripUpdatesProvider,
                        "makeCachedStatusFromAgencyData() > GTFS [R:'${rds.route.shortestName}'|D:${rds.direction.headsignValue}] trip update: ${gTripUpdate.toStringExt()}."
                    )
                }
            }
            if (sortedRDS == null) {
                sortedRDS = context.getRDS(rds.authority, routeId, directionId)
            }
            if (uuidSchedule == null) {
                uuidSchedule = sortedRDS
                    ?.let { rdsList ->
                        context
                            .getRDSSchedule(targetAuthority, rdsList)
                            .associateBy { it.targetUUID }
                    }
            }
            uuidSchedule ?: return null
            sortedRDS ?: return null
            processRDTripUpdates(rdTripUpdates, uuidSchedule, sortedRDS)
            val tripsWithRealTime = uuidSchedule.values
                .asSequence()
                .mapNotNull { it?.timestamps }.flatten()
                .filter { it.isRealTime }
                .map { it.tripId }
                .toSet() // distinct
            uuidSchedule.values.filterNotNull().forEach { schedule ->
                val now = TimeUtilsK.currentInstant()
                if (!schedule.timestamps.any { it.isRealTime || (it.tripId in tripsWithRealTime && it.departure < now) }) return@forEach
                var oldestDateForRealTime = now - 1.minutes
                var maxFutureDateForRealTime = now + 12.hours
                val (past, future) = schedule.timestamps.partition { it.departure < now }
                oldestDateForRealTime = past.filter { it.isRealTime }.minOfOrNull { it.arrival } // all real-time
                    ?: oldestDateForRealTime
                maxFutureDateForRealTime = future.take(10).maxOfOrNull { it.departure } // keep firsts 10
                    ?.takeIf { it > maxFutureDateForRealTime }
                    ?: maxFutureDateForRealTime
                maxFutureDateForRealTime = future.filter { it.isRealTime }.maxOfOrNull { it.departure } // all real-time
                    ?.takeIf { it > maxFutureDateForRealTime }
                    ?: maxFutureDateForRealTime
                schedule.timestamps
                    .filterNot {
                        it.isRealTime || oldestDateForRealTime < it.arrival && it.departure < maxFutureDateForRealTime
                    }
                    .forEach { timestamp ->
                        schedule.removeTimestamp(timestamp)
                    }
                schedule.sourceLabel = sourceLabel
                schedule.lastUpdateInMs = readFromSourceMs
                schedule.readFromSourceAtInMs = readFromSourceMs
                schedule.providerPrecisionInMs = PROVIDER_PRECISION_IN_MS
                schedule.maxValidityInMs = maxValidityInMs
                cacheStatus(schedule)
            }
            return getCachedStatusS(filter.targetUUID, tripIds)
        } catch (e: Exception) {
            MTLog.w(this, e, "makeCachedStatusFromAgencyData() > error!")
            return null
        }
    }

    @JvmStatic
    fun GTFSRealTimeProvider.getNew(statusFilter: StatusProviderContract.Filter): POIStatus? {
        val filter = statusFilter as? Schedule.ScheduleStatusFilter ?: run {
            MTLog.w(this, "getNew() > Can't find new schedule without schedule filter!")
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
        val newStatusesLoaded = loadAgencyDataFromWWW(context)
        if (newStatusesLoaded) { // empty is OK
            if (!deleteAllDone) {
                deleteAllCachedStatus()
            }
            // no caching, will make as requested from cached file
        } // else keep whatever we have until max validity reached
    }

    private const val GTFS_RT_TRIP_UPDATE_PB_FILE_NAME = "gtfs_rt_trip_update.pb"

    private fun GTFSRealTimeProvider.loadAgencyDataFromWWW(context: Context): Boolean {
        try {
            val urlRequest = makeRequest(
                context,
                urlCachedString = GTFSRealTimeProvider.getAGENCY_TRIP_UPDATES_URL_CACHED(context),
                getUrlString = { token -> GTFSRealTimeProvider.getAgencyTripUpdatesUrlString(context, token) }
            ) ?: return false
            getOkHttpClient(context).newCall(urlRequest).execute().use { response ->
                GtfsRealTimeStorage.saveTripUpdateLastUpdateCode(context, response.code)
                GtfsRealTimeStorage.saveTripUpdateLastUpdateMs(context, TimeUtils.currentTimeMillis())
                when (response.code) {
                    HttpURLConnection.HTTP_OK -> {
                        try {
                            try {
                                File(context.cacheDir, GTFS_RT_TRIP_UPDATE_PB_FILE_NAME).writeBytes(response.body.bytes())
                            } catch (e: IOException) {
                                MTLog.w(this@GTFSRealTimeTripUpdatesProvider, e, "loadAgencyDataFromWWW() > error while saving GTFS RT Trip Updates data!")
                            }
                        } catch (e: Exception) {
                            MTLog.w(this@GTFSRealTimeTripUpdatesProvider, e, "loadAgencyDataFromWWW() > error while parsing GTFS Real Time data!")
                        }
                        return true
                    }

                    else -> {
                        MTLog.w(
                            this@GTFSRealTimeTripUpdatesProvider,
                            "ERROR: HTTP URL-Connection Response Code ${response.code} (Message: ${response.message})"
                        )
                        return false
                    }
                }
            }
        } catch (sslhe: SSLHandshakeException) {
            MTLog.w(this, sslhe, "SSL error!")
            SecurityUtils.logCertPathValidatorException(sslhe)
            GtfsRealTimeStorage.saveTripUpdateLastUpdateCode(context, 567) // SSL certificate not trusted (on this device)
            GtfsRealTimeStorage.saveTripUpdateLastUpdateMs(context, TimeUtils.currentTimeMillis())
            return false
        } catch (uhe: UnknownHostException) {
            if (MTLog.isLoggable(Log.DEBUG)) {
                MTLog.w(this@GTFSRealTimeTripUpdatesProvider, uhe, "No Internet Connection!")
            } else {
                MTLog.w(this@GTFSRealTimeTripUpdatesProvider, "No Internet Connection!")
            }
            return false
        } catch (se: SocketException) {
            MTLog.w(this@GTFSRealTimeTripUpdatesProvider, se, "No Internet Connection!")
            return false
        } catch (e: Exception) { // Unknown error
            MTLog.e(this@GTFSRealTimeTripUpdatesProvider, e, "INTERNAL ERROR: Unknown Exception")
            return false
        }
    }
}
