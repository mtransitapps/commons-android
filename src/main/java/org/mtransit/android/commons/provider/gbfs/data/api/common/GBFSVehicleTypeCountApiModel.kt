package org.mtransit.android.commons.provider.gbfs.data.api.common

import com.google.gson.annotations.SerializedName

data class GBFSVehicleTypeCountApiModel(
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: GBFSIDApiType?,
    @SerializedName("count")
    val count: Int,
)