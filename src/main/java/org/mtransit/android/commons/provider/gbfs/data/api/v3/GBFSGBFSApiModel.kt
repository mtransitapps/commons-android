package org.mtransit.android.commons.provider.gbfs.data.api.v3

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.v3.GBFSGBFSApiModel.GBFSFeedsApiModel
import java.util.Date

// https://gbfs.org/specification/reference/#gbfsjson
data class GBFSGBFSApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: Date,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSFeedsApiModel,
) : GBFSCommonApiModel<GBFSFeedsApiModel>() {

    data class GBFSFeedsApiModel(
        @SerializedName("feeds")
        val feeds: List<FeedAPiModel>
    ) {
        data class FeedAPiModel(
            @SerializedName("name")
            val name: GBFSFileTypeApiModel,
            @SerializedName("url")
            val url: String,
        )
    }
}
