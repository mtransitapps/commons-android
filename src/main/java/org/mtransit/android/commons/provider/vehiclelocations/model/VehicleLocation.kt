package org.mtransit.android.commons.provider.vehiclelocations.model

import android.content.ContentValues
import android.database.Cursor
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.getFloat
import org.mtransit.android.commons.getLong
import org.mtransit.android.commons.getString
import org.mtransit.android.commons.optInt
import org.mtransit.android.commons.optLong
import org.mtransit.android.commons.optString
import org.mtransit.android.commons.provider.vehiclelocations.VehicleLocationProviderContract
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * See [VehicleLocationProviderContract]
 */
data class VehicleLocation(
    val id: Int? = null, // DB id
    val authority: String,
    val targetUUID: String, // route+direction or just route / routeTag / routeTag+dirTag
    val targetTripId: String?, // cleaned
    val lastUpdateInMs: Long,
    val maxValidityInMs: Long,
    //
    val vehicleId: String?, // not user visible
    val vehicleLabel: String?, // user visible
    val reportTimestamp: Duration?, // in SECONDS
    val latitude: Float,
    val longitude: Float,
    val bearingDegrees: Int?, // in degrees
    val speedMetersPerSecond: Int?, // in m/s
) {

    val reportTimestampSec: Long? get() = reportTimestamp?.inWholeSeconds

    @Suppress("unused")
    val reportTimestampMs: Long? get() = reportTimestamp?.inWholeMilliseconds

    val reportTimestampCountdown: Duration? get() = reportTimestamp?.let { (TimeUtils.currentTimeMillis().milliseconds - it) }

    private val _uid: String? = this.vehicleId ?: this.vehicleLabel

    val uuid: String? = _uid?.let { "${this.authority}-$it" }

    companion object {
        @JvmStatic
        fun fromCursor(cursor: Cursor, authority: String) = VehicleLocation(
            id = cursor.optInt(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_ID),
            authority = authority,
            targetUUID = cursor.getString(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_TARGET_UUID),
            targetTripId = cursor.optString(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_TARGET_TRIP_ID),
            lastUpdateInMs = cursor.getLong(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LAST_UPDATE),
            maxValidityInMs = cursor.getLong(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_MAX_VALIDITY_IN_MS),
            //
            vehicleId = cursor.optString(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_ID),
            vehicleLabel = cursor.optString(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_LABEL),
            reportTimestamp = cursor.optLong(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_REPORT_TIMESTAMP)?.seconds,
            latitude = cursor.getFloat(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LATITUDE),
            longitude = cursor.getFloat(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LONGITUDE),
            bearingDegrees = cursor.optInt(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_BEARING),
            speedMetersPerSecond = cursor.optInt(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_SPEED),
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
        reportTimestampSec?.let { put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_REPORT_TIMESTAMP, it) }
        put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LATITUDE, latitude)
        put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LONGITUDE, longitude)
        bearingDegrees?.let { put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_BEARING, it) }
        speedMetersPerSecond?.let { put(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_SPEED, it) }
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
        reportTimestampSec,
        latitude,
        longitude,
        bearingDegrees,
        speedMetersPerSecond,
    )

    val useful: Boolean get() = this.lastUpdateInMs + this.maxValidityInMs >= TimeUtils.currentTimeMillis()

    @Suppress("unused")
    fun toStringShort() = buildString {
        append("VLoc:{")
        vehicleId?.let { append("vId:").append(it).append(",") }
        vehicleLabel?.let { append("vLabel:").append(it).append(",") }
        targetTripId?.let { append("tTripId:").append(it).append(",") }
        targetUUID.let { append("tUUID:").append(it).append(",") }
        reportTimestampCountdown?.let { append("rCtSec:").append(it.inWholeSeconds).append(",") }
        append("}")
    }
}
