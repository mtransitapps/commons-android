package org.mtransit.android.commons.provider.gtfs

import com.google.transit.realtime.GtfsRealtime
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.formatDateTime
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.GTFSCommons
import org.mtransit.commons.secToMs
import java.util.regex.Pattern

@Suppress("MemberVisibilityCanBePrivate")
object GtfsRealtimeExt {

    private const val MAX_LIST_ITEMS: Int = 5

    @JvmStatic
    fun List<GtfsRealtime.TranslatedString.Translation>.filterUseless(): List<GtfsRealtime.TranslatedString.Translation> {
        return if (this.size <= 1) {
            this
        } else {
            this.filterNot { it.text.isNullOrBlank() }
        }
    }

    @JvmStatic
    fun List<GtfsRealtime.FeedEntity>.toAlerts(): List<GtfsRealtime.Alert> = this.filter { it.hasAlert() }.map { it.alert }.distinct()

    @JvmStatic
    fun List<GtfsRealtime.Alert>.sort(nowMs: Long = TimeUtils.currentTimeMillis()): List<GtfsRealtime.Alert> {
        return this.sortedBy { alert ->
            (alert.getActivePeriod(nowMs)?.startMs()
                ?: alert.activePeriodList.firstOrNull { it.hasStart() }?.startMs())
                ?: Long.MAX_VALUE // no active period == displayed as long as in the feed (probably less important?)
        }
    }

    // https://gtfs.org/realtime/feed-entities/service-alerts/#timerange
    @JvmStatic
    @JvmOverloads
    fun GtfsRealtime.Alert.isActive(nowMs: Long = TimeUtils.currentTimeMillis()): Boolean {
        return this.activePeriodList?.takeIf { it.isNotEmpty() }?.let {
            // if active period provided, must be respected
            it.any { timeRange -> timeRange.isActive(nowMs) } // If multiple ranges are given, the alert will be shown during all of them.
        } ?: true // optional (If missing, the alert will be shown as long as it appears in the feed.)
    }

    @JvmStatic
    fun GtfsRealtime.Alert.getActivePeriod(nowMs: Long = TimeUtils.currentTimeMillis()) = this.activePeriodList
        .filter { it.hasStart() && it.hasEnd() }
        .singleOrNull {
            it.isActive(nowMs)
        }

    @JvmStatic
    fun GtfsRealtime.EntitySelector.getRouteIdHash(idCleanupRegex: Pattern?): String {
        if (!FeatureFlags.F_USE_GTFS_ID_HASH_INT) {
            return this.routeId
        }
        return this.routeId.originalIdToHash(idCleanupRegex)
    }

    @JvmStatic
    fun GtfsRealtime.EntitySelector.getTripIdHash(idCleanupRegex: Pattern?): String {
        if (!FeatureFlags.F_USE_GTFS_ID_HASH_INT) {
            return this.trip.tripId
        }
        return this.trip.tripId.originalIdToHash(idCleanupRegex)
    }

    @JvmStatic
    fun GtfsRealtime.EntitySelector.getStopIdHash(idCleanupRegex: Pattern?): String {
        if (!FeatureFlags.F_USE_GTFS_ID_HASH_INT) {
            return this.stopId
        }
        return this.stopId.originalIdToHash(idCleanupRegex)
    }

    @JvmStatic
    fun String.originalIdToHash(idCleanupRegex: Pattern? = null): String {
        if (!FeatureFlags.F_USE_GTFS_ID_HASH_INT) {
            return this
        }
        return GTFSCommons.stringIdToHash(this, idCleanupRegex).toString()
    }

    fun GtfsRealtime.TimeRange.isActive(nowMs: Long = TimeUtils.currentTimeMillis()) =
        isStarted(nowMs) && !isEnded(nowMs)

    fun GtfsRealtime.TimeRange.isStarted(nowMs: Long = TimeUtils.currentTimeMillis()) =
        this.startMs()?.let { it <= nowMs } ?: true

    fun GtfsRealtime.TimeRange.startMs(): Long? =
        this.start.takeIf { this.hasStart() }?.secToMs()

    fun GtfsRealtime.TimeRange.isEnded(nowMs: Long = TimeUtils.currentTimeMillis()) =
        this.endMs()?.let { it <= nowMs } ?: false

    fun GtfsRealtime.TimeRange.endMs(): Long? =
        this.end.takeIf { this.hasEnd() }?.secToMs()

