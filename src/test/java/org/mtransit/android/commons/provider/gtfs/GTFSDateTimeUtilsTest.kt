package org.mtransit.android.commons.provider.gtfs

import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.provider.gtfs.GTFSDateTimeUtils.append24Hours
import org.mtransit.android.commons.provider.gtfs.GTFSDateTimeUtils.formatGtfsDateStr
import org.mtransit.android.commons.provider.gtfs.GTFSDateTimeUtils.formatGtfsTimeStr
import org.mtransit.commons.CommonsApp
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.datetime.TimeZone as KtTimeZone

class GTFSDateTimeUtilsTest {

    companion object {
        private const val AGENCY_TZ_ID = "America/Montreal"
        private val AGENCY_TZ = KtTimeZone.of(AGENCY_TZ_ID)
        private val AGENCY_TZ_CAL = GTFSDateTimeUtils.makeCal(AGENCY_TZ_ID)
    }

    @BeforeTest
    fun setUp() {
        CommonsApp.setup(isAndroidPlatform = false)
    }

    @Test
    fun test_parseToTime() {
        val nowInMs = 1793114553_000L // Tuesday, October 27, 2026 at 11:22:33 UTC-04:00 DST
        "12:24:00".let { // soon
            GTFSDateTimeUtils.parseToTime(nowInMs, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793118240_000L.millisToInstant(), it) // Tuesday, October 27, 2026 at 12:24:00 UTC-04:00 DST
        }
        "24:24:00".let { // tomorrow morning (night service)
            GTFSDateTimeUtils.parseToTime(nowInMs, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793161440_000L.millisToInstant(), it) // Wednesday, October 28, 2026 at 00:24:00 UTC-04:00 DST
        }
        "10:24:00".let { // in the past
            GTFSDateTimeUtils.parseToTime(nowInMs, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793111040_000L.millisToInstant(), it) // Tuesday, October 27, 2026 at 10:24:00 UTC-04:00 DST
        }
    }

    @Test
    fun test_parseToTime_closeToMidnight_DayChange() {
        val nowInMs = 1793160620_000L // Wednesday, October 28, 2026 at 00:10:20 UTC-04:00 DST
        "23:59:00".let { // started 30 minutes ago yesterday
            GTFSDateTimeUtils.parseToTime(nowInMs, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793159940_000L.millisToInstant(), it) // Tuesday, October 27, 2026 at 23:59:00 UTC-04:00 DST
        }
        "24:24:00".let { // in the past
            GTFSDateTimeUtils.parseToTime(nowInMs, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793161440_000L.millisToInstant(), it) // Wednesday, October 28, 2026 at 00:24:00 UTC-04:00 DST
        }
        "00:05:55".let { // in the recent past
            GTFSDateTimeUtils.parseToTime(nowInMs, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793160355_000L.millisToInstant(), it) // Wednesday, October 28, 2026 at 00:05:55 UTC-04:00 DST
        }
        "00:20:30".let { // soon
            GTFSDateTimeUtils.parseToTime(nowInMs, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793161230_000L.millisToInstant(), it) // Wednesday, October 28, 2026 at 00:20:30 UTC-04:00 DST
        }
    }

