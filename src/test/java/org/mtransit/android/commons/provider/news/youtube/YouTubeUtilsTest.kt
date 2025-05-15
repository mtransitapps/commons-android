package org.mtransit.android.commons.provider.news.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeUtilsTest {

    @Test
    fun test_pickChannelIdFromAuthorUrl_with_username() {
        val authorUrl = "https://www.youtube.com/user/websharestm"

        val (username, customUrl, channelId) = YouTubeUtils.pickChannelIdFromAuthorUrl(authorUrl)

        assertEquals("websharestm", username)
        assertNull(customUrl)
        assertNull(channelId)
    }

    @Test
    fun test_pickChannelIdFromAuthorUrl_with_custom_url() {
        val authorUrl = "https://www.youtube.com/c/ReseauexpressmetropolitainREM"

        val (username, customUrl, channelId) = YouTubeUtils.pickChannelIdFromAuthorUrl(authorUrl)

        assertNull(username)
        assertEquals("ReseauexpressmetropolitainREM", customUrl)
        assertNull(channelId)
    }

    @Test
    fun test_pickChannelIdFromAuthorUrl_with_custom_url2() {
        val authorUrl = "https://www.youtube.com/@ReseauexpressmetropolitainREM"

        val (username, customUrl, channelId) = YouTubeUtils.pickChannelIdFromAuthorUrl(authorUrl)

        assertNull(username)
        assertEquals("ReseauexpressmetropolitainREM", customUrl)
        assertNull(channelId)
    }

    @Test
    fun test_pickChannelIdFromAuthorUrl_with_channel_id() {
        val authorUrl = "https://www.youtube.com/channel/UCkMvg3gUin_OWDx1ag6V3sw"

        val (username, customUrl, channelId) = YouTubeUtils.pickChannelIdFromAuthorUrl(authorUrl)

        assertNull(username)
        assertNull(customUrl)
        assertEquals("UCkMvg3gUin_OWDx1ag6V3sw", channelId)
    }
}