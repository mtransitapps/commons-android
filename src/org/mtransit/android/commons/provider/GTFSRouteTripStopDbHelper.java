package org.mtransit.android.commons.provider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NotificationUtils;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.TimeUtils;

import android.app.NotificationManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.support.v4.app.NotificationCompat;

public class GTFSRouteTripStopDbHelper extends MTSQLiteOpenHelper {

	private static final String TAG = GTFSRouteTripStopDbHelper.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link GTFSRouteTripStopDbHelper} implementations in same app.
	 */
	public static final String DB_NAME = "gtfs_rts.db";

	public static final String T_ROUTE = "route";
	public static final String T_ROUTE_K_ID = BaseColumns._ID;
	public static final String T_ROUTE_K_SHORT_NAME = "short_name";
	public static final String T_ROUTE_K_LONG_NAME = "long_name";
	public static final String T_ROUTE_K_COLOR = "color";
	public static final String T_ROUTE_SQL_CREATE = SqlUtils.CREATE_TABLE_IF_NOT_EXIST + T_ROUTE + " (" //
			+ T_ROUTE_K_ID + SqlUtils.INT_PK //
			+ ", " //
			+ T_ROUTE_K_SHORT_NAME + SqlUtils.TXT //
			+ ", " //
			+ T_ROUTE_K_LONG_NAME + SqlUtils.TXT //
			+ ", " //
			+ T_ROUTE_K_COLOR + SqlUtils.TXT //
			+ ")";
	public static final String T_ROUTE_SQL_INSERT = "INSERT INTO " + T_ROUTE + " (" + T_ROUTE_K_ID + "," + T_ROUTE_K_SHORT_NAME + "," + T_ROUTE_K_LONG_NAME
			+ "," + T_ROUTE_K_COLOR + ") VALUES(%s)";
	public static final String T_ROUTE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_ROUTE);

	public static final String T_TRIP = "trip";
	public static final String T_TRIP_K_ID = BaseColumns._ID;
	public static final String T_TRIP_K_HEADSIGN_TYPE = "headsign_type";
	public static final String T_TRIP_K_HEADSIGN_VALUE = "headsign_value"; // really?
	public static final String T_TRIP_K_ROUTE_ID = "route_id";
	public static final String T_TRIP_SQL_CREATE = SqlUtils.CREATE_TABLE_IF_NOT_EXIST + T_TRIP + " (" //
			+ T_TRIP_K_ID + SqlUtils.INT_PK + ", " //
			+ T_TRIP_K_HEADSIGN_TYPE + SqlUtils.INT + ", " //
			+ T_TRIP_K_HEADSIGN_VALUE + SqlUtils.TXT + ", " //
			+ T_TRIP_K_ROUTE_ID + SqlUtils.INT + "," //
			+ SqlUtils.getSQLForeignKey(T_TRIP_K_ROUTE_ID, T_ROUTE, T_ROUTE_K_ID) + ")";
	public static final String T_TRIP_SQL_INSERT = "INSERT INTO " + T_TRIP + " (" + T_TRIP_K_ID + "," + T_TRIP_K_HEADSIGN_TYPE + "," + T_TRIP_K_HEADSIGN_VALUE
			+ "," + T_TRIP_K_ROUTE_ID + ") VALUES(%s)";
	public static final String T_TRIP_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_TRIP);

	public static final String T_STOP = "stop";
	public static final String T_STOP_K_ID = BaseColumns._ID;
	public static final String T_STOP_K_CODE = "code"; // optional
	public static final String T_STOP_K_NAME = "name";
	public static final String T_STOP_K_LAT = "lat";
	public static final String T_STOP_K_LNG = "lng";
	public static final String T_STOP_SQL_CREATE = SqlUtils.CREATE_TABLE_IF_NOT_EXIST + T_STOP + " (" //
			+ T_STOP_K_ID + SqlUtils.INT_PK + ", "//
			+ T_STOP_K_CODE + SqlUtils.TXT + ", " //
			+ T_STOP_K_NAME + SqlUtils.TXT + ", "//
			+ T_STOP_K_LAT + SqlUtils.REAL + ", " //
			+ T_STOP_K_LNG + SqlUtils.REAL + ")";
	public static final String T_STOP_SQL_INSERT = "INSERT INTO " + T_STOP + " (" + T_STOP_K_ID + "," + T_STOP_K_CODE + "," + T_STOP_K_NAME + ","
			+ T_STOP_K_LAT + "," + T_STOP_K_LNG + ") VALUES(%s)";
	public static final String T_STOP_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STOP);

	public static final String T_TRIP_STOPS = "trip_stops";
	public static final String T_TRIP_STOPS_K_ID = BaseColumns._ID;
	public static final String T_TRIP_STOPS_K_TRIP_ID = "trip_id";
	public static final String T_TRIP_STOPS_K_STOP_ID = "stop_id";
	public static final String T_TRIP_STOPS_K_STOP_SEQUENCE = "stop_sequence";
	public static final String T_TRIP_STOPS_K_DECENT_ONLY = "decent_only";
	public static final String T_TRIP_STOPS_SQL_CREATE = SqlUtils.CREATE_TABLE_IF_NOT_EXIST + T_TRIP_STOPS + "(" //
			+ T_TRIP_STOPS_K_ID + SqlUtils.INT_PK_AUTO + ", "//
			+ T_TRIP_STOPS_K_TRIP_ID + SqlUtils.INT + ", "//
			+ T_TRIP_STOPS_K_STOP_ID + SqlUtils.INT + ", "//
			+ T_TRIP_STOPS_K_STOP_SEQUENCE + SqlUtils.INT + ", "//
			+ T_TRIP_STOPS_K_DECENT_ONLY + SqlUtils.INT + "," //
			+ SqlUtils.getSQLForeignKey(T_TRIP_STOPS_K_TRIP_ID, T_TRIP, T_TRIP_K_ID) + ", "//
			+ SqlUtils.getSQLForeignKey(T_TRIP_STOPS_K_STOP_ID, T_STOP, T_STOP_K_ID) + ")";
	public static final String T_TRIP_STOPS_SQL_INSERT = "INSERT INTO " + T_TRIP_STOPS + " (" + T_TRIP_STOPS_K_TRIP_ID + "," + T_TRIP_STOPS_K_STOP_ID + ","
			+ T_TRIP_STOPS_K_STOP_SEQUENCE + "," + T_TRIP_STOPS_K_DECENT_ONLY + ") VALUES(%s)";
	public static final String T_TRIP_STOPS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_TRIP_STOPS);

	public static final String T_SERVICE_DATES = "service_dates";
	public static final String T_SERVICE_DATES_K_SERVICE_ID = "service_id";
	public static final String T_SERVICE_DATES_K_DATE = "date";
	private static final String T_SERVICE_DATES_SQL_CREATE = SqlUtils.CREATE_TABLE_IF_NOT_EXIST + T_SERVICE_DATES + " (" //
			+ T_SERVICE_DATES_K_SERVICE_ID + SqlUtils.TXT + ", "//
			+ T_SERVICE_DATES_K_DATE + SqlUtils.INT//
			+ ");";
	public static final String T_SERVICE_DATES_SQL_INSERT = "INSERT INTO " + T_SERVICE_DATES + " (" + T_SERVICE_DATES_K_SERVICE_ID + ","
			+ T_SERVICE_DATES_K_DATE + ") VALUES(%s)";
	private static final String T_SERVICE_DATES_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_SERVICE_DATES);

	public static final String T_ROUTE_TRIP_STOP_STATUS = StatusProvider.StatusDbHelper.T_STATUS;
	private static final String T_ROUTE_TRIP_STOP_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreate(T_ROUTE_TRIP_STOP_STATUS);
	private static final String T_ROUTE_TRIP_STOP_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_ROUTE_TRIP_STOP_STATUS);

	private Context context;

	private static int dbVersion = -1;

	/**
	 * Override if multiple {@link GTFSRouteTripStopDbHelper} in same app.
	 */
	public static int getDbVersion(Context context) {
		if (dbVersion < 0) {
			dbVersion = context.getResources().getInteger(R.integer.gtfs_rts_db_version);
		}
		return dbVersion;
	}

	public GTFSRouteTripStopDbHelper(Context context) {
		super(context, DB_NAME, null, getDbVersion(context));
		this.context = context;
	}

	@Override
	public void onCreateMT(SQLiteDatabase db) {
		initAllDbTables(db, false);
	}

	@Override
	public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(T_TRIP_STOPS_SQL_DROP);
		db.execSQL(T_STOP_SQL_DROP);
		db.execSQL(T_TRIP_SQL_DROP);
		db.execSQL(T_ROUTE_SQL_DROP);
		db.execSQL(T_SERVICE_DATES_SQL_DROP);
		db.execSQL(T_ROUTE_TRIP_STOP_STATUS_SQL_DROP);
		initAllDbTables(db, true);
	}

	public boolean isDbExist(Context context) {
		return SqlUtils.isDbExist(context, DB_NAME);
	}

	private void initAllDbTables(SQLiteDatabase db, boolean upgrade) {
		int nId = TimeUtils.currentTimeSec();
		int nbTotalOperations = 6;
		NotificationCompat.Builder nb = new NotificationCompat.Builder(this.context) //
				.setSmallIcon(android.R.drawable.stat_notify_sync)//
				.setContentTitle(PackageManagerUtils.getAppVersionName(this.context)) //
				.setContentText(this.context.getString(upgrade ? R.string.db_upgrading : R.string.db_deploying)) //
				.setProgress(nbTotalOperations, 0, true);
		NotificationManager nm = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(nId, nb.build());
		// global settings
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
	}

	private void initDbTableWithRetry(SQLiteDatabase db, String table, String sqlCreate, String sqlInsert, String sqlDrop, int[] files) {
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

	private boolean initDbTable(SQLiteDatabase db, String table, String sqlCreate, String sqlInsert, String sqlDrop, int[] files) {
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
					isr = new InputStreamReader(is, "UTF8");
					br = new BufferedReader(isr, 8192);
					while ((line = br.readLine()) != null) {
						db.execSQL(String.format(sqlInsert, line));
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
	 * Override if multiple {@link GTFSRouteTripStopDbHelper} implementations in same app.
	 */
	private int[] getServiceDatesFiles() {
		return new int[] { R.raw.gtfs_schedule_service_dates };
	}

	/**
	 * Override if multiple {@link GTFSRouteTripStopDbHelper} implementations in same app.
	 */
	public int[] getRouteFiles() {
		return new int[] { R.raw.gtfs_rts_routes };
	}

	/**
	 * Override if multiple {@link GTFSRouteTripStopDbHelper} implementations in same app.
	 */
	public int[] getStopFiles() {
		return new int[] { R.raw.gtfs_rts_stops };
	}

	/**
	 * Override if multiple {@link GTFSRouteTripStopDbHelper} implementations in same app.
	 */
	public int[] getTripFiles() {
		return new int[] { R.raw.gtfs_rts_trips };
	}

	/**
	 * Override if multiple {@link GTFSRouteTripStopDbHelper} in same app.
	 */
	public int[] getTripStopsFiles() {
		return new int[] { R.raw.gtfs_rts_trip_stops };
	}

}
