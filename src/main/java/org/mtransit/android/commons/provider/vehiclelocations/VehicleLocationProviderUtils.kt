package org.mtransit.android.commons.provider.vehiclelocations

import android.content.Context
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.provider.GTFSProvider

object VehicleLocationProviderUtils : MTLog.Loggable {

    private val LOG_TAG: String = VehicleLocationProviderUtils::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    private const val MAX_DISTANCE_PCT_ALLOWED = 10.0

    // Some transit agency RT feeds contain unrelated vehicle that are far away (like Metrolinx...)
    fun vehicleNearbyAgencyLocation(context: Context, vehicleLat: Float, vehicleLng: Float): Boolean {
        try {
            val agencyAreaMinLat = GTFSProvider.getAREA_MIN_LAT(context).toDoubleOrNull()
            val agencyAreaMaxLat = GTFSProvider.getAREA_MAX_LAT(context).toDoubleOrNull()
            val agencyAreaMinLng = GTFSProvider.getAREA_MIN_LNG(context).toDoubleOrNull()
            val agencyAreaMaxLng = GTFSProvider.getAREA_MAX_LNG(context).toDoubleOrNull()
            if (agencyAreaMaxLat == null || agencyAreaMinLat == null || agencyAreaMaxLng == null || agencyAreaMinLng == null) return true
            val agencyArea = Area(minLat = agencyAreaMinLat, maxLat = agencyAreaMaxLat, minLng = agencyAreaMinLng, maxLng = agencyAreaMaxLng)
            val vehicleLatD = vehicleLat.toString().toDouble()
            val vehicleLngD = vehicleLng.toString().toDouble()
            if (agencyArea.isInside(vehicleLatD, vehicleLngD)) {
                return true
            }
            val (agencyClosestLat, agencyClosestLng) = agencyArea.getNearestLatLng(vehicleLatD, vehicleLngD)
            val distanceFromAreaInMeters = LocationUtils.distanceToInMeters(vehicleLatD, vehicleLngD, agencyClosestLat, agencyClosestLng)
            val distanceFromAreaMinMax = LocationUtils.distanceToInMeters(agencyArea.minLat, agencyArea.minLng, agencyArea.maxLat, agencyArea.maxLng)
            val nearby = distanceFromAreaInMeters <= distanceFromAreaMinMax.div(MAX_DISTANCE_PCT_ALLOWED)
            if (!nearby) {
                MTLog.w(LOG_TAG, "Real-Time vehicle location is not nearby agency area: $distanceFromAreaInMeters m from agency area!")
            }
            return nearby
        } catch (e: Exception) {
            MTLog.w(LOG_TAG, e, "Error while validating vehicle location is nearby agency area")
            return true
        }
    }
}
