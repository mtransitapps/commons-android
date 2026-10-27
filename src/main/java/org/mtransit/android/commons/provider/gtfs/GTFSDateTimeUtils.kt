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

    private const val TIME_FORMAT_HOUR_DIVIDER = ":"
    private const val TIME_FORMAT_GROUPS = 3

    @Suppress("SpellCheckingInspection")
    private const val TIME_FORMAT_PATTERN = "HH${TIME_FORMAT_HOUR_DIVIDER}mm${TIME_FORMAT_HOUR_DIVIDER}ss"

    private const val TIME_FORMAT_LENGTH = 8

    private const val TIME_FORMAT_MAX_HOUR_TODAY = 23 // 24 means tomorrow (from start of service - night service)

    private const val TWENTY_FOUR_HOURS = 24

    override fun getLogTag() = LOG_TAG

    fun parseToTime(nowMs: Long, gtfsTimeStr: String?, agencyTimeZone: KtTimeZone): Instant? {
        try {
            val parts = gtfsTimeStr?.trim()?.split(TIME_FORMAT_HOUR_DIVIDER)?.takeIf { it.size == TIME_FORMAT_GROUPS }
            val hoursCount = parts?.getOrNull(0)?.toIntOrNull()?.takeIf { it >= 0 }
            val minutesCount = parts?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 }
            val secondsCount = parts?.getOrNull(2)?.toIntOrNull()?.takeIf { it in 0..59 }
            if (hoursCount == null || minutesCount == null || secondsCount == null) return null
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
            if (cleanedDate.length != TIME_FORMAT_LENGTH || cleanedDate.any { !it.isDigit() }) {
                MTLog.w(this, "Invalid GTFS date format '$cleanedDate'! Must be YYYYMMDD")
                return null
            }
            val parts = gtfsTimeStr?.trim()?.split(TIME_FORMAT_HOUR_DIVIDER)?.takeIf { it.size == TIME_FORMAT_GROUPS } ?: return null

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
    fun String.cleanGtfsTimeStr() = this.padStart(TIME_FORMAT_LENGTH, '0')

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
        val parts = this.trim().split(TIME_FORMAT_HOUR_DIVIDER)
        val hoursCount = parts.takeIf { it.size == TIME_FORMAT_GROUPS }?.getOrNull(0)?.toIntOrNull() ?: return null // wrong format!
        return hoursCount > TIME_FORMAT_MAX_HOUR_TODAY
    }

    fun String.append24Hours(): String {
        val parts = this.trim().split(TIME_FORMAT_HOUR_DIVIDER)
        val hoursCount = parts.takeIf { it.size == TIME_FORMAT_GROUPS }?.getOrNull(0)?.toIntOrNull() ?: return this // wrong format!
        if (hoursCount > TIME_FORMAT_MAX_HOUR_TODAY) return this // already 24+ hours
        return (hoursCount + TWENTY_FOUR_HOURS).toString().padStart(2, '0') + TIME_FORMAT_HOUR_DIVIDER +
            parts.subList(1, TIME_FORMAT_GROUPS).joinToString(TIME_FORMAT_HOUR_DIVIDER)
    }
}
