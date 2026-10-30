package org.mtransit.android.commons.provider.gtfs

import org.mtransit.android.commons.millisToInstant
import org.mtransit.android.commons.provider.gtfs.GTFSDateTimeUtils.append24Hours
import org.mtransit.commons.CommonsApp
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.TimeZone as KtTimeZone

class GTFSDateTimeUtilsTest {

    companion object {
        private const val LOCAL_TZ_ID = "America/Montreal"
        private val LOCAL_TZ = KtTimeZone.of(LOCAL_TZ_ID)
    }

    @BeforeTest
    fun setUp() {
        CommonsApp.setup(isAndroidPlatform = false)
    }

    @Test
    fun test_parseToTime() {
        val nowInMs = 1793114553_000L // Tuesday, October 27, 2026 at 11:22:33 UTC-04:00 DST
        "12:24:00".let { // soon
            GTFSDateTimeUtils.parseToTime(nowInMs, it, LOCAL_TZ)
        }.let {
            assertEquals(1793118240_000L.millisToInstant(), it) // Tuesday, October 27, 2026 at 12:24:00 UTC-04:00 DST
        }
        "24:24:00".let { // tomorrow morning (night service)
            GTFSDateTimeUtils.parseToTime(nowInMs, it, LOCAL_TZ)
        }.let {
            assertEquals(1793161440_000L.millisToInstant(), it) // Wednesday, October 28, 2026 at 00:24:00 UTC-04:00 DST
        }
        "10:24:00".let { // in the past
            GTFSDateTimeUtils.parseToTime(nowInMs, it, LOCAL_TZ)
        }.let {
            assertEquals(1793111040_000L.millisToInstant(), it) // Tuesday, October 27, 2026 at 10:24:00 UTC-04:00 DST
        }
    }

    @Test
    fun test_parseToTime_closeToMidnight_DayChange() {
        val nowInMs = 1793160620_000L // Wednesday, October 28, 2026 at 00:10:20 UTC-04:00 DST
        "23:59:00".let { // started 30 minutes ago yesterday
            GTFSDateTimeUtils.parseToTime(nowInMs, it, LOCAL_TZ)
        }.let {
            assertEquals(1793159940_000L.millisToInstant(), it) // Tuesday, October 27, 2026 at 23:59:00 UTC-04:00 DST
        }
        "00:20:30".let { // soon
            GTFSDateTimeUtils.parseToTime(nowInMs, it, LOCAL_TZ)
        }.let {
            assertEquals(1793161230_000L.millisToInstant(), it) // Wednesday, October 28, 2026 at 00:20:30 UTC-04:00 DST
        }
    }

    @Test
    fun test_parseToTime_latePM() {
        val nowInMs = 1793056759_000L // Monday, October 26, 2026, at 7:19:19 p.m. UTC-04:00 DST
        "19:37:00".let {
            GTFSDateTimeUtils.parseToTime(nowInMs, it, LOCAL_TZ)
        }.let {
            assertEquals(1793057820_000L.millisToInstant(), it) // Monday, October 26, 2026 at 19:37:00 UTC-04:00 DST
        }
        "24:24:24".let {
            GTFSDateTimeUtils.parseToTime(nowInMs, it, LOCAL_TZ)
        }.let {
            assertEquals(1793075064_000L.millisToInstant(), it) // Tuesday, October 27, 2026 at 00:24:24 UTC-04:00 DST
        }
    }

    @Test
    fun test_parseToDateTime() {
        ("20260730" to "12:24:00").let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, LOCAL_TZ)
        }.let { result ->
            assertEquals(1785428640_000L.millisToInstant(), result)
        }
        ("20260730" to "24:24:00").let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, LOCAL_TZ)
        }.let { result ->
            assertEquals(1785471840_000L.millisToInstant(), result)
        }
        ("20260730" to "00:24:00").let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, LOCAL_TZ)
        }.let { result ->
            assertEquals(1785385440_000L.millisToInstant(), result)
        }
        (null to null).let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, LOCAL_TZ)
        }.let { result ->
            assertNull(result)
        }
        ("2026-07-30" to "-1:74:77").let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, LOCAL_TZ)
        }.let { result ->
            assertNull(result)
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
