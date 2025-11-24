package org.mtransit.android.commons.provider.gtfs

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.mtransit.android.commons.FileUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.provider.GTFSProviderDbHelper
import org.mtransit.android.commons.provider.GTFSStringsUtils
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.GTFSCommons
import java.io.BufferedReader
import java.io.InputStreamReader

object GTFSProviderDBHelperUtils: MTLog.Loggable {

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
        allStrings: Map<Int, String>? = null,
        stringsColumnIdx: IntArray? = null,
        addStrings: (Int, String) -> Unit = { _, _ -> },
    ) {
        var tried = 0
        var success: Boolean
        do {
            try {
                success = initDbTable(context, db, table, sqlCreate, sqlInsert, sqlDrop, files, allStrings, stringsColumnIdx, addStrings)
            } catch (e: Exception) {
                MTLog.w(this, e, "Error while deploying DB table '$table'!")
                success = false
            }
            tried++
        } while (!success && tried < MAX_DB_INIT_RETRIES)
    }

    private fun initDbTable(
        context: Context,
        db: SQLiteDatabase,
        table: String,
        sqlCreate: String?,
        sqlInsert: String,
        sqlDrop: String?,
        files: IntArray,
        allStrings: Map<Int, String>?,
        stringsColumnIdx: IntArray?,
        addStrings: (Int, String) -> Unit,
    ): Boolean {
        try {
            db.beginTransaction()
            db.execSQL(sqlDrop) // drop if exists
            db.execSQL(sqlCreate) // create if not exists
            for (file in files) {
                try {
                    context.resources.openRawResource(file).use { inputStream ->
                        InputStreamReader(inputStream, FileUtils.getUTF8()).use { inputStreamReader ->
                            BufferedReader(inputStreamReader).forEachLine {
                                var line = it
                                if (FeatureFlags.F_EXPORT_STRINGS) {
                                    if (allStrings != null && stringsColumnIdx != null && stringsColumnIdx.isNotEmpty()) {
                                        MTLog.d(this, "initDbTable(%s) > B-line: %s.", table, line)
                                        line = GTFSStringsUtils.replaceLineStrings(line, allStrings, stringsColumnIdx)
                                        MTLog.d(this, "initDbTable(%s) > A-line: %s.", table, line)
                                    } else if (table == GTFSCommons.T_STRINGS) {
                                        GTFSStringsUtils.fromFileLine(line)?.let { (id, string) ->
                                            addStrings(id, string)
                                        }
                                    }
                                }
                                val sql = String.format(sqlInsert, line)
                                try {
                                    db.execSQL(sql)
                                } catch (e: Exception) {
                                    MTLog.w(this, e, "ERROR while executing '$sql' on database '${GTFSProviderDbHelper.DB_NAME}' table '$table' file '$file'!")
                                    throw e
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    MTLog.w(this, e, "ERROR while copying the database '${GTFSProviderDbHelper.DB_NAME}' table '$table' file '$file'!")
                    return false
                }
            }
            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            MTLog.w(this, e, "ERROR while copying the database ${GTFSProviderDbHelper.DB_NAME}' table '$table' file!")
            return false
        } finally {
            SqlUtils.endTransactionQuietly(db)
        }
    }
}
