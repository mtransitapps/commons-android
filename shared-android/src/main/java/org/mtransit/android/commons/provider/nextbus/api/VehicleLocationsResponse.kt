package org.mtransit.android.commons.provider.nextbus.api

import com.google.gson.annotations.SerializedName

data class VehicleLocationsResponse(
    @SerializedName("vehicle")
    val vehicle: List<Vehicle>?,
) {
    data class Vehicle(
        @SerializedName("id")
        val id: String?,
        @SerializedName("routeTag")
        val routeTag: String?,
        @SerializedName("dirTag")
        val dirTag: String?,
        @SerializedName("lat")
        val lat: Double?,
        @SerializedName("lon")
        val lon: Double?,
        @SerializedName("secsSinceReport")
        val secsSinceReport: Int?,
        @SerializedName("predictable")
        val predictable: Boolean?,
        @SerializedName("heading")
        val heading: Int?,
        @SerializedName("speedKmHr")
        val speedKmHr: Double?,
    )
}
