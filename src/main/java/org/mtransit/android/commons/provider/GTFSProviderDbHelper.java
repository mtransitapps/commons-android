package org.mtransit.android.commons.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NotificationUtils;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.commons.GTFSCommons;
import org.mtransit.commons.sql.SQLUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

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
	public static final String DB_NAME = "gtfs_rts.db";

	static final String T_ROUTE = GTFSCommons.T_ROUTE;
	static final String T_ROUTE_K_ID = GTFSCommons.T_ROUTE_K_ID;
	static final String T_ROUTE_K_SHORT_NAME = GTFSCommons.T_ROUTE_K_SHORT_NAME;
	static final String T_ROUTE_K_LONG_NAME = GTFSCommons.T_ROUTE_K_LONG_NAME;
	static final String T_ROUTE_K_COLOR = GTFSCommons.T_ROUTE_K_COLOR;
	private static final String T_ROUTE_SQL_CREATE = GTFSCommons.getT_ROUTE_SQL_CREATE();
	private static final String T_ROUTE_SQL_INSERT = GTFSCommons.getT_ROUTE_SQL_INSERT();
	private static final String T_ROUTE_SQL_DROP = GTFSCommons.getT_ROUTE_SQL_DROP();

	static final String T_TRIP = GTFSCommons.T_TRIP;
	static final String T_TRIP_K_ID = GTFSCommons.T_TRIP_K_ID;
	static final String T_TRIP_K_HEADSIGN_TYPE = GTFSCommons.T_TRIP_K_HEADSIGN_TYPE;
	static final String T_TRIP_K_HEADSIGN_VALUE = GTFSCommons.T_TRIP_K_HEADSIGN_VALUE; // really?
	static final String T_TRIP_K_ROUTE_ID = GTFSCommons.T_TRIP_K_ROUTE_ID;
	private static final String T_TRIP_SQL_CREATE = GTFSCommons.getT_TRIP_SQL_CREATE();
	private static final String T_TRIP_SQL_INSERT = GTFSCommons.getT_TRIP_SQL_INSERT();
	private static final String T_TRIP_SQL_DROP = GTFSCommons.getT_TRIP_SQL_DROP();

	static final String T_STOP = GTFSCommons.T_STOP;
	static final String T_STOP_K_ID = GTFSCommons.T_STOP_K_ID;
	static final String T_STOP_K_CODE = GTFSCommons.T_STOP_K_CODE; // optional
	static final String T_STOP_K_NAME = GTFSCommons.T_STOP_K_NAME;
	static final String T_STOP_K_LAT = GTFSCommons.T_STOP_K_LAT;
	static final String T_STOP_K_LNG = GTFSCommons.T_STOP_K_LNG;
	private static final String T_STOP_SQL_CREATE = GTFSCommons.getT_STOP_SQL_CREATE();
	private static final String T_STOP_SQL_INSERT = GTFSCommons.getT_STOP_SQL_INSERT();
	private static final String T_STOP_SQL_DROP = GTFSCommons.getT_STOP_SQL_DROP();

	static final String T_TRIP_STOPS = GTFSCommons.T_TRIP_STOPS;
	@SuppressWarnings("unused")
	static final String T_TRIP_STOPS_K_ID = GTFSCommons.T_TRIP_STOPS_K_ID;
	static final String T_TRIP_STOPS_K_TRIP_ID = GTFSCommons.T_TRIP_STOPS_K_TRIP_ID;
	static final String T_TRIP_STOPS_K_STOP_ID = GTFSCommons.T_TRIP_STOPS_K_STOP_ID;
	static final String T_TRIP_STOPS_K_STOP_SEQUENCE = GTFSCommons.T_TRIP_STOPS_K_STOP_SEQUENCE;
	static final String T_TRIP_STOPS_K_DESCENT_ONLY = GTFSCommons.T_TRIP_STOPS_K_DESCENT_ONLY;
	private static final String T_TRIP_STOPS_SQL_CREATE = GTFSCommons.getT_TRIP_STOPS_SQL_CREATE();
	private static final String T_TRIP_STOPS_SQL_INSERT = GTFSCommons.getT_TRIP_STOPS_SQL_INSERT();
	private static final String T_TRIP_STOPS_SQL_DROP = GTFSCommons.getT_TRIP_STOPS_SQL_DROP();

	static final String T_SERVICE_DATES = GTFSCommons.T_SERVICE_DATES;
	@SuppressWarnings("unused")
	private static final String T_SERVICE_DATES_K_SERVICE_ID = GTFSCommons.T_SERVICE_DATES_K_SERVICE_ID;
	@SuppressWarnings("unused")
	private static final String T_SERVICE_DATES_K_DATE = GTFSCommons.T_SERVICE_DATES_K_DATE;
	private static final String T_SERVICE_DATES_SQL_CREATE = GTFSCommons.getT_SERVICE_DATES_SQL_CREATE();
	private static final String T_SERVICE_DATES_SQL_INSERT = GTFSCommons.getT_SERVICE_DATES_SQL_INSERT();
	private static final String T_SERVICE_DATES_SQL_DROP = GTFSCommons.getT_SERVICE_DATES_SQL_DROP();

	static final String T_ROUTE_TRIP_STOP_STATUS = StatusProvider.StatusDbHelper.T_STATUS;
	private static final String T_ROUTE_TRIP_STOP_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_ROUTE_TRIP_STOP_STATUS).build();
	private static final String T_ROUTE_TRIP_STOP_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_ROUTE_TRIP_STOP_STATUS);

	@NonNull
	private final Context context;

	private static int dbVersion = -1;

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} in same app.
	 */
	public static int getDbVersion(@NonNull Context context) {
		if (dbVersion < 0) {
			dbVersion = context.getResources().getInteger(R.integer.gtfs_rts_db_version);
		}
		return dbVersion;
	}

	GTFSProviderDbHelper(@NonNull Context context) {
		super(context, DB_NAME, null, getDbVersion(context));
		this.context = context;
	}

	@Override
	public void onCreateMT(@NonNull SQLiteDatabase db) {
		initAllDbTables(db, false);
	}

	@Override
	public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(T_TRIP_STOPS_SQL_DROP);
		db.execSQL(T_STOP_SQL_DROP);
		db.execSQL(T_TRIP_SQL_DROP);
		db.execSQL(T_ROUTE_SQL_DROP);
		db.execSQL(T_SERVICE_DATES_SQL_DROP);
		db.execSQL(T_ROUTE_TRIP_STOP_STATUS_SQL_DROP);
		initAllDbTables(db, true);
	}

	public boolean isDbExist(@NonNull Context context) {
		return SqlUtils.isDbExist(context, DB_NAME);
	}

	private void initAllDbTables(@NonNull SQLiteDatabase db, boolean upgrade) {
		MTLog.i(this, "Data: deploying DB...");
		int nId = TimeUtils.currentTimeSec();
		int nbTotalOperations = 6;
		final NotificationManagerCompat nm = NotificationManagerCompat.from(this.context);
		final boolean notifEnabled = nm.areNotificationsEnabled();
		NotificationCompat.Builder nb = null;
		if (notifEnabled) {
			NotificationUtils.createNotificationChannel(this.context, NotificationUtils.CHANNEL_ID_DB);
			nb = new NotificationCompat.Builder(this.context, NotificationUtils.CHANNEL_ID_DB) //
					.setSmallIcon(android.R.drawable.stat_notify_sync)//
					.setContentTitle(PackageManagerUtils.getAppName(this.context)) //
					.setContentText(this.context.getString(upgrade ? R.string.db_upgrading : R.string.db_deploying)) //
					.setProgress(nbTotalOperations, 0, true);
			nm.notify(nId, nb.build());
		}
		db.execSQL(SQLUtils.PRAGMA_AUTO_VACUUM_NONE);
		if (notifEnabled) {
			NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 0);
		}
		initDbTableWithRetry(db, T_ROUTE, T_ROUTE_SQL_CREATE, T_ROUTE_SQL_INSERT, T_ROUTE_SQL_DROP, getRouteFiles());
		if (notifEnabled) {
			NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 1);
		}
		initDbTableWithRetry(db, T_TRIP, T_TRIP_SQL_CREATE, T_TRIP_SQL_INSERT, T_TRIP_SQL_DROP, getTripFiles());
		if (notifEnabled) {
			NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 2);
		}
		initDbTableWithRetry(db, T_STOP, T_STOP_SQL_CREATE, T_STOP_SQL_INSERT, T_STOP_SQL_DROP, getStopFiles());
		if (notifEnabled) {
			NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 3);
		}
		initDbTableWithRetry(db, T_TRIP_STOPS, T_TRIP_STOPS_SQL_CREATE, T_TRIP_STOPS_SQL_INSERT, T_TRIP_STOPS_SQL_DROP, getTripStopsFiles());
		if (notifEnabled) {
			NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 4);
		}
		initDbTableWithRetry(db, T_SERVICE_DATES, T_SERVICE_DATES_SQL_CREATE, T_SERVICE_DATES_SQL_INSERT, T_SERVICE_DATES_SQL_DROP, getServiceDatesFiles());
		if (notifEnabled) {
			NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 5);
		}
		db.execSQL(T_ROUTE_TRIP_STOP_STATUS_SQL_CREATE);
		if (notifEnabled) {
			nb.setSmallIcon(android.R.drawable.stat_notify_sync_noanim); //
			NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 6);
			nm.cancel(nId);
		}
		MTLog.i(this, "Data: deploying DB... DONE");
	}

	private void initDbTableWithRetry(@NonNull SQLiteDatabase db, String table, String sqlCreate, String sqlInsert, String sqlDrop, int[] files) {
		boolean success;
		do {
			try {
				success = initDbTable(db, table, sqlCreate, sqlInsert, sqlDrop, files);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while deploying DB table %s!", table);
				success = false;
			}
		} while (!success);
	}

	private boolean initDbTable(@NonNull SQLiteDatabase db, String table, String sqlCreate, String sqlInsert, String sqlDrop, int[] files) {
		try {
			db.beginTransaction();
			db.execSQL(sqlDrop); // drop if exists
			db.execSQL(sqlCreate); // create if not exists
			String line;
			BufferedReader br = null;
			InputStreamReader isr = null;
			InputStream is = null;
			for (int file : files) {
				try {
					is = this.context.getResources().openRawResource(file);
					isr = new InputStreamReader(is, FileUtils.getUTF8());
					br = new BufferedReader(isr, 8192);
					while ((line = br.readLine()) != null) {
						String sql = String.format(sqlInsert, line);
						try {
							db.execSQL(sql);
						} catch (Exception e) {
							MTLog.w(this, e, "ERROR while executing '%s' on database '%s' table '%s' file '%s'!", sql, DB_NAME, table, file);
							throw e;
						}
					}
				} catch (Exception e) {
					MTLog.w(this, e, "ERROR while copying the database '%s' table '%s' file '%s'!", DB_NAME, table, file);
					return false;
				} finally {
					FileUtils.closeQuietly(br);
					FileUtils.closeQuietly(isr);
					FileUtils.closeQuietly(is);
				}
			}
			db.setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			MTLog.w(this, e, "ERROR while copying the database '%s' table '%s' file!", DB_NAME, table);
			return false;
		} finally {
			SqlUtils.endTransactionQuietly(db);
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
				return new int[]{R.raw.next_gtfs_rts_routes};
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_rts_routes};
			}
		} else {
			return new int[]{R.raw.gtfs_rts_routes};
		}
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} implementations in same app.
	 */
	private int[] getStopFiles() {
		if (GTFSCurrentNextProvider.hasCurrentData(context)) {
			if (GTFSCurrentNextProvider.isNextData(context)) {
				return new int[]{R.raw.next_gtfs_rts_stops};
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_rts_stops};
			}
		} else {
			return new int[]{R.raw.gtfs_rts_stops};
		}
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} implementations in same app.
	 */
	private int[] getTripFiles() {
		if (GTFSCurrentNextProvider.hasCurrentData(context)) {
			if (GTFSCurrentNextProvider.isNextData(context)) {
				return new int[]{R.raw.next_gtfs_rts_trips};
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_rts_trips};
			}
		} else {
			return new int[]{R.raw.gtfs_rts_trips};
		}
	}

	/**
	 * Override if multiple {@link GTFSProviderDbHelper} in same app.
	 */
	private int[] getTripStopsFiles() {
		if (GTFSCurrentNextProvider.hasCurrentData(context)) {
			if (GTFSCurrentNextProvider.isNextData(context)) {
				return new int[]{R.raw.next_gtfs_rts_trip_stops};
			} else { // CURRENT = default
				return new int[]{R.raw.current_gtfs_rts_trip_stops};
			}
		} else {
			return new int[]{R.raw.gtfs_rts_trip_stops};
		}
	}
}
