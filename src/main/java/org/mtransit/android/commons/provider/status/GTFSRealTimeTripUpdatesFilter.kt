package org.mtransit.android.commons.provider.status

import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStartDate
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStartTime
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripIdNotEmpty
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.status.GTFSRealTimeTripUpdatesProvider.LOG_TAG
import com.google.transit.realtime.GtfsRealtime.TripDescriptor as GTripDescriptor
import com.google.transit.realtime.GtfsRealtime.TripUpdate as GTripUpdate

fun List<GTripUpdate>.filterDuplicatesTrips() = buildList<GTripUpdate> {
    this@filterDuplicatesTrips.groupBy {
        it.optTrip?.optTripIdNotEmpty to (it.optTrip?.optStartDate to it.optTrip?.optStartTime)
    }.forEach { (_, gTripUpdates) ->
        if (gTripUpdates.isEmpty()) return@forEach
        if (gTripUpdates.size == 1) {
            val gTripUpdate1 = gTripUpdates.firstOrNull() ?: return@forEach
            add(gTripUpdate1)
            return@forEach
        }
        // PICK ONE
        // 1 - pick one w/o ModifiedTrip
        gTripUpdates.singleOrNull { it.optTrip?.hasModifiedTrip() == false }?.let { gTripUpdate ->
            MTLog.d(LOG_TAG, "filterDuplicatesTrips() > pick 1 out of ${gTripUpdates.size} w/o ModifiedTrip: ${gTripUpdate.toStringExt()}")
            add(gTripUpdate)
            return@forEach
        }
        // ELSE -> pick 1st one in the list
        val gTripUpdate1 = gTripUpdates.firstOrNull() ?: return@forEach
        MTLog.d(LOG_TAG, "filterDuplicatesTrips() > use 1st one out of ${gTripUpdates.size}: ${gTripUpdate1.toStringExt()}")
        add(gTripUpdate1)
    }
}