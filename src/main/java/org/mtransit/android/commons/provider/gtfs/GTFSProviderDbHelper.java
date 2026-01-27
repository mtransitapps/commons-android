package org.mtransit.android.commons.provider.gtfs;

import static org.mtransit.android.commons.provider.gtfs.GTFSProviderDBHelperUtils.initDbTableWithRetry;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NotificationUtils;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.provider.common.MTSQLiteOpenHelper;
import org.mtransit.android.commons.provider.status.StatusProvider;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.GTFSCommons;
import org.mtransit.commons.sql.SQLUtils;

import java.util.HashMap;
import java.util.Map;

public class GTFSProviderDbHelper extends MTSQLiteOpenHelper {

	private static final String LOG_TAG = GTFSProviderDbHelper.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} implementations in same app.
	 */
	public static final String DB_NAME = "gtfs_rts.db"; // do not change to avoid breaking compat w/ old modules

	public static final String T_STRINGS = GTFSCommons.T_STRINGS;
	public static final String T_STRINGS_K_ID = GTFSCommons.T_STRINGS_K_ID;
	public static final String T_STRINGS_K_STRING = GTFSCommons.T_STRINGS_K_STRING;
	private static final String T_STRINGS_SQL_CREATE = GTFSCommons.getT_STRINGS_SQL_CREATE();
	private static final String T_STRINGS_SQL_INSERT = GTFSCommons.getT_STRINGS_SQL_INSERT();
	private static final String T_STRINGS_SQL_DROP = GTFSCommons.getT_STRINGS_SQL_DROP();

	static final String T_ROUTE = GTFSCommons.T_ROUTE;
	static final String T_ROUTE_K_ID = GTFSCommons.T_ROUTE_K_ID;
	static final String T_ROUTE_K_SHORT_NAME = GTFSCommons.T_ROUTE_K_SHORT_NAME;
	static final String T_ROUTE_K_LONG_NAME = GTFSCommons.T_ROUTE_K_LONG_NAME;
	static final String T_ROUTE_K_COLOR = GTFSCommons.T_ROUTE_K_COLOR;
	static final String T_ROUTE_K_ORIGINAL_ID_HASH = GTFSCommons.T_ROUTE_K_ORIGINAL_ID_HASH;
	static final String T_ROUTE_K_TYPE = GTFSCommons.T_ROUTE_K_TYPE;
	private static final int[] T_ROUTE_STRINGS_COLUMN_IDX = GTFSCommons.T_ROUTE_STRINGS_COLUMN_IDX;
	private static final String T_ROUTE_SQL_CREATE = GTFSCommons.getT_ROUTE_SQL_CREATE();
	private static final String T_ROUTE_SQL_INSERT = GTFSCommons.getT_ROUTE_SQL_INSERT();
	private static final String T_ROUTE_SQL_DROP = GTFSCommons.getT_ROUTE_SQL_DROP();

	static final String T_DIRECTION = GTFSCommons.T_DIRECTION;
	static final String T_DIRECTION_K_ID = GTFSCommons.T_DIRECTION_K_ID;
	static final String T_DIRECTION_K_HEADSIGN_TYPE = GTFSCommons.T_DIRECTION_K_HEADSIGN_TYPE;
	static final String T_DIRECTION_K_HEADSIGN_VALUE = GTFSCommons.T_DIRECTION_K_HEADSIGN_VALUE; // really?
	static final String T_DIRECTION_K_ROUTE_ID = GTFSCommons.T_DIRECTION_K_ROUTE_ID;
	private static final int[] T_DIRECTION_STRINGS_COLUMN_IDX = GTFSCommons.T_DIRECTION_STRINGS_COLUMN_IDX;
	private static final String T_DIRECTION_SQL_CREATE = GTFSCommons.getT_DIRECTION_SQL_CREATE();
	private static final String T_DIRECTION_SQL_INSERT = GTFSCommons.getT_DIRECTION_SQL_INSERT();
	private static final String T_DIRECTION_SQL_DROP = GTFSCommons.getT_DIRECTION_SQL_DROP();

	static final String T_STOP = GTFSCommons.T_STOP;
	static final String T_STOP_K_ID = GTFSCommons.T_STOP_K_ID;
	static final String T_STOP_K_CODE = GTFSCommons.T_STOP_K_CODE; // optional
	static final String T_STOP_K_NAME = GTFSCommons.T_STOP_K_NAME;
	static final String T_STOP_K_LAT = GTFSCommons.T_STOP_K_LAT;
	static final String T_STOP_K_LNG = GTFSCommons.T_STOP_K_LNG;
	static final String T_STOP_K_ACCESSIBLE = GTFSCommons.T_STOP_K_ACCESSIBLE;
	static final String T_STOP_K_ORIGINAL_ID_HASH = GTFSCommons.T_STOP_K_ORIGINAL_ID_HASH;
	private static final int[] T_STOP_STRINGS_COLUMN_IDX = GTFSCommons.T_STOP_STRINGS_COLUMN_IDX;
	private static final String T_STOP_SQL_CREATE = GTFSCommons.getT_STOP_SQL_CREATE();
	private static final String T_STOP_SQL_INSERT = GTFSCommons.getT_STOP_SQL_INSERT();
	private static final String T_STOP_SQL_DROP = GTFSCommons.getT_STOP_SQL_DROP();

	static final String T_DIRECTION_STOPS = GTFSCommons.T_DIRECTION_STOPS;
	@SuppressWarnings("unused")
	static final String T_DIRECTION_STOPS_K_ID = GTFSCommons.T_DIRECTION_STOPS_K_ID;
	static final String T_DIRECTION_STOPS_K_DIRECTION_ID = GTFSCommons.T_DIRECTION_STOPS_K_DIRECTION_ID;
	static final String T_DIRECTION_STOPS_K_STOP_ID = GTFSCommons.T_DIRECTION_STOPS_K_STOP_ID;
	static final String T_DIRECTION_STOPS_K_STOP_SEQUENCE = GTFSCommons.T_DIRECTION_STOPS_K_STOP_SEQUENCE;
	static final String T_DIRECTION_STOPS_K_NO_PICKUP = GTFSCommons.T_DIRECTION_STOPS_K_NO_PICKUP;
	private static final String T_DIRECTION_STOPS_SQL_CREATE = GTFSCommons.getT_DIRECTION_STOPS_SQL_CREATE();
	private static final String T_DIRECTION_STOPS_SQL_INSERT = GTFSCommons.getT_DIRECTION_STOPS_SQL_INSERT();
	private static final String T_DIRECTION_STOPS_SQL_DROP = GTFSCommons.getT_DIRECTION_STOPS_SQL_DROP();

	static final String T_TRIP = GTFSCommons.T_TRIP;
	static final String T_TRIP_K_TRIP_ID_OR_INT = GTFSCommons.getT_TRIP_K_TRIP_ID_OR_INT();
	static final String T_TRIP_K_ROUTE_ID = GTFSCommons.T_TRIP_K_ROUTE_ID;
	static final String T_TRIP_K_DIRECTION_ID = GTFSCommons.T_TRIP_K_DIRECTION_ID;
	static final String T_TRIP_K_SERVICE_ID_OR_INT = GTFSCommons.getT_TRIP_K_SERVICE_ID_OR_INT();
	private static final int T_TRIP_SAME_COLUMNS_COUNT = GTFSCommons.T_TRIP_SAME_COLUMNS_COUNT;
	private static final int T_TRIP_OTHER_COLUMNS_COUNT = GTFSCommons.T_TRIP_OTHER_COLUMNS_COUNT;
	private static final String T_TRIP_SQL_CREATE = GTFSCommons.getT_TRIP_SQL_CREATE();
	private static final String T_TRIP_SQL_INSERT = GTFSCommons.getT_TRIP_SQL_INSERT();
	private static final String T_TRIP_SQL_DROP = GTFSCommons.getT_TRIP_SQL_DROP();

	public static final String T_TRIP_IDS = GTFSCommons.T_TRIP_IDS;
	public static final String T_TRIP_IDS_K_ID = GTFSCommons.T_TRIP_IDS_K_ID;
	public static final String T_TRIP_IDS_K_ID_INT = GTFSCommons.T_TRIP_IDS_K_ID_INT;
	private static final String T_TRIP_IDS_SQL_CREATE = GTFSCommons.getT_TRIP_IDS_SQL_CREATE();
	private static final String T_TRIP_IDS_SQL_INSERT = GTFSCommons.getT_TRIP_IDS_SQL_INSERT();
	private static final String T_TRIP_IDS_SQL_DROP = GTFSCommons.getT_TRIP_IDS_SQL_DROP();

	@SuppressWarnings("WeakerAccess")
	static final String T_SERVICE_IDS = GTFSCommons.T_SERVICE_IDS;
	@SuppressWarnings("unused") // not used by main app currently
	static final String T_SERVICE_IDS_K_ID = GTFSCommons.T_SERVICE_IDS_K_ID;
	@SuppressWarnings("unused")
	static final String T_SERVICE_IDS_K_ID_INT = GTFSCommons.T_SERVICE_IDS_K_ID_INT;
	private static final String T_SERVICE_IDS_SQL_CREATE = GTFSCommons.getT_SERVICE_IDS_SQL_CREATE();
	private static final String T_SERVICE_IDS_SQL_INSERT = GTFSCommons.getT_SERVICE_IDS_SQL_INSERT();
	private static final String T_SERVICE_IDS_SQL_DROP = GTFSCommons.getT_SERVICE_IDS_SQL_DROP();

	static final String T_SERVICE_DATES = GTFSCommons.T_SERVICE_DATES;
	static final String T_SERVICE_DATES_K_SERVICE_ID = GTFSCommons.T_SERVICE_DATES_K_SERVICE_ID;
	static final String T_SERVICE_DATES_K_SERVICE_ID_INT = GTFSCommons.T_SERVICE_DATES_K_SERVICE_ID_INT;
	static final String T_SERVICE_DATES_K_DATE = GTFSCommons.T_SERVICE_DATES_K_DATE;
	static final String T_SERVICE_DATES_K_EXCEPTION_TYPE = GTFSCommons.T_SERVICE_DATES_K_EXCEPTION_TYPE;
	private static final int T_SERVICE_DATES_SAME_COLUMNS_COUNT = GTFSCommons.getT_SERVICE_DATES_SAME_COLUMNS_COUNT();
	private static final int T_SERVICE_DATES_OTHER_COLUMNS_COUNT = GTFSCommons.getT_SERVICE_DATES_OTHER_COLUMNS_COUNT();
	private static final String T_SERVICE_DATES_SQL_CREATE = GTFSCommons.getT_SERVICE_DATES_SQL_CREATE();
	private static final String T_SERVICE_DATES_SQL_INSERT = GTFSCommons.getT_SERVICE_DATES_SQL_INSERT();
	private static final String T_SERVICE_DATES_SQL_DROP = GTFSCommons.getT_SERVICE_DATES_SQL_DROP();

	static final String T_ROUTE_DIRECTION_STOP_STATUS = StatusProvider.StatusDbHelper.T_STATUS;
	private static final String T_ROUTE_DIRECTION_STOP_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_ROUTE_DIRECTION_STOP_STATUS).build();
	private static final String T_ROUTE_DIRECTION_STOP_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_ROUTE_DIRECTION_STOP_STATUS);

	@NonNull
	private final Context context;

	private static int dbVersion = -1;

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} in same app.
	 */
	public static int getDbVersion(@NonNull Context context) {
		if (dbVersion < 0) {
			dbVersion = context.getResources().getInteger(R.integer.gtfs_rts_db_version); // do not change to avoid breaking compat w/ old modules
		}
		return dbVersion;
	}

	public GTFSProviderDbHelper(@NonNull Context context) {
		super(context, DB_NAME, null, getDbVersion(context));
		this.context = context;
	}

	@Override
	public void onCreateMT(@NonNull SQLiteDatabase db) {
		initAllDbTables(db, false);
	}

	@Override
	public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(T_DIRECTION_STOPS_SQL_DROP);
		db.execSQL(T_STOP_SQL_DROP);
		db.execSQL(T_DIRECTION_SQL_DROP);
		db.execSQL(T_ROUTE_SQL_DROP);
		if (FeatureFlags.F_EXPORT_SERVICE_ID_INTS) {
			db.execSQL(T_SERVICE_IDS_SQL_DROP);
		}
		if (FeatureFlags.F_EXPORT_TRIP_ID_INTS) {
			db.execSQL(T_TRIP_IDS_SQL_DROP);
		}
		db.execSQL(T_SERVICE_DATES_SQL_DROP);
		db.execSQL(T_ROUTE_DIRECTION_STOP_STATUS_SQL_DROP);
		initAllDbTables(db, true);
	}

	public boolean isDbExist(@NonNull Context context) {
		return SqlUtils.isDbExist(context, DB_NAME);
	}

	@SuppressLint("MissingPermission") // no notification if not permitted (not requesting permission for that)
	private void initAllDbTables(@NonNull SQLiteDatabase db, boolean upgrade) {
		MTLog.i(this, "Data: deploying DB...");
		final int nId = TimeUtils.currentTimeSec();
		final long startInMs = TimeUtils.currentTimeMillis();
		int nbTotalOperations = 7;
		if (FeatureFlags.F_EXPORT_TRIP_ID) nbTotalOperations++;
		if (FeatureFlags.F_EXPORT_SERVICE_ID_INTS) nbTotalOperations++;
		if (FeatureFlags.F_EXPORT_TRIP_ID_INTS) nbTotalOperations++;
		int progress = -1;
		final NotificationManagerCompat nm = NotificationManagerCompat.from(this.context);
		final boolean notifEnabled = nm.areNotificationsEnabled();
		final NotificationCompat.Builder nb;
		if (notifEnabled) {
			NotificationUtils.createNotificationChannel(this.context, NotificationUtils.CHANNEL_ID_DB);
			nb = new NotificationCompat.Builder(this.context, NotificationUtils.CHANNEL_ID_DB) //
					.setSmallIcon(android.R.drawable.stat_notify_sync)//
					.setContentTitle(PackageManagerUtils.getAppName(this.context)) //
					.setContentText(this.context.getString(upgrade ? R.string.db_upgrading : R.string.db_deploying)) //
					.setProgress(nbTotalOperations, 0, true);
			nm.notify(nId, nb.build());
		} else {
			nb = null;
		}
		db.execSQL(SQLUtils.PRAGMA_AUTO_VACUUM_NONE);
		if (notifEnabled) NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, ++progress);
		final Map<Integer, String> allStrings = new HashMap<>();
		if (FeatureFlags.F_EXPORT_STRINGS || FeatureFlags.F_EXPORT_SCHEDULE_STRINGS) {
			initDbTableWithRetry(context, db, T_STRINGS, T_STRINGS_SQL_CREATE, T_STRINGS_SQL_INSERT, T_STRINGS_SQL_DROP, getStringsFiles(), 0, 0, null, null,
					(id, string) -> {
						allStrings.put(id, string);
						return kotlin.Unit.INSTANCE;
					}
			); // 1st
		}
		if (notifEnabled) NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, ++progress);
		initDbTableWithRetry(context, db, T_ROUTE, T_ROUTE_SQL_CREATE, T_ROUTE_SQL_INSERT, T_ROUTE_SQL_DROP, getRouteFiles(), 0, 0, allStrings, T_ROUTE_STRINGS_COLUMN_IDX);
		if (notifEnabled) NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, ++progress);
		initDbTableWithRetry(context, db, T_DIRECTION, T_DIRECTION_SQL_CREATE, T_DIRECTION_SQL_INSERT, T_DIRECTION_SQL_DROP, getDirectionFiles(), 0, 0, allStrings, T_DIRECTION_STRINGS_COLUMN_IDX);
		if (notifEnabled) NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, ++progress);
		initDbTableWithRetry(context, db, T_STOP, T_STOP_SQL_CREATE, T_STOP_SQL_INSERT, T_STOP_SQL_DROP, getStopFiles(), 0, 0, allStrings, T_STOP_STRINGS_COLUMN_IDX);
		if (notifEnabled) NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, ++progress);
		initDbTableWithRetry(context, db, T_DIRECTION_STOPS, T_DIRECTION_STOPS_SQL_CREATE, T_DIRECTION_STOPS_SQL_INSERT, T_DIRECTION_STOPS_SQL_DROP, getDirectionStopsFiles());
		if (FeatureFlags.F_EXPORT_TRIP_ID) {
			if (notifEnabled) NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, ++progress);
			initDbTableWithRetry(context, db, T_TRIP, T_TRIP_SQL_CREATE, T_TRIP_SQL_INSERT, T_TRIP_SQL_DROP, getTripFiles(), T_TRIP_SAME_COLUMNS_COUNT, T_TRIP_OTHER_COLUMNS_COUNT);
		}
		if (FeatureFlags.F_EXPORT_SERVICE_ID_INTS) {
			if (notifEnabled) NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, ++progress);
			initDbTableWithRetry(context, db, T_SERVICE_IDS, T_SERVICE_IDS_SQL_CREATE, T_SERVICE_IDS_SQL_INSERT, T_SERVICE_IDS_SQL_DROP, getServiceIdsFiles());
		}
		if (notifEnabled) NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, ++progress);
		initDbTableWithRetry(context, db, T_SERVICE_DATES, T_SERVICE_DATES_SQL_CREATE, T_SERVICE_DATES_SQL_INSERT, T_SERVICE_DATES_SQL_DROP, getServiceDatesFiles(), T_SERVICE_DATES_SAME_COLUMNS_COUNT, T_SERVICE_DATES_OTHER_COLUMNS_COUNT);
		if (FeatureFlags.F_EXPORT_TRIP_ID_INTS) {
			if (notifEnabled) NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, ++progress);
			initDbTableWithRetry(context, db, T_TRIP_IDS, T_TRIP_IDS_SQL_CREATE, T_TRIP_IDS_SQL_INSERT, T_TRIP_IDS_SQL_DROP, getTripIdsFiles());
		}
		if (notifEnabled) NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, ++progress);
		db.execSQL(T_ROUTE_DIRECTION_STOP_STATUS_SQL_CREATE);
		if (notifEnabled) {
			nb.setSmallIcon(android.R.drawable.stat_notify_sync_noanim);
			NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, nbTotalOperations);
			nm.cancel(nId);
		}
		final long durationInMs = TimeUtils.currentTimeMillis() - startInMs;
		MTLog.i(this, "Data: deploying DB... DONE (%s)", MTLog.formatDuration(durationInMs));
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} implementations in same app.
	 */
	private int[] getTripIdsFiles() {
		if (GTFSCurrentNextProvider.hasCurrentData(context)) {
			if (GTFSCurrentNextProvider.isNextData(context)) {
				return new int[]{R.raw.next_gtfs_schedule_path_ids}; // do not change to avoid breaking compat w/ old modules
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_schedule_path_ids}; // do not change to avoid breaking compat w/ old modules
			}
		} else {
			return new int[]{R.raw.gtfs_schedule_path_ids}; // do not change to avoid breaking compat w/ old modules
		}
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} in same app.
	 */
	private int[] getTripFiles() {
		if (GTFSCurrentNextProvider.hasCurrentData(context)) {
			if (GTFSCurrentNextProvider.isNextData(context)) {
				return new int[]{R.raw.next_gtfs_rts_paths}; // do not change to avoid breaking compat w/ old modules
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_rts_paths}; // do not change to avoid breaking compat w/ old modules
			}
		} else {
			return new int[]{R.raw.gtfs_rts_paths}; // do not change to avoid breaking compat w/ old modules
		}
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} implementations in same app.
	 */
	private int[] getServiceIdsFiles() {
		if (GTFSCurrentNextProvider.hasCurrentData(context)) {
			if (GTFSCurrentNextProvider.isNextData(context)) {
				return new int[]{R.raw.next_gtfs_schedule_service_ids};
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_schedule_service_ids};
			}
		} else {
			return new int[]{R.raw.gtfs_schedule_service_ids};
		}
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} implementations in same app.
	 */
	private int[] getStringsFiles() {
		if (GTFSCurrentNextProvider.hasCurrentData(context)) {
			if (GTFSCurrentNextProvider.isNextData(context)) {
				return new int[]{R.raw.next_gtfs_strings};
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_strings};
			}
		} else {
			return new int[]{R.raw.gtfs_strings};
		}
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} implementations in same app.
	 */
	private int[] getServiceDatesFiles() {
		if (GTFSCurrentNextProvider.hasCurrentData(context)) {
			if (GTFSCurrentNextProvider.isNextData(context)) {
				return new int[]{R.raw.next_gtfs_schedule_service_dates};
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_schedule_service_dates};
			}
		} else {
			return new int[]{R.raw.gtfs_schedule_service_dates};
		}
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} implementations in same app.
	 */
	private int[] getRouteFiles() {
		if (GTFSCurrentNextProvider.hasCurrentData(context)) {
			if (GTFSCurrentNextProvider.isNextData(context)) {
				return new int[]{R.raw.next_gtfs_rts_routes}; // do not change to avoid breaking compat w/ old modules
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_rts_routes}; // do not change to avoid breaking compat w/ old modules
			}
		} else {
			return new int[]{R.raw.gtfs_rts_routes}; // do not change to avoid breaking compat w/ old modules
		}
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} implementations in same app.
	 */
	private int[] getStopFiles() {
		if (GTFSCurrentNextProvider.hasCurrentData(context)) {
			if (GTFSCurrentNextProvider.isNextData(context)) {
				return new int[]{R.raw.next_gtfs_rts_stops}; // do not change to avoid breaking compat w/ old modules
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_rts_stops}; // do not change to avoid breaking compat w/ old modules
			}
		} else {
			return new int[]{R.raw.gtfs_rts_stops};
		}
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} implementations in same app.
	 */
	private int[] getDirectionFiles() {
		if (GTFSCurrentNextProvider.hasCurrentData(context)) {
			if (GTFSCurrentNextProvider.isNextData(context)) {
				return new int[]{R.raw.next_gtfs_rts_trips}; // do not change to avoid breaking compat w/ old modules
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_rts_trips}; // do not change to avoid breaking compat w/ old modules
			}
		} else {
			return new int[]{R.raw.gtfs_rts_trips}; // do not change to avoid breaking compat w/ old modules
		}
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} in same app.
	 */
	private int[] getDirectionStopsFiles() {
		if (GTFSCurrentNextProvider.hasCurrentData(context)) {
			if (GTFSCurrentNextProvider.isNextData(context)) {
				return new int[]{R.raw.next_gtfs_rts_trip_stops}; // do not change to avoid breaking compat w/ old modules
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_rts_trip_stops}; // do not change to avoid breaking compat w/ old modules
			}
		} else {
			return new int[]{R.raw.gtfs_rts_trip_stops}; // do not change to avoid breaking compat w/ old modules
		}
	}
}
