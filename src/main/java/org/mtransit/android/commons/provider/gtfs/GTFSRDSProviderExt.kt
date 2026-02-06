package org.mtransit.android.commons.provider.gtfs

import android.content.Context
import android.net.Uri
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.commons.provider.GTFSProviderContract

fun Context.getTripsIds(authority: String, routeId: Long, directionId: Long? = null) =
    getTrips(authority, routeId, directionId)?.map { it.tripId }

fun Context.getTrips(
    authority: String,
    routeId: Long,
    directionId: Long? = null,
): List<Trip>? = try {
    contentResolver.query(
        Uri.withAppendedPath(
            UriUtils.newContentUri(authority),
            GTFSProviderContract.TRIP_PATH
        ),
        GTFSProviderContract.PROJECTION_TRIP,
        buildString {
            append(
                SqlUtils.getWhereEquals(
                    GTFSProviderContract.TripColumns.T_TRIP_K_ROUTE_ID,
                    routeId
                )
            )
            directionId?.let {
                append(SqlUtils.AND)
                append(SqlUtils.getWhereEquals(GTFSProviderContract.TripColumns.T_TRIP_K_DIRECTION_ID, it))
            }
        },
        null,
        GTFSRDSProvider.TRIP_SORT_ORDER,
    ).use { cursor ->
        buildList {
            if (cursor != null && cursor.count > 0) {
                if (cursor.moveToFirst()) {
                    do {
                        add(Trip.fromCursor(cursor))
                    } while (cursor.moveToNext())
                }
            }
        }
    }
} catch (e: Exception) {
    MTLog.w(this, e, "Error!")
    null
}
