package org.mtransit.android.commons.data

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AccessibilityTest(
    private val stopAccessible: Int,
    private val tripAccessible: Int,
    private val expectedResult: Int,
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            arrayOf(Accessibility.UNKNOWN, Accessibility.UNKNOWN, Accessibility.UNKNOWN),
            arrayOf(Accessibility.UNKNOWN, Accessibility.POSSIBLE, Accessibility.POSSIBLE),
            arrayOf(Accessibility.UNKNOWN, Accessibility.NOT_POSSIBLE, Accessibility.NOT_POSSIBLE),
            arrayOf(Accessibility.POSSIBLE, Accessibility.UNKNOWN, Accessibility.POSSIBLE),
            arrayOf(Accessibility.POSSIBLE, Accessibility.POSSIBLE, Accessibility.POSSIBLE),
            arrayOf(Accessibility.POSSIBLE, Accessibility.NOT_POSSIBLE, Accessibility.NOT_POSSIBLE),
            arrayOf(Accessibility.NOT_POSSIBLE, Accessibility.UNKNOWN, Accessibility.NOT_POSSIBLE),
            arrayOf(Accessibility.NOT_POSSIBLE, Accessibility.NOT_POSSIBLE, Accessibility.NOT_POSSIBLE),
            arrayOf(Accessibility.NOT_POSSIBLE, Accessibility.POSSIBLE, Accessibility.NOT_POSSIBLE),
        )
    }

    @Test
    fun combine_Unknown_Unknown() {
        // Arrange
        // Act
        val result = Accessibility.combine(stopAccessible, tripAccessible)
        // Assert
        Assert.assertEquals(expectedResult, result)
    }
}