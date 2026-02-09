package org.mtransit.android.commons.provider.gtfs

import com.google.transit.realtime.GtfsRealtime
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.toDateTimeLog
import org.mtransit.commons.GTFSCommons
import org.mtransit.commons.secToMs
import java.util.regex.Pattern

@Suppress("MemberVisibilityCanBePrivate", "unused")
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
    fun List<GtfsRealtime.FeedEntity>.toVehicles(): List<GtfsRealtime.VehiclePosition> =
        this.filter { it.hasVehicle() }.map { it.vehicle }.distinct()

    @JvmStatic
    fun List<GtfsRealtime.FeedEntity>.toVehiclesWithIdPair(): List<Pair<GtfsRealtime.VehiclePosition, String>> =
        this.filter { it.hasVehicle() }.map { it.vehicle to it.id }.distinctBy { it.first }

    @JvmStatic
    fun List<GtfsRealtime.VehiclePosition>.sortVehicles(nowMs: Long = TimeUtils.currentTimeMillis()): List<GtfsRealtime.VehiclePosition> =
        this.sortedBy { vehiclePosition ->
            vehiclePosition.timestamp
        }

    @JvmStatic
    fun List<Pair<GtfsRealtime.VehiclePosition, String>>.sortVehiclesPair(nowMs: Long = TimeUtils.currentTimeMillis()): List<Pair<GtfsRealtime.VehiclePosition, String>> =
        this.sortedBy { (vehiclePosition, _) ->
            vehiclePosition.timestamp
        }

    @JvmStatic
    fun List<GtfsRealtime.FeedEntity>.toAlerts(): List<GtfsRealtime.Alert> =
        this.filter { it.hasAlert() }.map { it.alert }.distinct()

    @JvmStatic
    fun List<GtfsRealtime.FeedEntity>.toAlertsWithIdPair(): List<Pair<GtfsRealtime.Alert, String>> =
        this.filter { it.hasAlert() }.map { it.alert to it.id }.distinctBy { it.first }

    @JvmStatic
    fun List<GtfsRealtime.Alert>.sortAlerts(nowMs: Long = TimeUtils.currentTimeMillis()): List<GtfsRealtime.Alert> =
        this.sortedBy { alert ->
            (alert.getActivePeriod(nowMs)?.startMs()
                ?: alert.activePeriodList.firstOrNull { it.hasStart() }?.startMs())
                ?: Long.MAX_VALUE // no active period == displayed as long as in the feed (probably less important?)
        }

    @JvmStatic
    fun List<Pair<GtfsRealtime.Alert, String>>.sortAlertsPair(nowMs: Long = TimeUtils.currentTimeMillis()): List<Pair<GtfsRealtime.Alert, String>> =
        this.sortedBy { (alert, _) ->
            (alert.getActivePeriod(nowMs)?.startMs()
                ?: alert.activePeriodList.firstOrNull { it.hasStart() }?.startMs())
                ?: Long.MAX_VALUE // no active period == displayed as long as in the feed (probably less important?)
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
    fun GtfsRealtime.EntitySelector.getRouteIdHash(idCleanupRegex: Pattern?): String =
        this.routeId.originalIdToHash(idCleanupRegex)

    @JvmStatic
    fun GtfsRealtime.EntitySelector.getTripIdHash(idCleanupRegex: Pattern?): String =
        this.trip.tripId.originalIdToHash(idCleanupRegex)

    @JvmStatic
    fun GtfsRealtime.EntitySelector.getStopIdHash(idCleanupRegex: Pattern?): String =
        this.stopId.originalIdToHash(idCleanupRegex)

    @JvmStatic
    fun String.originalIdToHash(idCleanupRegex: Pattern? = null): String =
        GTFSCommons.stringIdToHash(this, idCleanupRegex).toString()

    @JvmStatic
    fun String.originalIdToId(idCleanupRegex: Pattern? = null): String =
        GTFSCommons.originalIdToId(this, idCleanupRegex)

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
    fun GtfsRealtime.VehiclePosition.toStringExt(debug: Boolean = Constants.DEBUG) = buildString {
        append("VehiclePosition:")
        append("{")
        if (hasTrip()) append(trip.toStringExt(short = true)).append(", ")
        if (hasPosition()) append(position.toStringExt(short = true)).append(", ")
        if (hasVehicle()) append(vehicle.toStringExt(short = true)).append(", ")
        if (hasCurrentStopSequence()) append("currentStopSequence=").append(currentStopSequence).append(", ")
        if (hasCurrentStatus()) append("currentStatus=").append(currentStatus).append(", ")
        if (hasStopId()) append("stopId=").append(stopId).append(", ")
        if (hasTimestamp()) append("timestamp=").append(timestamp).append(", ")
        if (hasOccupancyPercentage()) append("occupancyPct=").append(occupancyPercentage).append(", ")
        if (hasOccupancyStatus()) append("occupancyStatus=").append(occupancyStatus).append(", ")
        if (hasCongestionLevel()) append("congestionLevel=").append(congestionLevel).append(", ")
        append("}")
    }

    val GtfsRealtime.VehiclePosition.optTrip get() = if (hasTrip()) trip else null
    val GtfsRealtime.VehiclePosition.optTimestamp get() = if (hasTimestamp()) timestamp else null
    val GtfsRealtime.VehiclePosition.optPosition get() = if (hasPosition()) position else null
    val GtfsRealtime.VehiclePosition.optVehicle get() = if (hasVehicle()) vehicle else null

    @JvmStatic
    @JvmOverloads
    fun GtfsRealtime.Position.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "P:" else "Position:")
        append("{")
        if (hasLatitude()) append("lat=").append(latitude).append(", ")
        if (hasLongitude()) append("lon=").append(longitude).append(", ")
        if (hasBearing()) append("bearing=").append(bearing).append(", ")
        if (hasSpeed()) append("speed=").append(speed).append(", ")
        if (hasOdometer()) append("odometer=").append(odometer).append(", ")
        append("}")
    }

    val GtfsRealtime.Position.optLatitude get() = if (hasLatitude()) latitude else null
    val GtfsRealtime.Position.optLongitude get() = if (hasLongitude()) longitude else null
    val GtfsRealtime.Position.optBearing get() = if (hasBearing()) bearing else null
    val GtfsRealtime.Position.optSpeed get() = if (hasSpeed()) speed else null
    val GtfsRealtime.Position.optOdometer get() = if (hasOdometer()) odometer else null

    @JvmStatic
    @JvmOverloads
    fun GtfsRealtime.VehicleDescriptor.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "VD:" else "VehicleDescriptor:")
        append("{")
        if (hasId()) append("id=").append(id).append(", ")
        if (hasLabel()) append("lbl=").append(label).append(", ")
        if (hasLicensePlate()) append("licensePlate=").append(licensePlate).append(", ")
        if (hasWheelchairAccessible()) append("a18n=").append(wheelchairAccessible).append(", ")
        append("}")
    }

    val GtfsRealtime.VehicleDescriptor.optId get() = if (hasId()) id else null
    val GtfsRealtime.VehicleDescriptor.optLabel get() = if (hasLabel()) label else null

    @JvmStatic
    @JvmOverloads
    fun GtfsRealtime.Alert.toStringExt(debug: Boolean = Constants.DEBUG) = buildString {
        append("Alert:")
        append("{")
        append(informedEntityList.toStringExt(short = true, debug)).append(", ")
        append(activePeriodList.toStringExt(short = true, debug)).append(", ")
        if (hasCause()) append("cause=").append(cause).append(", ")
        if (debug && hasCauseDetail()) append("(").append(causeDetail.toStringExt("detail")).append(")").append(",")
        if (hasEffect()) append("effect=").append(effect).append(", ")
        if (debug && hasEffectDetail()) append("(").append(effectDetail.toStringExt("detail")).append(")").append(", ")
        if (hasHeaderText()) append(headerText.toStringExt("header", debug)).append(", ")
        if (hasDescriptionText()) append(descriptionText.toStringExt("desc", debug)).append(", ")
        if (hasUrl()) append(url.toStringExt("url", debug)).append(", ")
        append("}")
    }

    @JvmName("toStringExtEntity")
    @JvmStatic
    @JvmOverloads
    fun List<GtfsRealtime.EntitySelector>?.toStringExt(short: Boolean = false, debug: Boolean = Constants.DEBUG) = buildString {
        append(if (short) "ESs[" else "EntitySelectors[").append(this@toStringExt?.size ?: 0).append("]")
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
        append(if (short) "TRs[" else "TimeRanges[").append(this@toStringExt?.size ?: 0).append("]")
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
        append(if (short) "TR:" else "TimeRange:")
        append("{")
        if (hasStart()) {
            if (!short) append("start=")
            append(if (debug) startMs().toDateTimeLog() else start)
        }
        append("->")
        if (hasEnd()) {
            if (!short) append("end=")
            append(if (debug) endMs().toDateTimeLog() else end)
        }
        append("}")
    }

    @JvmStatic
    @JvmOverloads
    fun GtfsRealtime.EntitySelector.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "ES:" else "EntitySelector:")
        append("{")
        if (hasAgencyId()) append(if (short) "a=" else "agencyId=").append(agencyId).append("|")
        if (hasRouteType()) append(if (short) "rt=" else "routeType=").append(routeType).append("|")
        if (hasRouteId()) append(if (short) "r=" else "routeId=").append(routeId).append("|")
        if (hasStopId()) append(if (short) "s=" else "stopId=").append(stopId).append("|")
        if (hasDirectionId()) append(if (short) "d=" else "directionId=").append(directionId).append("|")
        if (hasTrip()) append(trip.toStringExt(short))
        append("}")
    }

    val GtfsRealtime.EntitySelector.optAgencyId get() = if (hasAgencyId()) agencyId else null
    val GtfsRealtime.EntitySelector.optRouteId get() = if (hasRouteId()) routeId else this.optTrip?.optRouteId
    val GtfsRealtime.EntitySelector.optDirectionId get() = if (hasDirectionId()) directionId else this.optTrip?.optDirectionId
    val GtfsRealtime.EntitySelector.optStopId get() = if (hasStopId()) stopId else null
    val GtfsRealtime.EntitySelector.optRouteType get() = if (hasRouteType()) routeType else null
    val GtfsRealtime.EntitySelector.optTrip get() = if (hasTrip()) this.trip else null

    @JvmStatic
    @JvmOverloads
    fun GtfsRealtime.TripDescriptor.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "TD:" else "TripDescriptor:")
        append("{")
        if (hasTripId()) append(if (short) "t=" else "tripId=").append(tripId).append("|")
        if (hasDirectionId()) append(if (short) "d=" else "directionId=").append(directionId).append("|")
        if (hasRouteId()) append(if (short) "r=" else "routeId=").append(routeId).append("|")
        if (hasModifiedTrip()) append(modifiedTrip.toStringExt())
        if (hasScheduleRelationship()) append(if (short) "sr=" else "schedRel=").append(scheduleRelationship).append("|")
        if (hasStartDate()) append(if (short) "sd=" else "startDate=").append(startDate).append("|")
        if (hasStartTime()) append(if (short) "st=" else "startTime=").append(startTime).append("|")
        append("}")
    }

    val GtfsRealtime.TripDescriptor.optTripId get() = if (hasTripId()) tripId else null
    val GtfsRealtime.TripDescriptor.optRouteId get() = if (hasRouteId()) routeId else null
    val GtfsRealtime.TripDescriptor.optDirectionId get() = if (hasDirectionId()) directionId else null

    @JvmStatic
    @JvmOverloads
    fun GtfsRealtime.TripDescriptor.ModifiedTripSelector.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "MTS:" else "ModifiedTripSelector:")
        append("{")
        if (hasModificationsId()) append(if (short) "m=" else "modificationsId=").append(modificationsId).append("|")
        if (hasAffectedTripId()) append(if (short) "at=" else "affectedTripId=").append(affectedTripId).append("|")
        if (hasStartDate()) append(if (short) "sd=" else "startDate=").append(startDate).append("|")
        if (hasStartTime()) append(if (short) "st=" else "startTime=").append(startTime).append("|")
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