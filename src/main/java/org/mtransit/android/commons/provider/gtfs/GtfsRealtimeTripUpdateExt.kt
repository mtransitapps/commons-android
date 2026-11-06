package org.mtransit.android.commons.provider.gtfs

import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopIdNotEmpty
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopSequence
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate as GTUStopTimeUpdate

// region stop time update

fun GTFSRealTimeProvider.isSameStop(gSTU: GTUStopTimeUpdate?, rds: RouteDirectionStop?, stopSequence: Int) =
    gSTU?.isSameStop(rds, stopSequence, this::parseStopId) == true

fun GTUStopTimeUpdate.isSameStop(
    rds: RouteDirectionStop?,
    stopSequence: Int,
    parseStopId: (String) -> String,
): Boolean {
    rds ?: return false // nothing to match on
    val gStuStopId = this.optStopIdNotEmpty
    val gStuStopSeq = this.optStopSequence
    if (gStuStopId == null && gStuStopSeq == null) return false // nothing to match on
    val sameOrUnspecifiedStopSequence = gStuStopSeq?.let {
        it == stopSequence
    } ?: true
    if (!sameOrUnspecifiedStopSequence) return false // no match
    val sameOrUnspecifiedStopId = gStuStopId?.let {
        rds.stop.isSameOriginalId(parseStopId(it))
    } ?: true
    return sameOrUnspecifiedStopId
}

fun GTFSRealTimeProvider.match(
    gSTU: GTUStopTimeUpdate,
    rdsCandidates: List<RouteDirectionStop>,
    findStopTimestamp: (rdsUUID: String, gSTUStopSeq: Int) -> Boolean
): Boolean =
    gSTU.match(rdsCandidates, findStopTimestamp, this::parseStopId)

fun GTUStopTimeUpdate.match(
    rdsCandidates: List<RouteDirectionStop>,
    findStopTimestamp: (rdsUUID: String, gSTUStopSeq: Int) -> Boolean,
    parseStopId: (String) -> String,
): Boolean {
    val gStuStopId = optStopIdNotEmpty
    val gStuStopSeq = optStopSequence
    if (gStuStopId == null && gStuStopSeq == null) return false // nothing to match on
    val gStuStopIdHash = gStuStopId?.let { parseStopId(gStuStopId) }
    return rdsCandidates.any { rds ->
        val sameOrUnspecifiedStopId = gStuStopIdHash?.let {
            rds.stop.isSameOriginalId(it)
        } ?: true
        if (!sameOrUnspecifiedStopId) return false
        gStuStopSeq ?: return@any true // stop ID was provided and matches
        return@any findStopTimestamp(rds.uuid, gStuStopSeq)
    }
}

// endregion stop time update
