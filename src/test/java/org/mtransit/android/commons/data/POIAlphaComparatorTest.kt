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
    fun test_compare_mixed_POIs() {
        // route 1 at stop “Zebra” precedes route 2 at “Apple” by route number, “Apple” precedes bike station “Museum” by name
        val poiList = buildList {
            add(makeRDS("authority2", routeId = 2, stopName = "Apple"))
            add(makeRDS("authority1", routeId = 1, stopName = "Zebra"))
            add(makeBikeStation("authority3", name = "Museum"))
        } // no shuffled, keep order

        val result = poiList.sortedWith(subject)

        assertEquals(3, result.size)
        assertEquals("authority3", result[0].authority)
        assertEquals("authority1", result[1].authority)
        assertEquals("authority2", result[2].authority)
    }
}
