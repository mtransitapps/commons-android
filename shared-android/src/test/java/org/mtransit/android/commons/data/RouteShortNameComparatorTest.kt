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
            mkRoute(shortName = "300"),
            mkRoute(shortName = "30"),
            mkRoute(shortName = "25"),
            mkRoute(shortName = "200"),
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
            mkRoute(shortName = "999999999999"),
            mkRoute(shortName = "1"),
            mkRoute(shortName = "2147483648"),
            mkRoute(shortName = "2147483647"),
            mkRoute(shortName = "100"),
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
            mkRoute(shortName = "66"),
            mkRoute(shortName = "A1"),
            mkRoute(shortName = "B2"),
            mkRoute(shortName = "C3"),
            mkRoute(shortName = "24X"),
            mkRoute(shortName = "25Y"),
            mkRoute(shortName = "26Z"),
            mkRoute(shortName = "É2"),
            mkRoute(shortName = "E2"),
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
        val rE2 = mkRoute(shortName = "É2")
        val rA1 = mkRoute(shortName = "A1")
        val rA2 = mkRoute(shortName = "A2")

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
        subject.areDifferent(mkRoute("authorityA", 1, "#1"), mkRoute("authorityB", 2, "#2")).let { result ->
            assertTrue(result)
        }
        subject.areDifferent(mkRoute("authorityA", 1, "#1"), mkRoute("authorityA", 2, "#2")).let { result ->
            assertTrue(result)
        }
        subject.areDifferent(mkRoute("authorityA", 1, "#1"), mkRoute("authorityB", 1, "#1")).let { result ->
            assertFalse(result)
        }
        subject.areDifferent(mkRoute("authorityA", 1, "#1"), mkRoute("authorityA", 1, "#1")).let { result ->
            assertFalse(result)
        }
    }

    @Test
    fun test_areComparable() {
        subject.areComparable(mkRoute(shortName = "123"), mkRoute(shortName = "456")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(mkRoute(shortName = "123"), mkRoute(shortName = "123")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(mkRoute(shortName = ""), mkRoute(shortName = "456")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(mkRoute(shortName = "123"), mkRoute(shortName = "")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(mkRoute(shortName = ""), mkRoute(shortName = "")).let { result ->
            assertTrue(result)
        }
    }
}
