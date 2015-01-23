package org.mtransit.android.commons.provider;

import org.mtransit.android.commons.SqlUtils;

import android.content.Context;
import android.provider.BaseColumns;

public abstract class StatusDbHelper extends MTSQLiteOpenHelper {

	private static final String TAG = StatusDbHelper.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final String T_STATUS = "status";
	public static final String T_STATUS_K_ID = BaseColumns._ID;
	public static final String T_STATUS_K_TYPE = "type";
	public static final String T_STATUS_K_TARGET_UUID = "target";
	public static final String T_STATUS_K_LAST_UPDATE = "last_update";
	public static final String T_STATUS_K_MAX_VALIDITY = "max_validity";
	public static final String T_STATUS_K_READ_FROM_SOURCE_AT_IN_MS = "read_from_source_at"; // TODO BUMP STATUS DB VERSION !
	public static final String T_STATUS_K_EXTRAS = "extras";
	public static final String T_STATUS_SQL_CREATE = getSqlCreate(T_STATUS);
	public static final String T_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STATUS);

	public StatusDbHelper(Context context, String dbName, int dbVersion) {
		super(context, dbName, null, dbVersion);
	}

	public abstract String getDbName();

	public static String getFkColumnName(String columnName) {
		return "fk" + "_" + columnName;
	}

	public static String getSqlCreate(String table, String... createLines) {
		StringBuilder sqlCreateSb = new StringBuilder(SqlUtils.CREATE_TABLE_IF_NOT_EXIST).append(table).append(" (") //
				.append(T_STATUS_K_ID).append(SqlUtils.INT_PK).append(", ") //
				.append(T_STATUS_K_TYPE).append(SqlUtils.INT).append(", ") //
				.append(T_STATUS_K_TARGET_UUID).append(SqlUtils.TXT).append(", ") //
				.append(T_STATUS_K_LAST_UPDATE).append(SqlUtils.INT).append(", ") //
				.append(T_STATUS_K_MAX_VALIDITY).append(SqlUtils.INT).append(", ") //
				.append(T_STATUS_K_READ_FROM_SOURCE_AT_IN_MS).append(SqlUtils.INT).append(", ") //
				.append(T_STATUS_K_EXTRAS).append(SqlUtils.TXT);
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

}
