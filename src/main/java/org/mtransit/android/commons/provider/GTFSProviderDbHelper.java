package org.mtransit.android.commons.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

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

	static final String T_ROUTE = "route";
	static final String T_ROUTE_K_ID = BaseColumns._ID;
	static final String T_ROUTE_K_SHORT_NAME = "short_name";
	static final String T_ROUTE_K_LONG_NAME = "long_name";
	static final String T_ROUTE_K_COLOR = "color";
	private static final String T_ROUTE_SQL_CREATE = SqlUtils.SQLCreateBuilder.getNew(T_ROUTE) //
			.appendColumn(T_ROUTE_K_ID, SqlUtils.INT_PK) //
			.appendColumn(T_ROUTE_K_SHORT_NAME, SqlUtils.TXT) //
			.appendColumn(T_ROUTE_K_LONG_NAME, SqlUtils.TXT) //
			.appendColumn(T_ROUTE_K_COLOR, SqlUtils.TXT) //
			.build();
	private static final String T_ROUTE_SQL_INSERT =
			SqlUtils.SQLInsertBuilder.getNew(T_ROUTE) //
					.appendColumn(T_ROUTE_K_ID) //
					.appendColumn(T_ROUTE_K_SHORT_NAME) //
					.appendColumn(T_ROUTE_K_LONG_NAME) //
					.appendColumn(T_ROUTE_K_COLOR) //
					.build();
	private static final String T_ROUTE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_ROUTE);

	static final String T_TRIP = "trip";
	static final String T_TRIP_K_ID = BaseColumns._ID;
	static final String T_TRIP_K_HEADSIGN_TYPE = "headsign_type";
	static final String T_TRIP_K_HEADSIGN_VALUE = "headsign_value"; // really?
	static final String T_TRIP_K_ROUTE_ID = "route_id";
	private static final String T_TRIP_SQL_CREATE = SqlUtils.SQLCreateBuilder.getNew(T_TRIP) //
			.appendColumn(T_TRIP_K_ID, SqlUtils.INT_PK) //
			.appendColumn(T_TRIP_K_HEADSIGN_TYPE, SqlUtils.INT) //
			.appendColumn(T_TRIP_K_HEADSIGN_VALUE, SqlUtils.TXT) //
			.appendColumn(T_TRIP_K_ROUTE_ID, SqlUtils.INT) //
			.appendForeignKey(T_TRIP_K_ROUTE_ID, T_ROUTE, T_ROUTE_K_ID) //
			.build();
	private static final String T_TRIP_SQL_INSERT =
			SqlUtils.SQLInsertBuilder.getNew(T_TRIP) //
					.appendColumn(T_TRIP_K_ID) //
					.appendColumn(T_TRIP_K_HEADSIGN_TYPE) //
					.appendColumn(T_TRIP_K_HEADSIGN_VALUE) //
					.appendColumn(T_TRIP_K_ROUTE_ID) //
					.build();
	private static final String T_TRIP_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_TRIP);

	static final String T_STOP = "stop";
	static final String T_STOP_K_ID = BaseColumns._ID;
	static final String T_STOP_K_CODE = "code"; // optional
	static final String T_STOP_K_NAME = "name";
	static final String T_STOP_K_LAT = "lat";
	static final String T_STOP_K_LNG = "lng";
	private static final String T_STOP_SQL_CREATE;

	static {
		SqlUtils.SQLCreateBuilder sb = SqlUtils.SQLCreateBuilder.getNew(T_STOP);
		sb
				.appendColumn(T_STOP_K_ID, SqlUtils.INT_PK) //
				.appendColumn(T_STOP_K_CODE, SqlUtils.TXT) //
		;
		sb
				.appendColumn(T_STOP_K_NAME, SqlUtils.TXT) //
				.appendColumn(T_STOP_K_LAT, SqlUtils.REAL) //
				.appendColumn(T_STOP_K_LNG, SqlUtils.REAL) //
		;
		T_STOP_SQL_CREATE = sb.build();
	}

	private static final String T_STOP_SQL_INSERT;

	static {
		SqlUtils.SQLInsertBuilder sb = SqlUtils.SQLInsertBuilder.getNew(T_STOP);
		sb
				.appendColumn(T_STOP_K_ID) //
				.appendColumn(T_STOP_K_CODE) //
		;
		sb
				.appendColumn(T_STOP_K_NAME) //
				.appendColumn(T_STOP_K_LAT) //
				.appendColumn(T_STOP_K_LNG) //
		;
		T_STOP_SQL_INSERT = sb.build();
	}

	private static final String T_STOP_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STOP);

	static final String T_TRIP_STOPS = "trip_stops";
	private static final String T_TRIP_STOPS_K_ID = BaseColumns._ID;
	static final String T_TRIP_STOPS_K_TRIP_ID = "trip_id";
	static final String T_TRIP_STOPS_K_STOP_ID = "stop_id";
	static final String T_TRIP_STOPS_K_STOP_SEQUENCE = "stop_sequence";
	static final String T_TRIP_STOPS_K_DESCENT_ONLY = "decent_only";
	private static final String T_TRIP_STOPS_SQL_CREATE = SqlUtils.SQLCreateBuilder.getNew(T_TRIP_STOPS) //
			.appendColumn(T_TRIP_STOPS_K_ID, SqlUtils.INT_PK_AUTO) //
			.appendColumn(T_TRIP_STOPS_K_TRIP_ID, SqlUtils.INT) //
			.appendColumn(T_TRIP_STOPS_K_STOP_ID, SqlUtils.INT) //
			.appendColumn(T_TRIP_STOPS_K_STOP_SEQUENCE, SqlUtils.INT) //
			.appendColumn(T_TRIP_STOPS_K_DESCENT_ONLY, SqlUtils.INT) //
			.appendForeignKey(T_TRIP_STOPS_K_TRIP_ID, T_TRIP, T_TRIP_K_ID) //
			.appendForeignKey(T_TRIP_STOPS_K_STOP_ID, T_STOP, T_STOP_K_ID) //
			.build();
	private static final String T_TRIP_STOPS_SQL_INSERT =
			SqlUtils.SQLInsertBuilder.getNew(T_TRIP_STOPS) //
					.appendColumn(T_TRIP_STOPS_K_TRIP_ID) //
					.appendColumn(T_TRIP_STOPS_K_STOP_ID) //
					.appendColumn(T_TRIP_STOPS_K_STOP_SEQUENCE) //
					.appendColumn(T_TRIP_STOPS_K_DESCENT_ONLY) //
					.build();
	private static final String T_TRIP_STOPS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_TRIP_STOPS);

	static final String T_SERVICE_DATES = "service_dates";
	private static final String T_SERVICE_DATES_K_SERVICE_ID = "service_id";
	private static final String T_SERVICE_DATES_K_DATE = "date";
	private static final String T_SERVICE_DATES_SQL_CREATE = SqlUtils.SQLCreateBuilder.getNew(T_SERVICE_DATES) //
			.appendColumn(T_SERVICE_DATES_K_SERVICE_ID, SqlUtils.TXT) //
			.appendColumn(T_SERVICE_DATES_K_DATE, SqlUtils.INT) //
			.build();
	private static final String T_SERVICE_DATES_SQL_INSERT = SqlUtils.SQLInsertBuilder.getNew(T_SERVICE_DATES) //
			.appendColumn(T_SERVICE_DATES_K_SERVICE_ID).appendColumn(T_SERVICE_DATES_K_DATE).build();
	private static final String T_SERVICE_DATES_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_SERVICE_DATES);

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
		NotificationUtils.createNotificationChannel(this.context, NotificationUtils.CHANNEL_ID_DB);
		NotificationCompat.Builder nb = new NotificationCompat.Builder(this.context, NotificationUtils.CHANNEL_ID_DB) //
				.setSmallIcon(android.R.drawable.stat_notify_sync)//
				.setContentTitle(PackageManagerUtils.getAppName(this.context)) //
				.setContentText(this.context.getString(upgrade ? R.string.db_upgrading : R.string.db_deploying)) //
				.setProgress(nbTotalOperations, 0, true);
		NotificationManagerCompat nm = NotificationManagerCompat.from(this.context);
		nm.notify(nId, nb.build());
		db.execSQL("PRAGMA auto_vacuum=NONE;");
		NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 0);
		initDbTableWithRetry(db, T_ROUTE, T_ROUTE_SQL_CREATE, T_ROUTE_SQL_INSERT, T_ROUTE_SQL_DROP, getRouteFiles());
		NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 1);
		initDbTableWithRetry(db, T_TRIP, T_TRIP_SQL_CREATE, T_TRIP_SQL_INSERT, T_TRIP_SQL_DROP, getTripFiles());
		NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 2);
		initDbTableWithRetry(db, T_STOP, T_STOP_SQL_CREATE, T_STOP_SQL_INSERT, T_STOP_SQL_DROP, getStopFiles());
		NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 3);
		initDbTableWithRetry(db, T_TRIP_STOPS, T_TRIP_STOPS_SQL_CREATE, T_TRIP_STOPS_SQL_INSERT, T_TRIP_STOPS_SQL_DROP, getTripStopsFiles());
		NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 4);
		initDbTableWithRetry(db, T_SERVICE_DATES, T_SERVICE_DATES_SQL_CREATE, T_SERVICE_DATES_SQL_INSERT, T_SERVICE_DATES_SQL_DROP, getServiceDatesFiles());
		NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 5);
		db.execSQL(T_ROUTE_TRIP_STOP_STATUS_SQL_CREATE);
		nb.setSmallIcon(android.R.drawable.stat_notify_sync_noanim); //
		NotificationUtils.setProgressAndNotify(nm, nb, nId, nbTotalOperations, 6);
		nm.cancel(nId);
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
