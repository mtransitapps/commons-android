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
            mkDirection(headsign = "Ccc"),
            mkDirection(headsign = "Aaa"),
            mkDirection(headsign = "Bbb"),
        )

        val result = directions.sortedWith(subject)

        assertEquals("Aaa", result[0].headsignValue)
        assertEquals("Bbb", result[1].headsignValue)
        assertEquals("Ccc", result[2].headsignValue)
    }

    @Test
    fun test_compare2() {
        val directions = listOf(
            mkDirection(headsign = "Head-Sign 1"),
            mkDirection(headsign = "Head-Sign 0"),
        )

        val result = directions.sortedWith(subject)

        assertEquals("Head-Sign 0", result[0].headsignValue)
        assertEquals("Head-Sign 1", result[1].headsignValue)
    }

    @Test
    fun test_areDifferent() {
        subject.areDifferent(mkDirection("authorityA", 10, 0, headsign = "HS 0"), mkDirection("authorityB", 20, 1, headsign = "HS 1")).let { result ->
            assertTrue(result)
        }
        subject.areDifferent(mkDirection("authorityA", 10, 0, headsign = "HS 0"), mkDirection("authorityA", 20, 1, headsign = "HS 1")).let { result ->
            assertTrue(result)
        }
        subject.areDifferent(mkDirection("authorityA", 10, 0, headsign = "HS 0"), mkDirection("authorityB", 10, 0, headsign = "HS 0")).let { result ->
            assertFalse(result)
        }
        subject.areDifferent(mkDirection("authorityA", 10, 0, headsign = "HS 0"), mkDirection("authorityA", 10, 1, headsign = "HS 1")).let { result ->
            assertTrue(result)
        }
        subject.areDifferent(mkDirection("authorityA", 10, 0, headsign = "HS 0"), mkDirection("authorityA", 10, 0, headsign = "HS 0")).let { result ->
            assertFalse(result)
        }
    }

    @Test
    fun test_areComparable() {
        subject.areComparable(mkDirection(headsign = "North"), mkDirection(headsign = "South")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(mkDirection(headsign = ""), mkDirection(headsign = "South")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(mkDirection(headsign = "North"), mkDirection(headsign = "")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(mkDirection(headsign = ""), mkDirection(headsign = "")).let { result ->
            assertTrue(result)
        }
        subject.areComparable(
            mkDirection(headsignType = Direction.HEADSIGN_TYPE_NONE),
            mkDirection(headsignType = Direction.HEADSIGN_TYPE_NONE)
        ).let { result ->
            assertTrue(result)
        }
        subject.areComparable(mkDirection(), mkDirection(headsignType = Direction.HEADSIGN_TYPE_NONE)).let { result ->
            assertTrue(result)
        }
        subject.areComparable(mkDirection(headsignType = Direction.HEADSIGN_TYPE_NONE), mkDirection()).let { result ->
            assertTrue(result)
        }
    }
}
