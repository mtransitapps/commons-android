package org.mtransit.android.commons

import org.junit.Assert.assertEquals
import org.junit.Test

class SqlUtilsTest {

    @Test
    fun test_unquotesUnescapeStringOrNull() {
        assertEquals(null, SqlUtils.unquotesUnescapeStringOrNull(""))
        assertEquals(null, SqlUtils.unquotesUnescapeStringOrNull("'"))
        assertEquals(null, SqlUtils.unquotesUnescapeStringOrNull("''"))
        assertEquals(null, SqlUtils.unquotesUnescapeStringOrNull("'''"))
        //
        assertEquals("abc", SqlUtils.unquotesUnescapeStringOrNull("abc"))
        assertEquals("abc", SqlUtils.unquotesUnescapeStringOrNull("'abc'"))
        assertEquals("a'bc", SqlUtils.unquotesUnescapeStringOrNull("a'bc"))
        assertEquals("a'bc", SqlUtils.unquotesUnescapeStringOrNull("'a'bc'"))
        assertEquals("a'bc", SqlUtils.unquotesUnescapeStringOrNull("a''bc"))
        assertEquals("a'bc", SqlUtils.unquotesUnescapeStringOrNull("'a''bc'"))
    }
}