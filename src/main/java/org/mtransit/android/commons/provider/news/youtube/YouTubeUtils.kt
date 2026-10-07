package org.mtransit.android.commons.provider.news.youtube

import org.mtransit.commons.model.Quadruple

object YouTubeUtils {

    private val YOUTUBE_VIDEO_PROFILE_URL_WITH_USERNAME = Regex("^https?://(?:www\\.)?youtube\\.com/user/([A-Za-z0-9._-]+)(?:[/?].*)?$")
    private val YOUTUBE_VIDEO_PROFILE_URL_WITH_CUSTOM_URL = Regex("^https?://(?:www\\.)?youtube\\.com/c/([A-Za-z0-9._-]+)(?:[/?].*)?$")
    private val YOUTUBE_VIDEO_PROFILE_URL_WITH_HANDLE = Regex("^https?://(?:www\\.)?youtube\\.com/@([A-Za-z0-9._-]+)(?:[/?].*)?$")
    private val YOUTUBE_VIDEO_PROFILE_URL_WITH_CHANNEL_ID = Regex("^https?://(?:www\\.)?youtube\\.com/channel/([A-Za-z0-9._-]+)(?:[/?].*)?$")

    /**
     * - Channel URL (ID-based)
     * Example: youtube.com/channel/UCUZHFZ9jIKrLroW8LcyJEQQ
     * - Handle URL
     * Example: youtube.com/@youtubecreators
     * - Custom URL
     * Example: youtube.com/c/YouTubeCreators
     * - Legacy username URL
     * Example: youtube.com/user/YouTube
     *
     * https://support.google.com/youtube/answer/6180214
     */
    fun pickChannelIdFromAuthorUrl(authorUrl: String?): Quadruple<String?, String?, String?, String?> {
        if (!authorUrl.isNullOrBlank()) {
            YOUTUBE_VIDEO_PROFILE_URL_WITH_USERNAME.find(authorUrl)?.groupValues?.get(1)?.let { username ->
                return Quadruple(username, null, null, null)
            }
            YOUTUBE_VIDEO_PROFILE_URL_WITH_HANDLE.find(authorUrl)?.groupValues?.get(1)?.let { handle ->
                return Quadruple(null, handle, null , null)
            }
            YOUTUBE_VIDEO_PROFILE_URL_WITH_CUSTOM_URL.find(authorUrl)?.groupValues?.get(1)?.let { customUrl ->
                return Quadruple(null, null, customUrl, null)
            }
            YOUTUBE_VIDEO_PROFILE_URL_WITH_CHANNEL_ID.find(authorUrl)?.groupValues?.get(1)?.let { channelId ->
                return Quadruple(null, null, null, channelId)
            }
        }
        return Quadruple(null, null, null, null)
    }
}
