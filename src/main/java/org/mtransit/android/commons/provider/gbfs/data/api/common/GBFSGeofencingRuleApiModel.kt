package org.mtransit.android.commons.provider.gbfs.data.api.common

import com.google.gson.annotations.SerializedName

// https://gbfs.org/specification/reference/#geofencing-rule-object
data class GBFSGeofencingRuleApiModel(
    @SerializedName("vehicle_type_ids")
    val vehicleTypeIds: List<GBFSIDApiType>?,
    @SerializedName("ride_start_allowed")
    val rideStartAllowed: Boolean,
    @SerializedName("ride_end_allowed")
    val rideEndAllowed: Boolean,
    @SerializedName("ride_through_allowed")
    val rideThroughAllowed: Boolean,
    @SerializedName("maximum_speed_kph")
    val maximumSpeedKph: Int?,
    @SerializedName("station_parking")
    val stationParking: Boolean?,
)
