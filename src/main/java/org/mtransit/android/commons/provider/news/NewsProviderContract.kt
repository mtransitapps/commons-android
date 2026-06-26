package org.mtransit.android.commons.provider.news

import android.annotation.SuppressLint
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.annotation.Discouraged
import androidx.collection.ArrayMap
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.provider.common.ProviderContract
import org.mtransit.commons.model.Secret

interface NewsProviderContract : ProviderContract {

    companion object {
        const val NEWS_PATH = "news"

        const val REMOVE_IMAGE_FROM_TEXT: Boolean = false // TODO later

        @JvmStatic
        val PROJECTION_NEWS = arrayOf(
            Columns.T_NEWS_K_ID,
            Columns.T_NEWS_K_AUTHORITY_META,
            Columns.T_NEWS_K_UUID,
            Columns.T_NEWS_K_SEVERITY,
            Columns.T_NEWS_K_NOTEWORTHY,
            Columns.T_NEWS_K_LAST_UPDATE,
            Columns.T_NEWS_K_MAX_VALIDITY_IN_MS,
            Columns.T_NEWS_K_CREATED_AT,
            Columns.T_NEWS_K_TARGET_UUID,
            Columns.T_NEWS_K_COLOR,
            Columns.T_NEWS_K_AUTHOR_NAME,
            Columns.T_NEWS_K_AUTHOR_USERNAME,
            Columns.T_NEWS_K_AUTHOR_PICTURE_URL,
            Columns.T_NEWS_K_AUTHOR_PROFILE_URL,
            Columns.T_NEWS_K_TEXT,
            Columns.T_NEWS_K_TEXT_HTML,
            Columns.T_NEWS_K_WEB_URL,
            Columns.T_NEWS_K_LANGUAGE,
            Columns.T_NEWS_K_SOURCE_ID,
            Columns.T_NEWS_K_SOURCE_LABEL,
            Columns.T_NEWS_K_IMAGE_URLS_COUNT,
            Columns.T_NEWS_K_IMAGE_URL_INDEX + 0,
            Columns.T_NEWS_K_IMAGE_URL_INDEX + 1,
            Columns.T_NEWS_K_IMAGE_URL_INDEX + 2,
            Columns.T_NEWS_K_IMAGE_URL_INDEX + 3,
            Columns.T_NEWS_K_IMAGE_URL_INDEX + 4,
            Columns.T_NEWS_K_IMAGE_URL_INDEX + 5,
            Columns.T_NEWS_K_IMAGE_URL_INDEX + 6,
            Columns.T_NEWS_K_IMAGE_URL_INDEX + 7,
            Columns.T_NEWS_K_IMAGE_URL_INDEX + 8,
            Columns.T_NEWS_K_IMAGE_URL_INDEX + 9,
        )
    }

    val authority: String

    val authorityUri: Uri

    fun getNewsFromDB(newsFilter: Filter): Cursor?

    val newsDbTableName: String

    val newsProjection: Array<String>

    val newsProjectionMap: ArrayMap<String, String>

    fun cacheNews(newNews: ArrayList<News>)

    fun getCachedNews(newsFilter: Filter): ArrayList<News>?

    fun getNewNews(newsFilter: Filter): ArrayList<News>?

    fun purgeUselessCachedNews(): Boolean

    fun deleteCachedNews(id: Int?): Boolean

    val newsMaxValidityInMs: Long

    fun getNewsValidityInMs(inFocusOrDefault: Boolean): Long

    fun getMinDurationBetweenNewsRefreshInMs(inFocusOrDefault: Boolean): Long

    val newsLanguages: Collection<String>

