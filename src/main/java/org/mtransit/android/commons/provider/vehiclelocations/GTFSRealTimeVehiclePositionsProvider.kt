package org.mtransit.android.commons.provider.vehiclelocations

import android.content.Context
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
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

    @JvmStatic
    fun getCached(context: Context, provider: GTFSRealTimeProvider, vehicleLocationFilter: VehicleLocationProviderContract.Filter): List<VehicleLocation>? {
        return (vehicleLocationFilter.poi as? RouteDirectionStop)?.let { getCached(context, provider, it) }
            ?: vehicleLocationFilter.routeDirection?.let { getCached(context, provider, it) }
            ?: vehicleLocationFilter.route?.let { getCached(context, provider, it) }
    }

    private fun getCached(context: Context, provider: GTFSRealTimeProvider, rds: RouteDirectionStop): List<VehicleLocation>? {
        TODO()
    }

    private fun getCached(context: Context, provider: GTFSRealTimeProvider, rd: RouteDirection): List<VehicleLocation>? {
        TODO()
    }

    private fun getCached(context: Context, provider: GTFSRealTimeProvider, r: Route): List<VehicleLocation>? {
        TODO()
    }
}