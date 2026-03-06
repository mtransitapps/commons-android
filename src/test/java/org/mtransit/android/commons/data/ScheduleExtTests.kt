package org.mtransit.android.commons.data

import org.mtransit.android.commons.secsToInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class ScheduleExtTests {

    companion object {
        private const val LOCAL_TZ_ID: String = "America/Montreal"
        private const val DEPARTURE_MS = 1772722800L // 2026-03-06 10:00:
    }

    @Test
    fun test1() {
        val departure = DEPARTURE_MS.secsToInstant()
        val arrival = departure - 1.minutes
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival)

        timestamp.departure += 1.minutes
        assertEquals(arrival, timestamp.arrival)
        assertEquals(departure + 1.minutes, timestamp.departure)
    }
}
