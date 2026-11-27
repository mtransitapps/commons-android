package org.mtransit.android.commons.provider

import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class GBFSProviderTest {

    @Test
    fun test_getIntOrBoolean() {
        // int - GBFS v1
        assertEquals(false, JsonPrimitive(0).getIntOrBoolean("key"))
        assertEquals(true, JsonPrimitive(1).getIntOrBoolean("key"))
        assertEquals(null, JsonPrimitive(2).getIntOrBoolean("key"))

        // boolean - GBFS v2
        assertEquals(false, JsonPrimitive(false).getIntOrBoolean("key"))
        assertEquals(true, JsonPrimitive(true).getIntOrBoolean("key"))
        assertEquals(null, JsonPrimitive(null).getIntOrBoolean("key"))
    }
}
