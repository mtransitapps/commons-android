package org.mtransit.android.commons.provider.gbfs.data.api.v2

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSLanguageApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSURLApiType

// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.0/gbfs.json
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.1/gbfs.json
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.2/gbfs.json
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.3/gbfs.json
data class GBFSGbfsApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: Map<GBFSLanguageApiType, GBFSFeedsAPiModel>,
) : GBFSCommonApiModel<Map<GBFSLanguageApiType, GBFSGbfsApiModel.GBFSFeedsAPiModel>>() {

    data class GBFSFeedsAPiModel(
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
                VEHICLE_TYPES, // (added in v2.1)

                @SerializedName("station_information")
                STATION_INFORMATION,

                @SerializedName("station_status")
                STATION_STATUS,

                @SerializedName("free_bike_status")
                FREE_BIKE_STATUS,

                @SerializedName("system_hours")
                SYSTEM_HOURS,

                @SerializedName("system_calendar")
                SYSTEM_CALENDAR,

                @SerializedName("system_regions")
                SYSTEM_REGIONS,

                @SerializedName("system_pricing_plans")
                SYSTEM_PRICING_PLANS,

                @SerializedName("system_alerts")
                SYSTEM_ALERTS,

                @SerializedName("geofencing_zones")
                GEOFENCING_ZONES, // (added in v2.1)
            }
        }
    }
}
