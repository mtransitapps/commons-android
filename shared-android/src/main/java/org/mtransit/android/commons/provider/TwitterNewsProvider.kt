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
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.StringUtils.EMPTY
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.provider.agency.AgencyUtils
import org.mtransit.android.commons.provider.common.ProviderConfig
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.commons.provider.news.ConfigurableNewsProvider
import org.mtransit.android.commons.provider.news.NewsProviderContract
import org.mtransit.android.commons.provider.news.NewsTextFormatter
import org.mtransit.android.commons.provider.news.twitter.TwitterDateAdapter
import org.mtransit.android.commons.provider.news.twitter.TwitterNewsConfig
import org.mtransit.android.commons.provider.news.twitter.TwitterNewsDbHelper
import org.mtransit.android.commons.provider.news.twitter.TwitterStorage
import org.mtransit.android.commons.provider.news.twitter.TwitterV2Api
import org.mtransit.android.commons.provider.news.twitter.model.Tweet
import org.mtransit.android.commons.provider.news.twitter.model.TweetMedia.TweetMediaVariant
import org.mtransit.android.commons.provider.news.twitter.model.TweetMediaType
import org.mtransit.android.commons.provider.news.twitter.model.TweetsResponse.TweetIncludes
import retrofit2.create
import java.io.IOException
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.minutes

// DO NOT MOVE: referenced in modules AndroidManifest.xml
@SuppressLint("Registered")
class TwitterNewsProvider : ConfigurableNewsProvider() {

    companion object {
        private val LOG_TAG: String = TwitterNewsProvider::class.java.simpleName

        private const val FORCE_REFRESH = false
        // private const val FORCE_REFRESH = true // DEBUG

        // private val VALIDITY_DEBUG_FACTOR = if (BuildConfig.DEBUG) 1L else 1L
        private const val VALIDITY_EXPANSIVE_API_FACTOR = 1L

        // https://docs.x.com/x-api/fundamentals/rate-limits (Basic)
        // - [GET /2/users/by/username/:username]   500 requests / 24 hours PER APP
        // - [GET /2/tweets]                        15 requests / 15 mins PER APP

        @Suppress("unused")
        val WEB_URL_REGEX = Regex("https?://(www)?(x|twitter)\\.com/(.+)/status/(\\d+)")

        // const val WEB_URL_REGEX_GROUP_USER_SCREEN_NAME = 3
        @Suppress("unused")
        const val WEB_URL_REGEX_GROUP_TWEET_ID = 4

        private const val AGENCY_SOURCE_ID = "twitter"

        private const val AGENCY_SOURCE_LABEL = "X"

        private const val API_MAX_RESULT = 5 // min 5, default 10, max 100

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
            "attachments.media_keys", // MEDIA representing the images, videos, GIFs included in the tweet
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
        @Suppress("SpellCheckingInspection")
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

    private val _authority: String by lazy {
        importedProviderConfig?.authority
            ?: ContentProviderCompat.requireContext(this).getString(R.string.twitter_authority)
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

    private val color: String by lazy {
        importedTwitterConfig?.color.orEmpty().ifEmpty {
            ContentProviderCompat.requireContext(this).getString(R.string.twitter_color)
        }
    }

    private val userNames: List<String> by lazy {
        importedTwitterConfig?.screenNames
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.twitter_screen_names).toList()
    }

    private val userNamesId: List<String> by lazy {
        importedTwitterConfig?.screenNamesId
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.twitter_screen_names_id).toList()
    }

