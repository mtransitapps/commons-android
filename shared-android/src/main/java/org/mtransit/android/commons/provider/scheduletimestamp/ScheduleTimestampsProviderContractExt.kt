package org.mtransit.android.commons.provider.scheduletimestamp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.toRouteDirectionStop
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.commons.provider.scheduletimestamp.ScheduleTimestampsProviderContract.Filter.LOG_TAG
import org.mtransit.commons.jsonObjectOrNull
import kotlinx.serialization.json.put as putK

// region Filter

private const val JSON_ROUTE_DIRECTION_STOP = "routeTripStop" // do not change to avoid breaking compat w/ old modules
private const val JSON_STARTS_AT_IN_MS = "startsAtInMs"
private const val JSON_ENDS_AT_IN_MS = "endsAtInMs"

fun String?.toFilterN() = this?.toFilter()

fun String.toFilter() = try {
    sharedJson.parseToJsonElement(this).jsonObject.toFilter()
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON string '$this'", se)
    throw se
}

fun JsonObject.toFilter() = try {
    this[JSON_ROUTE_DIRECTION_STOP]?.jsonObjectOrNull?.toRouteDirectionStop()?.let { rds ->
        ScheduleTimestampsProviderContract.Filter(
            rds,
            getValue(JSON_STARTS_AT_IN_MS).jsonPrimitive.long,
            getValue(JSON_ENDS_AT_IN_MS).jsonPrimitive.long
        )
    }
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON object '$this'", se)
    throw se
}

fun ScheduleTimestampsProviderContract.Filter.toJson() = try {
    buildJsonObject {
        put(JSON_ROUTE_DIRECTION_STOP, this@toJson.routeDirectionStop.toJson())
        putK(JSON_STARTS_AT_IN_MS, this@toJson.startsAtInMs)
        putK(JSON_ENDS_AT_IN_MS, this@toJson.endsAtInMs)
    }
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while making JSON object '$this'", se)
    throw se
}

fun ScheduleTimestampsProviderContract.Filter.toJsonString() = sharedJson.encodeToString(this.toJson())

// endregion
