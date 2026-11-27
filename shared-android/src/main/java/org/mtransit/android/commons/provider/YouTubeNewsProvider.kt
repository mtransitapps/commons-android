package org.mtransit.android.commons.provider

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.core.content.ContentProviderCompat
import com.google.gson.GsonBuilder
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.KeysIds
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.NetworkUtils
import org.mtransit.android.commons.R
import org.mtransit.android.commons.SecureStringUtils
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.StringUtils.EMPTY
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.linkifyAllURLs
import org.mtransit.android.commons.provider.agency.AgencyUtils
import org.mtransit.android.commons.provider.common.ProviderConfig
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.commons.provider.news.ConfigurableNewsProvider
import org.mtransit.android.commons.provider.news.NewsProviderContract
import org.mtransit.android.commons.provider.news.NewsTextFormatter
import org.mtransit.android.commons.provider.news.youtube.YouTubeDateAdapter
import org.mtransit.android.commons.provider.news.youtube.YouTubeNewsConfig
import org.mtransit.android.commons.provider.news.youtube.YouTubeNewsDbHelper
import org.mtransit.android.commons.provider.news.youtube.YouTubeStorage
import org.mtransit.android.commons.provider.news.youtube.YouTubeUtils
import org.mtransit.android.commons.provider.news.youtube.YouTubeV3Api
import retrofit2.create
import java.io.IOException
import java.util.Date
import java.util.Locale

// https://developers.google.com/youtube/v3
// https://developers.google.com/youtube/v3/docs/channels/list
// https://developers.google.com/youtube/v3/docs/playlistItems/list
// DO NOT MOVE: referenced in modules AndroidManifest.xml
@SuppressLint("Registered")
class YouTubeNewsProvider : ConfigurableNewsProvider() {

    companion object {
        private val LOG_TAG: String = YouTubeNewsProvider::class.java.simpleName

        private const val FORCE_REFRESH = false
        // private const val FORCE_REFRESH = true // DEBUG

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

        private const val API_MAX_RESULT = 25L // default: 5 (from 0 to 50)

        private const val BASE_HOST_URL = "https://www.googleapis.com/youtube/"

        fun createYouTubeApi(context: Context): YouTubeV3Api {
            val retrofit = NetworkUtils.makeNewRetrofitWithGson(
                baseHostUrl = BASE_HOST_URL,
                context = context,
                gsonBuilder = GsonBuilder()
                    .registerTypeAdapter(Date::class.java, YouTubeDateAdapter())
            )

            return retrofit.create()
        }
    }

    private val _authority: String by lazy {
        importedProviderConfig?.authority
            ?: ContentProviderCompat.requireContext(this).getString(R.string.youtube_authority)
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

    private val color: String by lazy {
        importedYouTubeConfig?.color.orEmpty().ifEmpty {
            ContentProviderCompat.requireContext(this).getString(R.string.youtube_color)
        }
    }

    private val userNames: List<String> by lazy {
        importedYouTubeConfig?.channelsUsername
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.youtube_channels_username).toList()
    }

