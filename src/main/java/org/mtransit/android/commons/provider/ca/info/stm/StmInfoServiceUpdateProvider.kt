package org.mtransit.android.commons.provider.ca.info.stm

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.NetworkUtils
import org.mtransit.android.commons.SecurityUtils
import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.POI.POIUtils.makeUUID
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.data.makeServiceUpdate
import org.mtransit.android.commons.provider.StmInfoApiProvider
import org.mtransit.android.commons.provider.StmInfoApiProvider.getSERVICE_UPDATES_URL_CACHED
import org.mtransit.android.commons.provider.StmInfoApiProvider.getURL_HEADER_NAMES
import org.mtransit.android.commons.provider.StmInfoApiProvider.getURL_HEADER_VALUES
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateCleaner
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateProviderContract
import org.mtransit.android.commons.provider.serviceupdate.getCachedServiceUpdatesS
import org.mtransit.android.commons.provider.serviceupdate.getServiceUpdateValidity
import org.mtransit.android.commons.provider.serviceupdate.serviceUpdateMaxValidity
import org.mtransit.commons.SourceUtils
import retrofit2.create
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.UnknownHostException
import java.util.Locale
import javax.net.ssl.SSLHandshakeException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

object StmInfoServiceUpdateProvider : MTLog.Loggable {

    internal val LOG_TAG: String = StmInfoServiceUpdateProvider::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    @JvmStatic
    val SERVICE_UPDATE_MAX_VALIDITY_IN_MS = 1.days.inWholeMilliseconds
        // .takeUnless { Constants.DEBUG } ?: 1.minutes.inWholeMilliseconds

    val SERVICE_UPDATE_VALIDITY_IN_MS = 1.hours.inWholeMilliseconds
        // .takeUnless { Constants.DEBUG } ?: 1.minutes.inWholeMilliseconds
    val SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = 10.minutes.inWholeMilliseconds
        // .takeUnless { Constants.DEBUG } ?: 1.minutes.inWholeMilliseconds

    val SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = 10.minutes.inWholeMilliseconds
        // .takeUnless { Constants.DEBUG } ?: 1.minutes.inWholeMilliseconds
    val SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = 1.minutes.inWholeMilliseconds
        // .takeUnless { Constants.DEBUG } ?: 1.minutes.inWholeMilliseconds

    @JvmStatic
    fun getValidityInMs(inFocus: Boolean) =
        if (inFocus) SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS else SERVICE_UPDATE_VALIDITY_IN_MS

    @JvmStatic
    fun getMinDurationBetweenRefreshInMs(inFocus: Boolean) =
        if (inFocus) SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS else SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS

    @JvmStatic
    fun StmInfoApiProvider.getCached(filter: ServiceUpdateProviderContract.Filter): List<ServiceUpdate>? {
        return ((filter.poi as? RouteDirectionStop)?.getTargetUUIDs(includeStopTags = true)
            ?: filter.routeDirection?.getTargetUUIDs()
            ?: filter.route?.getTargetUUIDs())
            ?.let { targetUUIDs ->
                getCached(targetUUIDs)
            }
    }

    fun StmInfoApiProvider.getCached(targetUUIDs: Map<String, String>) = buildList {
        getCachedServiceUpdatesS(targetUUIDs.keys)?.let {
            addAll(it)
        }
    }.map { it.apply { targetUUID = targetUUIDs[it.targetUUID] ?: it.targetUUID } }

    @JvmStatic
    fun StmInfoApiProvider.getNew(filter: ServiceUpdateProviderContract.Filter): List<ServiceUpdate>? {
        updateAgencyDataIfRequired(filter.isInFocusOrDefault)
        return getCached(filter)
    }

    private fun StmInfoApiProvider.updateAgencyDataIfRequired(inFocus: Boolean) {
        val context = requireContextCompat()
        var inFocus = inFocus
        val lastUpdate = StmInfoServiceUpdateStorage.getServiceUpdateLastUpdate(context, TimeUtilsK.EPOCH_TIME_0)
        val lastUpdateCode = StmInfoServiceUpdateStorage.getServiceUpdateLastUpdateCode(context, -1).takeIf { it >= 0 }
        if (lastUpdateCode != null && lastUpdateCode != HttpURLConnection.HTTP_OK) {
            inFocus = true // force earlier retry if last fetch returned HTTP error
        }
        val minUpdate = serviceUpdateMaxValidity.coerceAtMost(getServiceUpdateValidity(inFocus))
        val now = TimeUtilsK.currentInstant()
        if (lastUpdate + minUpdate > now) {
            return
        }
        updateAgencyDataIfRequiredSync(lastUpdate, inFocus)
    }

