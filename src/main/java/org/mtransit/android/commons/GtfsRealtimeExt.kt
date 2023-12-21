package org.mtransit.android.commons

import com.google.transit.realtime.GtfsRealtime
import org.mtransit.android.formatDateTime
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.GTFSCommons

object GtfsRealtimeExt {

    @JvmStatic
    fun List<GtfsRealtime.TranslatedString.Translation>.filterUseless(): List<GtfsRealtime.TranslatedString.Translation> {
        return if (this.size <= 1) {
            this
        } else {
            this.filterNot { it.text.isNullOrBlank() }
        }
    }

    @JvmStatic
    fun List<GtfsRealtime.FeedEntity>.toAlerts(): List<GtfsRealtime.Alert> = this.filter { it.hasAlert() }.map { it.alert }

    @JvmStatic
    fun List<GtfsRealtime.Alert>.sort(now: Long = TimeUtils.currentTimeMillis()): List<GtfsRealtime.Alert> {
        return this.sortedBy {
            (it.getActivePeriod(now)
                ?: it.getFirstActivePeriod())?.start
                ?: Long.MAX_VALUE // no active period == displayed as long as in the feed (probably less important?)
        }
    }

    @JvmStatic
    fun GtfsRealtime.Alert.getActivePeriod(now: Long = TimeUtils.currentTimeMillis()) = this.activePeriodList
        .filter { it.hasStart() && it.hasEnd() }
        .singleOrNull {
            now in it.start..it.end
        }

    @JvmStatic
    fun GtfsRealtime.Alert.getFirstActivePeriod() = this.activePeriodList
        .firstOrNull {
            it.hasStart() && it.hasEnd()
        }

    @JvmStatic
    fun GtfsRealtime.EntitySelector.getRouteIdHash(): String {
        if (!FeatureFlags.F_USE_GTFS_ID_HASH_INT) {
            return this.routeId
        }
        return this.routeId.originalIdToHash()
    }

    @JvmStatic
    fun GtfsRealtime.EntitySelector.getStopIdHash(): String {
        if (!FeatureFlags.F_USE_GTFS_ID_HASH_INT) {
            return this.stopId
        }
        return this.stopId.originalIdToHash()
    }

    @JvmStatic
    fun String.originalIdToHash(): String {
        if (!FeatureFlags.F_USE_GTFS_ID_HASH_INT) {
            return this
        }
        return GTFSCommons.stringIdToHash(this).toString()
    }

    @JvmStatic
    @JvmOverloads
    fun GtfsRealtime.Alert.toStringExt(debug: Boolean = Constants.DEBUG) = buildString {
        append("Alert:")
        append("{")
        append("activePeriods[").append(activePeriodList?.size ?: 0).append("]")
        if (debug) {
            activePeriodList?.forEachIndexed { idx, period ->
                if (idx > 0) append(",") else append("=")
                append(period.toStringExt())
            }
        }
        append(", ")
        append("informedEntities[").append(informedEntityList?.size ?: 0).append("]")
        if (debug) {
            informedEntityList?.forEachIndexed { idx, entity ->
                if (idx > 0) append(",") else append("=")
                append(entity.toStringExt())
            }
        }
        append(", ")
        append("cause=").append(cause)
        if (debug && hasCauseDetail()) {
            append("(").append(causeDetail).append(")")
        }
        append(", ")
        append("effect=").append(effect)
        if (debug && hasEffectDetail()) {
            append("(").append(effectDetail).append(")")
        }
        append(", ")
        append(headerText.toStringExt("header", debug))
        append(", ")
        append(descriptionText.toStringExt("desc", debug))
        append(", ")
        append(url.toStringExt("url", debug))
        append("}")
    }

    @JvmStatic
    @JvmOverloads
    fun List<GtfsRealtime.TimeRange>?.toStringExt(debug: Boolean = Constants.DEBUG) = buildString {
        append("activePeriods[").append(this@toStringExt?.size ?: 0).append("]")
        if (debug) {
            this@toStringExt?.forEachIndexed { idx, period ->
                if (idx > 0) append(",") else append("=")
                append(period.toStringExt(debug))
            }
        }
    }

    @JvmStatic
    fun GtfsRealtime.TimeRange.toStringExt(debug: Boolean = Constants.DEBUG) = buildString {
        append("Period:")
        append("{")
        if (hasStart()) {
            append("start=").append(if (debug) start.formatDateTime() else start)
        }
        append("->")
        if (hasEnd()) {
            append("end=").append(if (debug) end.formatDateTime() else end)
        }
        append("}")
    }

    @JvmStatic
    fun GtfsRealtime.EntitySelector.toStringExt() = buildString {
        append("Entity:")
        append("{")
        if (hasAgencyId()) {
            append("agencyId=").append(agencyId)
            append("|")
        }
        if (hasRouteType()) {
            append("routeType=").append(routeType)
            append("|")
        }
        if (hasRouteId()) {
            append("routeId=").append(routeId)
            append("|")
        }
        if (hasStopId()) {
            append("stopId=").append(stopId)
            append("|")
        }
        if (hasTrip()) {
            append(trip.toStringExt())
        }
        append("}")
    }

    @JvmStatic
    fun GtfsRealtime.TripDescriptor.toStringExt() = buildString {
        append("Trip:")
        append("{")
        if (hasTripId()) {
            append("trip.tripId=").append(tripId)
            append("|")
        }
        if (hasDirectionId()) {
            append("trip.directionId=").append(directionId)
            append("|")
        }
        if (hasRouteId()) {
            append("trip.routeId=").append(routeId)
            append("|")
        }
        if (hasStartDate()) {
            append("trip.startDate=").append(startDate)
            append("|")
        }
        if (hasStartTime()) {
            append("trip.startTime=").append(startTime)
        }
        append("}")
    }

    @JvmStatic
    fun GtfsRealtime.TranslatedString.toStringExt(name: String = "i18n", debug: Boolean = Constants.DEBUG) = buildString {
        append(name).append("[").append(translationList?.size ?: 0).append("]")
        if (debug) {
            translationList?.forEachIndexed { idx, translation ->
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