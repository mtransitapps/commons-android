package org.mtransit.android.commons.provider.vehiclelocations

import android.annotation.SuppressLint
import android.net.Uri
import android.provider.BaseColumns
import androidx.annotation.Discouraged
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.toJson
import org.mtransit.android.commons.data.toPOI
import org.mtransit.android.commons.data.toRoute
import org.mtransit.android.commons.data.toRouteDirection
import org.mtransit.android.commons.provider.common.ProviderContract
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.commons.provider.gtfs.GTFSRealTimeProviderFilter
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull
import org.mtransit.commons.model.Secret
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

interface VehicleLocationProviderContract : ProviderContract {

    companion object {
        const val VEHICLE_LOCATION_PATH = "vehicle"

        const val PING_PATH = ProviderContract.PING_PATH

        val DEFAULT_VEHICLE_LOCATION_MAX_VALIDITY_IN_MS = 1.hours.inWholeMilliseconds

        val DEFAULT_VEHICLE_LOCATION_VALIDITY_IN_MS = 2.minutes.inWholeMilliseconds
        val DEFAULT_VEHICLE_LOCATION_VALIDITY_IN_FOCUS_IN_MS = 10.seconds.inWholeMilliseconds

        val DEFAULT_VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_MS = 1.minutes.inWholeMilliseconds
        val DEFAULT_VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = 5.seconds.inWholeMilliseconds

        /**
         * see [VehicleLocation]
         */
        val PROJECTION_VEHICLE_LOCATION = arrayOf(
            Columns.T_VEHICLE_LOCATION_K_ID,
            Columns.T_VEHICLE_LOCATION_K_TARGET_UUID,
            Columns.T_VEHICLE_LOCATION_K_TARGET_TRIP_ID,
            Columns.T_VEHICLE_LOCATION_K_LAST_UPDATE,
            Columns.T_VEHICLE_LOCATION_K_MAX_VALIDITY_IN_MS,
            //
            Columns.T_VEHICLE_LOCATION_K_VEHICLE_ID,
            Columns.T_VEHICLE_LOCATION_K_VEHICLE_LABEL,
            Columns.T_VEHICLE_LOCATION_K_VEHICLE_REPORT_TIMESTAMP,
            Columns.T_VEHICLE_LOCATION_K_LATITUDE,
            Columns.T_VEHICLE_LOCATION_K_LONGITUDE,
            Columns.T_VEHICLE_LOCATION_K_BEARING,
            Columns.T_VEHICLE_LOCATION_K_SPEED,
        )
    }

    val authority: String

    val authorityUri: Uri

    val vehicleLocationMaxValidityInMs: Long get() = DEFAULT_VEHICLE_LOCATION_MAX_VALIDITY_IN_MS

    fun getVehicleLocationValidityInMs(inFocus: Boolean): Long =
        if (inFocus) DEFAULT_VEHICLE_LOCATION_VALIDITY_IN_FOCUS_IN_MS else DEFAULT_VEHICLE_LOCATION_VALIDITY_IN_MS

    @Suppress("unused")
    fun getMinDurationBetweenVehicleLocationRefreshInMs(inFocus: Boolean): Long =
        if (inFocus) DEFAULT_VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS else DEFAULT_VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_MS

    fun cacheVehicleLocations(newVehicleLocations: List<VehicleLocation>)

    fun getCachedVehicleLocations(vehicleLocationFilter: Filter): List<VehicleLocation>?

    fun getNewVehicleLocations(vehicleLocationFilter: Filter): List<VehicleLocation>?

    fun deleteCachedVehicleLocation(vehicleLocationId: Int): Boolean
    fun purgeUselessCachedVehicleLocations(): Boolean

    val vehicleLocationDbTableName: String

    /**
     * see [VehicleLocation]
     */
    interface Columns {
        companion object {
            const val T_VEHICLE_LOCATION_K_ID: String = BaseColumns._ID
            const val T_VEHICLE_LOCATION_K_TARGET_UUID = "target"
            const val T_VEHICLE_LOCATION_K_TARGET_TRIP_ID = "target_trip_id"
            const val T_VEHICLE_LOCATION_K_LAST_UPDATE = "last_update"
            const val T_VEHICLE_LOCATION_K_MAX_VALIDITY_IN_MS = "max_validity"

            const val T_VEHICLE_LOCATION_K_VEHICLE_ID = "vehicle_id"
            const val T_VEHICLE_LOCATION_K_VEHICLE_LABEL = "vehicle_label"
            const val T_VEHICLE_LOCATION_K_VEHICLE_REPORT_TIMESTAMP = "report_timestamp"
            const val T_VEHICLE_LOCATION_K_LATITUDE = "latitude"
            const val T_VEHICLE_LOCATION_K_LONGITUDE = "longitude"
            const val T_VEHICLE_LOCATION_K_BEARING = "bearing"
            const val T_VEHICLE_LOCATION_K_SPEED = "speed"
        }
    }

