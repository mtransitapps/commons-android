package org.mtransit.android.commons;

import java.util.Arrays;
import java.util.Collection;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.ArrayMap;

public final class SqlUtils {

	private static final String TAG = SqlUtils.class.getSimpleName();

	private static final String CREATE_TABLE = "CREATE TABLE ";
	private static final String CREATE_TABLE_IF_NOT_EXIST = CREATE_TABLE + "IF NOT EXISTS ";
	private static final String INSERT_INTO = "INSERT INTO ";
	private static final String INSERT_INTO_VALUES = ") VALUES(%s)";
	private static final String DROP_TABLE = "DROP TABLE ";
	private static final String DROP_TABLE_IF_EXISTS = DROP_TABLE + "IF EXISTS ";

	public static final String INT = " integer";
	public static final String INT_PK = INT + " PRIMARY KEY";
	public static final String INT_PK_AUTO = INT_PK + " AUTOINCREMENT";
	public static final String TXT = " text";
	public static final String REAL = " real";

	private static final String STRING_DELIMITER = "'";
	private static final String POINT = ".";
	public static final String P2 = ")";
	public static final String P1 = "(";
	private static final String PERCENT = "%";
	private static final String GT = ">";
	private static final String LT = "<";
	private static final String EQ = "=";
	private static final String NE = "!=";
	private static final String COLUMN_SEPARATOR = ",";
	private static final String LIKE = " LIKE ";
	private static final String ON = " ON ";
	private static final String INNER_JOIN = " INNER JOIN ";
	private static final String FOREIGN_KEY_REFERENCES = " REFERENCES ";
	private static final String FOREIGN_KEY = " FOREIGN KEY ";
	private static final String AS = " AS ";
	private static final String ASC = " ASC";
	private static final String DESC = " DESC";
	public static final String NOT = " NOT ";
	private static final String BETWEEN = " BETWEEN ";
	public static final String AND = " AND ";
	public static final String OR = " OR ";
	private static final String IN = " IN ";

	public static class SQLCreateBuilder {

		public static SQLCreateBuilder getNew(String table) {
			return new SQLCreateBuilder(table);
		}

		private StringBuilder sqlCreateSb;

		private int nbColumn = 0;

		private SQLCreateBuilder(String table) {
			this.sqlCreateSb = new StringBuilder(CREATE_TABLE_IF_NOT_EXIST).append(table).append(P1);
		}

		public SQLCreateBuilder appendColumn(String name, String type) {
			if (this.nbColumn > 0) {
				this.sqlCreateSb.append(COLUMN_SEPARATOR);
			}
			this.sqlCreateSb.append(name).append(type);
			this.nbColumn++;
			return this;
		}

		public SQLCreateBuilder appendColumns(String[]... createColumnNameAndTypes) {
			if (createColumnNameAndTypes != null) {
				for (String createLine[] : createColumnNameAndTypes) {
					appendColumn(createLine[0], createLine[1]);
				}
			}
			return this;
		}

		public SQLCreateBuilder appendForeignKey(String columnName, String fkTable, String fkColumn) {
			if (this.nbColumn > 0) {
				this.sqlCreateSb.append(COLUMN_SEPARATOR);
			}
			this.sqlCreateSb.append(getSQLForeignKey(columnName, fkTable, fkColumn));
			this.nbColumn++;
			return this;
		}

		public String build() {
			return this.sqlCreateSb.append(P2).toString();
		}
	}

	public static class SQLInsertBuilder {

		public static SQLInsertBuilder getNew(String table) {
			return new SQLInsertBuilder(table);
		}

		private StringBuilder sqlInstertSb;

		private int nbColumn = 0;

		private SQLInsertBuilder(String table) {
			this.sqlInstertSb = new StringBuilder(INSERT_INTO).append(table).append(P1);
		}

		public SQLInsertBuilder appendColumn(String name) {
			if (nbColumn > 0) {
				this.sqlInstertSb.append(COLUMN_SEPARATOR);
			}
			this.sqlInstertSb.append(name);
			nbColumn++;
			return this;
		}

		public SQLInsertBuilder appendColumns(String... insertColumns) {
			if (insertColumns != null) {
				for (String insertColumn : insertColumns) {
					appendColumn(insertColumn);
				}
			}
			return this;
		}

		public String build() {
			return this.sqlInstertSb.append(INSERT_INTO_VALUES).toString();
		}
	}

	public static String getSQLDropIfExistsQuery(String table) {
		return DROP_TABLE_IF_EXISTS + table;
	}

	private static String getSQLForeignKey(String columnName, String fkTable, String fkColumn) {
		return FOREIGN_KEY + P1 + columnName + P2 + FOREIGN_KEY_REFERENCES + fkTable + P1 + fkColumn + P2;
	}

	public static String getTableColumn(String table, String column) {
		return table + POINT + column;
	}

	public static class ProjectionMapBuilder {

		public static ProjectionMapBuilder getNew() {
			return new ProjectionMapBuilder();
		}

		private ArrayMap<String, String> map;

		private ProjectionMapBuilder() {
			this.map = new ArrayMap<String, String>();
		}

		public ProjectionMapBuilder appendValue(Object value, String alias) {
			appendProjection(this.map, value, alias);
			return this;
		}

