package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mtransit.android.commons.data.RouteDirectionStop.LOG_TAG
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

@JvmOverloads
fun makeRouteDirectionStop(
    @DataSourceTypeId.DataSourceType dataSourceTypeId: Int,
    route: Route,
    direction: Direction,
    stop: Stop,
    noPickup: Boolean = false,
    alwaysLastTripStop: Boolean? = null,
    score: Int? = null,
    accessible: Int = stop.accessible,
) = RouteDirectionStop(
    dataSourceTypeId,
    route,
    direction,
    stop,
    noPickup,
    alwaysLastTripStop,
).apply {
    optDefaultPOIInit(
        score = score,
        accessible = accessible,
    )
}

private const val JSON_ROUTE = "route"
private const val JSON_DIRECTION = "trip" // do not change to avoid breaking compat w/ old modules
private const val JSON_STOP = "stop"
private const val JSON_NO_PICKUP = "decentOnly"
private const val JSON_ALWAYS_LAST_TRIP_STOP = "lastStop"

fun RouteDirectionStop.toJsonK() = jsonRunCatching {
    buildJsonObject {
        put(JSON_ROUTE, route.toJson())
        put(JSON_DIRECTION, direction.toJson())
        put(JSON_STOP, stop.toJson())
        putK(JSON_NO_PICKUP, isNoPickup)
        if (FeatureFlags.F_EXPORT_DIRECTION_STOP_LAST) {
            putK(JSON_ALWAYS_LAST_TRIP_STOP, isAlwaysLastTripStop())
        }
        toJsonCommon(this@toJsonK)
    }
}.getOrThrow()

fun JsonObject.toRouteDirectionStop() = jsonRunCatching(LOG_TAG) {
    getAuthorityFromJson().let { authority ->
        makeRouteDirectionStop(
            dataSourceTypeId = getDSTypeIdFromJson(),
            route = getValue(JSON_ROUTE).jsonObject.toRoute(authority),
            direction = getValue(JSON_DIRECTION).jsonObject.toDirection(authority),
            stop = getValue(JSON_STOP).jsonObject.toStop(),
            noPickup = getValue(JSON_NO_PICKUP).jsonPrimitive.boolean,
            alwaysLastTripStop = get(JSON_ALWAYS_LAST_TRIP_STOP)?.jsonPrimitiveOrNull?.booleanOrNull,
            score = getScoreFromJson(),
        )
    }
}.getOrThrow()
