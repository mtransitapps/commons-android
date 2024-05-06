package org.mtransit.android.commons.provider.gbfs.data.api.common

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSLanguageApiType

data class GBFSLocalizedStringApiModel(
    @SerializedName("text")
    val text: String,
    @SerializedName("language")
    val language: GBFSLanguageApiType,
)