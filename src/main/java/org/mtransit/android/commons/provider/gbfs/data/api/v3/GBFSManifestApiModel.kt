package org.mtransit.android.commons.provider.gbfs.data.api.v3

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.v3.GBFSManifestApiModel.GBFSDatasetsApiModel
import java.util.Date

// (added in v3.0)
// https://gbfs.org/specification/reference/#manifestjson
data class GBFSManifestApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: Date,
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
            val systemId: String, // ID
            @SerializedName("versions")
            val versions: List<GBFSManifestVersionApiModel>,
        ) {
            data class GBFSManifestVersionApiModel(
                @SerializedName("version")
                val version: String,
                @SerializedName("url")
                val url: String,
            )
        }
    }
}
