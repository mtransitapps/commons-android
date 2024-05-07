package org.mtransit.android.commons.provider.gbfs.data.api

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSGbfsVersionsApiModel.GBFSVersionsApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSVersionApiModel

// https://gbfs.org/specification/reference/#gbfs_versionsjson
data class GBFSGbfsVersionsApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSVersionsApiModel,
) : GBFSCommonApiModel<GBFSVersionsApiModel>() {

    data class GBFSVersionsApiModel(
        @SerializedName("versions")
        val versions: List<GBFSVersionApiModel>
    )
}
