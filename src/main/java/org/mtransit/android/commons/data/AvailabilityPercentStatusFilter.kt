package org.mtransit.android.commons.data

import org.json.JSONException
import org.json.JSONObject
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.status.StatusProviderContract

data class AvailabilityPercentStatusFilter(
    override val cacheOnly: Boolean? = null,
    override val cacheValidityInMs: Long? = null,
    override val inFocus: Boolean? = null,
    override val providedEncryptKeysMap: Map<String, String>? = null,
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
        fun fromJSONString(jsonString: String?): StatusProviderContract.Filter? {
            try {
                return jsonString?.let { fromJSON(JSONObject(it)) }
            } catch (jsone: JSONException) {
                MTLog.w(LOG_TAG, jsone, "Error while parsing JSON string '$jsonString'")
                return null
            }
        }

        fun fromJSON(json: JSONObject): StatusProviderContract.Filter? {
            try {
                return AvailabilityPercentStatusFilter(
                    cacheOnly = getCacheOnlyFromJSON(json),
                    cacheValidityInMs = getCacheValidityInMsFromJSON(json),
                    inFocus = getInFocusFromJSON(json),
                    providedEncryptKeysMap = getProvidedEncryptKeysMapFromJSON(json),
                    targetUUID = getTargetUUIDFromJSON(json),
                )
            } catch (jsone: JSONException) {
                MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '$json'")
                return null
            }
        }

        private fun toJSONString(statusFilter: StatusProviderContract.Filter) = toJSON(statusFilter)?.toString()

        private fun toJSON(statusFilter: StatusProviderContract.Filter): JSONObject? {
            try {
                return JSONObject().apply {
                    toJSON(statusFilter, this)
                }
            } catch (jsone: JSONException) {
                MTLog.w(LOG_TAG, jsone, "Error while making JSON object '$statusFilter'")
                return null
            }
        }
    }

    override fun getLogTag() = LOG_TAG

    override fun copyWith(providedEncryptKeysMap: Map<String, String>?) = this.copy(providedEncryptKeysMap = providedEncryptKeysMap)

    override fun fromJSONStringStatic(jsonString: String?) = fromJSONString(jsonString)

    override fun toJSONString() = toJSONString(this)

    override fun toJSONStringStatic(statusFilter: StatusProviderContract.Filter) = toJSONString(statusFilter)
}
