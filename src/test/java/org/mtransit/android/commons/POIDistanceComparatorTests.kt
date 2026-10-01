package org.mtransit.android.commons

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mtransit.android.commons.LocationUtils.LocationPOI
import org.mtransit.android.commons.LocationUtils.POI_DISTANCE_COMPARATOR
import org.mtransit.android.commons.LocationUtils.SimpleLocationPOI
import org.mtransit.android.commons.data.makeRDS
import kotlin.test.Test
import kotlin.test.assertEquals

class POIDistanceComparatorTests {

    // @NonNull
    // 	private LocationPOI makeLocationPOI(int intTag, float distance,
    // 										@Nullable Long rdsRouteTag, @Nullable Long rdsDirectionTag, @Nullable Integer rdsStopTag) {

    @org.junit.Test
    fun testPOIDistanceComparator() {
        val poiList = buildList<LocationPOI> {
            add(SimpleLocationPOI(mock { on { authority } doReturn "authority1" }).apply { distance = 3f })
            add(SimpleLocationPOI(mock { on { authority } doReturn "authority2" }).apply { distance = 1f })
            add(SimpleLocationPOI(mock { on { authority } doReturn "authority3" }).apply { distance = 20_000f })
            add(SimpleLocationPOI(mock { on { authority } doReturn "authority4" }).apply { distance = .5f })
        }

        val result = poiList.sortedWith(POI_DISTANCE_COMPARATOR)

        assertEquals(4, result.size)
        assertEquals("authority4", result[0].poi.authority)
        assertEquals("authority2", result[1].poi.authority)
        assertEquals("authority1", result[2].poi.authority)
        assertEquals("authority3", result[3].poi.authority)
    }

    @Test
    fun testPOIDistanceComparatorRDS_SameStop_DistinctRoute() {
        val poiList = buildList<LocationPOI> {
            add(SimpleLocationPOI(mock { on { authority } doReturn "authority1" }).apply { distance = 3f })
            add(SimpleLocationPOI(makeRDS("authority2", routeId = 2L, originalDirectionId = 0, stopId = 100)).apply { distance = 1f })
            add(SimpleLocationPOI(makeRDS("authority3", routeId = 1L, originalDirectionId = 0, stopId = 100)).apply { distance = 1f })
            add(SimpleLocationPOI(mock { on { authority } doReturn "authority4" }).apply { distance = .5f })
        }

        val result = poiList.sortedWith(POI_DISTANCE_COMPARATOR)

        assertEquals(4, result.size)
        assertEquals("authority4", result[0].poi.authority)
        assertEquals("authority3", result[1].poi.authority)
        assertEquals("authority2", result[2].poi.authority)
        assertEquals("authority1", result[3].poi.authority)
    }

    @org.junit.Test
    fun testPOIDistanceComparatorRDS_SameStop_DistinctTrip() {
        val poiList = buildList<LocationPOI> {
            add(SimpleLocationPOI(mock { on { authority } doReturn "authority1" }).apply { distance = 3f })
            add(SimpleLocationPOI(makeRDS("authority2", routeId = 1L, originalDirectionId = 1, stopId = 100)).apply { distance = 1f })
            add(SimpleLocationPOI(makeRDS("authority3", routeId = 1L, originalDirectionId = 0, stopId = 100)).apply { distance = 1f })
            add(SimpleLocationPOI(mock { on { authority } doReturn "authority4" }).apply { distance = .5f })
        }

        val result = poiList.sortedWith(POI_DISTANCE_COMPARATOR)

        assertEquals(4, result.size)
        assertEquals("authority4", result[0].poi.authority)
        assertEquals("authority3", result[1].poi.authority)
        assertEquals("authority2", result[2].poi.authority)
        assertEquals("authority1", result[3].poi.authority)
    }

    @Test
    fun test_POIDistanceComparator() {
        val poiList = buildList<LocationPOI> {
            add(SimpleLocationPOI(makeRDS(routeId = 10L, stopId = 100)).apply { distance = 100f })
            add(SimpleLocationPOI(makeRDS(routeId = 20L, stopId = 100)).apply { distance = 100f })
            add(SimpleLocationPOI(makeRDS(authority = "diffAuthority", routeId = 30L, stopId = 100)).apply { distance = 300f }) // != agency, == stop
            add(SimpleLocationPOI(makeRDS(authority = "0LocAuthority", routeId = 99L, stopId = 999)).apply { distance = -1f }) // no distance
        }.shuffled()

        val result = poiList.sortedWith(POI_DISTANCE_COMPARATOR)

        assertEquals(4, poiList.size)
        assertEquals("authority-10-1001-100", result[0].getPOI().uuid)
        assertEquals("authority-20-2001-100", result[1].getPOI().uuid)
        assertEquals("diffAuthority-30-3001-100", result[2].getPOI().uuid)
        assertEquals("0LocAuthority-99-9901-999", result[3].getPOI().uuid)
    }
}
