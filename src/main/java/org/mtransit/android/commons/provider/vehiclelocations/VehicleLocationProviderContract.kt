package org.mtransit.android.commons.provider.vehiclelocations

import android.annotation.SuppressLint
import android.net.Uri
import android.provider.BaseColumns
import androidx.annotation.Discouraged
import org.json.JSONException
import org.json.JSONObject
import org.mtransit.android.commons.JSONUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.DefaultPOI
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.provider.common.ProviderContract
import org.mtransit.android.commons.provider.gtfs.GTFSRealTimeProviderFilter
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.commons.model.Secret

interface VehicleLocationProviderContract : ProviderContract {

    companion object {
        const val VEHICLE_LOCATION_PATH = "vehicle"

        const val PING_PATH = ProviderContract.PING_PATH

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

    val vehicleLocationMaxValidityInMs: Long

    fun getVehicleLocationValidityInMs(inFocus: Boolean): Long

    @Suppress("unused")
    fun getMinDurationBetweenVehicleLocationRefreshInMs(inFocus: Boolean): Long

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

    data class Filter @Discouraged("use secondary constructor() instead") constructor(
        override val cacheOnly: Boolean? = null,
        override val cacheValidityInMs: Long? = null,
        override val inFocus: Boolean? = null,
        override val providedEncryptKeysMap: Secret<Map<String, String>>? = null,
        val authority: String,
        override val poi: POI? = null, // RouteDirectionStop or DefaultPOI
        override val route: Route? = null,
        override val routeDirection: RouteDirection? = null,
    ) : ProviderContract.Filter(), GTFSRealTimeProviderFilter, MTLog.Loggable {

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

            fun fromJSONString(jsonString: String?): Filter? {
                try {
                    return if (jsonString == null) null else fromJSON(JSONObject(jsonString))
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while parsing JSON string '$jsonString'")
                    return null
                }
            }

            @SuppressLint("DiscouragedApi")
            fun fromJSON(json: JSONObject): Filter? {
                val poi = json.optJSONObject(JSON_POI)?.let { DefaultPOI.fromJSONStatic(it) }
                val authority = JSONUtils.optString(json, JSON_AUTHORITY) ?: poi?.authority ?: return null
                val route = json.optJSONObject(JSON_ROUTE)?.let { Route.fromJSON(it, authority) }
                val routeDirection = json.optJSONObject(JSON_ROUTE_DIRECTION)?.let { RouteDirection.fromJSON(it, authority) }
                return Filter(
                    cacheOnly = getCacheOnlyFromJSON(json),
                    cacheValidityInMs = getCacheValidityInMsFromJSON(json),
                    inFocus = getInFocusFromJSON(json),
                    providedEncryptKeysMap = getProvidedEncryptKeysMapFromJSON(json),
                    authority = authority,
                    poi = poi,
                    route = route,
                    routeDirection = routeDirection
                )
            }

            fun toJSONString(vehicleLocationFilter: Filter) =
                toJSON(vehicleLocationFilter)?.toString()

            fun toJSON(vehicleLocationFilter: Filter): JSONObject? {
                return try {
                    JSONObject().apply {
                        toJSON(vehicleLocationFilter, this)
                        put(JSON_AUTHORITY, vehicleLocationFilter.authority)
                        vehicleLocationFilter.poi?.let { put(JSON_POI, it.toJSON()) }
                        vehicleLocationFilter.route?.let { put(JSON_ROUTE, Route.toJSON(it)) }
                        vehicleLocationFilter.routeDirection?.let { put(JSON_ROUTE_DIRECTION, RouteDirection.toJSON(it)) }
                    }
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while making JSON object '$vehicleLocationFilter'!")
                    null
                }
            }
        }

        override fun getLogTag() = LOG_TAG

        @Suppress("unused") // used from main app
        override fun toJSONString() = toJSONString(this)

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
