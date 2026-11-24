package org.mtransit.android.commons.provider

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.data.Accessibility
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.provider.OneBusAwayProvider.LOG_TAG
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonArrayOrNull
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull

fun String?.toStatuses(
    localTimeZoneId: String,
    makeSchedule: () -> Schedule,
    cleanDirectionHeadsign: (tripHeadsign: String, forRds: Boolean) -> String,
    isSameRoute: (jRouteId: String, jRouteShortName: String) -> Boolean,
    isSameDirection: (tripHeadsign: String) -> Boolean,
) = try {
    this?.let {
        sharedJson.parseToJsonElement(it).jsonObject.toStatuses(
            localTimeZoneId = localTimeZoneId,
            makeSchedule = makeSchedule,
            cleanDirectionHeadsign = cleanDirectionHeadsign,
            isSameRoute = isSameRoute,
            isSameDirection = isSameDirection
        )
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", iae)
    null
}

private const val JSON_DATA = "data"
private const val JSON_ENTRY = "entry"
private const val JSON_ARRIVALS_AND_DEPARTURES = "arrivalsAndDepartures"

private fun JsonObject.toStatuses(
    localTimeZoneId: String,
    makeSchedule: () -> Schedule,
    cleanDirectionHeadsign: (tripHeadsign: String, forRds: Boolean) -> String,
    isSameRoute: (jRouteId: String, jRouteShortName: String) -> Boolean,
    isSameDirection: (tripHeadsign: String) -> Boolean,
): List<POIStatus>? = try {
    buildList {
        this@toStatuses[JSON_DATA]?.jsonObjectOrNull
            ?.get(JSON_ENTRY)?.jsonObjectOrNull
            ?.get(JSON_ARRIVALS_AND_DEPARTURES)?.jsonArrayOrNull
            ?.mapNotNull {
                it.jsonObjectOrNull?.toTimestamp(localTimeZoneId, cleanDirectionHeadsign, isSameRoute, isSameDirection)
            }?.takeIf { it.isNotEmpty() }
            ?.let { timestamps ->
                makeSchedule().apply {
                    setTimestampsAndSort(timestamps)
                }.let { newStatus ->
                    add(newStatus)
                }
            }
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", iae)
    null
}

private const val JSON_PREDICTED = "predicted"
private const val JSON_ROUTE_ID = "routeId"
private const val JSON_ROUTE_SHORT_NAME = "routeShortName"
private const val JSON_TRIP_HEADSIGN = "tripHeadsign"
private const val JSON_DEPARTURE_ENABLED = "departureEnabled"

private fun JsonObject.toTimestamp(
    localTimeZoneId: String,
    cleanDirectionHeadsign: (tripHeadsign: String, forRds: Boolean) -> String,
    isSameRoute: (jRouteId: String, jRouteShortName: String) -> Boolean,
    isSameDirection: (tripHeadsign: String) -> Boolean,
) =
    (this.getTimestamp() to this@toTimestamp.getValue(JSON_TRIP_HEADSIGN).jsonPrimitive.content)
        .takeIf { (timestamp, tripHeadsign) ->
            timestamp > 0L
                &&
                isSameDirection(tripHeadsign)
                &&
                (this[JSON_PREDICTED]?.jsonPrimitiveOrNull?.booleanOrNull ?: false)
                &&
                isSameRoute(
                    this.getValue(JSON_ROUTE_ID).jsonPrimitive.content,
                    this.getValue(JSON_ROUTE_SHORT_NAME).jsonPrimitive.content
                )
        }
        ?.let { (timestamp, tripHeadsign) ->
            Schedule.Timestamp(
                TimeUtils.timeToTheTensSecondsMillis(timestamp),
                localTimeZoneId,
            ).apply {
                try {
                    if (tripHeadsign.isNotEmpty()) {
                        var tripHeadsign = tripHeadsign
                        tripHeadsign = cleanDirectionHeadsign(tripHeadsign, false)
                        tripHeadsign = cleanDirectionHeadsign(tripHeadsign, true)
                        val departureEnabled = this@toTimestamp[JSON_DEPARTURE_ENABLED]?.jsonPrimitiveOrNull?.booleanOrNull ?: true
                        if (!departureEnabled) {
                            setHeadsign(Direction.HEADSIGN_TYPE_NO_PICKUP, null)
                        } else {
                            setHeadsign(Direction.HEADSIGN_TYPE_STRING, tripHeadsign)
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    MTLogK.w(this, "Error while reading trip headsign in '$this'!", e)
                }
                realTime = isRealTime
                accessible = Accessibility.UNKNOWN
            }
        }

private const val JSON_PREDICTED_DEPARTURE_TIME = "predictedDepartureTime"
private const val JSON_PREDICTED_ARRIVAL_TIME = "predictedArrivalTime"
private const val JSON_SCHEDULED_DEPARTURE_TIME = "scheduledDepartureTime"
private const val JSON_SCHEDULED_ARRIVAL_TIME = "scheduledArrivalTime"

private val JSON_TIME_CANDIDATES = listOf(
    JSON_PREDICTED_DEPARTURE_TIME,
    JSON_PREDICTED_ARRIVAL_TIME,
    JSON_SCHEDULED_DEPARTURE_TIME,
    JSON_SCHEDULED_ARRIVAL_TIME,
)

private fun JsonObject.getTimestamp() = try {
    JSON_TIME_CANDIDATES.firstNotNullOfOrNull { key ->
        this[key]?.jsonPrimitiveOrNull?.longOrNull?.takeIf { it > 0L }
    } ?: 0L
} catch (e: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while reading timestamp!", e)
    0L
}
