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
    fun getServiceUpdateLastUpdateMs(context: Context, default: Long) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, default)

    @JvmStatic
    @WorkerThread
    fun saveServiceUpdateLastUpdateMs(context: Context, lastUpdateInMs: Long) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, lastUpdateInMs)
    }

    /**
     * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
     */
    private const val PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_CODE = "pGTFSRealTimeServiceAlertsLastUpdateCode"

    @JvmStatic
    @WorkerThread
    fun getServiceUpdateLastUpdateCode(context: Context, default: Int) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_CODE, default)

    @JvmStatic
    @WorkerThread
    fun saveServiceUpdateLastUpdateCode(context: Context, code: Int) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_CODE, code)
    }

    /**
     * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
     */
    private const val PREF_KEY_SERVICE_UPDATE_LANGUAGES = "pGTFSRealTimeServiceAlertsLanguages"

    @JvmStatic
    @WorkerThread
    fun getServiceUpdateLanguages(context: Context, default: Set<String>?) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_SERVICE_UPDATE_LANGUAGES, default)

    @JvmStatic
    @WorkerThread
    fun saveServiceUpdateLanguages(context: Context, languages: Set<String>?) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_SERVICE_UPDATE_LANGUAGES, languages)
    }
}
