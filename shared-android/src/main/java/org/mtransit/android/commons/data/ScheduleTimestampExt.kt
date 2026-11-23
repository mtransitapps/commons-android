package org.mtransit.android.commons.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import org.mtransit.android.commons.BuildConfig
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.Schedule.Timestamp.LOG_TAG
import kotlinx.serialization.json.put as putK

private const val JSON_DEPARTURE = "t"
private const val JSON_ORIGINAL_DEPARTURE_DELAY = "tOD"
private const val JSON_DEPARTURE_ARRIVAL_DIFF = "tDiffA"
private const val JSON_ORIGINAL_ARRIVAL_DELAY = "tOA"
private const val JSON_TRIP_ID = "trip_id"
private const val JSON_STOP_SEQUENCE = "stop_seq"
private const val JSON_HEADSIGN_TYPE = "ht"
private const val JSON_HEADSIGN_VALUE = "hv"
private const val JSON_LOCAL_TIME_ZONE_ID_ = "localTimeZone"
private const val JSON_REAL_TIME = "rt"
private const val JSON_OLD_SCHEDULE = "old"
private const val JSON_ACCESSIBLE = "a11y"
private const val JSON_CANCELLED = "cancelled"

fun JsonObject.toTimestamp() = try {
    val localTimeZoneId = jsonObject[JSON_LOCAL_TIME_ZONE_ID_]?.jsonPrimitive?.contentOrNull
    if (localTimeZoneId == null) {
        check(!BuildConfig.DEBUG) { "Timestamp missing timezone in JSON!" }
        MTLogK.w(LOG_TAG, "Timestamp missing timezone in JSON '$this'!")
    }
    Schedule.Timestamp(
        getValue(JSON_DEPARTURE).jsonPrimitive.long,
        localTimeZoneId,
    ).apply {
        jsonObject[JSON_ORIGINAL_DEPARTURE_DELAY]?.jsonPrimitive?.longOrNull?.let { originalDepartureDelayMs = it }
        jsonObject[JSON_DEPARTURE_ARRIVAL_DIFF]?.jsonPrimitive?.longOrNull?.let { departureArrivalDiffMs = it }
        jsonObject[JSON_ORIGINAL_ARRIVAL_DELAY]?.jsonPrimitive?.longOrNull?.let { originalArrivalDelayMs = it }
        jsonObject[JSON_TRIP_ID]?.jsonPrimitive?.contentOrNull?.let { tripId = it }
        jsonObject[JSON_STOP_SEQUENCE]?.jsonPrimitive?.intOrNull?.let { setStopSequence(it) }
        val headSignType = jsonObject[JSON_HEADSIGN_TYPE]?.jsonPrimitive?.intOrNull ?: Direction.HEADSIGN_TYPE_NONE
        val headSignValue = jsonObject[JSON_HEADSIGN_VALUE]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (headSignType >= 0 && headSignValue.isNotEmpty()) {
            setHeadsign(headSignType, headSignValue)
        } else {
            if (headSignType == Direction.HEADSIGN_TYPE_NO_PICKUP) {
                setHeadsign(headSignType, null)
            }
        }
        jsonObject[JSON_REAL_TIME]?.jsonPrimitive?.booleanOrNull?.let { realTime = it }
        jsonObject[JSON_OLD_SCHEDULE]?.jsonPrimitive?.booleanOrNull?.let { oldSchedule = it }
        jsonObject[JSON_ACCESSIBLE]?.jsonPrimitive?.intOrNull?.let { accessible = it }
        jsonObject[JSON_CANCELLED]?.jsonPrimitive?.booleanOrNull?.let { cancelled = it }
    }
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON object '$jsonObject'", se)
    null // skip this 1 only
}

fun Schedule.Timestamp.toJson() = try {
    buildJsonObject {
        putK(JSON_DEPARTURE, departureT)
        //noinspection DiscouragedApi
        localTimeZoneId?.let { putK(JSON_LOCAL_TIME_ZONE_ID_, it) }
        originalDepartureDelayMs.takeIf { it != 0L }?.let { putK(JSON_ORIGINAL_DEPARTURE_DELAY, it) }
        departureArrivalDiffMs?.let { putK(JSON_DEPARTURE_ARRIVAL_DIFF, it) }
        originalArrivalDelayMs.takeIf { it != 0L }?.let { putK(JSON_ORIGINAL_ARRIVAL_DELAY, it) }
        tripId?.let { putK(JSON_TRIP_ID, it) }
        stopSequenceOrNull?.let { putK(JSON_STOP_SEQUENCE, it) }
        if (headsignType != Direction.HEADSIGN_TYPE_NONE && headsignValue != null) {
            putK(JSON_HEADSIGN_TYPE, headsignType)
            putK(JSON_HEADSIGN_VALUE, headsignValue)
        } else {
            if (headsignType == Direction.HEADSIGN_TYPE_NO_PICKUP) {
                putK(JSON_HEADSIGN_TYPE, headsignType)
            }
        }
        realTime?.let { putK(JSON_REAL_TIME, it) }
        oldSchedule?.let { putK(JSON_OLD_SCHEDULE, it) }
        accessible?.let { putK(JSON_ACCESSIBLE, it) }
        cancelled?.let { putK(JSON_CANCELLED, it) }
    }
} catch (se: SerializationException) {
    MTLogK.w(this, "Error while making JSON object '$this'", se)
    null // skip this 1 only
}
