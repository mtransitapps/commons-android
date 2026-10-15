package org.mtransit.android.commons.data

import org.mtransit.commons.CommonsApp
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LocationPOIDistanceComparatorTests {

    private val subject: Comparator<LocationPOI> = LocationPOI.DISTANCE_COMPARATOR

    @BeforeTest
    fun setUp() {
        CommonsApp.setup(isAndroidPlatform = false)
    }

    @Test
    fun test_compare_distinct_authority_distance() {
        val poiList = buildList<LocationPOI> {
            add(SimpleLocationPOI(makeDefaultPOI("authority1")).apply { distance = 3f })
            add(SimpleLocationPOI(makeDefaultPOI("authority2")).apply { distance = 1f })
            add(SimpleLocationPOI(makeDefaultPOI("authority3")).apply { distance = 20_000f })
            add(SimpleLocationPOI(makeDefaultPOI("authority4")).apply { distance = .5f })
        }

        val result = poiList.sortedWith(subject)

        assertEquals(4, result.size)
        assertEquals("authority4", result[0].poi.authority)
        assertEquals("authority2", result[1].poi.authority)
        assertEquals("authority1", result[2].poi.authority)
        assertEquals("authority3", result[3].poi.authority)
    }

    @Test
    fun test_POIDistanceComparatorRDS_SameStop_DistinctRoute_compare_equal() {
        val poiList = buildList<LocationPOI> {
            add(SimpleLocationPOI(makeDefaultPOI("authority1")).apply { distance = 3f })
            add(SimpleLocationPOI(makeRDS("authority2", routeId = 2L, originalDirectionId = 0, stopId = 100)).apply { distance = 1f })
            add(SimpleLocationPOI(makeRDS("authority3", routeId = 1L, originalDirectionId = 0, stopId = 100)).apply { distance = 1f })
            add(SimpleLocationPOI(makeDefaultPOI("authority4")).apply { distance = .5f })
        }

        val result = poiList.sortedWith(subject.then(LocationPOI.ALPHA_COMPARATOR))

        assertEquals(4, result.size)
        assertEquals("authority4", result[0].poi.authority)
        assertEquals("authority3", result[1].poi.authority)
        assertEquals("authority2", result[2].poi.authority)
        assertEquals("authority1", result[3].poi.authority)
    }

    @Test
    fun test_POIDistanceComparatorRDS_SameStop_DistinctTrip_compare_equal() {
        val poiList = buildList<LocationPOI> {
            add(SimpleLocationPOI(makeBikeStation("authority1", id = 100)).apply { distance = 3f })
            add(SimpleLocationPOI(makeRDS("authority2", routeId = 1L, originalDirectionId = 1, stopId = 100)).apply { distance = 1f })
            add(SimpleLocationPOI(makeRDS("authority3", routeId = 1L, originalDirectionId = 0, stopId = 100)).apply { distance = 1f })
            add(SimpleLocationPOI(makeBikeStation("authority4", id = 400)).apply { distance = .5f })
        }

        val result = poiList.sortedWith(subject.then(LocationPOI.ALPHA_COMPARATOR))

        assertEquals(4, result.size)
        assertEquals("authority4", result[0].poi.authority)
        assertEquals("authority3", result[1].poi.authority)
        assertEquals("authority2", result[2].poi.authority)
        assertEquals("authority1", result[3].poi.authority)
    }

    @Test
    fun test_compare_RDS() {
        val poiList = buildList<LocationPOI> {
            add(SimpleLocationPOI(makeRDS(routeId = 10L, stopId = 100)).apply { distance = 100f })
            add(SimpleLocationPOI(makeRDS(routeId = 20L, stopId = 100)).apply { distance = 100f })
            add(SimpleLocationPOI(makeRDS(authority = "diffAuthority", routeId = 30L, stopId = 100)).apply { distance = 300f }) // != agency, == stop
            add(SimpleLocationPOI(makeRDS(authority = "0LocAuthority", routeId = 99L, stopId = 999)).apply { distance = -1f }) // no distance
        }

        val result = poiList.sortedWith(subject)

        assertEquals(4, result.size)
        assertEquals("authority-10-1001-100", result[0].poi.uuid)
        assertEquals("authority-20-2001-100", result[1].poi.uuid)
        assertEquals("diffAuthority-30-3001-100", result[2].poi.uuid)
        assertEquals("0LocAuthority-99-9901-999", result[3].poi.uuid)
    }

    @Test
    fun test_compare_same_distance_RDS_and_Bike() {
        val poiList = buildList<LocationPOI> {
            add(SimpleLocationPOI(makeBikeStation("authorityFarAway", id = 1, name = "Bike Station A")).apply { distance = 1_000f }) // farthest
            add(SimpleLocationPOI(makeRDS(routeId = 10L, stopId = 100, stopName = "Bus Stop 100")).apply { distance = 100f })
            add(SimpleLocationPOI(makeBikeStation("authority1", id = 2, name = "Bike Station B")).apply { distance = 100f })
            add(SimpleLocationPOI(makeRDS(routeId = 20L, stopId = 200, stopName = "Bus Stop 200")).apply { distance = 100f })
            add(SimpleLocationPOI(makeBikeStation("authority2", id = 3, name = "Bike Station C")).apply { distance = 100f })
            add(SimpleLocationPOI(makeRDS(routeId = 99L, stopId = 999, stopName = "Bus Stop 999")).apply { distance = 1f }) // closest
        }.shuffled()

        val result = poiList.sortedWith(subject.then(LocationPOI.ALPHA_COMPARATOR))

        assertEquals(6, result.size)
        var index = -1
        assertEquals("authority-99-9901-999", result[++index].poi.uuid) // R99 #999 Bus Stop 999 // closest
        assertEquals("authority-10-1001-100", result[++index].poi.uuid) // R10 #100 Bus Stop 100
        assertEquals("authority-20-2001-200", result[++index].poi.uuid) // R20 #200 Bus Stop 200
        assertEquals("authority1-2", result[++index].poi.uuid) // Bike Station B
        assertEquals("authority2-3", result[++index].poi.uuid) // Bike Station C
        assertEquals("authorityFarAway-1", result[++index].poi.uuid) // Bike Station A // farthest
        assertEquals(result.size, ++index)
    }
}
