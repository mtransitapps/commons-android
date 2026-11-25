package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.mtransit.android.commons.data.Direction.LOG_TAG
import org.mtransit.android.commons.jsonRunCatching
import kotlinx.serialization.json.put as putK

private const val JSON_ID = "id"
private const val JSON_HEADSIGN_TYPE = "headsignType"
private const val JSON_HEADSIGN_VALUE = "headsignValue"
private const val JSON_ROUTE_ID = "routeId"

fun Direction.toJson() = jsonRunCatching {
    buildJsonObject {
        putK(JSON_ID, this@toJson.id)
        putK(JSON_HEADSIGN_TYPE, this@toJson.headsignType)
        putK(JSON_HEADSIGN_VALUE, this@toJson.headsignValue)
        putK(JSON_ROUTE_ID, this@toJson.routeId)
    }
}.getOrThrow()

fun JsonObject.toDirection(authority: String) = jsonRunCatching(LOG_TAG) {
    Direction(
        authority,
        getValue(JSON_ID).jsonPrimitive.long,
        getValue(JSON_HEADSIGN_TYPE).jsonPrimitive.int,
        getValue(JSON_HEADSIGN_VALUE).jsonPrimitive.content,
        getValue(JSON_ROUTE_ID).jsonPrimitive.long,
    )
}.getOrThrow()
