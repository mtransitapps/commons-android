package org.mtransit.android.commons.provider.gbfs.data.api.v2

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSVehicleTypesApiModel.GBFSVehicleTypesDataApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSCountryCodeApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSDateApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSIDApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSURLApiType

// https://gbfs.org/specification/reference/#vehicle_typesjson
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.1/vehicle_types.json
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.2/vehicle_types.json
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.3/vehicle_types.json
data class GBFSVehicleTypesApiModel( // (added in v2.1)
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSVehicleTypesDataApiModel,
) : GBFSCommonApiModel<GBFSVehicleTypesDataApiModel>() {

    data class GBFSVehicleTypesDataApiModel(
        @SerializedName("vehicle_types")
        val vehicleTypes: List<GBFSVehicleTypeApiModel>,
    ) {
        data class GBFSVehicleTypeApiModel(
            @SerializedName("vehicle_type_id")
            val vehicleTypeId: GBFSIDApiType,
            @SerializedName("form_factor")
            val formFactor: GBFSFormFactorApiModel,
            @SerializedName("rider_capacity") // (added in v2.3)
            val riderCapacity: Int?,
            @SerializedName("cargo_volume_capacity") // (added in v2.3)
            val cargoVolumeCapacity: Int?,
            @SerializedName("cargo_load_capacity") // (added in v2.3)
            val cargoLoadCapacity: Int?,
            @SerializedName("propulsion_type")
            val propulsionType: GBFSPropulsionTypeApiModel?,
            @SerializedName("eco_label") // added in v2.3
            val ecoLabel: List<GBFSEcoLabelApiModel>?,
            @SerializedName("max_range_meters")
            val maxRangeMeters: Float?,
            @SerializedName("name")
            val name: String?,
            @SerializedName("vehicle_accessories") // (added in v2.3)
            val vehicleAccessories: List<GBFSVehicleAccessoriesApiModel>?,
            @SerializedName("g_CO2_km") // (added in v2.3)
            val gCO2Km: Int?,
            @SerializedName("vehicle_image") // (added in v2.3)
            val vehicleImage: GBFSURLApiType?,
            @SerializedName("make") // (added in v2.3)
            val make: String?,
            @SerializedName("model") // (added in v2.3)
            val model: String?,
            @SerializedName("color") // (added in v2.3)
            val color: String,
            @SerializedName("wheel_count") // (added in v2.3)
            val wheelCount: Int?,
            @SerializedName("max_permitted_speed") // (added in v2.3)
            val maxPermittedSpeed: Int?,
            @SerializedName("rated_power") // (added in v2.3)
            val ratedPower: Int?,
            @SerializedName("default_reserve_time") // in Minutes // (added in v2.3)
            val defaultReserveTimeMin: Int?,
            @SerializedName("return_constraint") // (added in v2.3)
            val returnConstraint: GBFSReturnConstraintApiModel?,
            @SerializedName("vehicle_assets") // (added in v2.3)
            val vehicleAssets: GBFSVehicleAssetsApiModel?,
            @SerializedName("default_pricing_plan_id") // (added in v2.3)
            val defaultPricingPlanId: GBFSIDApiType?,
            @SerializedName("pricing_plan_ids") // (added in v2.3)
            val pricingPlanIds: List<GBFSIDApiType>?,
        ) {

            data class GBFSEcoLabelApiModel(
                @SerializedName("country_code") // (added in v2.3)
                val countryCode: GBFSCountryCodeApiType,
                @SerializedName("eco_sticker") // (added in v2.3)
                val ecoSticker: String,
            )

            data class GBFSVehicleAssetsApiModel(
                @SerializedName("icon_url")
                val iconUrl: GBFSURLApiType?,
                @SerializedName("icon_url_dark")
                val iconUrlDark: GBFSURLApiType?,
                @SerializedName("icon_last_modified")
                val iconLastModified: GBFSDateApiType?,
            )

            @Suppress("unused")
            enum class GBFSFormFactorApiModel {
                @SerializedName("bicycle")
                BICYCLE,

                @SerializedName("cargo_bicycle") // (added in v2.3)
                CARGO_BICYCLE,

                @SerializedName("car")
                CAR,

                @SerializedName("moped")
                MOPED,

                @SerializedName("scooter")
                SCOOTER,

                @SerializedName("scooter_standing") // (added in v2.3)
                SCOOTER_STANDING,

                @SerializedName("scooter_seated") // (added in v2.3)
                SCOOTER_SEATED,

                @SerializedName("other")
                OTHER,
            }

            @Suppress("unused")
            enum class GBFSReturnConstraintApiModel { // (added in v2.3)
                @SerializedName("free_floating")
                FREE_FLOATING,

                @SerializedName("roundtrip_station")
                ROUNDTRIP_STATION,

                @SerializedName("any_station")
                ANY_STATION,

                @SerializedName("hybrid")
                HYBRID,
            }

            @Suppress("unused")
            enum class GBFSVehicleAccessoriesApiModel {
                @SerializedName("air_conditioning")
                AIR_CONDITIONING,

                @SerializedName("automatic")
                AUTOMATIC,

                @SerializedName("manual")
                MANUAL,

                @SerializedName("convertible")
                CONVERTIBLE,

                @SerializedName("cruise_control")
                CRUISE_CONTROL,

                @SerializedName("doors_2")
                DOORS_2,

                @SerializedName("doors_3")
                DOORS_3,

                @SerializedName("doors_4")
                DOORS_4,

                @SerializedName("doors_5")
                DOORS_5,

                @SerializedName("navigation")
                NAVIGATION,
            }

            @Suppress("unused")
            enum class GBFSPropulsionTypeApiModel {
                @SerializedName("human")
                HUMAN,

                @SerializedName("electric_assist")
                ELECTRIC_ASSIST,

                @SerializedName("electric")
                ELECTRIC,

                @SerializedName("combustion")
                COMBUSTION,

                @SerializedName("combustion_diesel") // (added in v2.3)
                COMBUSTION_DIESEL,

                @SerializedName("hybrid") // (added in v2.3)
                HYBRID,

                @SerializedName("plug_in_hybrid") // (added in v2.3)
                PLUG_IN_HYBRID,

                @SerializedName("hydrogen_fuel_cell") // (added in v2.3)
                HYDROGEN_FUEL_CELL,
            }
        }
    }
}