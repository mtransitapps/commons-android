package org.mtransit.android.commons.provider.config.news.rss

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.provider.config.news.NewsFeedConfig

data class RSSNewsFeedConfig(
    val url: String, // rss feed url
    val label: String? = null, // should be guessed from url
    val authorIcon: String? = null,
    val authorName: String? = null, // fallback to agency short name
    val authorUrl: String,
    val encoding: String = ENCODING_DEFAULT,
    val copyToFile: Boolean = COPY_FILE_DEFAULT, // instead of streaming
    val ignoreGUID: Boolean = IGNORE_GUID_DEFAULT,
    val ignoreLink: Boolean = IGNORE_LINK_DEFAULT,
    override val target: String? = null,
    override val lang: String = LocaleUtils.UNKNOWN, // show all
    override val color: String? = null,
    override val severity: Int = SEVERITY_DEFAULT,
    override val noteworthy: Long = NOTEWORTHY_DEFAULT,
) : NewsFeedConfig {

    companion object {
        const val SEVERITY_DEFAULT = 2 // news_provider_severity_info_agency
        const val NOTEWORTHY_DEFAULT = 10_800_000L // news_provider_noteworthy_warning // 3 hours

        const val ENCODING_DEFAULT = "UTF-8"
        const val COPY_FILE_DEFAULT = false
        const val IGNORE_GUID_DEFAULT = false
        const val IGNORE_LINK_DEFAULT = false
    }

    override fun toCursorRow(): Array<Any> {
        return makeCursorRow(
            extra = Json.encodeToString(
                buildJsonObject {
                    put("url", url)
                    label?.let { put("label", it) }
                    authorIcon?.let { put("authorIcon", it) }
                    authorName?.let { put("authorName", it) }
                    put("authorUrl", authorUrl)
                    put("encoding", encoding)
                    if (copyToFile != COPY_FILE_DEFAULT) put("copyToFile", copyToFile)
                    if (ignoreGUID != IGNORE_GUID_DEFAULT) put("ignoreGUID", ignoreGUID)
                    if (ignoreLink != IGNORE_LINK_DEFAULT) put("ignoreLink", ignoreLink)

                }
            )
        )
    }
}