    data class Filter
    @Discouraged("use secondary constructor() instead")
    constructor(
        override val cacheOnly: Boolean? = null,
        override val cacheValidityInMs: Long? = null,
        override val inFocus: Boolean? = null,
        override val providedEncryptKeysMap: Secret<Map<String, String>>? = null,
        val authority: String,
        override val poi: POI? = null, // RouteDirectionStop or DefaultPOI
        override val route: Route? = null,
        override val routeDirection: RouteDirection? = null,
    ) : ProviderContract.Filter(), GTFSRealTimeProviderFilter, MTLogK.Loggable {

        @SuppressLint("DiscouragedApi")
        constructor(poi: POI) : this(authority = poi.authority, poi = poi)

        @SuppressLint("DiscouragedApi")
        constructor(route: Route) : this(authority = route.authority, route = route)

        @SuppressLint("DiscouragedApi")
        constructor(routeDirection: RouteDirection) : this(authority = routeDirection.authority, routeDirection = routeDirection)

        companion object {
            private val LOG_TAG: String = VehicleLocationProviderContract::class.java.simpleName + ">" + Filter::class.java.simpleName

            private const val JSON_AUTHORITY = "authority"
            private const val JSON_POI = "poi"
            private const val JSON_ROUTE = "route"
            private const val JSON_ROUTE_DIRECTION = "routeDirection"

            @JvmStatic
            fun String?.toFilterN() = this?.toFilter()

            fun String.toFilter() = try {
                sharedJson.parseToJsonElement(this).jsonObject.toFilter()
            } catch (iae: IllegalArgumentException) {
                MTLogK.w(LOG_TAG, "Error while parsing JSON object '$this'!", iae)
                throw iae
            }

            @SuppressLint("DiscouragedApi")
            fun JsonObject.toFilter(): Filter? {
                val poi = this[JSON_POI]?.jsonObjectOrNull?.toPOI()
                val authority = this[JSON_AUTHORITY]?.jsonPrimitiveOrNull?.contentOrNull ?: poi?.authority ?: return null
                val route = this[JSON_ROUTE]?.jsonObjectOrNull?.toRoute(authority)
                val routeDirection = this[JSON_ROUTE]?.jsonObjectOrNull?.toRouteDirection(authority)
                return Filter(
                    cacheOnly = getCacheOnlyFromJson(),
                    cacheValidityInMs = getCacheValidityInMsFromJson(),
                    inFocus = getInFocusFromJson(),
                    providedEncryptKeysMap = getProvidedEncryptKeysMapFromJson(),
                    authority = authority,
                    poi = poi,
                    route = route,
                    routeDirection = routeDirection
                )
            }

            fun toJson(vehicleLocationFilter: Filter) = try {
                buildJsonObject {
                    toJsonCommon(vehicleLocationFilter)
                    put(JSON_AUTHORITY, vehicleLocationFilter.authority)
                    vehicleLocationFilter.poi?.let { put(JSON_POI, it.toJson()) }
                    vehicleLocationFilter.route?.let { put(JSON_ROUTE, it.toJson()) }
                    vehicleLocationFilter.routeDirection?.let { put(JSON_ROUTE_DIRECTION, it.toJson()) }
                }
            } catch (iae: IllegalArgumentException) {
                MTLogK.w(LOG_TAG, "Error while making JSON object '$vehicleLocationFilter'!", iae)
                throw iae
            }
        }

        override fun getLogTag() = LOG_TAG

        @Suppress("unused") // used from main app
        override fun toJsonString() = toJson(this).toString()

        val rds: RouteDirectionStop? get() = poi as? RouteDirectionStop

        private val _route: Route?
            get() = rds?.route
                ?: route
                ?: routeDirection?.route

        private val _direction: Direction?
            get() = rds?.direction
                ?: routeDirection?.direction

        val routeId: Long? get() = _route?.id

        val directionId: Long? get() = _direction?.id

        val targetAuthority: String?
            get() = poi?.authority
                ?: route?.authority
                ?: routeDirection?.authority

        @get:JvmName("getTargetUUID")
        val targetUuid: String?
            get() = poi?.uuid
                ?: route?.uuid
                ?: routeDirection?.uuid
    }
}
