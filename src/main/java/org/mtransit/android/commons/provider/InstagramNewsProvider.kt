@file:Suppress("DEPRECATION")

package org.mtransit.android.commons.provider

import android.annotation.SuppressLint
import android.content.Context
import android.content.UriMatcher
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.core.content.ContentProviderCompat
import androidx.core.text.toHtml
import androidx.core.text.toSpanned
import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.NetworkUtils
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.R
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.provider.InstagramNewsProvider.InstagramApi.JEdgeOwnerToTimelineMediaNode
import org.mtransit.android.commons.provider.InstagramNewsProvider.InstagramApi.JProfileUser
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

@SuppressLint("Registered")
@Deprecated(message = "Public JSON API removed") // would required user token
class InstagramNewsProvider : NewsProvider() {

    companion object {
        private val LOG_TAG: String = InstagramNewsProvider::class.java.simpleName

        /**
         * Override if multiple [InstagramNewsProvider] implementations in same app.
         */
        private const val PREF_KEY_AGENCY_LAST_UPDATE_MS = InstagramNewsDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS

        /**
         * Override if multiple [InstagramNewsProvider] implementations in same app.
         */
        private const val PREF_KEY_AGENCY_LAST_UPDATE_LANG = InstagramNewsDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_LANG

        private const val NEWS_MAX_VALIDITY_IN_MS = Long.MAX_VALUE // FOREVER
        private val NEWS_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L)
        private val NEWS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.HOURS.toMillis(1L)
        private val NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(30L)
        private val NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10L)

        private const val MENTION_AND_SCREEN_NAME = "@%s"

        private const val AGENCY_SOURCE_ID = "instagram"

        private const val AGENCY_SOURCE_LABEL = "Instagram"

        private const val BASE_HOST = "instagram.com"
        private const val BASE_HOST_URL = "https://www.$BASE_HOST"

        fun createInstagramApi(): InstagramApi {
            val retrofit = NetworkUtils.makeNewRetrofitWithGson(BASE_HOST_URL)

            return retrofit.create(InstagramApi::class.java)
        }
    }

    private val _uriMatcher: UriMatcher by lazy {
        getNewUriMatcher(_authority)
    }

    private val _authority: String by lazy {
        ContentProviderCompat.requireContext(this).getString(
            R.string.instagram_authority
        )
    }

    private val _authorityUri: Uri by lazy {
        UriUtils.newContentUri(_authority)
    }

    private val _color: String by lazy {
        ContentProviderCompat.requireContext(this).getString(
            R.string.instagram_color
        )
    }

    private val _userNames: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.instagram_user_names
        ).toList()
    }

    private val _userNamesLang: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.instagram_user_names_lang
        ).toList()
    }

    private val _userNamesColors: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.instagram_user_names_colors
        ).toList()
    }

    private val _userNamesTarget: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.instagram_user_names_target
        ).toList()
    }

    private val _userNamesSeverity: List<Int> by lazy {
        ContentProviderCompat.requireContext(this).resources.getIntArray(
            R.array.instagram_user_names_severity
        ).toList()
    }

    private val _userNamesNoteworthy: List<Long> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.instagram_user_names_noteworthy
        ).toList().map { it.toLong() }
    }

    private val _languages: List<String> by lazy {
        listOf<String>(
            if (LocaleUtils.isFR()) Locale.FRENCH.language else Locale.ENGLISH.language,
            LocaleUtils.UNKNOWN
        )
    }

    override fun getLogTag() = LOG_TAG

    override fun getURI_MATCHER() = _uriMatcher

    private var _dbHelper: InstagramNewsDbHelper? = null
    private var _currentDbVersion: Int = -1

    private fun getDBHelper(context: Context): InstagramNewsDbHelper {
        when (val currentDbHelper: InstagramNewsDbHelper? = _dbHelper) {
            null -> {  // initialize
                val newDbHelper = getNewDbHelper(context)
                _dbHelper = newDbHelper
                _currentDbVersion = currentDbVersion
                return newDbHelper
            }
            else -> {
                return try {
                    if (_currentDbVersion == currentDbVersion) {
                        _dbHelper?.close()
                        _dbHelper = null
                        getDBHelper(context)
                    } else {
                        currentDbHelper
                    }
                } catch (e: Exception) { // fail if locked, will try again later
                    MTLog.w(this, e, "Can't check DB version!")
                    currentDbHelper
                }
            }
        }
    }

    /**
     * Override if multiple [InstagramNewsDbHelper] implementations in same app.
     */
    override fun getCurrentDbVersion() = _currentDbVersion

    override fun getNewDbHelper(context: Context): InstagramNewsDbHelper {
        return InstagramNewsDbHelper(context.applicationContext)
    }

    // override
    fun getDBHelper() = getDBHelper(ContentProviderCompat.requireContext(this))

    override fun getMinDurationBetweenNewsRefreshInMs(inFocus: Boolean) = if (inFocus) {
        NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS
    } else NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_MS

    override fun getNewsMaxValidityInMs() = NEWS_MAX_VALIDITY_IN_MS

    override fun getNewsValidityInMs(inFocus: Boolean) = if (inFocus) {
        NEWS_VALIDITY_IN_FOCUS_IN_MS
    } else NEWS_VALIDITY_IN_MS

    override fun purgeUselessCachedNews(): Boolean {
        return purgeUselessCachedNews(this)
    }

    override fun deleteCachedNews(newsId: Int?): Boolean {
        return deleteCachedNews(this, newsId)
    }

    private fun deleteAllAgencyNewsData(): Int {
        var affectedRows = 0
        try {
            val selection = SqlUtils.getWhereEqualsString(
                NewsProviderContract.Columns.T_NEWS_K_SOURCE_ID,
                AGENCY_SOURCE_ID
            )
            affectedRows = this.getDBHelper().writableDatabase.delete(newsDbTableName, selection, null)
        } catch (e: java.lang.Exception) {
            MTLog.w(
                this,
                e,
                "Error while deleting all agency news data!"
            )
        }
        return affectedRows
    }

    override fun getAuthority() = _authority

    override fun getAuthorityUri() = _authorityUri

    override fun cacheNews(newNews: ArrayList<News>) {
        cacheNewsS(this, newNews)
    }

    override fun getCachedNews(newsFilter: NewsProviderContract.Filter): ArrayList<News>? {
        return getCachedNewsS(this, newsFilter)
    }

    override fun getNewNews(newsFilter: NewsProviderContract.Filter): ArrayList<News>? {
        updateAgencyNewsDataIfRequired(requireContextCompat(), newsFilter.isInFocusOrDefault)
        return getCachedNews(newsFilter)
    }

    private fun updateAgencyNewsDataIfRequired(context: Context, inFocus: Boolean) {
        val lastUpdateInMs =
            PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L)
        val lastUpdateLang =
            PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_LANG, StringUtils.EMPTY)
        val minUpdateMs = newsMaxValidityInMs.coerceAtMost(getNewsValidityInMs(inFocus))
        val nowInMs = TimeUtils.currentTimeMillis()
        if (lastUpdateInMs + minUpdateMs > nowInMs && LocaleUtils.getDefaultLanguage() == lastUpdateLang) {
            return
        }
        updateAgencyNewsDataIfRequiredSync(context, lastUpdateInMs, inFocus)
    }

    @Synchronized
    private fun updateAgencyNewsDataIfRequiredSync(
        context: Context,
        lastLastUpdateInMs: Long,
        inFocus: Boolean
    ) {
        val lastUpdateInMs =
            PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L)
        val lastUpdateLang =
            PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_LANG, StringUtils.EMPTY)
        if (lastUpdateInMs > lastLastUpdateInMs // IF new more recent last update DO
            && LocaleUtils.getDefaultLanguage() == lastUpdateLang
        ) {
            return  // too late, another thread already updated
        }
        val nowInMs = TimeUtils.currentTimeMillis()
        var deleteAllRequired = false
        if (lastUpdateInMs + newsMaxValidityInMs < nowInMs
            || LocaleUtils.getDefaultLanguage() != lastUpdateLang
        ) {
            deleteAllRequired = true // too old to display
        }
        val minUpdateMs = newsMaxValidityInMs.coerceAtMost(getNewsValidityInMs(inFocus))
        if (deleteAllRequired
            || lastUpdateInMs + minUpdateMs < nowInMs
        ) {
            updateAllAgencyNewsDataFromWWW(context, deleteAllRequired) // try to update
        }
    }

    private fun updateAllAgencyNewsDataFromWWW(context: Context, deleteAllRequired: Boolean) {
        var deleteAllDone = false
        if (deleteAllRequired) {
            deleteAllAgencyNewsData()
            deleteAllDone = true
        }
        val newNews: ArrayList<News>? = loadAgencyNewsDataFromWWW()
        if (newNews != null) { // empty is OK
            val nowInMs = TimeUtils.currentTimeMillis()
            if (!deleteAllDone) {
                deleteAllAgencyNewsData()
            }
            cacheNews(newNews)
            PreferenceUtils.savePrefLclSync(
                context,
                PREF_KEY_AGENCY_LAST_UPDATE_MS,
                nowInMs,
            ) // sync
            PreferenceUtils.savePrefLclSync(
                context,
                PREF_KEY_AGENCY_LAST_UPDATE_LANG,
                LocaleUtils.getDefaultLanguage(),
            ) // sync
        } // else keep whatever we have until max validity reached
    }

    private val instagramApi by lazy {
        createInstagramApi()
    }

    private fun loadAgencyNewsDataFromWWW(): ArrayList<News>? {
        @Suppress("ConstantConditionIf")
        if (true) {
            return null // Public JSON API removed
        }
        try {
            val newNews = ArrayList<News>()
            val maxValidityInMs = newsMaxValidityInMs
            val authority = _authority
            for ((i, userName) in _userNames.withIndex()) {
                loadUserTimeline(
                    instagramApi,
                    newNews,
                    maxValidityInMs,
                    authority,
                    i,
                    userName
                )
            }
            MTLog.i(this, "Loaded %d news.", newNews.size)
            return newNews
        } catch (ioe: IOException) {
            MTLog.e(LOG_TAG, ioe, "I/O ERROR: Unknown Exception")
            return null
        } catch (e: Exception) {
            MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception")
            return null
        }
    }

    private fun loadUserTimeline(
        instagramApi: InstagramApi,
        newNews: MutableList<News>,
        maxValidityInMs: Long,
        authority: String,
        i: Int,
        username: String
    ) {
        val userLang = _userNamesLang[i]
        if (LocaleUtils.MULTIPLE != userLang
            && LocaleUtils.UNKNOWN != userLang
            && LocaleUtils.getDefaultLanguage() != userLang
        ) {
            MTLog.d(this, "SKIP loading '$username': different language ($userLang).")
            return
        }
        val newLastUpdateInMs = TimeUtils.currentTimeMillis()
        MTLog.i(this, "Loading '@$username' posts from '$BASE_HOST'...")
        val response = instagramApi.userProfile(username).execute()
        val target = _userNamesTarget[i]
        val severity = _userNamesSeverity[i]
        val noteworthyInMs = _userNamesNoteworthy[i]
        if (response.isSuccessful) {
            val responseBody = response.body()
            val user = responseBody?.graphQL?.user
            val timelineMediaEdges = user?.edgeOwnerToTimelineMedia?.edges
            timelineMediaEdges
                ?.mapNotNull { it?.node }
                ?.forEach { timelineMedia ->
                    readNews(
                        timelineMedia,
                        authority,
                        severity,
                        noteworthyInMs,
                        newLastUpdateInMs,
                        maxValidityInMs,
                        target,
                        username,
                        user,
                        userLang
                    ).apply {
                        newNews.add(this)
                    }
                }
        } else {
            MTLog.w(
                this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)",
                response.code(),
                response.message()
            )
        }
    }

    private fun readNews(
        timelineMedia: JEdgeOwnerToTimelineMediaNode,
        authority: String,
        severity: Int,
        noteworthyInMs: Long,
        newLastUpdateInMs: Long,
        maxValidityInMs: Long,
        target: String,
        username: String,
        user: JProfileUser,
        userLang: String
    ): News {
        val mediaToCaptions = timelineMedia.edgeMediaToCaption?.edges
        val captionText =
            mediaToCaptions
                ?.map { edge -> edge?.node?.text }
                ?.first { text -> text?.isNotEmpty() ?: false }
                ?: StringUtils.EMPTY
        val createdAtInMs = if (timelineMedia.takenAtTimestampInSec != null) {
            TimeUnit.SECONDS.toMillis(timelineMedia.takenAtTimestampInSec)
        } else {
            newLastUpdateInMs - TimeUnit.DAYS.toMillis(99L)
        }
        val webURL = "https://www.instagram.com/p/" + timelineMedia.shortCode
        val textHTMLSb = StringBuilder()
        textHTMLSb.append(
            HtmlUtils.toHTML(
                captionText.toSpanned().toHtml()
            )
        ) // TODO #Hashtags @mentions links
        appendVideoAndGIF(textHTMLSb, timelineMedia)
        if (textHTMLSb.isNotEmpty()) {
            textHTMLSb.append(HtmlUtils.BR).append(HtmlUtils.BR)
        }
        textHTMLSb.append(HtmlUtils.linkify(webURL))
        return News(
            null,
            authority,
            AGENCY_SOURCE_ID + timelineMedia.id,
            severity,
            noteworthyInMs,
            newLastUpdateInMs,
            maxValidityInMs,
            createdAtInMs,
            target,
            getColor(username),
            user.fullName ?: user.username ?: username,
            String.format(MENTION_AND_SCREEN_NAME, user.username ?: username),
            user.profilePicUrl,
            "https://www.instagram.com/" + user.username,
            StringUtils.oneLineOneSpace(
                HtmlUtils.fromHtml(captionText).toString()
            ),
            textHTMLSb.toString(),
            webURL,
            userLang,
            AGENCY_SOURCE_ID,
            AGENCY_SOURCE_LABEL,
            readImages(timelineMedia)
        )
    }

    private fun appendVideoAndGIF(sb: StringBuilder, timelineMediaNode: JEdgeOwnerToTimelineMediaNode) {
        if (timelineMediaNode.isVideo == true
            && timelineMediaNode.videoUrl != null
        ) {
            appendVideo(sb, timelineMediaNode.videoUrl)
        }
        timelineMediaNode.edgeSidecarToChildren?.edges
            ?.mapNotNull { edge -> edge?.node }
            ?.filter { node -> node.isVideo == true }
            ?.mapNotNull { node -> node.videoUrl }
            ?.forEach { videoUrl ->
                appendVideo(sb, videoUrl)
            }
    }

    private fun appendVideo(sb: StringBuilder, videoUrl: String) {
        if (sb.isEmpty()) {
            sb.append(HtmlUtils.BR)
        }
        sb.append(HtmlUtils.BR)
        sb.append("VIDEO").append(": ")
        sb.append(HtmlUtils.linkify(videoUrl))
    }

    private fun readImages(timelineMediaNode: JEdgeOwnerToTimelineMediaNode): List<String> {
        timelineMediaNode.edgeSidecarToChildren?.edges?.let { edges ->
            return edges
                .map { edge -> edge?.node }
                .filter { node -> node?.isVideo != true } // no support for video, yet
                .mapNotNull { node -> node?.displayUrl }
                .toList()
        }
        if (timelineMediaNode.isVideo == true) {
            return emptyList() // no support for video, yet
        }
        return listOf(
            timelineMediaNode.displayUrl
                ?: timelineMediaNode.thumbnailSrc
                ?: StringUtils.EMPTY
        )
    }

    interface InstagramApi {

        // @GET("/{username}/?__a=1")
        // https://www.instagram.com/stminfo/?__a=1&__d=dis
        @GET("/{username}/?__a=1&__d=dis")
        fun userProfile(@Path("username") username: String): Call<JProfilePageResponse?>

        data class JProfilePageResponse(
            @SerializedName("graphql")
            val graphQL: JProfileGraphQL?
        )

        data class JProfileGraphQL(
            @SerializedName("user")
            val user: JProfileUser?
        )

        data class JProfileUser(
            @SerializedName("full_name")
            val fullName: String?,
            @SerializedName("id")
            val id: String?,
            @SerializedName("profile_pic_url")
            val profilePicUrl: String?,
            @SerializedName("profile_pic_url_hd")
            val profilePicUrlHD: String?,
            @SerializedName("username")
            val username: String?,
            @SerializedName("edge_owner_to_timeline_media")
            val edgeOwnerToTimelineMedia: JEdgeOwnerToTimelineMedia?
        )

        data class JEdgeOwnerToTimelineMedia(
            @SerializedName("edges")
            val edges: ArrayList<JEdgeOwnerToTimelineMediaEdge?>?
        )

        data class JEdgeOwnerToTimelineMediaEdge(
            @SerializedName("node")
            val node: JEdgeOwnerToTimelineMediaNode?
        )

        data class JEdgeOwnerToTimelineMediaNode(
            @SerializedName("id")
            val id: String?,
            @SerializedName("shortcode")
            val shortCode: String?,
            @SerializedName("display_url")
            val displayUrl: String?,
            @SerializedName("is_video")
            val isVideo: Boolean?,
            @SerializedName("video_url")
            val videoUrl: String?,
            @SerializedName("edge_media_to_caption")
            val edgeMediaToCaption: JEdgeMediaToCaption?,
            @SerializedName("taken_at_timestamp")
            val takenAtTimestampInSec: Long?,
            @SerializedName("thumbnail_src")
            val thumbnailSrc: String?,
            @SerializedName("edge_sidecar_to_children")
            val edgeSidecarToChildren: JEdgeSidecarToChildren?
        )

        data class JEdgeMediaToCaption(
            @SerializedName("edges")
            val edges: ArrayList<JEdgeMediaToCaptionEdge?>?
        )

        data class JEdgeMediaToCaptionEdge(
            @SerializedName("node")
            val node: JEdgeMediaToCaptionNode?
        )

        data class JEdgeMediaToCaptionNode(
            @SerializedName("text")
            val text: String?
        )

        data class JEdgeSidecarToChildren(
            @SerializedName("edges")
            val edges: ArrayList<JEdgeSidecarToChildrenEdge?>?
        )

        data class JEdgeSidecarToChildrenEdge(
            @SerializedName("node")
            val node: JEdgeSidecarToChildrenNode?
        )

        data class JEdgeSidecarToChildrenNode(
            @SerializedName("display_url")
            val displayUrl: String?,
            @SerializedName("is_video")
            val isVideo: Boolean?,
            @SerializedName("video_url")
            val videoUrl: String?
        )
    }

    override fun getNewsLanguages() = _languages

    private fun getColor(
        username: String
    ): String {
        return try {
            _userNamesColors[_userNames.indexOf(username)]
        } catch (e: java.lang.Exception) {
            MTLog.w(
                this,
                "Error while finding user color '%s'!",
                username
            )
            _color
        }
    }

    class InstagramNewsDbHelper(
        val context: Context,
        dbName: String = DB_NAME,
        dbVersion: Int = getDbVersion(context)
    ) : NewsDbHelper(context, dbName, dbVersion) {

        companion object {
            private val LOG_TAG: String = InstagramNewsDbHelper::class.java.simpleName

            /**
             * Override if multiple [InstagramNewsDbHelper] implementations in same app.
             */
            private const val DB_NAME = "news_instagram.db"

            /**
             * Override if multiple [InstagramNewsDbHelper] implementations in same app.
             */
            const val PREF_KEY_AGENCY_LAST_UPDATE_MS = "pInstagramNewsLastUpdate"

            /**
             * Override if multiple [InstagramNewsDbHelper] implementations in same app.
             */
            const val PREF_KEY_AGENCY_LAST_UPDATE_LANG = "pInstagramNewsLastUpdateLang"

            private const val T_INSTAGRAM_NEWS = T_NEWS

            private val T_INSTAGRAM_NEWS_SQL_CREATE = getSqlCreateBuilder(T_INSTAGRAM_NEWS).build()

            private val T_INSTAGRAM_NEWS_SQL_DROP =
                SqlUtils.getSQLDropIfExistsQuery(T_INSTAGRAM_NEWS)

            private var dbVersion = -1

            /**
             * Override if multiple [InstagramNewsDbHelper] in same app.
             */
            fun getDbVersion(context: Context): Int {
                if (dbVersion < 0) {
                    dbVersion = context.resources.getInteger(R.integer.instagram_db_version)
                    dbVersion++ // add news articles images URLs do DB -> FORCE DB update
                }
                return dbVersion
            }
        }

        override fun getLogTag() = LOG_TAG

        override fun getDbName(): String {
            return DB_NAME
        }

        override fun onCreateMT(db: SQLiteDatabase) {
            initAllDbTables(db)
        }

        override fun onUpgradeMT(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL(T_INSTAGRAM_NEWS_SQL_DROP)
            PreferenceUtils.savePrefLclSync(
                context,
                PREF_KEY_AGENCY_LAST_UPDATE_MS,
                0L,
            )
            PreferenceUtils.savePrefLclSync(
                context,
                PREF_KEY_AGENCY_LAST_UPDATE_LANG,
                StringUtils.EMPTY,
            )
            initAllDbTables(db)
        }

        private fun initAllDbTables(db: SQLiteDatabase) {
            db.execSQL(T_INSTAGRAM_NEWS_SQL_CREATE)
        }
    }
}