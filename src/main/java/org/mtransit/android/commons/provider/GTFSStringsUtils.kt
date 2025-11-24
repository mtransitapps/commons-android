package org.mtransit.android.commons.provider

import android.database.Cursor
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Schedule
import org.mtransit.commons.GTFSCommons
import org.mtransit.commons.sql.SQLUtils
import org.mtransit.commons.sql.SQLUtils.quotes
import org.mtransit.commons.sql.SQLUtils.unquotes

object GTFSStringsUtils : MTLog.Loggable {

    private val LOG_TAG: String = GTFSStringsUtils::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    @Suppress("DiscouragedApi")
    @JvmStatic
    fun updateStrings(timestamps: Set<Schedule.Timestamp>, gtfsProvider: GTFSProvider): Set<Schedule.Timestamp> {
        val stringIds = timestamps
            .mapNotNull { it.headsignValue?.split(GTFSCommons.STRINGS_SEPARATOR) }
            .flatten()
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?: return timestamps
        val idToStringMap = loadStrings(gtfsProvider, stringIds)
        timestamps.forEach { timestamp ->
            timestamp.headsignValue?.let { headsignValue ->
                timestamp.headsignValue = headsignValue
                    .split(GTFSCommons.STRINGS_SEPARATOR)
                    .mapNotNull { idToStringMap[it.toIntOrNull()] }
                    .joinToString(GTFSCommons.STRINGS_SEPARATOR)
            }
        }
        return timestamps
    }

    private fun loadStrings(gtfsProvider: GTFSProvider, stringIds: List<String>): Map<Int, String> {
        if (stringIds.isEmpty()) return emptyMap()
        return gtfsProvider.readDB.query(
            GTFSProviderDbHelper.T_STRINGS,
            arrayOf(GTFSProviderDbHelper.T_STRINGS_K_ID, GTFSProviderDbHelper.T_STRINGS_K_STRING),
            "${GTFSProviderDbHelper.T_STRINGS_K_ID} IN (${stringIds.joinToString(",")})",
            null,
            null,
            null,
            null
        ).use { cursor ->
            cursorToStrings(cursor)
        }
    }

    private fun cursorToStrings(cursor: Cursor) = buildMap {
        while (cursor.moveToNext()) {
            try {
                put(
                    cursor.getInt(cursor.getColumnIndexOrThrow(GTFSProviderDbHelper.T_STRINGS_K_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(GTFSProviderDbHelper.T_STRINGS_K_STRING)) ?: continue
                )
            } catch (e: Exception) {
                MTLog.w(this@GTFSStringsUtils, e, "Cannot parse strings cursor: '$cursor'!")
            }
        }
    }

    @JvmStatic
    fun fromFileLine(line: String) =
        line.split(SQLUtils.COLUMN_SEPARATOR)
            .takeIf { it.size == 2 }
            ?.let { columns ->
                columns[0].toInt() to columns[1].unquotes()
            }
            ?: run {
                MTLog.w(this@GTFSStringsUtils, "Invalid string line: '$line'!")
                null
            }

    @JvmStatic
    fun replaceLineStrings(line: String, allStrings: Map<Int, String>?, stringsColumnIdx: IntArray): String {
        return line
            .takeUnless { allStrings.isNullOrEmpty() || stringsColumnIdx.isEmpty() }
            ?.split(SQLUtils.COLUMN_SEPARATOR)
            ?.takeIf { it.isNotEmpty() }
            ?.toMutableList()
            ?.apply {
                for (idx in stringsColumnIdx) {
                    this.getOrNull(idx)?.unquotes()?.let { replaceStrings(it, allStrings) }?.quotes()?.let { this[idx] = it }
                }
            }?.joinToString(SQLUtils.COLUMN_SEPARATOR)
            ?: line
    }

    fun replaceStrings(stringIds: String, allStrings: Map<Int, String>?): String {
        return stringIds
            .takeUnless { allStrings.isNullOrEmpty() }
            ?.split(GTFSCommons.STRINGS_SEPARATOR)
            ?.takeIf { it.isNotEmpty() }
            ?.toMutableList()
            ?.apply {
                indices.forEach { idx ->
                    allStrings?.get(this[idx].toIntOrNull())?.let { this[idx] = it }
                }
            }?.joinToString(GTFSCommons.STRINGS_SEPARATOR)
            ?: stringIds
    }
}
