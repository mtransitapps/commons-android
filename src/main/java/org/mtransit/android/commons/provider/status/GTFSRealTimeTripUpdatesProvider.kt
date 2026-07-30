package org.mtransit.android.commons.provider.status

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.Context
import androidx.annotation.VisibleForTesting
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
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.ALLOW_IGNORE_TRIP_DESCRIPTOR_DIRECTION_ID
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optArrival
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDelay
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDeparture
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDirectionIdValid
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optScheduleRelationship
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optScheduledTimeMs
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopSequence
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopTimeUpdateList
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimeMs
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTimestampMs
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripIdNotEmpty
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optVehicle
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
import org.mtransit.android.commons.provider.gtfs.storage
import org.mtransit.android.toDateTimeLog
import org.mtransit.android.toDurationLog
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
import com.google.transit.realtime.GtfsRealtime.FeedMessage as GFeedMessage
import com.google.transit.realtime.GtfsRealtime.TripDescriptor as GTripDescriptor
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship as GTDScheduleRelationship
import com.google.transit.realtime.GtfsRealtime.TripUpdate as GTripUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent as GTUStopTimeEvent

object GTFSRealTimeTripUpdatesProvider : MTLog.Loggable {

    internal val LOG_TAG: String = GTFSRealTimeTripUpdatesProvider::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    val PROVIDER_PRECISION = 10.seconds
    val PROVIDER_PRECISION_IN_MS = PROVIDER_PRECISION.inWholeMilliseconds

