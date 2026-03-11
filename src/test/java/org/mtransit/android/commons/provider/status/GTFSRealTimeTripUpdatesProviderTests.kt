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
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship as GTDScheduleRelationship
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate as GTUStopTimeUpdate
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent as GTUStopTimeEvent
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship as GTUSTUScheduleRelationship

class GTFSRealTimeTripUpdatesProviderTests {

    companion object {
        private const val LOCAL_TZ_ID: String = "America/Montreal"

        private val DEPARTURE = 1772722800L.secsToInstant() // 2026-03-06 10:00:

        private const val NOW_IN_MS = 123456789_000L

        private const val TRIP_ID = "123456789"
        private const val STOP_SEQUENCE = 1
    }

    // region same stop

    @Test
    fun test_isSameStop() {
        assertTrue { stopTimeUpdate { stopId = "1234" }.isSameStop(makeRDS(stopId = 1234), 1, parseStopId = { it }) }
        assertFalse { stopTimeUpdate { stopId = "1234" }.isSameStop(makeRDS(stopId = 5678), 1, parseStopId = { it }) }
        assertFalse { stopTimeUpdate { }.isSameStop(makeRDS(stopId = 1234), 1, parseStopId = { it }) }
        assertTrue { stopTimeUpdate { stopSequence = 7 }.isSameStop(makeRDS(stopId = 1234), 7, parseStopId = { it }) }
        assertFalse { stopTimeUpdate { }.isSameStop(makeRDS(stopId = 1234), 7, parseStopId = { it }) }
        assertTrue { stopTimeUpdate { stopId = "1234"; stopSequence = 7 }.isSameStop(makeRDS(stopId = 1234), 7, parseStopId = { it }) }
        assertFalse { stopTimeUpdate { stopId = "1234"; stopSequence = 7 }.isSameStop(makeRDS(stopId = 5678), 1, parseStopId = { it }) }
    }

    // endregion

    // region applyDelay

    @Test
    fun test_applyDelay_null() {
        val departure = DEPARTURE
        val timestamp = mkTime(departure)
        val delay: Duration? = null

        val result = applyDelay(TRIP_ID, STOP_SEQUENCE, timestamp.toSchedule(), delay)

        assertNull(result)
        assertFalse { timestamp.isRealTime }
        assertEquals(departure, timestamp.departure)
    }

    @Test
    fun test_applyDelay_0_on_time() {
        val departure = DEPARTURE
        val timestamp = mkTime(departure)
        val delay = Duration.ZERO

        val result = applyDelay(TRIP_ID, STOP_SEQUENCE, timestamp.toSchedule(), delay)

        assertNotNull(result)
        assertEquals(delay, result) // delay not consumed
        assertTrue { timestamp.isRealTime }
        assertEquals(departure, timestamp.departure)
    }

    @Test
    fun test_applyDelay_simple_late() {
        val departure = DEPARTURE
        val timestamp = mkTime(departure)
        val delay = 10.minutes

        val result = applyDelay(TRIP_ID, STOP_SEQUENCE, timestamp.toSchedule(), delay)

        assertNotNull(result)
        assertEquals(delay, result) // delay not consumed
        assertTrue { timestamp.isRealTime }
        assertEquals(departure + delay, timestamp.departure)
    }

    @Test
    fun test_applyDelay_differentArrival_late() {
        val departure = DEPARTURE
        val arrival = departure - 1.minutes
        val timestamp = mkTime(departure, arrival = arrival)
        val delay = 10.minutes

        val result = applyDelay(TRIP_ID, STOP_SEQUENCE, timestamp.toSchedule(), delay)

        assertNotNull(result)
        assertEquals(9.minutes, result) // delay partially consumed
        assertTrue { timestamp.isRealTime }
        assertEquals(arrival + delay, timestamp.arrival)
        assertEquals(departure + result, timestamp.departure)
    }

    @Test
    fun test_applyDelay_consumed_late() {
        val departure = DEPARTURE
        val arrival = departure - 15.minutes
        val timestamp = mkTime(departure, arrival = arrival)
        val delay = 10.minutes

        val result = applyDelay(TRIP_ID, STOP_SEQUENCE, timestamp.toSchedule(), delay)

        assertNotNull(result)
        assertEquals(Duration.ZERO, result) // delay consumed
        assertTrue { timestamp.isRealTime }
        assertEquals(arrival + delay, timestamp.arrival)
        assertEquals(departure, timestamp.departure)
    }

