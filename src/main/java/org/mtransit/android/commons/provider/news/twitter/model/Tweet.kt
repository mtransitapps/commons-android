package org.mtransit.android.commons.provider.news.twitter.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Tweet(
    @SerializedName("id")
    val id: String,
    @SerializedName("text")
    val text: String,
    @SerializedName("created_at")
    val createdAt: Date? = null,
    @SerializedName("author_id")
    val authorId: String? = null,
    @SerializedName("in_reply_to_user_id")
    val inReplyToUserId: String? = null,
    @SerializedName("attachments")
    val attachments: TweetAttachments? = null,
    @SerializedName("entities")
    val entities: TweetEntities? = null,
    @SerializedName("lang")
    val lang: String? = null,
) {

    data class TweetAttachments(
        @SerializedName("media_keys")
        val mediaKeys: List<String>? = null,
    )

    data class TweetEntities(
        @SerializedName("hashtags")
        val hashtags: List<TweetEntityTag>? = null,
        @SerializedName("mentions")
        val mentions: List<TweetEntityMention>? = null,
        @SerializedName("urls")
        val urls: List<TweetEntityURL>? = null
    ){

        data class TweetEntityTag(
            @SerializedName("tag")
            val tag: String // For hashtags, this is the hashtag text; for cashtags, the cashtag text
        )

        data class TweetEntityMention(
            @SerializedName("username")
            val username: String,
        )

        data class TweetEntityURL(
            @SerializedName("url")
            val url: String?,
            @SerializedName("expanded_url")
            val expandedUrl: String?,
            @SerializedName("display_url")
            val displayUrl: String?,
        )
    }
}
