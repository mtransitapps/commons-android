package org.mtransit.android.commons.provider.gbfs.data.api.v3.common

import com.google.gson.annotations.SerializedName

data class GBFSGeoJSONFeatureCollectionApiModel(
    @SerializedName("type")
    val type: GBFSGeoJSONTypeApiModel,
    @SerializedName("features")
    val features: List<GBFSGeoJSONFeatureApiModel>,
)