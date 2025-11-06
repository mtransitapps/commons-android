package org.mtransit.android.commons.provider.news.youtube.model

import com.google.gson.annotations.SerializedName

data class ChannelsResponse(
    @SerializedName("items")
    val items: List<Channel>?,
) {

    data class Channel(
        @SerializedName("id")
        val id: String?,
        @SerializedName("snippet")
        val snippet: Snippet?,
        @SerializedName("contentDetails")
        val contentDetails: ContentDetails?,
    ) {

        data class Snippet(
            @SerializedName("title")
            val title: String?,
            @SerializedName("customUrl")
            val customUrl: String?,
            @SerializedName("thumbnails")
            val thumbnailDetails: ThumbnailDetails?,
            @SerializedName("localized")
            val localized: Localized?,
        ) {

            data class Localized(
                @SerializedName("title")
                val title: String?,
            )
        }

        data class ContentDetails(
            @SerializedName("relatedPlaylists")
            val relatedPlaylists: RelatedPlaylists?,
        ) {

            data class RelatedPlaylists(
                @SerializedName("uploads")
                val uploads: String?,
            )
        }
    }
}
