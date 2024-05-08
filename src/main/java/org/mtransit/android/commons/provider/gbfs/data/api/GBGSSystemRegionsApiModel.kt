package org.mtransit.android.commons.provider.gbfs.data.api

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.GBGSSystemRegionsApiModel.GBGSSystemRegionsDataApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSIDApiType
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSLocalizedStringApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSTimestampApiType

// https://gbfs.org/specification/reference/#system_regionsjson
class GBGSSystemRegionsApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBGSSystemRegionsDataApiModel,
) : GBFSCommonApiModel<GBGSSystemRegionsDataApiModel>() {
    data class GBGSSystemRegionsDataApiModel(
        @SerializedName("regions")
        val regions: List<GBGSRegionApiModel>,
    ) {
        data class GBGSRegionApiModel(
            @SerializedName("region_id")
            val regionId: GBFSIDApiType,
            @SerializedName("name") // added in v3.0
            val name: List<GBFSLocalizedStringApiModel>?,
        )
    }
}