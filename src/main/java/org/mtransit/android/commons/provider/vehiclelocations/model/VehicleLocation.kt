package org.mtransit.android.commons.provider.vehiclelocations.model

import android.content.ContentValues
import android.database.Cursor
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.getFloat
import org.mtransit.android.commons.getLong
import org.mtransit.android.commons.getString
import org.mtransit.android.commons.optFloat
import org.mtransit.android.commons.optInt
import org.mtransit.android.commons.optLong
import org.mtransit.android.commons.optString
import org.mtransit.android.commons.provider.vehiclelocations.VehicleLocationProviderContract

/**
 * See [VehicleLocationProviderContract]
 */
data class VehicleLocation(
    val id: Int? = null,
    val targetUUID: String, // route+direction or just route / routeTag / routeTag+dirTag
    val targetTripId: String?, // cleaned
    val lastUpdateInMs: Long,
    val maxValidityInMs: Long,
    //
    val vehicleId: String?, // not user visible
    val vehicleLabel: String?, // user visible
    val reportTimestamp: Long?,
    val latitude: Float,
    val longitude: Float,
    val bearing: Float?, // in degree
    val speed: Float?, // m/s OR km/h
) {

    companion object {
        @JvmStatic
        fun fromCursor(cursor: Cursor) = VehicleLocation(
            id = cursor.optInt(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_ID),
            targetUUID = cursor.getString(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_TARGET_UUID),
            targetTripId = cursor.optString(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_TARGET_TRIP_ID),
            lastUpdateInMs = cursor.getLong(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LAST_UPDATE),
            maxValidityInMs = cursor.getLong(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_MAX_VALIDITY_IN_MS),
            //
            vehicleId = cursor.optString(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_ID),
            vehicleLabel = cursor.optString(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_LABEL),
            reportTimestamp = cursor.optLong(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_REPORT_TIMESTAMP),
            latitude = cursor.getFloat(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LATITUDE),
            longitude = cursor.getFloat(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LONGITUDE),
            bearing = cursor.optFloat(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_BEARING),
            speed = cursor.optFloat(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_SPEED),
        )
    }

    fun toContentValues() = ContentValues().apply {
        id?.let { put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_ID, it) } // ELSE AUTO INCREMENT
        put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_TARGET_UUID, targetUUID)
        targetTripId?.let { put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_TARGET_TRIP_ID, it) }
        put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LAST_UPDATE, lastUpdateInMs)
        put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_MAX_VALIDITY_IN_MS, maxValidityInMs)
        //
        vehicleId?.let { put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_ID, it) }
        vehicleLabel?.let { put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_LABEL, it) }
        reportTimestamp?.let { put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_REPORT_TIMESTAMP, it) }
        put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LATITUDE, latitude)
        put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LONGITUDE, longitude)
        bearing?.let { put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_BEARING, it) }
        speed?.let { put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_SPEED, it) }
    }

    /**
     * see [VehicleLocationProviderContract.PROJECTION_VEHICLE_LOCATION]
     */
    val cursorRow: Array<Any?> get() = arrayOf(
        id,
        targetUUID,
        targetTripId,
        lastUpdateInMs,
        maxValidityInMs,
        //
        vehicleId,
        vehicleLabel,
        reportTimestamp,
        latitude,
        longitude,
        bearing,
        speed,
    )

    val useful: Boolean get() = this.lastUpdateInMs + this.maxValidityInMs >= TimeUtils.currentTimeMillis()
}
