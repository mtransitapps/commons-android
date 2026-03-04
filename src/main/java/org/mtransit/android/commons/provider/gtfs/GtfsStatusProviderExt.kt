package org.mtransit.android.commons.provider.gtfs

import android.content.Context
import android.net.Uri
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.provider.status.StatusProviderContract

fun Context.getRDSSchedule(
    authority: String,
    targetUUID: String,
): Schedule? = getRDSSchedule(authority, listOf(targetUUID))?.singleOrNull()

fun Context.getRDSSchedule(
    authority: String,
    targetUUIDs: List<String>,
): List<Schedule>? = try {
    contentResolver.query(
        Uri.withAppendedPath(
            UriUtils.newContentUri(authority),
            StatusProviderContract.STATUS_PATH
        ),
        StatusProviderContract.PROJECTION_STATUS,
        buildString {
            append(
                append(SqlUtils.getWhereInString(StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID, targetUUIDs))
            )
        },
        null,
        null
    ).use { cursor ->
        buildList {
            if (cursor != null && cursor.count > 0) {
                if (cursor.moveToFirst()) {
                    do {
                        Schedule.fromCursorWithExtra(cursor)?.let {
                            add(it)
                        }
                    } while (cursor.moveToNext())
                }
            }
        }
    }
} catch (e: Exception) {
    MTLog.w(this, e, "Error!")
    null
}
