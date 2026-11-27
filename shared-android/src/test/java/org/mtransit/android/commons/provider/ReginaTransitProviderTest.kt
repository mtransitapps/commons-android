package org.mtransit.android.commons.provider

import kotlin.test.Test
import kotlin.test.assertEquals

class ReginaTransitProviderTest {

    @Test
    fun test_parseTime_09_19_PM() {
        val timeString = "09:19 PM"
        val expected = ((21L * 60L + 19L) * 60L) * 1000L

        val result = timeString.parseTime()

        assertEquals(expected, result)
    }
}
