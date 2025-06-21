package org.mtransit.android.commons.provider.config.news

import android.database.Cursor
import android.database.MatrixCursor
import org.mtransit.android.commons.provider.NewsProviderContract
import org.mtransit.android.commons.provider.config.ProviderConfig

interface NewsProviderConfig : ProviderConfig {
    val newsFeedConfigs: List<NewsFeedConfig>

    fun toCursor(): Cursor =
        MatrixCursor(NewsProviderContract.PROJECTION_NEWS_FEED_CONFIG).apply {
            newsFeedConfigs.forEach {
                addRow(it.toCursorRow())
            }
        }
}