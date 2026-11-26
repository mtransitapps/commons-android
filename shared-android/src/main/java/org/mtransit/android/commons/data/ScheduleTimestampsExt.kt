package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.mtransit.android.commons.data.ScheduleTimestamps.LOG_TAG
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonArrayOrNull
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private const val JSON_SOURCE_LABEL = "sourceLabel"
private const val JSON_LOCAL_TIME_ZONE_ID = "tz"
private const val JSON_TIMESTAMPS = "timestamps"

fun String?.toScheduleTimestampsOrNull(scheduleTimestamps: ScheduleTimestamps) = this?.takeIf { it.isNotBlank() }?.jsonRunCatching(LOG_TAG) {
    sharedJson.parseToJsonElement(this).jsonObject.toScheduleTimestamps(scheduleTimestamps)
}?.getOrNull()

private fun JsonObject.toScheduleTimestamps(scheduleTimestamps: ScheduleTimestamps) = scheduleTimestamps.apply {
    sourceLabel = this@toScheduleTimestamps[JSON_SOURCE_LABEL]?.jsonPrimitiveOrNull?.contentOrNull
    var localTimeZoneId = this@toScheduleTimestamps[JSON_LOCAL_TIME_ZONE_ID]?.jsonPrimitiveOrNull?.contentOrNull
    val timestamps = this@toScheduleTimestamps.getValue(JSON_TIMESTAMPS).jsonArray.mapNotNull {
        it.jsonObjectOrNull?.toTimestamp()
    }
    if (localTimeZoneId == null) {
        //noinspection DiscouragedApi
        localTimeZoneId = timestamps.find { it.localTimeZoneId != null }?.localTimeZoneId
    }
    setTimestampsAndSort(timestamps)
    setLocalTimeZoneId(localTimeZoneId)
}

fun ScheduleTimestamps.toJson() = jsonRunCatching {
    buildJsonObject {
        this@toJson.sourceLabel?.let { putK(JSON_SOURCE_LABEL, it) }
        this@toJson.localTimeZoneId?.let { putK(JSON_LOCAL_TIME_ZONE_ID, it) }
        put(
            JSON_TIMESTAMPS,
            buildJsonArray {
                addAll(this@toJson.timestamps.mapNotNull { it.toJson() })
            }
        )
    }
}.getOrThrow()

fun ScheduleTimestamps.toJsonString() = sharedJson.encodeToString(toJson())
