package org.mtransit.android.commons.provider

import android.content.Context
import android.content.UriMatcher
import android.net.Uri
import androidx.core.content.ContentProviderCompat
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequestInitializer
import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.KeysIds
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLog
import java.util.concurrent.TimeUnit
import org.mtransit.android.commons.R
import org.mtransit.android.commons.SecureStringUtils
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.provider.news.NewsTextFormatter
import org.mtransit.android.commons.provider.news.youtube.YouTubeNewsDbHelper
import org.mtransit.android.commons.provider.news.youtube.YouTubeStorage
import java.io.IOException
import java.util.Locale

// https://developers.google.com/youtube/v3
// https://developers.google.com/youtube/v3/docs/channels/list
// https://developers.google.com/youtube/v3/docs/playlistItems/list
class YouTubeNewsProvider : NewsProvider() {

    companion object {
        private val LOG_TAG: String = YouTubeNewsProvider::class.java.simpleName

        private val NEWS_MAX_VALIDITY_IN_MS = MAX_CACHE_VALIDITY_MS
        private val NEWS_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(14L)
        private val NEWS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.DAYS.toMillis(7L)
        private val NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.HOURS.toMillis(48L)
        private val NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.HOURS.toMillis(12L)

        const val YOUTUBE_VIDEO_PROFILE_URL = "https://www.youtube.com/user/%s"

        const val YOUTUBE_VIDEO_URL_BEFORE_ID = "https://www.youtube.com/watch?v="
        const val YOUTUBE_VIDEO_LINK_AND_VIDEO_ID = "$YOUTUBE_VIDEO_URL_BEFORE_ID%s"

        private const val AGENCY_SOURCE_ID = "youtube"

        private const val AGENCY_SOURCE_LABEL = "YouTube"

        // https://developers.google.com/youtube/v3/docs/channels/list#parameters
        private val CHANNEL_PARTS = listOf(
            "snippet",
            "contentDetails",
        )

        // https://developers.google.com/youtube/v3/docs/playlistItems/list#parameters
        private val PLAYLIST_ITEMS_PARTS = listOf(
            "snippet",
        )

        private const val API_MAX_RESULT = 10L // default: 5 (from 0 to 50)
    }

    private val _uriMatcher: UriMatcher by lazy {
        getNewUriMatcher(_authority)
    }

    private val _authority: String by lazy {
        ContentProviderCompat.requireContext(this).getString(
            R.string.youtube_authority
        )
    }

    private val _authorityUri: Uri by lazy {
        UriUtils.newContentUri(_authority)
    }

    private val _apiKey: String by lazy {
        ContentProviderCompat.requireContext(this).getString(
            R.string.youtube_api_key
        )
    }

    private var providedApiKey: String? = null

    private val _color: String by lazy {
        ContentProviderCompat.requireContext(this).getString(
            R.string.youtube_color
        )
    }

