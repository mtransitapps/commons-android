package org.mtransit.android.commons.provider.gtfs

import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.millisToInstant
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.TimeZone as KtTimeZone

object GTFSDateTimeUtils : MTLog.Loggable {

    private val LOG_TAG: String = GTFSDateTimeUtils::class.java.simpleName

    private const val DATE_FORMAT_PATTERN = "yyyyMMdd"

    private const val TIME_FORMA_HOUR_DIVIDER = ":"

    @Suppress("SpellCheckingInspection")
    private const val TIME_FORMAT_PATTERN = "HH${TIME_FORMA_HOUR_DIVIDER}mm${TIME_FORMA_HOUR_DIVIDER}ss"

    override fun getLogTag() = LOG_TAG

    fun parseToTime(nowMs: Long, gtfsTimeStr: String?, agencyTimeZone: KtTimeZone): Instant? {
        try {
            val parts = gtfsTimeStr?.trim()?.split(TIME_FORMA_HOUR_DIVIDER) ?: return null
            if (parts.size != 3) {
                MTLog.w(this, "Invalid GTFS time format '$gtfsTimeStr'! Must be HH:MM:SS")
                return null
            }
            val hoursCount = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it >= 0 } ?: return null
            val minutesCount = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
            val secondsCount = parts.getOrNull(2)?.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
            val durationFromStartOfDay = hoursCount.hours + // compat with 24+ hours
                minutesCount.minutes + secondsCount.seconds

            val startOfDay = nowMs.millisToInstant()
                .toLocalDateTime(agencyTimeZone).date
                .atStartOfDayIn(agencyTimeZone)

            return startOfDay + durationFromStartOfDay
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while parsing GTFS time '$gtfsTimeStr'!")
            return null
        }
    }

    fun parseToDateTime(gtfsDateStr: String?, gtfsTimeStr: String?, agencyTimeZone: KtTimeZone): Instant? {
        try {
            val cleanedDate = gtfsDateStr?.trim() ?: return null
            if (cleanedDate.length != 8 || cleanedDate.any { !it.isDigit() }) {
                MTLog.w(this, "Invalid GTFS date format '$cleanedDate'! Must be YYYYMMDD")
                return null
            }
            val parts = gtfsTimeStr?.trim()?.split(TIME_FORMA_HOUR_DIVIDER) ?: return null
            if (parts.size != 3) {
                MTLog.w(this, "Invalid GTFS time format '$gtfsTimeStr'! Must be HH:MM:SS")
                return null
            }

            val hoursCount = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it >= 0 } ?: return null
            val minutesCount = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
            val secondsCount = parts.getOrNull(2)?.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
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

    /**
     * Makes sure GTFS time has 2 digits for hours: '9:01:02' -> '09:01:02'
     */
    @Suppress("unused")
    fun String.cleanGtfsTimeStr() = this.padStart(8, '0')

    private val DATE_FORMATTER by lazy { DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN) }
    private val TIME_FORMATTER by lazy { DateTimeFormatter.ofPattern(TIME_FORMAT_PATTERN) }

    @Suppress("unused")
    fun Instant.formatGtfsDateStr(agencyTimeZone: KtTimeZone, startedYesterday: Boolean = false): String {
        if (startedYesterday) { // need to return previous day yyyyMMdd
            return DATE_FORMATTER.format(this.minus(1.days).toLocalDateTime(agencyTimeZone).toJavaLocalDateTime())
        }
        return DATE_FORMATTER.format(toLocalDateTime(agencyTimeZone).toJavaLocalDateTime())
    }

    @Suppress("unused")
    fun Instant.formatGtfsTimeStr(agencyTimeZone: KtTimeZone, startedYesterday: Boolean = false): String {
        if (startedYesterday) { // needs to return GTFS time > 24:00:00
            return TIME_FORMATTER.format(this.minus(1.days).toLocalDateTime(agencyTimeZone).toJavaLocalDateTime()).append24Hours()
        }
        return TIME_FORMATTER.format(toLocalDateTime(agencyTimeZone).toJavaLocalDateTime())
    }

    @Suppress("unused")
    fun String.isGtfsTimeAfterMidnight(): Boolean? {
        val parts = this.trim().split(TIME_FORMA_HOUR_DIVIDER)
        if (parts.size != 3) return null // wrong format!
        val hoursCount = parts.getOrNull(0)?.toIntOrNull() ?: return null // wrong format!
        return hoursCount >= 24
    }

    fun String.append24Hours(): String {
        val parts = this.trim().split(TIME_FORMA_HOUR_DIVIDER)
        if (parts.size != 3) return this // wrong format!
        val hoursCount = parts.getOrNull(0)?.toIntOrNull() ?: return this // wrong format!
        if (hoursCount > 24) return this // already 24+ hours
        return (hoursCount + 24).toString().padStart(2, '0') + TIME_FORMA_HOUR_DIVIDER + parts.subList(1, 3).joinToString(TIME_FORMA_HOUR_DIVIDER)
    }
}
