package org.mtransit.android.commons.provider.gtfs

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import org.mtransit.android.commons.PreferenceUtils

class GtfsRealTimeStorage(
    context: Context,
) {

    companion object {
        private const val PREF_KEY_TRIP_UPDATE_LAST_UPDATE_MS = "pGTFSRealTimeTripUpdatesLastUpdate"
        private const val PREF_KEY_TRIP_UPDATE_READ_FROM_SOURCE_MS = "pGTFSRealTimeTripUpdatesReadFromSource"
        private const val PREF_KEY_TRIP_UPDATE_LAST_UPDATE_CODE = "pGTFSRealTimeTripUpdateLastUpdateCode"
        private const val PREF_KEY_TRIP_UPDATE_TRIP_IDS_OUT_OF_SYNC = "pGTFSRealTimeTripUpdateTripIdsOutOfSync"

        private const val PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_MS = "pGTFSRealTimeVehicleLocationsLastUpdate"
        private const val PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_CODE = "pGTFSRealTimeVehicleLocationLastUpdateCode"
        private const val PREF_KEY_VEHICLE_LOCATION_TRIP_IDS_OUT_OF_SYNC = "pGTFSRealTimeVehicleLocationTripIdsOutOfSync"

        private const val PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS = "pGTFSRealTimeServiceAlertsLastUpdate"
        private const val PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_CODE = "pGTFSRealTimeServiceAlertsLastUpdateCode"
        private const val PREF_KEY_SERVICE_UPDATE_LANGUAGES = "pGTFSRealTimeServiceAlertsLanguages"
        private const val PREF_KEY_SERVICE_UPDATE_TRIP_IDS_OUT_OF_SYNC = "pGTFSRealTimeServiceAlertsTripIdsOutOfSync"
    }

    private val prefLcl: SharedPreferences by lazy { PreferenceUtils.getPrefLcl(context) }

    // region Trip Updates (status schedule)

    @WorkerThread
    fun getTripUpdateLastUpdateMs(default: Long) =
        prefLcl.getLong(PREF_KEY_TRIP_UPDATE_LAST_UPDATE_MS, default)

    @WorkerThread
    fun saveTripUpdateLastUpdateMs(lastUpdateInMs: Long) {
        prefLcl.edit { putLong(PREF_KEY_TRIP_UPDATE_LAST_UPDATE_MS, lastUpdateInMs) }
    }

    @WorkerThread
    fun getTripUpdateReadFromSourceMs(default: Long) =
        prefLcl.getLong(PREF_KEY_TRIP_UPDATE_READ_FROM_SOURCE_MS, default)

    @WorkerThread
    fun saveTripUpdateReadFromSourceMs(readFromSourceMs: Long?) {
        prefLcl.edit { putLong(PREF_KEY_TRIP_UPDATE_READ_FROM_SOURCE_MS, readFromSourceMs ?: 0) }
    }


    @WorkerThread
    fun getTripUpdateLastUpdateCode(default: Int) =
        prefLcl.getInt(PREF_KEY_TRIP_UPDATE_LAST_UPDATE_CODE, default)

    @WorkerThread
    fun saveTripUpdateLastUpdateCode(code: Int) {
        prefLcl.edit { putInt(PREF_KEY_TRIP_UPDATE_LAST_UPDATE_CODE, code) }
    }

    @Suppress("unused") // TODO
    @WorkerThread
    fun getTripUpdateTripIdsOutOfSync(default: Boolean) =
        prefLcl.getBoolean(PREF_KEY_TRIP_UPDATE_TRIP_IDS_OUT_OF_SYNC, default)

    @Suppress("unused") // TODO
    @WorkerThread
    fun saveTripUpdateTripIdsOutOfSync(outOfSync: Boolean) {
        prefLcl.edit { putBoolean(PREF_KEY_TRIP_UPDATE_TRIP_IDS_OUT_OF_SYNC, outOfSync) }
    }

    // end region

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

    @WorkerThread
    fun getVehicleLocationTripIdsOutOfSync(default: Boolean) =
        prefLcl.getBoolean(PREF_KEY_VEHICLE_LOCATION_TRIP_IDS_OUT_OF_SYNC, default)

    @WorkerThread
    fun saveVehicleLocationTripIdsOutOfSync(outOfSync: Boolean) {
        prefLcl.edit { putBoolean(PREF_KEY_VEHICLE_LOCATION_TRIP_IDS_OUT_OF_SYNC, outOfSync) }
    }

    // endregion

    // region Service alerts

    @WorkerThread
    fun getServiceUpdateLastUpdateMs(default: Long) =
        prefLcl.getLong(PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, default)

    @WorkerThread
    fun saveServiceUpdateLastUpdateMs(lastUpdateInMs: Long) {
        prefLcl.edit { putLong(PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_MS, lastUpdateInMs) }
    }


    @WorkerThread
    fun getServiceUpdateLastUpdateCode(default: Int) =
        prefLcl.getInt(PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_CODE, default)

    @WorkerThread
    fun saveServiceUpdateLastUpdateCode(code: Int) {
        prefLcl.edit { putInt(PREF_KEY_SERVICE_UPDATE_LAST_UPDATE_CODE, code) }
    }

    @WorkerThread
    fun getServiceUpdateLanguages(default: Set<String>?) =
        prefLcl.getStringSet(PREF_KEY_SERVICE_UPDATE_LANGUAGES, default)

    @WorkerThread
    fun saveServiceUpdateLanguages(languages: Set<String>?) {
        prefLcl.edit {
            if (languages == null) {
                remove(PREF_KEY_SERVICE_UPDATE_LANGUAGES)
            } else {
                putStringSet(PREF_KEY_SERVICE_UPDATE_LANGUAGES, languages)
            }
        }
    }

    @WorkerThread
    fun getServiceUpdateTripIdsOutOfSync(default: Boolean) =
        prefLcl.getBoolean(PREF_KEY_SERVICE_UPDATE_TRIP_IDS_OUT_OF_SYNC, default)

    @WorkerThread
    fun saveServiceUpdateTripIdsOutOfSync(outOfSync: Boolean) {
        prefLcl.edit { putBoolean(PREF_KEY_SERVICE_UPDATE_TRIP_IDS_OUT_OF_SYNC, outOfSync) }
    }

    // endregion

}
