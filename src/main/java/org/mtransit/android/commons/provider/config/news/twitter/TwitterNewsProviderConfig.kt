package org.mtransit.android.commons.provider.config.news.twitter

import org.mtransit.android.commons.provider.config.news.NewsProviderConfig

data class TwitterNewsProviderConfig(
    override val newsFeedConfigs: List<TwitterNewsFeedConfig>
) : NewsProviderConfig