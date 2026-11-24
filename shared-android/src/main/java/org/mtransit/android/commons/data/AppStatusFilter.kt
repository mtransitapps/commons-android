package org.mtransit.android.commons.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.commons.provider.status.StatusProviderContract
import org.mtransit.commons.jsonObjectOrNull
import org.mtransit.commons.jsonPrimitiveOrNull
import org.mtransit.commons.model.Secret

data class AppStatusFilter(
    override val cacheOnly: Boolean? = null,
    override val cacheValidityInMs: Long? = null,
    override val inFocus: Boolean? = null,
    override val providedEncryptKeysMap: Secret<Map<String, String>>? = null,
    override val targetUUID: String,
    val pkg: String
) : StatusProviderContract.Filter(POI.ITEM_STATUS_TYPE_APP, targetUUID) {

    companion object {
        private val LOG_TAG: String = AppStatusFilter::class.java.getSimpleName()

        @JvmStatic
        fun from(targetUUID: String, pkg: String) = AppStatusFilter(
            targetUUID = targetUUID,
            pkg = pkg,
        )

        @JvmStatic
        fun String?.toFilterN() = this?.toFilter()

        fun String.toFilter() = try {
            sharedJson.parseToJsonElement(this).jsonObjectOrNull?.toFilter()
        } catch (iae: IllegalArgumentException) {
            MTLogK.w(LOG_TAG, "Error while parsing JSON string '$this'", iae)
            throw iae
        }

        private const val JSON_PKG = "pkg"

        fun JsonObject.toFilter() = try {
            AppStatusFilter(
                cacheOnly = getCacheOnlyFromJson(),
                cacheValidityInMs = getCacheValidityInMsFromJson(),
                inFocus = getInFocusFromJson(),
                providedEncryptKeysMap = getProvidedEncryptKeysMapFromJson(),
                targetUUID = getTargetUUIDFromJson(),
                pkg = this[JSON_PKG]?.jsonPrimitiveOrNull?.contentOrNull ?: throw SerializationException("Missing key '$JSON_PKG'!"),
            )
        } catch (iae: IllegalArgumentException) {
            MTLogK.w(LOG_TAG, "Error while parsing JSON object '$this'", iae)
            throw iae
        }

        private fun StatusProviderContract.Filter.toJson() = try {
            buildJsonObject {
                toJson(this@toJson)
                if (this@toJson is AppStatusFilter) {
                    put(JSON_PKG, this@toJson.pkg)
                }
            }
        } catch (iae: IllegalArgumentException) {
            MTLogK.w(this, "Error while making JSON object '$this'", iae)
            throw iae
        }
    }

    override fun getLogTag() = LOG_TAG

    override fun copyWithProvidedEncryptKeysMap(providedEncryptKeysMap: Secret<Map<String, String>>?) =
        this.copy(providedEncryptKeysMap = providedEncryptKeysMap)

    override fun copyWithCacheOnly(cacheOnly: Boolean) =
        this.copy(cacheOnly = cacheOnly)

    override fun toJsonString() = sharedJson.encodeToString(this.toJson())
}
