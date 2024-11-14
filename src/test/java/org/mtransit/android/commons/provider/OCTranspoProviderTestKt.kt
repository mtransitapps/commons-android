package org.mtransit.android.commons.provider

import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("DEPRECATION")
class OCTranspoProviderTestKt {

    @Test
    fun testTargetUUIDsAreDistinct() {
        val agencyTag = "1"
        val routeTag = "1"

        val rtsTargetUUIDs = listOf(
            OCTranspoProvider.getAgencyTargetUUID(agencyTag),
            OCTranspoProvider.getAgencyRouteShortNameTargetUUID(agencyTag, routeTag),
        )

        assertEquals(
            rtsTargetUUIDs.size,
            rtsTargetUUIDs.distinct().size
        )
    }
}