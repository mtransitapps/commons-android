package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.mtransit.android.commons.data.Schedule.Frequency.LOG_TAG
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private const val JSON_START_TIME_IN_MS = "startTimeInMs"
private const val JSON_END_TIME_IN_MS = "endTimeInMs"
private const val JSON_HEADWAY_IN_SEC = "headwayInSec"
private const val JSON_OLD_SCHEDULE = "old"

fun JsonObject.toFrequency() = jsonRunCatching(LOG_TAG) {
    Schedule.Frequency(
        getValue(JSON_START_TIME_IN_MS).jsonPrimitive.long,
        getValue(JSON_END_TIME_IN_MS).jsonPrimitive.long,
        getValue(JSON_HEADWAY_IN_SEC).jsonPrimitive.int,
        get(JSON_OLD_SCHEDULE)?.jsonPrimitiveOrNull?.booleanOrNull,
    )
}.getOrNull() // skip this 1 only

fun Schedule.Frequency.toJson() = jsonRunCatching {
    buildJsonObject {
        putK(JSON_START_TIME_IN_MS, startTimeInMs)
        putK(JSON_END_TIME_IN_MS, endTimeInMs)
        putK(JSON_HEADWAY_IN_SEC, headwayInSec)
        oldSchedule?.let { putK(JSON_OLD_SCHEDULE, it) }
    }
}.getOrNull() // skip this 1 only