    @JvmStatic
    fun Long.adaptForCachedAPI(context: Context?) =
        if (context?.let { GTFSRealTimeProvider.getAGENCY_TRIP_UPDATES_URL_CACHED(it) }?.isNotBlank() == true) {
            this.coerceAtLeast(1.minutes.inWholeMilliseconds) // fewer calls to Cached API $$
        } else this

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
        }?.takeIf { tripIds -> tripIds.isNotEmpty() } // trip IDs REQUIRED for GTFS Trip Updates
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

    private const val DEBUG_STATIC_RT_MATCH = false
    // private const val DEBUG_STATIC_RT_MATCH = true // DEBUG

    private const val PRINT_ALL_RD_STOP_TIME_UPDATES = false
    // private const val PRINT_ALL_RD_STOP_TIME_UPDATES = true // DEBUG

    private fun GTripUpdate.isUseful(nowMs: Long): Boolean {
        optTimestampMs?.let { tuTimestamp ->
            if (tuTimestamp + TRIP_UPDATE_MAX_AGE_MS < nowMs) {
                MTLog.d(LOG_TAG, "isUseful() > IGNORE ${(nowMs - tuTimestamp).toDurationLog()} old: ${this.optTrip?.toStringExt(true)}")
                return false // not useful (too old to display)
            }
        }
        optStopTimeUpdateList
            ?.takeUnless { optVehicle?.hasId() == true } // SKIP FUTURE CHECK IF vehicle info provided
            ?.sortedBy { it.optStopSequence }
            ?.firstOrNull()
            ?.takeUnless { it.optDeparture?.hasDelay() == true || it.optArrival?.hasDelay() == true } // SKIP FUTURE CHECK IF delay available
            ?.takeUnless { it.optDeparture?.hasTimeDelay() == true || it.optArrival?.hasTimeDelay() == true } // SKIP FUTURE CHECK IF computed time delay available
            ?.let { firstStu ->
                val timeMs = firstStu.optDeparture?.optTimeMs ?: firstStu.optArrival?.optTimeMs ?: return@let
                if (nowMs + FUTURE_TRIP_UPDATE_MAX_DIFF_MS < timeMs) {
                    MTLog.d(LOG_TAG, "isUseful() > IGNORE ${(timeMs - nowMs).toDurationLog()} in the future: ${this.optTrip?.toStringExt(true)}")
                    return false // not useful (too far in advance to display)
                }
            }
        if (optDelay != null
            || optStopTimeUpdateList?.isNotEmpty() == true
        ) {
            return true // useful
        }
        optTrip?.let { td -> // cannot match w/ static data
            if (td.optScheduleRelationship?.let { it != GTDScheduleRelationship.SCHEDULED } == true) {
                return true // useful
            }
        }
        MTLog.w(LOG_TAG, "isUseful() > IGNORE (why?): ${this.toStringExt()}")
        return false // not useful
    }

    private fun GTUStopTimeEvent.hasTimeDelay() =
        timeDelayMs?.takeIf { it != 0L } != null

    private val GTUStopTimeEvent.timeDelayMs: Long?
        get() {
            val timeMs = this.optTimeMs ?: return null
            val scheduledTimeMs = this.optScheduledTimeMs ?: return null
            return (timeMs - scheduledTimeMs)
        }

    // Best practices: 90 seconds
    // https://gtfs.org/documentation/realtime/realtime-best-practices/#feed-publishing-general-practices
    // Montréal-based competitor: 10 minutes
    private val TRIP_UPDATE_MAX_AGE_MS = 90.times(7).seconds.inWholeMilliseconds

    // Montréal-based competitor: 4.5 hours (?)
    private val FUTURE_TRIP_UPDATE_MAX_DIFF_MS = 90.minutes.times(3).inWholeMilliseconds

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
                    MTLog.w(LOG_TAG, "makeCachedStatusFromAgencyData() > IGNORE cached feed (too old: ${feedTimestamp.toDateTimeLog()})")
                    return null
                }
            } ?: lastUpdateInMs
        val gTripUpdates = gTripUpdates ?: return null
        val sourceLabel = SourceUtils.getSourceLabel( // always use source from official API
            GTFSRealTimeProvider.getAgencyTripUpdatesUrlString(context, "T")
        )
        try {
            val (targetRoute, targetDirection) = filter.routeDirectionStop.let { it.route to it.direction }
            val targetAuthority = filter.targetAuthority
            val targetRouteIdHash = targetRoute.originalIdHash.toString()
            val targetDirectionOriginalId = targetDirection.originalDirectionIdOrNull
            if (DEBUG_STATIC_RT_MATCH) {
                MTLog.d(LOG_TAG, "makeCachedStatusFromAgencyData() > target trip IDs [${staticRDTripIds.size}]:")
                staticRDTripIds.chunked(9).forEach {
                    MTLog.d(LOG_TAG, "makeCachedStatusFromAgencyData() > - ${it.joinToString(",")}")
                }
            }
            val sortedRDS by lazy {
                context.getRDS(targetAuthority, targetRoute.id, targetDirection.id).orEmpty()
            }
            val rdSchedules: Collection<Schedule> by lazy {
                context.getRDSSchedule(targetAuthority, sortedRDS, filter.isIncludeCancelledTimestampsOrDefault)
            }
            val rdTripUpdates = gTripUpdates
                .filter { gTripUpdate ->
                    gTripUpdate.optTrip?.match(
                        targetRouteIdHash = targetRouteIdHash,
                        targetDirectionOriginalId = targetDirectionOriginalId,
                        staticRDTripIds = staticRDTripIds,
                        ignoreDirection = ignoreDirection,
                        parseRouteId = ::parseRouteId,
                        parseTripId = ::parseTripId,
                    ) == true
                }
                .filter { it.isUseful(nowMs) }
                .filterDuplicatesTrips()
                .takeIf { it.isNotEmpty() }
                ?.mapNotNull { gTripUpdate -> gTripUpdate.optTrip?.let { it to gTripUpdate } }
            rdTripUpdates ?: run {
                sortedRDS.forEach { rds ->
                    rds.makeSchedule(
                        lastUpdateInMs = lastUpdateInMs,
                        maxValidityInMs = statusMaxValidityInMs,
                        readFromSourceAtInMs = feedReadFromSourceMs,
                        providerPrecisionInMs = PROVIDER_PRECISION_IN_MS,
                        sourceLabel = sourceLabel,
                        noData = true, // NO DATA
                    ).let { noDataStatus ->
                        cacheStatus(noDataStatus)
                    }
                }
                MTLog.i(
                    LOG_TAG,
                    "No trip updates found for route '${targetRoute.shortestName}' direction '${targetDirection.headsignValue}'."
                )
                return null
            }
            val distinctTripId = rdTripUpdates.mapNotNull { it.first.optTripIdNotEmpty }.distinct()
            MTLog.i(
                LOG_TAG,
                "Using ${rdTripUpdates.size} trip updates for route '${targetRoute.shortestName}' direction '${targetDirection.headsignValue}': $distinctTripId."
            )
            if (Constants.DEBUG) {
                MTLog.d(
                    LOG_TAG,
                    "makeCachedStatusFromAgencyData() > GTFS {R:'${targetRoute.shortestName}'|D:${targetDirection.headsignValue}} [${gTripUpdates.size}]: "
                )
                rdTripUpdates.forEach { (_, gTripUpdate) ->
                    MTLog.d(LOG_TAG, "makeCachedStatusFromAgencyData() > GTFS - ${gTripUpdate.toStringExt()}.")
                    if (PRINT_ALL_RD_STOP_TIME_UPDATES) {
                        gTripUpdate.optStopTimeUpdateList?.forEachIndexed { idx, stu ->
                            MTLog.d(LOG_TAG, "makeCachedStatusFromAgencyData() > GTFS - [$idx] ${stu.toStringExt()}")
                        }
                    }
                }
            }
            if (sortedRDS.isEmpty()) return null
            if (rdSchedules.isEmpty()) return null
            processRDTripUpdates(
                rdTripUpdates = rdTripUpdates,
                rdSchedules = rdSchedules,
                sortedRDS = sortedRDS,
                feedReadFromSourceMs = feedReadFromSourceMs,
                includeCancelledTimestamps = filter.isIncludeCancelledTimestampsOrDefault,
            )
            cacheRealTimeSchedules(rdSchedules = rdSchedules, sourceLabel = sourceLabel, lastUpdateInMs = lastUpdateInMs)
            return getCachedStatusS(filter.targetUUID, staticRDTripIds)
        } catch (e: Exception) {
            MTLog.w(LOG_TAG, e, "makeCachedStatusFromAgencyData() > error!")
            return null
        }
    }

    // Montréal-based competitor: match with 'trip_id' first,
    // else use 'route_id', 'direction_id', 'start_date' & 'start_time' to try to match correct trip as fallback

    @VisibleForTesting
    internal fun GTripDescriptor.match(
        targetRouteIdHash: String,
        targetDirectionOriginalId: Int?,
        staticRDTripIds: Set<String>,
        ignoreDirection: Boolean,
        parseRouteId: (GTripDescriptor) -> String?,
        parseTripId: (GTripDescriptor) -> String?,
    ): Boolean {
        parseTripId(this)?.let { tripId ->
            if (tripId in staticRDTripIds) {
                return true // MATCH
            }
        }
        @Suppress("ConstantConditionIf")
        if (false) { // FIXME later try to match with route_id & direction_id & start date+time
            parseRouteId(this)?.let { rtRouteIdHash ->
                if (rtRouteIdHash != targetRouteIdHash) {
                    // if (DEBUG_STATIC_RT_MATCH) { // too much log
                    // MTLog.d(LOG_TAG, "match() > IGNORE: wrong route ID '$rtRouteIdHash' (t:$targetRouteIdHash)")
                    // }
                    return false // NOT A MATCH
                }
            }
            @Suppress("SimplifyBooleanWithConstants")
            if (!ALLOW_IGNORE_TRIP_DESCRIPTOR_DIRECTION_ID || optTripIdNotEmpty == null) {
                optDirectionIdValid?.takeIf { !ignoreDirection }?.let { directionId ->
                    if (directionId != targetDirectionOriginalId) {
                        if (DEBUG_STATIC_RT_MATCH) {
                            MTLog.d(
                                LOG_TAG,
                                "match() > IGNORE: wrong direction ID '$directionId' for ${toStringExt(short = true)}"
                            )
                        }
                        return false // NOT A MATCH
                    }
                }
            }
            parseTripId(this)?.let { tripId ->
                if (tripId !in staticRDTripIds) {
                    if (hasRouteId()) {
                        if (DEBUG_STATIC_RT_MATCH) {
                            MTLog.d(LOG_TAG, "match() > IGNORE: wrong trip ID ($tripId) for ${toStringExt(short = true)}")
                        }
                    }
                    return false // NOT A MATCH
                }
            }
            return true // MATCH
        }
        return false // NOT A MATCH
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
                    it.isRealTimeOrCancelled || oldestDateForRealTime < it.arrival && it.departure < maxFutureDateForRealTime
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
        if (storage.getTripUpdateLastUpdateMs(0L) > lastUpdateInMs) return  // too late, another thread already updated
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
                                @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions")
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
