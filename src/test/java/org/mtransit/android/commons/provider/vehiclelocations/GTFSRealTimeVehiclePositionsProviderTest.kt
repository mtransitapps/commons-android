package org.mtransit.android.commons.provider.vehiclelocations

import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mtransit.android.commons.data.getGTFSRTTargetUUID
import org.mtransit.android.commons.data.makeRDS
import org.mtransit.android.commons.data.toRouteDirection
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyTagTargetUUID
import org.mtransit.android.commons.provider.setupProviderForRDS
import org.mtransit.android.commons.provider.vehiclelocations.GTFSRealTimeVehiclePositionsProvider.getCached
import org.mtransit.android.commons.provider.vehiclelocations.model.VehicleLocation
import org.mtransit.android.commons.provider.vehiclelocations.model.makeVehicleLocation
import org.mtransit.commons.CommonsApp
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class GTFSRealTimeVehiclePositionsProviderTest {

    private val gtfsRealTimeProvider: GTFSRealTimeProvider = mock {
        on { getAgencyTag(anyOrNull()) } doReturn "static_agency_id"
    }

    @BeforeTest
    fun setUp() {
        CommonsApp.setup(false)
    }

    @Test
    fun test() {
        val rds1 = makeRDS(
            authority = "static_agency_id",
            routeId = 1L,
            originalDirectionId = 1,
            stopId = 10,
        )
        gtfsRealTimeProvider.setupProviderForRDS(rds1)
        val rds2 = makeRDS(
            authority = "static_agency_id",
            routeId = 2L,
            originalDirectionId = 0,
            stopId = 20,
        )
        gtfsRealTimeProvider.setupProviderForRDS(rds2)
        val cachedVehicleLocation = buildList {
            add(makeVehicleLocation(targetUUID = rds1.getGTFSRTTargetUUID(false), targetTripId = "tripId10", vehicleId = "vehicleId10"))
            add(makeVehicleLocation(targetUUID = rds1.getGTFSRTTargetUUID(false), targetTripId = "tripId11", vehicleId = "vehicleId11"))
            add(makeVehicleLocation(targetUUID = rds1.toRouteDirection().getGTFSRTTargetUUID(), targetTripId = "tripId12", vehicleId = "vehicleId12"))
            add(makeVehicleLocation(targetUUID = rds1.route.getGTFSRTTargetUUID(), targetTripId = "tripId13", vehicleId = "vehicleId13"))
            add(makeVehicleLocation(targetUUID = getAgencyTagTargetUUID("static_agency_id"), targetTripId = "tripId00", vehicleId = "vehicleId00"))
        }
        val staticTripIds = (cachedVehicleLocation.mapNotNull { it.targetTripId } + "tripId22").toSet()
        val getCachedVehicleLocations: (targetUUIDs: Collection<String>, tripIds: List<String>?) -> List<VehicleLocation>? = { targetUUIDs, tripIds ->
            cachedVehicleLocation.filter { vehicleLocation ->
                targetUUIDs.contains(vehicleLocation.targetUUID)
                        && vehicleLocation.targetTripId?.let { tripIds?.contains(it) } != false // ignore if target tripID or local trip ID null
            }.map { it.copy() }
        }
        gtfsRealTimeProvider.getCached(
            filter = VehicleLocationProviderContract.Filter(rds1),
            getCachedVehicleLocations = getCachedVehicleLocations,
            getTripIds = { _, _, _ -> listOf("tripId10", "tripId11", "tripId12", "tripId13") },
            tripIdsOutOfSync = !staticTripIds.contains("tripId10"),
        ).let { result ->
            assertNotNull(result)
            assertEquals(4, result.size)
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId10" }) {
                assertEquals(rds1.toRouteDirection().uuid, it.targetUUID)
                assertEquals("vehicleId10", it.vehicleId)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId11" }) {
                assertEquals(rds1.toRouteDirection().uuid, it.targetUUID)
                assertEquals("vehicleId11", it.vehicleId)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId12" }) {
                assertEquals(rds1.toRouteDirection().uuid, it.targetUUID)
                assertEquals("vehicleId12", it.vehicleId)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId13" }) {
                assertEquals(rds1.route.uuid, it.targetUUID)
                assertEquals("vehicleId13", it.vehicleId)
            }
        }
        gtfsRealTimeProvider.getCached(
            filter = VehicleLocationProviderContract.Filter(rds1.toRouteDirection()),
            getCachedVehicleLocations = getCachedVehicleLocations,
            getTripIds = { _, _, _ -> listOf("tripId10", "tripId11", "tripId12", "tripId13") },
            tripIdsOutOfSync = !staticTripIds.contains("tripId10"),
        ).let { result ->
            assertNotNull(result)
            assertEquals(4, result.size)
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId10" }) {
                assertEquals(rds1.toRouteDirection().uuid, it.targetUUID)
                assertEquals("vehicleId10", it.vehicleId)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId11" }) {
                assertEquals(rds1.toRouteDirection().uuid, it.targetUUID)
                assertEquals("vehicleId11", it.vehicleId)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId12" }) {
                assertEquals(rds1.toRouteDirection().uuid, it.targetUUID)
                assertEquals("vehicleId12", it.vehicleId)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId13" }) {
                assertEquals(rds1.route.uuid, it.targetUUID)
                assertEquals("vehicleId13", it.vehicleId)
            }
        }
        gtfsRealTimeProvider.getCached(
            filter = VehicleLocationProviderContract.Filter(rds1.toRouteDirection()),
            getCachedVehicleLocations = getCachedVehicleLocations,
            getTripIds = { _, _, _ -> fail("should not call since out of sync for tripId177777") },
            tripIdsOutOfSync = !staticTripIds.contains("tripId177777"),
        ).let { result ->
            assertNotNull(result)
            assertEquals(4, result.size)
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId10" }) {
                assertEquals(rds1.toRouteDirection().uuid, it.targetUUID)
                assertEquals("vehicleId10", it.vehicleId)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId11" }) {
                assertEquals(rds1.toRouteDirection().uuid, it.targetUUID)
                assertEquals("vehicleId11", it.vehicleId)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId12" }) {
                assertEquals(rds1.toRouteDirection().uuid, it.targetUUID)
                assertEquals("vehicleId12", it.vehicleId)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId13" }) {
                assertEquals(rds1.route.uuid, it.targetUUID)
                assertEquals("vehicleId13", it.vehicleId)
            }
        }
    }
}
