package org.mtransit.android.commons.provider

import com.google.transit.realtime.GtfsRealtime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mtransit.android.commons.TimeUtils

class GTFSRealTimeProviderTest {

    @Test
    fun testIsInActivePeriod_InRange() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = GtfsRealtime.Alert.newBuilder()
            .addActivePeriod(mock<GtfsRealtime.TimeRange>().apply {
                `when`(start).thenReturn(nowInMs - 1000L)
                `when`(end).thenReturn(nowInMs + 1000L)
            })
            .buildPartial()

        val result = GTFSRealTimeProvider.isInActivePeriod(gAlert)

        assertTrue(result)
    }

    @Test
    fun testIsInActivePeriod_OutRange_Before() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = GtfsRealtime.Alert.newBuilder()
            .addActivePeriod(mock<GtfsRealtime.TimeRange>().apply {
                `when`(hasStart()).thenReturn(true)
                `when`(start).thenReturn(nowInMs - 2000L)
                `when`(hasEnd()).thenReturn(true)
                `when`(end).thenReturn(nowInMs - 1000L)
            })
            .buildPartial()

        val result = GTFSRealTimeProvider.isInActivePeriod(gAlert)

        assertFalse(result)
    }

    @Test
    fun testIsInActivePeriod_OutRange_After() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = GtfsRealtime.Alert.newBuilder()
            .addActivePeriod(mock<GtfsRealtime.TimeRange>().apply {
                `when`(hasStart()).thenReturn(true)
                `when`(start).thenReturn(nowInMs + 1000L)
                `when`(hasEnd()).thenReturn(true)
                `when`(end).thenReturn(nowInMs + 2000L)
            })
            .buildPartial()

        val result = GTFSRealTimeProvider.isInActivePeriod(gAlert)

        assertFalse(result)
    }

    // https://gtfs.org/realtime/feed-entities/service-alerts/#timerange
    @Test
    fun testIsInActivePeriod_0_Range() {
        val gAlert = GtfsRealtime.Alert.newBuilder()
            .addActivePeriod(mock<GtfsRealtime.TimeRange>().apply {
                `when`(hasStart()).thenReturn(false)
                `when`(hasEnd()).thenReturn(false)
            })
            .buildPartial()

        val result = GTFSRealTimeProvider.isInActivePeriod(gAlert)

        assertTrue(result)
    }
}