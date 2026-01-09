package org.mtransit.android.commons.provider.vehiclelocations

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.provider.common.MTSQLiteOpenHelper
import org.mtransit.commons.sql.SQLCreateBuilder.Companion.getNew

abstract class VehicleLocationDbHelper(
    context: Context?,
    dbName: String,
    factory: SQLiteDatabase.CursorFactory?,
    dbVersion: Int,
) : MTSQLiteOpenHelper(
    context,
    dbName,
    factory,
    dbVersion,
) {
    companion object {
        const val T_VEHICLE_LOCATION = "vehicle_location"

        const val T_VEHICLE_LOCATION_K_ID: String = BaseColumns._ID
        const val T_VEHICLE_LOCATION_K_TARGET_UUID = "target"
        const val T_VEHICLE_LOCATION_K_TARGET_TRIP_ID = "target_trip_id"
        const val T_VEHICLE_LOCATION_K_LAST_UPDATE = "last_update"
        const val T_VEHICLE_LOCATION_K_MAX_VALIDITY_IN_MS = "max_validity"

        const val T_VEHICLE_LOCATION_K_VEHICLE_ID = "vehicle_id"
        const val T_VEHICLE_LOCATION_K_VEHICLE_LABEL = "vehicle_label"
        const val T_VEHICLE_LOCATION_K_VEHICLE_REPORT_TIMESTAMP = "report_timestamp"
        const val T_VEHICLE_LOCATION_K_LATITUDE = "latitude"
        const val T_VEHICLE_LOCATION_K_LONGITUDE = "longitude"
        const val T_VEHICLE_LOCATION_K_BEARING = "bearing"
        const val T_VEHICLE_LOCATION_K_SPEED = "speed"

        @JvmStatic
        fun getSqlCreateBuilder(table: String) = getNew(table)
            .appendColumn(T_VEHICLE_LOCATION_K_ID, SqlUtils.INT_PK_AUTO)
            .appendColumn(T_VEHICLE_LOCATION_K_TARGET_UUID, SqlUtils.TXT)
            .appendColumn(T_VEHICLE_LOCATION_K_TARGET_TRIP_ID, SqlUtils.TXT)
            .appendColumn(T_VEHICLE_LOCATION_K_LAST_UPDATE, SqlUtils.INT)
            .appendColumn(T_VEHICLE_LOCATION_K_MAX_VALIDITY_IN_MS, SqlUtils.INT)
            //
            .appendColumn(T_VEHICLE_LOCATION_K_VEHICLE_ID, SqlUtils.TXT)
            .appendColumn(T_VEHICLE_LOCATION_K_VEHICLE_LABEL, SqlUtils.TXT)
            .appendColumn(T_VEHICLE_LOCATION_K_VEHICLE_REPORT_TIMESTAMP, SqlUtils.INT)
            .appendColumn(T_VEHICLE_LOCATION_K_LATITUDE, SqlUtils.REAL)
            .appendColumn(T_VEHICLE_LOCATION_K_LONGITUDE, SqlUtils.REAL)
            .appendColumn(T_VEHICLE_LOCATION_K_BEARING, SqlUtils.REAL)
            .appendColumn(T_VEHICLE_LOCATION_K_SPEED, SqlUtils.REAL)
    }

    abstract val dbName: String
}