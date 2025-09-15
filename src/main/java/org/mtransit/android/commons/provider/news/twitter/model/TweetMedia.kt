package org.mtransit.android.commons.provider.news.twitter.model

import com.google.gson.annotations.SerializedName

data class TweetMedia(
    @SerializedName("media_key")
    val mediaKey: String,
    @SerializedName("type")
    val type: String, // e.g., "animated_gif", "photo", "video"
    @SerializedName("url")
    val url: String? = null, // For type=photo
    @SerializedName("preview_image_url")
    val previewImageUrl: String? = null, // For type=video/animated_gif
    @SerializedName("variants") // For videos, provides different encodings/bitrates
    val variants: List<TweetMediaVariant>? = null
){

    data class TweetMediaVariant(
        @SerializedName("bit_rate")
        val bitRate: Int? = null,
        @SerializedName("content_type")
        val contentType: String? = null, // e.g., "video/mp4"
        @SerializedName("url")
        val url: String? = null
    )
}

enum class TweetMediaType(val type: String) {
    PHOTO("photo"),
    VIDEO("video"),
    ANIMATED_GIF("animated_gif"),
}
