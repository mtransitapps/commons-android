package org.mtransit.android.commons.provider.serviceupdate

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.provider.common.MTSQLiteOpenHelper
import org.mtransit.commons.sql.SQLCreateBuilder

abstract class ServiceUpdateDbHelper(
    context: Context?,
    dbName: String?,
    factory: SQLiteDatabase.CursorFactory?,
    dbVersion: Int,
) : MTSQLiteOpenHelper(context, dbName, factory, dbVersion) {

    companion object {
        const val T_SERVICE_UPDATE = "service_update"

        const val T_SERVICE_UPDATE_K_ID: String = BaseColumns._ID
        const val T_SERVICE_UPDATE_K_TARGET_UUID = "target"
        const val T_SERVICE_UPDATE_K_TARGET_TRIP_ID = "trip_id"
        const val T_SERVICE_UPDATE_K_LAST_UPDATE = "last_update"
        const val T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS = "max_validity"
        const val T_SERVICE_UPDATE_K_SEVERITY = "severity"
        const val T_SERVICE_UPDATE_K_TEXT = "text"
        const val T_SERVICE_UPDATE_K_TEXT_HTML = "text_html"
        const val T_SERVICE_UPDATE_K_LANGUAGE = "lang"
        const val T_SERVICE_UPDATE_K_SOURCE_LABEL = "source_label"
        const val T_SERVICE_UPDATE_K_ORIGINAL_ID = "original_id"
        const val T_SERVICE_UPDATE_K_SOURCE_ID = "source_id"

        @Suppress("unused")
        val T_SERVICE_UPDATE_SQL_CREATE = getSqlCreateBuilder(T_SERVICE_UPDATE).build()

        @Suppress("unused")
        val T_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_SERVICE_UPDATE)

        @Suppress("unused")
        fun getFkColumnName(columnName: String) = "fk_$columnName"

        @JvmStatic
        fun getSqlCreateBuilder(table: String) = SQLCreateBuilder.getNew(table)
            .appendColumn(T_SERVICE_UPDATE_K_ID, SqlUtils.INT_PK_AUTO)
            .appendColumn(T_SERVICE_UPDATE_K_TARGET_UUID, SqlUtils.TXT)
            .appendColumn(T_SERVICE_UPDATE_K_TARGET_TRIP_ID, SqlUtils.TXT)
            .appendColumn(T_SERVICE_UPDATE_K_LAST_UPDATE, SqlUtils.INT)
            .appendColumn(T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS, SqlUtils.INT)
            .appendColumn(T_SERVICE_UPDATE_K_SEVERITY, SqlUtils.INT)
            .appendColumn(T_SERVICE_UPDATE_K_TEXT, SqlUtils.TXT)
            .appendColumn(T_SERVICE_UPDATE_K_TEXT_HTML, SqlUtils.TXT)
            .appendColumn(T_SERVICE_UPDATE_K_LANGUAGE, SqlUtils.TXT)
            .appendColumn(T_SERVICE_UPDATE_K_ORIGINAL_ID, SqlUtils.TXT)
            .appendColumn(T_SERVICE_UPDATE_K_SOURCE_LABEL, SqlUtils.TXT)
            .appendColumn(T_SERVICE_UPDATE_K_SOURCE_ID, SqlUtils.TXT)
    }

    abstract val dbName: String
}