    @Test
    fun test_parseToTime_closeToMidnight_DayChange2() {
        val nowInMsGeminiPRExample = 1793161800_000L // Wednesday, October 28, 2026 at 00:30:00 UTC-04:00 DST
        "00:24:00".let { // in the recent past
            GTFSDateTimeUtils.parseToTime(nowInMsGeminiPRExample, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793161440_000L.millisToInstant(), it) // Wednesday, October 28, 2026 at 00:24:00 UTC-04:00 DST
        }
        "24:24:00".let { // in the recent past
            GTFSDateTimeUtils.parseToTime(nowInMsGeminiPRExample, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793161440_000L.millisToInstant(), it) // Wednesday, October 28, 2026 at 00:24:00 UTC-04:00 DST
        }
        val nowInMsCodeRabbitExemple = 1793159400_000L // Tuesday, October 27, 2026 at 23:50:00 UTC-04:00 DST
        "00:15:00".let { // in the recent past
            GTFSDateTimeUtils.parseToTime(nowInMsCodeRabbitExemple, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793160900_000L.millisToInstant(), it) // Wednesday, October 28, 2026 at 00:15:00 UTC-04:00 DST
        }
        val nowInMsTimeChange = 1793595000_000L // Sunday, November 1, 2026 at 23:50:00 UTC-05:00
        "00:15:00".let { // in the recent past
            GTFSDateTimeUtils.parseToTime(nowInMsTimeChange, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793596500_000L.millisToInstant(), it) // Monday, November 2, 2026 at 00:15:00 UTC-05:00
        }
    }

    @Test
    fun test_parseToTime_earlyMorning_nextDay() {
        val nowInMs = 1793160620_000L // Wednesday, October 28, 2026 at 00:10:20 UTC-04:00 DST
        "01:00:00".let {
            GTFSDateTimeUtils.parseToTime(nowInMs, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793163600_000L.millisToInstant(), it) // Wednesday, October 28, 2026 at 01:00:00 UTC-04:00 DST
        }
    }

    @Test
    fun test_parseToTime_latePM() {
        val nowInMs = 1793056759_000L // Monday, October 26, 2026, at 7:19:19 p.m. UTC-04:00 DST
        "19:37:00".let {
            GTFSDateTimeUtils.parseToTime(nowInMs, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793057820_000L.millisToInstant(), it) // Monday, October 26, 2026 at 19:37:00 UTC-04:00 DST
        }
        "24:24:24".let {
            GTFSDateTimeUtils.parseToTime(nowInMs, it, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let {
            assertEquals(1793075064_000L.millisToInstant(), it) // Tuesday, October 27, 2026 at 00:24:24 UTC-04:00 DST
        }
    }

    @Test
    fun test_parseToTime_withoutDate_multiDayService() {
        listOf(
            Triple("2026-10-26T04:10:00Z", "48:00:00", "2026-10-26T04:00:00Z"),
            Triple("2026-10-26T04:10:00Z", "48:24:00", "2026-10-26T04:24:00Z"),
            Triple("2026-10-26T10:00:00Z", "48:00:00", "2026-10-27T04:00:00Z"),
            Triple("2026-10-26T04:10:00Z", "72:00:00", "2026-10-26T04:00:00Z"),
            Triple("2026-11-02T05:10:00Z", "48:00:00", "2026-11-02T05:00:00Z"),
            Triple("2026-03-09T04:10:00Z", "48:00:00", "2026-03-09T04:00:00Z"),
        ).forEach { (now, time, expected) ->
            assertEquals(
                Instant.parse(expected),
                GTFSDateTimeUtils.parseToTime(Instant.parse(now).toEpochMilliseconds(), time, AGENCY_TZ, AGENCY_TZ_CAL),
                "$now / $time",
            )
        }
    }

    @Test
    fun test_parseToDateTime() {
        ("20260730" to "12:24:00").let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let { result ->
            assertEquals(1785428640_000L.millisToInstant(), result)
        }
        ("20260730" to "24:24:00").let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let { result ->
            assertEquals(1785471840_000L.millisToInstant(), result)
        }
        ("20260730" to "00:24:00").let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let { result ->
            assertEquals(1785385440_000L.millisToInstant(), result)
        }
        (null to null).let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let { result ->
            assertNull(result)
        }
        ("2026-07-30" to "-1:74:77").let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, AGENCY_TZ, AGENCY_TZ_CAL)
        }.let { result ->
            assertNull(result)
        }
    }

