package org.mtransit.android.commons.provider.gtfs

import android.database.Cursor
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.provider.GTFSProvider
import org.mtransit.commons.FeatureFlags

object GTFSTripIdsUtils : MTLog.Loggable {

    private val LOG_TAG: String = GTFSTripIdsUtils::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    @Suppress("DiscouragedApi")
    @JvmStatic
    fun <T : Collection<Schedule.Timestamp>> updateTripIds(timestamps: T, gtfsProvider: GTFSProvider): T {
        if (!FeatureFlags.F_EXPORT_TRIP_ID_INTS) return timestamps
        val tripIdInts = timestamps
            .mapNotNull { it.tripId }
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?: return timestamps
        val idIntToIdMap = loadTripIds(gtfsProvider, tripIdInts)
        timestamps.forEach { timestamp ->
            timestamp.tripId?.let { tripIdInt ->
                timestamp.tripId = tripIdInt.toIntOrNull()?.let { idIntToIdMap[it] } ?: tripIdInt
            }
        }
        return timestamps
    }

    private fun loadTripIds(gtfsProvider: GTFSProvider, tripIdInts: List<String>): Map<Int, String> {
        if (tripIdInts.isEmpty()) return emptyMap()
        val placeholders = tripIdInts.joinToString(",") { "?" }
        return gtfsProvider.readDB.query(
            GTFSProviderDbHelper.T_TRIP_IDS,
            arrayOf(GTFSProviderDbHelper.T_TRIP_IDS_K_ID_INT, GTFSProviderDbHelper.T_TRIP_IDS_K_ID),
            "${GTFSProviderDbHelper.T_TRIP_IDS_K_ID_INT} IN ($placeholders)",
            tripIdInts.toTypedArray(),
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
                    cursor.getInt(cursor.getColumnIndexOrThrow(GTFSProviderDbHelper.T_TRIP_IDS_K_ID_INT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(GTFSProviderDbHelper.T_TRIP_IDS_K_ID)) ?: continue
                )
            } catch (e: Exception) {
                MTLog.w(this@GTFSTripIdsUtils, e, "Cannot parse trip ID cursor: '$cursor'!")
            }
        }
    }
}
