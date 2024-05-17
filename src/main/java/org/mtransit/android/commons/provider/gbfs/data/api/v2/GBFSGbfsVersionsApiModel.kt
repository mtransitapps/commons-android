package org.mtransit.android.commons.provider.gbfs.data.api.v2

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSGbfsVersionsApiModel.GBFSVersionsApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSVersionApiModel

// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.0/gbfs_versions.json
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.1/gbfs_versions.json
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.2/gbfs_versions.json
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.3/gbfs_versions.json
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
