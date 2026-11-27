package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.mtransit.android.commons.data.RouteDirection.LOG_TAG
import org.mtransit.android.commons.jsonRunCatching

private const val JSON_ROUTE: String = "route"
private const val JSON_DIRECTION = "trip" // do not change to avoid breaking compat w/ old modules

fun RouteDirection.toJson() = jsonRunCatching {
    buildJsonObject {
        put(JSON_ROUTE, route.toJson())
        put(JSON_DIRECTION, direction.toJson())
    }
}.getOrThrow()

fun JsonObject.toRouteDirection(authority: String) = jsonRunCatching(LOG_TAG) {
    RouteDirection(
        getValue(JSON_ROUTE).jsonObject.toRoute(authority),
        getValue(JSON_DIRECTION).jsonObject.toDirection(authority),
    )
}.getOrThrow()
