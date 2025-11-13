package org.mtransit.android.commons.provider.config.news.rss

import android.database.Cursor
import org.mtransit.android.commons.provider.config.news.NewsProviderConfig
import org.mtransit.android.commons.provider.config.news.NewsType

data class RSSNewsProviderConfig(
    override val newsFeedConfigs: List<RSSNewsFeedConfig>
) : NewsProviderConfig {
    override val type = NewsType.RSS

    companion object {
        fun fromCursor(cursor: Cursor) = RSSNewsProviderConfig(
            newsFeedConfigs = RSSNewsFeedConfig.fromCursor(cursor)
        )
    }
}
