package org.mtransit.android.commons.provider.news.twitter.model

import com.google.gson.annotations.SerializedName

data class TweetMedia(
    @SerializedName("media_key")
    val mediaKey: String?,
    @SerializedName("type")
    val type: String?, // e.g., "animated_gif", "photo", "video"
    @SerializedName("url")
    val url: String?, // For type=photo
    @SerializedName("preview_image_url")
    val previewImageUrl: String?, // For type=video/animated_gif
    @SerializedName("variants") // For videos, provides different encodings/bitrates
    val variants: List<TweetMediaVariant>?,
) {

    data class TweetMediaVariant(
        @SerializedName("bit_rate")
        val bitRate: Int?,
        @SerializedName("content_type")
        val contentType: String?, // e.g., "video/mp4"
        @SerializedName("url")
        val url: String?,
    )
}

enum class TweetMediaType(val type: String) {
    PHOTO("photo"),
    VIDEO("video"),
    ANIMATED_GIF("animated_gif"),
}
