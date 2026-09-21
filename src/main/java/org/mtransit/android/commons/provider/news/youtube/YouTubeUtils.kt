package org.mtransit.android.commons.provider.news.youtube

object YouTubeUtils {

    private val YOUTUBE_VIDEO_PROFILE_URL_WITH_USERNAME = Regex("^https?://(?:www\\.)?youtube\\.com/user/([A-Za-z0-9._-]+)(?:[/?].*)?$")
    private val YOUTUBE_VIDEO_PROFILE_URL_WITH_CUSTOM_URL = Regex("^https?://(?:www\\.)?youtube\\.com/(?:c/|@)([A-Za-z0-9._-]+)(?:[/?].*)?$")
    private val YOUTUBE_VIDEO_PROFILE_URL_WITH_CHANNEL_ID = Regex("^https?://(?:www\\.)?youtube\\.com/channel/([A-Za-z0-9._-]+)(?:[/?].*)?$")

    fun pickChannelIdFromAuthorUrl(authorUrl: String?): Triple<String?, String?, String?> {
        if (!authorUrl.isNullOrBlank()) {
            YOUTUBE_VIDEO_PROFILE_URL_WITH_USERNAME.find(authorUrl)?.groupValues?.get(1)?.let { username ->
                return Triple(username, null, null)
            }
            YOUTUBE_VIDEO_PROFILE_URL_WITH_CUSTOM_URL.find(authorUrl)?.groupValues?.get(1)?.let { customUrl ->
                return Triple(null, customUrl, null)
            }
            YOUTUBE_VIDEO_PROFILE_URL_WITH_CHANNEL_ID.find(authorUrl)?.groupValues?.get(1)?.let { channelId ->
                return Triple(null, null, channelId)
            }
        }
        return Triple(null, null, null)
    }
}
