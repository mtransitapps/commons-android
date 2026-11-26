package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.mtransit.android.commons.data.POIStatus.LOG_TAG
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlin.time.Instant

val POIStatus.readFromSource: Instant? get() = this.readFromSourceAtInMs.takeIf { it > 0 }?.millisToInstant()

fun POIStatus.setReadFromSourceAtInMsKeepMostRecent(readFromSourceAtInMs: Long) {
    if (this.readFromSourceAtInMs < readFromSourceAtInMs) {
        this.readFromSourceAtInMs = readFromSourceAtInMs
    }
}

private const val JSON_SOURCE_LABEL = "sourceLabel"
private const val JSON_NO_DATA = "noData"

fun String?.toJsonObject() = this?.takeIf { it.isNotBlank() }?.jsonRunCatching(LOG_TAG) {
    sharedJson.parseToJsonElement(this).jsonObject
}?.getOrNull()

fun JsonObject?.getSourceLabelFromJson() = this?.get(JSON_SOURCE_LABEL)?.jsonPrimitiveOrNull?.contentOrNull
fun JsonObject?.getNoDataFromJson() = this?.get(JSON_NO_DATA)?.jsonPrimitiveOrNull?.booleanOrNull
fun JsonObject?.getNoDataFromJsonOrDefault() = getNoDataFromJson() ?: false

fun JsonObjectBuilder.toJsonCommon(poiStatus: POIStatus) = poiStatus.apply {
    put(JSON_SOURCE_LABEL, sourceLabel)
    put(JSON_NO_DATA, isNoData)
}
