package org.mtransit.android.commons.provider.config.news

data class DefaultNewsProviderConfig @JvmOverloads constructor(
    override val newsFeedConfigs: List<NewsFeedConfig> = emptyList()
) : NewsProviderConfig
