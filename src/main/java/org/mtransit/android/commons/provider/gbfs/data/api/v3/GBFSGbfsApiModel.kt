package org.mtransit.android.commons.provider.gbfs.data.api.v3

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.v3.GBFSGbfsApiModel.GBFSFeedsApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSURLApiType

// https://gbfs.org/specification/reference/#gbfsjson
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v3.0/gbfs.json
data class GBFSGbfsApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSFeedsApiModel,
) : GBFSCommonApiModel<GBFSFeedsApiModel>() {

    data class GBFSFeedsApiModel(
        @SerializedName("feeds")
        val feeds: List<FeedAPiModel>,
    ) {
        data class FeedAPiModel(
            @SerializedName("name")
            val name: GBFSFileTypeApiModel,
            @SerializedName("url")
            val url: GBFSURLApiType,
        ) {
            @Suppress("unused")
            enum class GBFSFileTypeApiModel {
                @SerializedName("gbfs")
                GBFS,

                @SerializedName("gbfs_versions")
                GBFS_VERSIONS,

                @SerializedName("system_information")
                SYSTEM_INFORMATION,

                @SerializedName("vehicle_types")
                VEHICLE_TYPES,

                @SerializedName("station_information")
                STATION_INFORMATION,

                @SerializedName("station_status")
                STATION_STATUS,

                @Deprecated("Removed in v3.0")
                @SerializedName("free_bike_status")
                FREE_BIKE_STATUS,

                @SerializedName("vehicle_status")
                VEHICLE_STATUS,

                @Deprecated("Removed in v3.0")
                @SerializedName("system_hours")
                SYSTEM_HOURS,

                @SerializedName("system_alerts")
                SYSTEM_ALERTS,

                @Deprecated("Removed in v3.0")
                @SerializedName("system_calendar")
                SYSTEM_CALENDAR,

                @SerializedName("system_regions")
                SYSTEM_REGIONS,

                @SerializedName("system_pricing_plans")
                SYSTEM_PRICING_PLANS,

                @SerializedName("geofencing_zones")
                GEOFENCING_ZONES,
            }
        }
    }
}
