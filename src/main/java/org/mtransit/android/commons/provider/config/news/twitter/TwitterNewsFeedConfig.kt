package org.mtransit.android.commons.provider.config.news.twitter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.provider.config.news.NewsFeedConfig

data class TwitterNewsFeedConfig(
    val username: String,
    val userId: String? = null, // optional (saved 1 call to Twitter API)
    override val target: String? = null,
    override val lang: String = LocaleUtils.UNKNOWN, // TODO ? LocaleUtils.MULTIPLE, // detect lang from Twitter API
    override val color: String? = null,
    override val severity: Int = SEVERITY_DEFAULT,
    override val noteworthy: Long = NOTEWORTHY_DEFAULT,
) : NewsFeedConfig {

    companion object {
        const val SEVERITY_DEFAULT = 2 // news_provider_severity_info_agency
        const val NOTEWORTHY_DEFAULT = 10_800_000L // news_provider_noteworthy_warning // 3 hours
    }

    override fun toCursorRow(): Array<Any> {
        return makeCursorRow(
            extra = Json.encodeToString(
                buildJsonObject {
                    put("username", username)
                    put("userId", userId)
                }
            )
        )
    }
}
