@file:Suppress("unused")

package org.mtransit.android.commons

import kotlinx.coroutines.Job
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Instant

// region duration

fun Long?.toDurationLog(): String? = if (Constants.DEBUG) MtLogExt.formatDuration(this) else this?.toString() // formatting is expensive, only in debug
fun Duration?.toDurationLog(): String? = this?.inWholeMilliseconds.toDurationLog()

// endregion

object MtLogExt {
    private val dateTimeFormatter: ThreadSafeDateFormatter by lazy {
        ThreadSafeDateFormatter(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM))
    }

    fun formatDateTime(timeInMs: Long?): String? = try {
        timeInMs?.let {
            dateTimeFormatter.formatThreadSafe(it)
        }
    } catch (_: Exception) {
        "e:$timeInMs!"
    }

    fun formatDuration(durationInMs: Long?) = try {
        durationInMs?.let { TimeUtils.formatSimpleDuration(it) }
    } catch (_: Exception) {
        "e:$durationInMs!"
    }
}

// region date & time

@Deprecated("Use toDateTimeLog() instead", ReplaceWith("this.toDateTimeLog()"))
fun Long?.formatDateTime(): String? = this.toDateTimeLog()
fun Long?.toDateTimeLog(): String? = if (Constants.DEBUG) MtLogExt.formatDateTime(this) else this?.toString() // formatting is expensive, only in debug
fun Date?.toDateTimeLog(): String? = this?.time.toDateTimeLog()
fun Calendar?.toDateTimeLog(): String? = this?.time.toDateTimeLog()
fun Instant?.toDateTimeLog(): String? = this?.toMillis().toDateTimeLog()

// endregion

// region Coroutines

fun Job.logCancellation(loggable: MTLog.Loggable, jobTag: String?) = logCancellation(loggable.logTag, jobTag)

fun Job.logCancellation(logTag: String, jobTag: String?) {
    if (!MTLog.isLoggable(android.util.Log.DEBUG)) return
    invokeOnCompletion {
        if (it is CancellationException) {
            MTLog.d(logTag, "loadPOIMarkers($jobTag) -- CANCELLED")
        }
    }
}

// end region
