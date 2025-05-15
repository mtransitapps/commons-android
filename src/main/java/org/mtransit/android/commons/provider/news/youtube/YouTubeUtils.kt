package org.mtransit.android.commons.provider.news.youtube

object YouTubeUtils {

    private val YOUTUBE_VIDEO_PROFILE_URL_WITH_USERNAME = Regex("^https?://(?:www\\.)?youtube\\.com/user/(\\w+)$")
    private val YOUTUBE_VIDEO_PROFILE_URL_WITH_CUSTOM_URL = Regex("^https?://(?:www\\.)?youtube\\.com/(c/|@)(\\w+)$")
    private val YOUTUBE_VIDEO_PROFILE_URL_WITH_CHANNEL_ID = Regex("^https?://(?:www\\.)?youtube\\.com/channel/(\\w+)$")

    fun pickChannelIdFromAuthorUrl(authorUrl: String?): Triple<String?, String?, String?> {
        if (!authorUrl.isNullOrBlank()) {
            if (authorUrl.matches(YOUTUBE_VIDEO_PROFILE_URL_WITH_USERNAME)) {
                val username = YOUTUBE_VIDEO_PROFILE_URL_WITH_USERNAME.find(authorUrl)?.groupValues?.get(1)
                return Triple(username, null, null)
            }
            if (authorUrl.matches(YOUTUBE_VIDEO_PROFILE_URL_WITH_CUSTOM_URL)) {
                val customUrl = YOUTUBE_VIDEO_PROFILE_URL_WITH_CUSTOM_URL.find(authorUrl)?.groupValues?.get(2)
                return Triple(null, customUrl, null)
            }
            if (authorUrl.matches(YOUTUBE_VIDEO_PROFILE_URL_WITH_CHANNEL_ID)) {
                val channelId = YOUTUBE_VIDEO_PROFILE_URL_WITH_CHANNEL_ID.find(authorUrl)?.groupValues?.get(1)
                return Triple(null, null, channelId)
            }
        }
        return Triple(null, null, null)
    }
}