package org.mtransit.android.commons.provider.news.twitter

import com.google.gson.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant
import java.util.Date

class TwitterDateAdapterTest {

    private val subject = TwitterDateAdapter()

    @Test
    fun deserialize_withMilliseconds() {
        val result = subject.deserialize(JsonPrimitive("2026-07-01T11:00:24.000Z"), null, null)

        assertNotNull(result)
        assertEquals(Date.from(Instant.parse("2026-07-01T11:00:24Z")), result)
    }

    @Test
    fun deserialize_withoutMilliseconds() {
        val result = subject.deserialize(JsonPrimitive("2026-07-01T11:00:24Z"), null, null)

        assertNotNull(result)
        assertEquals(Date.from(Instant.parse("2026-07-01T11:00:24Z")), result)
    }
}
