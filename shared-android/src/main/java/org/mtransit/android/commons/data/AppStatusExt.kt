package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.mtransit.android.commons.data.AppStatus.LOG_TAG
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private const val JSON_APP_INSTALLED = "appInstalled"
private const val JSON_APP_ENABLED = "appEnabled"
private const val JSON_UPDATE_AVAILABLE = "updateAvailable"

@JvmOverloads
fun String?.toAppStatusOrDefault(
    status: POIStatus,
    getDefault: () -> AppStatus = { AppStatus(status, false, true, false) },
) = this?.takeIf { it.isNotBlank() }?.jsonRunCatching(LOG_TAG) {
    sharedJson.parseToJsonElement(this).jsonObject.toAppStatus(status)
}?.getOrDefault(getDefault()) ?: getDefault()

private fun JsonObject.toAppStatus(status: POIStatus) = AppStatus(
    status,
    this[JSON_APP_INSTALLED]?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
    this[JSON_APP_ENABLED]?.jsonPrimitiveOrNull?.booleanOrNull ?: true,
    this[JSON_UPDATE_AVAILABLE]?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
)

fun AppStatus.toJson() = jsonRunCatching {
    buildJsonObject {
        toJsonCommon(this@toJson)
        putK(JSON_APP_INSTALLED, this@toJson.isAppInstalled)
        putK(JSON_APP_ENABLED, this@toJson.isAppEnabled)
        putK(JSON_UPDATE_AVAILABLE, this@toJson.isUpdateAvailable)
    }
}.getOrThrow()

fun AppStatus.toJsonString() = sharedJson.encodeToString(this.toJson())
