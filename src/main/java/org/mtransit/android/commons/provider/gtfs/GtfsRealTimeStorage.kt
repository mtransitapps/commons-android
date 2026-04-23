package org.mtransit.android.commons.provider.gtfs

import android.content.Context
import androidx.annotation.WorkerThread
import org.mtransit.android.commons.PreferenceUtils

object GtfsRealTimeStorage {

    // region Trip Updates (status schedule)

    /**
     * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
     */
    private const val PREF_KEY_TRIP_UPDATE_LAST_UPDATE_MS = "pGTFSRealTimeTripUpdatesLastUpdate"

    @JvmStatic
    @WorkerThread
    fun getTripUpdateLastUpdateMs(context: Context, default: Long) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_TRIP_UPDATE_LAST_UPDATE_MS, default)

    @JvmStatic
    @WorkerThread
    fun saveTripUpdateLastUpdateMs(context: Context, lastUpdateInMs: Long) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_TRIP_UPDATE_LAST_UPDATE_MS, lastUpdateInMs)
    }

    /**
     * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
     */
    private const val PREF_KEY_TRIP_UPDATE_READ_FROM_SOURCE_MS = "pGTFSRealTimeTripUpdatesReadFromSource"

    @JvmStatic
    @WorkerThread
    fun getTripUpdateReadFromSourceMs(context: Context, default: Long) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_TRIP_UPDATE_READ_FROM_SOURCE_MS, default)

    @JvmStatic
    @WorkerThread
    fun saveTripUpdateReadFromSourceMs(context: Context, readFromSourceMs: Long?) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_TRIP_UPDATE_READ_FROM_SOURCE_MS, readFromSourceMs)
    }

    /**
     * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
     */
    private const val PREF_KEY_TRIP_UPDATE_LAST_UPDATE_CODE = "pGTFSRealTimeTripUpdateLastUpdateCode"

    @JvmStatic
    @WorkerThread
    fun getTripUpdateLastUpdateCode(context: Context, default: Int) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_TRIP_UPDATE_LAST_UPDATE_CODE, default)

    @JvmStatic
    @WorkerThread
    fun saveTripUpdateLastUpdateCode(context: Context, code: Int) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_TRIP_UPDATE_LAST_UPDATE_CODE, code)
    }

    /**
     * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
     */
    private const val PREF_KEY_TRIP_UPDATE_TRIP_IDS_OUT_OF_SYNC = "pGTFSRealTimeTripUpdateTripIdsOutOfSync"

    @JvmStatic
    @WorkerThread
    fun getTripUpdateTripIdsOutOfSync(context: Context, default: Boolean) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_TRIP_UPDATE_TRIP_IDS_OUT_OF_SYNC, default)

    @JvmStatic
    @WorkerThread
    fun saveTripUpdateTripIdsOutOfSync(context: Context, outOfSync: Boolean) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_TRIP_UPDATE_TRIP_IDS_OUT_OF_SYNC, outOfSync)
    }

    // end region

    // region Vehicle location

    /**
     * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
     */
    private const val PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_MS = "pGTFSRealTimeVehicleLocationsLastUpdate"

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
     * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
     */
    private const val PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_CODE = "pGTFSRealTimeVehicleLocationLastUpdateCode"

    @JvmStatic
    @WorkerThread
    fun getVehicleLocationLastUpdateCode(context: Context, default: Int) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_CODE, default)

    @JvmStatic
    @WorkerThread
    fun saveVehicleLocationLastUpdateCode(context: Context, code: Int) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_VEHICLE_LOCATION_LAST_UPDATE_CODE, code)
    }

    /**
     * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
     */
    private const val PREF_KEY_VEHICLE_LOCATION_TRIP_IDS_OUT_OF_SYNC = "pGTFSRealTimeVehicleLocationTripIdsOutOfSync"

    @JvmStatic
    @WorkerThread
    fun getVehicleLocationTripIdsOutOfSync(context: Context, default: Boolean) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_VEHICLE_LOCATION_TRIP_IDS_OUT_OF_SYNC, default)

    @JvmStatic
    @WorkerThread
    fun saveVehicleLocationTripIdsOutOfSync(context: Context, outOfSync: Boolean) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_VEHICLE_LOCATION_TRIP_IDS_OUT_OF_SYNC, outOfSync)
    }

    // endregion

    // region Service alerts

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

    /**
     * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
     */
    private const val PREF_KEY_SERVICE_UPDATE_TRIP_IDS_OUT_OF_SYNC = "pGTFSRealTimeServiceAlertsTripIdsOutOfSync"

    @JvmStatic
    @WorkerThread
    fun getServiceUpdateTripIdsOutOfSync(context: Context, default: Boolean) =
        PreferenceUtils.getPrefLcl(context, PREF_KEY_SERVICE_UPDATE_TRIP_IDS_OUT_OF_SYNC, default)

    @JvmStatic
    @WorkerThread
    fun saveServiceUpdateTripIdsOutOfSync(context: Context, outOfSync: Boolean) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_SERVICE_UPDATE_TRIP_IDS_OUT_OF_SYNC, outOfSync)
    }

    // endregion

}
