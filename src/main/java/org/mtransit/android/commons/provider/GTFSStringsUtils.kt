package org.mtransit.android.commons.provider

import org.mtransit.android.commons.data.Schedule

object GTFSStringsUtils {

    private const val SPACE = " "

    @Suppress("DiscouragedApi")
    @JvmStatic
    fun updateStrings(timestamps: Set<Schedule.Timestamp>, gtfsProvider: GTFSProvider): Set<Schedule.Timestamp> {
        val stringIds = timestamps
            .mapNotNull { it.headsignValue?.split(SPACE) }
            .flatten()
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?: return timestamps
        val idToStringMap = loadStrings(gtfsProvider, stringIds)
        timestamps.forEach { timestamp ->
            timestamp.headsignValue?.let { headsignValue ->
                timestamp.headsignValue = headsignValue
                    .split(SPACE)
                    .mapNotNull { idToStringMap[it.toIntOrNull()] }
                    .joinToString(SPACE)
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
            buildMap {
                while (cursor.moveToNext()) {
                    try {
                        put(
                            cursor.getInt(cursor.getColumnIndexOrThrow(GTFSProviderDbHelper.T_STRINGS_K_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(GTFSProviderDbHelper.T_STRINGS_K_STRING)) ?: continue
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
