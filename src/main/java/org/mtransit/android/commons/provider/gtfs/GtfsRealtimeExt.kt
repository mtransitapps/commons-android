package org.mtransit.android.commons.provider.gtfs

import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.secsToInstant
import org.mtransit.android.toDateTimeLog
import org.mtransit.commons.GTFSCommons
import org.mtransit.commons.secToMs
import java.util.regex.Pattern
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import com.google.transit.realtime.GtfsRealtime.Alert as GAlert
import com.google.transit.realtime.GtfsRealtime.EntitySelector as GEntitySelector
import com.google.transit.realtime.GtfsRealtime.FeedEntity as GFeedEntity
import com.google.transit.realtime.GtfsRealtime.Position as GPosition
import com.google.transit.realtime.GtfsRealtime.TimeRange as GTimeRange
import com.google.transit.realtime.GtfsRealtime.TranslatedString as GTranslatedString
import com.google.transit.realtime.GtfsRealtime.TranslatedString.Translation as GTSTranslation
import com.google.transit.realtime.GtfsRealtime.TripDescriptor as GTripDescriptor
import com.google.transit.realtime.GtfsRealtime.TripUpdate as GTripUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent as GTUStopTimeEvent
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate as GTUStopTimeUpdate
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor as GVehicleDescriptor
import com.google.transit.realtime.GtfsRealtime.VehiclePosition as GVehiclePosition

@Suppress("MemberVisibilityCanBePrivate", "unused")
object GtfsRealtimeExt {

    private const val MAX_LIST_ITEMS: Int = 5

    @JvmStatic
    fun List<GTSTranslation>.filterUseless(): List<GTSTranslation> {
        return if (this.size <= 1) {
            this
        } else {
            this.filterNot { it.text.isNullOrBlank() }
        }
    }

    @JvmStatic
    fun List<GFeedEntity>.toTripUpdates(): List<GTripUpdate> =
        this.filter { it.hasTripUpdate() }.map { it.tripUpdate }.distinct()

    @JvmStatic
    fun List<GFeedEntity>.toTripUpdatesWithIdPair(): List<Pair<GTripUpdate, String>> =
        this.filter { it.hasTripUpdate() }.map { it.tripUpdate to it.id }.distinctBy { it.first }

    @JvmStatic
    fun List<GTripUpdate>.sortTripUpdates(nowMs: Long = TimeUtils.currentTimeMillis()): List<GTripUpdate> =
        this.sortedBy { it.timestamp }

    @JvmStatic
    fun List<Pair<GTripUpdate, String>>.sortTripUpdatesPair(nowMs: Long = TimeUtils.currentTimeMillis()): List<Pair<GTripUpdate, String>> =
        this.sortedBy { (it, _) -> it.timestamp }

    @JvmStatic
    fun List<GFeedEntity>.toVehicles(): List<GVehiclePosition> =
        this.filter { it.hasVehicle() }.map { it.vehicle }.distinct()

    @JvmStatic
    fun List<GFeedEntity>.toVehiclesWithIdPair(): List<Pair<GVehiclePosition, String>> =
        this.filter { it.hasVehicle() }.map { it.vehicle to it.id }.distinctBy { it.first }

    @JvmStatic
    fun List<GVehiclePosition>.sortVehicles(nowMs: Long = TimeUtils.currentTimeMillis()): List<GVehiclePosition> =
        this.sortedBy { it.timestamp }

    @JvmStatic
    fun List<Pair<GVehiclePosition, String>>.sortVehiclesPair(nowMs: Long = TimeUtils.currentTimeMillis()): List<Pair<GVehiclePosition, String>> =
        this.sortedBy { (vehiclePosition, _) -> vehiclePosition.timestamp }

    @JvmStatic
    fun List<GFeedEntity>.toAlerts(): List<GAlert> =
        this.filter { it.hasAlert() }.map { it.alert }.distinct()

