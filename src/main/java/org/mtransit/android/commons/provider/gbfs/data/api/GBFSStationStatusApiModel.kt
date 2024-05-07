package org.mtransit.android.commons.provider.gbfs.data.api

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSStationStatusApiModel.GBFSStationStatusDataApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSIDApiType
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSVehicleTypeCountApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSVehicleTypesCountApiModel

// https://gbfs.org/specification/reference/#station_statusjson
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v3.0/station_status.json
// https://developers.google.com/micromobility/reference/gbfs-definitions#station_status
data class GBFSStationStatusApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSStationStatusDataApiModel,
) : GBFSCommonApiModel<GBFSStationStatusDataApiModel>() {
    data class GBFSStationStatusDataApiModel(
        @SerializedName("stations")
        val stations: List<GBFSStationStatusApiModel>?,
    ) {
        data class GBFSStationStatusApiModel(
            @SerializedName("station_id")
            val stationId: GBFSIDApiType,
            // VEHICLE
            @SerializedName("num_vehicles_available")
            val numVehiclesAvailable: Int?,
            @SerializedName("vehicle_types_available")
            val vehicleTypesAvailable: List<GBFSVehicleTypeCountApiModel>?,
            @SerializedName("num_vehicles_disabled")
            val numVehiclesDisabled: Int?,
            @Deprecated("Removed in v3.0")
            @SerializedName("num_bikes_available")
            val numBikesAvailable: Int?,
            @Deprecated("Removed in v3.0")
            @SerializedName("num_bikes_disabled")
            val numBikesDisabled: Int?,
            // DOCKS
            @SerializedName("num_docks_available")
            val numDocksAvailable: Int?,
            @SerializedName("vehicle_docks_available")
            val vehicleDocksAvailable: List<GBFSVehicleTypesCountApiModel>?,
            @SerializedName("num_docks_disabled")
            val numDocksDisabled: Int?,
            // STATUS
            @SerializedName("is_installed")
            val isInstalled: Boolean?,
            @SerializedName("is_renting")
            val isRenting: Boolean?,
            @SerializedName("is_returning")
            val isReturning: Boolean?,
            @SerializedName("last_reported")
            val lastReported: GBFSTimestampApiType?,
        )
    }
}