    interface Columns {
        companion object {
            const val T_NEWS_K_ID: String = BaseColumns._ID
            const val T_NEWS_K_AUTHORITY_META = "authority"
            const val T_NEWS_K_UUID = "uuid"
            const val T_NEWS_K_SEVERITY = "severity"
            const val T_NEWS_K_NOTEWORTHY = "noteworthy"
            const val T_NEWS_K_LAST_UPDATE = "last_update"
            const val T_NEWS_K_CREATED_AT = "created_at"
            const val T_NEWS_K_MAX_VALIDITY_IN_MS = "max_validity"
            const val T_NEWS_K_TARGET_UUID = "target"
            const val T_NEWS_K_COLOR = "color"
            const val T_NEWS_K_AUTHOR_NAME = "author_name"
            const val T_NEWS_K_AUTHOR_USERNAME = "author_username"
            const val T_NEWS_K_AUTHOR_PICTURE_URL = "author_picture_url"
            const val T_NEWS_K_AUTHOR_PROFILE_URL = "author_profile_url"
            const val T_NEWS_K_TEXT = "text"
            const val T_NEWS_K_TEXT_HTML = "text_html"
            const val T_NEWS_K_WEB_URL = "web_url"
            const val T_NEWS_K_LANGUAGE = "lang"
            const val T_NEWS_K_SOURCE_ID = "source_id"
            const val T_NEWS_K_SOURCE_LABEL = "source_label"
            const val T_NEWS_K_IMAGE_URLS_COUNT = "image_urls_count"
            const val T_NEWS_K_IMAGE_URL_INDEX = "image_urls_"
        }
    }

