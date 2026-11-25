package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.commons.provider.status.StatusProviderContract
import org.mtransit.commons.model.Secret

data class AvailabilityPercentStatusFilter(
    override val cacheOnly: Boolean? = null,
    override val cacheValidityInMs: Long? = null,
    override val inFocus: Boolean? = null,
    override val providedEncryptKeysMap: Secret<Map<String, String>>? = null,
    override val targetUUID: String,
) : StatusProviderContract.Filter(POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT, targetUUID) {

    companion object {
        private val LOG_TAG: String = AvailabilityPercentStatusFilter::class.java.getSimpleName()

        @JvmStatic
        fun from(poi: POI) = from(poi.uuid)

        @JvmStatic
        fun from(targetUUID: String) = AvailabilityPercentStatusFilter(
            targetUUID = targetUUID,
        )

        @JvmStatic
        fun String?.toFilterN() = this?.toFilter()

        fun String.toFilter() = jsonRunCatching(LOG_TAG) {
            sharedJson.parseToJsonElement(this).jsonObject.toFilter()
        }.getOrThrow()

        fun JsonObject.toFilter() = jsonRunCatching(LOG_TAG) {
            AvailabilityPercentStatusFilter(
                cacheOnly = getCacheOnlyFromJson(),
                cacheValidityInMs = getCacheValidityInMsFromJson(),
                inFocus = getInFocusFromJson(),
                providedEncryptKeysMap = getProvidedEncryptKeysMapFromJson(),
                targetUUID = getTargetUUIDFromJson(),
            )
        }.getOrThrow()

        private fun StatusProviderContract.Filter.toJson() = jsonRunCatching {
            buildJsonObject {
                toJson(this@toJson)
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
