package org.mtransit.android.commons.provider.gbfs.data.api.v3

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.v3.GBFSGbfsVersionsApiModel.GBFSVersionsApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSVersionApiModel

// https://gbfs.org/specification/reference/#gbfs_versionsjson
// https://github.com/MobilityData/gbfs/blob/v3.0/gbfs.md#gbfs_versionsjson
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
