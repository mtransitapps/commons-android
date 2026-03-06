package org.mtransit.android.commons.provider.status

import com.google.transit.realtime.TripUpdateKt.stopTimeEvent
import com.google.transit.realtime.TripUpdateKt.stopTimeUpdate
import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.arrivalDiff
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.data.toScheduleTimestamp
import org.mtransit.android.commons.secsToInstant
import org.mtransit.android.commons.toSecs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent as GTUStopTimeEvent

class GTFSRealTimeTripUpdatesProviderTests {

    companion object {
        private const val LOCAL_TZ_ID: String = "America/Montreal"

        private const val DEPARTURE_MS = 1772722800L // 2026-03-06 10:00:
    }

    // region applyDelay

    @Test
    fun text_applyDelay_null() {
        val departure = DEPARTURE_MS.secsToInstant()
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID)
        val delay: Duration? = null

        val result = wipApplyDelay(timestamp, delay)

        assertNull(result)
        assertFalse { timestamp.isRealTime }
        assertEquals(departure, timestamp.departure)
    }

    @Test
    fun text_applyDelay_0_on_time() {
        val departure = DEPARTURE_MS.secsToInstant()
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
        val departure = DEPARTURE_MS.secsToInstant()
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
        val departure = DEPARTURE_MS.secsToInstant()
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
        val departure = DEPARTURE_MS.secsToInstant()
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
        val departure = DEPARTURE_MS.secsToInstant()
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

    // endregion

    // region applyDelaySTU

    @Test
    fun text_applyDelaySTU_simple() {
        val departure = DEPARTURE_MS.secsToInstant()
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID)
        val stopTimeUpdate = stopTimeUpdate {
            this.departure = stopTimeEvent {
                time = (departure + 1.minutes).toSecs()
            }
        }

        val result = wipApplyDelaySTU(timestamp, stopTimeUpdate)

        assertNotNull(result)
        assertEquals(1.minutes, result)
        assertTrue { timestamp.isRealTime }
        assertEquals(departure + 1.minutes, timestamp.departure)
    }

    @Test
    fun text_applyDelaySTU_2() {
        val departure = DEPARTURE_MS.secsToInstant()
        val arrival = departure - 5.minutes
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival)
        val stopTimeUpdate = stopTimeUpdate {
            this.arrival = stopTimeEvent {
                time = (arrival - 1.minutes).toSecs()
            }
            this.departure = stopTimeEvent {
                time = (departure + 2.minutes).toSecs()
            }
        }

        val result = wipApplyDelaySTU(timestamp, stopTimeUpdate)

        assertNotNull(result)
        assertEquals(2.minutes, result)
        assertTrue { timestamp.isRealTime }
        assertEquals(arrival - 1.minutes, timestamp.arrival)
        assertEquals(departure + 2.minutes, timestamp.departure)
    }

    @Test
    fun text_applyDelaySTU_3() {
        val departure = DEPARTURE_MS.secsToInstant()
        val arrival = departure - 5.minutes
        val delay = 1.minutes
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival)
        val stopTimeUpdate = stopTimeUpdate {
            this.departure = stopTimeEvent {
                time = (departure + 2.minutes).toSecs()
            }
        }

        val result = wipApplyDelaySTU(timestamp, stopTimeUpdate, delay)

        assertNotNull(result)
        assertEquals(2.minutes, result)
        assertTrue { timestamp.isRealTime }
        assertEquals(arrival + 1.minutes, timestamp.arrival)
        assertEquals(departure + 2.minutes, timestamp.departure)
    }

    @Test
    fun text_applyDelaySTU_4() {
        val departure = DEPARTURE_MS.secsToInstant()
        val arrival = departure - 1.minutes
        val delay = 15.minutes // should be ignored
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival)
        val stopTimeUpdate = stopTimeUpdate {
            this.arrival = stopTimeEvent {
                time = (arrival + 3.minutes).toSecs()
            }
        }

        val result = wipApplyDelaySTU(timestamp, stopTimeUpdate, delay)

        assertNotNull(result)
        assertEquals(2.minutes, result)
        assertTrue { timestamp.isRealTime }
        assertEquals(arrival + 3.minutes, timestamp.arrival)
        assertEquals(departure + 2.minutes, timestamp.departure)
    }

    // endregion

    // region makeDelay

    @Test
    fun test_makeDelay_1() {
        val originalTime = DEPARTURE_MS.secsToInstant()
        val stopTimeEvent = stopTimeEvent {
            delay = 10
        }

        val result = stopTimeEvent.wipMakeDelay(originalTime)

        assertNotNull(result)
        assertEquals(10.seconds, result)
    }

    @Test
    fun test_makeDelay_2() {
        val originalTime = DEPARTURE_MS.secsToInstant()
        val stopTimeEvent = stopTimeEvent {
            time = (originalTime + 10.seconds).toSecs()
        }

        val result = stopTimeEvent.wipMakeDelay(originalTime)

        assertNotNull(result)
        assertEquals(10.seconds, result)
    }

    @Test
    fun test_makeDelay_3() {
        val departure = DEPARTURE_MS.secsToInstant()
        val arrival = departure - 5.minutes
        val timestamp = departure.toScheduleTimestamp(LOCAL_TZ_ID, arrival)
        val previousDelay = 10.minutes
        val stopTimeEvent: GTUStopTimeEvent? = null

        val result = stopTimeEvent.wipMakeDelay(
            originalTime = timestamp.departure,
            previousDelay = previousDelay,
            previousOriginalDiff = timestamp.arrivalDiff
        )

        assertNotNull(result)
        assertEquals(5.minutes, result)
    }

    // endregion
}
