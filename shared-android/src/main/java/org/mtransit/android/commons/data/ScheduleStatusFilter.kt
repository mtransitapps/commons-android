package org.mtransit.android.commons.data

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.common.sharedJson
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
        fun String?.toFilterN() = this?.toFilter()

        fun String.toFilter() = try {
            sharedJson.parseToJsonElement(this).jsonObject.toFilter()
        } catch (se: SerializationException) {
            MTLogK.w(LOG_TAG, "Error while parsing JSON string '$this'", se)
            throw se
        }

        private const val JSON_MIN_USEFUL_DURATION_COVERED_IN_MS = "minUsefulDurationCoveredInMs"
        private const val JSON_MIN_USEFUL_RESULTS = "minUsefulResults"
        private const val JSON_MAX_DATA_REQUESTS = "maxDataRequests"
        private const val JSON_ROUTE_DIRECTION_STOP = "routeTripStop" // do not change to avoid breaking compat w/ old modules
        private const val JSON_LOOK_BEHIND_IN_MS = "lookBehindInMs"
        private const val JSON_INCLUDE_CANCELLED_TIMESTAMPS = "includeCancelledTimestamps"

        fun JsonObject.toFilter() = try {
            this[JSON_ROUTE_DIRECTION_STOP]?.jsonObject?.toRouteDirectionStop()?.let { routeDirectionStop ->
                ScheduleStatusFilter(
                    cacheOnly = getCacheOnlyFromJson(),
                    cacheValidityInMs = getCacheValidityInMsFromJson(),
                    inFocus = getInFocusFromJson(),
                    providedEncryptKeysMap = getProvidedEncryptKeysMapFromJson(),
                    routeDirectionStop = routeDirectionStop,
                    lookBehindInMs = this[JSON_LOOK_BEHIND_IN_MS]?.jsonPrimitive?.longOrNull,
                    minUsefulDurationCoveredInMs = this[JSON_MIN_USEFUL_DURATION_COVERED_IN_MS]?.jsonPrimitive?.longOrNull,
                    minUsefulResults = this[JSON_MIN_USEFUL_RESULTS]?.jsonPrimitive?.intOrNull,
                    maxDataRequests = this[JSON_MAX_DATA_REQUESTS]?.jsonPrimitive?.intOrNull,
                    includeCancelledTimestamps = this[JSON_INCLUDE_CANCELLED_TIMESTAMPS]?.jsonPrimitive?.booleanOrNull,
                )
            }
        } catch (se: SerializationException) {
            MTLogK.w(LOG_TAG, "Error while parsing JSON object '$this'", se)
            throw se
        }
    }

    @Throws(SerializationException::class)
    private fun StatusProviderContract.Filter.toJson() = try {
        buildJsonObject {
            toJson(this@toJson)
            with(this@toJson is ScheduleStatusFilter) {
                put(JSON_ROUTE_DIRECTION_STOP, routeDirectionStop.toJson())
                lookBehindInMs?.let { put(JSON_LOOK_BEHIND_IN_MS, it) }
                minUsefulDurationCoveredInMs?.let { put(JSON_MIN_USEFUL_DURATION_COVERED_IN_MS, it) }
                minUsefulResults?.let { put(JSON_MIN_USEFUL_RESULTS, it) }
                maxDataRequests?.let { put(JSON_MAX_DATA_REQUESTS, it) }
                includeCancelledTimestamps?.let { put(JSON_INCLUDE_CANCELLED_TIMESTAMPS, it) }
            }
        }
    } catch (se: SerializationException) {
        MTLogK.w(LOG_TAG, "Error while making JSON object '$this'", se)
        throw se
    }

    override fun getLogTag() = LOG_TAG

    override fun copyWithProvidedEncryptKeysMap(providedEncryptKeysMap: Secret<Map<String, String>>?) =
        this.copy(providedEncryptKeysMap = providedEncryptKeysMap)

    override fun copyWithCacheOnly(cacheOnly: Boolean) =
        this.copy(cacheOnly = cacheOnly)

    val targetAuthority: String get() = this.routeDirectionStop.authority

    val routeId: Long get() = this.routeDirectionStop.route.id

    val directionId: Long get() = this.routeDirectionStop.direction.id

    val lookBehindInMsOrDefault: Long get() = lookBehindInMs ?: LOOK_BEHIND_IN_MS_DEFAULT

    val timestampOrDefault: Long get() = newDefaultTimestamp

    val minUsefulDurationCoveredInMsOrDefault: Long get() = minUsefulDurationCoveredInMs ?: MIN_USEFUL_DURATION_COVERED_IN_MS_DEFAULT

    val minUsefulResultsOrDefault: Int get() = minUsefulResults ?: MIN_USEFUL_RESULTS_DEFAULT

    val maxDataRequestsOrDefault: Int get() = maxDataRequests ?: MAX_DATA_REQUESTS_DEFAULT

    val isIncludeCancelledTimestampsOrDefault: Boolean get() = this.includeCancelledTimestamps == true

    @Throws(SerializationException::class)
    override fun toJsonString() = sharedJson.encodeToString(this.toJson())

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
