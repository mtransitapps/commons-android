package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.RouteDirection.LOG_TAG

private const val JSON_ROUTE: String = "route"
private const val JSON_DIRECTION = "trip" // do not change to avoid breaking compat w/ old modules

fun RouteDirection.toJson() = try {
    buildJsonObject {
        put(JSON_ROUTE, route.toJson())
        put(JSON_DIRECTION, direction.toJson())
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(this, "Error while converting to JSON ($this)!", iae)
    throw iae
}

fun JsonObject.toRouteDirection(authority: String) = try {
    RouteDirection(
        getValue(JSON_ROUTE).jsonObject.toRoute(authority),
        getValue(JSON_DIRECTION).jsonObject.toDirection(authority),
    )
} catch (iae: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", iae)
    throw iae
}