    @Synchronized
    private fun StmInfoApiProvider.updateAgencyDataIfRequiredSync(lastUpdate: Instant, inFocus: Boolean) {
        val context = requireContextCompat()
        if (StmInfoServiceUpdateStorage.getServiceUpdateLastUpdate(context, TimeUtilsK.EPOCH_TIME_0) > lastUpdate) {
            return  // too late, another thread already updated
        }
        val now = TimeUtilsK.currentInstant()
        var deleteAllRequired = false
        if (lastUpdate + serviceUpdateMaxValidity < now) {
            deleteAllRequired = true // too old to display
        }
        val minUpdate = serviceUpdateMaxValidity.coerceAtMost(getServiceUpdateValidity(inFocus))
        if (!deleteAllRequired && lastUpdate + minUpdate >= now) {
            return
        }
        updateAllAgencyDataFromWWW(context, deleteAllRequired) // try to update
    }

    private fun StmInfoApiProvider.updateAllAgencyDataFromWWW(context: Context, deleteAllRequired: Boolean) {
        var deleteAllDone = false
        if (deleteAllRequired) {
            deleteAllAgencyServiceUpdateData()
            deleteAllDone = true
        }
        val newServiceUpdates = loadAgencyDataFromWWW(context)
        if (newServiceUpdates != null) { // empty is OK
            if (!deleteAllDone) {
                deleteAllAgencyServiceUpdateData()
            }
            cacheServiceUpdates(newServiceUpdates.toList())
        } // else keep whatever we have until max validity reached
    }

    private var _stmInfoApi: StmInfoApi? = null

    private fun getStmInfoApi(context: Context) =
        _stmInfoApi ?: createStmInfoApi(context).also { _stmInfoApi = it }

    private fun createStmInfoApi(context: Context): StmInfoApi {
        val retrofit = NetworkUtils.makeNewRetrofitWithGson(
            baseHostUrl = StmInfoApi.BASE_HOST_URL,
            context = context,
        )

        return retrofit.create()
    }

    @JvmStatic
    val serviceUpdateLanguage: String get() = if (LocaleUtils.isFR()) Locale.FRENCH.language else DEFAULT_LANGUAGE

    private val DEFAULT_LANGUAGE: String = Locale.ENGLISH.language

    private const val SERVICE_UPDATE_URL = "https://api.stm.info/pub/od/i3/v2/messages/etatservice"

    private fun StmInfoApiProvider.loadAgencyDataFromWWW(context: Context): Collection<ServiceUpdate>? {
        try {
            val call = getSERVICE_UPDATES_URL_CACHED(context).takeIf { it.isNotBlank() }?.let { urlCachedString ->
                getStmInfoApi(context).getV2MessageEtatService(urlCachedString)
            } ?: run {
                val agencyUrlHeaderNames = getURL_HEADER_NAMES(context)
                val agencyUrlHeaderValues = getURL_HEADER_VALUES(context)
                if (agencyUrlHeaderNames.size != agencyUrlHeaderValues.size) {
                    MTLog.w(this@StmInfoServiceUpdateProvider, "ERROR: agencyUrlHeaderNames.size != agencyUrlHeaderValues.size!")
                    return null
                }
                val headers: Map<String, String> = agencyUrlHeaderNames.zip(agencyUrlHeaderValues).associate { (name, value) ->
                    name to value
                }
                getStmInfoApi(context).getV2MessageEtatService(SERVICE_UPDATE_URL, headers = headers)
            }
            call.execute().let { response ->
                val now = TimeUtilsK.currentInstant()
                StmInfoServiceUpdateStorage.saveServiceUpdateLastUpdateCode(context, response.code())
                StmInfoServiceUpdateStorage.saveServiceUpdateLastUpdate(context, now)
                when (response.code()) {
                    HttpURLConnection.HTTP_OK -> {
                        val sourceLabel = SourceUtils.getSourceLabel( // always use source from official API
                            SERVICE_UPDATE_URL
                        )
                        val etatServiceResponse = response.body()
                        val serviceUpdates = etatServiceResponse.toServiceUpdates(
                            maxValidity = serviceUpdateMaxValidity,
                            sourceLabel = sourceLabel,
                            now = now,
                        )
                        MTLog.i(this@StmInfoServiceUpdateProvider, "Found %d service updates.", serviceUpdates.size)
                        if (Constants.DEBUG) {
                            for (serviceUpdate in serviceUpdates) {
                                MTLog.d(this@StmInfoServiceUpdateProvider, "loadAgencyServiceUpdateDataFromWWW() > service update: %s.", serviceUpdate)
                            }
                        }
                        return serviceUpdates
                    }

                    else -> {
                        MTLog.w(
                            this@StmInfoServiceUpdateProvider,
                            "ERROR: HTTP URL-Connection Response Code ${response.code()} (Message: ${response.message()})"
                        )
                        return null
                    }
                }
            }

        } catch (sslhe: SSLHandshakeException) {
            MTLog.w(this, sslhe, "SSL error!")
            SecurityUtils.logCertPathValidatorException(sslhe)
            StmInfoServiceUpdateStorage.saveServiceUpdateLastUpdateCode(context, 567) // SSL certificate not trusted (on this device)
            StmInfoServiceUpdateStorage.saveServiceUpdateLastUpdate(context, TimeUtilsK.currentInstant())
            return null
        } catch (uhe: UnknownHostException) {
            if (MTLog.isLoggable(Log.DEBUG)) {
                MTLog.w(this@StmInfoServiceUpdateProvider, uhe, "No Internet Connection!")
            } else {
                MTLog.w(this@StmInfoServiceUpdateProvider, "No Internet Connection!")
            }
            return null
        } catch (se: SocketException) {
            MTLog.w(this@StmInfoServiceUpdateProvider, se, "No Internet Connection!")
            return null
        } catch (e: Exception) { // Unknown error
            MTLog.e(this@StmInfoServiceUpdateProvider, e, "INTERNAL ERROR: Unknown Exception")
            return null
        }
    }

