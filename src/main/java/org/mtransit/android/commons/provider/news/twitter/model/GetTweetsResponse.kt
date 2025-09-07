package org.mtransit.android.commons.provider.news.twitter.model

import com.google.gson.annotations.SerializedName

data class GetTweetsResponse(
    @SerializedName("data")
    val data: List<Tweet>? = null,
    @SerializedName("includes")
    val includes: TweetIncludes? = null, // If you use expansions (e.g., user objects for authors)
)

data class TweetIncludes(
    @SerializedName("users")
    val users: List<TwitterUser>? = null,
    @SerializedName("tweets")
    val tweets: List<Tweet>? = null, // For referenced tweets like quotes or replies
    @SerializedName("media")
    val media: List<TweetMedia>? = null // For media objects (photos, videos, GIFs)
)

