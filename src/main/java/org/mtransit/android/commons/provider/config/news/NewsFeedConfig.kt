package org.mtransit.android.commons.provider.config.news

import android.database.Cursor
import androidx.core.database.getStringOrNull
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
    fun toCursorRow(): Array<Any>

    fun makeCursorRow(extra: String? = null) = arrayOf<Any>(
        target.orEmpty(),
        lang,
        color.orEmpty(),
        severity,
        noteworthy,
        extra.orEmpty(),
    )

    fun fromCursorRow(row: Array<Any?>): NewsFeedConfig

    companion object {
        fun getTargetFromCursorRow(row: Array<Any?>): String? = (row.getOrNull(0) as? String)?.takeIf { it.isNotEmpty() }
        fun getLangFromCursorRow(row: Array<Any?>): String = row[1] as String
        fun getColorFromCursorRow(row: Array<Any?>): String? = (row.getOrNull(2) as? String)?.takeIf { it.isNotEmpty() }
        fun getSeverityFromCursorRow(row: Array<Any?>): Int = row[3] as Int
        fun getNoteworthyFromCursorRow(row: Array<Any?>): Long = row[4] as Long
        fun getExtraFromCursorRow(row: Array<Any?>): String? = (row.getOrNull(5) as? String)?.takeIf { it.isNotEmpty() }

        fun getTargetFromCursorRow(cursor: Cursor): String? = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("target"))
        fun getLangFromCursorRow(cursor: Cursor): String = cursor.getString(cursor.getColumnIndexOrThrow("lang"))
        fun getColorFromCursorRow(cursor: Cursor): String? = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("color"))
        fun getSeverityFromCursorRow(cursor: Cursor): Int = cursor.getInt(cursor.getColumnIndexOrThrow("severity"))
        fun getNoteworthyFromCursorRow(cursor: Cursor): Long = cursor.getLong(cursor.getColumnIndexOrThrow("noteworthy"))
        fun getExtraFromCursorRow(cursor: Cursor): String? = cursor.getStringOrNull(cursor.getColumnIndexOrThrow("extra"))
    }
}