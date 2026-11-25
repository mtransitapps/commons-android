package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.mtransit.android.commons.data.Route.LOG_TAG
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.commons.GTFSCommons
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private const val JSON_AUTHORITY = "authority"
private const val JSON_ID = "id"
private const val JSON_SHORT_NAME = "shortName"
private const val JSON_LONG_NAME = "longName"
private const val JSON_COLOR = "color"
private const val JSON_ORIGINAL_ID_HASH = "o_id_hash"
private const val JSON_TYPE = "type"

fun Route.toJson() = jsonRunCatching {
    buildJsonObject {
        putK(JSON_AUTHORITY, this@toJson.authority)
        putK(JSON_ID, this@toJson.id)
        putK(JSON_SHORT_NAME, this@toJson.shortName)
        putK(JSON_LONG_NAME, this@toJson.longName)
        putK(JSON_COLOR, this@toJson.color)
        putK(JSON_ORIGINAL_ID_HASH, this@toJson.originalIdHash)
        putK(JSON_TYPE, this@toJson.type)
    }
}.getOrThrow()

fun JsonObject.toRoute(authority: String) = jsonRunCatching(LOG_TAG) {
    Route(
        this[JSON_AUTHORITY]?.jsonPrimitiveOrNull?.contentOrNull ?: authority,
        getValue(JSON_ID).jsonPrimitive.long,
        getValue(JSON_SHORT_NAME).jsonPrimitive.content,
        getValue(JSON_LONG_NAME).jsonPrimitive.content,
        getValue(JSON_COLOR).jsonPrimitive.content,
        this[JSON_ORIGINAL_ID_HASH]?.jsonPrimitiveOrNull?.intOrNull ?: GTFSCommons.DEFAULT_ID_HASH,
        this[JSON_TYPE]?.jsonPrimitiveOrNull?.intOrNull ?: GTFSCommons.DEFAULT_ROUTE_TYPE,
    )
}.getOrThrow()
