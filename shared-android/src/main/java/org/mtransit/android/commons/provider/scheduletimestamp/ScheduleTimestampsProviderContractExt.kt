package org.mtransit.android.commons.provider.scheduletimestamp

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.mtransit.android.commons.data.toRouteDirectionStop
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.commons.provider.scheduletimestamp.ScheduleTimestampsProviderContract.Filter.LOG_TAG
import org.mtransit.commons.jsonObjectOrNull
import kotlinx.serialization.json.put as putK

// region Filter

private const val JSON_ROUTE_DIRECTION_STOP = "routeTripStop" // do not change to avoid breaking compat w/ old modules
private const val JSON_STARTS_AT_IN_MS = "startsAtInMs"
private const val JSON_ENDS_AT_IN_MS = "endsAtInMs"

fun String?.toFilter() = this?.jsonRunCatching(LOG_TAG) {
    sharedJson.parseToJsonElement(this).jsonObject.toFilter()
}?.getOrNull()

private fun JsonObject.toFilter() =
    this[JSON_ROUTE_DIRECTION_STOP]?.jsonObjectOrNull?.toRouteDirectionStop()?.let { rds ->
        ScheduleTimestampsProviderContract.Filter(
            rds,
            getValue(JSON_STARTS_AT_IN_MS).jsonPrimitive.long,
            getValue(JSON_ENDS_AT_IN_MS).jsonPrimitive.long
        )
    }

fun ScheduleTimestampsProviderContract.Filter.toJson() = jsonRunCatching {
    buildJsonObject {
        put(JSON_ROUTE_DIRECTION_STOP, this@toJson.routeDirectionStop.toJson())
        putK(JSON_STARTS_AT_IN_MS, this@toJson.startsAtInMs)
        putK(JSON_ENDS_AT_IN_MS, this@toJson.endsAtInMs)
    }
}.getOrThrow()

fun ScheduleTimestampsProviderContract.Filter.toJsonString() = sharedJson.encodeToString(this.toJson())

// endregion
