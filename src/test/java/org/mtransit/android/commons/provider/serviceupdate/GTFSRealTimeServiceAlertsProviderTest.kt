package org.mtransit.android.commons.provider.serviceupdate

import com.google.transit.realtime.entitySelector
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.data.clone
import org.mtransit.android.commons.data.getGTFSRTTargetUUID
import org.mtransit.android.commons.data.makeRDS
import org.mtransit.android.commons.data.makeServiceUpdate
import org.mtransit.android.commons.data.toRouteDirection
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTypeTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyTagTargetUUID
import org.mtransit.android.commons.provider.serviceupdate.GTFSRealTimeServiceAlertsProvider.getCached
import org.mtransit.android.commons.provider.serviceupdate.GTFSRealTimeServiceAlertsProvider.parseProviderTargetUUID
import org.mtransit.android.commons.provider.setupProviderForRDS
import org.mtransit.android.commons.provider.stringIdToHash
import org.mtransit.commons.CommonsApp
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

class GTFSRealTimeServiceAlertsProviderTest {

    private val gtfsRealTimeProvider: GTFSRealTimeProvider = mock {
        on { getAgencyTag(anyOrNull()) } doReturn "static_agency_id"
    }

    @BeforeTest
    fun setUp() {
        CommonsApp.setup(false)
    }

