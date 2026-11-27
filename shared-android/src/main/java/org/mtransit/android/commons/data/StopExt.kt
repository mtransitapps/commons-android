package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mtransit.android.commons.data.Stop.LOG_TAG
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.commons.FeatureFlags.F_EXPORT_STOP_TIMEZONE_ID
import org.mtransit.commons.GTFSCommons
import org.mtransit.commons.jsonPrimitiveOrNull

@Suppress("LongParameterList")
fun makeStop(
    stopId: Int,
    stopCode: String,
    stopName: String,
    stopLat: Double,
    stopLng: Double,
    @Accessibility.AccessibilityType stopAccessibility: Int = Accessibility.DEFAULT,
    stopOriginalIdHash: Int? = GTFSCommons.DEFAULT_ID_HASH,
    stopTimeZoneId: String? = null,
) = Stop(
    stopId,
    stopCode,
    stopName,
    stopLat,
    stopLng,
    stopAccessibility,
    stopOriginalIdHash,
    stopTimeZoneId,
)

private const val JSON_ID = "id"
private const val JSON_CODE = "code"
private const val JSON_NAME = "name"
private const val JSON_LAT = "lat"
private const val JSON_LNG = "lng"
private const val JSON_ACCESSIBLE = "a11y"
private const val JSON_ORIGINAL_ID_HASH = "o_id_hash"
private const val JSON_TIMEZONE_ID = "tz"

fun Stop.toJson() = jsonRunCatching {
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
}.getOrThrow()

fun JsonObject.toStop() = jsonRunCatching(LOG_TAG) {
    makeStop(
        stopId = getValue(JSON_ID).jsonPrimitive.int,
        stopCode = getValue(JSON_CODE).jsonPrimitive.content,
        stopName = getValue(JSON_NAME).jsonPrimitive.content,
        stopLat = getValue(JSON_LAT).jsonPrimitive.double,
        stopLng = getValue(JSON_LNG).jsonPrimitive.double,
        stopAccessibility = this[JSON_ACCESSIBLE]?.jsonPrimitiveOrNull?.intOrNull ?: Accessibility.DEFAULT,
        stopOriginalIdHash = this[JSON_ORIGINAL_ID_HASH]?.jsonPrimitiveOrNull?.intOrNull ?: GTFSCommons.DEFAULT_ID_HASH,
        stopTimeZoneId = this[JSON_TIMEZONE_ID]?.jsonPrimitiveOrNull?.contentOrNull,
    )
}.getOrThrow()