    private val userNamesChannelsId: List<String> by lazy {
        importedYouTubeConfig?.channelsId
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.youtube_channels_id).toList()
    }

    private val userNamesHandles: List<String> by lazy {
        importedYouTubeConfig?.channelsHandle
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.youtube_channels_handle).toList()
    }

    private val authorUrls: List<String> by lazy {
        importedYouTubeConfig?.channelsAuthorUrl
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.youtube_channels_author_url).toList()
    }

    private val userNamesLang: List<String> by lazy {
        importedYouTubeConfig?.channelsLang
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.youtube_channels_lang).toList()
    }

    private val userNamesColors: List<String> by lazy {
        importedYouTubeConfig?.channelsColors
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.youtube_channels_colors).toList()
    }

    private val targetAuthority: String by lazy {
        importedProviderConfig?.targetAuthority
            ?: ContentProviderCompat.requireContext(this).getString(R.string.youtube_target_for_poi_authority)
    }

    private val userNamesTarget: List<String> by lazy {
        importedYouTubeConfig?.channelsTarget
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.youtube_channels_target).toList()
    }

    private val userNamesSeverity: List<Int> by lazy {
        importedYouTubeConfig?.channelsSeverity
            ?: ContentProviderCompat.requireContext(this).resources.getIntArray(R.array.youtube_channels_severity).toList()
    }

    @get:IntegerRes
    private val userNamesSeverityDefaultResId: Int get() = R.integer.news_provider_severity_info_agency

    private val userNamesNoteworthy: List<Long> by lazy {
        importedYouTubeConfig?.channelsNoteworthy
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.youtube_channels_noteworthy).toList().map { it.toLong() }
    }

    @get:StringRes
    private val userNamesNoteworthyDefaultResId: Int get() = R.string.news_provider_noteworthy_long_term

    private val languages: List<String> by lazy {
        listOf<String>(
            if (LocaleUtils.isFR()) Locale.FRENCH.language else Locale.ENGLISH.language,
            LocaleUtils.UNKNOWN
        )
    }

    override fun getLogTag() = LOG_TAG

    @Volatile
    private var importedYouTubeConfig: YouTubeNewsConfig? = null

    override fun configureNewsConfig(config: ProviderConfig) {
        require(config.type == ProviderConfig.TYPE_NEWS_YOUTUBE) { "Unsupported YouTube config type" }
        importedYouTubeConfig = sharedJson.decodeFromJsonElement<YouTubeNewsConfig>(config.params)
    }

    override fun getProviderConfig(): ProviderConfig {
        importedProviderConfig?.let { return it }
        val context = requireContextCompat()
        return ProviderConfig.create(
            type = ProviderConfig.TYPE_NEWS_YOUTUBE,
            authority = _authority,
            targetAuthority = targetAuthority.takeIf { it.isNotBlank() } ?: AgencyUtils.getAgencyAuthority(context).orEmpty(),
            params = sharedJson.encodeToJsonElement(
                YouTubeNewsConfig(
                    color = color.takeIf { it.isNotBlank() } ?: AgencyUtils.getAgencyColor(context),
                    channelsUsername = userNames,
                    channelsId = userNamesChannelsId,
                    channelsHandle = userNamesHandles,
                    channelsAuthorUrl = authorUrls,
                    channelsLang = userNamesLang,
                    channelsColors = userNamesColors,
                    channelsTarget = userNamesTarget,
                    channelsSeverity = userNamesSeverity,
                    channelsNoteworthy = userNamesNoteworthy,
                    defaultSeverity = context.resources.getInteger(userNamesSeverityDefaultResId),
                    defaultNoteworthy = context.getString(userNamesNoteworthyDefaultResId).toLong(),
                )
            ).jsonObject,
            secrets = mapOf(KeysIds.YOUTUBE_API_KEY to _apiKey),
        )
    }

    override fun closeConfiguredRuntime() {
        super.closeConfiguredRuntime()
        _storage = null
    }

    override fun getCurrentDbVersion() = YouTubeNewsDbHelper.getDbVersion(requireContextCompat())

    override fun getNewDbHelper(context: Context) = YouTubeNewsDbHelper(
        context.applicationContext,
        getStorage(context),
        dbNamePrefix = importedProviderConfig?.authorityStoragePrefix,
    )

    override fun getMinDurationBetweenNewsRefreshInMs(inFocusOrDefault: Boolean) =
        super.getMinDurationBetweenNewsRefreshInMs(inFocusOrDefault)
            .times(10L) // long term / infrequent news
            .times(2L) // API key

    override val newsMaxValidityInMs: Long get() = super.newsMaxValidityInMs.takeUnless { FORCE_REFRESH } ?: 0L

    override fun getNewsValidityInMs(inFocusOrDefault: Boolean) =
        super.getNewsValidityInMs(inFocusOrDefault)
            .times(10L) // long term / infrequent news
            .times(2L) // API key

    override fun purgeUselessCachedNews(): Boolean {
        return purgeUselessCachedNews(this)
    }

    override fun deleteCachedNews(id: Int?): Boolean {
        return deleteCachedNews(this, id)
    }

    private fun deleteAllAgencyNewsData(): Int {
        var affectedRows = 0
        try {
            val selection = SqlUtils.getWhereEqualsString(
                NewsProviderContract.Columns.T_NEWS_K_SOURCE_ID,
                AGENCY_SOURCE_ID
            )
            affectedRows = this.getDBHelper().writableDatabase.delete(newsDbTableName, selection, null)
        } catch (e: Exception) {
            MTLogK.w(this, "Error while deleting all agency news data!", e)
        }
        return affectedRows
    }

    override val authority: String get() = _authority

    override val authorityUri: Uri get() = _authorityUri

    override fun cacheNews(newNews: ArrayList<News>) {
        cacheNewsS(this, newNews)
    }

    override fun getCachedNews(newsFilter: NewsProviderContract.Filter): ArrayList<News>? {
        return getCachedNewsS(this, newsFilter)
    }

    override fun getNewNews(newsFilter: NewsProviderContract.Filter): ArrayList<News>? {
        this.providedApiKey = SecureStringUtils.dec(newsFilter.getProvidedEncryptKey(KeysIds.YOUTUBE_API_KEY))
        try {
            updateAgencyNewsDataIfRequired(requireContextCompat(), newsFilter.isInFocusOrDefault)
            return getCachedNews(newsFilter)
        } finally {
            this.providedApiKey = null
        }
    }

    private fun updateAgencyNewsDataIfRequired(context: Context, inFocus: Boolean) {
        if (FORCE_REFRESH) {
            getStorage(context).saveLastUpdate(null, null) // force refresh
        }
        val lastUpdateInMs = getStorage(context).getLastUpdateMs(0L)
        val lastUpdateLang = getStorage(context).getLastUpdateLang(EMPTY)
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
        val lastUpdateInMs = getStorage(context).getLastUpdateMs(0L)
        val lastUpdateLang = getStorage(context).getLastUpdateLang(EMPTY)
        if (lastUpdateInMs > lastLastUpdateInMs // IF new more recent last update DO
            && LocaleUtils.getDefaultLanguage() == lastUpdateLang
        ) {
            return // too late, another thread already updated
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
        @Suppress("SimplifyBooleanWithConstants")
        if (deleteAllRequired || FORCE_REFRESH) {
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
            getStorage(context).saveLastUpdate(nowInMs, LocaleUtils.getDefaultLanguage()) // sync
        } // else keep whatever we have until max validity reached
    }

    @Volatile
    private var _storage: YouTubeStorage? = null

    private fun getStorage(context: Context) = _storage ?: synchronized(this) {
        _storage ?: YouTubeStorage(
            context.applicationContext,
            prefix = importedProviderConfig?.authorityStoragePrefix,
        ).also { _storage = it }
    }

    private var _youtubeApi: YouTubeV3Api? = null

    private fun getYouTubeApi(context: Context) =
        _youtubeApi ?: createYouTubeApi(context).also { _youtubeApi = it }

    private fun loadAgencyNewsDataFromWWW(context: Context): ArrayList<News>? {
        val apiKey = this.providedApiKey?.takeIf { it.isNotBlank() }
            ?: this._apiKey.takeIf { it.isNotBlank() }
            ?: return null
        val youTubeApi = getYouTubeApi(context)
        try {
            val newNews = ArrayList<News>()
            val maxValidityInMs = newsMaxValidityInMs
            val authority = _authority
            for ((i, authorUrl) in authorUrls.withIndex()) {
                loadUserUploadsPlaylist(
                    context,
                    youTubeApi,
                    apiKey,
                    newNews,
                    maxValidityInMs,
                    authority,
                    i,
                    authorUrl
                )
            }
            MTLogK.i(this, "Loaded ${newNews.size} news.")
            return newNews
        } catch (ioe: IOException) {
            MTLogK.e(LOG_TAG, "I/O ERROR: Unknown Exception", ioe)
            return null
        } catch (e: Exception) {
            MTLogK.e(LOG_TAG, "INTERNAL ERROR: Unknown Exception", e)
            return null
        }
    }

    private fun loadUserUploadsPlaylist(
        context: Context,
        youTubeApi: YouTubeV3Api,
        apiKey: String,
        newNews: MutableList<News>,
        maxValidityInMs: Long,
        authority: String,
        i: Int,
        authorUrl: String,
    ) {
        val (usernameFromAuthorUrl, userHandleFromAuthorUrl, customUrlFromAuthorUrl, channelIdFromAuthorUrl) =
            YouTubeUtils.pickChannelIdFromAuthorUrl(authorUrl)
        val userLog = usernameFromAuthorUrl ?: userHandleFromAuthorUrl ?: customUrlFromAuthorUrl ?: channelIdFromAuthorUrl ?: authorUrl
        val userLang = userNamesLang.getOrNull(i) ?: LocaleUtils.UNKNOWN
        if (LocaleUtils.MULTIPLE != userLang
            && LocaleUtils.UNKNOWN != userLang
            && LocaleUtils.getDefaultLanguage() != userLang
        ) {
            MTLogK.d(this, "SKIP loading '$userLog': different language ($userLang).")
            return
        }
        // 1 - load user channel uploads
        val (id, username, handle) = when {
            usernameFromAuthorUrl?.isNotBlank() == true -> Triple(null, usernameFromAuthorUrl, null)
            userHandleFromAuthorUrl?.isNotBlank() == true -> Triple(null, null, userHandleFromAuthorUrl)
            channelIdFromAuthorUrl?.isNotBlank() == true -> Triple(channelIdFromAuthorUrl, null, null)
            userNames.getOrNull(i)?.isNotBlank() == true -> Triple(null, userNames[i], null)
            userNamesHandles.getOrNull(i)?.isNotBlank() == true -> Triple(null, null, userNamesHandles[i])
            userNamesChannelsId.getOrNull(i)?.isNotBlank() == true -> Triple(userNamesChannelsId[i], null, null)
            customUrlFromAuthorUrl?.isNotBlank() == true -> {
                MTLogK.w(this, "SKIP loading '$userLog': custom URL '$authorUrl' not supported!")
                return
            }
            else -> Triple(null, null, null)
        }
        if (id == null && username == null && handle == null) {
            MTLogK.w(this, "SKIP loading '$userLog': no channel ID/username/handle found for '$authorUrl'.")
            return
        }
        val channelListResp = youTubeApi.getChannels(
            part = CHANNEL_PARTS.joinToString(separator = ","),
            forUsername = username,
            forHandle = handle,
            id = id,
            hl = when {
                LocaleUtils.isFR() -> Locale.FRENCH.language
                else -> Locale.ENGLISH.language
            },
            key = apiKey,
        ).execute().body()
        MTLogK.d(this, "Found ${channelListResp?.items?.size} channel for '$userLog'.")
        val channel = channelListResp?.items?.firstOrNull() ?: run {
            MTLogK.d(this, "SKIP loading '$userLog': no channel found.")
            return
        }
        val uploadsPlaylistId = channel.contentDetails?.relatedPlaylists?.uploads
        if (uploadsPlaylistId.isNullOrEmpty()) {
            MTLogK.d(this, "SKIP loading '$userLog': no uploads playlist found.")
            return
        }
        MTLogK.i(this, "Found uploads playlist '$uploadsPlaylistId' for '$userLog'.")
        val channelSnippet = channel.snippet
        val authorUsername = channelSnippet?.customUrl
        val authorName = channelSnippet?.localized?.title
            ?: channelSnippet?.title
            ?: usernameFromAuthorUrl
            ?: userHandleFromAuthorUrl
            ?: userNames.getOrNull(i)?.takeIf { it.isNotBlank() }
            ?: userNamesHandles.getOrNull(i)?.takeIf { it.isNotBlank() }
            ?: run {
                MTLogK.w(this, "SKIP loading '$userLog': no author name found!")
                return
            }
        // https://developers.google.com/youtube/v3/docs/thumbnails
        val authorPictureURL = channelSnippet?.thumbnailDetails?.let { thumbnails ->
            thumbnails.medium?.url // 240x240
                ?: thumbnails.default?.url // 88x88
                ?: thumbnails.high?.url // 800x800
                ?: thumbnails.standard?.url // ? 800x800
                ?: thumbnails.maxres?.url // ? 800x800
        }

        // 2 - load user channel uploads
        val newLastUpdateInMs = TimeUtils.currentTimeMillis()
        val playlistItemsListResp = youTubeApi.getPlaylistItems(
            part = PLAYLIST_ITEMS_PARTS.joinToString(separator = ","),
            playlistId = uploadsPlaylistId,
            maxResults = API_MAX_RESULT,
            hl = if (LocaleUtils.isFR()) Locale.FRENCH.language else Locale.ENGLISH.language,
            key = apiKey,
        ).execute().body()
        MTLogK.i(this, "Found ${playlistItemsListResp?.items?.size} videos for '$userLog'.")
        playlistItemsListResp?.items
            ?.filter { it?.status?.privacyStatus != "private" }
            ?.forEach { playlistItem ->
                val snippet = playlistItem?.snippet ?: return@forEach
                val videoId = snippet.resourceId?.videoId ?: return@forEach
                val id = playlistItem.id ?: return@forEach
                val uuid = AGENCY_SOURCE_ID + id
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
                        val descriptionLinkify = description.linkifyAllURLs()
                        val toHTML = HtmlUtils.toHTML(descriptionLinkify)
                        append(toHTML)
                    }
                    link.takeIf { it.isNotBlank() }?.let { link ->
                        if (isNotEmpty()) {
                            append(HtmlUtils.BR).append(HtmlUtils.BR)
                        }
                        append(HtmlUtils.linkify(link))
                    }
                }
                if (text.isEmpty() || textHtml.isEmpty()) {
                    MTLogK.w(this, "loadUserUploadsPlaylist() > skip (no text)")
                    return
                }
                // https://developers.google.com/youtube/v3/docs/thumbnails
                val thumbnail = snippet.thumbnailDetails?.let { thumbnails ->
                    thumbnails.medium?.url // 320x180 no black bars
                        ?: thumbnails.maxres?.url // 1280x720 no black bars
                        ?: thumbnails.standard?.url // 640x480 with black bars
                        ?: thumbnails.high?.url // 480x360 with black bars
                        ?: thumbnails.default?.url // 120x90 with black bars
                }
                val colorString = userNamesColors.getOrNull(i)
                    ?: color.takeIf { it.isNotBlank() }
                    ?: AgencyUtils.getAgencyColor(context)
                    ?: ColorUtils.BLACK
                        .also {
                            MTLogK.w(this, "No color found for '$userLog'! (used fallback)")
                        }
                val targetUUID = userNamesTarget.getOrNull(i)?.takeIf { it.isNotBlank() }
                    ?: targetAuthority.takeIf { it.isNotBlank() }
                    ?: AgencyUtils.getAgencyAuthority(context)
                    ?: EMPTY.takeIf { context.packageName == Constants.MAIN_APP_PACKAGE_NAME } // target all allowed for main app
                    ?: run {
                        MTLogK.w(this, "SKIP loading '$userLog': no target UUID!")
                        return
                    }
                val severity = userNamesSeverity.getOrNull(i)
                    ?: importedYouTubeConfig?.defaultSeverity
                    ?: context.resources.getInteger(userNamesSeverityDefaultResId)
                val noteworthyInMs = userNamesNoteworthy.getOrNull(i)
                    ?: importedYouTubeConfig?.defaultNoteworthy
                    ?: context.resources.getString(userNamesNoteworthyDefaultResId).toLong()
                newNews.add(
                    News(
                        null,
                        authority,
                        uuid,
                        severity,
                        noteworthyInMs,
                        newLastUpdateInMs,
                        maxValidityInMs,
                        snippet.publishedAt?.time ?: newLastUpdateInMs,
                        targetUUID,
                        colorString,
                        authorName,
                        authorUsername,
                        authorPictureURL,
                        authorUrl,
                        text,
                        textHtml,
                        link,
                        languages.firstOrNull() ?: LocaleUtils.UNKNOWN,
                        AGENCY_SOURCE_ID,
                        AGENCY_SOURCE_LABEL,
                        listOf(thumbnail),
                    )
                )
            }
    }

    override val newsLanguages: Collection<String> get() = languages
}
