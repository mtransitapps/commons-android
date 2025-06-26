package org.mtransit.android.commons.provider.config.news.instagram

import android.database.Cursor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.provider.config.news.NewsFeedConfig
import org.mtransit.android.commons.provider.config.news.rss.RSSNewsFeedConfig
import java.lang.IllegalStateException

data class InstagramNewsFeedConfig(
    val username: String,
    override val target: String? = null,
    override val lang: String = LocaleUtils.UNKNOWN, // show all
    override val color: String? = null,
    override val severity: Int = SEVERITY_DEFAULT,
    override val noteworthy: Long = NOTEWORTHY_DEFAULT,
) : NewsFeedConfig {

    companion object {
        const val SEVERITY_DEFAULT = 2 // news_provider_severity_info_agency
        const val NOTEWORTHY_DEFAULT = 604_800_000L // news_provider_noteworthy_long_term // 1 week

        fun fromCursor(cursor: Cursor) = buildList {
            while (cursor.moveToNext()) {
                add(fromCursorRow(cursor))
            }
        }

        private fun fromCursorRow(cursor: Cursor): InstagramNewsFeedConfig {
            val extraFromCursorRow = NewsFeedConfig.getExtraFromCursorRow(cursor) ?: throw IllegalStateException("Extra is missing")
            val extra = Json.parseToJsonElement(extraFromCursorRow).jsonObject
            return InstagramNewsFeedConfig(
                username = extra["username"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing username"),
                target = NewsFeedConfig.getTargetFromCursorRow(cursor),
                lang = NewsFeedConfig.getLangFromCursorRow(cursor),
                color = NewsFeedConfig.getColorFromCursorRow(cursor),
                severity = NewsFeedConfig.getSeverityFromCursorRow(cursor),
                noteworthy = NewsFeedConfig.getNoteworthyFromCursorRow(cursor),
            )
        }
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

    override fun fromCursorRow(row: Array<Any?>): NewsFeedConfig {
        val extraFromCursorRow = NewsFeedConfig.getExtraFromCursorRow(row) ?: throw IllegalStateException("Extra is missing")
        val extra = Json.parseToJsonElement(extraFromCursorRow).jsonObject
        return InstagramNewsFeedConfig(
            username = extra["username"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing username"),
            target = NewsFeedConfig.getTargetFromCursorRow(row),
            lang = NewsFeedConfig.getLangFromCursorRow(row),
            color = NewsFeedConfig.getColorFromCursorRow(row),
            severity = NewsFeedConfig.getSeverityFromCursorRow(row),
            noteworthy = NewsFeedConfig.getNoteworthyFromCursorRow(row),
        )
    }
}
