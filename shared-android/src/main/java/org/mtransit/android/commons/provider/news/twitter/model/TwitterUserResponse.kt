package org.mtransit.android.commons.provider.news.twitter.model

import com.google.gson.annotations.SerializedName

data class TwitterUserResponse(
    @SerializedName("data")
    val data: TwitterUser?,
)
