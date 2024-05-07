package org.mtransit.android.commons.provider.gbfs.data.api

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSVehicleTypesApiModel.*
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSCountryCodeApiType
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSIDApiType
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSLocalizedStringApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSURLApiType
import java.util.Date

// https://gbfs.org/specification/reference/#vehicle_typesjson
data class GBFSVehicleTypesApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: Date,
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
            @SerializedName("rider_capacity")
            val riderCapacity: Int?,
            @SerializedName("cargo_volume_capacity")
            val cargoVolumeCapacity: Int?,
            @SerializedName("cargo_load_capacity")
            val cargoLoadCapacity: Int?,
            @SerializedName("propulsion_type")
            val propulsionType: GBFSPropulsionTypeApiModel?,
            @SerializedName("eco_labels") // added in v3.0
            val ecoLabels: List<GBFSEcoLabelApiModel>?,
            @Deprecated("Removed in v3.0")
            @SerializedName("eco_label") // added in v2.3
            val ecoLabel: List<GBFSEcoLabelApiModel>?,
            @SerializedName("max_range_meters")
            val maxRangeMeters: Float?,
            @SerializedName("name")
            val name: List<GBFSLocalizedStringApiModel>?,
            @SerializedName("vehicle_accessories")
            val vehicleAccessories: List<GBFSVehicleAccessoriesApiModel>?,
            @SerializedName("g_CO2_km")
            val gCO2Km: Int?,
            @SerializedName("vehicle_image")
            val vehicleImage: GBFSURLApiType?,
            @SerializedName("make")
            val make: List<GBFSLocalizedStringApiModel>?,
            @SerializedName("model")
            val model: List<GBFSLocalizedStringApiModel>?,
            @SerializedName("color")
            val color: String,
            @SerializedName("description")
            val description: List<GBFSLocalizedStringApiModel>?,
            @SerializedName("wheel_count")
            val wheelCount: Int?,
            @SerializedName("max_permitted_speed")
            val maxPermittedSpeed: Int?,
            @SerializedName("rated_power")
            val ratedPower: Int?,
            @SerializedName("default_reserve_time") // in Minutes
            val defaultReserveTimeMin: Int?,
            @SerializedName("return_constraint")
            val returnConstraint: GBFSReturnConstraintApiModel?,
            @SerializedName("vehicle_assets")
            val vehicleAssets: GBFSVehicleAssetsApiModel?,
            @SerializedName("default_pricing_plan_id")
            val defaultPricingPlanId: GBFSIDApiType?,
            @SerializedName("pricing_plan_ids")
            val pricingPlanIds: List<GBFSIDApiType>?,
        ) {

            data class GBFSEcoLabelApiModel(
                @SerializedName("country_code")
                val countryCode: GBFSCountryCodeApiType,
                @SerializedName("eco_sticker")
                val ecoSticker: String,
            )

            data class GBFSVehicleAssetsApiModel(
                @SerializedName("icon_url")
                val iconUrl: GBFSURLApiType?,
                @SerializedName("icon_url_dark")
                val iconUrlDark: GBFSURLApiType?,
                @SerializedName("icon_last_modified")
                val iconLastModified: Date?,
            )

            @Suppress("unused")
            enum class GBFSFormFactorApiModel {
                @SerializedName("bicycle")
                BICYCLE,

                @SerializedName("cargo_bicycle")
                CARGO_BICYCLE,

                @SerializedName("car")
                CAR,

                @SerializedName("moped")
                MOPED,

                @SerializedName("scooter_standing")
                SCOOTER_STANDING,

                @SerializedName("scooter_seated")
                SCOOTER_SEATED,

                @SerializedName("other")
                OTHER,
            }

            @Suppress("unused")
            enum class GBFSReturnConstraintApiModel {
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

                @SerializedName("combustion_diesel")
                COMBUSTION_DIESEL,

                @SerializedName("hybrid")
                HYBRID,

                @SerializedName("plug_in_hybrid")
                PLUG_IN_HYBRID,

                @SerializedName("hydrogen_fuel_cell")
                HYDROGEN_FUEL_CELL,
            }
        }
    }
}