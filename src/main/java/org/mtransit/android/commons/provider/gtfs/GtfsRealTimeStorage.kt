package org.mtransit.android.commons.provider.gtfs

import android.content.Context
import androidx.annotation.WorkerThread
import org.mtransit.android.commons.PreferenceUtils

object GtfsRealTimeStorage {

    /**
     * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
     */
    private const val PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS = "pGTFSRealTimeServiceAlertsLastUpdate"

    @JvmStatic
    @WorkerThread
    fun getServiceUpdateLastUpdateMs(context: Context, default: Long): Long {
        return PreferenceUtils.getPrefLcl(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, default)
    }

    @JvmStatic
    @WorkerThread
    fun saveServiceUpdateLastUpdateMs(context: Context, lastUpdateInMs: Long) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, lastUpdateInMs)
    }

    /**
     * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
     */
    private const val PREF_KEY_SERVICE_UPDATE_LANGUAGES = "pGTFSRealTimeServiceAlertsLanguages"

    @JvmStatic
    @WorkerThread
    fun getServiceUpdateLanguages(context: Context, default: Set<String>?): Set<String>? {
        return PreferenceUtils.getPrefLcl(context, PREF_KEY_SERVICE_UPDATE_LANGUAGES, default)
    }

    @JvmStatic
    @WorkerThread
    fun saveServiceUpdateLanguages(context: Context, languages: Set<String>?) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, languages)
    }
}
