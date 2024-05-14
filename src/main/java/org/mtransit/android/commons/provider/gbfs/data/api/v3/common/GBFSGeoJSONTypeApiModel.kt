package org.mtransit.android.commons.provider.gbfs.data.api.v3.common

import com.google.gson.annotations.SerializedName

enum class GBFSGeoJSONTypeApiModel {
    // https://github.com/mapbox/mapbox-java/blob/main/services-geojson/src/main/java/com/mapbox/geojson/Feature.java
    @SerializedName("Feature")
    FEATURE,

    // IETF RFC 7946 https://tools.ietf.org/html/rfc7946#section-3.3
    // https://github.com/mapbox/mapbox-java/blob/main/services-geojson/src/main/java/com/mapbox/geojson/FeatureCollection.java
    @SerializedName("FeatureCollection")
    FEATURE_COLLECTION,

    // IETF RFC https://tools.ietf.org/html/rfc7946#section-3.1.7
    // https://github.com/mapbox/mapbox-java/blob/main/services-geojson/src/main/java/com/mapbox/geojson/MultiPolygon.java
    @SerializedName("MultiPolygon")
    MULTI_POLYGON,
}