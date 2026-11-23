package org.mtransit.android.commons.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.ScheduleTimestamps.LOG_TAG
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonArrayOrNull
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private const val JSON_SOURCE_LABEL = "sourceLabel"
private const val JSON_LOCAL_TIME_ZONE_ID = "tz"
private const val JSON_TIMESTAMPS = "timestamps"

fun ScheduleTimestamps.toScheduleTimestamps(jsonString: String) = try {
    this.toScheduleTimestamps(sharedJson.parseToJsonElement(jsonString).jsonObject)
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON string '$jsonString'", se)
    throw se
}

fun ScheduleTimestamps.toScheduleTimestamps(jsonObject: JsonObject) = this.apply {
    try {
        sourceLabel = jsonObject[JSON_SOURCE_LABEL]?.jsonPrimitiveOrNull?.contentOrNull
        var localTimeZoneId = jsonObject[JSON_LOCAL_TIME_ZONE_ID]?.jsonPrimitiveOrNull?.contentOrNull
        val timestamps = jsonObject[JSON_TIMESTAMPS]?.jsonArrayOrNull?.mapNotNull {
            it.jsonObjectOrNull?.toTimestamp()
        }.orEmpty()
        if (localTimeZoneId == null) {
            //noinspection DiscouragedApi
            localTimeZoneId = timestamps.find { it.localTimeZoneId != null }?.localTimeZoneId
        }
        setTimestampsAndSort(timestamps)
        setLocalTimeZoneId(localTimeZoneId)
    } catch (se: SerializationException) {
        MTLogK.w(LOG_TAG, "Error while parsing JSON object '$jsonObject'", se)
        throw se
    }
}

fun ScheduleTimestamps.toJson() = try {
    buildJsonObject {
        putK(JSON_SOURCE_LABEL, this@toJson.sourceLabel)
        putK(JSON_LOCAL_TIME_ZONE_ID, this@toJson.localTimeZoneId)
        put(
            JSON_TIMESTAMPS,
            buildJsonArray {
                addAll(this@toJson.timestamps.mapNotNull { it.toJson() })
            }
        )
    }
} catch (se: SerializationException) {
    MTLogK.w(this, "Error while making JSON object '$this'", se)
    throw se
}

fun ScheduleTimestamps.toJsonString() = sharedJson.encodeToString(toJson())
