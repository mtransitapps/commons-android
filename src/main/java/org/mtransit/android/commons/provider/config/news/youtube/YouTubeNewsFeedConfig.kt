package org.mtransit.android.commons.provider.config.news.youtube

import android.database.Cursor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.provider.config.news.NewsFeedConfig
import org.mtransit.android.commons.provider.config.news.NewsType
import java.lang.IllegalStateException

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

        fun fromCursor(cursor: Cursor) = buildList {
            while (cursor.moveToNext()) {
                add(fromCursorRow(cursor))
            }
        }

        private fun fromCursorRow(cursor: Cursor): YouTubeNewsFeedConfig {
            val extraFromCursorRow = NewsFeedConfig.getExtraFromCursorRow(cursor) ?: throw IllegalStateException("Extra is missing")
            val extra = Json.parseToJsonElement(extraFromCursorRow).jsonObject
            return YouTubeNewsFeedConfig(
                authorUrl = extra["authorUrl"]?.jsonPrimitive?.content ?: throw IllegalStateException("authorUrl is missing"),
                username = extra["username"]?.jsonPrimitive?.contentOrNull,
                userHandle = extra["userHandle"]?.jsonPrimitive?.contentOrNull,
                channelId = extra["channelId"]?.jsonPrimitive?.contentOrNull,
                target = NewsFeedConfig.getTargetFromCursorRow(cursor),
                color = NewsFeedConfig.getColorFromCursorRow(cursor),
                lang = NewsFeedConfig.getLangFromCursorRow(cursor),
                severity = NewsFeedConfig.getSeverityFromCursorRow(cursor),
                noteworthy = NewsFeedConfig.getNoteworthyFromCursorRow(cursor),
            )
        }
    }

    override fun toCursorRow(): Array<Any> {
        return makeCursorRow(
            type = NewsType.YOUTUBE,
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

    override fun fromCursorRow(row: Array<Any?>): NewsFeedConfig {
        val extraFromCursorRow = NewsFeedConfig.getExtraFromCursorRow(row) ?: throw IllegalStateException("Extra is missing")
        val extra = Json.parseToJsonElement(extraFromCursorRow).jsonObject
        return YouTubeNewsFeedConfig(
            authorUrl = extra["authorUrl"]?.jsonPrimitive?.content ?: throw IllegalStateException("authorUrl is missing"),
            username = extra["username"]?.jsonPrimitive?.contentOrNull,
            userHandle = extra["userHandle"]?.jsonPrimitive?.contentOrNull,
            channelId = extra["channelId"]?.jsonPrimitive?.contentOrNull,
            target = NewsFeedConfig.getTargetFromCursorRow(row),
            color = NewsFeedConfig.getColorFromCursorRow(row),
            lang = NewsFeedConfig.getLangFromCursorRow(row),
            severity = NewsFeedConfig.getSeverityFromCursorRow(row),
            noteworthy = NewsFeedConfig.getNoteworthyFromCursorRow(row),
        )
    }
}
