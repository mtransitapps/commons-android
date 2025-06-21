package org.mtransit.android.commons.provider.config.news.instagram

import org.mtransit.android.commons.provider.config.news.NewsProviderConfig

data class InstagramNewsProviderConfig(
    override val newsFeedConfigs: List<InstagramNewsFeedConfig>
) : NewsProviderConfig