package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.AppStatus.LOG_TAG
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull
import kotlinx.serialization.json.put as putK

private const val JSON_APP_INSTALLED = "appInstalled"
private const val JSON_APP_ENABLED = "appEnabled"
private const val JSON_UPDATE_AVAILABLE = "updateAvailable"

@JvmOverloads
fun POIStatus.toAppStatus(jsonString: String? = null) = try {
    this.toAppStatus(jsonString?.let { sharedJson.parseToJsonElement(it).jsonObjectOrNull })
} catch (iae: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON string '$jsonString'", iae)
    throw iae
}

private fun POIStatus.toAppStatus(jsonObject: JsonObject?) = try {
    AppStatus(
        this,
        jsonObject?.get(JSON_APP_INSTALLED)?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
        jsonObject?.get(JSON_APP_ENABLED)?.jsonPrimitiveOrNull?.booleanOrNull ?: true,
        jsonObject?.get(JSON_UPDATE_AVAILABLE)?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
    )
} catch (iae: IllegalArgumentException) {
    MTLogK.w(LOG_TAG, "Error while parsing JSON object '$jsonObject'", iae)
    throw iae
}

fun AppStatus.toJson() = try {
    buildJsonObject {
        putK(JSON_APP_INSTALLED, this@toJson.isAppInstalled)
        putK(JSON_APP_ENABLED, this@toJson.isAppEnabled)
        putK(JSON_UPDATE_AVAILABLE, this@toJson.isUpdateAvailable)
    }
} catch (iae: IllegalArgumentException) {
    MTLogK.w(this, "Error while making JSON object '$this'", iae)
    throw iae
}
