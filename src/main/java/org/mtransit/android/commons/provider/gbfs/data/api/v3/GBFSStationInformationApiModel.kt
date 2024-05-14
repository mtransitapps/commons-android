package org.mtransit.android.commons.provider.gbfs.data.api.v3

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.v3.GBFSStationInformationApiModel.GBFSStationInformationDataApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSGeoJSONMultiPolygonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSIDApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSLatitudeApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSLocalizedStringApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSLongitudeApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSPhoneNumberApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSRentalUrisApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSVehicleTypesCountApiModel

// https://gbfs.org/specification/reference/#station_informationjson
data class GBFSStationInformationApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSStationInformationDataApiModel,
) : GBFSCommonApiModel<GBFSStationInformationDataApiModel>() {
    data class GBFSStationInformationDataApiModel(
        @SerializedName("stations")
        val stations: List<GBFSStationApiModel>?,
    ) {
        data class GBFSStationApiModel(
            @SerializedName("station_id")
            val stationId: GBFSIDApiType,
            @SerializedName("name")
            val name: List<GBFSLocalizedStringApiModel>,
            @SerializedName("short_name")
            val shortName: List<GBFSLocalizedStringApiModel>?,
            @SerializedName("lat")
            val lat: GBFSLatitudeApiType,
            @SerializedName("lon")
            val lon: GBFSLongitudeApiType,
            @SerializedName("address")
            val address: String?,
            @SerializedName("cross_street")
            val crossStreet: String?,
            @SerializedName("region_id")
            val regionId: GBFSIDApiType?,
            @SerializedName("post_code")
            val postCode: String?,
            @SerializedName("station_opening_hours")
            val stationOpeningHours: String?,
            @SerializedName("rental_methods")
            val rentalMethods: List<GBFSRentalMethodApiModel>?,
            @SerializedName("is_virtual_station")
            val isVirtualStation: Boolean?,
            @SerializedName("station_area")
            val stationArea: GBFSGeoJSONMultiPolygonApiModel?,
            @SerializedName("parking_type")
            val parkingType: GBFSParkingTypeApiModel?,
            @SerializedName("parking_hoop")
            val parkingHoop: Boolean?,
            @SerializedName("contact_phone")
            val contactPhone: GBFSPhoneNumberApiType,
            @SerializedName("capacity")
            val capacity: Int?,
            @SerializedName("vehicle_types_capacity")
            val vehicleTypesCapacity: List<GBFSVehicleTypesCountApiModel>,
            @SerializedName("vehicle_docks_capacity")
            val vehicleDocksCapacity: List<GBFSVehicleTypesCountApiModel>,
            @SerializedName("is_valet_station")
            val isValetStation: Boolean?,
            @SerializedName("is_charging_station")
            val isChargingStation: Boolean?,
            @SerializedName("rental_uris")
            val rentalUris: GBFSRentalUrisApiModel?,
        ) {
            @Suppress("unused")
            enum class GBFSRentalMethodApiModel {
                @SerializedName("key")
                KEY,

                @SerializedName("creditcard")
                CREDIT_CARD,

                @SerializedName("paypass")
                PAY_PASS,

                @SerializedName("applepay")
                APPLE_PAY,

                @SerializedName("androidpay")
                ANDROID_PAY,

                @SerializedName("transitcard")
                TRANSIT_CARD,

                @SerializedName("accountnumber")
                ACCOUNT_NUMBER,

                @SerializedName("phone")
                PHONE,

            }

            @Suppress("unused")
            enum class GBFSParkingTypeApiModel {
                @SerializedName("parking_lot")
                PARKING_LOT,

                @SerializedName("street_parking")
                STREET_PARKING,

                @SerializedName("underground_parking")
                UNDERGROUND_PARKING,

                @SerializedName("sidewalk_parking")
                SIDEWALK_PARKING,

                @SerializedName("unknown")
                UNKNOWN,
            }
        }
    }
}