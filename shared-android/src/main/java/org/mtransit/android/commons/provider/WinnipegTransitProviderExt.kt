package org.mtransit.android.commons.provider

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.Accessibility
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.commons.provider.WinnipegTransitProvider.AGENCY_SOURCE_ID
import org.mtransit.android.commons.provider.WinnipegTransitProvider.DEFAULT_LINK
import org.mtransit.android.commons.provider.WinnipegTransitProvider.LOG_TAG
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonArrayOrNull
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull
import java.util.Locale

// region status

fun String?.toStatuses(
    directionId: Long,
    makeSchedule: () -> Schedule,
    cleanTripHeadsign: (String) -> String
) = this?.jsonRunCatching(LOG_TAG) {
    sharedJson.parseToJsonElement(this).jsonObjectOrNull?.toStatuses(
        directionId = directionId,
        makeSchedule = makeSchedule,
        cleanTripHeadsign = cleanTripHeadsign
    )
}?.getOrNull()

private const val JSON_STOP_SCHEDULE = "stop-schedule"
private const val JSON_ROUTE_SCHEDULES = "route-schedules"
private const val JSON_SCHEDULED_STOPS = "scheduled-stops"

private fun JsonObject.toStatuses(
    directionId: Long,
    makeSchedule: () -> Schedule,
    cleanTripHeadsign: (String) -> String
) = buildList<POIStatus> {
    val directionIdS = directionId.toString().last().toString()
    this@toStatuses[JSON_STOP_SCHEDULE]?.jsonObjectOrNull
        ?.get(JSON_ROUTE_SCHEDULES)?.jsonArrayOrNull?.firstOrNull()?.jsonObjectOrNull
        ?.get(JSON_SCHEDULED_STOPS)?.jsonArrayOrNull?.mapNotNull {
            it.jsonObjectOrNull?.toScheduleTimestamp(directionIdS, cleanTripHeadsign)
        }?.let { timestamps ->
            makeSchedule().apply {
                setTimestampsAndSort(timestamps)
            }.let {
                add(it)
            }
        }
}

private const val JSON_TIMES = "times"
private const val JSON_VARIANT = "variant"
private const val JSON_NAME = "name"
private const val JSON_BUS = "bus"

private const val JSON_KEY = "key"

private fun JsonObject.toScheduleTimestamp(
    directionId: String,
    cleanTripHeadsign: (String) -> String
): Schedule.Timestamp? {
    val variant = this[JSON_VARIANT]?.jsonObject
    val variantName = variant?.get(JSON_NAME)?.jsonPrimitiveOrNull?.contentOrNull
    val variantKey = variant?.get(JSON_KEY)?.jsonPrimitiveOrNull?.contentOrNull
    if (!variantKey.isNullOrEmpty() && !variantKey.contains(directionId)) {
        MTLogK.d(LOG_TAG, "Skip direction > other variant direction: '$variantKey' VS '$directionId'.")
        return null
    }
    val jTimes = this[JSON_TIMES]?.jsonObjectOrNull
    val timeString = jTimes?.timeString()?.takeIf { it.isNotEmpty() } ?: return null
    val date = runCatching { WinnipegTransitProvider.DATE_FORMATTER.parseThreadSafe(timeString) }.getOrNull() ?: return null
    return Schedule.Timestamp(
        TimeUtils.timeToTheTensSecondsMillis(date.time),
        WinnipegTransitProvider.WINNIPEG_TZ_ID
    ).apply {
        if (!variantName.isNullOrEmpty()) {
            setHeadsign(Direction.HEADSIGN_TYPE_STRING, cleanTripHeadsign(variantName))
        }
        realTime = this@toScheduleTimestamp[JSON_BUS] != null && jTimes.isRealTime()
        accessible = Accessibility.UNKNOWN
    }
}

private const val JSON_DEPARTURE = "departure"
private const val JSON_ARRIVAL = "arrival"
private val JSON_PERIOD_CANDIDATES = setOf(JSON_DEPARTURE, JSON_ARRIVAL)
private const val JSON_ESTIMATED = "estimated"
private const val JSON_SCHEDULED = "scheduled"
private val JSON_TIME_CANDIDATES = setOf(JSON_ESTIMATED, JSON_SCHEDULED)

private fun JsonObject.timeString(): String? = jsonRunCatching(LOG_TAG) {
    JSON_PERIOD_CANDIDATES.firstNotNullOfOrNull { period ->
        this[period]?.jsonObjectOrNull?.let { periodObject ->
            JSON_TIME_CANDIDATES.firstNotNullOfOrNull { timeType ->
                periodObject[timeType]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { time -> time.isNotEmpty() }
            }
        }
    }
}.getOrNull()

