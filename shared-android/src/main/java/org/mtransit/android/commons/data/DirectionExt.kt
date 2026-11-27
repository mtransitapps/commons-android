package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.mtransit.android.commons.data.Direction.HeadSignType
import org.mtransit.android.commons.data.Direction.LOG_TAG
import org.mtransit.android.commons.jsonRunCatching
import kotlinx.serialization.json.put as putK

fun makeDirection(
    authority: String,
    id: Long,
    @HeadSignType headsignType: Int,
    headsignValue: String,
    routeId: Long,
) = Direction(
    authority,
    id,
    headsignType,
    headsignValue,
    routeId,
)

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
    makeDirection(
        authority = authority,
        id = getValue(JSON_ID).jsonPrimitive.long,
        headsignType = getValue(JSON_HEADSIGN_TYPE).jsonPrimitive.int,
        headsignValue = getValue(JSON_HEADSIGN_VALUE).jsonPrimitive.content,
        routeId = getValue(JSON_ROUTE_ID).jsonPrimitive.long,
    )
}.getOrThrow()
