package org.mtransit.android.commons.data

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
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.commons.jsonPrimitiveOrNull
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

fun JsonObject.toTimestamp() = jsonRunCatching(LOG_TAG) {
    val localTimeZoneId = this[JSON_LOCAL_TIME_ZONE_ID_]?.jsonPrimitiveOrNull?.contentOrNull
    if (localTimeZoneId == null) {
        check(!BuildConfig.DEBUG) { "Timestamp missing timezone in JSON!" }
        MTLogK.w(LOG_TAG, "Timestamp missing timezone in JSON '$this'!")
    }
    Schedule.Timestamp(
        getValue(JSON_DEPARTURE).jsonPrimitive.long,
        localTimeZoneId,
    ).apply {
        get(JSON_ORIGINAL_DEPARTURE_DELAY)?.jsonPrimitiveOrNull?.longOrNull?.let { originalDepartureDelayMs = it }
        get(JSON_DEPARTURE_ARRIVAL_DIFF)?.jsonPrimitiveOrNull?.longOrNull?.let { departureArrivalDiffMs = it }
        get(JSON_ORIGINAL_ARRIVAL_DELAY)?.jsonPrimitiveOrNull?.longOrNull?.let { originalArrivalDelayMs = it }
        get(JSON_TRIP_ID)?.jsonPrimitiveOrNull?.contentOrNull?.let { tripId = it }
        get(JSON_STOP_SEQUENCE)?.jsonPrimitiveOrNull?.intOrNull?.let { setStopSequence(it) }
        val headSignType = get(JSON_HEADSIGN_TYPE)?.jsonPrimitiveOrNull?.intOrNull ?: Direction.HEADSIGN_TYPE_NONE
        val headSignValue = get(JSON_HEADSIGN_VALUE)?.jsonPrimitiveOrNull?.contentOrNull.orEmpty()
        if (headSignType >= 0 && headSignValue.isNotEmpty()) {
            setHeadsign(headSignType, headSignValue)
        } else {
            if (headSignType == Direction.HEADSIGN_TYPE_NO_PICKUP) {
                setHeadsign(headSignType, null)
            }
        }
        get(JSON_REAL_TIME)?.jsonPrimitiveOrNull?.booleanOrNull?.let { realTime = it }
        get(JSON_OLD_SCHEDULE)?.jsonPrimitiveOrNull?.booleanOrNull?.let { oldSchedule = it }
        get(JSON_ACCESSIBLE)?.jsonPrimitiveOrNull?.intOrNull?.let { accessible = it }
        get(JSON_CANCELLED)?.jsonPrimitiveOrNull?.booleanOrNull?.let { cancelled = it }
    }
}.getOrNull() // no partial results

fun Schedule.Timestamp.toJson() = jsonRunCatching {
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
}.getOrNull() // no partial results
