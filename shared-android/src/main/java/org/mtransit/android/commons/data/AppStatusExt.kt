package org.mtransit.android.commons.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.AppStatus.LOG_TAG
import org.mtransit.android.commons.provider.common.sharedJson
import kotlinx.serialization.json.put as putK

private const val JSON_APP_INSTALLED = "appInstalled"
private const val JSON_APP_ENABLED = "appEnabled"
private const val JSON_UPDATE_AVAILABLE = "updateAvailable"

@JvmOverloads
fun POIStatus.toAppStatus(jsonString: String? = null) = try {
    this.toAppStatus(jsonString?.let { sharedJson.parseToJsonElement(it).jsonObject })
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON string '$jsonString'", se)
    throw se
}

private fun POIStatus.toAppStatus(jsonObject: JsonObject?) = try {
    AppStatus(
        this,
        jsonObject?.get(JSON_APP_INSTALLED)?.jsonPrimitive?.booleanOrNull ?: false,
        jsonObject?.get(JSON_APP_ENABLED)?.jsonPrimitive?.booleanOrNull ?: true,
        jsonObject?.get(JSON_UPDATE_AVAILABLE)?.jsonPrimitive?.booleanOrNull ?: false,
    )
} catch (se: SerializationException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON object '$jsonObject'", se)
    throw se
}

fun AppStatus.toJson() = try {
    buildJsonObject {
        putK(JSON_APP_INSTALLED, this@toJson.isAppInstalled)
        putK(JSON_APP_ENABLED, this@toJson.isAppEnabled)
        putK(JSON_UPDATE_AVAILABLE, this@toJson.isUpdateAvailable)
    }
} catch (se: SerializationException) {
    MTLogK.w(this, "Error while making JSON object '$this'", se)
    throw se
}
