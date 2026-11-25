package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import org.mtransit.android.commons.jsonRunCatching
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

        fun String.toFilter() = jsonRunCatching(LOG_TAG) {
            sharedJson.parseToJsonElement(this).jsonObjectOrNull?.toFilter()
        }.getOrThrow()

        private const val JSON_PKG = "pkg"

        fun JsonObject.toFilter() = jsonRunCatching(LOG_TAG) {
            AppStatusFilter(
                cacheOnly = getCacheOnlyFromJson(),
                cacheValidityInMs = getCacheValidityInMsFromJson(),
                inFocus = getInFocusFromJson(),
                providedEncryptKeysMap = getProvidedEncryptKeysMapFromJson(),
                targetUUID = getTargetUUIDFromJson(),
                pkg = this[JSON_PKG]?.jsonPrimitiveOrNull?.contentOrNull ?: throw IllegalArgumentException("Missing key '$JSON_PKG'!"),
            )
        }.getOrThrow()

        private fun StatusProviderContract.Filter.toJson() = jsonRunCatching {
            buildJsonObject {
                toJson(this@toJson)
                if (this@toJson is AppStatusFilter) {
                    put(JSON_PKG, this@toJson.pkg)
                }
            }
        }.getOrThrow()
    }

    override fun getLogTag() = LOG_TAG

    override fun copyWithProvidedEncryptKeysMap(providedEncryptKeysMap: Secret<Map<String, String>>?) =
        this.copy(providedEncryptKeysMap = providedEncryptKeysMap)

    override fun copyWithCacheOnly(cacheOnly: Boolean) =
        this.copy(cacheOnly = cacheOnly)

    override fun toJsonString() = sharedJson.encodeToString(this.toJson())
}
