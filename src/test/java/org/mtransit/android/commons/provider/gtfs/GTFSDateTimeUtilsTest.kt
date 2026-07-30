package org.mtransit.android.commons.provider.gtfs

import org.mtransit.android.commons.millisToInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.TimeZone as KtTimeZone

class GTFSDateTimeUtilsTest {

    private val timeZone = KtTimeZone.of("America/Edmonton")

    @Test
    fun test_parseToDateTime() {
        ("20260730" to "12:24:00").let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, timeZone)
        }.let { result ->
            assertEquals(1785435840_000L.millisToInstant(), result)
        }
        ("20260730" to "24:24:00").let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, timeZone)
        }.let { result ->
            println(result?.toEpochMilliseconds())
            assertEquals(1785479040_000L.millisToInstant(), result)
        }
        (null to null).let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, timeZone)
        }.let { result ->
            assertNull(result)
        }
        ("2026-07-30" to "-1:74:77").let { (date, time) ->
            GTFSDateTimeUtils.parseToDateTime(date, time, timeZone)
        }.let { result ->
            assertNull(result)
        }
    }

}
