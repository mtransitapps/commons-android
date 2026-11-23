package org.mtransit.android.commons.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.edit
import kotlin.time.Instant

val POIStatus.readFromSource: Instant? get() = this.readFromSourceAtInMs.takeIf { it > 0 }?.millisToInstant()

fun POIStatus.setReadFromSourceAtInMsKeepMostRecent(readFromSourceAtInMs: Long) {
    if (this.readFromSourceAtInMs < readFromSourceAtInMs) {
        this.readFromSourceAtInMs = readFromSourceAtInMs
    }
}

private const val JSON_SOURCE_LABEL = "sourceLabel"
private const val JSON_NO_DATA = "noData"

fun String?.toJsonObject() = try {
    this?.let { sharedJson.parseToJsonElement(it).jsonObject }
} catch (se: SerializationException) {
    MTLogK.w(POIStatus.LOG_TAG, "Error while parsing JSON string '$this'", se)
    throw se
}

fun JsonObject?.getSourceLabelFromJson() = this?.get(JSON_SOURCE_LABEL)?.jsonPrimitive?.contentOrNull
fun JsonObject?.getNoDataFromJson() = this?.get(JSON_NO_DATA)?.jsonPrimitive?.booleanOrNull
fun JsonObject?.getNoDataFromJsonOrDefault() = getNoDataFromJson() ?: false

fun JsonObject.toJsonCommon(poiStatus: POIStatus) = edit { toJsonCommon(poiStatus) }

fun JsonObjectBuilder.toJsonCommon(poiStatus: POIStatus) = poiStatus.apply {
    put(JSON_SOURCE_LABEL, sourceLabel)
    put(JSON_NO_DATA, isNoData)
}

fun JsonObject.toJsonString() = sharedJson.encodeToString(this)
