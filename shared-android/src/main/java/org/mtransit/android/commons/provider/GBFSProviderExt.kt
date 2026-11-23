package org.mtransit.android.commons.provider

import androidx.annotation.VisibleForTesting
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.provider.GBFSProvider.LOG_TAG

private val gbfsJson = Json {
    allowTrailingComma = true // do not fail on trailing comma
    coerceInputValues = true // Falls back to default Kotlin values if org.json outputs null or mismatched types
    ignoreUnknownKeys = true // new keys might be added/updated between main app and agency module app releases
    isLenient = true // allows unquoted strings or malformed formatting (if legacy org.json writers were used)
}

// region station information

private const val JSON_DATA = "data"
private const val JSON_STATIONS = "stations"
private const val JSON_STATION_ID = "station_id"
private const val JSON_NAME = "name"
private const val JSON_SHORT_NAME = "short_name"
private const val JSON_LAT = "lat"
private const val JSON_LON = "lon"
private const val JSON_CAPACITY = "capacity"

internal fun String.toJStationInfoList() = try {
    gbfsJson.parseToJsonElement(this).jsonObject.toJStationInfoList()
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", se)
    throw se
}

internal fun JsonObject.toJStationInfoList() = try {
    get(JSON_DATA)?.jsonObject?.get(JSON_STATIONS)?.jsonArray?.map { jStation ->
        jStation.jsonObject.toJStationInfo()
    }.orEmpty()
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", se)
    throw se
}

internal fun JsonObject.toJStationInfo() = GBFSProvider.JStationInformation.JData.JStation(
    this[JSON_STATION_ID]?.jsonPrimitive?.contentOrNull,
    this[JSON_NAME]?.jsonPrimitive?.contentOrNull,
    this[JSON_SHORT_NAME]?.jsonPrimitive?.contentOrNull,
    this[JSON_LAT]?.jsonPrimitive?.doubleOrNull,
    this[JSON_LON]?.jsonPrimitive?.doubleOrNull,
    this[JSON_CAPACITY]?.jsonPrimitive?.intOrNull,
)

// endregion

// region station information

private const val JSON_NUM_BIKES_AVAILABLE = "num_bikes_available"
private const val JSON_NUM_EBIKES_AVAILABLE = "num_ebikes_available"
private const val JSON_NUM_BIKES_AVAILABLE_TYPES = "num_bikes_available_types"
private const val JSON_MECHANICAL = "mechanical"
private const val JSON_EBIKE = "ebike"
private const val JSON_NUM_DOCKS_AVAILABLE = "num_docks_available"
private const val JSON_IS_INSTALLED = "is_installed"
private const val JSON_IS_RENTING = "is_renting"
private const val JSON_IS_RETURNING = "is_returning"
private const val JSON_LAST_REPORTED = "last_reported"

internal fun String.toJStationStatusList() = try {
    gbfsJson.parseToJsonElement(this).jsonObject.toJStationStatusList()
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", se)
    throw se
}

internal fun JsonObject.toJStationStatusList() = try {
    get(JSON_DATA)?.jsonObject?.get(JSON_STATIONS)?.jsonArray?.map { jStation ->
        jStation.jsonObject.toJStationStatus()
    }.orEmpty()
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON '$this'!", se)
    throw se
}

internal fun JsonObject.toJStationStatus() = GBFSProvider.JStationStatus.JData.JStation(
    this[JSON_STATION_ID]?.jsonPrimitive?.contentOrNull,
    this[JSON_NUM_BIKES_AVAILABLE]?.jsonPrimitive?.intOrNull,
    this[JSON_NUM_EBIKES_AVAILABLE]?.jsonPrimitive?.intOrNull,
    this[JSON_NUM_BIKES_AVAILABLE_TYPES]?.jsonObject?.toJNumBikesAvailableTypes(),
    this[JSON_NUM_DOCKS_AVAILABLE]?.jsonPrimitive?.intOrNull,
    this[JSON_IS_INSTALLED]?.jsonPrimitive?.getIntOrBoolean(JSON_IS_INSTALLED),
    this[JSON_IS_RENTING]?.jsonPrimitive?.getIntOrBoolean(JSON_IS_RENTING),
    this[JSON_IS_RETURNING]?.jsonPrimitive?.getIntOrBoolean(JSON_IS_RETURNING),
    this[JSON_LAST_REPORTED]?.jsonPrimitive?.longOrNull,
)

@VisibleForTesting
internal fun JsonPrimitive.getIntOrBoolean(key: String): Boolean? {
    booleanOrNull?.let { return it }
    intOrNull?.let { intValue ->
        if (intValue == 0) return false
        if (intValue == 1) return true
    }
    MTLogK.w(LOG_TAG, "Unexpected int/boolean value '$this' for key '$key'!")
    return null
}

private fun JsonObject.toJNumBikesAvailableTypes() = GBFSProvider.JStationStatus.JData.JStation.JNumBikesAvailableTypes(
    get(JSON_MECHANICAL)?.jsonPrimitive?.intOrNull,
    get(JSON_EBIKE)?.jsonPrimitive?.intOrNull,
)

// endregion
