package org.mtransit.android.commons.provider

import com.google.transit.realtime.GtfsRealtime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.isActive
import org.mtransit.commons.msToSec

class GTFSRealTimeProviderTest {

    @Test
    fun testTargetUUIDsAreDistinct() {
        val agencyTag = "1"
        val routeTag = "1"
        val stopTag = "1"
        val routeType = 1

        val rdsProviderTargetUUIDs = listOf(
            GTFSRealTimeProvider.getAgencyTargetUUID(agencyTag),
            GTFSRealTimeProvider.getAgencyRouteTypeTargetUUID(agencyTag, routeType),
            GTFSRealTimeProvider.getAgencyRouteStopTagTargetUUID(agencyTag, routeTag, stopTag),
            GTFSRealTimeProvider.getAgencyRouteTagTargetUUID(agencyTag, routeTag),
            GTFSRealTimeProvider.getAgencyStopTagTargetUUID(agencyTag, stopTag),
        )

        assertEquals(
            rdsProviderTargetUUIDs.size,
            rdsProviderTargetUUIDs.distinct().size
        )
    }

    @Test
    fun testIsInActivePeriod_InRange() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = GtfsRealtime.Alert.newBuilder()
            .addActivePeriod(mock<GtfsRealtime.TimeRange>().apply {
                whenever(hasStart()).thenReturn(true)
                whenever(start).thenReturn((nowInMs - 1000L).msToSec())
                whenever(hasEnd()).thenReturn(true)
                whenever(end).thenReturn((nowInMs + 1000L).msToSec())
            })
            .buildPartial()

        val result = gAlert.isActive(nowInMs)

        assertTrue(result)
    }

    @Test
    fun testIsInActivePeriod_InRange_StartOnly() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = GtfsRealtime.Alert.newBuilder()
            .addActivePeriod(mock<GtfsRealtime.TimeRange>().apply {
                whenever(hasStart()).thenReturn(true)
                whenever(start).thenReturn((nowInMs - 1000L).msToSec())
                whenever(hasEnd()).thenReturn(false)
            })
            .buildPartial()

        val result = gAlert.isActive(nowInMs)

        assertTrue(result)
    }

    @Test
    fun testIsInActivePeriod_InRange_EndOnly() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = GtfsRealtime.Alert.newBuilder()
            .addActivePeriod(mock<GtfsRealtime.TimeRange>().apply {
                whenever(hasStart()).thenReturn(false)
                whenever(hasEnd()).thenReturn(true)
                whenever(end).thenReturn((nowInMs + 1000L).msToSec())
            })
            .buildPartial()

        val result = gAlert.isActive(nowInMs)

        assertTrue(result)
    }

    @Test
    fun testIsInActivePeriod_OutRange_Before() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = GtfsRealtime.Alert.newBuilder()
            .addActivePeriod(mock<GtfsRealtime.TimeRange>().apply {
                whenever(hasStart()).thenReturn(true)
                whenever(start).thenReturn((nowInMs - 2000L).msToSec())
                whenever(hasEnd()).thenReturn(true)
                whenever(end).thenReturn((nowInMs - 1000L).msToSec())
            })
            .buildPartial()

        val result = gAlert.isActive(nowInMs)

        assertFalse(result)
    }

    @Test
    fun testIsInActivePeriod_OutRange_After() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = GtfsRealtime.Alert.newBuilder()
            .addActivePeriod(mock<GtfsRealtime.TimeRange>().apply {
                whenever(hasStart()).thenReturn(true)
                whenever(start).thenReturn((nowInMs + 1000L).msToSec())
                whenever(hasEnd()).thenReturn(true)
                whenever(end).thenReturn((nowInMs + 2000L).msToSec())
            })
            .buildPartial()

        val result = gAlert.isActive(nowInMs)

        assertFalse(result)
    }

    // https://gtfs.org/realtime/feed-entities/service-alerts/#timerange
    @Test
    fun testIsInActivePeriod_0_Range() {
        val gAlert = GtfsRealtime.Alert.newBuilder()
            // no active period
            .buildPartial()

        val result = gAlert.isActive()

        assertTrue(result)
    }
}