package org.mtransit.android.commons.provider.gbfs.data.api.v3.common

import com.google.gson.annotations.SerializedName

data class GBFSVersionApiModel(
    @SerializedName("version")
    val version: String,
    @SerializedName("url")
    val url: String,
)
