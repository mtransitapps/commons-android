package org.mtransit.android.commons.provider.gbfs.data.api.common

import com.google.gson.annotations.SerializedName

data class GBFSRentalUrisApiModel(
    @SerializedName("android")
    val android: GBFSURIApiType?,
    @SerializedName("ios")
    val ios: GBFSURIApiType?,
    @SerializedName("web")
    val web: GBFSURLApiType?,
)