package org.mtransit.android.commons.provider.gbfs.data.api.v3.common

import com.google.gson.annotations.SerializedName

data class GBFSGeoJSONFeatureApiModel(
    @SerializedName("type")
    val type: GBFSGeoJSONTypeApiModel,
    @SerializedName("geometry")
    val geometry: GBFSGeoJSONMultiPolygonApiModel,
    @SerializedName("properties")
    val properties: GBFSGeoJSONPropertiesApiModel,
) {
    data class GBFSGeoJSONPropertiesApiModel(
        @SerializedName("name")
        val name: List<GBFSLocalizedStringApiModel>?,
        @SerializedName("start")
        val start: GBFSTimestampApiType?,
        @SerializedName("end")
        val end: GBFSTimestampApiType?,
        @SerializedName("rules")
        val rules: List<GBFSGeofencingRuleApiModel>?,
    )
}
