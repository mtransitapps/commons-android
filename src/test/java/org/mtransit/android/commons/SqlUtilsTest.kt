package org.mtransit.android.commons

import org.junit.Assert.assertEquals
import org.junit.Test

class SqlUtilsTest {

    @Test
    fun test_unquoteUnescapeStringOrNull() {
        assertEquals(null, SqlUtils.unquoteUnescapeStringOrNull(""))
        assertEquals("'", SqlUtils.unquoteUnescapeStringOrNull("'"))
        assertEquals(null, SqlUtils.unquoteUnescapeStringOrNull("''"))
        assertEquals("'", SqlUtils.unquoteUnescapeStringOrNull("'''"))
        //
        assertEquals("abc", SqlUtils.unquoteUnescapeStringOrNull("abc"))
        assertEquals("abc", SqlUtils.unquoteUnescapeStringOrNull("'abc'"))
        assertEquals("a'bc", SqlUtils.unquoteUnescapeStringOrNull("a'bc"))
        assertEquals("a'bc", SqlUtils.unquoteUnescapeStringOrNull("'a'bc'"))
        assertEquals("a'bc'", SqlUtils.unquoteUnescapeStringOrNull("'a'bc''"))
        assertEquals("a'bc", SqlUtils.unquoteUnescapeStringOrNull("a''bc"))
        assertEquals("a'bc", SqlUtils.unquoteUnescapeStringOrNull("'a''bc'"))
    }
}