package org.mtransit.android.commons.provider.serviceupdate

import android.annotation.SuppressLint
import android.net.Uri
import android.provider.BaseColumns
import androidx.annotation.Discouraged
import org.json.JSONException
import org.json.JSONObject
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.DefaultPOI
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.ServiceUpdates
import org.mtransit.android.commons.data.Targetable
import org.mtransit.android.commons.optString
import org.mtransit.android.commons.provider.common.ProviderContract
import org.mtransit.android.commons.provider.gtfs.GTFSRealTimeProviderFilter
import org.mtransit.commons.model.Secret

interface ServiceUpdateProviderContract : ProviderContract {

    companion object {
        const val SERVICE_UPDATE_PATH = "service"

        @JvmStatic
        val PROJECTION_SERVICE_UPDATE = arrayOf(
            Columns.T_SERVICE_UPDATE_K_ID,
            Columns.T_SERVICE_UPDATE_K_TARGET_UUID,
            Columns.T_SERVICE_UPDATE_K_TARGET_TRIP_ID,
            Columns.T_SERVICE_UPDATE_K_LAST_UPDATE,
            Columns.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS,
            Columns.T_SERVICE_UPDATE_K_SEVERITY,
            Columns.T_SERVICE_UPDATE_K_TEXT,
            Columns.T_SERVICE_UPDATE_K_TEXT_HTML,
            Columns.T_SERVICE_UPDATE_K_LANGUAGE,
            Columns.T_SERVICE_UPDATE_K_ORIGINAL_ID,
            Columns.T_SERVICE_UPDATE_K_SOURCE_LABEL,
            Columns.T_SERVICE_UPDATE_K_SOURCE_ID,
            Columns.T_SERVICE_UPDATE_K_NO_SERVICE
        )
    }

    val authority: String

    val authorityUri: Uri

    val serviceUpdateMaxValidityInMs: Long

    fun getServiceUpdateValidityInMs(inFocus: Boolean): Long

    fun getMinDurationBetweenServiceUpdateRefreshInMs(inFocus: Boolean): Long

    fun cacheServiceUpdates(newServiceUpdates: ServiceUpdates)

    fun getCachedServiceUpdates(serviceUpdateFilter: Filter): ServiceUpdates?

    fun getNewServiceUpdates(serviceUpdateFilter: Filter): ServiceUpdates?

    fun deleteCachedServiceUpdate(serviceUpdateId: Int): Boolean

    fun deleteCachedServiceUpdate(targetUUID: String, sourceId: String): Boolean

    fun purgeUselessCachedServiceUpdates(): Boolean

    val serviceUpdateDbTableName: String

    val serviceUpdateLanguage: String

    object Columns {
        const val T_SERVICE_UPDATE_K_ID: String = BaseColumns._ID
        const val T_SERVICE_UPDATE_K_TARGET_UUID = "target"
        const val T_SERVICE_UPDATE_K_TARGET_TRIP_ID = "trip_id"
        const val T_SERVICE_UPDATE_K_LAST_UPDATE = "last_update"
        const val T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS = "max_validity"
        const val T_SERVICE_UPDATE_K_SEVERITY = "severity"
        const val T_SERVICE_UPDATE_K_TEXT = "text"
        const val T_SERVICE_UPDATE_K_TEXT_HTML = "text_html"
        const val T_SERVICE_UPDATE_K_LANGUAGE = "lang"
        const val T_SERVICE_UPDATE_K_SOURCE_LABEL = "source_label"
        const val T_SERVICE_UPDATE_K_ORIGINAL_ID = "original_id"
        const val T_SERVICE_UPDATE_K_SOURCE_ID = "source_id"
        const val T_SERVICE_UPDATE_K_NO_SERVICE = "no_service"
    }

