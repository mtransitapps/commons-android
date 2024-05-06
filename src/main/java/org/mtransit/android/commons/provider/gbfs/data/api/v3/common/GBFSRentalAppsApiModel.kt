package org.mtransit.android.commons.provider.gbfs.data.api.v3.common

import com.google.gson.annotations.SerializedName

data class GBFSRentalAppsApiModel(
    @SerializedName("android")
    val android: GBFSRentalAppApiModel?,
    @SerializedName("ios")
    val ios: GBFSRentalAppApiModel?,
) {
    data class GBFSRentalAppApiModel(
        @SerializedName("store_uri")
        val storeUri: GBFSURLApiType?,
        @SerializedName("discovery_uri")
        val discoveryUri: GBFSURLApiType?,
    )
}
