package org.mtransit.android.commons.provider.gtfs

import android.content.Context
import android.net.Uri
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.provider.status.StatusProviderContract
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private const val DEFAULT_MAX_DATA_REQUEST = 3 // yesterday service ending + today + tomorrow?
private val DEFAULT_LOOK_BEHIND = 1.hours

fun Context.getRDSSchedule(
    authority: String,
    rdsList: Iterable<RouteDirectionStop>,
    includeCancelledTimestamps: Boolean = false,
    lookBehind: Duration = DEFAULT_LOOK_BEHIND,
    maxDataRequest: Int = DEFAULT_MAX_DATA_REQUEST,
) = rdsList.mapNotNull {
    getRDSSchedule(
        authority = authority,
        rds = it,
        includeCancelledTimestamps = includeCancelledTimestamps,
        lookBehind = lookBehind,
        maxDataRequest = maxDataRequest,
    )
}

fun Context.getRDSSchedule(
    authority: String,
    rds: RouteDirectionStop,
    includeCancelledTimestamps: Boolean = false,
    lookBehind: Duration = DEFAULT_LOOK_BEHIND,
    maxDataRequest: Int = DEFAULT_MAX_DATA_REQUEST,
): Schedule? = try {
    contentResolver.query(
        Uri.withAppendedPath(
            UriUtils.newContentUri(authority),
            StatusProviderContract.STATUS_PATH
        ),
        StatusProviderContract.PROJECTION_STATUS,
        Schedule.ScheduleStatusFilter(rds).apply {
            setLookBehindInMs(lookBehind.inWholeMilliseconds)
            setMaxDataRequests(maxDataRequest)
            setIncludeCancelledTimestamps(includeCancelledTimestamps)
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
