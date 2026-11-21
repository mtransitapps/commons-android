package org.mtransit.android.commons.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.Route.LOG_TAG
import org.mtransit.commons.GTFSCommons
import kotlinx.serialization.json.put as putK

private const val JSON_AUTHORITY = "authority"
private const val JSON_ID = "id"
private const val JSON_SHORT_NAME = "shortName"
private const val JSON_LONG_NAME = "longName"
private const val JSON_COLOR = "color"
private const val JSON_ORIGINAL_ID_HASH = "o_id_hash"
private const val JSON_TYPE = "type"

fun Route.toJson() = try {
    buildJsonObject {
        putK(JSON_AUTHORITY, this@toJson.authority)
        putK(JSON_ID, this@toJson.id)
        putK(JSON_SHORT_NAME, this@toJson.shortName)
        putK(JSON_LONG_NAME, this@toJson.longName)
        putK(JSON_COLOR, this@toJson.color)
        putK(JSON_ORIGINAL_ID_HASH, this@toJson.originalIdHash)
        putK(JSON_TYPE, this@toJson.type)
    }
} catch (se: SerializationException) {
    MTLogK.w(this, "Error while converting to JSON ($this)!", se)
    throw se
}

@Throws(SerializationException::class)
fun JsonObject.toRoute(authority: String) = try {
    Route(
        this[JSON_AUTHORITY]?.jsonPrimitive?.contentOrNull ?: authority,
        getValue(JSON_ID).jsonPrimitive.long,
        getValue(JSON_SHORT_NAME).jsonPrimitive.content,
        getValue(JSON_LONG_NAME).jsonPrimitive.content,
        getValue(JSON_COLOR).jsonPrimitive.content,
        this[JSON_ORIGINAL_ID_HASH]?.jsonPrimitive?.intOrNull ?: GTFSCommons.DEFAULT_ID_HASH,
        this[JSON_TYPE]?.jsonPrimitive?.intOrNull ?: GTFSCommons.DEFAULT_ROUTE_TYPE,
    )
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", se)
    throw se
}
