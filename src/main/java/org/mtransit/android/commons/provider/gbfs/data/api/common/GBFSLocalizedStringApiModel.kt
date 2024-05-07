package org.mtransit.android.commons.provider.gbfs.data.api.common

import com.google.gson.annotations.SerializedName

data class GBFSLocalizedStringApiModel(
    @SerializedName("text")
    val text: String,
    @SerializedName("language")
    val language: GBFSLanguageApiType,
)