package org.mtransit.android.commons.provider.gbfs

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.data.Area
import org.mtransit.android.commons.provider.bike.BikeStationDbHelper

class GBFSStorage(
    context: Context
) {

    companion object {

        private const val PREF_KEY_AREA_MIN_LAT = "pGBFSAreaMinLat"
        private const val PREF_KEY_AREA_MAX_LAT = "pGBFSAreaMaxLat"
        private const val PREF_KEY_AREA_MIN_LON = "pGBFSAreaMinLon"
        private const val PREF_KEY_AREA_MAX_LON = "pGBFSAreaMaxLon"

        private const val PREF_KEY_LAST_UPDATE_MS = BikeStationDbHelper.PREF_KEY_LAST_UPDATE_MS
        private const val PREF_KEY_STATUS_LAST_UPDATE_MS = BikeStationDbHelper.PREF_KEY_STATUS_LAST_UPDATE_MS
    }

    private val prefLcl: SharedPreferences by lazy { PreferenceUtils.getPrefLcl(context.applicationContext) }

    @WorkerThread
    fun getArea(): Area? {
        return Area(
            minLat = getAreaMinLat(null)?.toDoubleOrNull() ?: return null,
            maxLat = getAreaMaxLat(null)?.toDoubleOrNull() ?: return null,
            minLng = getAreaMinLon(null)?.toDoubleOrNull() ?: return null,
            maxLng = getAreaMaxLon(null)?.toDoubleOrNull() ?: return null,
        )
    }

    @WorkerThread
    fun saveArea(area: Area?) {
        saveAreaMinLat(area?.minLat?.toString())
        saveAreaMaxLat(area?.maxLat?.toString())
        saveAreaMinLon(area?.minLng?.toString())
        saveAreaMaxLon(area?.maxLng?.toString())
    }

    @WorkerThread
    fun getAreaMinLat(default: String?) =
        prefLcl.getString(PREF_KEY_AREA_MIN_LAT, default)

    @WorkerThread
    fun saveAreaMinLat(minLat: String?) {
        prefLcl.edit { putString(PREF_KEY_AREA_MIN_LAT, minLat) }
    }

    @WorkerThread
    fun getAreaMaxLat(default: String?) =
        prefLcl.getString(PREF_KEY_AREA_MAX_LAT, default)

    @WorkerThread
    fun saveAreaMaxLat(maxLat: String?) {
        prefLcl.edit { putString(PREF_KEY_AREA_MAX_LAT, maxLat) }
    }

    @WorkerThread
    fun getAreaMinLon(default: String?) =
        prefLcl.getString(PREF_KEY_AREA_MIN_LON, default)

    @WorkerThread
    fun saveAreaMinLon(minLon: String?) {
        prefLcl.edit { putString(PREF_KEY_AREA_MIN_LON, minLon) }
    }

    @WorkerThread
    fun getAreaMaxLon(default: String?) =
        prefLcl.getString(PREF_KEY_AREA_MAX_LON, default)

    @WorkerThread
    fun saveAreaMaxLon(maxLon: String?) {
        prefLcl.edit { putString(PREF_KEY_AREA_MAX_LON, maxLon) }
    }

    @WorkerThread
    fun getLastUpdateInMs() = // POI
        prefLcl.getLong(PREF_KEY_LAST_UPDATE_MS, 0L)

    @WorkerThread
    fun setLastUpdateInMs(newLastUpdateInMs: Long) { // POI
        prefLcl.edit { putLong(PREF_KEY_LAST_UPDATE_MS, newLastUpdateInMs) }
    }

    @WorkerThread
    fun getLastUpdateStatusInMs() =
        prefLcl.getLong(PREF_KEY_STATUS_LAST_UPDATE_MS, 0L)

    @WorkerThread
    fun setLastUpdateStatusInMs(newLastUpdateStatusInMs: Long) {
        prefLcl.edit { putLong(PREF_KEY_STATUS_LAST_UPDATE_MS, newLastUpdateStatusInMs) }
    }
}