    @Test
    fun test_parseToTime_daylightSavingTransitions() {
        listOf(
            Triple("2026-11-02T00:19:19Z", "19:37:00", "2026-11-02T00:37:00Z"),
            Triple("2026-11-02T00:19:19Z", "24:24:24", "2026-11-02T05:24:24Z"),
            Triple("2026-11-02T05:10:20Z", "23:59:00", "2026-11-02T04:59:00Z"),
            Triple("2026-11-02T05:10:20Z", "24:24:24", "2026-11-02T05:24:24Z"),
            Triple("2026-11-01T03:50:00Z", "03:15:00", "2026-11-01T08:15:00Z"),
            Triple("2026-11-01T06:10:00Z", "01:30:00", "2026-11-01T06:30:00Z"),
            Triple("2026-03-08T23:19:19Z", "19:37:00", "2026-03-08T23:37:00Z"),
            Triple("2026-03-08T23:19:19Z", "24:24:24", "2026-03-09T04:24:24Z"),
            Triple("2026-03-09T04:10:20Z", "23:59:00", "2026-03-09T03:59:00Z"),
            Triple("2026-03-09T04:10:20Z", "24:24:24", "2026-03-09T04:24:24Z"),
            Triple("2026-03-08T04:50:00Z", "03:15:00", "2026-03-08T07:15:00Z"),
            Triple("2026-03-08T07:10:00Z", "02:30:00", "2026-03-08T07:30:00Z"),
        ).forEach { (now, time, expected) ->
            assertEquals(
                Instant.parse(expected),
                GTFSDateTimeUtils.parseToTime(Instant.parse(now).toEpochMilliseconds(), time, AGENCY_TZ, AGENCY_TZ_CAL),
                "$now / $time",
            )
        }
    }

    @Test
    fun test_parseToDateTime_matchesStaticScheduleOnDaylightSavingTransitions() {
        val staticScheduleFormat = SimpleDateFormat("yyyyMMddHH:mm:ss", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone(AGENCY_TZ_ID)
        }
        listOf(
            "20261101" to "19:37:00",
            "20261101" to "24:24:24",
            "20261031" to "25:30:00",
            "20261101" to "01:30:00",
            "20261031" to "49:24:24",
            "20260308" to "19:37:00",
            "20260308" to "24:24:24",
            "20260307" to "26:30:00",
            "20260308" to "02:30:00",
            "20260307" to "49:24:24",
        ).forEach { (date, time) ->
            val expected = assertNotNull(staticScheduleFormat.parse(date + time)).time.millisToInstant()
            assertEquals(expected, GTFSDateTimeUtils.parseToDateTime(date, time, AGENCY_TZ, AGENCY_TZ_CAL), "$date / $time")
        }
    }

    @Test
    fun test_formatGtfsDateTime_roundTripAcrossDaylightSavingTransitions() {
        listOf(
            Triple("2026-03-09T04:24:24Z", "20260308", "20260309"),
            Triple("2026-11-02T05:24:24Z", "20261101", "20261102"),
            Triple("2026-10-28T04:24:24Z", "20261027", "20261028"),
        ).forEach { (timestamp, previousDate, localDate) ->
            val instant = Instant.parse(timestamp)
            listOf(false, true).forEach { startedYesterday ->
                val date = instant.formatGtfsDateStr(AGENCY_TZ, startedYesterday)
                val time = instant.formatGtfsTimeStr(AGENCY_TZ, startedYesterday)
                assertEquals(if (startedYesterday) previousDate else localDate, date, timestamp)
                assertEquals(if (startedYesterday) "24:24:24" else "00:24:24", time, timestamp)
                assertEquals(instant, GTFSDateTimeUtils.parseToDateTime(date, time, AGENCY_TZ, AGENCY_TZ_CAL), timestamp)
            }
        }
    }

    @Test
    fun test_append24Hours() {
        "01:02:03".append24Hours().let { result ->
            assertEquals("25:02:03", result)
        }
        "1:02:03".append24Hours().let { result ->
            assertEquals("25:02:03", result)
        }
        "25:02:03".append24Hours().let { result ->
            assertEquals("25:02:03", result)
        }
    }
}
