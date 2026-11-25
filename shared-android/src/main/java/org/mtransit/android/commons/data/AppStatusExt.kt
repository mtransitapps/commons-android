package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import org.mtransit.android.commons.data.AppStatus.LOG_TAG
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private const val JSON_APP_INSTALLED = "appInstalled"
private const val JSON_APP_ENABLED = "appEnabled"
private const val JSON_UPDATE_AVAILABLE = "updateAvailable"

@JvmOverloads
fun POIStatus.toAppStatus(jsonString: String? = null) = jsonRunCatching(LOG_TAG) {
    this.toAppStatus(jsonString?.let { sharedJson.parseToJsonElement(it).jsonObjectOrNull })
}.getOrThrow()

private fun POIStatus.toAppStatus(jsonObject: JsonObject?) = jsonRunCatching(LOG_TAG) {
    AppStatus(
        this,
        jsonObject?.get(JSON_APP_INSTALLED)?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
        jsonObject?.get(JSON_APP_ENABLED)?.jsonPrimitiveOrNull?.booleanOrNull ?: true,
        jsonObject?.get(JSON_UPDATE_AVAILABLE)?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
    )
}.getOrThrow()

fun AppStatus.toJson() = jsonRunCatching {
    buildJsonObject {
        toJsonCommon(this@toJson)
        putK(JSON_APP_INSTALLED, this@toJson.isAppInstalled)
        putK(JSON_APP_ENABLED, this@toJson.isAppEnabled)
        putK(JSON_UPDATE_AVAILABLE, this@toJson.isUpdateAvailable)
    }
}.getOrThrow()

fun AppStatus.toJsonString() = sharedJson.encodeToString(this.toJson())
