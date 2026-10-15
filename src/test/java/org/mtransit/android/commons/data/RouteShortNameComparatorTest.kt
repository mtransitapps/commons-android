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
    fun test_compare_AZ19() {
        val routes = listOf(
            makeRoute(shortName = "66"),
            makeRoute(shortName = "A1"),
            makeRoute(shortName = "B2"),
            makeRoute(shortName = "C3"),
            makeRoute(shortName = "24X"),
            makeRoute(shortName = "25Y"),
            makeRoute(shortName = "26Z"),
            makeRoute(shortName = "É2"),
            makeRoute(shortName = "E2"),
        ).shuffled()

        val result = routes.sortedWith(subject)

        var index = -1
        assertEquals("24X", result[++index].shortName)
        assertEquals("25Y", result[++index].shortName)
        assertEquals("26Z", result[++index].shortName)
        assertEquals("66", result[++index].shortName)
        assertEquals("A1", result[++index].shortName)
        assertEquals("B2", result[++index].shortName)
        assertEquals("C3", result[++index].shortName)
        assertEquals("E2", result[++index].shortName)
        assertEquals("É2", result[++index].shortName)
        assertEquals(result.size, ++index)
    }

    @Test
    fun test_compare_transitiveOrdering_noCycle() {
        val rE2 = makeRoute(shortName = "É2")
        val rA1 = makeRoute(shortName = "A1")
        val rA2 = makeRoute(shortName = "A2")

        // A1 vs A2: same prefix "A", digits 1 < 2 -> A1 < A2
        assertTrue(subject.compare(rA1, rA2) < 0)
        assertTrue(subject.compare(rA2, rA1) > 0)

        // A1 vs É2: prefix "A" < prefix "É" -> A1 < É2
        assertTrue(subject.compare(rA1, rE2) < 0)
        assertTrue(subject.compare(rE2, rA1) > 0)

        // A2 vs É2: prefix "A" < prefix "É" -> A2 < É2 (digits match 2 == 2)
        assertTrue(subject.compare(rA2, rE2) < 0)
        assertTrue(subject.compare(rE2, rA2) > 0)

        val routes = listOf(rE2, rA1, rA2).shuffled()
        val result = routes.sortedWith(subject)

        assertEquals("A1", result[0].shortName)
        assertEquals("A2", result[1].shortName)
        assertEquals("É2", result[2].shortName)
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