    data class Filter @Discouraged("use static methods instead") constructor(
        override val cacheOnly: Boolean? = null,
        override val cacheValidityInMs: Long? = null,
        override val inFocus: Boolean? = null,
        override val providedEncryptKeysMap: Secret<Map<String, String>>? = null,
        val authority: String?,
        override val poi: POI? = null, // RouteDirectionStop or DefaultPOI
        override val route: Route? = null,
        override val routeDirection: RouteDirection? = null,
    ) : ProviderContract.Filter(), GTFSRealTimeProviderFilter, MTLog.Loggable {

        companion object {
            private val LOG_TAG = ServiceUpdateProviderContract::class.java.getSimpleName() + ">" + Filter::class.java.getSimpleName()

            @JvmOverloads
            @JvmStatic
            fun from(poi: POI, inFocus: Boolean? = null) = Filter(poi).copy(inFocus = inFocus)

            @JvmStatic
            fun fromJSONString(jsonString: String?): Filter? {
                try {
                    return jsonString?.let { fromJSON(JSONObject(it)) }
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while parsing JSON string '%s'", jsonString)
                    return null
                }
            }

            private const val JSON_POI = "poi"
            private const val JSON_ROUTE = "route"
            private const val JSON_ROUTE_DIRECTION = "routeDirection"
            private const val JSON_AUTHORITY = "authority"

            fun fromJSON(json: JSONObject): Filter? {
                try {
                    val poi = json.optJSONObject(JSON_POI)?.let { DefaultPOI.fromJSONStatic(it) }
                    val authority = json.optString(JSON_AUTHORITY, fallback = null)
                    val route = authority?.let { authority ->
                        json.optJSONObject(JSON_ROUTE)?.let { Route.fromJSON(it, authority) }
                    }
                    val routeDirection = authority?.let { authority ->
                        json.optJSONObject(JSON_ROUTE_DIRECTION)?.let { RouteDirection.fromJSON(it, authority) }
                    }
                    //noinspection DiscouragedApi
                    return Filter(
                        cacheOnly = getCacheOnlyFromJSON(json),
                        cacheValidityInMs = getCacheValidityInMsFromJSON(json),
                        inFocus = getInFocusFromJSON(json),
                        providedEncryptKeysMap = getProvidedEncryptKeysMapFromJSON(json),
                        authority = authority,
                        poi = poi,
                        route = route,
                        routeDirection = routeDirection,
                    ).takeIf {
                        (it.poi != null) || (it.route != null) || (it.routeDirection != null)
                    }
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '$json'")
                    return null
                }
            }

            fun toJSONString(serviceUpdateFilter: Filter) = toJSON(serviceUpdateFilter)?.toString()

            fun toJSON(serviceUpdateFilter: Filter): JSONObject? {
                try {
                    return JSONObject().apply {
                        toJSON(serviceUpdateFilter, this)
                        if (serviceUpdateFilter.poi != null) {
                            put(JSON_POI, serviceUpdateFilter.poi.toJSON())
                        }
                        if (serviceUpdateFilter.route != null) {
                            put(JSON_ROUTE, Route.toJSON(serviceUpdateFilter.route))
                        }
                        if (serviceUpdateFilter.routeDirection != null) {
                            put(JSON_ROUTE_DIRECTION, RouteDirection.toJSON(serviceUpdateFilter.routeDirection))
                        }
                        if (serviceUpdateFilter.authority != null) {
                            put(JSON_AUTHORITY, serviceUpdateFilter.authority)
                        }
                    }
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while making JSON object '$serviceUpdateFilter'")
                    return null
                }
            }
        }

        override fun getLogTag() = LOG_TAG

        @SuppressLint("DiscouragedApi")
        constructor(poi: POI) : this(
            authority = poi.getAuthority(),
            poi = poi,
        )

        @SuppressLint("DiscouragedApi")
        constructor(authority: String, route: Route) : this(
            authority = authority,
            route = route,
            poi = null,
        )

        @SuppressLint("DiscouragedApi")
        constructor(authority: String, routeDirection: RouteDirection) : this(
            authority = authority,
            routeDirection = routeDirection,
            poi = null,
        )

        override fun toString() = buildString {
            append(Filter::class.java.getSimpleName())
            append(super.toStringParts())
            authority?.let { append("authority:").append(it).append(',') }
            poi?.let { append("poi:").append(it).append(',') }
            route?.let { append("route:").append(it).append(',') }
            routeDirection?.let { append("routeDirection:").append(it).append(',') }
        }

        val target: Targetable? get() = this.poi ?: this.route ?: this.routeDirection

        val targetUUID: String? get() = this.target?.uUID

        val targetAuthority: String? get() = this.poi?.authority ?: this.route?.authority ?: this.routeDirection?.authority

        val targetRoute: Route? get() = (this.poi as? RouteDirectionStop)?.route ?: this.route ?: this.routeDirection?.route

        val targetRouteId: Long? get() = this.targetRoute?.id

        val targetDirection: Direction? get() = (this.poi as? RouteDirectionStop)?.direction ?: this.routeDirection?.direction

        val targetDirectionId: Long? get() = this.targetDirection?.id

        override fun toJSONString() = toJSONString(this)
    }
}
