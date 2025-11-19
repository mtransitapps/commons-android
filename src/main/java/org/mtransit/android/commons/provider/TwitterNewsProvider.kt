package org.mtransit.android.commons.provider

import android.annotation.SuppressLint
import android.content.Context
import android.content.UriMatcher
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat
import com.google.gson.GsonBuilder
import org.mtransit.android.commons.BuildConfig
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.KeysIds
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.NetworkUtils
import org.mtransit.android.commons.R
import org.mtransit.android.commons.SecureStringUtils
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.provider.agency.AgencyUtils
import org.mtransit.android.commons.provider.config.news.twitter.TwitterNewsFeedConfig
import org.mtransit.android.commons.provider.config.news.twitter.TwitterNewsProviderConfig
import org.mtransit.android.commons.provider.news.NewsTextFormatter
import org.mtransit.android.commons.provider.news.twitter.model.Tweet
import org.mtransit.android.commons.provider.news.twitter.model.TweetMediaType
import org.mtransit.android.commons.provider.news.twitter.TwitterV2Api
import org.mtransit.android.commons.provider.news.twitter.TwitterDateAdapter
import org.mtransit.android.commons.provider.news.twitter.TwitterNewsDbHelper
import org.mtransit.android.commons.provider.news.twitter.TwitterStorage
import org.mtransit.android.commons.provider.news.twitter.model.TweetsResponse.TweetIncludes
import org.mtransit.android.commons.provider.news.twitter.model.TweetMedia.TweetMediaVariant
import retrofit2.create
import java.io.IOException
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@SuppressLint("Registered")
class TwitterNewsProvider : NewsProvider() {

    companion object {
        private val LOG_TAG: String = TwitterNewsProvider::class.java.simpleName

        private const val FORCE_REFRESH = false
        // private const val FORCE_REFRESH = true // DEBUG

        private val VALIDITY_DEBUG_FACTOR = if (BuildConfig.DEBUG) 1L else 2L
        private val VALIDITY_EXPANSIVE_API_FACTOR = VALIDITY_DEBUG_FACTOR * 1L

        // https://docs.x.com/x-api/fundamentals/rate-limits (Basic)
        // - [GET /2/users/by/username/:username]   500 requests / 24 hours PER APP
        // - [GET /2/tweets]                        15 requests / 15 mins PER APP
        private val NEWS_MAX_VALIDITY_IN_MS = MAX_CACHE_VALIDITY_MS
        private val NEWS_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(24L) * VALIDITY_EXPANSIVE_API_FACTOR
        private val NEWS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.HOURS.toMillis(2L) * VALIDITY_EXPANSIVE_API_FACTOR
        private val NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(30L) * VALIDITY_EXPANSIVE_API_FACTOR
        private val NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(15L) * VALIDITY_EXPANSIVE_API_FACTOR

        @Suppress("unused")
        val WEB_URL_REGEX = Regex("https?://(www)?(x|twitter)\\.com/(.+)/status/(\\d+)")

        // const val WEB_URL_REGEX_GROUP_USER_SCREEN_NAME = 3
        @Suppress("unused")
        const val WEB_URL_REGEX_GROUP_TWEET_ID = 4

        private const val AGENCY_SOURCE_ID = "twitter"

        private const val AGENCY_SOURCE_LABEL = "X"

        private const val API_MAX_RESULT = 5  // min 5, default 10, max 100

        private val TWEETS_EXCLUDE = setOf(
            "replies", // not showing replies to users
            "retweets", // TODO re-enable retweets if not too expensive $$
        )

        // https://developer.x.com/en/docs/x-api/data-dictionary/object-model/tweet
        private val TWEET_FIELDS = setOf(
            "id",
            "text",
            "author_id",
            "created_at", // date
            "entities", // hashtags, mentions, urls...
            // "in_reply_to_user_id", TODO? when "retweets" not excluded $$
            "lang", // TODO? filter by lang?
        )

        // https://developer.x.com/en/docs/x-api/expansions
        private val TWEET_EXPANSIONS = setOf(
            "author_id", // USER representing the Post's author
            "attachments.media_keys", // MEDIA representing the images, videos, GIFs included in the tweet)
            // $$ "in_reply_to_user_id", // USER representing the tweet author this requested tweet is a reply of
            // $$ "referenced_tweets.id", // TWEET that this tweet is referencing (Retweet/Quoted Tweet/reply)
            // ? "referenced_tweets.id.author_id", // USER representing the author of the referenced tweet
            // ? "entities.mentions.username", // USER representing the mentioned users in the tweet
        )

        // https://developer.x.com/en/docs/x-api/data-dictionary/object-model/media
        private val MEDIA_FIELDS = setOf(
            "media_key", // ID
            "type", // ("photo", "animated_gif", "video")
            "alt_text", // ContentDescription
            "url", // photo URL (PNG)
            "preview_image_url", // GIF/VIDEO only
            "variants", // GIF/VIDEO only
            // "height",
            // "width",
            // "duration_ms", // video/gif?
        )

        // https://developer.x.com/en/docs/x-api/data-dictionary/object-model/user
        private val USER_FIELDS = setOf(
            "id", // like "329993645"
            "name", // like  "MonTransit"
            "username", // like "montransit"
            "profile_image_url", // like "https://pbs.twimg.com/profile_images/1267175364003901441/tBZNFAgA_normal.jpg"
        )

        private const val USING_CACHED_API_TOKEN = "MT-Cached"

        private const val BASE_HOST_URL = "https://api.x.com/"

        fun createTwitterApi(context: Context, baseHostUrl: String): TwitterV2Api {
            val retrofit = NetworkUtils.makeNewRetrofitWithGson(
                baseHostUrl = baseHostUrl,
                context = context,
                gsonBuilder = GsonBuilder()
                    .registerTypeAdapter(Date::class.java, TwitterDateAdapter())
            )

            return retrofit.create()
        }
    }