    @Test
    fun test_getCached() { // ignoreDirection handled during cache creation
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
        val cachedServiceUpdates = buildList {
            add(makeServiceUpdate(targetUUID = rds1.getGTFSRTTargetUUID(true), targetTripId = "tripId10", text = "Text 10"))
            add(makeServiceUpdate(targetUUID = rds1.getGTFSRTTargetUUID(true), targetTripId = "tripId11", text = "Text 11"))
            add(makeServiceUpdate(targetUUID = rds1.toRouteDirection().getGTFSRTTargetUUID(), targetTripId = "tripId12", text = "Text 12"))
            add(makeServiceUpdate(targetUUID = rds1.route.getGTFSRTTargetUUID(), targetTripId = "tripId13", text = "Text 13"))
            add(makeServiceUpdate(targetUUID = getAgencyTagTargetUUID("static_agency_id"), targetTripId = null, text = "Text 00"))
        }
        val staticTripIds = (cachedServiceUpdates.mapNotNull { it.targetTripId } + "tripId22").toSet()
        val getCachedServiceUpdates: (targetUUIDs: Collection<String>, tripIds: List<String>?) -> List<ServiceUpdate>? = { targetUUIDs, tripIds ->
            cachedServiceUpdates.filter { serviceUpdate ->
                targetUUIDs.contains(serviceUpdate.targetUUID)
                        && serviceUpdate.targetTripId?.let { tripIds?.contains(it) } != false // ignore if target tripID or local trip ID null
            }.map { it.clone() }
        }
        gtfsRealTimeProvider.getCached(
            filter = ServiceUpdateProviderContract.Filter(rds1),
            getCachedServiceUpdates = getCachedServiceUpdates,
            getTripIds = { _, _, _ -> listOf("tripId10") },
            tripIdsOutOfSync = !staticTripIds.contains("tripId10"),
        ).let { result ->
            assertNotNull(result)
            assertEquals(2, result.size)
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId10" }) {
                assertEquals(rds1.uuid, it.targetUUID)
                assertEquals("Text 10", it.text)
            }
            assertNotNull(result.singleOrNull { it.targetUUID == "static_agency_id" }) {
                assertNull(it.targetTripId)
                assertEquals("Text 00", it.text)
            }
        }
        gtfsRealTimeProvider.getCached(
            filter = ServiceUpdateProviderContract.Filter(rds1),
            getCachedServiceUpdates = getCachedServiceUpdates,
            getTripIds = { _, _, _ -> listOf("tripId12") },
            tripIdsOutOfSync = !staticTripIds.contains("tripId12"),
        ).let { result ->
            assertNotNull(result)
            assertEquals(2, result.size)
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId12" }) {
                assertEquals(rds1.toRouteDirection().uuid, it.targetUUID)
                assertEquals("Text 12", it.text)
            }
            assertNotNull(result.singleOrNull { it.targetUUID == "static_agency_id" }) {
                assertNull(it.targetTripId)
                assertEquals("Text 00", it.text)
            }
        }
        gtfsRealTimeProvider.getCached(
            filter = ServiceUpdateProviderContract.Filter(rds1),
            getCachedServiceUpdates = getCachedServiceUpdates,
            getTripIds = { _, _, _ -> fail("should not call since out of sync for tripId177777") },
            tripIdsOutOfSync = !staticTripIds.contains("tripId177777"),
        ).let { result ->
            assertNotNull(result)
            assertEquals(4, result.size)
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId10" }) {
                assertEquals(rds1.uuid, it.targetUUID)
                assertEquals("Text 10", it.text)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId11" }) {
                assertEquals(rds1.uuid, it.targetUUID)
                assertEquals("Text 11", it.text)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId12" }) {
                assertEquals(rds1.toRouteDirection().uuid, it.targetUUID)
                assertEquals("Text 12", it.text)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId13" }) {
                assertEquals(rds1.route.uuid, it.targetUUID)
                assertEquals("Text 13", it.text)
            }
        }
        gtfsRealTimeProvider.getCached(
            filter = ServiceUpdateProviderContract.Filter(rds2),
            getCachedServiceUpdates = getCachedServiceUpdates,
            getTripIds = { _, _, _ -> listOf("tripId22") },
            tripIdsOutOfSync = !staticTripIds.contains("tripId22"),
        ).let { result ->
            assertNotNull(result)
            assertEquals(1, result.size)
            assertNotNull(result.singleOrNull { it.targetUUID == "static_agency_id" }) {
                assertNull(it.targetTripId)
                assertEquals("Text 00", it.text)
            }
        }
    }

    @Test
    fun test_parseProviderTargetUUID() {
        // EMPTY
        gtfsRealTimeProvider.parseProviderTargetUUID(
            gEntitySelector = entitySelector {},
            ignoreDirection = false
        ).let { result ->
            assertNull(result)
        }
        // AGENCY only
        gtfsRealTimeProvider.parseProviderTargetUUID(
            gEntitySelector = entitySelector {
                agencyId = "agency_id"
            },
            ignoreDirection = false
        ).let { result ->
            assertEquals(getAgencyTagTargetUUID("static_agency_id"), result)
        }
        // ROUTE
        gtfsRealTimeProvider.parseProviderTargetUUID(
            gEntitySelector = entitySelector {
                routeId = "route_id"
            },
            ignoreDirection = false
        ).let { result ->
            assertEquals(getAgencyRouteTagTargetUUID("static_agency_id", stringIdToHash("route_id")), result)
        }
        // ROUTE + DIRECTION
        gtfsRealTimeProvider.parseProviderTargetUUID(
            gEntitySelector = entitySelector {
                routeId = "route_id"
                directionId = 0
            },
            ignoreDirection = false
        ).let { result ->
            assertEquals(getAgencyRouteDirectionTagTargetUUID("static_agency_id", stringIdToHash("route_id"), 0), result)
        }
        gtfsRealTimeProvider.parseProviderTargetUUID(
            gEntitySelector = entitySelector {
                routeId = "route_id"
                directionId = 777777777 // INVALID!
            },
            ignoreDirection = false
        ).let { result ->
            assertEquals(getAgencyRouteTagTargetUUID("static_agency_id", stringIdToHash("route_id")), result)
        }
        gtfsRealTimeProvider.parseProviderTargetUUID(
            gEntitySelector = entitySelector {
                routeId = "route_id"
                directionId = 0
            },
            ignoreDirection = true
        ).let { result ->
            assertEquals(getAgencyRouteTagTargetUUID("static_agency_id", stringIdToHash("route_id")), result)
        }
        // ROUTE + DIRECTION + STOP
        gtfsRealTimeProvider.parseProviderTargetUUID(
            gEntitySelector = entitySelector {
                routeId = "route_id"
                directionId = 0
                stopId = "stop_id"
            },
            ignoreDirection = false
        ).let { result ->
            assertEquals(getAgencyRouteDirectionStopTagTargetUUID("static_agency_id", stringIdToHash("route_id"), 0, stringIdToHash("stop_id")), result)
        }
        gtfsRealTimeProvider.parseProviderTargetUUID(
            gEntitySelector = entitySelector {
                routeId = "route_id"
                directionId = 0
                stopId = "stop_id"
            },
            ignoreDirection = true
        ).let { result ->
            assertEquals(getAgencyRouteStopTagTargetUUID("static_agency_id", stringIdToHash("route_id"), stringIdToHash("stop_id")), result)
        }
        // ROUTE + STOP
        gtfsRealTimeProvider.parseProviderTargetUUID(
            gEntitySelector = entitySelector {
                routeId = "route_id"
                stopId = "stop_id"
            },
            ignoreDirection = false
        ).let { result ->
            assertEquals(getAgencyRouteStopTagTargetUUID("static_agency_id", stringIdToHash("route_id"), stringIdToHash("stop_id")), result)
        }
        // STOP only
        gtfsRealTimeProvider.parseProviderTargetUUID(
            gEntitySelector = entitySelector {
                stopId = "stop_id"
            },
            ignoreDirection = false
        ).let { result ->
            assertEquals(getAgencyStopTagTargetUUID("static_agency_id", stringIdToHash("stop_id")), result)
        }
        // ROUTE TYPE
        gtfsRealTimeProvider.parseProviderTargetUUID(
            gEntitySelector = entitySelector {
                routeType = 3 // bus
            },
            ignoreDirection = false
        ).let { result ->
            assertEquals(getAgencyRouteTypeTagTargetUUID("static_agency_id", 3), result)
        }
    }
}
