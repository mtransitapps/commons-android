package org.mtransit.android.commons.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectionHeadSignComparatorTest {

    private val subject: DirectionHeadSignComparator = Direction.HEAD_SIGN_COMPARATOR

    @Test
    fun test_compare() {
        val directions = listOf(
            makeDirection(headsign = "Ccc"),
            makeDirection(headsign = "Aaa"),
            makeDirection(headsign = "Bbb"),
        )

        val result = directions.sortedWith(subject)

        assertEquals("Aaa", result[0].headsignValue)
        assertEquals("Bbb", result[1].headsignValue)
        assertEquals("Ccc", result[2].headsignValue)
    }

    @Test
    fun test_compare2() {
        val directions = listOf(
            makeDirection(headsign = "Head-Sign 1"),
            makeDirection(headsign = "Head-Sign 0"),
        )

        val result = directions.sortedWith(subject)

        assertEquals("Head-Sign 0", result[0].headsignValue)
        assertEquals("Head-Sign 1", result[1].headsignValue)
    }

    @Test
    fun test_areDifferent() {
        subject.areDifferent(makeDirection("authorityA", 10, 0, headsign = "HS 0"), makeDirection("authorityB", 20, 1, headsign = "HS 1")).let { result ->
            assertTrue(result)
        }
        subject.areDifferent(makeDirection("authorityA", 10, 0, headsign = "HS 0"), makeDirection("authorityA", 20, 1, headsign = "HS 1")).let { result ->
            assertTrue(result)
        }
        subject.areDifferent(makeDirection("authorityA", 10, 0, headsign = "HS 0"), makeDirection("authorityB", 10, 0, headsign = "HS 0")).let { result ->
            assertFalse(result)
        }
        subject.areDifferent(makeDirection("authorityA", 10, 0, headsign = "HS 0"), makeDirection("authorityA", 10, 1, headsign = "HS 1")).let { result ->
            assertTrue(result)
        }
        subject.areDifferent(makeDirection("authorityA", 10, 0, headsign = "HS 0"), makeDirection("authorityA", 10, 0, headsign = "HS 0")).let { result ->
            assertFalse(result)
        }
    }

    @Test
    fun test_areComparable() {
        subject.areComparable(makeDirection(headsign = "North"), makeDirection(headsign = "South")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(makeDirection(headsign = ""), makeDirection(headsign = "South")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(makeDirection(headsign = "North"), makeDirection(headsign = "")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(makeDirection(headsign = ""), makeDirection(headsign = "")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(
            makeDirection(headsignType = Direction.HEADSIGN_TYPE_NONE),
            makeDirection(headsignType = Direction.HEADSIGN_TYPE_NONE)
        ).let { result ->
            assertTrue(result)
        }
        subject.areComparable(makeDirection(), makeDirection(headsignType = Direction.HEADSIGN_TYPE_NONE)).let { result ->
            assertTrue(result)
        }
        subject.areComparable(makeDirection(headsignType = Direction.HEADSIGN_TYPE_NONE), makeDirection()).let { result ->
            assertTrue(result)
        }
    }
}
