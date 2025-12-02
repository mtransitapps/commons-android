package org.mtransit.android.commons.provider.vehiclelocations

import android.content.UriMatcher
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

        // TODO read DB here

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
