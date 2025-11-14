package org.mtransit.android

import org.mtransit.android.commons.MTLog
import java.util.Calendar
import java.util.Date

fun Long?.toDateTimeLog() = MTLog.formatDateTime(this)

fun Date?.toDateTimeLog() = this?.time.toDateTimeLog()

fun Calendar?.toDateTimeLog() = this?.time.toDateTimeLog()

@Deprecated("Use toDateTimeLog() instead", ReplaceWith("this.toDateTimeLog()"))
fun Long?.formatDateTime() = this.toDateTimeLog()