    @Test
    fun test_applyDelay_simple_early() {
        val departure = DEPARTURE
        val arrival = departure - 1.minutes
        val timestamp = mkTime(departure, arrival = arrival)
        val delay = (-5).minutes

        val result = applyDelay(TRIP_ID, STOP_SEQUENCE, timestamp.toSchedule(), delay)

        assertNotNull(result)
        assertEquals(delay, result) // delay not consumed
        assertTrue { timestamp.isRealTime }
        assertEquals(arrival - 5.minutes, timestamp.arrival)
        assertEquals(departure - 5.minutes, timestamp.departure)
    }

    // endregion

    // region applyDelaySTU

    @Test
    fun test_applyDelaySTU_simple() {
        val departure = DEPARTURE
        val timestamp = mkTime(departure)
        val stopTimeUpdate = stopTimeUpdate {
            this.departure = stopTimeEvent {
                time = (departure + 1.minutes).toSecs()
            }
        }

        val result = applyDelaySTU(TRIP_ID, STOP_SEQUENCE, timestamp.toSchedule(), stopTimeUpdate)

        assertNotNull(result)
        assertEquals(1.minutes, result)
        assertTrue { timestamp.isRealTime }
        assertEquals(departure + 1.minutes, timestamp.departure)
    }

    @Test
    fun test_applyDelaySTU_preferTimeOverDelay() {
        val departure = DEPARTURE
        val arrival = departure - 5.minutes
        // val updatedArrival =
        val timestamp = mkTime(departure, arrival = arrival)
        val stopTimeUpdate = stopTimeUpdate {
            this.arrival = stopTimeEvent {
                delayDuration = (-1).minutes
                time = (arrival - 63.seconds).toSecs()
            }
            this.departure = stopTimeEvent {
                delayDuration = 2.minutes
                time = (departure + 124.seconds).toSecs()
            }
        }

        val result = applyDelaySTU(TRIP_ID, STOP_SEQUENCE, timestamp.toSchedule(), stopTimeUpdate)

        assertNotNull(result)
        assertEquals(2.minutes, result)
        assertTrue { timestamp.isRealTime }
        assertEquals(arrival - 63.seconds, timestamp.arrival)
        assertEquals(departure + 124.seconds, timestamp.departure)
    }

    @Test
    fun test_applyDelaySTU_2() {
        val departure = DEPARTURE
        val arrival = departure - 5.minutes
        val timestamp = mkTime(departure, arrival = arrival)
        val stopTimeUpdate = stopTimeUpdate {
            this.arrival = stopTimeEvent {
                time = (arrival - 1.minutes).toSecs()
            }
            this.departure = stopTimeEvent {
                time = (departure + 2.minutes).toSecs()
            }
        }

        val result = applyDelaySTU(TRIP_ID, STOP_SEQUENCE, timestamp.toSchedule(), stopTimeUpdate)

        assertNotNull(result)
        assertEquals(2.minutes, result)
        assertTrue { timestamp.isRealTime }
        assertEquals(arrival - 1.minutes, timestamp.arrival)
        assertEquals(departure + 2.minutes, timestamp.departure)
    }

    @Test
    fun test_applyDelaySTU_3() {
        val departure = DEPARTURE
        val arrival = departure - 5.minutes
        val delay = 1.minutes
        val timestamp = mkTime(departure, arrival = arrival)
        val stopTimeUpdate = stopTimeUpdate {
            this.departure = stopTimeEvent {
                time = (departure + 2.minutes).toSecs()
            }
        }

        val result = applyDelaySTU(TRIP_ID, STOP_SEQUENCE, timestamp.toSchedule(), stopTimeUpdate, delay)

        assertNotNull(result)
        assertEquals(2.minutes, result)
        assertTrue { timestamp.isRealTime }
        assertEquals(arrival + 1.minutes, timestamp.arrival)
        assertEquals(departure + 2.minutes, timestamp.departure)
    }

