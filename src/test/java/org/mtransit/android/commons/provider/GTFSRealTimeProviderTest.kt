package org.mtransit.android.commons.provider

import com.google.transit.realtime.GtfsRealtime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mtransit.android.commons.GtfsRealtimeExt.isActive
import org.mtransit.android.commons.TimeUtils
import org.mtransit.commons.msToSec

class GTFSRealTimeProviderTest {

    @Test
    fun testTargetUUIDsAreDistinct() {
        val agencyTag = "1"
        val routeTag = "1"
        val stopTag = "1"
        val routeType = 1

        val rtsTargetUUIDs = listOf(
            GTFSRealTimeProvider.getAgencyTargetUUID(agencyTag),
            GTFSRealTimeProvider.getAgencyRouteTypeTargetUUID(agencyTag, routeType),
            GTFSRealTimeProvider.getAgencyRouteStopTagTargetUUID(agencyTag, routeTag, stopTag),
            GTFSRealTimeProvider.getAgencyRouteTagTargetUUID(agencyTag, routeTag),
            GTFSRealTimeProvider.getAgencyStopTagTargetUUID(agencyTag, stopTag),
        )

        assertEquals(
            rtsTargetUUIDs.size,
            rtsTargetUUIDs.distinct().size
        )
    }

    @Test
    fun testIsInActivePeriod_InRange() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = GtfsRealtime.Alert.newBuilder()
            .addActivePeriod(mock<GtfsRealtime.TimeRange>().apply {
                `when`(hasStart()).thenReturn(true)
                `when`(start).thenReturn((nowInMs - 1000L).msToSec())
                `when`(hasEnd()).thenReturn(true)
                `when`(end).thenReturn((nowInMs + 1000L).msToSec())
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
                `when`(hasStart()).thenReturn(true)
                `when`(start).thenReturn((nowInMs - 1000L).msToSec())
                `when`(hasEnd()).thenReturn(false)
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
                `when`(hasStart()).thenReturn(false)
                `when`(hasEnd()).thenReturn(true)
                `when`(end).thenReturn((nowInMs + 1000L).msToSec())
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
                `when`(hasStart()).thenReturn(true)
                `when`(start).thenReturn((nowInMs - 2000L).msToSec())
                `when`(hasEnd()).thenReturn(true)
                `when`(end).thenReturn((nowInMs - 1000L).msToSec())
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
                `when`(hasStart()).thenReturn(true)
                `when`(start).thenReturn((nowInMs + 1000L).msToSec())
                `when`(hasEnd()).thenReturn(true)
                `when`(end).thenReturn((nowInMs + 2000L).msToSec())
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