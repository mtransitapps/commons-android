package org.mtransit.android.commons.provider.gbfs.data.api.v3.common

import com.google.gson.annotations.SerializedName
import java.util.Date

data class GBFSSystemBrandAssetsApiModel(
    @SerializedName("brand_last_modified")
    val brandLastModified: Date?,
    @SerializedName("brand_terms_url")
    val brandTermsUrl: GBFSURLApiType?,
    @SerializedName("brand_image_url")
    val brandImageUrl: GBFSURLApiType?,
    @SerializedName("brand_image_url_dark")
    val brandImageUrlDark: GBFSURLApiType?,
    @SerializedName("color") // (added in v2.3)
    val color: String?,
)
