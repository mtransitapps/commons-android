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

@JvmOverloads
fun makeRoute(
    authority: String,
    id: Long,
    shortName: String,
    longName: String,
    color: String,
    originalIdHash: Int? = GTFSCommons.DEFAULT_ID_HASH,
    type: Int? = GTFSCommons.DEFAULT_ROUTE_TYPE,
) = Route(
    authority,
    id,
    shortName,
    longName,
    color,
    originalIdHash,
    type,
)

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
    makeRoute(
        authority = this[JSON_AUTHORITY]?.jsonPrimitiveOrNull?.contentOrNull ?: authority,
        id = getValue(JSON_ID).jsonPrimitive.long,
        shortName = getValue(JSON_SHORT_NAME).jsonPrimitive.content,
        longName = getValue(JSON_LONG_NAME).jsonPrimitive.content,
        color = getValue(JSON_COLOR).jsonPrimitive.content,
        originalIdHash = this[JSON_ORIGINAL_ID_HASH]?.jsonPrimitiveOrNull?.intOrNull ?: GTFSCommons.DEFAULT_ID_HASH,
        type = this[JSON_TYPE]?.jsonPrimitiveOrNull?.intOrNull ?: GTFSCommons.DEFAULT_ROUTE_TYPE,
    )
}.getOrThrow()
