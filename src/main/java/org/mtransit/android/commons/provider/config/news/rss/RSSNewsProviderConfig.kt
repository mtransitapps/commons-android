package org.mtransit.android.commons.provider.config.news.rss

import org.mtransit.android.commons.provider.config.news.NewsProviderConfig

data class RSSNewsProviderConfig(
    override val newsFeedConfigs: List<RSSNewsFeedConfig>
) : NewsProviderConfig