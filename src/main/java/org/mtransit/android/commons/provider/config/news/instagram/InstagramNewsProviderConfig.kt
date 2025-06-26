package org.mtransit.android.commons.provider.config.news.instagram

import android.database.Cursor
import org.mtransit.android.commons.provider.config.news.NewsProviderConfig
import org.mtransit.android.commons.provider.config.news.NewsType

data class InstagramNewsProviderConfig(
    override val newsFeedConfigs: List<InstagramNewsFeedConfig>
) : NewsProviderConfig {
    override val type = NewsType.INSTAGRAM

    companion object {
        fun fromCursor(cursor: Cursor)= InstagramNewsProviderConfig(
            newsFeedConfigs = InstagramNewsFeedConfig.fromCursor(cursor)
        )
    }
}
