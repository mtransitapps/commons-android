package org.mtransit.android.commons.provider.vehiclelocations

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import org.mtransit.android.commons.provider.common.MTSQLiteOpenHelper

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
        const val T_VEHICLE_LOCATION_K_ID = BaseColumns._ID
    }

    abstract val dbName: String
}