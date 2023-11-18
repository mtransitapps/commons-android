package org.mtransit.android.commons.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;

public class BikeStationDbHelper extends MTSQLiteOpenHelper {

	private static final String LOG_TAG = BikeStationDbHelper.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	/**
	 * Override if multiple {@link BikeStationDbHelper} implementations in same app.
	 */
	protected static final String DB_NAME = "bikestation.db";

	/**
	 * Override if multiple {@link BikeStationDbHelper} implementations in same app.
	 */
	static final String PREF_KEY_LAST_UPDATE_MS = "pBikeStationLastUpdate";
	/**
	 * Override if multiple {@link BikeStationDbHelper} implementations in same app.
	 */
	static final String PREF_KEY_STATUS_LAST_UPDATE_MS = "pBikeStationStatusLastUpdate";

	static final String T_BIKE_STATION = POIProvider.POIDbHelper.T_POI;
	private static final String T_BIKE_STATION_SQL_CREATE = POIProvider.POIDbHelper.getSqlCreateBuilder(T_BIKE_STATION).build();
	private static final String T_BIKE_STATION_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_BIKE_STATION);

	static final String T_BIKE_STATION_STATUS = StatusProvider.StatusDbHelper.T_STATUS;
	private static final String T_BIKE_STATION_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_BIKE_STATION_STATUS).build();
	private static final String T_BIKE_STATION_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_BIKE_STATION_STATUS);

	private static int dbVersion = -1;

	/**
	 * Override if multiple {@link BikeStationDbHelper} in same app.
	 */
	public static int getDbVersion(@NonNull Context context) {
		if (dbVersion < 0) {
			dbVersion = context.getResources().getInteger(R.integer.bike_station_db_version);
		}
		return dbVersion;
	}

	@NonNull
	private final Context context;

	BikeStationDbHelper(@NonNull Context context) {
		super(context, DB_NAME, null, getDbVersion(context));
		this.context = context;
	}

	@Override
	public void onCreateMT(@NonNull SQLiteDatabase db) {
		initAllDbTables(db);
	}

	@Override
	public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(T_BIKE_STATION_SQL_DROP);
		db.execSQL(T_BIKE_STATION_STATUS_SQL_DROP);
		PreferenceUtils.savePrefLclSync(this.context, PREF_KEY_LAST_UPDATE_MS, 0L);
		PreferenceUtils.savePrefLclSync(this.context, PREF_KEY_STATUS_LAST_UPDATE_MS, 0L);
		initAllDbTables(db);
	}

	public boolean isDbExist(@NonNull Context context) {
		return SqlUtils.isDbExist(context, DB_NAME);
	}

	private void initAllDbTables(@NonNull SQLiteDatabase db) {
		db.execSQL(T_BIKE_STATION_SQL_CREATE);
		db.execSQL(T_BIKE_STATION_STATUS_SQL_CREATE);
	}
}
