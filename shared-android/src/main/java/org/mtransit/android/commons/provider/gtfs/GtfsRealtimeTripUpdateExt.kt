package org.mtransit.android.commons.provider.gtfs

import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopIdNotEmpty
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopSequence
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate as GTUStopTimeUpdate

private const val LOG_TAG = "GtfsRealtimeTripUpdateExt"

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
    if (gStuStopId == null && gStuStopSeq == null) {
        MTLog.d(LOG_TAG, "isSameStop() > false (nothing to match on in ${this.toStringExt(short = true)})")
        return false // nothing to match on
    }
    return (gStuStopSeq == null || gStuStopSeq == stopSequence) // unspecified OR same stop sequence
        && (gStuStopId == null || rds.stop.isSameOriginalId(parseStopId(gStuStopId))) // unspecified OR same stop ID matches
}

fun GTFSRealTimeProvider.match(gSTU: GTUStopTimeUpdate, rdsCandidates: List<RouteDirectionStop>, findStopTimestamp: (String, Int) -> Boolean) =
    gSTU.match(rdsCandidates, findStopTimestamp, this::parseStopId)

fun GTUStopTimeUpdate.match(
    rdsCandidates: List<RouteDirectionStop>,
    findStopTimestamp: (rdsUUID: String, gSTUStopSeq: Int) -> Boolean,
    parseStopId: (String) -> String,
): Boolean {
    val gStuStopId = optStopIdNotEmpty
    val gStuStopSeq = optStopSequence
    if (gStuStopId == null && gStuStopSeq == null) {
        MTLog.d(LOG_TAG, "match() > false (nothing to match on in ${this.toStringExt(short = true)})")
        return false // nothing to match on
    }
    val gStuStopIdHash = gStuStopId?.let { parseStopId(gStuStopId) }
    return rdsCandidates.any { rds ->
        (gStuStopIdHash == null || rds.stop.isSameOriginalId(gStuStopIdHash)) // unspecified OR same stop ID matches
            && (gStuStopSeq == null || findStopTimestamp(rds.uuid, gStuStopSeq)) // unspecified OR matching timestamp stop sequence
    }
}

// endregion stop time update