    @Test
    fun test_applyDelaySTU_4() {
        val departure = DEPARTURE
        val arrival = departure - 1.minutes
        val delay = 15.minutes // should be ignored
        val timestamp = mkTime(departure, arrival = arrival)
        val stopTimeUpdate = stopTimeUpdate {
            this.arrival = stopTimeEvent {
                time = (arrival + 3.minutes).toSecs()
            }
        }

        val result = applyDelaySTU(TRIP_ID, STOP_SEQUENCE, timestamp.toSchedule(), stopTimeUpdate, delay)

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
        val originalTime = DEPARTURE
        val stopTimeEvent = stopTimeEvent {
            delay = 10
        }

        val result = stopTimeEvent.makeDelay(originalTime)

        assertNotNull(result)
        assertEquals(10.seconds, result)
    }

    @Test
    fun test_makeDelay_2() {
        val originalTime = DEPARTURE
        val stopTimeEvent = stopTimeEvent {
            time = (originalTime + 10.seconds).toSecs()
        }

        val result = stopTimeEvent.makeDelay(originalTime)

        assertNotNull(result)
        assertEquals(10.seconds, result)
    }

    @Test
    fun test_makeDelay_3() {
        val departure = DEPARTURE
        val arrival = departure - 3.minutes
        val timestamp = mkTime(departure, arrival = arrival)
        val previousDelay = 10.minutes
        val stopTimeEvent: GTUStopTimeEvent? = null

        val result = stopTimeEvent.makeDelay(
            originalTime = timestamp.departure,
            previousSTEDelay = previousDelay,
            previousCurrentDiff = timestamp.arrivalDiff
        )

        assertNotNull(result)
        assertEquals(7.minutes, result)
    }

    // endregion

    // region trip update

    private val isSameStop: ((GTUStopTimeUpdate?, RouteDirectionStop?, Int) -> Boolean) =
        { stu, rds, stopSeq ->
            stu?.isSameStop(rds, stopSeq, parseStopId = { it }) == true
        }

    @Test
    fun test_processRDTripUpdate_singleTUDelay() {
        val tripStart = DEPARTURE
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
            rdsList[0].uuid.let { put(it, mkSchedule(it, listOf(mkTime(tripStart)))) }
            rdsList[1].uuid.let { put(it, mkSchedule(it, listOf(mkTime(tripStart + 10.minutes)))) }
            rdsList[2].uuid.let { put(it, mkSchedule(it, listOf(mkTime(tripStart + 20.minutes)))) }
        }
        val sortedTargetUuidAndSequence = buildList {
            rdsList.forEachIndexed { index, rds ->
                add(rds.uuid to index + 1)
            }
        }