    private val _uriMatcher: UriMatcher by lazy {
        getNewUriMatcher(_authority)
    }

    private val _authority: String by lazy {
        ContentProviderCompat.requireContext(this).getString(
            R.string.twitter_authority
        )
    }

    private val _authorityUri: Uri by lazy {
        UriUtils.newContentUri(_authority)
    }

    private val _bearerToken: String by lazy {
        ContentProviderCompat.requireContext(this).getString(
            R.string.twitter_bearer_token
        )
    }

    private var providedCachedApiUrl: String? = null

    private var providedBearerToken: String? = null

    private val _color: String by lazy {
        ContentProviderCompat.requireContext(this).getString(
            R.string.twitter_color
        )
    }

    private val _userNames: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.twitter_screen_names
        ).toList()
    }

    private val _userNamesId: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.twitter_screen_names_id
        ).toList()
    }

    private val _userNamesLang: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.twitter_screen_names_lang
        ).toList()
    }

    private val _userNamesColors: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.twitter_screen_names_colors
        ).toList()
    }

    private val _targetAuthority: String by lazy {
        ContentProviderCompat.requireContext(this).getString(
            R.string.twitter_target_for_poi_authority
        )
    }

    private val _userNamesTarget: List<String> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.twitter_screen_names_target
        ).toList()
    }

    private val _userNamesSeverity: List<Int> by lazy {
        ContentProviderCompat.requireContext(this).resources.getIntArray(
            R.array.twitter_screen_names_severity
        ).toList()
    }

    private val _userNamesNoteworthy: List<Long> by lazy {
        ContentProviderCompat.requireContext(this).resources.getStringArray(
            R.array.twitter_screen_names_noteworthy
        ).toList().map { it.toLong() }
    }

    private val _languages: List<String> by lazy {
        listOf<String>(
            if (LocaleUtils.isFR()) Locale.FRENCH.language else Locale.ENGLISH.language,
            LocaleUtils.UNKNOWN
        )
    }

    override fun getNewsConfig() = TwitterNewsProviderConfig(
        newsFeedConfigs = _userNames.mapIndexed { i, username ->
            TwitterNewsFeedConfig(
                username = username,
                userId = _userNamesId.getOrNull(i), // optional (saved 1 request)
                lang = _userNamesLang.getOrNull(i) ?: LocaleUtils.UNKNOWN,
                color = _userNamesColors.getOrNull(i) ?: _color.takeIf { it.isNotBlank() }, // optional (fallback: agency color)
                target = _userNamesTarget.getOrNull(i)?.takeIf { it.isNotBlank() }
                    ?: _targetAuthority.takeIf { it.isNotBlank() },  // optional (fallback: agency UUID)
                severity = _userNamesSeverity.getOrNull(i) ?: TwitterNewsFeedConfig.SEVERITY_DEFAULT,
                noteworthy = _userNamesNoteworthy.getOrNull(i) ?: TwitterNewsFeedConfig.NOTEWORTHY_DEFAULT,
            )
        }
    )

    override fun getLogTag() = LOG_TAG

    override fun getURI_MATCHER() = _uriMatcher

    private var _dbHelper: TwitterNewsDbHelper? = null
    private var _currentDbVersion: Int = -1

    private fun getDBHelper(context: Context): TwitterNewsDbHelper {
        when (val currentDbHelper: TwitterNewsDbHelper? = _dbHelper) {
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
     * Override if multiple [TwitterNewsDbHelper] implementations in same app.
     */
    override fun getCurrentDbVersion() = _currentDbVersion

    override fun getNewDbHelper(context: Context): TwitterNewsDbHelper {
        return TwitterNewsDbHelper(context.applicationContext)
    }

    fun getDBHelper() = getDBHelper(ContentProviderCompat.requireContext(this))

    override fun getMinDurationBetweenNewsRefreshInMs(inFocus: Boolean) = if (inFocus) {
        NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS
    } else NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_MS

    override fun getNewsMaxValidityInMs() = NEWS_MAX_VALIDITY_IN_MS
        .takeUnless { FORCE_REFRESH } ?: 0L

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return getCachedNews(newsFilter)
        }
        this.providedBearerToken = SecureStringUtils.dec(newsFilter.getProvidedEncryptKey(KeysIds.TWITTER_BEARER_TOKEN))
        this.providedCachedApiUrl = SecureStringUtils.dec(newsFilter.getProvidedEncryptKey(KeysIds.TWITTER_CACHED_API_URL))
        updateAgencyNewsDataIfRequired(requireContextCompat(), newsFilter.isInFocusOrDefault)
        return getCachedNews(newsFilter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateAgencyNewsDataIfRequired(context: Context, inFocus: Boolean) {
        if (FORCE_REFRESH) {
            TwitterStorage.saveLastUpdateMs(context, 0L) // force refresh
            TwitterStorage.saveLastUpdateLang(context, StringUtils.EMPTY) // force refresh
        }
        val lastUpdateInMs = TwitterStorage.getLastUpdateMs(context, 0L)
        val lastUpdateLang = TwitterStorage.getLastUpdateLang(context, StringUtils.EMPTY)
        val minUpdateMs = newsMaxValidityInMs.coerceAtMost(getNewsValidityInMs(inFocus))
        val nowInMs = TimeUtils.currentTimeMillis()
        if (lastUpdateInMs + minUpdateMs > nowInMs && LocaleUtils.getDefaultLanguage() == lastUpdateLang) {
            return
        }
        updateAgencyNewsDataIfRequiredSync(context, lastUpdateInMs, inFocus)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Synchronized
    private fun updateAgencyNewsDataIfRequiredSync(
        context: Context,
        lastLastUpdateInMs: Long,
        inFocus: Boolean,
    ) {
        val lastUpdateInMs = TwitterStorage.getLastUpdateMs(context, 0L)
        val lastUpdateLang = TwitterStorage.getLastUpdateLang(context, StringUtils.EMPTY)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateAllAgencyNewsDataFromWWW(context: Context, deleteAllRequired: Boolean) {
        var deleteAllDone = false
        if (deleteAllRequired || FORCE_REFRESH) {
            deleteAllAgencyNewsData()
            deleteAllDone = true
        }
        val newNews: ArrayList<News>? = loadAgencyNewsDataFromWWW(context)
        if (newNews != null) { // empty is OK
            val nowInMs = TimeUtils.currentTimeMillis()
            @Suppress("KotlinConstantConditions", "KotlinUnreachableCode") // incremental data load, never delete old data
            if (false && !deleteAllDone) {
                deleteAllAgencyNewsData()
            }
            cacheNews(newNews)
            TwitterStorage.saveLastUpdateMs(context, nowInMs) // sync
            TwitterStorage.saveLastUpdateLang(context, LocaleUtils.getDefaultLanguage()) // sync
        } // else keep whatever we have until max validity reached
    }

    private var _twitterApi: TwitterV2Api? = null

    private fun getTwitterApi(context: Context, baseHostUrl: String) =
        _twitterApi ?: createTwitterApi(context, baseHostUrl).also { _twitterApi = it }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadAgencyNewsDataFromWWW(context: Context): ArrayList<News>? {
        // @Suppress("ConstantConditionIf")
        // if (true) {
        // return null // TOO expensive $$$
        // }
        var token: String
        val twitterApi = this.providedCachedApiUrl?.takeIf { it.isNotBlank() }?.let {
            token = USING_CACHED_API_TOKEN
            getTwitterApi(context, it)
        } ?: run {
            token = this.providedBearerToken?.takeIf { it.isNotBlank() }
                ?: this._bearerToken.takeIf { it.isNotBlank() }
                        ?: return null
            getTwitterApi(context, BASE_HOST_URL)
        }
        try {
            val newNews = ArrayList<News>()
            val maxValidityInMs = newsMaxValidityInMs
            val authority = _authority
            _userNames.forEachIndexed { i, userName ->
                loadUserTimeline(
                    context,
                    twitterApi,
                    token,
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

    private fun getURL(url: String, text: String) = HtmlUtils.linkify(url, text)

    private fun getNewsWebURL(tweet: Tweet, userScreenName: String) = "https://x.com/$userScreenName/status/${tweet.id}"

    private fun getAuthorProfileURL(userScreenName: String) = "https://x.com/$userScreenName"

    private fun getHashTagURL(hashTag: String) = "https://x.com/hashtag/$hashTag"

    private fun getUserName(screenName: String) = "@$screenName"

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadUserTimeline(
        context: Context,
        twitterApi: TwitterV2Api,
        token: String,
        newNews: MutableList<News>,
        maxValidityInMs: Long,
        authority: String,
        i: Int,
        username: String,
    ) {
        val userLang = _userNamesLang.getOrNull(i) ?: LocaleUtils.UNKNOWN
        if (LocaleUtils.MULTIPLE != userLang
            && LocaleUtils.UNKNOWN != userLang
            && LocaleUtils.getDefaultLanguage() != userLang
        ) {
            MTLog.d(this, "SKIP loading '$username': different language ($userLang).")
            return
        }
        // 1- load user ID
        val userId = _userNamesId.getOrNull(i)?.takeIf { it.isNotBlank() } // provided (optional)
            ?: TwitterStorage.getUserNameId(context, username, StringUtils.EMPTY)
                .takeIf { it.isNotBlank() }
            ?: loadUserNameIdFromApi(twitterApi, token, username)
                .also { userId ->
                    username.takeIf { it.isNotBlank() } ?: return@also
                    userId?.takeIf { it.isNotBlank() } ?: return@also
                    TwitterStorage.saveUserNameId(context, username, userId)
                }
        if (userId.isNullOrBlank()) {
            MTLog.d(this, "SKIP loading '$username': no user ID.")
            return
        }
        // 2 - load user timeline
        val newLastUpdateInMs = TimeUtils.currentTimeMillis()
        MTLog.i(this, "Loading '@$username' posts from '$AGENCY_SOURCE_LABEL'...")
        if (FORCE_REFRESH) {
            TwitterStorage.saveUserNameSinceId(context, username, StringUtils.EMPTY) // reset
        }
        val sinceId = TwitterStorage.getUserNameSinceId(context, username, StringUtils.EMPTY).takeIf { it.isNotBlank() }
        // FIXME WARNING: The following parameters are not supported on all ANDROID OS versions
        val startTime: Date? = null
        // val startTime: OffsetDateTime? = OffsetDateTime.now().minusDays(31L) // format issue???
        // https://developer.x.com/en/docs/x-api/tweets/timelines/api-reference/get-users-id-tweets
        val response = twitterApi.getUsersIdTweets(
            authorization = "Bearer $token".takeIf { token != USING_CACHED_API_TOKEN }, // cached API doesn't need token
            userId = userId,
            maxResults = API_MAX_RESULT.takeIf { token != USING_CACHED_API_TOKEN }, // need same URL for all app users + customizable
            sinceId = sinceId?.takeIf { token != USING_CACHED_API_TOKEN }, // need same URL for all app users
            startTime = startTime?.takeIf { token != USING_CACHED_API_TOKEN }, // need same URL for all app users
            exclude = TWEETS_EXCLUDE.joinToString(separator = ","),
            tweetFields = TWEET_FIELDS.joinToString(separator = ","),
            expansions = TWEET_EXPANSIONS.joinToString(separator = ","),
            mediaFields = MEDIA_FIELDS.joinToString(separator = ","),
            userFields = USER_FIELDS.joinToString(separator = ","),
        ).execute().apply {
            if (!isSuccessful) {
                MTLog.w(this, "SKIP loading '$username' tweets: API error ${code()} (${errorBody()?.string()})")
                return
            }
        }.body()
            ?: run {
                MTLog.w(this, "SKIP loading '$username': no response!")
                return
            }
        val targetUUID = _userNamesTarget.getOrNull(i)?.takeIf { it.isNotBlank() }
            ?: _targetAuthority.takeIf { it.isNotBlank() }
            ?: AgencyUtils.getAgencyAuthority(context)
            ?: run {
                MTLog.w(this, "SKIP loading '$username': no target UUID!")
                return
            }
        val severity = _userNamesSeverity.getOrNull(i)
            ?: context.resources.getInteger(R.integer.news_provider_severity_info_agency)
        val noteworthyInMs = _userNamesNoteworthy.getOrNull(i)
            ?: context.resources.getString(R.string.news_provider_noteworthy_warning).toLong()
        var loadedNewsCount = 0
        val tweetsResp = response.data
        val tweetsRespIncludedExpansions = response.includes
        var newSinceId: String? = null
        val colorString = _userNamesColors.getOrNull(i)?.takeIf { it.isNotBlank() }
            ?: _color.takeIf { it.isNotBlank() }
            ?: AgencyUtils.getAgencyColor(context)
            ?: ColorUtils.BLACK
                .also {
                    MTLog.w(this, "No color found for '$username'! (used fallback)")
                }
        tweetsResp
            ?.forEach { tweet ->
                readNews(
                    tweet,
                    tweetsRespIncludedExpansions,
                    authority,
                    severity,
                    noteworthyInMs,
                    newLastUpdateInMs,
                    maxValidityInMs,
                    targetUUID,
                    userId,
                    username,
                    userLang,
                    colorString,
                )?.apply {
                    if (newSinceId == null) {
                        newSinceId = tweet.id
                    }
                    newNews.add(this)
                    loadedNewsCount++
                }
            }
        newSinceId?.let { TwitterStorage.saveUserNameSinceId(context, username, it) }
        MTLog.i(this, "Loaded $loadedNewsCount news for '@$username'.")
    }

    // https://developer.x.com/en/docs/x-api/users/lookup/api-reference/get-users-by-username-username
    // https://github.com/xdevplatform/twitter-api-java-sdk/blob/main/docs/UsersApi.md#finduserbyusername
    private fun loadUserNameIdFromApi(twitterApi: TwitterV2Api, token: String, username: String): String? {
        val response = twitterApi.getUserByUsername(
            authorization = "Bearer $token".takeIf { token != USING_CACHED_API_TOKEN }, // cached API doesn't need token
            username = username,
        ).execute().apply {
            if (!isSuccessful) {
                MTLog.w(this, "SKIP loading '$username' ID: API error ${code()} (${errorBody()?.string()})")
                return null
            }
        }.body()
        return response?.data?.id?.takeIf { it.isNotBlank() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readNews(
        tweet: Tweet,
        includedExpansions: TweetIncludes?,
        authority: String,
        severity: Int,
        noteworthyInMs: Long,
        newLastUpdateInMs: Long,
        maxValidityInMs: Long,
        targetUUID: String,
        userId: String,
        userName: String,
        userLang: String,
        colorString: String,
    ): News? {
        if (tweet.inReplyToUserId != null && tweet.inReplyToUserId != userId) {
            MTLog.d(this, "readNews() > SKIP (in reply to screen name: '%s').", tweet.inReplyToUserId)
            return null
        }
        val author = includedExpansions?.users?.find { it.id == tweet.authorId }
        val authorUserName = author?.username ?: userName // "montransit"
        val authorName = author?.name ?: authorUserName // "MonTransit"
        val userProfileImageUrl = author?.profileImageUrl ?: ""

        val link = getNewsWebURL(tweet, authorUserName)
        val text = buildString {
            tweet.text?.let {
                append(
                    StringUtils.oneLineOneSpace(
                        HtmlUtils.fromHtml(it).toString()
                    )
                )
            }
        }
        val textHTML = buildString {
            getHTMLText(tweet, includedExpansions, REMOVE_IMAGE_FROM_TEXT)?.let { append(it) }
            if (!TextUtils.isEmpty(link)) {
                if (isNotEmpty()) {
                    append(HtmlUtils.BR).append(HtmlUtils.BR)
                }
                append(HtmlUtils.linkify(link))
            }
        }
        val lang: String = getLang(tweet, userLang)
        val createdAtInMs = tweet.createdAt?.time
            ?: newLastUpdateInMs // should never happen
        return News(
            null,
            authority,
            AGENCY_SOURCE_ID + tweet.id,
            severity,
            noteworthyInMs,
            newLastUpdateInMs,
            maxValidityInMs,
            createdAtInMs,
            targetUUID,
            colorString,
            authorName,
            getUserName(authorUserName),
            userProfileImageUrl,
            getAuthorProfileURL(authorUserName),
            text,
            textHTML,
            link,
            lang,
            AGENCY_SOURCE_ID,
            AGENCY_SOURCE_LABEL,
            getImageUrls(tweet, includedExpansions)
        )
    }

    // TODO later add retweeted media
    private fun getImageUrls(tweet: Tweet, tweetsRespExpansions: TweetIncludes?) =
        tweetsRespExpansions?.media
            ?.filter { tweet.attachments?.mediaKeys?.contains(it.mediaKey) == true }
            ?.mapNotNull { media ->
                when (media.type) {
                    TweetMediaType.PHOTO.type -> media.url
                    TweetMediaType.VIDEO.type -> media.previewImageUrl
                    TweetMediaType.ANIMATED_GIF.type -> media.previewImageUrl
                    else -> {
                        MTLog.w(this, "Unexpected media type '${media.type}'!")
                        null
                    }
                }
            }.orEmpty()

    private fun getHTMLText(
        status: Tweet,
        includedExpansions: TweetIncludes?,
        @Suppress("SameParameterValue", "UNUSED_PARAMETER", "unused") removeImageUrls: Boolean, // TODO later?
    ): String? {
        try {
            var textHTML = status.text ?: return null
            status.entities?.urls
                ?.distinctBy { it.url.toString() } // media elements are duplicated with same url, expanded_url, display_url but different media_key
                ?.forEach { urlEntity ->
                    val urlString = urlEntity.url.toString()
                    textHTML = textHTML.replace(
                        urlString,
                        getURL(
                            urlString,
                            (urlEntity.displayUrl ?: urlEntity.expandedUrl ?: urlEntity.url).toString()
                        )
                    )
                }
            status.entities?.hashtags?.mapNotNull { it.tag }?.forEach { hashTagTag ->
                val hashTag = "#${hashTagTag}"
                textHTML = textHTML.replace(
                    hashTag,
                    getURL(getHashTagURL(hashTagTag), hashTag)
                )
            }
            status.entities?.mentions?.mapNotNull { it.username }?.forEach { mentionUsername ->
                val userMention = "@${mentionUsername}"
                textHTML = textHTML.replace(
                    userMention,
                    getURL(getAuthorProfileURL(mentionUsername), userMention)
                )

            }
            textHTML += appendVideoAndGIF(includedExpansions, status.attachments?.mediaKeys)
            textHTML = HtmlUtils.toHTML(textHTML)
            return textHTML
        } catch (e: java.lang.Exception) {
            MTLog.w(this, e, "Error while generating HTML text for status '%s'!", status)
            return status.text
        }
    }

    private fun getLang(status: Tweet, userLang: String): String {
        var lang = userLang
        if (LocaleUtils.MULTIPLE == lang) {
            if (LocaleUtils.isFR(status.lang)) {
                lang = Locale.FRENCH.language
            } else if (LocaleUtils.isEN(status.lang)) {
                lang = Locale.ENGLISH.language
            }
        }
        if (TextUtils.isEmpty(lang)) {
            lang = LocaleUtils.UNKNOWN
        }
        return lang
    }

    private fun appendVideoAndGIF(includedExpansions: TweetIncludes?, mediaKeys: List<String>?) = buildString {
        includedExpansions?.media
            ?.filter { it.type == TweetMediaType.VIDEO.type }
            ?.filter { media -> mediaKeys?.contains(media.mediaKey) == true }
            ?.forEach { media ->
                val variants = media.variants
                variants ?: return@forEach
                val autoVariant = variants.singleOrNull { it.contentType == "application/x-mpegURL" }
                val sortedMp4Variants = variants.filter { it.contentType == "video/mp4" }.sortedBy { it.bitRate }
                val lowVariant = sortedMp4Variants.firstOrNull()
                val mediumVariant = sortedMp4Variants.getOrNull(variants.size / 2).takeIf { variants.size > 2 }
                val highVariant = sortedMp4Variants.lastOrNull()
                NewsTextFormatter.appendVideoLinkToHTMLText(
                    autoUrl = autoVariant?.url.toString(),
                    qualityToUrlList = listOfNotNull(
                        lowVariant?.let { NewsTextFormatter.getLowVideoText() to it.url.toString() },
                        mediumVariant?.let { NewsTextFormatter.getMediumVideoText() to it.url.toString() },
                        highVariant?.let { NewsTextFormatter.getHighVideoText() to it.url.toString() }
                    )
                ).let { videoLinks ->
                    if (videoLinks.isNotEmpty()) {
                        if (this@buildString.isEmpty()) {
                            append(HtmlUtils.BR)
                        }
                        append(HtmlUtils.BR)
                        append(videoLinks)
                    }
                }
            }
        includedExpansions?.media
            ?.filter { it.type == TweetMediaType.ANIMATED_GIF.type }
            ?.filter { media -> mediaKeys?.contains(media.mediaKey) == true }
            ?.forEach { media ->
                val variants = media.variants ?: return@forEach
                val variant = pickBestVideoVariant(variants)
                val variantUrlString = variant?.url
                if (!variantUrlString.isNullOrEmpty()) {
                    if (isEmpty()) {
                        append(HtmlUtils.BR)
                    }
                    append(HtmlUtils.BR)
                    append(getURL(variantUrlString, NewsTextFormatter.getGifPlayText(includeGif = true)))
                }
            }
    }

    private fun pickBestVideoVariant(variants: List<TweetMediaVariant>?): TweetMediaVariant? {
        variants?.takeIf { it.isNotEmpty() } ?: return null
        if (variants.size == 1) return variants[0]
        var selected: TweetMediaVariant? = null
        for (variant in variants) {
            if ("application/x-mpegURL" == variant.contentType) {
                selected = variant
            } else if (selected == null) {
                selected = variant
            }
        }
        return selected
    }

    override fun getNewsLanguages() = _languages
}