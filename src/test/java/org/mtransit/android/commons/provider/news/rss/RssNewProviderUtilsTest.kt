package org.mtransit.android.commons.provider.news.rss

import org.junit.Assert.*
import org.junit.Test
import org.mtransit.android.commons.provider.news.rss.RssNewProviderUtils as Subject

class RssNewProviderUtilsTest {

    @Test
    fun verify_pickLabel_with_string_www() {
        val url = "https://www.google.com"

        val result = Subject.pickLabel(url)

        assertEquals("google.com", result)
    }

    @Test
    fun verify_pickLabel_with_string_www_not() {
        val url = "https://google.com"

        val result = Subject.pickLabel(url)

        assertEquals("google.com", result)
    }

    @Test
    fun verify_pickLabel_with_string_bc_transit() {
        val url = "https://www.bctransit.com/feed/"

        val result = Subject.pickLabel(url)

        assertEquals("bctransit.com", result)
    }

    @Test
    fun verify_pickLabel_with_string_empty() {
        val url = ""

        val result = Subject.pickLabel(url)

        assertEquals("", result)
    }
}