        processRDTripUpdate(TRIP_ID, gTripUpdate, rdsList, sortedTargetUuidAndSequence, tripTargetUuidSchedule, isSameStop)

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
    fun test_processRDTripUpdate_combined_complex_stop_id() {
        val startsAt = DEPARTURE
        val gTripUpdate = tripUpdate {
            trip = tripDescriptor {
                tripId = TRIP_ID
            }
            stopTimeUpdate += stopTimeUpdate {
                stopId = "2000"
                departure = stopTimeEvent {
                    delayDuration = 3.minutes
                }
            }
            stopTimeUpdate += stopTimeUpdate {
                stopId = "4000"
                arrival = stopTimeEvent {
                    time = ((startsAt + 30.minutes) + 5.minutes).toSecs()
                }
            }
            stopTimeUpdate += stopTimeUpdate {
                stopId = "7000"
                scheduleRelationship = GTUSTUScheduleRelationship.SKIPPED
            }
            stopTimeUpdate += stopTimeUpdate {
                stopId = "9000"
                scheduleRelationship = GTUSTUScheduleRelationship.NO_DATA
            }
        }
        val rdsList = buildList {
            add(makeRDS(stopId = 1000))
            add(makeRDS(stopId = 2000))
            add(makeRDS(stopId = 3000))
            add(makeRDS(stopId = 4000))
            add(makeRDS(stopId = 5000))
            add(makeRDS(stopId = 6000))
            add(makeRDS(stopId = 7000))
            add(makeRDS(stopId = 8000))
            add(makeRDS(stopId = 9000))
            add(makeRDS(stopId = 10000))
        }
        val tripTargetUuidSchedule = buildMap<String, Schedule?> {
            rdsList[0].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt)))) }
            rdsList[1].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 10.minutes)))) }
            rdsList[2].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 20.minutes)))) }
            rdsList[3].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 30.minutes)))) }
            rdsList[4].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 43.minutes, arrival = startsAt + 37.minutes)))) }
            rdsList[5].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 50.minutes)))) }
            rdsList[6].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 60.minutes)))) }
            rdsList[7].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 70.minutes)))) }
            rdsList[8].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 80.minutes)))) }
            rdsList[9].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 90.minutes)))) }
        }
        val sortedTargetUuidAndSequence = buildList {
            rdsList.forEachIndexed { index, rds ->
                add(rds.uuid to index + 1)
            }
        }

        processRDTripUpdate(TRIP_ID, gTripUpdate, rdsList, sortedTargetUuidAndSequence, tripTargetUuidSchedule, isSameStop)

        assertNotNull(tripTargetUuidSchedule[rdsList[0].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt, timestamp.arrival)
                assertEquals(startsAt, timestamp.departure)
                assertFalse { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[1].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 10.minutes, timestamp.arrival)
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
        assertNotNull(tripTargetUuidSchedule[rdsList[6].uuid]) { schedule ->
            assertTrue { schedule.timestamps.isEmpty() }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[7].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 70.minutes, timestamp.departure)
                assertTrue { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[8].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 80.minutes, timestamp.departure)
                assertFalse { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[9].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 90.minutes, timestamp.departure)
                assertFalse { timestamp.isRealTime }
            }
        }
    }

    @Test
    fun test_processRDTripUpdate_combined_complex_stop_sequence() {
        val startsAt = DEPARTURE
        val gTripUpdate = tripUpdate {
            trip = tripDescriptor {
                tripId = TRIP_ID
            }
            delayDuration = 1.minutes
            stopTimeUpdate += stopTimeUpdate {
                stopSequence = 2
                departure = stopTimeEvent {
                    delayDuration = 3.minutes
                }
            }
            stopTimeUpdate += stopTimeUpdate {
                stopSequence = 4
                arrival = stopTimeEvent {
                    time = ((startsAt + 30.minutes) + 5.minutes).toSecs()
                }
            }
            stopTimeUpdate += stopTimeUpdate {
                stopSequence = 7
                scheduleRelationship = GTUSTUScheduleRelationship.SKIPPED
            }
            stopTimeUpdate += stopTimeUpdate {
                stopSequence = 9
                scheduleRelationship = GTUSTUScheduleRelationship.NO_DATA
            }
        }
        val rdsList = buildList {
            add(makeRDS(stopId = 1000))
            add(makeRDS(stopId = 2000))
            add(makeRDS(stopId = 3000))
            add(makeRDS(stopId = 4000))
            add(makeRDS(stopId = 5000))
            add(makeRDS(stopId = 6000))
            add(makeRDS(stopId = 7000))
            add(makeRDS(stopId = 8000))
            add(makeRDS(stopId = 9000))
            add(makeRDS(stopId = 10000))
        }
        var stopSeq = 0
        val tripTargetUuidSchedule = buildMap<String, Schedule?> {
            rdsList[0].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt, stopSeq = ++stopSeq)))) }
            rdsList[1].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 10.minutes, stopSeq = ++stopSeq)))) }
            rdsList[2].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 20.minutes, stopSeq = ++stopSeq)))) }
            rdsList[3].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 30.minutes, stopSeq = ++stopSeq)))) }
            rdsList[4].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 43.minutes, stopSeq = ++stopSeq, arrival = startsAt + 37.minutes)))) }
            rdsList[5].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 50.minutes, stopSeq = ++stopSeq)))) }
            rdsList[6].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 60.minutes, stopSeq = ++stopSeq)))) }
            rdsList[7].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 70.minutes, stopSeq = ++stopSeq)))) }
            rdsList[8].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 80.minutes, stopSeq = ++stopSeq)))) }
            rdsList[9].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 90.minutes, stopSeq = ++stopSeq)))) }
        }
        val sortedTargetUuidAndSequence = buildList {
            tripTargetUuidSchedule.forEach { (uuid, schedule) ->
                schedule?.timestamps?.forEach { timestamp ->
                    add(uuid to assertNotNull(timestamp.stopSequenceOrNull))
                }
            }
        }

        processRDTripUpdate(TRIP_ID, gTripUpdate, rdsList, sortedTargetUuidAndSequence, tripTargetUuidSchedule, isSameStop)

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
        assertNotNull(tripTargetUuidSchedule[rdsList[6].uuid]) { schedule ->
            assertTrue { schedule.timestamps.isEmpty() }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[7].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 70.minutes, timestamp.departure)
                assertTrue { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[8].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 80.minutes, timestamp.departure)
                assertFalse { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[9].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 90.minutes, timestamp.departure)
                assertFalse { timestamp.isRealTime }
            }
        }
    }

    @Test
    fun test_processRDTripUpdate_combined_complex_stop_sequence_repeated_stop() {
        val startsAt = DEPARTURE
        val gTripUpdate = tripUpdate {
            trip = tripDescriptor {
                tripId = TRIP_ID
            }
            delayDuration = 1.minutes
            stopTimeUpdate += stopTimeUpdate {
                stopSequence = 2
                departure = stopTimeEvent {
                    delayDuration = 3.minutes
                }
            }
            stopTimeUpdate += stopTimeUpdate {
                stopSequence = 4
                arrival = stopTimeEvent {
                    time = ((startsAt + 30.minutes) + 5.minutes).toSecs()
                }
            }
            stopTimeUpdate += stopTimeUpdate {
                stopSequence = 7
                scheduleRelationship = GTUSTUScheduleRelationship.SKIPPED
            }
            stopTimeUpdate += stopTimeUpdate {
                stopSequence = 9
                scheduleRelationship = GTUSTUScheduleRelationship.NO_DATA
            }
        }
        val rdsList = buildList {
            add(makeRDS(stopId = 1000))
            add(makeRDS(stopId = 2000))
            add(makeRDS(stopId = 3000))
            add(makeRDS(stopId = 4000))
            add(makeRDS(stopId = 5000))
            add(makeRDS(stopId = 6000))
            add(makeRDS(stopId = 7000))
            add(makeRDS(stopId = 9000))
            add(makeRDS(stopId = 10000))
        }
        var stopSeq = 0
        val tripTargetUuidSchedule = buildMap<String, Schedule?> {
            rdsList[0].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt, stopSeq = ++stopSeq)))) }
            rdsList[1].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 10.minutes, stopSeq = ++stopSeq)))) }
            rdsList[2].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 20.minutes, stopSeq = ++stopSeq)))) }
            rdsList[3].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 30.minutes, stopSeq = ++stopSeq)))) }
            rdsList[4].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 43.minutes, stopSeq = ++stopSeq, arrival = startsAt + 37.minutes)))) }
            rdsList[5].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 50.minutes, stopSeq = ++stopSeq)))) }
            rdsList[6].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 60.minutes, stopSeq = ++stopSeq)))) }
            get(rdsList[3].uuid)?.apply {
                addTimestampWithoutSort(mkTime(startsAt + 70.minutes, stopSeq = ++stopSeq))
                sortTimestamps()
            }
            rdsList[7].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 80.minutes, stopSeq = ++stopSeq)))) }
            rdsList[8].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 90.minutes, stopSeq = ++stopSeq)))) }
        }
        val sortedTargetUuidAndSequence = buildList {
            tripTargetUuidSchedule.forEach { (uuid, schedule) ->
                schedule?.timestamps?.forEach { timestamp ->
                    add(uuid to assertNotNull(timestamp.stopSequenceOrNull))
                }
            }
        }.sortedBy { (_, stopSequence) -> stopSequence }


        processRDTripUpdate(TRIP_ID, gTripUpdate, rdsList, sortedTargetUuidAndSequence, tripTargetUuidSchedule, isSameStop)

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
            assertNotNull(schedule.timestamps.getOrNull(0)) { timestamp ->
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
        assertNotNull(tripTargetUuidSchedule[rdsList[6].uuid]) { schedule ->
            assertTrue { schedule.timestamps.isEmpty() }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[3].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.getOrNull(1)) { timestamp ->
                assertEquals(startsAt + 70.minutes, timestamp.departure)
                assertTrue { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[7].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 80.minutes, timestamp.departure)
                assertFalse { timestamp.isRealTime }
            }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[8].uuid]) { schedule ->
            assertNotNull(schedule.timestamps.singleOrNull()) { timestamp ->
                assertEquals(startsAt + 90.minutes, timestamp.departure)
                assertFalse { timestamp.isRealTime }
            }
        }
    }

    @Test
    fun test_processRDTripUpdate_trip_cancelled() {
        val startsAt = DEPARTURE
        val gTripUpdate = tripUpdate {
            trip = tripDescriptor {
                tripId = TRIP_ID
                this.scheduleRelationship = GTDScheduleRelationship.CANCELED
            }
            delayDuration = 1.minutes
        }
        val rdsList = buildList {
            add(makeRDS(stopId = 1000))
            add(makeRDS(stopId = 2000))
            add(makeRDS(stopId = 3000))
        }
        val tripTargetUuidSchedule = buildMap<String, Schedule?> {
            rdsList[0].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt)))) }
            rdsList[1].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 10.minutes)))) }
            rdsList[2].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 20.minutes)))) }
        }
        val sortedTargetUuidAndSequence = buildList {
            rdsList.forEachIndexed { index, rds ->
                add(rds.uuid to index + 1)
            }
        }

        processRDTripUpdate(TRIP_ID, gTripUpdate, rdsList, sortedTargetUuidAndSequence, tripTargetUuidSchedule, isSameStop)

        assertNotNull(tripTargetUuidSchedule[rdsList[0].uuid]) { schedule ->
            assertTrue { schedule.timestamps.isEmpty() }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[1].uuid]) { schedule ->
            assertTrue { schedule.timestamps.isEmpty() }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[2].uuid]) { schedule ->
            assertTrue { schedule.timestamps.isEmpty() }
        }
    }

    @Test
    fun test_processRDTripUpdate_trip_deleted() {
        val startsAt = DEPARTURE
        val gTripUpdate = tripUpdate {
            trip = tripDescriptor {
                tripId = TRIP_ID
                this.scheduleRelationship = GTDScheduleRelationship.DELETED
            }
            delayDuration = 1.minutes
        }
        val rdsList = buildList {
            add(makeRDS(stopId = 1000))
            add(makeRDS(stopId = 2000))
            add(makeRDS(stopId = 3000))
        }
        val tripTargetUuidSchedule = buildMap<String, Schedule?> {
            rdsList[0].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt)))) }
            rdsList[1].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 10.minutes)))) }
            rdsList[2].uuid.let { put(it, mkSchedule(it, listOf(mkTime(startsAt + 20.minutes)))) }
        }
        val sortedTargetUuidAndSequence = buildList {
            rdsList.forEachIndexed { index, rds ->
                add(rds.uuid to index + 1)
            }
        }

        processRDTripUpdate(TRIP_ID, gTripUpdate, rdsList, sortedTargetUuidAndSequence, tripTargetUuidSchedule, isSameStop)

        assertNotNull(tripTargetUuidSchedule[rdsList[0].uuid]) { schedule ->
            assertTrue { schedule.timestamps.isEmpty() }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[1].uuid]) { schedule ->
            assertTrue { schedule.timestamps.isEmpty() }
        }
        assertNotNull(tripTargetUuidSchedule[rdsList[2].uuid]) { schedule ->
            assertTrue { schedule.timestamps.isEmpty() }
        }
    }

    // end region

    private fun Schedule.Timestamp.toSchedule() = mkSchedule(
        timestamps = listOf(this),
    )

    @Suppress("SameParameterValue")
    private fun mkTime(time: Instant, tripId: String? = TRIP_ID, stopSeq: Int? = null, arrival: Instant? = null) =
        time.toScheduleTimestamp(LOCAL_TZ_ID, arrival, tripId, stopSeq)

    private fun mkSchedule(
        targetUuid: String = makeRDS().uuid,
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
            stopId, // "$stopId".hashCode()
        ),
        false,
        false,
    )
}
