package org.mtransit.android.commons.provider.vehiclelocations

import android.content.UriMatcher
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import androidx.core.database.sqlite.transaction
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.common.ContentProviderConstants
import org.mtransit.android.commons.provider.common.MTContentProvider
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation

abstract class VehicleLocationProvider : MTContentProvider(),
    VehicleLocationProviderContract {

    companion object {
        private val LOG_TAG: String = VehicleLocationProvider::class.java.simpleName

        fun getNewUriMatcher(authority: String) = UriMatcher(UriMatcher.NO_MATCH).apply {
            append(this, authority)
        }

        fun append(uriMatcher: UriMatcher, authority: String) {
            uriMatcher.addURI(authority, VehicleLocationProviderContract.PING_PATH, ContentProviderConstants.PING)
            uriMatcher.addURI(authority, VehicleLocationProviderContract.VEHICLE_LOCATION_PATH, ContentProviderConstants.VEHICLE_LOCATION)
        }

        fun <P : VehicleLocationProviderContract> P.getCachedVehicleLocationsS(targetUUIDs: Collection<String>): List<VehicleLocation>? {
            return getCachedVehicleLocationsS(
                this.contentUri,
                SqlUtils.getWhereInString(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_TARGET_UUID, targetUUIDs)
            )
        }

        @Suppress("unused")
        fun <P : VehicleLocationProviderContract> P.getCachedVehicleLocationsS(targetUUID: String): List<VehicleLocation>? {
            return this.getCachedVehicleLocationsS(
                this.contentUri,
                SqlUtils.getWhereEqualsString(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_TARGET_UUID, targetUUID)
            )
        }

        //@formatter:off
        @JvmStatic
        private val VEHICLE_LOCATION_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
            .appendTableColumn(VehicleLocationDbHelper.T_VEHICLE_LOCATION, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_ID, VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_ID)
            .appendTableColumn(VehicleLocationDbHelper.T_VEHICLE_LOCATION, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_TARGET_UUID, VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_TARGET_UUID)
            .appendTableColumn(VehicleLocationDbHelper.T_VEHICLE_LOCATION, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_TARGET_TRIP_ID, VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_TARGET_TRIP_ID)
            .appendTableColumn(VehicleLocationDbHelper.T_VEHICLE_LOCATION, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_LAST_UPDATE, VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LAST_UPDATE)
            .appendTableColumn(VehicleLocationDbHelper.T_VEHICLE_LOCATION, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_MAX_VALIDITY_IN_MS, VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_MAX_VALIDITY_IN_MS)

            .appendTableColumn(VehicleLocationDbHelper.T_VEHICLE_LOCATION, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_VEHICLE_ID, VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_ID)
            .appendTableColumn(VehicleLocationDbHelper.T_VEHICLE_LOCATION, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_VEHICLE_LABEL, VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_LABEL)
            .appendTableColumn(VehicleLocationDbHelper.T_VEHICLE_LOCATION, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_LATITUDE, VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LATITUDE)
            .appendTableColumn(VehicleLocationDbHelper.T_VEHICLE_LOCATION, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_LONGITUDE, VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LONGITUDE)
            .appendTableColumn(VehicleLocationDbHelper.T_VEHICLE_LOCATION, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_BEARING, VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_BEARING)
            .appendTableColumn(VehicleLocationDbHelper.T_VEHICLE_LOCATION, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_SPEED, VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_SPEED)
            .build()
        //@formatter:on

        private fun <P : VehicleLocationProviderContract> P.getCachedVehicleLocationsS(
            @Suppress("unused") uri: Uri?,
            selection: String?,
        ): List<VehicleLocation>? =
            try {
                SQLiteQueryBuilder()
                    .apply {
                        tables = dbTableName
                        projectionMap = VEHICLE_LOCATION_PROJECTION_MAP
                    }.query(
                        getReadDB(), VehicleLocationProviderContract.PROJECTION_VEHICLE_LOCATION, selection, null, null,
                        null, null, null
                    ).use { cursor ->
                        buildList {
                            if (cursor != null && cursor.count > 0) {
                                if (cursor.moveToFirst()) {
                                    do {
                                        add(VehicleLocation.fromCursor(cursor))
                                    } while (cursor.moveToNext())
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                MTLog.w(LOG_TAG, e, "Error!")
                null
            }

        private val VehicleLocationProviderContract.contentUri: Uri
            get() = Uri.withAppendedPath(this.authorityUri, VehicleLocationProviderContract.VEHICLE_LOCATION_PATH)

        @JvmStatic
        @Synchronized
        fun cacheVehicleLocationsS(provider: VehicleLocationProviderContract, newVehicleLocations: List<VehicleLocation>?): Int {
            var affectedRows = 0
            try {
                provider.getWriteDB().transaction {
                    newVehicleLocations?.forEach { vehicleLocation ->
                        insert(provider.dbTableName, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_ID, vehicleLocation.toContentValues())
                            .let { rowId ->
                                if (rowId > 0L) affectedRows++
                            }
                    }
                }
            } catch (e: Exception) {
                MTLog.w(LOG_TAG, e, "ERROR while applying batch update to the database!")
            }
            return affectedRows
        }

        @JvmStatic
        fun deleteAllCachedVehicleLocations(provider: VehicleLocationProviderContract): Boolean {
            var deletedRows = 0
            try {
                deletedRows = provider.getWriteDB().delete(provider.dbTableName, null, null)
            } catch (e: Exception) {
                MTLog.w(LOG_TAG, e, "Error while deleting ALL cached vehicle locations!")
            }
            return deletedRows > 0
        }

        @JvmStatic
        fun deleteCachedVehicleLocation(provider: VehicleLocationProviderContract, vehicleLocationId: Int?): Boolean {
            vehicleLocationId ?: return false
            val selection = SqlUtils.getWhereEquals(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_ID, vehicleLocationId)
            var deletedRows = 0
            try {
                deletedRows = provider.getWriteDB().delete(provider.dbTableName, selection, null)
            } catch (e: Exception) {
                MTLog.w(LOG_TAG, e, "Error while deleting cached vehicle location '%s'!", vehicleLocationId)
            }
            return deletedRows > 0
        }

        @JvmStatic
        fun purgeUselessCachedVehicleLocations(provider: VehicleLocationProviderContract): Boolean {
            val oldestLastUpdate = TimeUtils.currentTimeMillis() - provider.vehicleLocationMaxValidityInMs
            val selection = SqlUtils.getWhereInferior(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_LAST_UPDATE, oldestLastUpdate)
            var deletedRows = 0
            try {
                deletedRows = provider.getWriteDB().delete(provider.dbTableName, selection, null)
            } catch (e: Exception) {
                MTLog.w(LOG_TAG, e, "Error while deleting cached vehicle locations!")
            }
            return deletedRows > 0
        }
    }

    override fun getLogTag() = LOG_TAG
}

private val VehicleLocationProviderContract.dbTableName: String
    get() = this.vehicleLocationDbTableName
