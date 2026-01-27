package org.mtransit.android.commons.provider.vehiclelocations

import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import androidx.core.database.sqlite.transaction
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.common.ContentProviderConstants
import org.mtransit.android.commons.provider.common.MTContentProvider
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation

abstract class VehicleLocationProvider : MTContentProvider(),
    VehicleLocationProviderContract {

    companion object {
        private val LOG_TAG: String = VehicleLocationProvider::class.java.simpleName

        fun getNewUriMatcher(authority: String) = UriMatcher(UriMatcher.NO_MATCH).apply {
            append(authority)
        }

        @JvmStatic
        fun UriMatcher.append(authority: String) {
            addURI(authority, VehicleLocationProviderContract.PING_PATH, ContentProviderConstants.PING)
            addURI(authority, VehicleLocationProviderContract.VEHICLE_LOCATION_PATH, ContentProviderConstants.VEHICLE_LOCATION)
        }

        @JvmStatic
        fun <P : VehicleLocationProviderContract> P.queryS(uri: Uri, selection: String?): Cursor? {
            return when (getURI_MATCHER().match(uri)) {
                ContentProviderConstants.PING -> ContentProviderConstants.EMPTY_CURSOR // empty cursor = processed
                ContentProviderConstants.VEHICLE_LOCATION -> getVehicleLocations(selection)
                else -> null // not processed
            }
        }

        private fun <P : VehicleLocationProviderContract> P.getVehicleLocations(selection: String?): Cursor {
            val filter = VehicleLocationProviderContract.Filter.fromJSONString(selection) ?: run {
                MTLog.w(LOG_TAG, "Error while parsing vehicle location filter! (%s)", selection)
                return getVehicleLocationCursor(null)
            }
            val nowInMs = TimeUtils.currentTimeMillis()
            val cachedVehicleLocations = getCachedVehicleLocations(filter)?.toMutableList()
            var purgeNecessary = false
            if (cachedVehicleLocations != null) {
                val iterator = cachedVehicleLocations.iterator()
                while (iterator.hasNext()) {
                    val cachedVehicleLocation = iterator.next()
                    if (cachedVehicleLocation.lastUpdateInMs + vehicleLocationMaxValidityInMs < nowInMs) {
                        iterator.remove()
                        purgeNecessary = true
                    }
                }
            }
            if (purgeNecessary) {
                purgeUselessCachedVehicleLocations()
            }
            if (cachedVehicleLocations != null) {
                val it = cachedVehicleLocations.iterator()
                while (it.hasNext()) {
                    val cachedVehicleLocation = it.next()
                    if (!cachedVehicleLocation.useful) {
                        cachedVehicleLocation.id?.let {
                            deleteCachedVehicleLocation(it)
                        }
                        it.remove()
                    }
                }
            }
            if (filter.cacheOnlyOrDefault) {
                if (cachedVehicleLocations.isNullOrEmpty()) {
                    MTLog.w(LOG_TAG, "getVehicleLocations() > No useful cache found!")
                }
                return getVehicleLocationCursor(cachedVehicleLocations)
            }
            val cacheValidityInMs = getVehicleLocationValidityInMs(filter.inFocusOrDefault)
            // TODO filter cache validity override like service update?
            var loadNewVehicleLocations = false
            if (cachedVehicleLocations.isNullOrEmpty()) {
                loadNewVehicleLocations = true
            } else {
                for (cachedVehicleLocation in cachedVehicleLocations) {
                    if (cachedVehicleLocation.lastUpdateInMs + cacheValidityInMs < nowInMs) {
                        loadNewVehicleLocations = true
                        break
                    }
                }
            }
            if (loadNewVehicleLocations) {
                val newVehicleLocations = getNewVehicleLocations(filter)
                if (!newVehicleLocations.isNullOrEmpty()) {
                    return getVehicleLocationCursor(newVehicleLocations)
                }
            }
            if (cachedVehicleLocations.isNullOrEmpty()) {
                MTLog.w(LOG_TAG, "getVehicleLocations() > no cache & no data from provider for %s!", filter.uuid)
            }
            return getVehicleLocationCursor(cachedVehicleLocations)
        }

        fun getVehicleLocationCursor(vehicleLocations: List<VehicleLocation>?): Cursor {
            if (vehicleLocations == null) {
                return ContentProviderConstants.EMPTY_CURSOR
            }
            return MatrixCursor(VehicleLocationProviderContract.PROJECTION_VEHICLE_LOCATION)
                .apply {
                    vehicleLocations.forEach { vehicleLocation ->
                        addRow(vehicleLocation.cursorRow)
                    }
                }
        }

        @JvmStatic
        fun <P : VehicleLocationProviderContract> P.getTypeS(uri: Uri): String? {
            return when (getURI_MATCHER().match(uri)) {
                ContentProviderConstants.PING,
                ContentProviderConstants.VEHICLE_LOCATION -> StringUtils.EMPTY // empty string = processed
                else -> null // not processed
            }
        }

        fun <P : VehicleLocationProviderContract> P.getCachedVehicleLocationsS(targetUUIDs: Collection<String>, tripIds: List<String>? = null): List<VehicleLocation>? {
            return getCachedVehicleLocationsS(
                this.contentUri,
                buildString {
                    append(SqlUtils.getWhereInString(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_TARGET_UUID, targetUUIDs))
                    tripIds?.takeIf { it.isNotEmpty() }?.let {
                        append(SqlUtils.AND)
                        append(SqlUtils.getWhereInString(VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_TARGET_TRIP_ID, it))
                    }
                }
            )
        }

        @Suppress("unused")
        fun <P : VehicleLocationProviderContract> P.getCachedVehicleLocationsS(targetUUID: String): List<VehicleLocation>? {
            return getCachedVehicleLocationsS(
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
            .appendTableColumn(VehicleLocationDbHelper.T_VEHICLE_LOCATION, VehicleLocationDbHelper.T_VEHICLE_LOCATION_K_VEHICLE_REPORT_TIMESTAMP, VehicleLocationProviderContract.Columns.T_VEHICLE_LOCATION_K_VEHICLE_REPORT_TIMESTAMP)
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
                                        add(VehicleLocation.fromCursor(cursor, this@getCachedVehicleLocationsS.authority))
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
