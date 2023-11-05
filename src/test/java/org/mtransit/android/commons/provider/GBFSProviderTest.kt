package org.mtransit.android.commons.provider

import org.junit.Assert.assertEquals
import org.junit.Test

class GBFSProviderTest {

    @Test
    fun test_getIntOrBoolean() {
        // int - GBFS v1
        assertEquals(false, GBFSProvider.getIntOrBoolean(0 as Any, "key"))
        assertEquals(true, GBFSProvider.getIntOrBoolean(1 as Any, "key"))
        assertEquals(null, GBFSProvider.getIntOrBoolean(2 as Any, "key"))

        // boolean - GBFS v2
        assertEquals(false, GBFSProvider.getIntOrBoolean(false as Any, "key"))
        assertEquals(true, GBFSProvider.getIntOrBoolean(true as Any, "key"))
        assertEquals(null, GBFSProvider.getIntOrBoolean(null, "key"))
    }
}