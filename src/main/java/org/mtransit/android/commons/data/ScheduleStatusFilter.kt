package org.mtransit.android.commons.data

import org.json.JSONException
import org.json.JSONObject
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.optBoolean
import org.mtransit.android.commons.optInt
import org.mtransit.android.commons.optLong
import org.mtransit.android.commons.provider.status.StatusProviderContract
import org.mtransit.commons.model.Secret
import java.util.concurrent.TimeUnit

data class ScheduleStatusFilter(
    override val cacheOnly: Boolean? = null,
    override val cacheValidityInMs: Long? = null,
    override val inFocus: Boolean? = null,
    override val providedEncryptKeysMap: Secret<Map<String, String>>? = null,
    val routeDirectionStop: RouteDirectionStop,
    private val lookBehindInMs: Long? = null,
    private val minUsefulDurationCoveredInMs: Long? = null,
    private val minUsefulResults: Int? = null,
    private val maxDataRequests: Int? = null,
    private val includeCancelledTimestamps: Boolean? = null,
) : StatusProviderContract.Filter(POI.ITEM_STATUS_TYPE_SCHEDULE, targetUUID = routeDirectionStop.uuid) {

    companion object {
        private val LOG_TAG: String = ScheduleStatusFilter::class.java.getSimpleName()

        @Suppress("unused") // main app
        const val DATA_REQUEST_MONTHS = 62

        @Suppress("unused")
        const val DATA_REQUEST_YEAR = 365

        private val MIN_USEFUL_DURATION_COVERED_IN_MS_DEFAULT = TimeUnit.DAYS.toMillis(1L)
        private const val MIN_USEFUL_RESULTS_DEFAULT = 10

        const val MAX_DATA_REQUESTS_DEFAULT = 7 * 7 // 7 weeks
        private val LOOK_BEHIND_IN_MS_DEFAULT = TimeUnit.MILLISECONDS.toMillis(0L)

        private val newDefaultTimestamp: Long get() = TimeUtils.currentTimeToTheMinuteMillis()

        @JvmStatic
        fun from(
            routeDirectionStop: RouteDirectionStop,
            lookBehindInMs: Long?,
            maxDataRequests: Int?,
            includeCancelledTimestamps: Boolean?,
        ) = ScheduleStatusFilter(
            routeDirectionStop = routeDirectionStop,
            lookBehindInMs = lookBehindInMs,
            maxDataRequests = maxDataRequests,
            includeCancelledTimestamps = includeCancelledTimestamps,
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

        private const val JSON_MIN_USEFUL_DURATION_COVERED_IN_MS = "minUsefulDurationCoveredInMs"
        private const val JSON_MIN_USEFUL_RESULTS = "minUsefulResults"
        private const val JSON_MAX_DATA_REQUESTS = "maxDataRequests"
        private const val JSON_ROUTE_DIRECTION_STOP = "routeTripStop" // do not change to avoid breaking compat w/ old modules
        private const val JSON_LOOK_BEHIND_IN_MS = "lookBehindInMs"
        private const val JSON_INCLUDE_CANCELLED_TIMESTAMPS = "includeCancelledTimestamps"

        fun fromJSON(json: JSONObject): StatusProviderContract.Filter? {
            try {
                val routeDirectionStop = RouteDirectionStop.fromJSONStatic(json.getJSONObject(JSON_ROUTE_DIRECTION_STOP)) ?: return null
                return ScheduleStatusFilter(
                    cacheOnly = getCacheOnlyFromJSON(json),
                    cacheValidityInMs = getCacheValidityInMsFromJSON(json),
                    inFocus = getInFocusFromJSON(json),
                    providedEncryptKeysMap = getProvidedEncryptKeysMapFromJSON(json),
                    routeDirectionStop = routeDirectionStop,
                    lookBehindInMs = json.optLong(JSON_LOOK_BEHIND_IN_MS, null),
                    minUsefulDurationCoveredInMs = json.optLong(JSON_MIN_USEFUL_DURATION_COVERED_IN_MS, null),
                    minUsefulResults = json.optInt(JSON_MIN_USEFUL_RESULTS, null),
                    maxDataRequests = json.optInt(JSON_MAX_DATA_REQUESTS, null),
                    includeCancelledTimestamps = json.optBoolean(JSON_INCLUDE_CANCELLED_TIMESTAMPS, null)
                )
            } catch (jsone: JSONException) {
                MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '$json'")
                return null
            }
        }

        fun toJSONString(statusFilter: StatusProviderContract.Filter) = toJSON(statusFilter)?.toString()

        fun toJSON(statusFilter: StatusProviderContract.Filter): JSONObject? {
            try {
                val json = JSONObject()
                toJSON(statusFilter, json)
                if (statusFilter is ScheduleStatusFilter) {
                    json.put(JSON_ROUTE_DIRECTION_STOP, statusFilter.routeDirectionStop.toJSON())
                    if (statusFilter.lookBehindInMs != null) {
                        json.put(JSON_LOOK_BEHIND_IN_MS, statusFilter.lookBehindInMs)
                    }
                    if (statusFilter.minUsefulDurationCoveredInMs != null) {
                        json.put(JSON_MIN_USEFUL_DURATION_COVERED_IN_MS, statusFilter.minUsefulDurationCoveredInMs)
                    }
                    if (statusFilter.minUsefulResults != null) {
                        json.put(JSON_MIN_USEFUL_RESULTS, statusFilter.minUsefulResults)
                    }
                    if (statusFilter.maxDataRequests != null) {
                        json.put(JSON_MAX_DATA_REQUESTS, statusFilter.maxDataRequests)
                    }
                    if (statusFilter.includeCancelledTimestamps != null) {
                        json.put(JSON_INCLUDE_CANCELLED_TIMESTAMPS, statusFilter.includeCancelledTimestamps)
                    }
                }
                return json
            } catch (jsone: JSONException) {
                MTLog.w(LOG_TAG, jsone, "Error while making JSON object '$statusFilter'")
                return null
            }
        }
    }

    override fun getLogTag() = LOG_TAG

    override fun copyWith(providedEncryptKeysMap: Secret<Map<String, String>>?) = this.copy(providedEncryptKeysMap = providedEncryptKeysMap)

    val targetAuthority: String get() = this.routeDirectionStop.authority

    val routeId: Long get() = this.routeDirectionStop.route.id

    val directionId: Long get() = this.routeDirectionStop.direction.id

    val lookBehindInMsOrDefault: Long get() = lookBehindInMs ?: LOOK_BEHIND_IN_MS_DEFAULT

    val timestampOrDefault: Long get() = newDefaultTimestamp

    val minUsefulDurationCoveredInMsOrDefault: Long get() = minUsefulDurationCoveredInMs ?: MIN_USEFUL_DURATION_COVERED_IN_MS_DEFAULT

    val minUsefulResultsOrDefault: Int get() = minUsefulResults ?: MIN_USEFUL_RESULTS_DEFAULT

    val maxDataRequestsOrDefault: Int get() = maxDataRequests ?: MAX_DATA_REQUESTS_DEFAULT

    val isIncludeCancelledTimestampsOrDefault: Boolean get() = this.includeCancelledTimestamps == true

    override fun fromJSONStringStatic(jsonString: String?) = fromJSONString(jsonString)

    override fun toJSONString() = toJSONString(this)

    override fun toJSONStringStatic(statusFilter: StatusProviderContract.Filter) = toJSONString(statusFilter)

    override fun toString(): String {
        return ScheduleStatusFilter::class.java.getSimpleName() + "{" +
                super.toString() +
                ", rds=" + routeDirectionStop +
                ", lookBehindInMs=" + lookBehindInMs +
                ", minUsefulDurationCoveredInMs=" + minUsefulDurationCoveredInMs +
                ", minUsefulResults=" + minUsefulResults +
                ", maxDataRequests=" + maxDataRequests +
                ", includeCancelledTimestamps=" + includeCancelledTimestamps +
                '}'
    }
}
