package org.mtransit.android.commons.provider.gbfs.data.api.v2.common

import com.google.gson.annotations.SerializedName

data class GBFSGeoJSONMultiPolygonApiModel(
    @SerializedName("type")
    val type: GBFSGeoJSONTypeApiModel,
    @SerializedName("coordinates")
    val coordinates: List<List<List<List<Double>>>>?,
)
