package org.mtransit.android.commons.provider.config.news

import android.database.Cursor

data class DefaultNewsProviderConfig @JvmOverloads constructor(
    override val type: NewsType,
    override val newsFeedConfigs: List<NewsFeedConfig> = emptyList()
) : NewsProviderConfig {
    companion object {
        fun getTypeFromCursor(cursor: Cursor): NewsType? = NewsType.fromId(
            NewsFeedConfig.getTypeIdFromCursorRow(cursor)
        )

        fun fromCursor(newsType: NewsType): NewsProviderConfig = DefaultNewsProviderConfig(newsType,)
    }
}
