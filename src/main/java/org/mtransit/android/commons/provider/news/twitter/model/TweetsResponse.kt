package org.mtransit.android.commons.provider.news.twitter.model

import com.google.gson.annotations.SerializedName

data class TweetsResponse(
    @SerializedName("data")
    val data: List<Tweet>?,
    @SerializedName("includes")
    val includes: TweetIncludes?, // If you use expansions (e.g., user objects for authors)
) {

    data class TweetIncludes(
        @SerializedName("users")
        val users: List<TwitterUser>?,
        @SerializedName("tweets")
        val tweets: List<Tweet>?, // For referenced tweets like quotes or replies
        @SerializedName("media")
        val media: List<TweetMedia>?, // For media objects (photos, videos, GIFs)
    )
}
