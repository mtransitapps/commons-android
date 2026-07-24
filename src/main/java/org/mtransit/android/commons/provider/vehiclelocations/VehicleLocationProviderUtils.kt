package org.mtransit.android.commons.provider.vehiclelocations

import android.content.Context
import androidx.annotation.VisibleForTesting
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.provider.GTFSProvider

object VehicleLocationProviderUtils : MTLog.Loggable {

    private val LOG_TAG: String = VehicleLocationProviderUtils::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    private const val MAX_DISTANCE_PCT_ALLOWED = 10.0

    fun vehicleNearbyAgencyLocation(context: Context, vehicleLat: Float, vehicleLng: Float) =
        vehicleNearbyAgencyLocation(
            agencyAreaMinLat = GTFSProvider.getAREA_MIN_LAT(context).toDoubleOrNull(),
            agencyAreaMaxLat = GTFSProvider.getAREA_MAX_LAT(context).toDoubleOrNull(),
            agencyAreaMinLng = GTFSProvider.getAREA_MIN_LNG(context).toDoubleOrNull(),
            agencyAreaMaxLng = GTFSProvider.getAREA_MAX_LNG(context).toDoubleOrNull(),
            vehicleLat = vehicleLat.toString().toDoubleOrNull(),
            vehicleLng = vehicleLng.toString().toDoubleOrNull(),
            distanceToInMeters = { startLat: Double, startLng: Double, endLat: Double, endLng: Double ->
                LocationUtils.distanceToInMeters(startLat, startLng, endLat, endLng)
            }
        )

    // Some transit agency RT feeds contain unrelated vehicle that are far away (like Metrolinx...)
    @VisibleForTesting
    internal fun vehicleNearbyAgencyLocation(
        agencyAreaMinLat: Double?,
        agencyAreaMaxLat: Double?,
        agencyAreaMinLng: Double?,
        agencyAreaMaxLng: Double?,
        vehicleLat: Double?,
        vehicleLng: Double?,
        distanceToInMeters: (startLat: Double, startLng: Double, endLat: Double, endLng: Double) -> Float,
    ): Boolean? {
        try {
            if (vehicleLat == null || vehicleLng == null) return null
            if (agencyAreaMaxLat == null || agencyAreaMinLat == null || agencyAreaMaxLng == null || agencyAreaMinLng == null) return null
            val agencyArea = Area(minLat = agencyAreaMinLat, maxLat = agencyAreaMaxLat, minLng = agencyAreaMinLng, maxLng = agencyAreaMaxLng)
            if (agencyArea.isInside(vehicleLat, vehicleLng)) {
                return true
            }
            val (agencyClosestLat, agencyClosestLng) = agencyArea.getNearestLatLng(vehicleLat, vehicleLng)
            val distanceFromAreaInMeters = distanceToInMeters(vehicleLat, vehicleLng, agencyClosestLat, agencyClosestLng)
            val distanceFromAreaMinMax = distanceToInMeters(agencyArea.minLat, agencyArea.minLng, agencyArea.maxLat, agencyArea.maxLng)
            val nearby = distanceFromAreaInMeters <= distanceFromAreaMinMax.div(MAX_DISTANCE_PCT_ALLOWED)
            if (!nearby) {
                MTLog.w(LOG_TAG, "Real-Time vehicle location is not nearby agency area: $distanceFromAreaInMeters m from agency area!")
            }
            return nearby
        } catch (e: Exception) {
            MTLog.w(LOG_TAG, e, "Error while validating vehicle location is nearby agency area")
            return null
        }
    }
}
