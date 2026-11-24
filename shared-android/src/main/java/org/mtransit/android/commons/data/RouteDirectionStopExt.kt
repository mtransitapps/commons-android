package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.RouteDirectionStop.LOG_TAG
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private const val JSON_ROUTE = "route"
private const val JSON_DIRECTION = "trip" // do not change to avoid breaking compat w/ old modules
private const val JSON_STOP = "stop"
private const val JSON_NO_PICKUP = "decentOnly"
private const val JSON_ALWAYS_LAST_TRIP_STOP = "lastStop"

fun RouteDirectionStop.toJsonK() = try {
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
} catch (iae: IllegalArgumentException) {
    MTLogK.w(this, "Error while converting to JSON ($this)!", iae)
    throw iae
}

fun JsonObject.toRouteDirectionStop() = try {
    getAuthorityFromJson().let { authority ->
        RouteDirectionStop(
            getDSTypeIdFromJson(),
            getValue(JSON_ROUTE).jsonObject.toRoute(authority),
            getValue(JSON_DIRECTION).jsonObject.toDirection(authority),
            getValue(JSON_STOP).jsonObject.toStop(),
            getValue(JSON_NO_PICKUP).jsonPrimitive.boolean,
            get(JSON_ALWAYS_LAST_TRIP_STOP)?.jsonPrimitiveOrNull?.booleanOrNull,
        ).apply {
            applyTo(this)
        }
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while converting from JSON ($this)!", iae)
    throw iae
}
