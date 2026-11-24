package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.Stop.LOG_TAG
import org.mtransit.commons.FeatureFlags.F_EXPORT_STOP_TIMEZONE_ID
import org.mtransit.commons.GTFSCommons
import org.mtransit.commons.jsonPrimitiveOrNull

private const val JSON_ID = "id"
private const val JSON_CODE = "code"
private const val JSON_NAME = "name"
private const val JSON_LAT = "lat"
private const val JSON_LNG = "lng"
private const val JSON_ACCESSIBLE = "a11y"
private const val JSON_ORIGINAL_ID_HASH = "o_id_hash"
private const val JSON_TIMEZONE_ID = "tz"

fun Stop.toJson() = try {
    buildJsonObject {
        put(JSON_ID, this@toJson.id)
        put(JSON_CODE, this@toJson.code)
        put(JSON_NAME, this@toJson.name)
        put(JSON_LAT, this@toJson.lat)
        put(JSON_LNG, this@toJson.lng)
        put(JSON_ACCESSIBLE, this@toJson.accessible)
        put(JSON_ORIGINAL_ID_HASH, this@toJson.originalIdHash)
        if (F_EXPORT_STOP_TIMEZONE_ID) {
            this@toJson.timeZoneId?.let { put(JSON_TIMEZONE_ID, it) }
        }
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(this, "Error while converting to JSON ($this)!", iae)
    throw iae
}

@Throws(IllegalArgumentException::class)
fun JsonObject.toStop() = try {
    Stop(
        getValue(JSON_ID).jsonPrimitive.int,
        getValue(JSON_CODE).jsonPrimitive.content,
        getValue(JSON_NAME).jsonPrimitive.content,
        getValue(JSON_LAT).jsonPrimitive.double,
        getValue(JSON_LNG).jsonPrimitive.double,
        this[JSON_ACCESSIBLE]?.jsonPrimitiveOrNull?.intOrNull ?: Accessibility.DEFAULT,
        this[JSON_ORIGINAL_ID_HASH]?.jsonPrimitiveOrNull?.intOrNull ?: GTFSCommons.DEFAULT_ID_HASH,
        this[JSON_TIMEZONE_ID]?.jsonPrimitiveOrNull?.contentOrNull,
    )
} catch (iae: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while converting from JSON ($this)!", iae)
    throw iae
}
