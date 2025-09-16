package org.mtransit.android.commons.provider.news.twitter.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Tweet(
    @SerializedName("id")
    val id: String?,
    @SerializedName("text")
    val text: String?,
    @SerializedName("created_at")
    val createdAt: Date?,
    @SerializedName("author_id")
    val authorId: String?,
    @SerializedName("in_reply_to_user_id")
    val inReplyToUserId: String?,
    @SerializedName("attachments")
    val attachments: TweetAttachments?,
    @SerializedName("entities")
    val entities: TweetEntities?,
    @SerializedName("lang")
    val lang: String?,
) {

    data class TweetAttachments(
        @SerializedName("media_keys")
        val mediaKeys: List<String>?,
    )

    data class TweetEntities(
        @SerializedName("hashtags")
        val hashtags: List<TweetEntityTag>?,
        @SerializedName("mentions")
        val mentions: List<TweetEntityMention>?,
        @SerializedName("urls")
        val urls: List<TweetEntityURL>?,
    ) {

        data class TweetEntityTag(
            @SerializedName("tag")
            val tag: String?, // For hashtags, this is the hashtag text; for cashtags, the cashtag text
        )

        data class TweetEntityMention(
            @SerializedName("username")
            val username: String?,
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
