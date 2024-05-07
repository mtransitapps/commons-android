package org.mtransit.android.commons.provider.gbfs.data.api.common

import com.google.gson.annotations.SerializedName

data class GBFSVehicleTypesCountApiModel(
    @SerializedName("vehicle_type_ids")
    val vehicleTypeIds: List<GBFSIDApiType>,
    @SerializedName("count")
    val count: Int,
    @Deprecated("Removed in v3.0")
    @SerializedName("vehicle_type_id")
    val vehicleTypeId: GBFSIDApiType?,
)