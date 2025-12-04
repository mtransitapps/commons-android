package org.mtransit.android.commons.provider.gtfs

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.sqlite.transaction
import org.mtransit.android.commons.FileUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.GTFSProviderDbHelper
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.GTFSCommons
import org.mtransit.commons.sql.SQLUtils
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object GTFSProviderDBHelperUtils : MTLog.Loggable {

    private val LOG_TAG: String = GTFSProviderDBHelperUtils::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    private const val MAX_DB_INIT_RETRIES = 3

    @JvmStatic
    @JvmOverloads
    fun initDbTableWithRetry(
        context: Context,
        db: SQLiteDatabase,
        table: String,
        sqlCreate: String?,
        sqlInsert: String,
        sqlDrop: String?,
        files: IntArray,
        sameColumnsCount: Int = 0,
        otherColumnsCount: Int = 0,
        allStrings: Map<Int, String>? = null,
        stringsColumnIdx: IntArray? = null,
        addStrings: (Int, String) -> Unit = { _, _ -> },
        openRawResource: (Int) -> InputStream = { context.resources.openRawResource(it) }
    ) {
        var tried = 0
        var success: Boolean
        do {
            try {
                success = db.initDbTable(table, sqlCreate, sqlInsert, sqlDrop, files, allStrings, stringsColumnIdx, addStrings, sameColumnsCount, otherColumnsCount, openRawResource)
            } catch (e: Exception) {
                MTLog.w(this, e, "Error while deploying DB table '$table'!")
                success = false
            }
            tried++
        } while (!success && tried < MAX_DB_INIT_RETRIES)
    }

    private fun SQLiteDatabase.initDbTable(
        table: String,
        sqlCreate: String?,
        sqlInsert: String,
        sqlDrop: String?,
        files: IntArray,
        allStrings: Map<Int, String>?,
        stringsColumnIdx: IntArray?,
        addStrings: (Int, String) -> Unit,
        sameColumnsCount: Int,
        otherColumnsCount: Int,
        openRawResource: (Int) -> InputStream,
    ): Boolean {
        try {
            transaction {
                execSQL(sqlDrop) // drop if exists
                execSQL(sqlCreate) // create if not exists
                for (file in files) {
                    try {
                        openRawResource(file).use { inputStream ->
                            InputStreamReader(inputStream, FileUtils.getUTF8()).use { inputStreamReader ->
                                BufferedReader(inputStreamReader).forEachLine { flattenLines ->
                                    flattenLines.toLines(sameColumnsCount, otherColumnsCount).forEach { unflattenLine ->
                                        var line = unflattenLine
                                        if (FeatureFlags.F_EXPORT_STRINGS) {
                                            if (allStrings != null && stringsColumnIdx != null && stringsColumnIdx.isNotEmpty()) {
                                                line = GTFSStringsUtils.replaceLineStrings(line, allStrings, stringsColumnIdx)
                                            } else if (table == GTFSCommons.T_STRINGS) {
                                                GTFSStringsUtils.fromFileLine(line)?.let { (id, string) ->
                                                    addStrings(id, string)
                                                }
                                            }
                                        }
                                        val sql = String.format(sqlInsert, line)
                                        try {
                                            execSQL(sql)
                                        } catch (e: Exception) {
                                            MTLog.w(
                                                this,
                                                e,
                                                "ERROR while executing '$sql' on database '${GTFSProviderDbHelper.DB_NAME}' table '$table' file '$file'!"
                                            )
                                            throw e
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        MTLog.w(this, e, "ERROR while copying the database '${GTFSProviderDbHelper.DB_NAME}' table '$table' file '$file'!")
                        return false
                    }
                }
            }
            return true
        } catch (e: Exception) {
            MTLog.w(this, e, "ERROR while copying the database '${GTFSProviderDbHelper.DB_NAME}' table '$table' file!")
            return false
        }
    }

    private fun String.toLines(sameColumnsCount: Int, otherColumnsCount: Int): List<String> {
        if (sameColumnsCount == 0 || otherColumnsCount == 0) return listOf(this)
        return split(SQLUtils.COLUMN_SEPARATOR)
            .let {
                it.take(sameColumnsCount) to it.drop(sameColumnsCount).chunked(otherColumnsCount)
            }.let { (sameColumns, otherColumns) ->
                buildList {
                    otherColumns.forEach { otherColumn ->
                        add((sameColumns + otherColumn).joinToString(SQLUtils.COLUMN_SEPARATOR))
                    }
                }
            }
    }
}
