package org.mtransit.android.commons.provider

import android.annotation.SuppressLint
import androidx.annotation.Discouraged
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.provider.StmInfoApiProvider.JArrivals
import org.mtransit.android.commons.provider.StmInfoApiProvider.LOG_TAG
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonArrayOrNull
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull
import org.mtransit.android.commons.provider.StmInfoApiProvider.JMessages as LegacyMessages

@SuppressLint("DiscouragedApi")
@Discouraged(message = "Using StmInfoServiceUpdateProvider instead") // USING same feed as real-time POI status schedule
fun String?.toLegacyMessagesResults() = this?.toLegacyMessages()?.results.orEmpty()

@SuppressLint("DiscouragedApi")
@Discouraged(message = "Using StmInfoServiceUpdateProvider instead") // USING same feed as real-time POI status schedule
fun String?.toLegacyMessages() = try {
    this?.let { sharedJson.parseToJsonElement(it).jsonObject.toLegacyMessages() }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", iae)
    null
}

private const val JSON_RESULT = "result"

@SuppressLint("DiscouragedApi")
@Discouraged(message = "Using StmInfoServiceUpdateProvider instead") // USING same feed as real-time POI status schedule
fun JsonObject.toLegacyMessages() = try {
    LegacyMessages(
        buildList {
        val shortNameResultRouteList = mutableListOf<MutableMap<String, List<LegacyMessages.JResult.JResultRoute>>>()
        this@toLegacyMessages[JSON_RESULT]?.jsonArrayOrNull?.forEach { jResult ->
            val shortNameResultRoutes = mutableMapOf<String, List<LegacyMessages.JResult.JResultRoute>>()
            jResult.jsonObject.forEach { (jResultKey, jResultRoutes) ->
                shortNameResultRoutes[jResultKey] = jResultRoutes.jsonArray.map { it.jsonObject.toJResultRoute() }
            }
            shortNameResultRouteList.add(shortNameResultRoutes)
        }
        add(LegacyMessages.JResult(shortNameResultRouteList))
    }
    )
} catch (e: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", e)
    null
}

private const val JSON_DIRECTION = "direction"
private const val JSON_DIRECTION_NAME = "direction_name"
private const val JSON_DATE = "date"
private const val JSON_TEXT = "text"
private const val JSON_CODE = "code"

@SuppressLint("DiscouragedApi")
@Discouraged(message = "Using StmInfoServiceUpdateProvider instead") // USING same feed as real-time POI status schedule
fun JsonObject.toJResultRoute() = LegacyMessages.JResult.JResultRoute(
    this.getValue(JSON_DIRECTION).jsonPrimitive.content,
    this.getValue(JSON_DIRECTION_NAME).jsonPrimitive.content,
    this.getValue(JSON_TEXT).jsonPrimitive.content,
    this.getValue(JSON_CODE).jsonPrimitive.content,
    this.getValue(JSON_DATE).jsonPrimitive.content,
)

@SuppressLint("DiscouragedApi")
@Discouraged(message = "Using GTFS-RT Trip Updates instead")
fun String?.toJArrivals() = try {
    this?.let { sharedJson.parseToJsonElement(it).jsonObject.toJArrivals() }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", iae)
    null
}

private const val JSON_MESSAGES = "messages"
private const val JSON_LINE = "line"
private const val JSON_STOP_POINT = "stopPoint"

@SuppressLint("DiscouragedApi")
@Discouraged(message = "Using GTFS-RT Trip Updates instead")
fun JsonObject.toJArrivals() = try {
    val jMessages = this[JSON_MESSAGES]?.jsonObjectOrNull
    JArrivals(
        JArrivals.JMessages(
            jMessages?.get(JSON_LINE)?.jsonArrayOrNull?.mapNotNull {
                it.jsonObject.toJMessage()
            }.orEmpty(),
            jMessages?.get(JSON_STOP_POINT)?.jsonArrayOrNull?.mapNotNull {
                it.jsonObject.toJMessage()
            }.orEmpty(),
        ),
        this[JSON_RESULT]?.jsonArrayOrNull?.mapNotNull {
            it.jsonObject.toJResult()
        }.orEmpty(),
    )
} catch (e: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", e)
    null
}

private const val JSON_TIME = "time"
private const val JSON_IS_REAL = "is_real"
private const val JSON_IS_CONGESTION = "is_congestion"
private const val JSON_IS_RAMP_CANCELLED = "is_ramp_cancelled"
private const val JSON_IS_ON_REQUESTED_DAY = "is_on_requested_day"

private const val TIME_RT_NOT_LENGTH = 4

@Discouraged(message = "Using GTFS-RT Trip Updates instead")
private fun JsonObject.toJResult() = try {
    this[JSON_TIME]?.jsonPrimitiveOrNull?.contentOrNull?.let { time ->
        JArrivals.JResult(
            time,
            this[JSON_IS_REAL]?.jsonPrimitiveOrNull?.booleanOrNull ?: (time.length != TIME_RT_NOT_LENGTH),
            this[JSON_IS_CONGESTION]?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
            this[JSON_IS_RAMP_CANCELLED]?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
            this[JSON_IS_ON_REQUESTED_DAY]?.jsonPrimitiveOrNull?.booleanOrNull ?: true
        )
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", iae)
    null
}

private const val JSON_START_DATE = "start_date"

@Discouraged(message = "Using GTFS-RT Trip Updates instead")
private fun JsonObject.toJMessage() =
    this[JSON_TEXT]?.jsonPrimitiveOrNull?.contentOrNull
        ?.takeIf { it.isNotEmpty() }
        ?.let { text ->
            JArrivals.JMessages.JMessage(
                text,
                this[JSON_START_DATE]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty()
            )
        }
