package org.mtransit.android.commons.provider.nextbus.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

// https://retro.umoiq.com/xmlFeedDocs/NextBusXMLFeed.pdf
interface NextBusApi {

    companion object {
        const val BASE_HOST_URL = "https://retro.umoiq.com/service/"
    }

    @GET("publicJSONFeed")
    fun getVehicleLocations(
        @Query("command") command: String = "vehicleLocations",
        @Query("a") agencyTag: String,
        @Query("r") routeTag: String? = null,
        @Query("t") lastTimestamp: Long? = 0, // 0 == all (avoid error in JSON response)
    ): Call<VehicleLocationsResponse>
}
