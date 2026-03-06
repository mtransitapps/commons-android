package org.mtransit.android.commons.provider.status

import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.data.toScheduleTimestamp
import org.mtransit.android.commons.secsToInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class GTFSRealTimeTripUpdatesProviderTests {

    companion object {
        private const val LOCAL_TZ_ID: String = "America/Montreal"

        private const val DEPARTURE_MS = 1772722800L // 2026-03-06 10:00:
    }

    @Test
    fun text_applyDelay_null() {
        val departure = DEPARTURE_MS.secsToInstant() // 2026-03-06 10:00:00 ET
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID)
        val delay: Duration? = null

        val result = wipApplyDelay(timestamp, delay)

        assertNull(result)
        assertFalse { timestamp.isRealTime }
        assertEquals(departure, timestamp.departure)
    }

    @Test
    fun text_applyDelay_0_on_time() {
        val departure = DEPARTURE_MS.secsToInstant() // 2026-03-06 10:00:00 ET
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID)
        val delay = Duration.ZERO

        val result = wipApplyDelay(timestamp, delay)

        assertNotNull(result)
        assertEquals(delay, result) // delay not consumed
        assertTrue { timestamp.isRealTime }
        assertEquals(departure, timestamp.departure)
    }

    @Test
    fun text_applyDelay_simple_late() {
        val departure = DEPARTURE_MS.secsToInstant() // 2026-03-06 10:00:00 ET
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID)
        val delay = 10.minutes

        val result = wipApplyDelay(timestamp, delay)

        assertNotNull(result)
        assertEquals(delay, result) // delay not consumed
        assertTrue { timestamp.isRealTime }
        assertEquals(departure + delay, timestamp.departure)
    }

    @Test
    fun text_applyDelay_differentArrival_late() {
        val departure = DEPARTURE_MS.secsToInstant() // 2026-03-06 10:00:00 ET
        val arrival = departure - 1.minutes
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival)
        val delay = 10.minutes

        val result = wipApplyDelay(timestamp, delay)

        assertNotNull(result)
        assertEquals(9.minutes, result) // delay partially consumed
        assertTrue { timestamp.isRealTime }
        assertEquals(arrival + delay, timestamp.arrival)
        assertEquals(departure + result, timestamp.departure)
    }

    @Test
    fun text_applyDelay_consumed_late() {
        val departure = DEPARTURE_MS.secsToInstant() // 2026-03-06 10:00:00 ET
        val arrival = departure - 15.minutes
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival)
        val delay = 10.minutes

        val result = wipApplyDelay(timestamp, delay)

        assertNotNull(result)
        assertEquals(Duration.ZERO, result) // delay consumed
        assertTrue { timestamp.isRealTime }
        assertEquals(arrival + delay, timestamp.arrival)
        assertEquals(departure, timestamp.departure)
    }

    @Test
    fun text_applyDelay_simple_early() {
        val departure = DEPARTURE_MS.secsToInstant() // 2026-03-06 10:00:00 ET
        val arrival = departure - 1.minutes
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival)
        val delay = (-5).minutes

        val result = wipApplyDelay(timestamp, delay)

        assertNotNull(result)
        assertEquals(delay, result) // delay not consumed
        assertTrue { timestamp.isRealTime }
        assertEquals(arrival - 5.minutes, timestamp.arrival)
        assertEquals(departure - 5.minutes, timestamp.departure)
    }
}
