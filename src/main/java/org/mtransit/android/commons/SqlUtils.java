package org.mtransit.android.commons;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.collection.SimpleArrayMap;

import org.mtransit.commons.sql.SQLUtils;

import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings("WeakerAccess")
public final class SqlUtils {

	private static final String LOG_TAG = SqlUtils.class.getSimpleName();

	public static final String INT = SQLUtils.INT;
	public static final String INT_PK = SQLUtils.INT_PK;
	public static final String INT_PK_AUTO = SQLUtils.INT_PK_AUTO;
	public static final String TXT = SQLUtils.TXT;
	public static final String REAL = SQLUtils.REAL;

	public static final String P2 = SQLUtils.P2;
	public static final String P1 = SQLUtils.P1;
	public static final String NOT = SQLUtils.NOT;
	public static final String AND = SQLUtils.AND;
	public static final String OR = SQLUtils.OR;

	@NonNull
	public static String getSQLDropIfExistsQuery(@NonNull String table) {
		return SQLUtils.getSQLDropIfExistsQuery(table);
	}

	@NonNull
	public static String getTableColumn(@NonNull String table, @NonNull String column) {
		return SQLUtils.getTableColumn(table, column);
	}

	public static class ProjectionMapBuilder { // Android only

		@NonNull
		public static ProjectionMapBuilder getNew() {
			return new ProjectionMapBuilder();
		}

		@NonNull
		private final ArrayMap<String, String> map;

		private ProjectionMapBuilder() {
			this.map = new ArrayMap<>();
		}

		@NonNull
		public ProjectionMapBuilder appendValue(@NonNull Object value, @NonNull String alias) {
			appendProjection(this.map, value, alias);
			return this;
		}

		@NonNull
		public ProjectionMapBuilder appendTableColumn(@NonNull String table, @NonNull String column, @NonNull String alias) {
			appendProjection(this.map, getTableColumn(table, column), alias);
			return this;
		}

		@NonNull
		public ArrayMap<String, String> build() {
			return this.map;
		}
	}

	public static void appendProjection(@NonNull SimpleArrayMap<String, String> projectionMap, @NonNull Object value, @NonNull String alias) {
		projectionMap.put(alias, value + SQLUtils.AS + alias);
	}

	@NonNull
	public static String getLike(@NonNull String tableColumn, @NonNull String value) {
		return SQLUtils.getLike(tableColumn, value);
	}

	@NonNull
	public static String getWhereGroup(@NonNull String andOr, @NonNull String... whereClauses) {
		return SQLUtils.getWhereGroup(andOr, whereClauses);
	}

	@NonNull
	public static String getBetween(@NonNull String tableColumn, @NonNull Object value1, @NonNull Object value2) {
		return SQLUtils.getBetween(tableColumn, value1, value2);
	}

	@NonNull
	public static String mergeSortOrder(@NonNull String... sortOrders) {
		return SQLUtils.mergeSortOrder(sortOrders);
	}

	@NonNull
	public static String getSortOrderAscending(@NonNull String column) {
		return SQLUtils.getSortOrderAscending(column);
	}

	@NonNull
	public static String getSortOrderDescending(@NonNull String column) {
		return SQLUtils.getSortOrderDescending(column);
	}

	@NonNull
	public static String getWhereEquals(@NonNull String column, @NonNull Object value) {
		return SQLUtils.getWhereEquals(column, value);
	}

	@NonNull
	public static String getWhereInferior(@NonNull String column, @NonNull Object value) {
		return SQLUtils.getWhereInferior(column, value);
	}

	@NonNull
	public static String getWhereSuperior(@NonNull String column, @NonNull Object value) {
		return SQLUtils.getWhereSuperior(column, value);
	}

	@NonNull
	public static String getWhereEqualsString(@NonNull String column, @NonNull String value) {
		return SQLUtils.getWhereEqualsString(column, value);
	}

	@NonNull
	public static String getWhereIn(@NonNull String tableColumn, @NonNull Collection<?> values) {
		return SQLUtils.getWhereIn(tableColumn, values, false);
	}

	@NonNull
	public static String getWhereNotIn(@NonNull String tableColumn, @NonNull Collection<?> values) {
		return SQLUtils.getWhereIn(tableColumn, values, true);
	}

	@NonNull
	public static String getWhereInString(@NonNull String tableColumn, @Nullable Collection<String> values) {
		return SQLUtils.getWhereInString(tableColumn, values, false);
	}

	@NonNull
	public static String getWhereNotInString(@NonNull String tableColumn, @Nullable Collection<String> values) {
		return SQLUtils.getWhereInString(tableColumn, values, true);
	}

	@NonNull
	public static String escapeString(@NonNull String string) {
		return SQLUtils.escapeString(string);
	}

	private SqlUtils() {
	}

	public static int getCurrentDbVersion(@NonNull Context context, @NonNull String dbName) { // Android only
		SQLiteDatabase db = null;
		try {
			String dbPath = context.getDatabasePath(dbName).getPath();
			db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
			return db.getVersion();
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while reading current DB version!");
			return -1;
		} finally {
			closeQuietly(db);
		}
	}

	@Deprecated
	public static boolean getBoolean(@NonNull Cursor cursor, @NonNull String columnName) {
		return getBoolean(cursor, cursor.getColumnIndexOrThrow(columnName));
	}

	public static boolean getBoolean(@NonNull Cursor cursor, int columnIndex) {
		return SQLUtils.fromSQLBoolean(cursor.getInt(columnIndex));
	}

	public static int toSQLBoolean(boolean value) {
		return SQLUtils.toSQLBoolean(value);
	}

	@NonNull
	public static String getWhereBooleanNotTrue(@NonNull String tableColumn) {
		return SQLUtils.getWhereBooleanNotTrue(tableColumn);
	}

	@NonNull
	public static String appendToSelection(@Nullable String selection, @NonNull String append) {
		return SQLUtils.appendToSelection(selection, append);
	}

	public static boolean isDbExist(@NonNull Context context, @NonNull String dbName) { // Android only
		return Arrays.asList(context.databaseList()).contains(dbName);
	}

	@SuppressWarnings("UnusedReturnValue")
	public static boolean deleteDb(@NonNull Context context, @NonNull String dbName) { // Android only
		if (!isDbExist(context, dbName)) {
			return false;
		}
		try {
			return context.deleteDatabase(dbName);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while deleting DB '%s'!", dbName);
			return false;
		}
	}

	@NonNull
	public static String concatenate(@NonNull String separator, @NonNull String... strings) {
		return SQLUtils.concatenate(separator, strings);
	}

	public static void closeQuietly(@Nullable Cursor cursor) { // Android only
		try {
			close(cursor);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while closing cursor!");
		}
	}

	public static void close(@Nullable Cursor cursor) { // Android only
		if (cursor != null) {
			cursor.close();
		}
	}

	private static void closeQuietly(@Nullable SQLiteDatabase db) { // Android only
		try {
			close(db);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while closing DB!");
		}
	}

	private static void close(@Nullable SQLiteDatabase db) { // Android only
		if (db != null) {
			db.close();
		}
	}

	public static void endTransactionQuietly(@Nullable SQLiteDatabase db) { // Android only
		try {
			endTransaction(db);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while ending transaction DB!");
		}
	}

	public static void endTransaction(@Nullable SQLiteDatabase db) { // Android only
		if (db != null) {
			db.endTransaction();
		}
	}
}