    @VisibleForTesting
    internal fun EtatServiceResponse?.toServiceUpdates(
        maxValidity: Duration,
        sourceLabel: String,
        now: Instant,
    ): Collection<ServiceUpdate> {
        val serviceUpdates = mutableSetOf<ServiceUpdate>()
        val alerts = this?.alerts?.takeIf { it.isNotEmpty() } ?: return serviceUpdates
        val headerTimestamp = this.header?.timestamp ?: now
        alerts.forEach { alert ->
            if (!alert.isActive()) {
                MTLog.d(this@StmInfoServiceUpdateProvider, "Ignore inactive alert. ($alert)")
                return@forEach
            }
            val informedEntities = alert.informedEntities?.takeIf { it.isNotEmpty() }
                ?: run {
                    MTLog.w(this@StmInfoServiceUpdateProvider, "Ignore alert w/o informed entities! ($alert)")
                    return@forEach
                }
            val routeShortNames = informedEntities.mapNotNull { it.routeShortName }.takeIf { it.isNotEmpty() }
                ?: run {
                    MTLog.w(this@StmInfoServiceUpdateProvider, "Ignore alert w/o route short names! ($alert)")
                    return@forEach
                }
            val directionId = informedEntities.singleOrNull { !it.directionId.isNullOrBlank() }?.directionId
            val stopIds = informedEntities.mapNotNull { it.stopCode }.toSet()

            val targetUUIDs: Set<String> = buildSet {
                routeShortNames.forEach { routeShortName ->
                    if (stopIds.isEmpty()) {
                        (getAgencyRouteDirectionTagTargetUUID(routeShortName, directionId)
                            ?: getAgencyRouteTagTargetUUID(routeShortName)).let {
                            add(it)
                        }
                    } else {
                        stopIds.forEach { stopId ->
                            (getAgencyRouteDirectionStopTagTargetUUID(routeShortName, directionId, stopId)
                                ?: getAgencyRouteStopTagTargetUUID(routeShortName, stopId)).let {
                                add(it)
                            }
                        }
                    }
                }
            }
            val headerTexts = alert.headerTexts?.parseTranslations()
            val descriptionTexts = alert.descriptionTexts?.parseTranslations()
            val languages = headerTexts?.keys.orEmpty() + descriptionTexts?.keys.orEmpty()
            if (languages.isEmpty()) {
                MTLog.w(this@StmInfoServiceUpdateProvider, "Ignore alert w/o translations! ($alert)")
                return@forEach
            }
            targetUUIDs.forEach { targetUUID ->
                val severity = if (stopIds.isNotEmpty()) {
                    ServiceUpdate.SEVERITY_WARNING_POI
                } else {
                    ServiceUpdate.SEVERITY_INFO_RELATED_POI
                } // else ServiceUpdate.SEVERITY_INFO_UNKNOWN?
                languages.forEach { language ->
                    val header = headerTexts?.get(language)
                    val description = descriptionTexts?.get(language)
                        ?: return@forEach // no description == no service update to show
                    val replacement = ServiceUpdateCleaner.getReplacement(severity)
                    val descriptionHtml = description.let {
                        var textHtml = it
                        textHtml = HtmlUtils.toHTML(textHtml)
                        textHtml = HtmlUtils.fixTextViewBR(textHtml)
                        textHtml = ServiceUpdateCleaner.clean(textHtml, replacement, language)
                        textHtml
                    }
                    serviceUpdates.add(
                        makeServiceUpdate(
                            targetUUID = targetUUID,
                            lastUpdate = headerTimestamp,
                            maxValidity = maxValidity,
                            text = ServiceUpdateCleaner.makeText(header, description),
                            optTextHTML = ServiceUpdateCleaner.makeTextHTML(header, descriptionHtml),
                            severity = severity,
                            sourceId = StmInfoApiProvider.SERVICE_UPDATE_SOURCE_ID,
                            sourceLabel = sourceLabel,
                            language = language
                        )
                    )
                }
            }
        }
        return serviceUpdates
    }

