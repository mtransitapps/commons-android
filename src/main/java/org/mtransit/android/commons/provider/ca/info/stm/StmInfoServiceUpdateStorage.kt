package org.mtransit.android.commons.provider.ca.info.stm

import android.content.Context
import androidx.annotation.WorkerThread
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.provider.StmInfoApiProvider
import org.mtransit.android.commons.toMillis
import kotlin.time.Instant

object StmInfoServiceUpdateStorage {
    
    // region service update

    /**
     * Override if multiple [StmInfoApiProvider] implementations in same app.
     */
    private const val PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS = "pCaInfoStmServiceUpdatesLastUpdate"

    @JvmStatic
    @WorkerThread
    fun getServiceUpdateLastUpdate(context: Context, default: Instant) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, default.toMillis()).millisToInstant()

    @JvmStatic
    @WorkerThread
    fun saveServiceUpdateLastUpdate(context: Context, lastUpdate: Instant) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, lastUpdate.toMillis())
    }

    /**
     * Override if multiple [StmInfoApiProvider] implementations in same app.
     */
    private const val PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_CODE = "pCaInfoStmServiceUpdateLastUpdateCode"

    @JvmStatic
    @WorkerThread
    fun getServiceUpdateLastUpdateCode(context: Context, default: Int) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_CODE, default)

    @JvmStatic
    @WorkerThread
    fun saveServiceUpdateLastUpdateCode(context: Context, code: Int) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_CODE, code)
    }

    // endregion
}