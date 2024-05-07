package org.mtransit.android.commons.provider.gbfs.data.api.common

import com.google.gson.annotations.SerializedName

data class GBFSLocalizedURLApiModel(
    @SerializedName("text")
    val text: GBFSURLApiType,
    @SerializedName("language")
    val language: GBFSLanguageApiType,
)