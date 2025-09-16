package org.mtransit.android.commons.provider.config.news.twitter

import android.database.Cursor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.provider.config.news.NewsFeedConfig
import org.mtransit.android.commons.provider.config.news.NewsType
import java.lang.IllegalStateException

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

        fun fromCursor(cursor: Cursor) = buildList {
            while (cursor.moveToNext()) {
                add(fromCursorRow(cursor))
            }
        }

        private fun fromCursorRow(cursor: Cursor): TwitterNewsFeedConfig {
            val extraFromCursorRow = NewsFeedConfig.getExtraFromCursorRow(cursor) ?: throw IllegalStateException("Extra is missing")
            val extra = Json.parseToJsonElement(extraFromCursorRow).jsonObject
            return TwitterNewsFeedConfig(
                username = extra["username"]?.jsonPrimitive?.content ?: throw IllegalStateException("username is missing"),
                userId = extra["userId"]?.jsonPrimitive?.contentOrNull,
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
            type = NewsType.TWITTER,
            extra = Json.encodeToString(
                buildJsonObject {
                    put("username", username)
                    userId?.let { put("userId", it) }
                }
            )
        )
    }

    override fun fromCursorRow(row: Array<Any?>): NewsFeedConfig {
        val extraFromCursorRow = NewsFeedConfig.getExtraFromCursorRow(row) ?: throw IllegalStateException("Extra is missing")
        val extra = Json.parseToJsonElement(extraFromCursorRow).jsonObject
        return TwitterNewsFeedConfig(
            username = extra["username"]?.jsonPrimitive?.content ?: throw IllegalStateException("username is missing"),
            userId = extra["userId"]?.jsonPrimitive?.contentOrNull,
            target = NewsFeedConfig.getTargetFromCursorRow(row),
            lang = NewsFeedConfig.getLangFromCursorRow(row),
            color = NewsFeedConfig.getColorFromCursorRow(row),
            severity = NewsFeedConfig.getSeverityFromCursorRow(row),
            noteworthy = NewsFeedConfig.getNoteworthyFromCursorRow(row),
        )
    }
}
