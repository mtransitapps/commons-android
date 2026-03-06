package org.mtransit.android.commons.provider.status

import com.google.transit.realtime.TripUpdateKt.stopTimeEvent
import com.google.transit.realtime.TripUpdateKt.stopTimeUpdate
import com.google.transit.realtime.tripDescriptor
import com.google.transit.realtime.tripUpdate
import org.mtransit.android.commons.data.Accessibility
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.Schedule
import org.mtransit.android.commons.data.Stop
import org.mtransit.android.commons.data.arrival
import org.mtransit.android.commons.data.arrivalDiff
import org.mtransit.android.commons.data.departure
import org.mtransit.android.commons.data.toScheduleTimestamp
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.delayDuration
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
import kotlin.time.Instant
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate as GTUStopTimeUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent as GTUStopTimeEvent
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship as GTUScheduleRelationship

class GTFSRealTimeTripUpdatesProviderTests {

    companion object {
        private const val LOCAL_TZ_ID: String = "America/Montreal"

        private const val DEPARTURE_MS = 1772722800L // 2026-03-06 10:00:

        private const val NOW_IN_MS = 123456789_000L
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

    // region trip update

    private val isSameStopId: ((GTUStopTimeUpdate?, RouteDirectionStop?) -> Boolean) =
        { stu, rds ->
            rds?.stop?.originalIdHashString == stu?.stopId?.hashCode()?.toString()
        }

    @Test
    fun test_wipTripUpdate_singleTUDelay() {
        val tripId = "123456789"
        val tripStart = DEPARTURE_MS.secsToInstant()
        val gTripUpdate = tripUpdate {
            trip = tripDescriptor {
                this.tripId = tripId
            }
            delayDuration = 1.minutes
        }
        val rdsList = buildList {
            add(makeRDS(stopId = 1000))
            add(makeRDS(stopId = 2000))
            add(makeRDS(stopId = 3000))
        }
        val tripTargetUuidSchedule = buildMap<String, Schedule?> {
            rdsList[0].uuid.let { put(it, mkSchedule(it, listOf(mkTime(tripStart, tripId)))) }
            rdsList[1].uuid.let { put(it, mkSchedule(it, listOf(mkTime(tripStart + 10.minutes, tripId)))) }
            rdsList[2].uuid.let { put(it, mkSchedule(it, listOf(mkTime(tripStart + 20.minutes, tripId)))) }
        }

        wipTripUpdate(tripId, gTripUpdate, rdsList, tripTargetUuidSchedule, isSameStopId)

        assertNotNull(tripTargetUuidSchedule[rdsList[0].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(tripStart + 1.minutes, timestamp.departure)
                assertTrue { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[1].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(tripStart + 11.minutes, timestamp.departure)
                assertTrue { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[2].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(tripStart + 21.minutes, timestamp.departure)
                assertTrue { timestamp.isRealTime }
            }
        }
    }

    @Test
    fun test_wipTripUpdate_2() {
        val tripId = "123456789"
        val startsAt = DEPARTURE_MS.secsToInstant()
        val gTripUpdate = tripUpdate {
            trip = tripDescriptor {
                this.tripId = tripId
            }
            delayDuration = 1.minutes
            stopTimeUpdate += stopTimeUpdate {
                stopId = "2000"
                departure = stopTimeEvent {
                    delayDuration = 3.minutes
                }
            }
            stopTimeUpdate += stopTimeUpdate {
                stopId = "4000"
                arrival = stopTimeEvent {
                    delayDuration = 5.minutes
                }
            }
            if (true) return@tripUpdate // WIP
            stopTimeUpdate += stopTimeUpdate {
                stopId = "7000"
                scheduleRelationship = GTUScheduleRelationship.NO_DATA
            }
        }
        val rdsList = buildList {
            add(makeRDS(stopId = 1000))
            add(makeRDS(stopId = 2000))
            add(makeRDS(stopId = 3000))
            add(makeRDS(stopId = 4000))
            add(makeRDS(stopId = 5000))
            add(makeRDS(stopId = 6000))
            if (true) return@buildList // WIP
            add(makeRDS(stopId = 7000))
            add(makeRDS(stopId = 8000))
        }
        val tripTargetUuidSchedule = buildMap<String, Schedule?> {
            rdsList[0].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt, tripId)))) }
            rdsList[1].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 10.minutes, tripId)))) }
            rdsList[2].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 20.minutes, tripId)))) }
            rdsList[3].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 30.minutes, tripId)))) }
            rdsList[4].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 43.minutes, tripId, arrival = startsAt + 37.minutes)))) }
            rdsList[5].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 50.minutes, tripId)))) }
            if (true) return@buildMap // WIP
            rdsList[6].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 60.minutes, tripId)))) }
            rdsList[7].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 70.minutes, tripId)))) }
        }

        wipTripUpdate(tripId, gTripUpdate, rdsList, tripTargetUuidSchedule, isSameStopId)

        assertNotNull(tripTargetUuidSchedule[rdsList[0].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 1.minutes, timestamp.arrival)
                assertEquals(startsAt + 1.minutes, timestamp.departure)
                assertTrue { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[1].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 10.minutes + 1.minutes, timestamp.arrival)
                assertEquals(startsAt + 10.minutes + 3.minutes, timestamp.departure)
                assertTrue { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[2].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 20.minutes + 3.minutes, timestamp.departure)
                assertTrue { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[3].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 30.minutes + 5.minutes, timestamp.arrival)
                assertEquals(startsAt + 30.minutes + 5.minutes, timestamp.departure)
                assertTrue { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[4].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 37.minutes + 5.minutes, timestamp.arrival)
                assertEquals(startsAt + 43.minutes, timestamp.departure)
                assertTrue { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[5].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 50.minutes, timestamp.departure)
                assertTrue { timestamp.isRealTime }
            }
        }
        if (true) return // WIP
        assertNotNull(tripTargetUuidSchedule[rdsList[6].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 60.minutes, timestamp.departure)
                assertFalse { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[7].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 70.minutes, timestamp.departure)
                assertFalse { timestamp.isRealTime }
            }
        }
    }

    // end region

    @Suppress("SameParameterValue")
    private fun mkTime(time: Instant, tripId: String?, arrival: Instant? = null) =
        time.toScheduleTimestamp(LOCAL_TZ_ID, arrival)
            .apply {
                this.tripId = tripId
            }

    private fun mkSchedule(
        targetUuid: String,
        timestamps: List<Schedule.Timestamp> = emptyList(),
        nowInMs: Long = NOW_IN_MS,
    ) = Schedule(
        null,
        targetUuid,
        nowInMs,
        nowInMs,
        nowInMs,
        10.seconds.inWholeMilliseconds,
        false,
        null,
        false
    ).apply {
        setTimestampsAndSort(timestamps)
    }

    private fun makeRDS(stopId: Int = 1) = RouteDirectionStop(
        1,
        Route(
            "authority",
            1,
            "1",
            "route 1",
            "color"
        ),
        Direction(
            "authority",
            1,
            1,
            "headsign",
            1
        ),
        Stop(
            stopId,
            "#$stopId",
            "Stop #$stopId",
            1.0,
            2.0,
            Accessibility.DEFAULT,
            "$stopId".hashCode()
        ),
        false,
        false,
    )
}
