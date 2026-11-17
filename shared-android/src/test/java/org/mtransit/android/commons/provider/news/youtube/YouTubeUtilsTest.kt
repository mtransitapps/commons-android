package org.mtransit.android.commons.provider.news.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeUtilsTest {

    @Test
    fun test_pickChannelIdFromAuthorUrl_with_username() {
        val authorUrl = "https://www.youtube.com/user/websharestm"

        val (username, handle, customUrl, channelId) = YouTubeUtils.pickChannelIdFromAuthorUrl(authorUrl)

        assertEquals("websharestm", username)
        assertNull(handle)
        assertNull(customUrl)
        assertNull(channelId)
    }

    @Test
    fun test_pickChannelIdFromAuthorUrl_with_custom_url() {
        val authorUrl = "https://www.youtube.com/c/ReseauexpressmetropolitainREM"

        val (username, handle, customUrl, channelId) = YouTubeUtils.pickChannelIdFromAuthorUrl(authorUrl)

        assertNull(username)
        assertNull(handle)
        assertEquals("ReseauexpressmetropolitainREM", customUrl)
        assertNull(channelId)
    }

    @Test
    fun test_pickChannelIdFromAuthorUrl_with_custom_url1() {
        val authorUrl = "https://www.youtube.com/c/exoreseaudetransportmetropolitain"

        val (username, handle, customUrl, channelId) = YouTubeUtils.pickChannelIdFromAuthorUrl(authorUrl)

        assertNull(username)
        assertNull(handle)
        assertEquals("exoreseaudetransportmetropolitain", customUrl)
        assertNull(channelId)
    }

    @Test
    fun test_pickChannelIdFromAuthorUrl_with_handle() {
        val authorUrl = "https://www.youtube.com/@ReseauexpressmetropolitainREM"

        val (username, handle, customUrl, channelId) = YouTubeUtils.pickChannelIdFromAuthorUrl(authorUrl)

        assertNull(username)
        assertEquals("ReseauexpressmetropolitainREM", handle)
        assertNull(customUrl)
        assertNull(channelId)
    }

    @Test
    fun test_pickChannelIdFromAuthorUrl_with_handle2() {
        val authorUrl = "https://www.youtube.com/@STTR-officiel"

        val (username, handle, customUrl, channelId) = YouTubeUtils.pickChannelIdFromAuthorUrl(authorUrl)

        assertNull(username)
        assertEquals("STTR-officiel", handle)
        assertNull(customUrl)
        assertNull(channelId)
    }

    @Test
    fun test_pickChannelIdFromAuthorUrl_with_handle2_1() {
        val authorUrl = "https://www.youtube.com/@STTR-officiel/not?x=1"

        val (username, handle, customUrl, channelId) = YouTubeUtils.pickChannelIdFromAuthorUrl(authorUrl)

        assertNull(username)
        assertEquals("STTR-officiel", handle)
        assertNull(customUrl)
        assertNull(channelId)
    }

    @Test
    fun test_pickChannelIdFromAuthorUrl_with_channel_id() {
        val authorUrl = "https://www.youtube.com/channel/UCkMvg3gUin_OWDx1ag6V3sw"

        val (username, handle, customUrl, channelId) = YouTubeUtils.pickChannelIdFromAuthorUrl(authorUrl)

        assertNull(username)
        assertNull(handle)
        assertNull(customUrl)
        assertEquals("UCkMvg3gUin_OWDx1ag6V3sw", channelId)
    }
}
