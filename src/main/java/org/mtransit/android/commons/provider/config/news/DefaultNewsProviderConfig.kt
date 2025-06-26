package org.mtransit.android.commons.provider.config.news

import android.database.Cursor
import org.mtransit.android.commons.provider.NewsProviderContract.FeedConfigColumns

data class DefaultNewsProviderConfig @JvmOverloads constructor(
    override val type: NewsType,
    override val newsFeedConfigs: List<NewsFeedConfig> = emptyList()
) : NewsProviderConfig {
    companion object {
        fun getTypeFromCursor(cursor: Cursor): NewsType? = NewsType.fromId(
            cursor.getInt(cursor.getColumnIndexOrThrow(FeedConfigColumns.T_NEWS_FEED_CONFIG_K_TYPE))
        )

        fun fromCursor(newsType: NewsType): NewsProviderConfig = DefaultNewsProviderConfig(newsType,)
    }
}