    @JvmStatic
    fun List<GFeedEntity>.toAlertsWithIdPair(): List<Pair<GAlert, String>> =
        this.filter { it.hasAlert() }.map { it.alert to it.id }.distinctBy { it.first }

    @JvmStatic
    fun List<GAlert>.sortAlerts(nowMs: Long = TimeUtils.currentTimeMillis()): List<GAlert> =
        this.sortedBy { alert ->
            (alert.getActivePeriod(nowMs)?.startMs()
                ?: alert.activePeriodList.firstOrNull { it.hasStart() }?.startMs())
                ?: Long.MAX_VALUE // no active period == displayed as long as in the feed (probably less important?)
        }

    @JvmStatic
    fun List<Pair<GAlert, String>>.sortAlertsPair(nowMs: Long = TimeUtils.currentTimeMillis()): List<Pair<GAlert, String>> =
        this.sortedBy { (alert, _) ->
            (alert.getActivePeriod(nowMs)?.startMs()
                ?: alert.activePeriodList.firstOrNull { it.hasStart() }?.startMs())
                ?: Long.MAX_VALUE // no active period == displayed as long as in the feed (probably less important?)
        }

    // https://gtfs.org/realtime/feed-entities/service-alerts/#timerange
    @JvmStatic
    @JvmOverloads
    fun GAlert.isActive(nowMs: Long = TimeUtils.currentTimeMillis()): Boolean {
        return this.activePeriodList?.takeIf { it.isNotEmpty() }?.let {
            // if active period provided, must be respected
            it.any { timeRange -> timeRange.isActive(nowMs) } // If multiple ranges are given, the alert will be shown during all of them.
        } ?: true // optional (If missing, the alert will be shown as long as it appears in the feed.)
    }

    @JvmStatic
    fun GAlert.getActivePeriod(nowMs: Long = TimeUtils.currentTimeMillis()) = this.activePeriodList
        .filter { it.hasStart() && it.hasEnd() }
        .singleOrNull {
            it.isActive(nowMs)
        }

    @JvmStatic
    fun GEntitySelector.getRouteIdHash(idCleanupRegex: Pattern?): String =
        this.routeId.originalIdToHash(idCleanupRegex)

    @JvmStatic
    fun GEntitySelector.getTripIdHash(idCleanupRegex: Pattern?): String =
        this.trip.tripId.originalIdToHash(idCleanupRegex)

    @JvmStatic
    fun GEntitySelector.getStopIdHash(idCleanupRegex: Pattern?): String =
        this.stopId.originalIdToHash(idCleanupRegex)

    @JvmStatic
    fun String.originalIdToHash(idCleanupRegex: Pattern? = null): String =
        GTFSCommons.stringIdToHash(this, idCleanupRegex).toString()

    @JvmStatic
    fun String.originalIdToId(idCleanupRegex: Pattern? = null): String =
        GTFSCommons.originalIdToId(this, idCleanupRegex)

    fun GTimeRange.isActive(nowMs: Long = TimeUtils.currentTimeMillis()) =
        isStarted(nowMs) && !isEnded(nowMs)

    fun GTimeRange.isStarted(nowMs: Long = TimeUtils.currentTimeMillis()) =
        this.startMs()?.let { it <= nowMs } ?: true

    fun GTimeRange.startMs(): Long? =
        this.start.takeIf { this.hasStart() }?.secToMs()

    fun GTimeRange.isEnded(nowMs: Long = TimeUtils.currentTimeMillis()) =
        this.endMs()?.let { it <= nowMs } ?: false

    fun GTimeRange.endMs(): Long? =
        this.end.takeIf { this.hasEnd() }?.secToMs()

    @JvmStatic
    @JvmOverloads
    fun GTripUpdate.toStringExt(debug: Boolean = Constants.DEBUG) = buildString {
        append("TripUpdate:")
        append(
            buildList {
                optTrip?.let { add(it.toStringExt(short = true)) }
                optVehicle?.let { add(it.toStringExt(short = true)) }
                optStopTimeUpdateList?.let { add(it.toStringExt(short = true)) }
                optTimestamp?.let { add("timestamp=$timestamp") }
                optDelay?.let { add("delay=$delay") }
            }.joinToString(separator = ",", prefix = "{", postfix = "}")
        )
    }

