package org.mtransit.android.commons.provider.config.news.youtube

import org.mtransit.android.commons.provider.config.news.NewsProviderConfig

data class YouTubeNewsProviderConfig(
    override val newsFeedConfigs: List<YouTubeNewsFeedConfig>
) : NewsProviderConfig