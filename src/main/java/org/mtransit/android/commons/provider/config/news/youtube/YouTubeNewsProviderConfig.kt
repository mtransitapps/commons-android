package org.mtransit.android.commons.provider.config.news.youtube

import android.database.Cursor
import org.mtransit.android.commons.provider.config.news.NewsProviderConfig
import org.mtransit.android.commons.provider.config.news.NewsType

data class YouTubeNewsProviderConfig(
    override val newsFeedConfigs: List<YouTubeNewsFeedConfig>
) : NewsProviderConfig {
    override val type = NewsType.YOUTUBE

    companion object {
        fun fromCursor(cursor: Cursor)= YouTubeNewsProviderConfig(
            newsFeedConfigs = YouTubeNewsFeedConfig.fromCursor(cursor)
        )
    }
}
