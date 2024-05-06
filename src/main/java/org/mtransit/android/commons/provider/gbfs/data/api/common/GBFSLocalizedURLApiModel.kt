package org.mtransit.android.commons.provider.gbfs.data.api.common

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSLanguageApiType
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSURLApiType

data class GBFSLocalizedURLApiModel(
    @SerializedName("text")
    val text: GBFSURLApiType,
    @SerializedName("language")
    val language: GBFSLanguageApiType,
)