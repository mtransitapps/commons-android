package org.mtransit.android.commons.provider.status

import com.google.transit.realtime.TripDescriptorKt.modifiedTripSelector
import com.google.transit.realtime.TripUpdateKt.stopTimeEvent
import com.google.transit.realtime.TripUpdateKt.stopTimeUpdate
import com.google.transit.realtime.tripDescriptor
import com.google.transit.realtime.tripUpdate
import com.google.transit.realtime.vehicleDescriptor
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optRouteId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopTimeUpdateList
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripId
import org.mtransit.commons.CommonsApp
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.TimeZone as KtTimeZone

class GTFSRealTimeTripUpdatesFilterTests {

    companion object {
        private const val LOCAL_TZ_ID: String = "America/Montreal"
        private val LOCAL_TZ = KtTimeZone.of(LOCAL_TZ_ID)

        private const val TRIP_ID = "123456789"

        private const val NOW_IN_MS = 1793056759_000L // Monday, October 26, 2026, at 7:19:19 p.m. UTC-04:00 DST
    }

    @BeforeTest
    fun setUp() {
        CommonsApp.setup(isAndroidPlatform = false)
    }

    // region TripDescriptor match

    @Test
    fun test_match_trip_id_only_matches() {
        tripDescriptor {
            tripId = TRIP_ID
        }.match(
            nowMs = NOW_IN_MS,
            targetRouteIdHash = "1",
            targetDirectionOriginalId = 1,
            agencyTimeZone = LOCAL_TZ,
            staticTripIdToFirstStartDateTime = emptyMap(),
            staticRDTripIds = setOf(TRIP_ID),
            ignoreDirection = false,
            parseRouteId = { it.optRouteId },
            parseTripId = { it.optTripId },
        ).let { result ->
            if (IGNORE_TRIP_ID) {
                assertEquals(null, result)
            } else {
                assertEquals(TRIP_ID, result)
            }
        }
    }

    @Test
    fun test_match_trip_id_only_does_not_match() {
        tripDescriptor {
            tripId = TRIP_ID
        }.match(
            nowMs = NOW_IN_MS,
            targetRouteIdHash = "1",
            targetDirectionOriginalId = 1,
            agencyTimeZone = LOCAL_TZ,
            staticTripIdToFirstStartDateTime = emptyMap(),
            staticRDTripIds = setOf("different_trip_id"),
            ignoreDirection = false,
            parseRouteId = { it.optRouteId },
            parseTripId = { it.optTripId },
        ).let { result ->
            assertEquals(null, result)
        }
    }

    @Test
    fun test_match_trip_id_does_not_match_and_route_matches() {
        tripDescriptor {
            routeId = "1"
            tripId = TRIP_ID
        }.match(
            nowMs = NOW_IN_MS,
            targetRouteIdHash = "1",
            targetDirectionOriginalId = 1,
            agencyTimeZone = LOCAL_TZ,
            staticTripIdToFirstStartDateTime = emptyMap(),
            staticRDTripIds = setOf("different_trip_id"),
            ignoreDirection = false,
            parseRouteId = { it.optRouteId },
            parseTripId = { it.optTripId },
        ).let { result ->
            assertEquals(null, result)
        }
    }

    @Test
    fun test_match_outdated_trip_id_matches_static_route_id_start_date_time() {
        tripDescriptor {
            routeId = "1"
            tripId = "outdated"
            startDate = "20261026"
            startTime = "19:37:00"
        }.match(
            nowMs = NOW_IN_MS,
            targetRouteIdHash = "1",
            targetDirectionOriginalId = 1,
            agencyTimeZone = LOCAL_TZ,
            staticTripIdToFirstStartDateTime = mapOf(
                TRIP_ID to ("20261026" to "19:37:00")
            ),
            staticRDTripIds = setOf(TRIP_ID),
            ignoreDirection = false,
            parseRouteId = { it.optRouteId },
            parseTripId = { it.optTripId },
        ).let { result ->
            assertEquals(TRIP_ID, result)
        }
    }

    @Test
    fun test_match_outdated_trip_id_matches_static_route_id_start_date() { // not frequency-based
        tripDescriptor {
            routeId = "1"
            tripId = "outdated"
            startTime = "19:37:00"
        }.match(
            nowMs = NOW_IN_MS,
            targetRouteIdHash = "1",
            targetDirectionOriginalId = 1,
            agencyTimeZone = LOCAL_TZ,
            staticTripIdToFirstStartDateTime = mapOf(
                "previous_trip_id" to ("20261025" to "19:37:00"),
                TRIP_ID to ("20261026" to "19:37:00"),
                "next_trip_id" to ("20261027" to "19:37:00"),
            ),
            staticRDTripIds = setOf(TRIP_ID),
            ignoreDirection = false,
            parseRouteId = { it.optRouteId },
            parseTripId = { it.optTripId },
        ).let { result ->
            assertEquals(TRIP_ID, result)
        }
    }

    @Test
    fun test_match_outdated_trip_id_matches_static_route_id_start_time() { // not frequency-based
        tripDescriptor {
            routeId = "1"
            tripId = "outdated"
            startDate = "20261026"
        }.match(
            nowMs = NOW_IN_MS,
            targetRouteIdHash = "1",
            targetDirectionOriginalId = 1,
            agencyTimeZone = LOCAL_TZ,
            staticTripIdToFirstStartDateTime = mapOf(
                TRIP_ID to ("20261026" to "19:37:00")
            ),
            staticRDTripIds = setOf(TRIP_ID),
            ignoreDirection = false,
            parseRouteId = { it.optRouteId },
            parseTripId = { it.optTripId },
        ).let { result ->
            assertEquals(null, result)
        }
    }

    // endregion

    // region filterDuplicatesTrips

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
            assertEquals(1, result.size)
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
            },
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
        ).filterDuplicatesTrips().let { result ->
            assertEquals(1, result.size)
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
            assertEquals(1, result.size)
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

    // endregion
}
