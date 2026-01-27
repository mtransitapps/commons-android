package org.mtransit.android.commons.provider.nextbus

import android.content.Context
import androidx.annotation.WorkerThread
import org.mtransit.android.commons.PreferenceUtils

object NextBusStorage {

    // region Vehicle location

    /**
     * Override if multiple [org.mtransit.android.commons.provider.NextBusProvider] implementations in same app.
     */
    private const val PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_MS = "pNextBusVehicleLocationsLastUpdate"

    @JvmStatic
    @WorkerThread
    fun getVehicleLocationLastUpdateMs(context: Context, default: Long) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_MS, default)

    @JvmStatic
    @WorkerThread
    fun saveVehicleLocationLastUpdateMs(context: Context, lastUpdateInMs: Long) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_MS, lastUpdateInMs)
    }

    /**
     * Override if multiple [org.mtransit.android.commons.provider.NextBusProvider] implementations in same app.
     */
    private const val PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_CODE = "pNextBusVehicleLocationLastUpdateCode"

    @JvmStatic
    @WorkerThread
    fun getVehicleLocationLastUpdateCode(context: Context, default: Int) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_CODE, default)

    @JvmStatic
    @WorkerThread
    fun saveVehicleLocationLastUpdateCode(context: Context, code: Int) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_CODE, code)
    }

    // endregion

    // region Service update (messages)

    /**
     * Override if multiple [org.mtransit.android.commons.provider.NextBusProvider] implementations in same app.
     */
    private const val PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS = "pNextBusMessagesLastUpdate"

    @JvmStatic
    @WorkerThread
    fun getServiceUpdateLastUpdateMs(context: Context, default: Long) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, default)

    @JvmStatic
    @WorkerThread
    fun saveServiceUpdateLastUpdateMs(context: Context, lastUpdateInMs: Long) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, lastUpdateInMs)
    }

    // endregion
}
