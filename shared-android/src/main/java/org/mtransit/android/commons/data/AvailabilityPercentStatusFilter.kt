package org.mtransit.android.commons.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.mtransit.android.commons.MTLogK
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

        fun String.toFilter() = try {
            sharedJson.parseToJsonElement(this).jsonObject.toFilter()
        } catch (iae: IllegalArgumentException) {
            MTLogK.w(LOG_TAG, "Error while parsing JSON string '$this'", iae)
            throw iae
        }

        fun JsonObject.toFilter() = try {
            AvailabilityPercentStatusFilter(
                cacheOnly = getCacheOnlyFromJson(),
                cacheValidityInMs = getCacheValidityInMsFromJson(),
                inFocus = getInFocusFromJson(),
                providedEncryptKeysMap = getProvidedEncryptKeysMapFromJson(),
                targetUUID = getTargetUUIDFromJson(),
            )
        } catch (iae: IllegalArgumentException) {
            MTLogK.w(LOG_TAG, "Error while parsing JSON object '$this'", iae)
            throw iae
        }

        private fun StatusProviderContract.Filter.toJson() = try {
            buildJsonObject {
                toJson(this@toJson)
            }
        } catch (iae: IllegalArgumentException) {
            MTLogK.w(LOG_TAG, "Error while making JSON object '$this'", iae)
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
