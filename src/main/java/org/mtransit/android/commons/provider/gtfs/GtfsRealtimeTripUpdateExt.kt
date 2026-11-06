package org.mtransit.android.commons.provider.gtfs

import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopIdNotEmpty
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopSequence
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate as GTUStopTimeUpdate

object GtfsRealtimeTripUpdateExt {

    fun GTFSRealTimeProvider.match(
        gSTU: GTUStopTimeUpdate,
        rdsCandidates: List<RouteDirectionStop>,
        findStopTimestamp: (rdsUUID: String, gSTUStopSeq: Int) -> Schedule.Timestamp?,
    ): Boolean {
        val gSTUStopIDHash = gSTU.optStopIdNotEmpty?.let { parseStopId(it) } ?: return false // no match
        val gSTUStopSequence = gSTU.optStopSequence ?: return false // no match
        return rdsCandidates.any { rds ->
            rds.stop.isSameOriginalId(gSTUStopIDHash)
                && findStopTimestamp(rds.uuid, gSTUStopSequence) != null
        }
    }
}
