package org.mtransit.android.commons.provider.gbfs.data.api

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSGbfsApiModel.GBFSFeedsApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSURLApiType

// https://gbfs.org/specification/reference/#gbfsjson
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
            enum class GBFSFileTypeApiModel {
                @SerializedName("system_information")
                SYSTEM_INFORMATION,
                @SerializedName("station_information")
                STATION_INFORMATION,
                // TODO...
            }
        }
    }
}
