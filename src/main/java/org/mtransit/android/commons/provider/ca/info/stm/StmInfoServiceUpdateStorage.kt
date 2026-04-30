package org.mtransit.android.commons.provider.ca.info.stm

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.toMillis
import kotlin.time.Instant

class StmInfoServiceUpdateStorage(
    context: Context,
) {

    companion object {
        private const val PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS = "pCaInfoStmServiceUpdatesLastUpdate"
        private const val PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_CODE = "pCaInfoStmServiceUpdateLastUpdateCode"
    }

    private val prefLcl: SharedPreferences by lazy { PreferenceUtils.getPrefLcl(context) }

    // region service update

    @WorkerThread
    fun getServiceUpdateLastUpdate(default: Instant) =
        prefLcl.getLong(PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, default.toMillis()).millisToInstant()

    @WorkerThread
    fun saveServiceUpdateLastUpdate(lastUpdate: Instant) {
        prefLcl.edit { putLong(PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, lastUpdate.toMillis()) }
    }

    @WorkerThread
    fun getServiceUpdateLastUpdateCode(default: Int) =
        prefLcl.getInt(PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_CODE, default)

    @WorkerThread
    fun saveServiceUpdateLastUpdateCode(code: Int) {
        prefLcl.edit { putInt(PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_CODE, code) }
    }

    // endregion
}
