package org.mtransit.android.commons.provider.vehiclelocations.model

data class VehicleLocation(
    val id: Int,
    val targetTripId: String?,
    val targetUUID: String, // route+direction or just route / routeTag / routeTag+dirTag
    val lastUpdateInMs: Long,
    val maxValidityInMs: Long,
    //
    val vehicleId: String?, // not user visible
    val vehicleLabel: String?, // user visible
    val latitude: Double,
    val longitude: Double,
    val bearing: Float?, // in degree
    val speed: Float?, // m/s OR km/h
)
