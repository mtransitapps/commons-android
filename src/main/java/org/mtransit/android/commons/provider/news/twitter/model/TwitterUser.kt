package org.mtransit.android.commons.provider.news.twitter.model

import com.google.gson.annotations.SerializedName

data class TwitterUser(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("username")
    val username: String?,
    @SerializedName("profile_image_url")
    val profileImageUrl: String?,
)
