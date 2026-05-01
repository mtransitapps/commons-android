package org.mtransit.android.commons.provider.nextbus

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import org.mtransit.android.commons.PreferenceUtils
import androidx.core.content.edit

class NextBusStorage(
    context: Context,
) {

    companion object {
        private const val PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_MS = "pNextBusVehicleLocationsLastUpdate"
        private const val PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_CODE = "pNextBusVehicleLocationLastUpdateCode"

        private const val PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS = "pNextBusMessagesLastUpdate"
    }

    private val prefLcl: SharedPreferences by lazy { PreferenceUtils.getPrefLcl(context.applicationContext) }

    // region Vehicle location

    @WorkerThread
    fun getVehicleLocationLastUpdateMs(default: Long) =
        prefLcl.getLong(PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_MS, default)

    @WorkerThread
    fun saveVehicleLocationLastUpdateMs(lastUpdateInMs: Long) {
        prefLcl.edit { putLong(PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_MS, lastUpdateInMs) }
    }

    @WorkerThread
    fun getVehicleLocationLastUpdateCode(default: Int) =
        prefLcl.getInt(PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_CODE, default)

    @WorkerThread
    fun saveVehicleLocationLastUpdateCode(code: Int) {
        prefLcl.edit { putInt(PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_CODE, code) }
    }

    // endregion

    // region Service update (messages)

    @WorkerThread
    fun getServiceUpdateLastUpdateMs(default: Long) =
        prefLcl.getLong(PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, default)

    @WorkerThread
    fun saveServiceUpdateLastUpdateMs(lastUpdateInMs: Long?) {
        prefLcl.edit {
            lastUpdateInMs?.let { putLong(PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, it) }
                ?: remove(PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS)
        }
    }

    // endregion
}
