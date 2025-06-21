package org.mtransit.android.commons.provider.config.news

import org.mtransit.android.commons.LocaleUtils

interface NewsFeedConfig {
    val target: String? get() = null // target the entire agency
    val lang: String get() = LocaleUtils.UNKNOWN // show all
    val color: String? get() = null // same as agency (target?) color
    val severity: Int get() = 1 // news_provider_severity_info_unknown
    val noteworthy: Long get() = 86_400_000L // news_provider_noteworthy_info // 1 days

    /**
     * See [org.mtransit.android.commons.provider.NewsProviderContract.FeedConfigColumns]
     * See [org.mtransit.android.commons.provider.NewsProviderContract.PROJECTION_NEWS_FEED_CONFIG]
     */
    fun toCursorRow(): Array<Any> {
        return makeCursorRow()
    }

    fun makeCursorRow(extra: String? = null) = arrayOf<Any>(
        target.orEmpty(),
        lang,
        color.orEmpty(),
        severity,
        noteworthy,
        extra.orEmpty(),
    )
}