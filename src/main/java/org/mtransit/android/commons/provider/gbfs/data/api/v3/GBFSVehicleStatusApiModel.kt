package org.mtransit.android.commons.provider.gbfs.data.api.v3

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.v3.GBFSVehicleStatusApiModel.GBFSVehicleStatusDataApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSDateTimeApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSIDApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSLatitudeApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSLongitudeApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSRentalUrisApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSTimestampApiType

// https://gbfs.org/specification/reference/#vehicle_statusjson
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v3.0/vehicle_status.json
data class GBFSVehicleStatusApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSVehicleStatusDataApiModel,
) : GBFSCommonApiModel<GBFSVehicleStatusDataApiModel>() {

    data class GBFSVehicleStatusDataApiModel(
        @SerializedName("vehicles")
        val vehicles: List<GBFSVehicleStatusApiModel>,
    ) {
        data class GBFSVehicleStatusApiModel(
            @SerializedName("vehicle_id")
            val vehicleId: GBFSIDApiType,
            @SerializedName("lat")
            val lat: GBFSLatitudeApiType,
            @SerializedName("lon")
            val lon: GBFSLongitudeApiType,
            @SerializedName("is_reserved")
            val isReserved: Boolean?,
            @SerializedName("is_disabled")
            val isDisabled: Boolean?,
            @SerializedName("rental_uris")
            val rentalUris: GBFSRentalUrisApiModel?,
            @SerializedName("vehicle_type_id")
            val vehicleTypeId: GBFSIDApiType?,
            @SerializedName("last_reported")
            val lastReported: GBFSTimestampApiType,
            @SerializedName("current_range_meters")
            val currentRangeMeters: Float?,
            @SerializedName("current_fuel_percent")
            val currentFuelPercent: Float?,
            @SerializedName("station_id")
            val stationId: GBFSIDApiType?,
            @SerializedName("home_station_id")
            val homeStationId: GBFSIDApiType?,
            @SerializedName("pricing_plan_id")
            val pricingPlanId: GBFSIDApiType?,
            @SerializedName("vehicle_equipment")
            val vehicleEquipment: List<GBFSVehicleEquipmentApiModel>?,
            @SerializedName("available_until")
            val availableUntil: GBFSDateTimeApiType?,
        ) {
            @Suppress("unused")
            enum class GBFSVehicleEquipmentApiModel {
                @SerializedName("child_seat_a")
                CHILD_SEAT_A,

                @SerializedName("child_seat_b")
                CHILD_SEAT_B,

                @SerializedName("child_seat_c")
                CHILD_SEAT_C,

                @SerializedName("winter_tires")
                WINTER_TIRES,

                @SerializedName("snow_chains")
                SNOW_CHAINS,
            }
        }
    }
}

// added in v2.1
@Deprecated("Removed in v3.0")
data class GBFSFreeBikeStatusApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSVehicleStatusDataApiModel,
) : GBFSCommonApiModel<GBFSVehicleStatusDataApiModel>()