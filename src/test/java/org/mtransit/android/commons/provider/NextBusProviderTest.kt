package org.mtransit.android.commons.provider

import org.junit.Assert
import org.junit.Test

class NextBusProviderTest {

    @Test
    fun testTargetUUIDsAreDistinct() {
        val agencyTag = "1"
        val routeTag = "1"
        val stopTag = "1"

        val rtsTargetUUIDs = listOf(
            NextBusProvider.getAgencyTargetUUID(agencyTag),
            NextBusProvider.getAgencyRouteStopTagTargetUUID(agencyTag, routeTag, stopTag),
            NextBusProvider.getAgencyRouteTagTargetUUID(agencyTag, routeTag),
        )

        Assert.assertEquals(
            rtsTargetUUIDs.size,
            rtsTargetUUIDs.distinct().size
        )
    }
}