    private val userNamesLang: List<String> by lazy {
        importedTwitterConfig?.screenNamesLang
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.twitter_screen_names_lang).toList()
    }

    private val userNamesColors: List<String> by lazy {
        importedTwitterConfig?.screenNamesColors
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.twitter_screen_names_colors).toList()
    }

    private val targetAuthority: String by lazy {
        importedProviderConfig?.targetAuthority
            ?: ContentProviderCompat.requireContext(this).getString(R.string.twitter_target_for_poi_authority)
    }

    private val userNamesTarget: List<String> by lazy {
        importedTwitterConfig?.screenNamesTarget
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.twitter_screen_names_target).toList()
    }

    private val userNamesSeverity: List<Int> by lazy {
        importedTwitterConfig?.screenNamesSeverity
            ?: ContentProviderCompat.requireContext(this).resources.getIntArray(R.array.twitter_screen_names_severity).toList()
    }

    @get:IntegerRes
    private val userNamesSeverityDefaultResId: Int get() = R.integer.news_provider_severity_info_agency

    private val userNamesNoteworthy: List<Long> by lazy {
        importedTwitterConfig?.screenNamesNoteworthy
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.twitter_screen_names_noteworthy).toList().map { it.toLong() }
    }

    @get:StringRes
    private val userNamesNoteworthyDefaultResId: Int get() = R.string.news_provider_noteworthy_warning

    private val _languages: List<String> by lazy {
        listOf<String>(
            if (LocaleUtils.isFR()) Locale.FRENCH.language else Locale.ENGLISH.language,
            LocaleUtils.UNKNOWN
        )
    }

    override fun getLogTag() = LOG_TAG

    @Volatile
    private var importedTwitterConfig: TwitterNewsConfig? = null

    override fun configureNewsConfig(config: ProviderConfig) {
        require(config.type == ProviderConfig.TYPE_NEWS_TWITTER) { "Unsupported Twitter config type" }
        importedTwitterConfig = sharedJson.decodeFromJsonElement<TwitterNewsConfig>(config.params)
    }

    override fun closeConfiguredRuntime() {
        super.closeConfiguredRuntime()
        _storage = null
    }

    override fun getProviderConfig(): ProviderConfig {
        importedProviderConfig?.let { return it }
        val context = requireContextCompat()
        return ProviderConfig.create(
            type = ProviderConfig.TYPE_NEWS_TWITTER,
            authority = _authority,
            targetAuthority = targetAuthority.takeIf { it.isNotBlank() } ?: AgencyUtils.getAgencyAuthority(context).orEmpty(),
            params = sharedJson.encodeToJsonElement(
                TwitterNewsConfig(
                    color = color.takeIf { it.isNotBlank() } ?: AgencyUtils.getAgencyColor(context),
                    screenNames = userNames,
                    screenNamesId = userNamesId,
                    screenNamesLang = userNamesLang,
                    screenNamesColors = userNamesColors,
                    screenNamesTarget = userNamesTarget,
                    screenNamesSeverity = userNamesSeverity,
                    screenNamesNoteworthy = userNamesNoteworthy,
                    defaultSeverity = context.resources.getInteger(userNamesSeverityDefaultResId),
                    defaultNoteworthy = context.getString(userNamesNoteworthyDefaultResId).toLong(),
                )
            ).jsonObject,
            secrets = mapOf(KeysIds.TWITTER_BEARER_TOKEN to _bearerToken),
        )
    }

    override fun getCurrentDbVersion() = TwitterNewsDbHelper.getDbVersion(requireContextCompat())

    override fun getNewDbHelper(context: Context) = TwitterNewsDbHelper(
        context.applicationContext,
        getStorage(context),
        dbNamePrefix = importedProviderConfig?.authorityStoragePrefix,
    )

    override fun getMinDurationBetweenNewsRefreshInMs(inFocusOrDefault: Boolean) =
        super.getMinDurationBetweenNewsRefreshInMs(inFocusOrDefault)
            .times(VALIDITY_EXPANSIVE_API_FACTOR)
            .coerceAtLeast(15.minutes.inWholeMilliseconds)

    override val newsMaxValidityInMs: Long get() = super.newsMaxValidityInMs.takeUnless { FORCE_REFRESH } ?: 0L

    override fun getNewsValidityInMs(inFocusOrDefault: Boolean) =
        super.getNewsValidityInMs(inFocusOrDefault)
            .times(VALIDITY_EXPANSIVE_API_FACTOR)
            .coerceAtLeast(30.minutes.inWholeMilliseconds)

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
        this.providedBearerToken = SecureStringUtils.dec(newsFilter.getProvidedEncryptKey(KeysIds.TWITTER_BEARER_TOKEN))
        this.providedCachedApiUrl = SecureStringUtils.dec(newsFilter.getProvidedEncryptKey(KeysIds.TWITTER_CACHED_API_URL))
        try {
            updateAgencyNewsDataIfRequired(requireContextCompat(), newsFilter.isInFocusOrDefault)
            return getCachedNews(newsFilter)
        } finally {
            this.providedBearerToken = null
            this.providedCachedApiUrl = null
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
        if (!deleteAllRequired && lastUpdateInMs + minUpdateMs >= nowInMs) {
            return
        }
        updateAllAgencyNewsDataFromWWW(context, deleteAllRequired) // try to update
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
            @Suppress("KotlinConstantConditions", "KotlinUnreachableCode", "SimplifyBooleanWithConstants") // incremental data load, never delete old data
            if (false && !deleteAllDone) {
                deleteAllAgencyNewsData()
            }
            cacheNews(newNews)
            getStorage(context).saveLastUpdate(nowInMs, LocaleUtils.getDefaultLanguage()) // sync
        } // else keep whatever we have until max validity reached
    }

    @Volatile
    private var _storage: TwitterStorage? = null

    private fun getStorage(context: Context) = _storage ?: synchronized(this) {
        _storage ?: TwitterStorage(
            context.applicationContext,
            prefix = importedProviderConfig?.authorityStoragePrefix,
        ).also { _storage = it }
    }

    private var _twitterApi: TwitterV2Api? = null

    private fun getTwitterApi(context: Context, baseHostUrl: String) =
        _twitterApi ?: createTwitterApi(context, baseHostUrl).also { _twitterApi = it }

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
            for ((i, userName) in userNames.withIndex()) {
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

    private fun getURL(url: String, text: String) = HtmlUtils.linkify(url, text)

    private fun getNewsWebURL(tweet: Tweet, userScreenName: String) = "https://x.com/$userScreenName/status/${tweet.id}"

    private fun getAuthorProfileURL(userScreenName: String) = "https://x.com/$userScreenName"

    private fun getHashTagURL(hashTag: String) = "https://x.com/hashtag/$hashTag"

    private fun getUserName(screenName: String) = "@$screenName"

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
        val userLang = userNamesLang.getOrNull(i) ?: LocaleUtils.UNKNOWN
        if (LocaleUtils.MULTIPLE != userLang
            && LocaleUtils.UNKNOWN != userLang
            && LocaleUtils.getDefaultLanguage() != userLang
        ) {
            MTLogK.d(this, "SKIP loading '$username': different language ($userLang).")
            return
        }
        // 1- load user ID
        val userId = userNamesId.getOrNull(i)?.takeIf { it.isNotBlank() } // provided (optional)
            ?: getStorage(context).getUserNameId(username, EMPTY)
                .takeIf { it.isNotBlank() }
            ?: loadUserNameIdFromApi(twitterApi, token, username)
                .also { userId ->
                    username.takeIf { it.isNotBlank() } ?: return@also
                    userId?.takeIf { it.isNotBlank() } ?: return@also
                    getStorage(context).saveUserNameId(username, userId)
                }
        if (userId.isNullOrBlank()) {
            MTLogK.d(this, "SKIP loading '$username': no user ID.")
            return
        }
        // 2 - load user timeline
        val newLastUpdateInMs = TimeUtils.currentTimeMillis()
        MTLogK.i(this, "Loading '@$username' posts from '$AGENCY_SOURCE_LABEL'...")
        if (FORCE_REFRESH) {
            getStorage(context).saveUserNameSinceId(username, EMPTY) // reset
        }
        val sinceId = getStorage(context).getUserNameSinceId(username, EMPTY).takeIf { it.isNotBlank() }
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
                MTLogK.w(this, "SKIP loading '$username' tweets: API error ${code()} (${errorBody()?.string()})")
                return
            }
        }.body()
            ?: run {
                MTLogK.w(this, "SKIP loading '$username': no response!")
                return
            }
        val targetUUID = userNamesTarget.getOrNull(i)?.takeIf { it.isNotBlank() }
            ?: targetAuthority.takeIf { it.isNotBlank() }
            ?: AgencyUtils.getAgencyAuthority(context)
            ?: EMPTY.takeIf { context.packageName == Constants.MAIN_APP_PACKAGE_NAME } // target all allowed for main app
            ?: run {
                MTLogK.w(this, "SKIP loading '$username': no target UUID!")
                return
            }
        val severity = userNamesSeverity.getOrNull(i)
            ?: importedTwitterConfig?.defaultSeverity
            ?: context.resources.getInteger(userNamesSeverityDefaultResId)
        val noteworthyInMs = userNamesNoteworthy.getOrNull(i)
            ?: importedTwitterConfig?.defaultNoteworthy
            ?: context.resources.getString(userNamesNoteworthyDefaultResId).toLong()
        var loadedNewsCount = 0
        val tweetsResp = response.data
        val tweetsRespIncludedExpansions = response.includes
        var newSinceId: String? = null
        tweetsResp
            ?.forEach { tweet ->
                readNews(
                    context,
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
                    userLang
                )?.apply {
                    if (newSinceId == null) {
                        newSinceId = tweet.id
                    }
                    newNews.add(this)
                    loadedNewsCount++
                }
            }
        newSinceId?.let { getStorage(context).saveUserNameSinceId(username, it) }
        MTLogK.i(this, "Loaded $loadedNewsCount news for '@$username'.")
    }

    // https://developer.x.com/en/docs/x-api/users/lookup/api-reference/get-users-by-username-username
    // https://github.com/xdevplatform/twitter-api-java-sdk/blob/main/docs/UsersApi.md#finduserbyusername
    private fun loadUserNameIdFromApi(twitterApi: TwitterV2Api, token: String, username: String): String? {
        val response = twitterApi.getUserByUsername(
            authorization = "Bearer $token".takeIf { token != USING_CACHED_API_TOKEN }, // cached API doesn't need token
            username = username,
        ).execute().apply {
            if (!isSuccessful) {
                MTLogK.w(this, "SKIP loading '$username' ID: API error ${code()} (${errorBody()?.string()})")
                return null
            }
        }.body()
        return response?.data?.id?.takeIf { it.isNotBlank() }
    }

    private fun readNews(
        context: Context,
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
    ): News? {
        if (tweet.inReplyToUserId != null && tweet.inReplyToUserId != userId) {
            MTLogK.d(this, "readNews() > SKIP (in reply to screen name: '${tweet.inReplyToUserId}').")
            return null
        }
        val author = includedExpansions?.users?.find { it.id == tweet.authorId }

        @Suppress("SpellCheckingInspection")
        val authorUserName = author?.username ?: userName // "montransit"
        val authorName = author?.name ?: authorUserName // "MonTransit"
        val userProfileImageUrl = author?.profileImageUrl.orEmpty()

        val link = getNewsWebURL(tweet, authorUserName)
        val text = buildString {
            tweet.text?.let {
                append(
                    StringUtils.oneLineOneSpace(
                        HtmlUtils.fromHtmlLegacy(it).toString()
                    )
                )
            }
        }
        val textHTML = buildString {
            getHTMLText(tweet, includedExpansions, NewsProviderContract.REMOVE_IMAGE_FROM_TEXT)?.let { append(it) }
            if (link.isNotEmpty()) {
                if (isNotEmpty()) {
                    append(HtmlUtils.BR).append(HtmlUtils.BR)
                }
                append(HtmlUtils.linkify(link))
            }
        }
        val lang: String = getLang(tweet, userLang)
        val createdAtInMs = tweet.createdAt?.time
            ?: newLastUpdateInMs // should never happen
        val colorString = userNamesColors.getOrNull(userNames.indexOf(authorUserName))?.takeIf { it.isNotBlank() }
            ?: color.takeIf { it.isNotBlank() }
            ?: AgencyUtils.getAgencyColor(context)
            ?: ColorUtils.BLACK
                .also {
                    MTLogK.w(this, "No color found for '$userName'! (used fallback)")
                }
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
                        MTLogK.w(this, "Unexpected media type '${media.type}'!")
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
                val hashTag = "#$hashTagTag"
                textHTML = textHTML.replace(
                    hashTag,
                    getURL(getHashTagURL(hashTagTag), hashTag)
                )
            }
            status.entities?.mentions?.mapNotNull { it.username }?.forEach { mentionUsername ->
                val userMention = "@$mentionUsername"
                textHTML = textHTML.replace(
                    userMention,
                    getURL(getAuthorProfileURL(mentionUsername), userMention)
                )
            }
            textHTML += appendVideoAndGIF(includedExpansions, status.attachments?.mediaKeys)
            textHTML = HtmlUtils.toHTML(textHTML)
            return textHTML
        } catch (e: Exception) {
            MTLogK.w(this, "Error while generating HTML text for status '$status'!", e)
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
        if (lang.isEmpty()) {
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

    override val newsLanguages: Collection<String> get() = _languages
}
