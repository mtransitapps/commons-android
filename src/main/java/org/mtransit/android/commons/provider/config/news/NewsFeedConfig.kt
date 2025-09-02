package org.mtransit.android.commons.provider.config.news

import android.database.Cursor
import androidx.core.database.getStringOrNull
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.provider.NewsProviderContract.FeedConfigColumns

interface NewsFeedConfig {
    val target: String? get() = null // target the entire agency
    val lang: String get() = LocaleUtils.UNKNOWN // show all
    val color: String? get() = null // same as agency (target?) color
    val severity: Int get() = 1 // news_provider_severity_info_unknown
    val noteworthy: Long get() = 86_400_000L // news_provider_noteworthy_info // 1 days

    /**
     * See [FeedConfigColumns]
     * See [org.mtransit.android.commons.provider.NewsProviderContract.PROJECTION_NEWS_FEED_CONFIG]
     */
    fun toCursorRow(): Array<Any>

    fun makeCursorRow(type: NewsType, extra: String? = null) = arrayOf<Any>(
        type.id,
        target.orEmpty(),
        lang,
        color.orEmpty(),
        severity,
        noteworthy,
        extra.orEmpty(),
    )

    fun fromCursorRow(row: Array<Any?>): NewsFeedConfig

    companion object {
        fun getTypeIdFromCursorRow(row: Array<Any?>): Int = row[0] as Int
        fun getTargetFromCursorRow(row: Array<Any?>): String? = (row.getOrNull(1) as? String)?.takeIf { it.isNotEmpty() }
        fun getLangFromCursorRow(row: Array<Any?>): String = row[2] as String
        fun getColorFromCursorRow(row: Array<Any?>): String? = (row.getOrNull(3) as? String)?.takeIf { it.isNotEmpty() }
        fun getSeverityFromCursorRow(row: Array<Any?>): Int = row[4] as Int
        fun getNoteworthyFromCursorRow(row: Array<Any?>): Long = row[5] as Long
        fun getExtraFromCursorRow(row: Array<Any?>): String? = (row.getOrNull(6) as? String)?.takeIf { it.isNotEmpty() }

        fun getTypeIdFromCursorRow(cursor: Cursor): Int = cursor.getInt(cursor.getColumnIndexOrThrow(FeedConfigColumns.T_NEWS_FEED_CONFIG_K_TYPE))
        fun getTargetFromCursorRow(cursor: Cursor): String? = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(FeedConfigColumns.T_NEWS_FEED_CONFIG_K_TARGET))
        fun getLangFromCursorRow(cursor: Cursor): String = cursor.getString(cursor.getColumnIndexOrThrow(FeedConfigColumns.T_NEWS_FEED_CONFIG_K_LANG))
        fun getColorFromCursorRow(cursor: Cursor): String? = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(FeedConfigColumns.T_NEWS_FEED_CONFIG_K_COLOR))
        fun getSeverityFromCursorRow(cursor: Cursor): Int = cursor.getInt(cursor.getColumnIndexOrThrow(FeedConfigColumns.T_NEWS_FEED_CONFIG_K_SEVERITY))
        fun getNoteworthyFromCursorRow(cursor: Cursor): Long = cursor.getLong(cursor.getColumnIndexOrThrow(FeedConfigColumns.T_NEWS_FEED_CONFIG_K_NOTEWORTHY))
        fun getExtraFromCursorRow(cursor: Cursor): String? = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(FeedConfigColumns.T_NEWS_FEED_CONFIG_K_EXTRA))
    }
}