    private val _userNames: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.youtube_channels_username
        ).toList()
    }

    private val _userNamesLang: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.youtube_channels_lang
        ).toList()
    }

    private val _userNamesColors: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.youtube_channels_colors
        ).toList()
    }

    private val _userNamesTarget: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.youtube_channels_target
        ).toList()
    }

    private val _userNamesSeverity: List<Int> by lazy {
        ContentProviderCompat.requireContext(this).resources.getIntArray(
            R.array.youtube_channels_severity
        ).toList()
    }

    private val _userNamesNoteworthy: List<Long> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.youtube_channels_noteworthy
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

    private var _dbHelper: YouTubeNewsDbHelper? = null
    private var _currentDbVersion: Int = -1

    private fun getDBHelper(context: Context): YouTubeNewsDbHelper {
        when (val currentDbHelper: YouTubeNewsDbHelper? = _dbHelper) {
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
     * Override if multiple [YouTubeNewsDbHelper] implementations in same app.
     */
    override fun getCurrentDbVersion() = _currentDbVersion

    override fun getNewDbHelper(context: Context): YouTubeNewsDbHelper {
        return YouTubeNewsDbHelper(context.applicationContext)
    }

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
        this.providedApiKey = SecureStringUtils.dec(newsFilter.getProvidedEncryptKey(KeysIds.YOUTUBE_API_KEY))
        updateAgencyNewsDataIfRequired(requireContextCompat(), newsFilter.isInFocusOrDefault)
        return getCachedNews(newsFilter)
    }

    private fun updateAgencyNewsDataIfRequired(context: Context, inFocus: Boolean) {
        val lastUpdateInMs = YouTubeStorage.getLastUpdateMs(context, 0L)
        val lastUpdateLang = YouTubeStorage.getLastUpdateLang(context, StringUtils.EMPTY)
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
        inFocus: Boolean,
    ) {
        val lastUpdateInMs = YouTubeStorage.getLastUpdateMs(context, 0L)
        val lastUpdateLang = YouTubeStorage.getLastUpdateLang(context, StringUtils.EMPTY)
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
        val newNews: ArrayList<News>? = loadAgencyNewsDataFromWWW(context)
        if (newNews != null) { // empty is OK
            val nowInMs = TimeUtils.currentTimeMillis()
            if (!deleteAllDone) {
                deleteAllAgencyNewsData()
            }
            cacheNews(newNews)
            YouTubeStorage.saveLastUpdateMs(context, nowInMs) // sync
            YouTubeStorage.saveLastUpdateLang(context, LocaleUtils.getDefaultLanguage()) // sync
        } // else keep whatever we have until max validity reached
    }

    private var _youTubeService: YouTube? = null

    private fun getYouTubeService(context: Context) =
        _youTubeService ?: createYouTubeService(context).also { _youTubeService = it }

    private fun createYouTubeService(context: Context) = (this.providedApiKey ?: this._apiKey).takeIf { it.isNotBlank() }?.let { apiKey ->
        YouTube.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            null,
        )
            .setApplicationName(context.getString(R.string.app_name))
            .setGoogleClientRequestInitializer(YouTubeRequestInitializer(apiKey))
            .build()
    }

    private fun loadAgencyNewsDataFromWWW(context: Context): ArrayList<News>? {
        val youtubeService: YouTube = getYouTubeService(context) ?: return null
        try {
            val newNews = ArrayList<News>()
            val maxValidityInMs = newsMaxValidityInMs
            val authority = _authority
            for ((i, userName) in _userNames.withIndex()) {
                loadUserUploadsPlaylist(
                    youtubeService,
                    newNews,
                    maxValidityInMs,
                    authority,
                    i,
                    userName
                )
            }
            MTLog.i(this, "Loaded ${newNews.size} news.")
            return newNews
        } catch (ioe: IOException) {
            MTLog.e(LOG_TAG, ioe, "I/O ERROR: Unknown Exception")
            return null
        } catch (e: Exception) {
            MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception")
            return null
        }
    }

    private fun loadUserUploadsPlaylist(
        youTubeService: YouTube,
        newNews: MutableList<News>,
        maxValidityInMs: Long,
        authority: String,
        i: Int,
        username: String,
    ) {
        val userLang = _userNamesLang[i]
        if (LocaleUtils.MULTIPLE != userLang
            && LocaleUtils.UNKNOWN != userLang
            && LocaleUtils.getDefaultLanguage() != userLang
        ) {
            MTLog.d(this, "SKIP loading '$username': different language ($userLang).")
            return
        }
        // 1 - load user channel uploads playlist
        val channelListResp = youTubeService
            .channels()
            .list(CHANNEL_PARTS)
            .setForUsername(username)
            .setHl(if (LocaleUtils.isFR()) Locale.FRENCH.language else Locale.ENGLISH.language)
            .execute()
        MTLog.d(this, "Found ${channelListResp?.items?.size} channel for '$username'.")
        val channel = channelListResp?.items?.firstOrNull() ?: run {
            MTLog.d(this, "SKIP loading '$username': no channel found.")
            return
        }
        val uploadsPlaylistId = channel.contentDetails?.relatedPlaylists?.uploads
        if (uploadsPlaylistId.isNullOrEmpty()) {
            MTLog.d(this, "SKIP loading '$username': no uploads playlist found.")
            return
        }
        MTLog.i(this, "Found uploads playlist '$uploadsPlaylistId' for '$username'.")
        val channelSnippet = channel.snippet
        val authorUsername = channelSnippet.customUrl ?: username
        val authorName = channelSnippet.localized?.title ?: channelSnippet.title
        // https://developers.google.com/youtube/v3/docs/thumbnails
        val authorPictureURL = channelSnippet.thumbnails?.let { thumbnails ->
            thumbnails.medium?.url // 240x240
                ?: thumbnails.default?.url // 88x88
                ?: thumbnails.high?.url // 800x800
                ?: thumbnails.standard?.url // ? 800x800
                ?: thumbnails.maxres?.url // ? 800x800
        }

        // 2 - load user channel uploads
        val newLastUpdateInMs = TimeUtils.currentTimeMillis()
        val playlistItemsListResp = youTubeService
            .playlistItems()
            .list(PLAYLIST_ITEMS_PARTS)
            .setPlaylistId(uploadsPlaylistId)
            .setMaxResults(API_MAX_RESULT)
            .execute()
        MTLog.i(this, "Found ${playlistItemsListResp?.items?.size} videos for '$username'.")
        playlistItemsListResp?.items
            ?.filter { it?.status?.privacyStatus != "private" }
            ?.forEach { playlistItem ->
                val snippet = playlistItem.snippet ?: return@forEach
                val videoId = snippet.resourceId?.videoId ?: return@forEach
                val link = YOUTUBE_VIDEO_LINK_AND_VIDEO_ID.format(videoId)
                val text = buildString {
                    snippet.title?.takeIf { it.isNotBlank() }?.let { title ->
                        append(title)
                    }
                    snippet.description?.takeIf { it.isNotBlank() }?.let { description ->
                        if (isNotEmpty()) {
                            append(": ")
                        }
                        append(HtmlUtils.fromHtmlCompact(description))
                    }
                }
                val textHtml = buildString {
                    snippet.title?.takeIf { it.isNotBlank() }?.let { title ->
                        append(NewsTextFormatter.formatHTMLTitle(title))
                    }
                    snippet.description?.takeIf { it.isNotBlank() }?.let { description ->
                        append(NewsTextFormatter.getHTMLAfterTitleSpace(length))
                        append(
                            HtmlUtils.toHTML(
                                HtmlUtils.linkifyAllURLs(description)
                            )
                        )
                    }
                    link.takeIf { it.isNotBlank() }?.let { link ->
                        if (isNotEmpty()) {
                            append(HtmlUtils.BR).append(HtmlUtils.BR)
                        }
                        append(HtmlUtils.linkify(link))
                    }
                }
                if (text.isEmpty() || textHtml.isEmpty()) {
                    MTLog.w(this, "loadUserUploadsPlaylist() > skip (no text)")
                    return
                }
                // https://developers.google.com/youtube/v3/docs/thumbnails
                val thumbnail = snippet.thumbnails?.let { thumbnails ->
                    thumbnails.medium?.url // 320x180 no black bars
                        ?: thumbnails.maxres?.url // 1280x720 no black bars
                        ?: thumbnails.standard?.url // 640x480 with black bars
                        ?: thumbnails.high?.url // 480x360 with black bars
                        ?: thumbnails.default?.url // 120x90 with black bars
                }
                newNews.add(
                    News(
                        null,
                        authority,
                        AGENCY_SOURCE_ID + videoId,
                        _userNamesSeverity[i],
                        _userNamesNoteworthy[i],
                        newLastUpdateInMs,
                        maxValidityInMs,
                        snippet.publishedAt?.value ?: newLastUpdateInMs,
                        _userNamesTarget[i],
                        getColor(username, i),
                        authorName,
                        authorUsername,
                        authorPictureURL,
                        String.format(YOUTUBE_VIDEO_PROFILE_URL, username),
                        text,
                        textHtml,
                        link,
                        _languages[0],
                        AGENCY_SOURCE_ID,
                        AGENCY_SOURCE_LABEL,
                        listOf(thumbnail),
                    )
                )
            }
    }

    override fun getNewsLanguages() = _languages

    private fun getColor(
        username: String,
        index: Int = _userNames.indexOf(username),
    ): String {
        return try {
            _userNamesColors[index]
        } catch (e: java.lang.Exception) {
            MTLog.w(this, e, "Error while finding user color '$username'!")
            _color
        }
    }
}