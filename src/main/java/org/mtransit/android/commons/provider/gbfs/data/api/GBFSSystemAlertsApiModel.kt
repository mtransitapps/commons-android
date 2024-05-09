package org.mtransit.android.commons.provider.gbfs.data.api

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.GBFSSystemAlertsApiModel.GBFSSystemAlertsDataApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSIDApiType
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSLocalizedURLApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.common.GBFSTimestampApiType

// https://gbfs.org/specification/reference/#system_alertsjson
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v3.0/system_alerts.json
data class GBFSSystemAlertsApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSSystemAlertsDataApiModel,
) : GBFSCommonApiModel<GBFSSystemAlertsDataApiModel>() {
    data class GBFSSystemAlertsDataApiModel(
        @SerializedName("alerts")
        val alerts: List<GBFSAlertApiModel>,
    ) {
        data class GBFSAlertApiModel(
            @SerializedName("alert_id")
            val alertId: GBFSIDApiType,
            @SerializedName("type")
            val type: GBFSAlertTypeApiModel,
            @SerializedName("times")
            val times: List<GBFSAlertTimeApiModel>,
            @SerializedName("station_ids")
            val stationIds: List<GBFSIDApiType>?,
            @SerializedName("region_ids")
            val regionIds: List<GBFSIDApiType>?,
            @SerializedName("url") // added in v3.0
            val url: List<GBFSLocalizedURLApiModel>?,
            @SerializedName("summary") // added in v3.0
            val summary: List<GBFSLocalizedURLApiModel>?,
            @SerializedName("description") // added in v3.0
            val description: List<GBFSLocalizedURLApiModel>?,
            @SerializedName("last_updated")
            val lastUpdated: GBFSTimestampApiType?,
        ) {
            @Suppress("unused")
            enum class GBFSAlertTypeApiModel {
                @SerializedName("system_closure")
                SYSTEM_CLOSURE,

                @SerializedName("station_closure")
                STATION_CLOSURE,

                @SerializedName("station_move")
                STATION_MOVE,

                @SerializedName("other")
                OTHER,
            }

            data class GBFSAlertTimeApiModel(
                @SerializedName("start")
                val start: GBFSTimestampApiType,
                @SerializedName("end")
                val end: GBFSTimestampApiType?,
            )
        }
    }
}
