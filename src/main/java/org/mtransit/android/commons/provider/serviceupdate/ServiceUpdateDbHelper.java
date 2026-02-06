package org.mtransit.android.commons.provider.serviceupdate;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.provider.common.MTSQLiteOpenHelper;
import org.mtransit.commons.sql.SQLCreateBuilder;

public abstract class ServiceUpdateDbHelper extends MTSQLiteOpenHelper {

	public static final String T_SERVICE_UPDATE = "service_update";
	static final String T_SERVICE_UPDATE_K_ID = BaseColumns._ID;
	static final String T_SERVICE_UPDATE_K_TARGET_UUID = "target";
	static final String T_SERVICE_UPDATE_K_LAST_UPDATE = "last_update";
	static final String T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS = "max_validity";
	static final String T_SERVICE_UPDATE_K_SEVERITY = "severity";
	static final String T_SERVICE_UPDATE_K_TEXT = "text";
	static final String T_SERVICE_UPDATE_K_TEXT_HTML = "text_html";
	static final String T_SERVICE_UPDATE_K_LANGUAGE = "lang";
	static final String T_SERVICE_UPDATE_K_SOURCE_LABEL = "source_label";
	static final String T_SERVICE_UPDATE_K_ORIGINAL_ID = "original_id";
	static final String T_SERVICE_UPDATE_K_SOURCE_ID = "source_id";

	@SuppressWarnings("unused")
	public static final String T_SERVICE_UPDATE_SQL_CREATE = getSqlCreateBuilder(T_SERVICE_UPDATE).build();

	@SuppressWarnings("unused")
	public static final String T_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_SERVICE_UPDATE);

	public ServiceUpdateDbHelper(@Nullable Context context, @Nullable String dbName, @Nullable SQLiteDatabase.CursorFactory factory, int dbVersion) {
		super(context, dbName, factory, dbVersion);
	}

	@NonNull
	public abstract String getDbName();

	@SuppressWarnings("unused")
	@NonNull
	public static String getFkColumnName(@NonNull String columnName) {
		return "fk" + "_" + columnName;
	}

	@NonNull
	public static SQLCreateBuilder getSqlCreateBuilder(@NonNull String table) {
		return SQLCreateBuilder.getNew(table) //
				.appendColumn(T_SERVICE_UPDATE_K_ID, SqlUtils.INT_PK_AUTO) //
				.appendColumn(T_SERVICE_UPDATE_K_TARGET_UUID, SqlUtils.TXT) //
				.appendColumn(T_SERVICE_UPDATE_K_LAST_UPDATE, SqlUtils.INT) //
				.appendColumn(T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS, SqlUtils.INT) //
				.appendColumn(T_SERVICE_UPDATE_K_SEVERITY, SqlUtils.INT) //
				.appendColumn(T_SERVICE_UPDATE_K_TEXT, SqlUtils.TXT) //
				.appendColumn(T_SERVICE_UPDATE_K_TEXT_HTML, SqlUtils.TXT) //
				.appendColumn(T_SERVICE_UPDATE_K_LANGUAGE, SqlUtils.TXT) //
				.appendColumn(T_SERVICE_UPDATE_K_ORIGINAL_ID, SqlUtils.TXT) //
				.appendColumn(T_SERVICE_UPDATE_K_SOURCE_LABEL, SqlUtils.TXT) //
				.appendColumn(T_SERVICE_UPDATE_K_SOURCE_ID, SqlUtils.TXT) //
				;
	}
}
