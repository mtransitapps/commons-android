package org.mtransit.android.commons.provider.config.news.youtube

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.provider.config.news.NewsFeedConfig

data class YouTubeNewsFeedConfig(
    val authorUrl: String, // can extract "username OR @userHandle or channel ID" from url
    val username: String? = null, // optional (username OR @userHandle or channel ID)
    val userHandle: String? = null, // optional (username OR @userHandle or channel ID)
    val channelId: String? = null, // optional (username OR @userHandle or channel ID)
    override val target: String? = null, // default to agency
    override val color: String? = null, // defaults to agency
    override val lang: String = LocaleUtils.UNKNOWN, // show all
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
                    put("authorUrl", authorUrl)
                    username?.let { put("username", it) }
                    userHandle?.let { put("userHandle", it) }
                    channelId?.let { put("channelId", it) }
                }
            )
        )
    }
}
