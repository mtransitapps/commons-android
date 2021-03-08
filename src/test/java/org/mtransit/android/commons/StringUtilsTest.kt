package org.mtransit.android.commons

import org.junit.Assert
import org.junit.Test

class StringUtilsTest {

    @Test
    fun oneLineOneSpaceEmptyString() {
        // Arrange
        val string = StringUtils.EMPTY
        // Act
        val result = StringUtils.oneLineOneSpace(string)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertEquals(StringUtils.EMPTY, result)
    }

    @Test
    fun oneLineOneSpace() {
        // Arrange
        val string = ("This is a multiline "
                + "\n" //
                + "\n" //
                + "\n" //
                + "string with too much spacing "
                + " " //
                + " " //
                + " " //
                + "between words.")
        // Act
        val result = StringUtils.oneLineOneSpace(string)
        // Assert
        Assert.assertNotNull(result)
        Assert.assertEquals(
            "This is a multiline "
                    + "string with too much spacing "
                    + "between words.", result
        )
    }
}