package org.mtransit.android.commons.provider.gtfs

import android.content.Context
import android.net.Uri
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Trip
import org.mtransit.android.commons.provider.GTFSProviderContract
import org.mtransit.android.commons.provider.poi.POIProviderContract

fun Context.getRDS(
    authority: String,
    routeId: Long,
    directionId: Long? = null,
): List<RouteDirectionStop>? = try {
    contentResolver.query(
        Uri.withAppendedPath(
            UriUtils.newContentUri(authority),
            POIProviderContract.POI_PATH
        ),
        GTFSProviderContract.PROJECTION_RDS_POI,
        POIProviderContract.Filter.toJSON(
            POIProviderContract.Filter.getNewSqlSelectionFilter(
                buildString {
                    append(
                        SqlUtils.getWhereEquals(
                            GTFSProviderContract.RouteDirectionStopColumns.T_ROUTE_K_ID,
                            routeId
                        )
                    )
                    directionId?.let {
                        append(SqlUtils.AND)
                        append(SqlUtils.getWhereEquals(GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_K_ID, it))
                    }
                })
        ).toString(),
        null,
        SqlUtils.getSortOrderAscending(GTFSProviderContract.RouteDirectionStopColumns.T_DIRECTION_STOPS_K_STOP_SEQUENCE)
    ).use { cursor ->
        buildList {
            if (cursor != null && cursor.count > 0) {
                if (cursor.moveToFirst()) {
                    do {
                        add(RouteDirectionStop.fromCursorStatic(cursor, authority))
                    } while (cursor.moveToNext())
                }
            }
        }
    }
} catch (e: Exception) {
    MTLog.w(this, e, "Error!")
    null
}

fun Context.getTripIds(authority: String, routeId: Long, directionId: Long? = null) =
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
