package org.mtransit.android.commons.provider.vehiclelocations

import android.net.Uri
import android.provider.BaseColumns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.mtransit.android.commons.MTLog.Loggable
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.provider.common.ProviderContract
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation

interface VehicleLocationProviderContract : ProviderContract {

    companion object {
        const val VEHICLE_LOCATION_PATH = "vehicle"

        const val PING_PATH = ProviderContract.PING_PATH
    }

    val authorityUri: Uri

    val vehicleLocationMaxValidityInMs: Long

    fun getVehicleLocationValidityInMs(inFocus: Boolean): Long

    fun getMinDurationBetweenVehicleLocationRefreshInMs(inFocus: Boolean): Long

    fun cacheVehicleLocations(newVehicleLocations: List<VehicleLocation>)

    fun getCachedVehicleLocations(vehicleLocationFilter: Filter): List<VehicleLocation>

    fun getNewVehicleLocations(vehicleLocationFilter: Filter): List<VehicleLocation>

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


    @Serializable
    data class Filter(
        @SerialName("r_id")
        val routeIdHash: Int?, // original // GTFS // cleaned + hash
        @SerialName("r_sn")
        val routeShortName: String, // used for NextBus
        @SerialName("d_id")
        val directionId: Int?, // original // GTFS
        @SerialName("d_hv")
        val directionHeadsignValue: String, // used for NextBus
        @SerialName("t_ids")
        val tripIds: List<String>?, // original // GTFS // cleaned
        @SerialName("s_id")
        val stopIdHash: Int?, // original // GTFS // cleaned + hash
    ) : Loggable {

        companion object {
            private val LOG_TAG: String = VehicleLocationProviderContract::class.java.simpleName + ">" + Filter::class.java.simpleName

            fun from(rd: RouteDirection, stopIdHash: Int? = null, tripIds: List<String>? = null) = Filter(
                routeIdHash = rd.route.originalIdHash,
                routeShortName = rd.route.shortName,
                directionId = rd.direction.originalDirectionIdOrNull,
                directionHeadsignValue = rd.direction.headsignValue,
                tripIds = tripIds,
                stopIdHash = stopIdHash,
            )

            fun from(rds: RouteDirectionStop, tripIds: List<String>? = null) = Filter(
                routeIdHash = rds.route.originalIdHash,
                routeShortName = rds.route.shortName,
                directionId = rds.direction.originalDirectionIdOrNull,
                directionHeadsignValue = rds.direction.headsignValue,
                tripIds = tripIds,
                stopIdHash = rds.stop.originalIdHash,
            )

            fun fromJSON(jsonString: String): Filter = Json.decodeFromString(serializer(), jsonString)

        }

        override fun getLogTag() = LOG_TAG

        fun toJSON() = Json.encodeToString(serializer(), this)

    }

}
