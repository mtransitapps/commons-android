package org.mtransit.android.commons.provider.gbfs.data.api

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSGeofencingZonesApiModel.GBFSGeofencingZonesDataApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSGeoJSONFeatureCollectionApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSGeofencingRuleApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSTimestampApiType

// https://gbfs.org/specification/reference/#geofencing_zonesjson
data class GBFSGeofencingZonesApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSGeofencingZonesDataApiModel,
) : GBFSCommonApiModel<GBFSGeofencingZonesDataApiModel>() {
    data class GBFSGeofencingZonesDataApiModel(
        @SerializedName("geofencing_zones")
        val geofencingZones: GBFSGeoJSONFeatureCollectionApiModel,
        @SerializedName("global_rules")
        val globalRules: List<GBFSGeofencingRuleApiModel>,
    )
}
