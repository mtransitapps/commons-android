package org.mtransit.android.commons.provider.serviceupdate

import android.annotation.SuppressLint
import android.net.Uri
import android.provider.BaseColumns
import androidx.annotation.Discouraged
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.ServiceUpdates
import org.mtransit.android.commons.data.Targetable
import org.mtransit.android.commons.data.toPOI
import org.mtransit.android.commons.data.toRoute
import org.mtransit.android.commons.data.toRouteDirection
import org.mtransit.android.commons.provider.common.ProviderContract
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.commons.provider.gtfs.GTFSRealTimeProviderFilter
import org.mtransit.commons.model.Secret
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.mtransit.android.commons.data.toJson as toJsonK

interface ServiceUpdateProviderContract : ProviderContract {

    companion object {
        const val SERVICE_UPDATE_PATH = "service"

        val DEFAULT_SERVICE_UPDATE_MAX_VALIDITY_IN_MS = 1.days.inWholeMilliseconds

        val DEFAULT_SERVICE_UPDATE_VALIDITY_IN_MS = 1.hours.inWholeMilliseconds
        val DEFAULT_SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = 10.minutes.inWholeMilliseconds

        val DEFAULT_SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = 10.minutes.inWholeMilliseconds
        val DEFAULT_SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = 1.minutes.inWholeMilliseconds

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

    val serviceUpdateMaxValidityInMs: Long get() = DEFAULT_SERVICE_UPDATE_MAX_VALIDITY_IN_MS

    fun getServiceUpdateValidityInMs(inFocus: Boolean): Long =
        if (inFocus) DEFAULT_SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS else DEFAULT_SERVICE_UPDATE_VALIDITY_IN_MS

    fun getMinDurationBetweenServiceUpdateRefreshInMs(inFocus: Boolean): Long =
        if (inFocus) DEFAULT_SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS else DEFAULT_SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS

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

    data class Filter
    @Discouraged("use static methods instead")
    constructor(
        override val cacheOnly: Boolean? = null,
        override val cacheValidityInMs: Long? = null,
        override val inFocus: Boolean? = null,
        override val providedEncryptKeysMap: Secret<Map<String, String>>? = null,
        val authority: String?,
        override val poi: POI? = null, // RouteDirectionStop or DefaultPOI
        override val route: Route? = null,
        override val routeDirection: RouteDirection? = null,
    ) : ProviderContract.Filter(), GTFSRealTimeProviderFilter, MTLogK.Loggable {

        companion object {
            private val LOG_TAG = ServiceUpdateProviderContract::class.java.getSimpleName() + ">" + Filter::class.java.getSimpleName()

            @JvmOverloads
            @JvmStatic
            fun from(poi: POI, inFocus: Boolean? = null) = Filter(poi).copy(inFocus = inFocus)

            @JvmStatic
            fun String?.toFilterN() = this?.toFilter()
            fun String.toFilter() = try {
                sharedJson.parseToJsonElement(this).jsonObject.toFilter()
            } catch (se: SerializationException) {
                MTLogK.w(this@Companion, "Error while parsing JSON string '$this'", se)
                throw se
            }

            private const val JSON_POI = "poi"
            private const val JSON_ROUTE = "route"
            private const val JSON_ROUTE_DIRECTION = "routeDirection"
            private const val JSON_AUTHORITY = "authority"

            fun JsonObject.toFilter() =
                try {
                    val poi = this[JSON_POI]?.jsonObject?.toPOI()
                    val authority = this[JSON_AUTHORITY]?.jsonPrimitive?.contentOrNull ?: poi?.authority
                    val route = authority?.let { authority ->
                        this[JSON_ROUTE]?.jsonObject?.toRoute(authority)
                    }
                    val routeDirection = authority?.let { authority ->
                        this[JSON_ROUTE_DIRECTION]?.jsonObject?.toRouteDirection(authority)
                    }
                    //noinspection DiscouragedApi
                    Filter(
                        cacheOnly = getCacheOnlyFromJson(),
                        cacheValidityInMs = getCacheValidityInMsFromJson(),
                        inFocus = getInFocusFromJson(),
                        providedEncryptKeysMap = getProvidedEncryptKeysMapFromJson(),
                        authority = authority,
                        poi = poi,
                        route = route,
                        routeDirection = routeDirection,
                    ).takeIf {
                        (it.poi != null) || (it.route != null) || (it.routeDirection != null)
                    }
                } catch (se: SerializationException) {
                    MTLogK.w(LOG_TAG, "Error while parsing JSON object '$this'", se)
                    throw se
                }

            fun Filter.toJson() =
                try {
                    buildJsonObject {
                        toJsonCommon(this@toJson)
                        poi?.let { put(JSON_POI, it.toJson()) }
                        route?.let { put(JSON_ROUTE, it.toJsonK()) }
                        routeDirection?.let { put(JSON_ROUTE_DIRECTION, it.toJsonK()) }
                        authority?.let { put(JSON_AUTHORITY, it) }
                    }
                } catch (se: SerializationException) {
                    MTLogK.w(LOG_TAG, "Error while making JSON object '$this'", se)
                    throw se
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

        @Suppress("unused") // main app only
        fun copyWithCacheOnly(cacheOnly: Boolean): Filter = this.copy(
            cacheOnly = cacheOnly,
        )

        override fun toJsonString() = sharedJson.encodeToString(this.toJson())
    }
}
