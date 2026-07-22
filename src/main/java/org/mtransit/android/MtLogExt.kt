@file:Suppress("unused")

package org.mtransit.android

import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.ThreadSafeDateFormatter
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.toMillis
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
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
    } catch (e: Exception) {
        "e:$timeInMs!"
    }

    fun formatDuration(durationInMs: Long?) = try {
        durationInMs?.let { TimeUtils.formatSimpleDuration(it) }
    } catch (e: Exception) {
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
