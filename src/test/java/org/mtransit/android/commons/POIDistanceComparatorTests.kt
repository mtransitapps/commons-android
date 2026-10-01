package org.mtransit.android.commons

import org.mtransit.android.commons.LocationUtils.LocationPOI
import org.mtransit.android.commons.LocationUtils.POIDistanceComparator
import org.mtransit.android.commons.LocationUtils.SimpleLocationPOI
import org.mtransit.android.commons.data.makeRDS
import kotlin.test.Test
import kotlin.test.assertEquals

class POIDistanceComparatorTests {

    @Test
    fun test_POIDistanceComparator() {
        val poiList = buildList<LocationPOI> {
            add(SimpleLocationPOI(makeRDS(routeId = 10L, stopId = 100)).apply { distance = 100f })
            add(SimpleLocationPOI(makeRDS(routeId = 20L, stopId = 100)).apply { distance = 100f })
            add(SimpleLocationPOI(makeRDS(authority = "diffAuthority", routeId = 30L, stopId = 100)).apply { distance = 300f }) // != agency, == stop
            add(SimpleLocationPOI(makeRDS(authority = "0LocAuthority", routeId = 99L, stopId = 999)).apply { distance = -1f }) // no distance
        }.shuffled()

        val result = poiList.sortedWith(POIDistanceComparator())

        assertEquals("authority-10-1001-100", result[0].getPOI().uuid)
        assertEquals("authority-20-2001-100", result[1].getPOI().uuid)
        assertEquals("diffAuthority-30-3001-100", result[2].getPOI().uuid)
        assertEquals("0LocAuthority-99-9901-999", result[3].getPOI().uuid)
    }
}
