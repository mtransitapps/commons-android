@file:Suppress("unused")

package org.mtransit.android

import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.toMillis
import java.util.Calendar
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Instant

// region duration

fun Long?.toDurationLog() = MTLog.formatDuration(this)
fun Duration?.toDurationLog() = this?.inWholeMilliseconds.toDurationLog()

// endregion

// region date & time

@Deprecated("Use toDateTimeLog() instead", ReplaceWith("this.toDateTimeLog()"))
fun Long?.formatDateTime() = this.toDateTimeLog()
fun Long?.toDateTimeLog() = MTLog.formatDateTime(this)
fun Date?.toDateTimeLog() = this?.time.toDateTimeLog()
fun Calendar?.toDateTimeLog() = this?.time.toDateTimeLog()
fun Instant?.toDateTimeLog() = this?.toMillis().toDateTimeLog()

// endregion
