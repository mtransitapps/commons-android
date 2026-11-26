package org.mtransit.android.commons.provider

import androidx.annotation.VisibleForTesting
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.Accessibility
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.commons.provider.ReginaTransitProvider.LOG_TAG
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonArrayOrNull
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull
import java.text.ParseException
import java.util.concurrent.TimeUnit

fun String?.toStatusesOrNull(
    localTimeZoneId: String,
    afterMs: Long,
    stopCode: String,
    getBeginningOfTodayMs: () -> Long,
    makeSchedule: () -> Schedule,
    cleanTripHeadsign: (String) -> String,
): List<POIStatus>? = this?.takeIf { it.isNotBlank() }?.jsonRunCatching(LOG_TAG) {
    sharedJson.parseToJsonElement(this).jsonArrayOrNull?.toStatuses(
        localTimeZoneId = localTimeZoneId,
        afterMs = afterMs,
        stopCode = stopCode,
        getBeginningOfTodayMs = getBeginningOfTodayMs,
        makeSchedule = makeSchedule,
        cleanTripHeadsign = cleanTripHeadsign,
    )
}?.getOrNull()

private fun JsonArray.toStatuses(
    localTimeZoneId: String,
    afterMs: Long,
    stopCode: String,
    getBeginningOfTodayMs: () -> Long,
    makeSchedule: () -> Schedule,
    cleanTripHeadsign: (String) -> String,
) = buildList {
    val beginningOfTodayMs = getBeginningOfTodayMs()
    this@toStatuses
        .mapNotNull {
            it.jsonObjectOrNull?.toTimestamp(localTimeZoneId, beginningOfTodayMs, afterMs, stopCode, cleanTripHeadsign)
        }.takeIf { it.isNotEmpty() }
        ?.let { timestamps ->
            makeSchedule().apply {
                setTimestampsAndSort(timestamps)
            }.let { newStatus ->
                add(newStatus)
            }
        }
}

private const val JSON_PRED_TIME = "pred_time"
private const val JSON_LINE_NAME = "line_name"
private const val JSON_BUS_ID = "bus_id"
private const val JSON_LAST_STOP = "last_stop"

private val ONE_DAY = TimeUnit.DAYS.toMillis(1L)

@VisibleForTesting
internal fun String?.parseTime() = try {
    this?.takeIf { it.isNotEmpty() }?.let {
        ReginaTransitProvider.DATE_FORMATTER_UTC.parseThreadSafe(it)
    }?.time
} catch (pe: ParseException) {
    MTLogK.w(LOG_TAG, "Error while parsing '$this'!", pe)
    null
}

private fun JsonObject.toTimestamp(
    localTimeZoneId: String,
    beginningOfTodayMs: Long,
    after: Long,
    stopCode: String,
    cleanTripHeadsign: (String) -> String,
) =
    this[JSON_PRED_TIME]?.jsonPrimitiveOrNull?.contentOrNull
        ?.parseTime()
        ?.let { time ->
            Schedule.Timestamp(
                (beginningOfTodayMs + TimeUtils.timeToTheTensSecondsMillis(time)).let { timestampMs ->
                    if (timestampMs < after) timestampMs + ONE_DAY else timestampMs
                },
                localTimeZoneId
            ).apply {
                this@toTimestamp[JSON_LINE_NAME]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotEmpty() }?.let { destinationName ->
                    setHeadsign(Direction.HEADSIGN_TYPE_STRING, cleanTripHeadsign(destinationName))
                }
                this@toTimestamp[JSON_BUS_ID]?.jsonPrimitiveOrNull?.contentOrNull?.let { busId ->
                    realTime = busId.isNotEmpty() // no bus ID = scheduled = not real-time
                }
                accessible = Accessibility.UNKNOWN // no info available on https://transitlive.com/mobile/
                if (this@toTimestamp[JSON_LAST_STOP]?.jsonPrimitiveOrNull?.contentOrNull == stopCode) {
                    setHeadsign(Direction.HEADSIGN_TYPE_NO_PICKUP, null)
                }
            }
        }
