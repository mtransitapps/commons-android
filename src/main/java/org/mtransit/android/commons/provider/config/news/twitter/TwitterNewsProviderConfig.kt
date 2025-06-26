package org.mtransit.android.commons.provider.config.news.twitter

import android.database.Cursor
import org.mtransit.android.commons.provider.config.news.NewsProviderConfig
import org.mtransit.android.commons.provider.config.news.NewsType

data class TwitterNewsProviderConfig(
    override val newsFeedConfigs: List<TwitterNewsFeedConfig>
) : NewsProviderConfig {
    override val type = NewsType.TWITTER

    companion object {
        fun fromCursor(cursor: Cursor)= TwitterNewsProviderConfig(
            newsFeedConfigs = TwitterNewsFeedConfig.fromCursor(cursor)
        )
    }
}