    data class Filter @Discouraged("use static methods instead") constructor(
        override val cacheOnly: Boolean? = null,
        override val cacheValidityInMs: Long? = null,
        override val inFocus: Boolean? = null,
        override val providedEncryptKeysMap: Secret<Map<String, String>>? = null,
        private val articlesUUIDs: List<String>? = null,
        private val targetsUUIDs: List<String>? = null,
        private val minCreatedAtInMs: Long? = null,
    ) : ProviderContract.Filter(), MTLog.Loggable {

        companion object {
            private val LOG_TAG = NewsProviderContract::class.java.getSimpleName() + ">" + Filter::class.java.getSimpleName()

            @SuppressLint("DiscouragedApi")
            @JvmStatic
            fun newEmptyFilter() = Filter()

            @JvmStatic
            fun newArticleUUIDFilter(articleUUID: String) = newArticlesUUIDsFilter(listOf(articleUUID))

            @SuppressLint("DiscouragedApi")
            @JvmStatic
            fun newArticlesUUIDsFilter(articlesUUIDs: List<String>) = Filter(articlesUUIDs = articlesUUIDs)

            @JvmStatic
            fun newPOIFilter(poi: POI) = newTargetsUUIDsFilter(poi.toTargetsUUIDs())

            @JvmStatic
            fun POI.toTargetsUUIDs(): List<String> = buildList {
                add(getAuthority())
                if (this@toTargetsUUIDs is RouteDirectionStop) {
                    add(POI.POIUtils.makeUUID(authority, route.id))
                }
            }

            @SuppressLint("DiscouragedApi")
            @JvmStatic
            fun newTargetsUUIDsFilter(targetsUUIDs: List<String>) = Filter(targetsUUIDs = targetsUUIDs)

            @JvmStatic
            fun fromJSONString(jsonString: String?): Filter? {
                try {
                    return jsonString?.let { fromJSON(JSONObject(it)) }
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while parsing JSON string '%s'", jsonString)
                    return null
                }
            }

            private const val JSON_ARTICLE_UUIDS = "uuids" // article UUIDs
            private const val JSON_TARGETS_UUIDS = "targets" // POI UUIDs
            private const val JSON_MIN_CREATED_AT_IN_MS = "minCreatedAtInMs"

            fun fromJSON(json: JSONObject): Filter? {
                try {
                    //noinspection DiscouragedApi
                    return Filter(
                        cacheOnly = getCacheOnlyFromJSON(json),
                        cacheValidityInMs = getCacheValidityInMsFromJSON(json),
                        inFocus = getInFocusFromJSON(json),
                        providedEncryptKeysMap = getProvidedEncryptKeysMapFromJSON(json),
                        articlesUUIDs = json.optJSONArray(JSON_ARTICLE_UUIDS)?.takeIf { it.length() > 0 }?.let { jArticleUUIDs ->
                            buildList {
                                for (i in 0..<jArticleUUIDs.length()) {
                                    add(jArticleUUIDs.getString(i))
                                }
                            }

                        },
                        targetsUUIDs = json.optJSONArray(JSON_TARGETS_UUIDS)?.takeIf { it.length() > 0 }?.let { jTargetUUIDs ->
                            buildList {
                                for (i in 0..<jTargetUUIDs.length()) {
                                    add(jTargetUUIDs.getString(i))
                                }
                            }
                        },
                        minCreatedAtInMs = json.takeIf { it.has(JSON_MIN_CREATED_AT_IN_MS) }?.getLong(JSON_MIN_CREATED_AT_IN_MS)
                    )
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while parsing JSON object '$json'")
                    return null
                }
            }

            fun toJSONString(newsFilter: Filter) = toJSON(newsFilter)?.toString()

            fun toJSON(newsFilter: Filter): JSONObject? {
                try {
                    return JSONObject().apply {
                        toJSON(newsFilter, this)
                        if (newsFilter.minCreatedAtInMs != null) {
                            put(JSON_MIN_CREATED_AT_IN_MS, newsFilter.minCreatedAtInMs)
                        }
                        if (newsFilter.isArticlesUUIDFilter && newsFilter.articlesUUIDs != null) {
                            val jArticleUUIDs = JSONArray()
                            for (articleUUID in newsFilter.articlesUUIDs) {
                                jArticleUUIDs.put(articleUUID)
                            }
                            put(JSON_ARTICLE_UUIDS, jArticleUUIDs)
                        } else if (newsFilter.isTargetsUUIDFilter && newsFilter.targetsUUIDs != null) {
                            val jTargetUUIDs = JSONArray()
                            for (targetUUID in newsFilter.targetsUUIDs) {
                                jTargetUUIDs.put(targetUUID)
                            }
                            put(JSON_TARGETS_UUIDS, jTargetUUIDs)
                        }
                    }
                } catch (jsone: JSONException) {
                    MTLog.w(LOG_TAG, jsone, "Error while making JSON object '$newsFilter'")
                    return null
                }
            }
        }

        init {
            if (articlesUUIDs != null && articlesUUIDs.isEmpty()) {
                throw UnsupportedOperationException("Need at least 1 article UUID!")
            }
            if (targetsUUIDs != null && targetsUUIDs.isEmpty()) {
                throw UnsupportedOperationException("Need at least 1 target UUID!")
            }
        }

        override fun getLogTag() = LOG_TAG

        val isArticlesUUIDFilter = articlesUUIDs?.isNotEmpty() == true
        val isTargetsUUIDFilter = targetsUUIDs?.isNotEmpty() == true

        override fun toString() = buildString {
            append(Filter::class.java.getSimpleName()).append('[')
            if (isArticlesUUIDFilter) {
                append("articleUUIDs:").append(articlesUUIDs).append(',')
            } else if (isTargetsUUIDFilter) {
                append("targetsUUIDs:").append(targetsUUIDs).append(',')
            }
            append(super.toStringParts())
            append("minCreatedAtInMs:").append(minCreatedAtInMs)
            append(']')
        }

        @Suppress("unused")
        fun toStringTargetsAndUuid(): String {
            val sb = StringBuilder(Filter::class.java.getSimpleName()).append('[')
            if (isArticlesUUIDFilter) {
                sb.append("articleUUIDs:").append(this.articlesUUIDs).append(',')
            } else if (isTargetsUUIDFilter) {
                sb.append("targetsUUIDs:").append(this.targetsUUIDs).append(',')
            }
            sb.append(']')
            return sb.toString()
        }

        fun getSqlSelection(uuidTableColumn: String, targetColumn: String, createdAtColumn: String) = buildString {
            if (isArticlesUUIDFilter) {
                append(SqlUtils.getWhereInString(uuidTableColumn, articlesUUIDs))
            } else if (isTargetsUUIDFilter) {
                append(SqlUtils.getWhereInString(targetColumn, targetsUUIDs))
            }
            minCreatedAtInMs?.let {
                if (isNotEmpty()) append(SqlUtils.AND)
                append(SqlUtils.getWhereSuperior(createdAtColumn, it))
            }
        }

        override fun toJSONString() = toJSONString(this)
    }
}
