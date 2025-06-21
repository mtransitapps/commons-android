package org.mtransit.android.commons.provider.config.news.instagram

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.provider.config.news.NewsFeedConfig

data class InstagramNewsFeedConfig(
    val username: String,
    override val target: String? = null,
    override val lang: String = LocaleUtils.UNKNOWN,
    override val color: String? = null,
    override val severity: Int = SEVERITY_DEFAULT,
    override val noteworthy: Long = NOTEWORTHY_DEFAULT,
) : NewsFeedConfig {

    companion object {
        const val SEVERITY_DEFAULT = 2 // news_provider_severity_info_agency
        const val NOTEWORTHY_DEFAULT = 604_800_000L // news_provider_noteworthy_long_term // 1 week
    }

    override fun toCursorRow(): Array<Any> {
        return makeCursorRow(
            extra = Json.encodeToString(
                buildJsonObject {
                    put("username", username)
                }
            )
        )
    }
}
