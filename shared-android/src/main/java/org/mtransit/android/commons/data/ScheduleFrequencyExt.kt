package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.Schedule.Frequency.LOG_TAG
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private const val JSON_START_TIME_IN_MS = "startTimeInMs"
private const val JSON_END_TIME_IN_MS = "endTimeInMs"
private const val JSON_HEADWAY_IN_SEC = "headwayInSec"
private const val JSON_OLD_SCHEDULE = "old"

fun JsonObject.toFrequency() = try {
    Schedule.Frequency(
        getValue(JSON_START_TIME_IN_MS).jsonPrimitive.long,
        getValue(JSON_END_TIME_IN_MS).jsonPrimitive.long,
        getValue(JSON_HEADWAY_IN_SEC).jsonPrimitive.int,
        get(JSON_OLD_SCHEDULE)?.jsonPrimitiveOrNull?.booleanOrNull,
    )
} catch (iae: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON object '$this'", iae)
    null // skip this 1 only
}

fun Schedule.Frequency.toJson() = try {
    buildJsonObject {
        putK(JSON_START_TIME_IN_MS, startTimeInMs)
        putK(JSON_END_TIME_IN_MS, endTimeInMs)
        putK(JSON_HEADWAY_IN_SEC, headwayInSec)
        oldSchedule?.let { putK(JSON_OLD_SCHEDULE, it) }
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(this, "Error while making JSON object '$this'", iae)
    null // skip this 1 only
}
