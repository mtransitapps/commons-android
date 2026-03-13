package org.mtransit.android.commons.provider.gtfs

import android.content.Context
import android.net.Uri
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.provider.status.StatusProviderContract
import kotlin.time.Duration.Companion.hours

fun Context.getRDSSchedule(
    authority: String,
    rdsList: Iterable<RouteDirectionStop>,
) = rdsList.mapNotNull {
    getRDSSchedule(authority, it)
}

fun Context.getRDSSchedule(
    authority: String,
    rds: RouteDirectionStop,
): Schedule? = try {
    contentResolver.query(
        Uri.withAppendedPath(
            UriUtils.newContentUri(authority),
            StatusProviderContract.STATUS_PATH
        ),
        StatusProviderContract.PROJECTION_STATUS,
        Schedule.ScheduleStatusFilter(rds).apply {
            setLookBehindInMs(1.hours.inWholeMilliseconds)
            setMaxDataRequests(3) // yesterday service ending + today + tomorrow?
        }.let { it.toJSONStringStatic(it) },
        null,
        null
    ).use { cursor ->
        cursor?.takeIf { it.count > 0 && it.moveToFirst() }?.let {
            Schedule.fromCursorWithExtra(it)
        }
    }
} catch (e: Exception) {
    MTLog.w(this, e, "Error!")
    null
}