    val GTripUpdate.optTrip get() = if (hasTrip()) trip else null
    val GTripUpdate.optVehicle get() = if (hasVehicle()) vehicle else null
    val GTripUpdate.optStopTimeUpdateList get() = stopTimeUpdateList?.takeIf { it.isNotEmpty() }
    val GTripUpdate.optTimestamp get() = if (hasTimestamp()) timestamp else null
    val GTripUpdate.optDelay get() = if (hasDelay()) delay else null
    val GTripUpdate.optDelayDuration get() = this.optDelay?.seconds
    val GTripUpdate.optTripProperties get() = if (hasTripProperties()) tripProperties else null

    @JvmName("toStringExtStopTimeUpdate")
    @JvmStatic
    @JvmOverloads
    fun List<GTUStopTimeUpdate>?.toStringExt(short: Boolean = false, debug: Boolean = Constants.DEBUG) = buildString {
        append(if (short) "STUs[" else "StopTimeUpdate[").append(this@toStringExt?.size ?: 0).append("]")
        if (debug) {
            this@toStringExt?.take(MAX_LIST_ITEMS)?.forEachIndexed { idx, stopTimeUpdate ->
                if (idx > 0) append(",") else append("=")
                append(stopTimeUpdate.toStringExt(short = true))
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun GTUStopTimeUpdate.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "STU:" else "StopTimeUpdate:")
        append("{")
        optStopSequence?.let { append("stopSeq=").append(stopSequence).append(", ") }
        optStopId?.let { append("stopId=").append(stopId).append(", ") }
        optArrival?.let { append(it.toStringExt(short = true)).append(", ") }
        optDeparture?.let { append(it.toStringExt(short = true)).append(", ") }
        optDepartureOccupancyStatus?.let { append("depOcc=").append(departureOccupancyStatus).append(", ") }
        optScheduleRelationship?.let { append("schedRel=").append(scheduleRelationship).append(", ") }
        optStopTimeProperties?.let { append(it.toStringExt(short = true)).append(", ") }
        append("}")
    }

    val GTUStopTimeUpdate.optStopSequence get() = if (hasStopSequence()) stopSequence else null
    val GTUStopTimeUpdate.optStopId get() = if (hasStopId()) stopId else null
    val GTUStopTimeUpdate.optArrival get() = if (hasArrival()) arrival else null
    val GTUStopTimeUpdate.optDeparture get() = if (hasDeparture()) departure else null
    val GTUStopTimeUpdate.optDepartureOccupancyStatus get() = if (hasDepartureOccupancyStatus()) departureOccupancyStatus else null
    val GTUStopTimeUpdate.optScheduleRelationship get() = if (hasScheduleRelationship()) scheduleRelationship else null
    val GTUStopTimeUpdate.optStopTimeProperties get() = if (hasStopTimeProperties()) stopTimeProperties else null

    @JvmStatic
    @JvmOverloads
    fun GTUStopTimeEvent.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "STE:" else "StopTimeEvent:")
        append("{")
        optDelay?.let { append("delay=").append(delay).append(", ") }
        optTime?.let { append("time=").append(time).append(", ") }
        optUncertainty?.let { append("uncertainty=").append(uncertainty).append(", ") }
        optScheduledTime?.let { append("schedTime=").append(scheduledTime).append(", ") }
        append("}")
    }

    val GTUStopTimeEvent.optDelay get() = if (hasDelay()) delay else null
    val GTUStopTimeEvent.optDelayDuration: Duration? get() = this.optDelay?.seconds
    val GTUStopTimeEvent.optTime get() = if (hasTime()) time else null
    val GTUStopTimeEvent.optTimeInstant get() = if (hasTime()) time.secsToInstant() else null
    val GTUStopTimeEvent.optUncertainty get() = if (hasUncertainty()) uncertainty else null
    val GTUStopTimeEvent.optScheduledTime get() = if (hasScheduledTime()) scheduledTime else null

    @JvmStatic
    @JvmOverloads
    fun GTUStopTimeUpdate.StopTimeProperties.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "STP:" else "StopTimeProperties:")
        append("{")
        optAssignedStopId?.let { append("aStopId=").append(assignedStopId).append(", ") }
        optStopHeadsign?.let { append("stopHeadsign=").append(stopHeadsign).append(", ") }
        optPickupType?.let { append("pickupType=").append(pickupType).append(", ") }
        optDropOffType?.let { append("dropOffType=").append(dropOffType).append(", ") }
        append("}")
    }

    val GTUStopTimeUpdate.StopTimeProperties.optAssignedStopId get() = if (hasAssignedStopId()) assignedStopId else null
    val GTUStopTimeUpdate.StopTimeProperties.optStopHeadsign get() = if (hasStopHeadsign()) stopHeadsign else null
    val GTUStopTimeUpdate.StopTimeProperties.optPickupType get() = if (hasPickupType()) pickupType else null
    val GTUStopTimeUpdate.StopTimeProperties.optDropOffType get() = if (hasDropOffType()) dropOffType else null

    @JvmStatic
    @JvmOverloads
    fun GVehiclePosition.toStringExt(debug: Boolean = Constants.DEBUG) = buildString {
        append("VehiclePosition:")
        append("{")
        optTrip?.let { append(it.toStringExt(short = true)).append(", ") }
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

    val GVehiclePosition.optTrip get() = if (hasTrip()) trip else null
    val GVehiclePosition.optTimestamp get() = if (hasTimestamp()) timestamp else null
    val GVehiclePosition.optPosition get() = if (hasPosition()) position else null
    val GVehiclePosition.optVehicle get() = if (hasVehicle()) vehicle else null

    @JvmStatic
    @JvmOverloads
    fun GPosition.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "P:" else "Position:")
        append("{")
        if (hasLatitude()) append("lat=").append(latitude).append(", ")
        if (hasLongitude()) append("lon=").append(longitude).append(", ")
        if (hasBearing()) append("bearing=").append(bearing).append(", ")
        if (hasSpeed()) append("speed=").append(speed).append(", ")
        if (hasOdometer()) append("odometer=").append(odometer).append(", ")
        append("}")
    }

    val GPosition.optLatitude get() = if (hasLatitude()) latitude else null
    val GPosition.optLongitude get() = if (hasLongitude()) longitude else null
    val GPosition.optBearing get() = if (hasBearing()) bearing else null
    val GPosition.optSpeed get() = if (hasSpeed()) speed else null
    val GPosition.optOdometer get() = if (hasOdometer()) odometer else null

    @JvmStatic
    @JvmOverloads
    fun GVehicleDescriptor.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "VD:" else "VehicleDescriptor:")
        append("{")
        optId?.let { append("id=").append(id).append(", ") }
        optLabel?.let { append("label=").append(label).append(", ") }
        optLicensePlate?.let { append("licensePlate=").append(licensePlate).append(", ") }
        optWheelchairAccessible?.let { append("a18n=").append(wheelchairAccessible).append(", ") }
        append("}")
    }

    val GVehicleDescriptor.optId get() = if (hasId()) id else null
    val GVehicleDescriptor.optLabel get() = if (hasLabel()) label else null
    val GVehicleDescriptor.optLicensePlate get() = if (hasLicensePlate()) licensePlate else null
    val GVehicleDescriptor.optWheelchairAccessible get() = if (hasWheelchairAccessible()) wheelchairAccessible else null

    @JvmStatic
    @JvmOverloads
    fun GAlert.toStringExt(debug: Boolean = Constants.DEBUG) = buildString {
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
    fun List<GEntitySelector>?.toStringExt(short: Boolean = false, debug: Boolean = Constants.DEBUG): String = buildString {
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
    fun List<GTimeRange>?.toStringExt(short: Boolean = false, debug: Boolean = Constants.DEBUG) = buildString {
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
    fun GTimeRange.toStringExt(short: Boolean = false, debug: Boolean = Constants.DEBUG) = buildString {
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
    fun GEntitySelector.toStringExt(short: Boolean = false) = buildString {
        append(if (short) "ES:" else "EntitySelector:")
        append(
            buildList {
                optAgencyId?.let { add((if (short) "a=" else "agencyId=") + agencyId) }
                optRouteType?.let { add((if (short) "rt=" else "routeType=") + routeType) }
                optRouteId?.let { add((if (short) "r=" else "routeId=") + routeId) }
                optStopId?.let { add((if (short) "s=" else "stopId=") + stopId) }
                optDirectionId?.let { add((if (short) "d=" else "directionId=") + directionId) }
                optTrip?.let { add(it.toStringExt(short)) }
            }.joinToString(separator = "|", prefix = "{", postfix = "}")
        )
    }

    val GEntitySelector.optAgencyId get() = if (hasAgencyId()) agencyId else null
    val GEntitySelector.optRouteId get() = if (hasRouteId()) routeId else this.optTrip?.optRouteId
    val GEntitySelector.optDirectionId get() = if (hasDirectionId()) directionId else this.optTrip?.optDirectionId
    val GEntitySelector.optStopId get() = if (hasStopId()) stopId else null
    val GEntitySelector.optRouteType get() = if (hasRouteType()) routeType else null
    val GEntitySelector.optTrip get() = if (hasTrip()) this.trip else null

    @JvmStatic
    @JvmOverloads
    fun GTripDescriptor.toStringExt(short: Boolean = false): String = buildString {
        append(if (short) "TD:" else "TripDescriptor:")
        append(
            buildList {
                optRouteId?.let { add((if (short) "r=" else "routeId=") + routeId) }
                optDirectionId?.let { add((if (short) "d=" else "directionId=") + directionId) }
                optTripId?.let { add((if (short) "t=" else "tripId=") + tripId) }
                optModifiedTrip?.let { add(modifiedTrip.toStringExt()) }
                optScheduleRelationship?.let { add((if (short) "sr=" else "schedRel=") + scheduleRelationship) }
                optStartDate?.let { add((if (short) "sd=" else "startDate=") + startDate) }
                optStartTime?.let { add((if (short) "st=" else "startTime=") + startTime) }
            }.joinToString(separator = "|", prefix = "{", postfix = "}")
        )
    }

    val GTripDescriptor.optTripId get() = if (hasTripId()) tripId else null
    val GTripDescriptor.optRouteId get() = if (hasRouteId()) routeId else null
    val GTripDescriptor.optDirectionId get() = if (hasDirectionId()) directionId else null
    val GTripDescriptor.optModifiedTrip get() = if (hasModifiedTrip()) modifiedTrip else null
    val GTripDescriptor.optScheduleRelationship get() = if (hasScheduleRelationship()) scheduleRelationship else null
    val GTripDescriptor.optStartDate get() = if (hasStartDate()) startDate else null
    val GTripDescriptor.optStartTime get() = if (hasStartTime()) startTime else null

    @JvmStatic
    @JvmOverloads
    fun GTripDescriptor.ModifiedTripSelector.toStringExt(short: Boolean = false) = buildString {
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
    fun GTranslatedString.toStringExt(name: String = "i18n", debug: Boolean = Constants.DEBUG) = buildString {
        append(name).append("[").append(translationList?.size ?: 0).append("]")
        if (debug) {
            translationList?.take(MAX_LIST_ITEMS)?.forEachIndexed { idx, translation ->
                if (idx > 0) append(",") else append("=")
                append(translation.toStringExt())
            }
        }
    }

    @JvmStatic
    fun GTSTranslation.toStringExt() = buildString {
        append("{").append(language).append(":").append(text).append("}")
    }
}