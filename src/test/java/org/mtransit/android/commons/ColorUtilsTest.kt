package org.mtransit.android.commons

import org.junit.Assert.assertEquals
import org.junit.Test

class ColorUtilsTest {

    @Test
    fun test_filterColors_5_to_2() {
        val colors = mutableListOf(1, 2, 3, 4, 5)

        val result = ColorUtils.filterColors(colors, 2)

        assertEquals(mutableListOf(3, 4), result)
    }

    @Test
    fun test_filterColors_5_to_3() {
        val colors = mutableListOf(1, 2, 3, 4, 5)

        val result = ColorUtils.filterColors(colors, 3)

        assertEquals(mutableListOf(2, 3, 4), result)
    }

    @Test
    fun test_filterColors_5_to_10() {
        val colors = mutableListOf(1, 2, 3, 4, 5)

        val result = ColorUtils.filterColors(colors, 10)

        assertEquals(mutableListOf(1, 2, 3, 4, 5), result)
    }

    @Test
    fun test_filterColors_9_to_3() {
        val colors = mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

        val result = ColorUtils.filterColors(colors, 3)

        assertEquals(mutableListOf(4, 5, 6), result)
    }

    @Test
    fun test_filterColors_5_to_4() {
        val colors = mutableListOf(1, 2, 3, 4, 5)

        val result = ColorUtils.filterColors(colors, 4)

        assertEquals(mutableListOf(2, 3, 4, 5), result)
    }

    @Test
    fun test_filterColors_9_to_6() {
        val colors = mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9)

        val result = ColorUtils.filterColors(colors, 6)

        assertEquals(mutableListOf(3, 4, 5, 6, 7, 8), result)
    }
}