package org.mtransit.android.commons.provider.serviceupdate

import com.google.transit.realtime.entitySelector
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTypeTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyTagTargetUUID
import org.mtransit.android.commons.provider.serviceupdate.GTFSRealTimeServiceAlertsProvider.parseProviderTargetUUID
import org.mtransit.commons.CommonsApp
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
