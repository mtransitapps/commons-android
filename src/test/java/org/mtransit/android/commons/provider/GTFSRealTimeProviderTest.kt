package org.mtransit.android.commons.provider

import com.google.transit.realtime.alert
import com.google.transit.realtime.timeRange
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.isActive
import org.mtransit.commons.msToSec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GTFSRealTimeProviderTest {

    @Test
    fun testTargetUUIDsAreDistinct() {
        val agencyTag = "1"
        val routeTag = "1"
        val directionTag = 1
        val stopTag = "1"
        val routeType = 1

        val rdsProviderTargetUUIDs = listOf(
            GTFSRealTimeProvider.getAgencyTagTargetUUID(agencyTag),
            GTFSRealTimeProvider.getAgencyRouteTypeTagTargetUUID(agencyTag, routeType),
            GTFSRealTimeProvider.getAgencyRouteDirectionTagTargetUUID(agencyTag, routeTag, directionTag),
            GTFSRealTimeProvider.getAgencyRouteDirectionStopTagTargetUUID(agencyTag, routeTag, directionTag, stopTag),
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
    fun test_isInActive_ActivePeriod_InRange() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            @Suppress("DEPRECATION")
            activePeriod.add(
                timeRange {
                    start = (nowInMs - 1000L).msToSec()
                    end = (nowInMs + 1000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertTrue(result)
    }

    @Test
    fun test_isInActive_ActivePeriod_InRange_StartOnly() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            @Suppress("DEPRECATION")
            activePeriod.add(
                timeRange {
                    start = (nowInMs - 1000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertTrue(result)
    }

    @Test
    fun test_isInActive_ActivePeriod_InRange_EndOnly() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            @Suppress("DEPRECATION")
            activePeriod.add(
                timeRange {
                    end = (nowInMs + 1000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertTrue(result)
    }

    @Test
    fun test_isInActive_ActivePeriod_OutRange_Before() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            @Suppress("DEPRECATION")
            activePeriod.add(
                timeRange {
                    start = (nowInMs - 2000L).msToSec()
                    end = (nowInMs - 1000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertFalse(result)
    }

    @Test
    fun test_isInActive_ActivePeriod_OutRange_After() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            @Suppress("DEPRECATION")
            activePeriod.add(
                timeRange {
                    start = (nowInMs + 1000L).msToSec()
                    end = (nowInMs + 2000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertFalse(result)
    }

    // https://gtfs.org/documentation/realtime/feed-entities/service-alerts/#timerange
    @Test
    fun test_isInActive_ActivePeriod_0_Range() {
        val gAlert = alert {
            // no active period
        }

        val result = gAlert.isActive()

        assertTrue(result)
    }

    @Test
    fun test_isInActive_CommunicationPeriod_InRange() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            communicationPeriod.add(
                timeRange {
                    start = (nowInMs - 1000L).msToSec()
                    end = (nowInMs + 1000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertTrue(result)
    }

    @Test
    fun test_isInActive_CommunicationPeriod_InRange_StartOnly() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            communicationPeriod.add(
                timeRange {
                    start = (nowInMs - 1000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertTrue(result)
    }

    @Test
    fun test_isInActive_CommunicationPeriod_InRange_EndOnly() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            communicationPeriod.add(
                timeRange {
                    end = (nowInMs + 1000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertTrue(result)
    }

    @Test
    fun test_isInActive_CommunicationPeriod_OutRange_Before() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            communicationPeriod.add(
                timeRange {
                    start = (nowInMs - 2000L).msToSec()
                    end = (nowInMs - 1000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertFalse(result)
    }

    @Test
    fun test_isInActive_CommunicationPeriod_OutRange_After() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            communicationPeriod.add(
                timeRange {
                    start = (nowInMs + 1000L).msToSec()
                    end = (nowInMs + 2000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertFalse(result)
    }

    // https://gtfs.org/documentation/realtime/feed-entities/service-alerts/#timerange
    @Test
    fun test_isInActive_CommunicationPeriod_0_Range() {
        val gAlert = alert {
            // no communication period
        }

        val result = gAlert.isActive()

        assertTrue(result)
    }

    @Test
    fun test_isInActive_ImpactPeriod_OutRange_NoCommunicationPeriod() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            // no communication period provided == active (visible to user)
            impactPeriod.add(
                timeRange {
                    start = (nowInMs + 1000L).msToSec()
                    end = (nowInMs + 2000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertTrue(result)
    }

    @Test
    fun test_isInActive_ImpactPeriod_OutRange_After_CommunicationPeriodBefore() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            communicationPeriod.add(
                timeRange {
                    start = (nowInMs - 2000L).msToSec()
                    end = (nowInMs - 1000L).msToSec()
                }
            )
            impactPeriod.add(
                timeRange {
                    start = (nowInMs + 1000L).msToSec()
                    end = (nowInMs + 2000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertFalse(result)
    }

    /**
     * If `communication_period` is specified, every time interval in `impact_period` must be fully contained within at least one time interval of `communication_period`.
     */
    @Test
    fun test_isInActive_ImpactPeriod_InRange_CommunicationPeriodBefore() {
        val nowInMs = TimeUtils.currentTimeMillis()
        val gAlert = alert {
            communicationPeriod.add(
                timeRange {
                    start = (nowInMs - 2000L).msToSec()
                    end = (nowInMs - 1000L).msToSec()
                }
            )
            impactPeriod.add( // should not happen, but just in case
                timeRange {
                    start = (nowInMs - 1000L).msToSec()
                    end = (nowInMs + 1000L).msToSec()
                }
            )
        }

        val result = gAlert.isActive(nowInMs)

        assertTrue(result)
    }
}
