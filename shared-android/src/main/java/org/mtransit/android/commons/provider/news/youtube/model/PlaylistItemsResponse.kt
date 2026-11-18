package org.mtransit.android.commons.provider.news.youtube.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class PlaylistItemsResponse(
    @SerializedName("items")
    val items: List<PlaylistItem?>?,
) {

    data class PlaylistItem(
        @SerializedName("id")
        val id: String?,
        @SerializedName("status")
        val status: PlaylistItemStatus?,
        @SerializedName("snippet")
        val snippet: PlaylistItemSnippet?,
    ) {

        data class PlaylistItemStatus(
            @SerializedName("privacyStatus")
            val privacyStatus: String?,
        )

        data class PlaylistItemSnippet(
            @SerializedName("title")
            val title: String?,
            @SerializedName("resourceId")
            val resourceId: ResourceId?,
            @SerializedName("description")
            val description: String?,
            @SerializedName("publishedAt")
            val publishedAt: Date?,
            @SerializedName("thumbnails")
            val thumbnailDetails: ThumbnailDetails?,
        ) {

            data class ResourceId(
                @SerializedName("kind")
                val kind: String?,
                @SerializedName("videoId")
                val videoId: String?,
            )
        }
    }
}
