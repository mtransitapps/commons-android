package org.mtransit.android.commons.provider.status

import android.net.Uri
import android.provider.BaseColumns
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.data.AppStatusFilter
import org.mtransit.android.commons.data.AvailabilityPercentStatusFilter
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.POIStatus
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.ScheduleStatusFilter
import org.mtransit.android.commons.jsonRunCatching
import org.mtransit.android.commons.provider.common.ProviderContract
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.commons.model.Secret
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

interface StatusProviderContract : ProviderContract {

    companion object {
        const val STATUS_PATH = "status"

        @JvmStatic
        val DEFAULT_STATUS_MAX_VALIDITY_IN_MS = 1.hours.inWholeMilliseconds

        @JvmStatic
        val DEFAULT_STATUS_VALIDITY_IN_MS = 60.seconds.inWholeMilliseconds

        @JvmStatic
        val DEFAULT_STATUS_VALIDITY_IN_FOCUS_IN_MS = 30.seconds.inWholeMilliseconds

        @JvmStatic
        val DEFAULT_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = 30.seconds.inWholeMilliseconds

        @JvmStatic
        val DEFAULT_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = 15.seconds.inWholeMilliseconds

        @JvmStatic
        val PROJECTION_STATUS = arrayOf(
            Columns.T_STATUS_K_ID,
            Columns.T_STATUS_K_TYPE,
            Columns.T_STATUS_K_TARGET_UUID,
            Columns.T_STATUS_K_LAST_UPDATE,
            Columns.T_STATUS_K_MAX_VALIDITY,
            Columns.T_STATUS_K_READ_FROM_SOURCE_AT,
            Columns.T_STATUS_K_EXTRAS
        )
    }

    val statusMaxValidityInMs: Long get() = DEFAULT_STATUS_MAX_VALIDITY_IN_MS

    /**
     * 1 minute maximum for Real-Time providers
     */
    fun getStatusValidityInMs(inFocus: Boolean) =
        if (inFocus) DEFAULT_STATUS_VALIDITY_IN_FOCUS_IN_MS else DEFAULT_STATUS_VALIDITY_IN_MS

    /**
     * 1 minute maximum for Real-Time providers
     */
    fun getMinDurationBetweenStatusRefreshInMs(inFocus: Boolean) =
        if (inFocus) DEFAULT_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS else DEFAULT_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS

    fun getNewStatus(statusFilter: Filter): POIStatus?

    fun cacheStatus(newStatusToCache: POIStatus)

    fun getCachedStatus(statusFilter: Filter): POIStatus?

    fun purgeUselessCachedStatuses(): Boolean

    fun deleteCachedStatus(cachedStatusId: Int): Boolean

    val authorityUri: Uri

    val statusType: Int

    val statusDbTableName: String

    object Columns {
        const val T_STATUS_K_ID: String = BaseColumns._ID
        const val T_STATUS_K_TYPE = "type"
        const val T_STATUS_K_TARGET_UUID = "target"
        const val T_STATUS_K_EXTRAS = "extras"
        const val T_STATUS_K_LAST_UPDATE = "last_update"
        const val T_STATUS_K_MAX_VALIDITY = "max_validity"
        const val T_STATUS_K_READ_FROM_SOURCE_AT = "read_from_source_at"
    }

    abstract class Filter(
        val type: Int,
        open val targetUUID: String,
    ) : ProviderContract.Filter(), MTLogK.Loggable {

        companion object {
            private val LOG_TAG: String = StatusProviderContract::class.java.getSimpleName() + ">" + Filter::class.java.getSimpleName()

            private const val JSON_TYPE = "type"
            private const val JSON_TARGET_UUID = "target"

            @JvmStatic
            fun from(
                poi: POI,
                inFocus: Boolean?,
                scheduleBehindInMs: Long?,
                scheduleMaxDataRequests: Int?,
                scheduleIncludeCancelledTimestamps: Boolean?,
                getAppPkg: () -> String?
            ): Filter? = when (poi.statusType) {
                POI.ITEM_STATUS_TYPE_NONE -> null
                POI.ITEM_STATUS_TYPE_SCHEDULE -> {
                    (poi as? RouteDirectionStop)?.let {
                        ScheduleStatusFilter(
                            inFocus = inFocus,
                            routeDirectionStop = it,
                            lookBehindInMs = scheduleBehindInMs,
                            maxDataRequests = scheduleMaxDataRequests,
                            includeCancelledTimestamps = scheduleIncludeCancelledTimestamps
                        )
                    } ?: run {
                        MTLogK.w(LOG_TAG, "Schedule filter w/o RDS '${poi.uuid}'!")
                        null
                    }
                }

                POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT -> AvailabilityPercentStatusFilter(
                    targetUUID = poi.uuid,
                    inFocus = inFocus,
                )

                POI.ITEM_STATUS_TYPE_APP -> {
                    getAppPkg()?.let { pkg ->
                        AppStatusFilter(inFocus = inFocus, targetUUID = poi.uuid, pkg = pkg)
                    } ?: run {
                        MTLogK.w(LOG_TAG, "App status filter w/o module '${poi.uuid}'!")
                        null
                    }
                }

                else -> {
                    MTLogK.w(LOG_TAG, "Unexpected status type '${poi.statusType}' for filter!")
                    null
                }
            }

            @POI.ItemStatusType
            @JvmStatic
            fun String?.getTypeFromJsonStringN() = this?.getTypeFromJsonString() ?: POI.ITEM_STATUS_TYPE_NONE

            @POI.ItemStatusType
            fun String.getTypeFromJsonString() = jsonRunCatching(LOG_TAG) {
                sharedJson.parseToJsonElement(this).jsonObject.getTypeFromJson()
            }.getOrDefault(POI.ITEM_STATUS_TYPE_NONE)

            fun JsonObject.getTypeFromJson() = getValue(JSON_TYPE).jsonPrimitive.int

            fun JsonObject.getTargetUUIDFromJson() = getValue(JSON_TARGET_UUID).jsonPrimitive.content

            fun toJson(statusFilter: Filter, json: JsonObjectBuilder) = json.toJson(statusFilter)

            fun JsonObjectBuilder.toJson(statusFilter: Filter) {
                toJsonCommon(statusFilter)
                put(JSON_TYPE, statusFilter.type)
                put(JSON_TARGET_UUID, statusFilter.targetUUID)
            }
        }

        override fun getLogTag() = LOG_TAG

        abstract fun copyWithProvidedEncryptKeysMap(providedEncryptKeysMap: Secret<Map<String, String>>?): Filter

        abstract fun copyWithCacheOnly(cacheOnly: Boolean): Filter

        override fun toString(): String {
            return Filter::class.java.getSimpleName() + "{" +
                "targetUUID='" + targetUUID + '\'' +
                ", type=" + type +
                ", " + super.toStringParts() +
                '}'
        }
    }
}
