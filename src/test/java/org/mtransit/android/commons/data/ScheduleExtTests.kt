package org.mtransit.android.commons.data

import org.mtransit.android.commons.secsToInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ScheduleExtTests {

    companion object {
        private const val LOCAL_TZ_ID: String = "America/Montreal"
        private const val DEPARTURE_MS = 1772722800L // 2026-03-06 10:00:
    }

    @Test
    fun test_departure_update() {
        val departure = DEPARTURE_MS.secsToInstant()
        val arrival = departure - 10.minutes
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival)

        timestamp.updateForRealTime(newArrival = arrival + 7.minutes, newDeparture = departure + 1.minutes)

        assertTrue { timestamp.isRealTime }
        assertTrue { timestamp.isArrivalLate(minDelay = 1.minutes) }
        assertEquals(arrival + 7.minutes, timestamp.arrival)
        assertEquals(7.minutes, timestamp.originalArrivalDelay)
        assertEquals(arrival, timestamp.originalArrival)
        assertTrue { timestamp.isArrivalLate(minDelay = 1.minutes) }
        assertEquals(departure + 1.minutes, timestamp.departure)
        assertEquals(1.minutes, timestamp.originalDepartureDelay)
        assertEquals(departure, timestamp.originalDeparture)
        assertEquals(4.minutes, timestamp.arrivalDiff)
    }

    @Test
    fun test_departure_update_early() {
        val departure = DEPARTURE_MS.secsToInstant()
        val arrival = departure - 10.minutes
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival)

        timestamp.updateForRealTime(arrivalDelay = (-3).minutes, departureDelay = (-5).minutes, currentPrecision = 1.minutes, delayPrecision = 10.seconds)

        assertTrue { timestamp.isRealTime }
        assertTrue { timestamp.isArrivalEarly() }
        assertFalse { timestamp.isArrivalEarly(maxDelay = 5.minutes) }
        assertEquals(arrival - 3.minutes, timestamp.arrival)
        assertEquals((-3).minutes, timestamp.originalArrivalDelay)
        assertEquals(arrival, timestamp.originalArrival)
        assertTrue { timestamp.isDepartureEarly() }
        assertFalse { timestamp.isDepartureEarly(maxDelay = 10.minutes) }
        assertEquals(departure - 5.minutes, timestamp.departure)
        assertEquals((-5).minutes, timestamp.originalDepartureDelay)
        assertEquals(departure, timestamp.originalDeparture)
        assertEquals(8.minutes, timestamp.arrivalDiff)
    }

    @Test
    fun test_departure_update_no_effect_on_arrival() {
        val departure = DEPARTURE_MS.secsToInstant()
        val arrival = departure - 1.minutes
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival)

        timestamp.updateForRealTime(arrivalDelay = null, departureDelay = 1.minutes, currentPrecision = 1.minutes, delayPrecision = 10.seconds)

        assertTrue { timestamp.isRealTime }
        assertFalse { timestamp.isArrivalLate() || timestamp.isArrivalEarly() }
        assertEquals(arrival, timestamp.arrival)
        assertTrue { timestamp.isDepartureLate(minDelay = 30.seconds) }
        assertEquals(departure + 1.minutes, timestamp.departure)
        assertEquals(1.minutes, timestamp.originalDepartureDelay)
        assertEquals(departure, timestamp.originalDeparture)
        assertEquals(2.minutes, timestamp.arrivalDiff)
    }

    @Test
    fun test_updateForRealTime_w_arrival() {
        val departure = DEPARTURE_MS.secsToInstant()
        departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival = departure).apply {
            updateForRealTime(delay = (-61).seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertEquals(departure - 2.minutes, result.arrival)
            assertEquals(departure - 2.minutes, result.departure)
            assertTrue { result.arrival <= result.departure }
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival = departure).apply {
            updateForRealTime(delay = (-59).seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertTrue { result.arrival <= result.departure }
            assertEquals(departure - 1.minutes, result.arrival)
            assertEquals(departure - 1.minutes, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival = departure).apply {
            updateForRealTime(delay = (-30).seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertTrue { result.arrival <= result.departure }
            assertEquals(departure - 1.minutes, result.arrival)
            assertEquals(departure - 1.minutes, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival = departure).apply {
            updateForRealTime(delay = (-15).seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertTrue { result.arrival <= result.departure }
            assertEquals(departure - 1.minutes, result.arrival)
            assertEquals(departure - 1.minutes, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival = departure).apply {
            updateForRealTime(delay = 15.seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertTrue { result.arrival <= result.departure }
            assertEquals(departure, result.arrival)
            assertEquals(departure, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival = departure).apply {
            updateForRealTime(delay = 30.seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertTrue { result.arrival <= result.departure }
            assertEquals(departure, result.arrival)
            assertEquals(departure, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival = departure).apply {
            updateForRealTime(delay = 59.seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertTrue { result.arrival <= result.departure }
            assertEquals(departure, result.arrival)
            assertEquals(departure, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival = departure).apply {
            updateForRealTime(delay = 61.seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertTrue { result.arrival <= result.departure }
            assertEquals(departure + 1.minutes, result.arrival)
            assertEquals(departure + 1.minutes, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival = departure).apply {
            updateForRealTime(delay = 1.minutes + 30.seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertTrue { result.arrival <= result.departure }
            assertEquals(departure + 1.minutes, result.arrival)
            assertEquals(departure + 1.minutes, result.departure)
        }
    }

    @Test
    fun test_updateDepartureForRealTime() {
        val departure = DEPARTURE_MS.secsToInstant()
        departure.toScheduleTimestamp(LOCAL_TZ_ID).apply {
            updateDepartureForRealTime(departureDelay = (-61).seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertEquals(departure - 2.minutes, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID).apply {
            updateDepartureForRealTime(departureDelay = (-59).seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertEquals(departure - 1.minutes, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID).apply {
            updateDepartureForRealTime(departureDelay = (-30).seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertEquals(departure - 1.minutes, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID).apply {
            updateDepartureForRealTime(departureDelay = (-15).seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertEquals(departure - 1.minutes, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID).apply {
            updateDepartureForRealTime(departureDelay = 15.seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertEquals(departure, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID).apply {
            updateDepartureForRealTime(departureDelay = 30.seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertEquals(departure, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID).apply {
            updateDepartureForRealTime(departureDelay = 59.seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertEquals(departure, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID).apply {
            updateDepartureForRealTime(departureDelay = 61.seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertEquals(departure + 1.minutes, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID).apply {
            updateDepartureForRealTime(departureDelay = 1.minutes + 30.seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertEquals(departure + 1.minutes, result.departure)
        }
        departure.toScheduleTimestamp(LOCAL_TZ_ID).apply {
            updateDepartureForRealTime(departureDelay = 2.minutes + 36.seconds, currentPrecision = 1.minutes, delayPrecision = 10.seconds)
        }.let { result ->
            assertEquals(departure + 2.minutes, result.departure)
        }
    }
}
