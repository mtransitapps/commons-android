package org.mtransit.android.commons.provider.vehiclelocations

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VehicleLocationProviderUtilsTest {

    private val distanceToInMeters: (startLat: Double, startLng: Double, endLat: Double, endLng: Double) -> Float =
        { lat1: Double, lon1: Double, lat2: Double, lon2: Double ->
            val earthRadiusKm = 6371.0 // Use 3958.8 for miles instead

            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)

            val originLatRad = Math.toRadians(lat1)
            val destinationLatRad = Math.toRadians(lat2)

            val a = sin(dLat / 2).pow(2) +
                    sin(dLon / 2).pow(2) *
                    cos(originLatRad) * cos(destinationLatRad)

            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            (earthRadiusKm * c).toFloat() // Returns distance in kilometers
        }

    @Test
    fun test_vehicleNearbyAgencyLocation() {
        VehicleLocationProviderUtils.vehicleNearbyAgencyLocation(
            agencyAreaMinLat = null,
            agencyAreaMaxLat = null,
            agencyAreaMinLng = null,
            agencyAreaMaxLng = null,
            vehicleLat = 1.0,
            vehicleLng = 2.0,
            distanceToInMeters = distanceToInMeters,
        ).let { result ->
            assertNull(result)
        }
        VehicleLocationProviderUtils.vehicleNearbyAgencyLocation(
            agencyAreaMinLat = -10.0,
            agencyAreaMaxLat = 10.0,
            agencyAreaMinLng = -20.0,
            agencyAreaMaxLng = 20.0,
            vehicleLat = null,
            vehicleLng = null,
            distanceToInMeters = distanceToInMeters,
        ).let { result ->
            assertNull(result)
        }
        VehicleLocationProviderUtils.vehicleNearbyAgencyLocation(
            agencyAreaMinLat = -10.0,
            agencyAreaMaxLat = 10.0,
            agencyAreaMinLng = -20.0,
            agencyAreaMaxLng = 20.0,
            vehicleLat = 1.0,
            vehicleLng = 2.0,
            distanceToInMeters = distanceToInMeters,
        ).let { result ->
            assertEquals(true, result)
        }
        VehicleLocationProviderUtils.vehicleNearbyAgencyLocation(
            agencyAreaMinLat = -10.0,
            agencyAreaMaxLat = 10.0,
            agencyAreaMinLng = -20.0,
            agencyAreaMaxLng = 20.0,
            vehicleLat = 10.0,
            vehicleLng = 20.0,
            distanceToInMeters = distanceToInMeters,
        ).let { result ->
            assertEquals(true, result)
        }
        VehicleLocationProviderUtils.vehicleNearbyAgencyLocation(
            agencyAreaMinLat = -10.0,
            agencyAreaMaxLat = 10.0,
            agencyAreaMinLng = -20.0,
            agencyAreaMaxLng = 20.0,
            vehicleLat = 11.0,
            vehicleLng = 20.0,
            distanceToInMeters = distanceToInMeters,
        ).let { result ->
            assertEquals(true, result)
        }
        VehicleLocationProviderUtils.vehicleNearbyAgencyLocation(
            agencyAreaMinLat = -10.0,
            agencyAreaMaxLat = 10.0,
            agencyAreaMinLng = -20.0,
            agencyAreaMaxLng = 20.0,
            vehicleLat = 10.0,
            vehicleLng = 21.0,
            distanceToInMeters = distanceToInMeters,
        ).let { result ->
            assertEquals(true, result)
        }
        VehicleLocationProviderUtils.vehicleNearbyAgencyLocation(
            agencyAreaMinLat = -10.0,
            agencyAreaMaxLat = 10.0,
            agencyAreaMinLng = -20.0,
            agencyAreaMaxLng = 20.0,
            vehicleLat = 20.0,
            vehicleLng = 30.0,
            distanceToInMeters = distanceToInMeters,
        ).let { result ->
            assertEquals(false, result)
        }
    }
}
