package org.mtransit.android.commons.provider.serviceupdate

import com.google.transit.realtime.entitySelector
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.data.clone
import org.mtransit.android.commons.data.getGTFSRTTargetUUID
import org.mtransit.android.commons.data.makeRDS
import org.mtransit.android.commons.data.makeServiceUpdate
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTypeTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyTagTargetUUID
import org.mtransit.android.commons.provider.gtfs.getTargetUUIDs
import org.mtransit.android.commons.provider.serviceupdate.GTFSRealTimeServiceAlertsProvider.getCached
import org.mtransit.android.commons.provider.serviceupdate.GTFSRealTimeServiceAlertsProvider.parseProviderTargetUUID
import org.mtransit.commons.CommonsApp
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GTFSRealTimeServiceAlertsProviderTest {

    private val gtfsRealTimeProvider: GTFSRealTimeProvider = mock {
        on { getAgencyTag(anyOrNull()) } doReturn "static_agency_id"
    }

    @BeforeTest
    fun setUp() {
        CommonsApp.setup(false)
    }

    @Test
    fun test_getCached() {
        val rds1 = makeRDS(
            authority = "static_agency_id",
            routeId = 1L,
            originalDirectionId = 1,
            stopId = 10,
        )
        setupProviderForRDS(rds1)
        val rds2 = makeRDS(
            authority = "static_agency_id",
            routeId = 2L,
            originalDirectionId = 0,
            stopId = 20,
        )
        setupProviderForRDS(rds2)
        val cachedServiceUpdates = buildList {
            add(makeServiceUpdate(targetUUID = rds1.getGTFSRTTargetUUID(), targetTripId = "tripId10"))
            add(makeServiceUpdate(targetUUID = rds1.getGTFSRTTargetUUID(), targetTripId = "tripId11"))
        }
        val getCachedServiceUpdates: (targetUUIDs: Collection<String>, tripIds: List<String>?) -> List<ServiceUpdate>? = { targetUUIDs, tripIds ->
            cachedServiceUpdates.filter { targetUUIDs.contains(it.targetUUID) && tripIds?.contains(it.targetTripId) != false }.map { it.clone() }
        }
        gtfsRealTimeProvider.getCached(
            filter = ServiceUpdateProviderContract.Filter(rds1),
            targetUUIDs = rds1.getTargetUUIDs(gtfsRealTimeProvider, includeAgencyTag = true, includeRouteType = true, includeStopTags = true),
            tripIds = listOf("tripId10"),
            getCachedServiceUpdates = getCachedServiceUpdates,
            ignoreDirection = false
        ).let { result ->
            assertEquals(1, result.size)
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId10" }) {
                assertEquals(rds1.uuid, it.targetUUID)
            }
        }
        gtfsRealTimeProvider.getCached(
            filter = ServiceUpdateProviderContract.Filter(rds1),
            targetUUIDs = rds1.getTargetUUIDs(gtfsRealTimeProvider, includeAgencyTag = true, includeRouteType = true, includeStopTags = true),
            tripIds = listOf("tripId177777"), // out-of-sync (static!=real-time)
            getCachedServiceUpdates = getCachedServiceUpdates,
            ignoreDirection = false
        ).let { result ->
            assertEquals(2, result.size)
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId10" }) {
                assertEquals(rds1.uuid, it.targetUUID)
            }
            assertNotNull(result.singleOrNull { it.targetTripId == "tripId11" }) {
                assertEquals(rds1.uuid, it.targetUUID)
            }
        }
        gtfsRealTimeProvider.getCached(
            filter = ServiceUpdateProviderContract.Filter(rds1),
            targetUUIDs = rds1.getTargetUUIDs(gtfsRealTimeProvider, includeAgencyTag = true, includeRouteType = true, includeStopTags = true),
            tripIds = listOf("tripId177777"), // out-of-sync (static!=real-time)
            getCachedServiceUpdates = getCachedServiceUpdates,
            ignoreDirection = true
        ).let { result ->
            assertEquals(0, result.size)
        }
        gtfsRealTimeProvider.getCached(
            filter = ServiceUpdateProviderContract.Filter(rds2),
            targetUUIDs = rds2.getTargetUUIDs(gtfsRealTimeProvider, includeAgencyTag = true, includeRouteType = true, includeStopTags = true),
            tripIds = listOf("tripId22"),
            getCachedServiceUpdates = getCachedServiceUpdates,
            ignoreDirection = false
        ).let { result ->
            assertEquals(0, result.size)
        }
    }

    private fun setupProviderForRDS(rds: RouteDirectionStop) {
        whenever { gtfsRealTimeProvider.getRouteTag(eq(rds.route)) } doReturn rds.route.originalIdHash.toString()
        whenever { gtfsRealTimeProvider.getDirectionTag(eq(rds.direction)) } doReturn rds.direction.originalDirectionIdOrNull
        whenever { gtfsRealTimeProvider.getStopTag(eq(rds.stop)) } doReturn rds.stop.originalIdHashString
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

    private fun stringIdToHash(originalId: String) = originalId.hashCode().toString()

}
