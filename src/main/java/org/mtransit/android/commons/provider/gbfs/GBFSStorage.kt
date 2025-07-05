package org.mtransit.android.commons.provider.gbfs

import android.content.Context
import androidx.annotation.WorkerThread
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.data.Area

object GBFSStorage {

    private const val PREF_KEY_AREA_MIN_LAT = "pGBFSAreaMinLat"
    private const val PREF_KEY_AREA_MAX_LAT = "pGBFSAreaMaxLat"
    private const val PREF_KEY_AREA_MIN_LON = "pGBFSAreaMinLon"
    private const val PREF_KEY_AREA_MAX_LON = "pGBFSAreaMaxLon"

    @JvmStatic
    fun getArea(context: Context): Area? {
        return Area(
            minLat = getAreaMinLat(context, null)?.toDoubleOrNull() ?: return null,
            maxLat = getAreaMaxLat(context, null)?.toDoubleOrNull() ?: return null,
            minLng = getAreaMinLon(context, null)?.toDoubleOrNull() ?: return null,
            maxLng = getAreaMaxLon(context, null)?.toDoubleOrNull() ?: return null,
        )
    }

    @JvmStatic
    fun saveArea(context: Context, area: Area?) {
        saveAreaMinLat(context, area?.minLat?.toString())
        saveAreaMaxLat(context, area?.maxLat?.toString())
        saveAreaMinLon(context, area?.minLng?.toString())
        saveAreaMaxLon(context, area?.maxLng?.toString())
    }

    @WorkerThread
    fun getAreaMinLat(context: Context, default: String?): String? {
        return PreferenceUtils.getPrefLcl(context, PREF_KEY_AREA_MIN_LAT, default)
    }

    @WorkerThread
    fun saveAreaMinLat(context: Context, minLat: String?) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_AREA_MIN_LAT, minLat)
    }

    @WorkerThread
    fun getAreaMaxLat(context: Context, default: String?): String? {
        return PreferenceUtils.getPrefLcl(context, PREF_KEY_AREA_MAX_LAT, default)
    }

    @WorkerThread
    fun saveAreaMaxLat(context: Context, maxLat: String?) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_AREA_MAX_LAT, maxLat)
    }

    @WorkerThread
    fun getAreaMinLon(context: Context, default: String?): String? {
        return PreferenceUtils.getPrefLcl(context, PREF_KEY_AREA_MIN_LON, default)
    }

    @WorkerThread
    fun saveAreaMinLon(context: Context, minLon: String?) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_AREA_MIN_LON, minLon)
    }

    @WorkerThread
    fun getAreaMaxLon(context: Context, default: String?): String? {
        return PreferenceUtils.getPrefLcl(context, PREF_KEY_AREA_MAX_LON, default)
    }

    @WorkerThread
    fun saveAreaMaxLon(context: Context, maxLon: String?) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_AREA_MAX_LON, maxLon)
    }
}