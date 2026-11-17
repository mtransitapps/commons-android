package org.mtransit.android.commons.data

import org.mtransit.commons.CommonsApp
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class POIAlphaComparatorTest {

    private val subject: Comparator<POI> = POI_ALPHA_COMPARATOR

    @BeforeTest
    fun setUp() {
        CommonsApp.setup(isAndroidPlatform = false)
    }

    @Test
    fun test_compare_empty_direction_head_sign() {
        val poiList = buildList {
            add(makeRDS("authority1", direction = makeDirection(headsign = "B"), stopName = "Zebra"))
            add(makeRDS("authority2", direction = makeDirection(headsign = "C"), stopName = "Apple"))
            add(makeRDS("authority3", direction = makeDirection(headsign = ""), stopName = "Museum"))
        }.shuffled()

        val result = poiList.sortedWith(subject)

        assertEquals(3, result.size)
        assertEquals("authority3", result[0].authority)
        assertEquals("authority1", result[1].authority)
        assertEquals("authority2", result[2].authority)
    }

    @Test
    fun test_compare_empty_route_short_name_transitive_no_cycle() {
        val sharedDirection = makeDirection(headsign = "North")
        val rdsA = makeRDS(
            authority = "authorityA",
            route = makeRoute("authorityA", id = 1L, shortName = "1"),
            direction = sharedDirection,
            stopName = "Zulu",
        )
        val rdsB = makeRDS(
            authority = "authorityB",
            route = makeRoute("authorityB", id = 1L, shortName = "2"),
            direction = sharedDirection,
            stopName = "Apple",
        )
        val rdsC = makeRDS(
            authority = "authorityC",
            route = makeRoute("authorityC", id = 1L, shortName = ""),
            direction = sharedDirection,
            stopName = "Museum",
        )

        // Empty route short name sorts before non-empty short names consistently:
        // C < A < B
        val cCompareA = subject.compare(rdsC, rdsA)
        val aCompareB = subject.compare(rdsA, rdsB)
        val cCompareB = subject.compare(rdsC, rdsB)

        assertEquals(-1, cCompareA.coerceIn(-1, 1))
        assertEquals(-1, aCompareB.coerceIn(-1, 1))
        assertEquals(-1, cCompareB.coerceIn(-1, 1))

        val poiList = listOf(rdsA, rdsB, rdsC).shuffled()
        val result = poiList.sortedWith(subject)

        assertEquals(3, result.size)
        assertEquals("authorityC", result[0].authority)
        assertEquals("authorityA", result[1].authority)
        assertEquals("authorityB", result[2].authority)
    }

    @Test
    fun test_compare_mixed_POIs_RDS_RDS_Bike() {
        // route 1 at stop “Zebra” precedes route 2 at “Apple” by route number, “Apple” precedes bike station “Museum” by name
        val poiList = buildList {
            add(makeRDS("authority2", routeId = 2, stopName = "Apple"))
            add(makeRDS("authority1", routeId = 1, stopName = "Zebra"))
            add(makeBikeStation("authority3", name = "Museum"))
        } // no shuffled, keep order

        val result = poiList.sortedWith(subject)

        assertEquals(3, result.size)
        assertEquals("authority1", result[0].authority)
        assertEquals("authority2", result[1].authority)
        assertEquals("authority3", result[2].authority)
    }

    @Test
    fun test_compare_mixed_POIs_RDS_Bike_RDS() {
        // route 1 at stop “Zebra” precedes route 2 at “Apple” by route number, “Apple” precedes bike station “Museum” by name
        val poiList = buildList {
            add(makeRDS("authority2", routeId = 2, stopName = "Apple"))
            add(makeBikeStation("authority3", name = "Museum"))
            add(makeRDS("authority1", routeId = 1, stopName = "Zebra"))
        } // no shuffled, keep order

        val result = poiList.sortedWith(subject)

        assertEquals(3, result.size)
        assertEquals("authority1", result[0].authority)
        assertEquals("authority2", result[1].authority)
        assertEquals("authority3", result[2].authority)
    }

    @Test
    fun test_compare_mixed_POIs_Bike_RDS_RDS() {
        // route 1 at stop “Zebra” precedes route 2 at “Apple” by route number, “Apple” precedes bike station “Museum” by name
        val poiList = buildList {
            add(makeBikeStation("authority3", name = "Museum"))
            add(makeRDS("authority2", routeId = 2, stopName = "Apple"))
            add(makeRDS("authority1", routeId = 1, stopName = "Zebra"))
        } // no shuffled, keep order

        val result = poiList.sortedWith(subject)

        assertEquals(3, result.size)
        assertEquals("authority1", result[0].authority)
        assertEquals("authority2", result[1].authority)
        assertEquals("authority3", result[2].authority)
    }
}
