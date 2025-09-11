package org.mtransit.android.commons.provider.news.twitter

import org.mtransit.android.commons.provider.news.twitter.model.GetTweetsResponse
import org.mtransit.android.commons.provider.news.twitter.model.GetTwitterUserByUsernameResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Date

interface TwitterApi {

    /**
     * Docs: https://developer.x.com/en/docs/x-api/users/lookup/api-reference/get-users-by-username-username
     */
    @GET("2/users/by/username/{username}")
    fun getUserByUsername(
        @Header("Authorization") authorization: String,
        @Path("username") username: String,
        @Query("user.fields") userFields: String? = null,
        @Query("expansions") expansions: String? = null,
        @Query("tweet.fields") tweetFields: String? = null
    ): Call<GetTwitterUserByUsernameResponse>

    /**
     * Docs: https://developer.x.com/en/docs/x-api/tweets/timelines/api-reference/get-users-id-tweets
     */
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
    ): Call<GetTweetsResponse>
}
