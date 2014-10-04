package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.SqlUtils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class POIDbHelper extends MTSQLiteOpenHelper {

	private static final String TAG = POIDbHelper.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link POIDbHelper} implementations in same app.
	 */
	public static final String DB_NAME = "poi.db";

	public static final int DB_VERSION = 2;

	public static final String T_POI = "poi";
	public static final String T_POI_K_ID = BaseColumns._ID;
	public static final String T_POI_K_NAME = "name";
	public static final String T_POI_K_LAT = "lat";
	public static final String T_POI_K_LNG = "lng";
	public static final String T_POI_K_TYPE = "type";
	public static final String T_POI_K_STATUS_TYPE = "statustype";

	public static final String T_POI_SQL_CREATE = getSqlCreate();
	public static final String T_POI_SQL_INSERT = getSqlInsert();
	public static final String T_POI_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_POI);

	public POIDbHelper(Context context) {
		this(context, DB_NAME, DB_VERSION);
	}

	public POIDbHelper(Context context, String dbName, int dbVersion) {
		super(context, dbName, null, dbVersion);
	}

	@Override
	public void onCreateMT(SQLiteDatabase db) {
		initAllDbTables(db);
	}

	@Override
	public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL(T_POI_SQL_DROP);
		initAllDbTables(db);
	}

	private void initAllDbTables(SQLiteDatabase db) {
		db.execSQL(T_POI_SQL_CREATE);
	}

	/**
	 * Override if multiple {@link POIDbHelper} implementations in same app.
	 */
	public String getDbName() {
		return DB_NAME;
	}

	/**
	 * Override if multiple {@link POIDbHelper} in same app.
	 */
	public static int getDbVersion(Context context) {
		return DB_VERSION;
	}

	public static String getSqlCreate(String... createLines) {
		StringBuilder sqlCreateSb = new StringBuilder(SqlUtils.CREATE_TABLE_IF_NOT_EXIST).append(T_POI).append(" (") //
				.append(T_POI_K_ID).append(SqlUtils.INT_PK).append(", ")//
				.append(T_POI_K_NAME).append(SqlUtils.TXT).append(", ")//
				.append(T_POI_K_LAT).append(SqlUtils.REAL).append(", ") //
				.append(T_POI_K_LNG).append(SqlUtils.REAL).append(", ") //
				.append(T_POI_K_TYPE).append(SqlUtils.INT).append(", ") //
				.append(T_POI_K_STATUS_TYPE).append(SqlUtils.INT);
		if (createLines != null) {
			for (String createLine : createLines) {
				if (sqlCreateSb.length() > 0) {
					sqlCreateSb.append(", ");
				}
				sqlCreateSb.append(createLine);
			}
		}
		sqlCreateSb.append(")");
		return sqlCreateSb.toString();
	}

	public static String getSqlInsert(String... columns) {
		StringBuilder sqlInsertSb = new StringBuilder("INSERT INTO ").append(T_POI).append(" (") //
				.append(T_POI_K_ID).append(",") //
				.append(T_POI_K_NAME).append(",")//
				.append(T_POI_K_LAT).append(",") //
				.append(T_POI_K_LNG).append(",") //
				.append(T_POI_K_TYPE).append(",") //
				.append(T_POI_K_STATUS_TYPE);
		if (columns != null) {
			for (String column : columns) {
				if (sqlInsertSb.length() > 0) {
					sqlInsertSb.append(",");
				}
				sqlInsertSb.append(column);
			}
		}
		sqlInsertSb.append(") VALUES(%s)");
		return sqlInsertSb.toString();
	}

}
