package org.mtransit.android.commons.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RouteShortNameComparatorTest {

    private val subject: RouteShortNameComparator = Route.SHORT_NAME_COMPARATOR

    @Test
    fun test_compare() {
        val routes = listOf(
            makeRoute(shortName = "300"),
            makeRoute(shortName = "30"),
            makeRoute(shortName = "25"),
            makeRoute(shortName = "200"),
        ).shuffled()

        val result = routes.sortedWith(subject)

        assertEquals("25", result[0].shortName)
        assertEquals("30", result[1].shortName)
        assertEquals("200", result[2].shortName)
        assertEquals("300", result[3].shortName)
    }

    @Test
    fun test_areDifferent() {
        subject.areDifferent(makeRoute("authorityA", 1, "#1"), makeRoute("authorityB", 2, "#2")).let { result ->
            assertTrue(result)
        }
        subject.areDifferent(makeRoute("authorityA", 1, "#1"), makeRoute("authorityA", 2, "#2")).let { result ->
            assertTrue(result)
        }
        subject.areDifferent(makeRoute("authorityA", 1, "#1"), makeRoute("authorityB", 1, "#1")).let { result ->
            assertFalse(result)
        }
        subject.areDifferent(makeRoute("authorityA", 1, "#1"), makeRoute("authorityA", 1, "#1")).let { result ->
            assertFalse(result)
        }
    }

    @Test
    fun test_areComparable() {
        subject.areComparable(makeRoute(shortName = "123"), makeRoute(shortName = "456")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(makeRoute(shortName = "123"), makeRoute(shortName = "123")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(makeRoute(shortName = ""), makeRoute(shortName = "456")).let { result ->
            assertFalse(result)
        }
        subject.areComparable(makeRoute(shortName = "123"), makeRoute(shortName = "")).let { result ->
            assertFalse(result)
        }
        subject.areComparable(makeRoute(shortName = ""), makeRoute(shortName = "")).let { result ->
            assertFalse(result)
        }
    }
}