		public ProjectionMapBuilder appendTableColumn(String table, String column, String alias) {
			appendProjection(this.map, getTableColumn(table, column), alias);
			return this;
		}

		public ArrayMap<String, String> build() {
			return this.map;
		}
	}

	public static void appendProjection(ArrayMap<String, String> projectionMap, Object value, String alias) {
		projectionMap.put(alias, value + AS + alias);
	}

	public static class JoinBuilder {

		public static JoinBuilder getNew(String table) {
			return new JoinBuilder(table);
		}

		private StringBuilder sqlJoinSb;

		private JoinBuilder(String table) {
			this.sqlJoinSb = new StringBuilder(table);
		}

		public JoinBuilder innerJoin(String table, String table1, String column1, String table2, String column2) {
			this.sqlJoinSb.append(INNER_JOIN).append(table).append(ON) //
					.append(getTableColumn(table1, column1)) //
					.append(EQ) //
					.append(getTableColumn(table2, column2));
			return this;
		}

		public String build() {
			return this.sqlJoinSb.toString();
		}
	}

	public static String getLike(String tableColumn, String value) {
		return tableColumn + LIKE + STRING_DELIMITER + PERCENT + value + PERCENT + STRING_DELIMITER;
	}

	public static String getWhereGroup(String andOr, String... whereClauses) {
		StringBuilder sb = new StringBuilder(P1);
		if (whereClauses != null) {
			for (String whereClause : whereClauses) {
				if (sb.length() > 0) {
					sb.append(andOr);
				}
				sb.append(whereClause);
			}
		}
		return sb.append(P2).toString();
	}

	public static String getBetween(String tableColumn, Object value1, Object value2) {
		return tableColumn + BETWEEN + value1 + AND + value2;
	}

	public static String mergeSortOrder(String... sortOrders) {
		StringBuilder sb = new StringBuilder();
		if (sortOrders != null) {
			for (String sortOrder : sortOrders) {
				if (sb.length() > 0) {
					sb.append(COLUMN_SEPARATOR);
				}
				sb.append(sortOrder);
			}
		}
		return sb.toString();
	}

	public static String getSortOrderAscending(String column) {
		return column + ASC;
	}

	public static String getSortOrderDescending(String column) {
		return column + DESC;
	}

	public static String getWhereEquals(String column, Object value) {
		return column + EQ + value;
	}

	public static String getWhereInferior(String column, Object value) {
		return column + LT + value;
	}

	public static String getWhereSuperior(String column, Object value) {
		return column + GT + value;
	}

	public static String getWhereEqualsString(String column, String value) {
		return column + EQ + escapeString(value);
	}

	public static String getWhereIn(String tableColumn, Collection<?> values) {
		StringBuilder sb = new StringBuilder(tableColumn).append(IN).append(P1);
		if (values != null) {
			int i = 0;
			for (Object value : values) {
				if (i > 0) {
					sb.append(COLUMN_SEPARATOR);
				}
				sb.append(value);
				i++;
			}
		}
		return sb.append(P2).toString();
	}

	public static String getWhereInString(String tableColumn, Collection<String> values) {
		StringBuilder sb = new StringBuilder(tableColumn).append(IN).append(P1);
		if (values != null) {
			int i = 0;
			for (String value : values) {
				if (i > 0) {
					sb.append(COLUMN_SEPARATOR);
				}
				sb.append(escapeString(value));
				i++;
			}
		}
		return sb.append(P2).toString();
	}

	public static String escapeString(String string) {
		return STRING_DELIMITER + string + STRING_DELIMITER;
	}

	private SqlUtils() {
	}

	public static int getCurrentDbVersion(Context context, String dbName) {
		SQLiteDatabase db = null;
		try {
			String dbPath = context.getDatabasePath(dbName).getPath();
			db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
			return db.getVersion();
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while reading current DB version!");
			return -1;
		} finally {
			closeQuietly(db);
		}
	}

	private static final int BOOLEAN_TRUE = 1;
	private static final int BOOLEAN_FALSE = 0;

	public static boolean getBoolean(Cursor cursor, String columnName) {
		return getBoolean(cursor, cursor.getColumnIndexOrThrow(columnName));
	}

	public static boolean getBoolean(Cursor cursor, int columnIndex) {
		return cursor.getInt(columnIndex) == BOOLEAN_TRUE;
	}

	public static int toSQLBoolean(boolean value) {
		return value ? BOOLEAN_TRUE : BOOLEAN_FALSE;
	}

	public static String getWhereBooleanNotTrue(String tableColumn) {
		return tableColumn + NE + BOOLEAN_TRUE;
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

	public static void closeQuietly(Cursor cursor) {
		try {
			close(cursor);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while closing cursor!");
		}
	}

	public static void close(Cursor cursor) {
		if (cursor != null) {
			cursor.close();
		}
	}

	private static void closeQuietly(SQLiteDatabase db) {
		try {
			close(db);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while closing DB!");
		}
	}

	private static void close(SQLiteDatabase db) {
		if (db != null) {
			db.close();
		}
	}

	public static void endTransactionQuietly(SQLiteDatabase db) {
		try {
			endTransaction(db);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while ending transaction DB!");
		}
	}

	public static void endTransaction(SQLiteDatabase db) {
		if (db != null) {
			db.endTransaction();
		}
	}
}
