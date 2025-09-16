package org.mtransit.android.commons.provider.news.twitter

import org.mtransit.android.commons.provider.news.twitter.model.TweetsResponse
import org.mtransit.android.commons.provider.news.twitter.model.TwitterUserResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Date

// https://docs.x.com/x-api/
interface TwitterV2Api {

    // https://docs.x.com/x-api/users/get-user-by-username
    @GET("2/users/by/username/{username}")
    fun getUserByUsername(
        @Header("Authorization") authorization: String,
        @Path("username") username: String,
        @Query("user.fields") userFields: String? = null,
        @Query("expansions") expansions: String? = null,
        @Query("tweet.fields") tweetFields: String? = null
    ): Call<TwitterUserResponse>

     // https://docs.x.com/x-api/users/get-posts
    @GET("2/users/{id}/tweets")
    fun getUsersIdTweets(
        @Header("Authorization") authorization: String,
        @Path("id") userId: String,
        @Query("max_results") maxResults: Int? = null,
        @Query("since_id") sinceId: String? = null,
        @Query("start_time") startTime: Date? = null, // TODO TwitterDateAdapter > add JsonSerializer
        @Query("exclude") exclude: String? = null,
        @Query("tweet.fields") tweetFields: String? = null,
        @Query("expansions") expansions: String? = null,
        @Query("media.fields") mediaFields: String? = null,
        @Query("user.fields") userFields: String? = null
    ): Call<TweetsResponse>
}