    @VisibleForTesting
    internal fun List<EtatServiceResponse.Alert.TranslatedText>.parseTranslations(): Map<String, String>? {
        this.takeIf { it.isNotEmpty() } ?: return null
        var hasDefaultLanguage = false
        val translations = this.mapNotNull { translatedText ->
            val text = translatedText.text ?: return@mapNotNull null
            val language = translatedText.language ?: DEFAULT_LANGUAGE
            if (language == DEFAULT_LANGUAGE) hasDefaultLanguage = true
            language to text
        }.toMap()
        if (!hasDefaultLanguage) {
            this.firstOrNull()?.text?.let {
                return translations + (DEFAULT_LANGUAGE to it)
            }
        }
        return translations
    }

    private fun EtatServiceResponse.Alert.isActive(now: Instant = TimeUtilsK.currentInstant()): Boolean {
        activePeriods?.start?.let { start ->
            if (now < start) return false // not yet
        }
        activePeriods?.end?.let { end ->
            if (end < now) return false // too late
        }
        return true
    }

    private const val AGENCY_TAG = "StmInfo"

    private fun getAgencyRouteTagTargetUUID(routeShortName: String) =
        makeUUID(AGENCY_TAG, "rsn$routeShortName")

    private fun getAgencyRouteStopTagTargetUUID(routeShortName: String, stopCode: String) =
        makeUUID(AGENCY_TAG, "rsn$routeShortName", "sc$stopCode")

    private fun getAgencyRouteDirectionTagTargetUUID(routeShortName: String, directionHeadsignValue: String?) =
        directionHeadsignValue?.let { makeUUID(AGENCY_TAG, "rsn$routeShortName", "dhv$it") }

    private fun getAgencyRouteDirectionStopTagTargetUUID(routeShortName: String, directionHeadsignValue: String?, stopCode: String) =
        directionHeadsignValue?.let { makeUUID(AGENCY_TAG, "rsn$routeShortName", "dhv$directionHeadsignValue", "sc$stopCode") }

    private fun RouteDirectionStop.getRouteTag() = this.route.shortName
    private fun RouteDirectionStop.getDirectionTag() = this.direction.headsignValue
        .takeIf { this.direction.headsignType == Direction.HEADSIGN_TYPE_DIRECTION }

    private fun RouteDirectionStop.getStopTag() = this.stop.code

    private fun RouteDirectionStop.getTargetUUIDs(
        includeStopTags: Boolean = false
    ): Map<String, String> = buildMap {
        put(getAgencyRouteTagTargetUUID(getRouteTag()), route.uuid)
        getAgencyRouteDirectionTagTargetUUID(getRouteTag(), getDirectionTag())?.let { put(it, routeDirectionUUID) }
        if (includeStopTags) {
            put(getAgencyRouteStopTagTargetUUID(getRouteTag(), getStopTag()), uuid)
            getAgencyRouteDirectionStopTagTargetUUID(getRouteTag(), getDirectionTag(), getStopTag())?.let { put(it, uuid) }
        }
    }

    private fun RouteDirection.getRouteTag() = this.route.shortName
    private fun RouteDirection.getDirectionTag() = this.direction.headsignValue

    private fun RouteDirection.getTargetUUIDs(
    ): Map<String, String> = buildMap {
        put(getAgencyRouteTagTargetUUID(getRouteTag()), route.uuid)
        getAgencyRouteDirectionTagTargetUUID(getRouteTag(), getDirectionTag())?.let { put(it, uuid) }
    }

    private fun Route.getRouteTag() = this.shortName

    private fun Route.getTargetUUIDs(
    ): Map<String, String> = buildMap {
        put(getAgencyRouteTagTargetUUID(getRouteTag()), uuid)
    }
}