private fun JsonObject.isRealTime(): Boolean = jsonRunCatching(LOG_TAG) {
    JSON_PERIOD_CANDIDATES.firstNotNullOfOrNull { period ->
        this[period]?.jsonObjectOrNull?.let { periodObject ->
            periodObject[JSON_ESTIMATED]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotEmpty() }?.let {
                return@firstNotNullOfOrNull false
            }
            periodObject[JSON_SCHEDULED]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotEmpty() }?.let {
                return@firstNotNullOfOrNull true
            }
        }
    } ?: false
}.getOrDefault(false)

// endregion

// region news

private const val JSON_SERVICE_ADVISORIES = "service-advisories"

fun String?.toNews(
    makeNewsArticle: (uuid: String, priority: Int?, updatedAtMs: Long, text: String, textHTML: String, link: String) -> News,
) = this?.jsonRunCatching(LOG_TAG) {
    sharedJson.parseToJsonElement(this).jsonObject.toNews(makeNewsArticle)
}?.getOrNull()

private fun JsonObject.toNews(
    makeNewsArticle: (uuid: String, priority: Int?, updatedAtMs: Long, text: String, textHTML: String, link: String) -> News,
) = this[JSON_SERVICE_ADVISORIES]?.jsonArrayOrNull?.mapNotNullTo(ArrayList()) {
    it.jsonObjectOrNull?.toNewsArticle(makeNewsArticle)
}

private const val JSON_PRIORITY = "priority"
private const val JSON_TITLE = "title"
private const val JSON_BODY = "body"
private const val JSON_CATEGORY = "category"
private const val JSON_UPDATED_AT = "updated-at"

private val TRANSIT_CATEGORIES_LC = setOf("all", "transit")

private fun JsonObject.toNewsArticle(
    makeNewsArticle: (uuid: String, priority: Int?, updatedAtMs: Long, text: String, textHTML: String, link: String) -> News,
): News? = jsonRunCatching(LOG_TAG) {
    this[JSON_CATEGORY]?.jsonPrimitiveOrNull?.contentOrNull?.lowercase(Locale.ENGLISH)?.let { categoryLC ->
        if (categoryLC !in TRANSIT_CATEGORIES_LC) {
            return null
        }
    }
    val updatedAt = this[JSON_UPDATED_AT]?.jsonPrimitiveOrNull?.contentOrNull?.takeIf { it.isNotEmpty() } ?: run {
        return null
    }
    val date = runCatching { WinnipegTransitProvider.DATE_FORMATTER.parseThreadSafe(updatedAt) }.getOrNull() ?: run {
        MTLogK.w(LOG_TAG, "Date '$updatedAt' could NOT be parsed!")
        return null
    }
    val updatedAtMs = date.time
    val title = this[JSON_TITLE]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty()
    val body = this[JSON_BODY]?.jsonPrimitiveOrNull?.contentOrNull.orEmpty()
    val key = this[JSON_KEY]?.jsonPrimitiveOrNull?.contentOrNull?.toIntOrNull() ?: -1
    val (link, uuid) = key.toNewsLinkAndUuid(updatedAtMs)
    val priority = (this[JSON_PRIORITY]?.jsonPrimitiveOrNull?.contentOrNull?.toIntOrNull() ?: -1)
        .takeIf { it >= 0 }
    val (text, textHTML) = createNewsText(title, body, link) ?: run {
        MTLogK.w(LOG_TAG, "parseAgencyJSON() > skip (no text)")
        return null
    }
    return makeNewsArticle(
        uuid,
        priority,
        updatedAtMs,
        text,
        textHTML,
        link,
    )
}.getOrNull()

private const val LINK_AND_KEY = "https://info.winnipegtransit.com/en/schedules-maps-tools/service-advisories/%s"

private fun Int.toNewsLinkAndUuid(updatedAtInMs: Long): Pair<String, String> {
    val uuid = if (this < 0) AGENCY_SOURCE_ID + updatedAtInMs else AGENCY_SOURCE_ID + this
    val link = if (this < 0) DEFAULT_LINK else String.format(Locale.ENGLISH, LINK_AND_KEY, this)
    return link to uuid
}

private const val COLON = ": "

private fun createNewsText(title: String, body: String, link: String): Pair<String, String>? {
    val plainText = StringBuilder()
    val html = StringBuilder()
    if (title.isNotEmpty()) {
        plainText.append(title)
        html.append(HtmlUtils.applyBold(title))
    }
    if (body.isNotEmpty()) {
        if (plainText.isNotEmpty()) plainText.append(COLON)
        plainText.append(HtmlUtils.fromHtmlLegacy(body))
        if (html.isNotEmpty()) html.append(HtmlUtils.BR)
        html.append(HtmlUtils.toHTML(body))
    }
    if (plainText.isEmpty() || html.isEmpty()) return null
    if (link.isNotEmpty()) {
        if (html.isNotEmpty()) html.append(HtmlUtils.BR).append(HtmlUtils.BR)
        html.append(HtmlUtils.linkify(link))
    }
    return plainText.toString() to html.toString()
}

// endregion
