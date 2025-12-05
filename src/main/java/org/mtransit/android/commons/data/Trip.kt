package org.mtransit.android.commons.data

import android.database.Cursor
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.getLong
import org.mtransit.android.commons.getString
import org.mtransit.android.commons.provider.GTFSProviderContract

data class Trip(
    val tripId: String,
    val routeId: Long,
    val directionId: Long,
    val serviceId: String,
) : MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = Trip::class.java.simpleName

        @JvmStatic
        fun fromCursor(c: Cursor) = Trip(
            tripId = c.getString(GTFSProviderContract.TripColumns.T_TRIP_K_TRIP_ID),
            routeId = c.getLong(GTFSProviderContract.TripColumns.T_TRIP_K_ROUTE_ID),
            directionId = c.getLong(GTFSProviderContract.TripColumns.T_TRIP_K_DIRECTION_ID),
            serviceId = c.getString(GTFSProviderContract.TripColumns.T_TRIP_K_SERVICE_ID),
        )
    }

    override fun getLogTag() = LOG_TAG
}

