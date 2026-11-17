package org.mtransit.android.commons.provider.news.youtube

import org.mtransit.android.commons.provider.news.youtube.model.ChannelsResponse
import org.mtransit.android.commons.provider.news.youtube.model.PlaylistItemsResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeV3Api {

    // https://developers.google.com/youtube/v3/docs/channels/list
    @GET("v3/channels")
    fun getChannels(
        @Query("part") part: String,
        @Query("forUsername") forUsername: String?, // XOR
        @Query("forHandle") forHandle: String?, // XOR
        @Query("id") id: String?, // XOR
        @Query("hl") hl: String?,
        @Query("key") key: String,
    ): Call<ChannelsResponse>

    // https://developers.google.com/youtube/v3/docs/playlistItems/list
    @GET("v3/playlistItems")
    fun getPlaylistItems(
        @Query("part") part: String,
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Long,
        @Query("hl") hl: String?,
        @Query("key") key: String,
    ): Call<PlaylistItemsResponse>
}
