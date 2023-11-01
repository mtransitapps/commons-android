package org.mtransit.android.commons.provider

import org.junit.Assert
import org.junit.Test

class OCTranspoProviderTestKt {

    @Test
    fun testTargetUUIDsAreDistinct() {
        val agencyTag = "1"
        val routeTag = "1"

        val rtsTargetUUIDs = listOf(
            OCTranspoProvider.getAgencyTargetUUID(agencyTag),
            OCTranspoProvider.getAgencyRouteShortNameTargetUUID(agencyTag, routeTag),
        )

        Assert.assertEquals(
            rtsTargetUUIDs.size,
            rtsTargetUUIDs.distinct().size
        )
    }
}