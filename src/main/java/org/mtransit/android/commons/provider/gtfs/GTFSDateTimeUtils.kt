package org.mtransit.android.commons.provider.gtfs

import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import org.mtransit.android.commons.MTLog
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.TimeZone as KtTimeZone

object GTFSDateTimeUtils : MTLog.Loggable {

    private val LOG_TAG: String = GTFSDateTimeUtils::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    fun parseToDateTime(gtfsDateStr: String?, gtfsTimeStr: String?, agencyTimeZone: KtTimeZone): Instant? {
        try {
            val cleanedDate = gtfsDateStr?.trim() ?: return null
            if (cleanedDate.length != 8) {
                MTLog.w(this, "Invalid GTFS date format '$cleanedDate'! Must be YYYYMMDD")
                return null
            }
            val parts = gtfsTimeStr?.trim()?.split(":") ?: return null
            if (parts.size != 3) {
                MTLog.w(this, "Invalid GTFS time format '$gtfsTimeStr'! Must be HH:MM:SS")
                return null
            }

            val hoursCount = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val minutesCount = parts.getOrNull(1)?.toIntOrNull() ?: return null
            val secondsCount = parts.getOrNull(2)?.toIntOrNull() ?: return null
            val durationFromStartOfDay = hoursCount.hours + // compat with 24+ hours
                    minutesCount.minutes + secondsCount.seconds

            val year = cleanedDate.substring(0, 4).toIntOrNull() ?: return null
            val month = cleanedDate.substring(4, 6).toIntOrNull() ?: return null
            val day = cleanedDate.substring(6, 8).toIntOrNull() ?: return null
            val startOfDay = LocalDate(year, month, day).atStartOfDayIn(agencyTimeZone)

            return startOfDay + durationFromStartOfDay
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while parsing GTFS date '$gtfsDateStr' & time '$gtfsTimeStr'!")
            return null
        }
    }
}
