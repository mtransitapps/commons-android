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
    fun test_compare_largeNumbers() {
        val routes = listOf(
            makeRoute(shortName = "999999999999"),
            makeRoute(shortName = "1"),
            makeRoute(shortName = "2147483648"),
            makeRoute(shortName = "2147483647"),
            makeRoute(shortName = "100"),
        ).shuffled()

        val result = routes.sortedWith(subject)

        assertEquals("1", result[0].shortName)
        assertEquals("100", result[1].shortName)
        assertEquals("2147483647", result[2].shortName)
        assertEquals("2147483648", result[3].shortName)
        assertEquals("999999999999", result[4].shortName)
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
