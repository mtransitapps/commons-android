package org.mtransit.android.commons.provider.vehiclelocations

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

object GTFSRealTimeVehiclePositionsProvider {

    val VEHICLE_LOCATION_MAX_VALIDITY_IN_MS = 1.hours.inWholeMilliseconds
    val VEHICLE_LOCATION_VALIDITY_IN_MS = 30.minutes.inWholeMilliseconds
    val VEHICLE_LOCATION_VALIDITY_IN_FOCUS_IN_MS = 1.minutes.inWholeMilliseconds
    val VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_MS = 10.minutes.inWholeMilliseconds
    val VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = 1.minutes.inWholeMilliseconds

    @JvmStatic
    fun getMinDurationBetweenRefreshInMs(inFocus: Boolean) =
        if (inFocus) VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS
        else VEHICLE_LOCATION_MIN_DURATION_BETWEEN_REFRESH_IN_MS

    @JvmStatic
    fun getValidityInMs(inFocus: Boolean) =
        if (inFocus) VEHICLE_LOCATION_VALIDITY_IN_FOCUS_IN_MS
        else VEHICLE_LOCATION_VALIDITY_IN_MS

    @JvmStatic
    val maxValidityInMs: Long get() = VEHICLE_LOCATION_MAX_VALIDITY_IN_MS
}