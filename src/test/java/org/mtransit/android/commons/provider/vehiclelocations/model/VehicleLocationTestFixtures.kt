package org.mtransit.android.commons.provider.vehiclelocations.model

import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.toMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

fun makeVehicleLocation(
    id: Int? = null,
    authority: String = "authority",
    targetUUID: String = "uuid",
    targetTripId: String? = null,
    lastUpdate: Instant = TimeUtilsK.currentInstant(),
    maxValidity: Duration = 1.hours,
    //
    vehicleId: String? = null,
    vehicleLabel: String? = null,
    reportTimestamp: Instant? = lastUpdate - 1.seconds,
    latitude: Float = 1.0f,
    longitude: Float = 2.0f,
    bearingDegrees: Int? = 45,
    speedMetersPerSecond: Int? = null,
) = VehicleLocation(
    id = id,
    authority = authority,
    targetUUID = targetUUID,
    targetTripId = targetTripId,
    lastUpdateInMs = lastUpdate.toMillis(),
    maxValidityInMs = maxValidity.inWholeMilliseconds,
    //
    vehicleId = vehicleId,
    vehicleLabel = vehicleLabel,
    reportTimestamp = reportTimestamp,
    latitude = latitude,
    longitude = longitude,
    bearingDegrees = bearingDegrees,
    speedMetersPerSecond = speedMetersPerSecond,
)
