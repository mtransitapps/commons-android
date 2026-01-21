package org.mtransit.android.commons.provider.nextbus.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface NextBusApi {

    companion object {
        const val BASE_HOST_URL = "https://retro.umoiq.com/service/"
    }

    // https://developers.google.com/youtube/v3/docs/channels/list
    @GET("publicJSONFeed")
    fun getVehicleLocations(
        @Query("command") command: String = "vehicleLocations",
        @Query("a") agencyTag: String,
        @Query("r") routeTag: String? = null,
        @Query("t") lastTimestamp: Long? = 0, // 0 == all (avoid error in JSON response)
    ): Call<VehicleLocationsResponse>
}
