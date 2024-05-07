package org.mtransit.android.commons.provider.gbfs.data.api.common

import com.google.gson.annotations.SerializedName

data class GBFSGeoJSONApiModel(
    @SerializedName("type")
    val type: GBFSGeoJSONTypeApiModel,
    @SerializedName("coordinates")
    val coordinates: List<List<List<List<Double>>>>,
) {
    enum class GBFSGeoJSONTypeApiModel {
        @SerializedName("MultiPolygon")
        MULTI_POLYGON,
    }
}
