package org.mtransit.android.commons.provider.config.news

import android.database.Cursor
import android.database.MatrixCursor
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.commons.provider.config.ProviderConfig
import org.mtransit.android.commons.provider.config.news.instagram.InstagramNewsProviderConfig
import org.mtransit.android.commons.provider.config.news.rss.RSSNewsProviderConfig
import org.mtransit.android.commons.provider.config.news.twitter.TwitterNewsProviderConfig
import org.mtransit.android.commons.provider.config.news.youtube.YouTubeNewsProviderConfig

interface NewsProviderConfig : ProviderConfig {
    val type: NewsType
    val newsFeedConfigs: List<NewsFeedConfig>

    fun toCursor(): Cursor =
        MatrixCursor(NewsProviderContract.PROJECTION_NEWS_FEED_CONFIG).apply {
            newsFeedConfigs.forEach { newsFeedConfig ->
                addRow(newsFeedConfig.toCursorRow())
            }
        }

    companion object {

        @JvmStatic
        fun fromCursor(cursor: Cursor?): NewsProviderConfig? {
            cursor ?: return null
            val newsType = DefaultNewsProviderConfig.getTypeFromCursor(cursor) ?: return null
            return when (newsType) {
                NewsType.RSS -> RSSNewsProviderConfig.fromCursor(cursor)
                NewsType.YOUTUBE -> YouTubeNewsProviderConfig.fromCursor(cursor)
                NewsType.TWITTER -> TwitterNewsProviderConfig.fromCursor(cursor)
                NewsType.INSTAGRAM -> InstagramNewsProviderConfig.fromCursor(cursor)
                NewsType.CA_STO -> DefaultNewsProviderConfig.fromCursor(newsType)
                NewsType.CA_WINNIPEG -> DefaultNewsProviderConfig.fromCursor(newsType)
            }
        }
    }
}