package org.mtransit.android.commons.provider.vehiclelocations

import android.annotation.SuppressLint
import android.net.Uri
import android.provider.BaseColumns
import androidx.annotation.Discouraged
import org.json.JSONException
import org.json.JSONObject
import org.mtransit.android.commons.JSONUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.MTLog.Loggable
import org.mtransit.android.commons.data.DefaultPOI
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.provider.common.ProviderContract
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation

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
            Columns.T_VEHICLE_LOCATION_K_LATITUDE,
            Columns.T_VEHICLE_LOCATION_K_LONGITUDE,
            Columns.T_VEHICLE_LOCATION_K_BEARING,
            Columns.T_VEHICLE_LOCATION_K_SPEED,
        )
    }

    val authorityUri: Uri

    val vehicleLocationMaxValidityInMs: Long

    fun getVehicleLocationValidityInMs(inFocus: Boolean): Long

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
            const val T_VEHICLE_LOCATION_K_LATITUDE = "latitude"
            const val T_VEHICLE_LOCATION_K_LONGITUDE = "longitude"
            const val T_VEHICLE_LOCATION_K_BEARING = "bearing"
            const val T_VEHICLE_LOCATION_K_SPEED = "speed"
        }
    }


    data class Filter @Discouraged("use from() instead") constructor(
        val authority: String,
        val poi: POI? = null, // RouteDirectionStop or DefaultPOI
        val route: Route? = null,
        val routeDirection: RouteDirection? = null,
        val tripIds: List<String>?, // original // GTFS // cleaned
        val inFocus: Boolean?,
        val cacheOnly: Boolean?,
        val providedEncryptKeysMap: Map<String, String>?,
    ) : Loggable {

        @SuppressLint("DiscouragedApi")
        constructor(
            poi: POI,
            tripIds: List<String>? = null,
            inFocus: Boolean? = null,
            cacheOnly: Boolean? = null,
            providedEncryptKeysMap: Map<String, String>? = null
        ) : this(
            authority = poi.authority,
            poi = poi,
            tripIds = tripIds,
            inFocus = inFocus,
            cacheOnly = cacheOnly,
            providedEncryptKeysMap = providedEncryptKeysMap,
        )

        @SuppressLint("DiscouragedApi")
        constructor(
            route: Route,
            tripIds: List<String>? = null,
            inFocus: Boolean? = null,
            cacheOnly: Boolean? = null,
            providedEncryptKeysMap: Map<String, String>? = null,
        ) : this(
            authority = route.authority,
            route = route,
            tripIds = tripIds,
            inFocus = inFocus,
            cacheOnly = cacheOnly,
            providedEncryptKeysMap = providedEncryptKeysMap,
        )

        @SuppressLint("DiscouragedApi")
        constructor(
            routeDirection: RouteDirection,
            tripIds: List<String>? = null,
            inFocus: Boolean? = null,
            cacheOnly: Boolean? = null,
            providedEncryptKeysMap: Map<String, String>? = null,
        ) : this(
            authority = routeDirection.authority,
            routeDirection = routeDirection,
            tripIds = tripIds,
            inFocus = inFocus,
            cacheOnly = cacheOnly,
            providedEncryptKeysMap = providedEncryptKeysMap,
        )

        val inFocusOrDefault = inFocus ?: false

        fun getProvidedEncryptKey(key: String) =
            this.providedEncryptKeysMap?.get(key)?.takeIf { it.isNotBlank() }

        companion object {
            private val LOG_TAG: String = VehicleLocationProviderContract::class.java.simpleName + ">" + Filter::class.java.simpleName

            private const val JSON_AUTHORITY = "authority"
            private const val JSON_POI = "poi"
            private const val JSON_ROUTE = "route"
            private const val JSON_ROUTE_DIRECTION = "routeDirection"
            private const val JSON_TRIP_IDS = "tripIds"
            private const val JSON_CACHE_ONLY = "cacheOnly"
            private const val JSON_IN_FOCUS = "inFocus"
            private const val JSON_PROVIDED_ENCRYPT_KEYS_MAP = "providedEncryptKeysMap"

            fun fromJSONString(jsonString: String?): Filter? {
                try {
                    return if (jsonString == null) null else fromJSON(JSONObject(jsonString))
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while parsing JSON string '%s'", jsonString)
                    return null
                }
            }

            @SuppressLint("DiscouragedApi")
            fun fromJSON(json: JSONObject): Filter? {
                val poi = json.optJSONObject(JSON_POI)?.let { jPoi ->
                    DefaultPOI.fromJSONStatic(jPoi)
                }
                val authority = JSONUtils.optString(json, JSON_AUTHORITY)
                val route = json.optJSONObject(JSON_ROUTE)?.let { jRoute ->
                    authority?.let { Route.fromJSON(jRoute, it) }
                }
                val routeDirection = json.optJSONObject(JSON_ROUTE_DIRECTION)?.let { jRouteDirection ->
                    authority?.let { RouteDirection.fromJSON(jRouteDirection, it) }
                }
                val tripIds = json.optJSONArray(JSON_TRIP_IDS)?.let { jTripIds ->
                    buildList {
                        for (i in 0 until jTripIds.length()) {
                            add(jTripIds.getString(i))
                        }
                    }
                }
                val inFocus = JSONUtils.optBoolean(json, JSON_IN_FOCUS)
                val cacheOnly = JSONUtils.optBoolean(json, JSON_CACHE_ONLY)
                val providedEncryptKeysMap: Map<String, String>? = json.optJSONObject(JSON_PROVIDED_ENCRYPT_KEYS_MAP)?.let { jProvidedEncryptKeysMap ->
                    JSONUtils.toMapOfStrings(jProvidedEncryptKeysMap)
                }
                return poi?.let {
                    Filter(
                        authority = it.authority,
                        poi = it,
                        tripIds = tripIds,
                        inFocus = inFocus,
                        cacheOnly = cacheOnly,
                        providedEncryptKeysMap = providedEncryptKeysMap
                    )
                } ?: route?.let {
                    Filter(
                        authority = route.authority,
                        route = it,
                        tripIds = tripIds,
                        inFocus = inFocus,
                        cacheOnly = cacheOnly,
                        providedEncryptKeysMap = providedEncryptKeysMap
                    )
                }
                ?: routeDirection?.let {
                    Filter(
                        authority = routeDirection.authority,
                        routeDirection = it,
                        tripIds = tripIds,
                        inFocus = inFocus,
                        cacheOnly = cacheOnly,
                        providedEncryptKeysMap = providedEncryptKeysMap
                    )
                }
            }

            fun toJSONString(vehicleLocationFilter: Filter) =
                toJSON(vehicleLocationFilter)?.toString()

            fun toJSON(vehicleLocationFilter: Filter): JSONObject? {
                return try {
                    JSONObject().apply {
                        put(JSON_AUTHORITY, vehicleLocationFilter.authority)
                        vehicleLocationFilter.poi?.let { put(JSON_POI, it.toJSON()) }
                        vehicleLocationFilter.route?.let { put(JSON_ROUTE, Route.toJSON(it)) }
                        vehicleLocationFilter.routeDirection?.let { put(JSON_ROUTE_DIRECTION, RouteDirection.toJSON(it)) }
                        vehicleLocationFilter.tripIds?.let { put(JSON_TRIP_IDS, it) }
                        vehicleLocationFilter.providedEncryptKeysMap?.let { put(JSON_PROVIDED_ENCRYPT_KEYS_MAP, JSONUtils.toJSONObject(it)) }
                    }
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while making JSON object '%s'", vehicleLocationFilter)
                    null
                }
            }
        }

        override fun getLogTag() = LOG_TAG

        @Suppress("unused")
        fun toJSONString() = toJSONString(this)

        val uuid: String?
            get() = poi?.uuid
                ?: route?.uuid
                ?: routeDirection?.uuid
    }
}
