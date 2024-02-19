package org.mtransit.android.commons

import org.junit.Assert.assertEquals
import org.junit.Test

class SqlUtilsTest {

    @Test
    fun test_unescapeStringOrNull() {
        assertEquals(null, SqlUtils.unescapeStringOrNull(""))
        assertEquals(null, SqlUtils.unescapeStringOrNull("'"))
        assertEquals(null, SqlUtils.unescapeStringOrNull("''"))
        assertEquals(null, SqlUtils.unescapeStringOrNull("'''"))
        //
        assertEquals("abc", SqlUtils.unescapeStringOrNull("abc"))
        assertEquals("abc", SqlUtils.unescapeStringOrNull("'abc'"))
        assertEquals("a'bc", SqlUtils.unescapeStringOrNull("a'bc"))
        assertEquals("a'bc", SqlUtils.unescapeStringOrNull("'a'bc'"))
        assertEquals("a'bc", SqlUtils.unescapeStringOrNull("a''bc"))
        assertEquals("a'bc", SqlUtils.unescapeStringOrNull("'a''bc'"))
    }
}