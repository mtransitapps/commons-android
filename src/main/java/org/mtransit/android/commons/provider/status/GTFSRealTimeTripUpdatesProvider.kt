package org.mtransit.android.commons.provider.status

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.Context
import android.util.Log
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SecurityUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.data.toNoData
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.gtfs.GtfsRealTimeStorage
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDirectionIdValid
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.sortTripUpdates
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toTripUpdates
import org.mtransit.android.commons.provider.gtfs.getRDS
import org.mtransit.android.commons.provider.gtfs.getRDSSchedule
import org.mtransit.android.commons.provider.gtfs.getTripIds
import org.mtransit.android.commons.provider.gtfs.ignoreDirection
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
import com.google.transit.realtime.GtfsRealtime.TripUpdate as GTripUpdate

object GTFSRealTimeTripUpdatesProvider : MTLog.Loggable {

    internal val LOG_TAG: String = GTFSRealTimeTripUpdatesProvider::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    val PROVIDER_PRECISION = 10.seconds
    val PROVIDER_PRECISION_IN_MS = PROVIDER_PRECISION.inWholeMilliseconds

    val TRIP_UPDATE_MAX_VALIDITY_IN_MS = 1.hours.inWholeMilliseconds

    val TRIP_UPDATE_VALIDITY_IN_MS = 3.minutes.inWholeMilliseconds
    val TRIP_UPDATE_VALIDITY_IN_FOCUS_IN_MS = 30.seconds.inWholeMilliseconds