    @JvmStatic
    @JvmOverloads
    fun GtfsRealtime.Alert.toStringExt(debug: Boolean = Constants.DEBUG) = buildString {
        append("Alert:")
        append("{")
        append(informedEntityList.toStringExt(short = true, debug))
        append(", ")
        append(activePeriodList.toStringExt(short = true, debug))
        append(", ")
        append("cause=").append(cause)
        if (debug && hasCauseDetail()) {
            append("(").append(causeDetail.toStringExt("detail")).append(")")
        }
        append(", ")
        append("effect=").append(effect)
        if (debug && hasEffectDetail()) {
            append("(").append(effectDetail.toStringExt("detail")).append(")")
        }
        append(", ")
        append(headerText.toStringExt("header", debug))
        append(", ")
        append(descriptionText.toStringExt("desc", debug))
        append(", ")
        append(url.toStringExt("url", debug))
        append("}")
    }

    @JvmName("toStringExtEntity")
    @JvmStatic
    @JvmOverloads
    fun List<GtfsRealtime.EntitySelector>?.toStringExt(short: Boolean = false, debug: Boolean = Constants.DEBUG) = buildString {
        append(if (short) "ES[" else "informedEntities[").append(this@toStringExt?.size ?: 0).append("]")
        if (debug) {
            this@toStringExt?.take(MAX_LIST_ITEMS)?.forEachIndexed { idx, entity ->
                if (idx > 0) append(",") else append("=")
                append(entity.toStringExt(short = true))
            }
        }
    }

    @JvmName("toStringExtRange")
    @JvmStatic
    @JvmOverloads
    fun List<GtfsRealtime.TimeRange>?.toStringExt(short: Boolean = false, debug: Boolean = Constants.DEBUG) = buildString {
        append(if (short) "TR[" else "activePeriods[").append(this@toStringExt?.size ?: 0).append("]")
        if (debug) {
            this@toStringExt?.take(MAX_LIST_ITEMS)?.forEachIndexed { idx, period ->
                if (idx > 0) append(",") else append("=")
                @Suppress("KotlinConstantConditions")
                append(period.toStringExt(short = true, debug))
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun GtfsRealtime.TimeRange.toStringExt(short: Boolean = false, debug: Boolean = Constants.DEBUG) = buildString {
        append(if (short) "TR:" else "Period:")
        append("{")
        if (hasStart()) {
            if (!short) append("start=")
            append(if (debug) startMs().formatDateTime() else start)
        }
        append("->")
        if (hasEnd()) {
            if (!short) append("end=")
            append(if (debug) endMs().formatDateTime() else end)
        }
        append("}")
    }

    @JvmStatic
    @JvmOverloads
    fun GtfsRealtime.EntitySelector.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "ES:" else "Entity:")
        append("{")
        if (hasAgencyId()) {
            append(if (short) "a=" else "agencyId=").append(agencyId)
            append("|")
        }
        if (hasRouteType()) {
            append(if (short) "rt=" else "routeType=").append(routeType)
            append("|")
        }
        if (hasRouteId()) {
            append(if (short) "r=" else "routeId=").append(routeId)
            append("|")
        }
        if (hasStopId()) {
            append(if (short) "s=" else "stopId=").append(stopId)
            append("|")
        }
        if (hasTrip()) {
            append(trip.toStringExt(short))
        }
        append("}")
    }

    @JvmStatic
    @JvmOverloads
    fun GtfsRealtime.TripDescriptor.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "TD:" else "Trip:")
        append("{")
        if (hasTripId()) {
            append(if (short) "t=" else "tripId=").append(tripId)
            append("|")
        }
        if (hasDirectionId()) {
            append(if (short) "d=" else "directionId=").append(directionId)
            append("|")
        }
        if (hasRouteId()) {
            append(if (short) "r=" else "routeId=").append(routeId)
            append("|")
        }
        if (hasStartDate()) {
            append(if (short) "sd=" else "startDate=").append(startDate)
            append("|")
        }
        if (hasStartTime()) {
            append(if (short) "st=" else "startTime=").append(startTime)
        }
        append("}")
    }

    @JvmOverloads
    @JvmStatic
    fun GtfsRealtime.TranslatedString.toStringExt(name: String = "i18n", debug: Boolean = Constants.DEBUG) = buildString {
        append(name).append("[").append(translationList?.size ?: 0).append("]")
        if (debug) {
            translationList?.take(MAX_LIST_ITEMS)?.forEachIndexed { idx, translation ->
                if (idx > 0) append(",") else append("=")
                append(translation.toStringExt())
            }
        }
    }

    @JvmStatic
    fun GtfsRealtime.TranslatedString.Translation.toStringExt() = buildString {
        append("{").append(language).append(":").append(text).append("}")
    }
}