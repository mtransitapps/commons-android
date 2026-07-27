package org.mtransit.android.commons.provider.status

import com.google.transit.realtime.TripDescriptorKt.modifiedTripSelector
import com.google.transit.realtime.TripUpdateKt.stopTimeEvent
import com.google.transit.realtime.TripUpdateKt.stopTimeUpdate
import com.google.transit.realtime.tripDescriptor
import com.google.transit.realtime.tripUpdate
import com.google.transit.realtime.vehicleDescriptor
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopTimeUpdateList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GTFSRealTimeTripUpdatesFilterTests {

    companion object {
        private const val TRIP_ID = "123456789"
    }

    @Test
    fun test_filterDuplicatesTrips_1Regular_2ModifiedTrips() {
        listOf(
            tripUpdate {
                trip = tripDescriptor {
                    tripId = TRIP_ID
                    startDate = "20260727"
                    startTime = "151800"
                }
                vehicle = vehicleDescriptor {
                    id = "1234"
                    label = "hello"
                }
            },
            tripUpdate {
                trip = tripDescriptor {
                    tripId = TRIP_ID
                    modifiedTrip = modifiedTripSelector {
                        affectedTripId = TRIP_ID
                        modificationsId = TRIP_ID
                        startDate = "20260727"
                        startTime = "151800"
                    }
                }
                vehicle = vehicleDescriptor {
                    id = "1234"
                    label = "hello"
                }
            },
            tripUpdate {
                trip = tripDescriptor {
                    tripId = TRIP_ID
                    modifiedTrip = modifiedTripSelector {
                        affectedTripId = TRIP_ID
                        modificationsId = TRIP_ID
                        startDate = "20260727"
                        startTime = "151800"
                    }
                }
                vehicle = vehicleDescriptor {
                    id = "1234"
                    label = "hello"
                }
            },
        ).filterDuplicatesTrips().let { result ->
            assert(result.size == 1)
            assertNotNull(result.getOrNull(0)) {
                assertEquals(TRIP_ID, it.trip.tripId)
                assertFalse(it.trip.hasModifiedTrip())
                assertTrue(it.hasVehicle())
            }
        }
    }

    @Test
    fun test_filterDuplicatesTrips_2Regulars_W_WO_VehicleDescriptor() {
        listOf(
            tripUpdate {
                trip = tripDescriptor {
                    tripId = TRIP_ID
                    startDate = "20260727"
                    startTime = "151800"
                }
                vehicle = vehicleDescriptor {
                    id = "1234"
                    label = "hello"
                }
            },
            tripUpdate {
                trip = tripDescriptor {
                    tripId = TRIP_ID
                    startDate = "20260727"
                    startTime = "151800"
                }
            },
        ).filterDuplicatesTrips().let { result ->
            assert(result.size == 1)
            assertNotNull(result.getOrNull(0)) {
                assertEquals(TRIP_ID, it.trip.tripId)
                assertFalse(it.trip.hasModifiedTrip())
                assertTrue(it.hasVehicle())
            }
        }
    }

    @Test
    fun test_filterDuplicatesTrips_2ModifiedTrips() {
        listOf(
            tripUpdate {
                trip = tripDescriptor {
                    tripId = TRIP_ID
                    modifiedTrip = modifiedTripSelector {
                        affectedTripId = TRIP_ID
                        modificationsId = TRIP_ID
                        startDate = "20260727"
                        startTime = "151800"
                    }
                }
                vehicle = vehicleDescriptor {
                    id = "1234"
                    label = "hello"
                }
                stopTimeUpdate += stopTimeUpdate {
                    stopSequence = 25
                    stopId = "1096"
                    departure = stopTimeEvent { delay = 39 }
                }
                // etc. (all other trip stops until last one)
            },
            tripUpdate {
                trip = tripDescriptor {
                    tripId = TRIP_ID
                    modifiedTrip = modifiedTripSelector {
                        affectedTripId = TRIP_ID
                        modificationsId = TRIP_ID
                        startDate = "20260727"
                        startTime = "151800"
                    }
                }
                vehicle = vehicleDescriptor {
                    id = "1234"
                    label = "hello"
                }
                stopTimeUpdate += stopTimeUpdate {
                    stopSequence = 1
                    stopId = "1001"
                    departure = stopTimeEvent { delay = 5 }
                }
                // etc. (stop time update unrelated to this trip ID (static data) (either trip modification is massive or just vehicle next trip)
            },
        ).filterDuplicatesTrips().let { result ->
            assert(result.size == 1)
            assertNotNull(result.getOrNull(0)) { gTripUpdate ->
                assertEquals(TRIP_ID, gTripUpdate.trip.tripId)
                assertTrue(gTripUpdate.trip.hasModifiedTrip())
                assertTrue(gTripUpdate.hasVehicle())
                assertEquals(1, gTripUpdate.stopTimeUpdateCount)
                assertNotNull(gTripUpdate.optStopTimeUpdateList?.getOrNull(0)) { stu ->
                    assertEquals(25, stu.stopSequence)
                    assertEquals("1096", stu.stopId)
                    assertEquals(39, stu.departure.delay)
                }
            }
        }
    }
}
