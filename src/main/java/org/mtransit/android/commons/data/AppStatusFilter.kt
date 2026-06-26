package org.mtransit.android.commons.data

import org.json.JSONException
import org.json.JSONObject
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.status.StatusProviderContract

data class AppStatusFilter(
    override val cacheOnly: Boolean? = null,
    override val cacheValidityInMs: Long? = null,
    override val inFocus: Boolean? = null,
    override val providedEncryptKeysMap: Map<String, String>? = null,
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
        fun fromJSONString(jsonString: String?): StatusProviderContract.Filter? {
            try {
                return jsonString?.let { fromJSON(JSONObject(it)) }
            } catch (jsone: JSONException) {
                MTLog.w(LOG_TAG, jsone, "Error while parsing JSON string '$jsonString'")
                return null
            }
        }

        private const val JSON_PKG = "pkg"

        fun fromJSON(json: JSONObject): StatusProviderContract.Filter? {
            try {
                return AppStatusFilter(
                    cacheOnly = getCacheOnlyFromJSON(json),
                    cacheValidityInMs = getCacheValidityInMsFromJSON(json),
                    inFocus = getInFocusFromJSON(json),
                    providedEncryptKeysMap = getProvidedEncryptKeysMapFromJSON(json),
                    targetUUID = getTargetUUIDFromJSON(json),
                    pkg = json.getString(JSON_PKG)
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
                    if (statusFilter is AppStatusFilter) {
                        put(JSON_PKG, statusFilter.pkg)
                    }
                }
            } catch (jsone: JSONException) {
                MTLog.w(LOG_TAG, jsone, "Error while making JSON object '%s'", statusFilter)
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
