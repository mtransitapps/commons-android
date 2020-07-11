package org.mtransit.android.commons.data

import android.content.ContentValues
import android.database.Cursor
import android.text.TextUtils
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.NewsProviderContract
import java.util.ArrayList
import java.util.Comparator

@Suppress("unused", "MemberVisibilityCanBePrivate")
data class NewsArticle(
    var id: Int?, // internal DB ID (useful to delete) OR NULL
    val authority: String,
    val uUID: String,
    val severity: Int,
    val noteworthyInMs: Long,
    val lastUpdateInMs: Long,
    val maxValidityInMs: Long,
    val createdAtInMs: Long,
    val targetUUID: String,
    val color: String,
    val authorName: String,
    val authorUsername: String?,
    val authorPictureURL: String?,
    val authorProfileURL: String,
    val text: String,
    val _textHTML: String?,
    val webURL: String,
    val language: String,
    val sourceId: String,
    val sourceLabel: String,
    val imageUrls: List<String>
) : MTLog.Loggable {

    override fun getLogTag(): String {
        return LOG_TAG
    }

    val isUseful: Boolean
        get() = lastUpdateInMs + maxValidityInMs >= TimeUtils.currentTimeMillis()

    val authorOneLine: String
        get() {
            return if (authorUsername.isNullOrBlank()) {
                authorName
            } else "$authorName ($authorUsername)"
        }

    fun hasColor(): Boolean {
        return !TextUtils.isEmpty(color)
    }

    val colorInt: Int by lazy {
        ColorUtils.parseColor(color)
    }

    val textHTML: String by lazy {
        if (_textHTML.isNullOrBlank()) {
            text
        } else {
            _textHTML
        }
    }

    val hasAuthorPictureURL: Boolean
        get() {
            return authorPictureURL?.isNotBlank() ?: false
        }

    val hasValidImageUrls: Boolean
        get() {
            return imageUrls.any { it.isNotBlank() }
        }

    val firstValidImageUrl: String
        get() = imageUrls.first { it.isNotBlank() }

    private fun getImageUrl(index: Int): String {
        return if (index < imageUrls.size) imageUrls[index] else StringUtils.EMPTY
    }

    fun toContentValues(): ContentValues {
        val contentValues = ContentValues()
        if (id != null) {
            contentValues.put(NewsProviderContract.Columns.T_NEWS_K_ID, id)
        } // ELSE AUTO INCREMENT
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_UUID, uUID)
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_SEVERITY, severity)
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_NOTEWORTHY, noteworthyInMs)
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_LAST_UPDATE, lastUpdateInMs)
        contentValues.put(
            NewsProviderContract.Columns.T_NEWS_K_MAX_VALIDITY_IN_MS,
            maxValidityInMs
        )
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_CREATED_AT, createdAtInMs)
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_TARGET_UUID, targetUUID)
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_COLOR, color)
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_NAME, authorName)
        contentValues.put(
            NewsProviderContract.Columns.T_NEWS_K_AUTHOR_USERNAME,
            authorUsername
        )
        contentValues.put(
            NewsProviderContract.Columns.T_NEWS_K_AUTHOR_PICTURE_URL,
            authorPictureURL
        )
        contentValues.put(
            NewsProviderContract.Columns.T_NEWS_K_AUTHOR_PROFILE_URL,
            authorProfileURL
        )
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_TEXT, text)
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_TEXT_HTML, textHTML)
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_WEB_URL, webURL)
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_LANGUAGE, language)
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_SOURCE_ID, sourceId)
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_SOURCE_LABEL, sourceLabel)
        var count = imageUrls.size
        count = count.coerceAtMost(10) // MAX 10 for now
        contentValues.put(NewsProviderContract.Columns.T_NEWS_K_IMAGE_URLS_COUNT, count)
        for (i in 0 until count) {
            contentValues.put(
                NewsProviderContract.Columns.T_NEWS_K_IMAGE_URL_INDEX + i,
                imageUrls[i]
            )
        }
        return contentValues
    }

    /**
     * [NewsProviderContract.PROJECTION_NEWS]
     */
    val cursorRow: Array<Any?>
        get() = arrayOf( //
            id,  //
            authority,  //
            uUID,  //
            severity,
            noteworthyInMs,
            lastUpdateInMs,
            maxValidityInMs,
            createdAtInMs,
            targetUUID,
            color,
            authorName,
            authorUsername,
            authorPictureURL,
            authorProfileURL,
            text,
            textHTML,
            webURL,
            language,
            sourceId,
            sourceLabel,
            imageUrls.size,
            getImageUrl(0),
            getImageUrl(1),
            getImageUrl(2),
            getImageUrl(3),
            getImageUrl(4),
            getImageUrl(5),
            getImageUrl(6),
            getImageUrl(7),
            getImageUrl(8),
            getImageUrl(9)
        )

    class NewsComparator : Comparator<NewsArticle?> {
        override fun compare(lhs: NewsArticle?, rhs: NewsArticle?): Int {
            val lCreatedAtInMs = lhs?.createdAtInMs ?: 0L
            val rCreatedAtInMs = rhs?.createdAtInMs ?: 0L
            return rCreatedAtInMs.compareTo(lCreatedAtInMs)
        }
    }

    class NewsSeverityComparator : Comparator<NewsArticle?> {
        override fun compare(lhs: NewsArticle?, rhs: NewsArticle?): Int {
            val lSeverity = lhs?.severity ?: 0
            val rSeverity = rhs?.severity ?: 0
            if (lSeverity != rSeverity) {
                return rSeverity - lSeverity
            }
            val lCreatedAtInMs = lhs?.createdAtInMs ?: 0L
            val rCreatedAtInMs = rhs?.createdAtInMs ?: 0L
            return rCreatedAtInMs.compareTo(lCreatedAtInMs)
        }
    }

    companion object {

        private val LOG_TAG = NewsArticle::class.java.simpleName

        @JvmField
        val NEWS_COMPARATOR = NewsComparator()

        @JvmField
        val NEWS_SEVERITY_COMPARATOR = NewsSeverityComparator()

        @JvmStatic
        fun fromCursorStatic(cursor: Cursor, authority: String): NewsArticle {
            val idIdx = cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_ID)
            val id = if (cursor.isNull(idIdx)) null else cursor.getInt(idIdx)
            val uuid =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_UUID))
            val severity =
                cursor.getInt(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_SEVERITY))
            val noteworthyInMs =
                cursor.getLong(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_NOTEWORTHY))
            val lastUpdateInMs =
                cursor.getLong(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_LAST_UPDATE))
            val maxValidityInMs =
                cursor.getLong(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_MAX_VALIDITY_IN_MS))
            val createdAtInMs =
                cursor.getLong(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_CREATED_AT))
            val targetUUID =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_TARGET_UUID))
            val color =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_COLOR))
            val authorName =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_NAME))
            val authorUsername =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_USERNAME))
            val authorPictureURL =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_PICTURE_URL))
            val authorProfileURL =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_AUTHOR_PROFILE_URL))
            val text =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_TEXT))
            val textHTML =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_TEXT_HTML))
            val webURL =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_WEB_URL))
            val language =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_LANGUAGE))
            val sourceId =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_SOURCE_ID))
            val sourceLabel =
                cursor.getString(cursor.getColumnIndexOrThrow(NewsProviderContract.Columns.T_NEWS_K_SOURCE_LABEL))
            val imageUrls: MutableList<String> = ArrayList()
            var count =
                cursor.getColumnIndex(NewsProviderContract.Columns.T_NEWS_K_IMAGE_URLS_COUNT)
            if (count > 0) {
                count = count.coerceAtMost(10) // MAX 10 for now
                for (i in 0 until count) {
                    val index =
                        cursor.getColumnIndex(NewsProviderContract.Columns.T_NEWS_K_IMAGE_URL_INDEX + i)
                    if (index >= 0) {
                        val string: String = cursor.getString(index) ?: StringUtils.EMPTY
                        imageUrls.add(i, string)
                    }
                }
            }
            return NewsArticle(
                id,
                authority,
                uuid,
                severity,
                noteworthyInMs,
                lastUpdateInMs,
                maxValidityInMs,
                createdAtInMs,
                targetUUID,
                color,
                authorName,
                authorUsername,
                authorPictureURL,
                authorProfileURL,
                text,
                textHTML,
                webURL,
                language,
                sourceId,
                sourceLabel,
                imageUrls
            )
        }
    }
}