    val TRIP_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = 1.minutes.inWholeMilliseconds
    val TRIP_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = 10.seconds.inWholeMilliseconds

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
            this.coerceAtLeast(1.minutes.inWholeMilliseconds) // fewer calls to Cached API $$
        } else this

    @JvmStatic
    fun GTFSRealTimeProvider.getCached(statusFilter: StatusProviderContract.Filter): POIStatus? {
        val filter = statusFilter as? Schedule.ScheduleStatusFilter ?: run {
            MTLog.w(LOG_TAG, "getCached() > Can't find new schedule without schedule filter!")
            return null
        }
        val tripIds = filter.targetAuthority.let { targetAuthority ->
            filter.routeId.let { routeId ->
                context?.getTripIds(targetAuthority, routeId, filter.directionId)
            }
        }?.takeIf { tripIds -> tripIds.isNotEmpty() } // trip IDs REQUIRED for GTFS Trip Updates
            ?: return null
        return getCachedStatusS(filter.targetUUID, tripIds)
            ?: makeCachedStatusFromAgencyDataLock(filter, tripIds)
    }

    private val tripUpdateLock = mutableMapOf<String, Any>()

    private fun GTFSRealTimeProvider.makeCachedStatusFromAgencyDataLock(
        filter: Schedule.ScheduleStatusFilter,
        tripIds: List<String>
    ): POIStatus? {
        val context = context ?: return null
        if (GtfsRealTimeStorage.getTripUpdateLastUpdateMs(context, 0L) <= 0L) return null // never loaded
        gTripUpdates ?: return null
        synchronized(tripUpdateLock.getOrPut(filter.routeDirectionStop.routeDirectionUUID) { Any() }) {
            return getCachedStatusS(filter.targetUUID, tripIds) // try another time
                ?: makeCachedStatusFromAgencyData(context, filter, tripIds)
        }
    }

    private fun GTFSRealTimeProvider.makeCachedStatusFromAgencyData(
        context: Context,
        filter: Schedule.ScheduleStatusFilter,
        tripIds: List<String>
    ): POIStatus? {
        val readFromSourceMs = GtfsRealTimeStorage.getTripUpdateLastUpdateMs(context, 0L)
            .takeIf { it > 0L } ?: return null // never loaded
        val gTripUpdates = gTripUpdates ?: return null
        val sourceLabel = SourceUtils.getSourceLabel( // always use source from official API
            GTFSRealTimeProvider.getAgencyTripUpdatesUrlString(context, "T")
        )
        try {
            val (targetRoute, targetDirection) = filter.routeDirectionStop.let { it.route to it.direction }
            val targetAuthority = filter.targetAuthority
            val targetRouteIdHash = targetRoute.originalIdHash.toString()
            val targetDirectionOriginalId = targetDirection.originalDirectionIdOrNull
            val rdTripUpdates = gTripUpdates
                .mapNotNull { gTripUpdate ->
                    gTripUpdate.optTrip?.let { it to gTripUpdate }
                }.filter { (td, _) ->
                    parseTripId(td)?.let { tripId ->
                        if (tripId !in tripIds) {
                            return@filter false
                        }
                    }
                    parseRouteId(td)?.let { routeIdHash ->
                        if (routeIdHash != targetRouteIdHash) {
                            return@filter false
                        }
                    }
                    td.optDirectionIdValid?.takeIf { !ignoreDirection }?.let { directionId ->
                        if (directionId != targetDirectionOriginalId) {
                            return@filter false
                        }
                    }
                    return@filter true
                }.takeIf { it.isNotEmpty() }
            rdTripUpdates ?: return null
            if (Constants.DEBUG) {
                MTLog.d(
                    LOG_TAG,
                    "makeCachedStatusFromAgencyData() > GTFS {R:'${targetRoute.shortestName}'|D:${targetDirection.headsignValue}} [${gTripUpdates.size}]: "
                )
                rdTripUpdates.forEach { (_, gTripUpdate) ->
                    MTLog.d(LOG_TAG, "makeCachedStatusFromAgencyData() > GTFS - ${gTripUpdate.toStringExt()}.")
                }
            }
            val sortedRDS = context.getRDS(targetAuthority, targetRoute.id, targetDirection.id)
            sortedRDS ?: return null
            val uuidSchedule = context.getRDSSchedule(targetAuthority, sortedRDS, filter.isIncludeCancelledTimestampsOrDefault)
                .takeIf { it.isNotEmpty() }
                ?.associateBy { it.targetUUID }
            uuidSchedule ?: return null
            processRDTripUpdates(rdTripUpdates, uuidSchedule, sortedRDS, filter.isIncludeCancelledTimestampsOrDefault)
            cacheRealTimeSchedules(uuidSchedule.values, sourceLabel, readFromSourceMs, readFromSourceMs)
            return getCachedStatusS(filter.targetUUID, tripIds)
        } catch (e: Exception) {
            MTLog.w(LOG_TAG, e, "makeCachedStatusFromAgencyData() > error!")
            return null
        }
    }

    private val OLDEST_FOR_REAL_TIME = 1.minutes
    private val MAX_FUTURE_FOR_REAL_TIME = 12.hours

    private fun GTFSRealTimeProvider.cacheRealTimeSchedules(
        scheduleList: Collection<Schedule>,
        sourceLabel: String,
        lastUpdateInMs: Long,
        readFromSourceMs: Long,
        ignorePastRealTime: Boolean = false,
        tripsWithRealTime: Set<String> = scheduleList
            .asSequence()
            .mapNotNull { schedule -> schedule.timestamps.takeIf { it.isNotEmpty() } }.flatten()
            .filter { it.isRealTime }
            .mapNotNull { it.tripId }
            .toSet() // distinct
    ) {
        scheduleList.forEach { schedule ->
            schedule.sourceLabel = sourceLabel
            schedule.lastUpdateInMs = lastUpdateInMs
            schedule.readFromSourceAtInMs = readFromSourceMs
            schedule.providerPrecisionInMs = PROVIDER_PRECISION_IN_MS
            schedule.validityInMs = TRIP_UPDATE_VALIDITY_IN_MS
            val now = TimeUtilsK.currentInstant()
            if (!schedule.timestamps.any { it.isRealTime || (it.tripId in tripsWithRealTime && it.departure < now) }) {
                cacheStatus(schedule.toNoData()) // avoid re-run
                return@forEach
            }
            var oldestDateForRealTime = now - OLDEST_FOR_REAL_TIME
            var maxFutureDateForRealTime = now + MAX_FUTURE_FOR_REAL_TIME
            val (past, future) = schedule.timestamps.partition { it.departure < now }
            if (!ignorePastRealTime) {
                oldestDateForRealTime = past.filter { it.isRealTime }.minOfOrNull { it.arrival } // all real-time
                    ?: oldestDateForRealTime
            }
            maxFutureDateForRealTime = future.take(10).maxOfOrNull { it.departure } // keep firsts 10
                ?.takeIf { it > maxFutureDateForRealTime }
                ?: maxFutureDateForRealTime
            maxFutureDateForRealTime = future.filter { it.isRealTime }.maxOfOrNull { it.departure } // all real-time
                ?.takeIf { it > maxFutureDateForRealTime }
                ?: maxFutureDateForRealTime
            // remove timestamps that are not real-time & outside of min/max date for real-time
            schedule.timestamps
                .filterNot {
                    it.isRealTime || oldestDateForRealTime < it.arrival && it.departure < maxFutureDateForRealTime
                }
                .forEach { timestamp ->
                    schedule.removeTimestamp(timestamp)
                }
            cacheStatus(schedule)
        }
    }

    @JvmStatic
    fun GTFSRealTimeProvider.getNew(statusFilter: StatusProviderContract.Filter): POIStatus? {
        val filter = statusFilter as? Schedule.ScheduleStatusFilter ?: run {
            MTLog.w(LOG_TAG, "getNew() > Can't find new schedule without schedule filter!")
            return null
        }
        updateAgencyDataIfRequired(filter.isInFocusOrDefault)
        return getCached(filter)
    }

    private fun GTFSRealTimeProvider.updateAgencyDataIfRequired(inFocus: Boolean) {
        val context = requireContextCompat()
        var inFocus = inFocus
        val lastUpdateCode = GtfsRealTimeStorage.getTripUpdateLastUpdateCode(context, -1).takeIf { it >= 0 }
        if (lastUpdateCode != null && lastUpdateCode != HttpURLConnection.HTTP_OK) {
            inFocus = true // force earlier retry if last fetch returned HTTP error
        }
        val minUpdateMs = min(statusMaxValidityInMs, getStatusValidityInMs(inFocus))
        val lastUpdateInMs = GtfsRealTimeStorage.getTripUpdateLastUpdateMs(context, 0L)
        if (lastUpdateInMs + minUpdateMs > TimeUtils.currentTimeMillis()) {
            return
        }
        updateAgencyDataIfRequiredSync(context, lastUpdateInMs, inFocus)
    }

    @Synchronized
    private fun GTFSRealTimeProvider.updateAgencyDataIfRequiredSync(context: Context, lastUpdateInMs: Long, inFocus: Boolean) {
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
                                    GFeedMessage.parseFrom(inputStream)
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
                                val responseBodyByes = response.body.bytes()
                                File(context.cacheDir, GTFS_RT_TRIP_UPDATE_PB_FILE_NAME).writeBytes(responseBodyByes)
                                gTripUpdates = GFeedMessage.parseFrom(responseBodyByes).entityList.toTripUpdates() // will be used soon
                                @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions")
                                if (Constants.DEBUG && PRINT_ALL_LOADED_TRIP_UPDATES) {
                                    MTLog.d(LOG_TAG, "loadAgencyDataFromWWW() > GTFS trip updates[${gTripUpdates?.size}]: ")
                                    gTripUpdates?.sortTripUpdates()?.forEach { gTripUpdate ->
                                        MTLog.d(LOG_TAG, "loadAgencyDataFromWWW() > - GTFS ${gTripUpdate.toStringExt()}")
                                    }
                                }
                            } catch (e: IOException) {
                                MTLog.w(LOG_TAG, e, "loadAgencyDataFromWWW() > error while saving GTFS RT Trip Updates data!")
                            }
                        } catch (e: Exception) {
                            MTLog.w(LOG_TAG, e, "loadAgencyDataFromWWW() > error while parsing GTFS Real Time data!")
                        }
                        return true
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
            GtfsRealTimeStorage.saveTripUpdateLastUpdateCode(context, 567) // SSL certificate not trusted (on this device)
            GtfsRealTimeStorage.saveTripUpdateLastUpdateMs(context, TimeUtils.currentTimeMillis())
            return false
        } catch (uhe: UnknownHostException) {
            if (MTLog.isLoggable(Log.DEBUG)) {
                MTLog.w(LOG_TAG, uhe, "No Internet Connection!")
            } else {
                MTLog.w(LOG_TAG, "No Internet Connection!")
            }
            return false
        } catch (se: SocketException) {
            MTLog.w(LOG_TAG, se, "No Internet Connection!")
            return false
        } catch (e: Exception) { // Unknown error
            MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception")
            return false
        }
    }
}
