package org.mtransit.android.commons.provider.gbfs.data.api

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSManifestApiModel.GBFSDatasetsApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSIDApiType
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSVersionApiModel

// (added in v3.0)
// https://gbfs.org/specification/reference/#manifestjson
data class GBFSManifestApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSDatasetsApiModel,
) : GBFSCommonApiModel<GBFSDatasetsApiModel>() {
    data class GBFSDatasetsApiModel(
        @SerializedName("datasets")
        val datasets: List<GBFSDatasetApiModel>,
    ) {
        data class GBFSDatasetApiModel(
            @SerializedName("system_id")
            val systemId: GBFSIDApiType,
            @SerializedName("versions")
            val versions: List<GBFSVersionApiModel>,
        )
    }
}
