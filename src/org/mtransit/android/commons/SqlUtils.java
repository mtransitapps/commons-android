package org.mtransit.android.commons;

import java.util.Arrays;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class SqlUtils {

	private static final String TAG = SqlUtils.class.getSimpleName();

	public static final String CREATE_TABLE = "CREATE TABLE ";
	public static final String CREATE_TABLE_IF_NOT_EXIST = CREATE_TABLE + "IF NOT EXISTS ";
	public static final String DROP_TABLE = "DROP TABLE ";
	public static final String DROP_TABLE_IF_EXISTS = DROP_TABLE + "IF EXISTS ";

	public static final String INT = " integer";
	public static final String INT_PK = INT + " PRIMARY KEY";
	public static final String INT_PK_AUTO = INT_PK + " AUTOINCREMENT";

	public static final String TXT = " text";

	public static final String REAL = " real";

	public static final String INNER_JOIN = " INNER JOIN ";
	public static final String LEFT_OUTER_JOIN = " LEFT OUTER JOIN ";
	public static final String FULL_OUTER_JOIN = " FULL OUTER JOIN ";

	public static String getSQLDropIfExistsQuery(String table) {
		return DROP_TABLE_IF_EXISTS + table;
	}

	public static String getSQLForeignKey(String columnName, String fkTable, String fkColumn) {
		return " FOREIGN KEY(" + columnName + ") REFERENCES " + fkTable + "(" + fkColumn + ")";
	}

	private SqlUtils() {
	}

	public static int getCurrentDbVersion(Context context, String dbName) {
		SQLiteDatabase db = null;
		try {
			db = SQLiteDatabase.openDatabase(context.getDatabasePath(dbName).getPath(), null, SQLiteDatabase.OPEN_READONLY);
			return db.getVersion();
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error while reading current DB version!");
			return -1;
		} finally {
			if (db != null) {
				db.close();
			}
		}
	}

	public static boolean getBoolean(Cursor cursor, String columnName) {
		return cursor.getInt(cursor.getColumnIndexOrThrow(columnName)) == 1;
	}

	public static int toSQLBoolean(boolean value) {
		return value ? 1 : 0;
	}

	public static boolean isDbExist(Context context, String dbName) {
		return Arrays.asList(context.databaseList()).contains(dbName);
	}

	private static final String CONCATENATE_SEPARATOR = "||";

	public static String concatenate(String separator, String... strings) {
		StringBuilder sb = new StringBuilder();
		if (strings != null && strings.length > 0) {
			for (String string : strings) {
				if (sb.length() > 0) {
					sb.append(CONCATENATE_SEPARATOR).append(separator).append(CONCATENATE_SEPARATOR);
				}
				sb.append(string);
			}
		}
		return sb.toString();
	}

}
