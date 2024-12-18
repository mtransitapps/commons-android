package org.mtransit.android.commons.provider

import android.annotation.SuppressLint
import android.content.Context
import android.content.UriMatcher
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat
import com.twitter.clientlib.ApiClient
import com.twitter.clientlib.TwitterCredentialsBearer
import com.twitter.clientlib.api.TwitterApi
import com.twitter.clientlib.model.AnimatedGif
import com.twitter.clientlib.model.Expansions
import com.twitter.clientlib.model.Get2UsersByUsernameUsernameResponse
import com.twitter.clientlib.model.Photo
import com.twitter.clientlib.model.Tweet
import com.twitter.clientlib.model.Variant
import com.twitter.clientlib.model.Video
import org.mtransit.android.commons.BuildConfig
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
import org.mtransit.android.commons.provider.news.NewsTextFormatter
import org.mtransit.android.commons.provider.news.twitter.TwitterNewsDbHelper
import org.mtransit.android.commons.provider.news.twitter.TwitterStorage
import java.io.IOException
import java.time.OffsetDateTime
import java.util.Locale
import java.util.concurrent.TimeUnit

@SuppressLint("Registered")
class TwitterNewsProvider : NewsProvider() {

    companion object {
        private val LOG_TAG: String = TwitterNewsProvider::class.java.simpleName

        private const val FORCE_REFRESH = false
        // private const val FORCE_REFRESH = true // DEBUG

        private val VALIDITY_DEBUG_FACTOR = if (BuildConfig.DEBUG) 1L else 2L
        private val VALIDITY_EXPANSIVE_API_FACTOR = VALIDITY_DEBUG_FACTOR * 2L

        private val NEWS_MAX_VALIDITY_IN_MS = MAX_CACHE_VALIDITY_MS
        private val NEWS_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L) * VALIDITY_EXPANSIVE_API_FACTOR
        private val NEWS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.HOURS.toMillis(1L) * VALIDITY_EXPANSIVE_API_FACTOR
        private val NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(30L) * VALIDITY_EXPANSIVE_API_FACTOR
        private val NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10L) * VALIDITY_EXPANSIVE_API_FACTOR

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
            @Suppress("KotlinConstantConditions") // incremental data load, never delete old data
            if (false && !deleteAllDone) {
                deleteAllAgencyNewsData()
            }
            cacheNews(newNews)
            TwitterStorage.saveLastUpdateMs(context, nowInMs) // sync
            TwitterStorage.saveLastUpdateLang(context, LocaleUtils.getDefaultLanguage()) // sync
        } // else keep whatever we have until max validity reached
    }

    private var _twitterApi: TwitterApi? = null

    private fun getTwitterApi() =
        _twitterApi ?: createTwitterApi().also { _twitterApi = it }

    private fun createTwitterApi() = (this.providedBearerToken ?: this._bearerToken).takeIf { it.isNotBlank() }?.let { bearerToken ->
        val apiClient = ApiClient(NetworkUtils.makeNewOkHttpClientWithInterceptor(context))
        apiClient.setTwitterCredentials(TwitterCredentialsBearer(bearerToken))
        TwitterApi(apiClient)
    } ?: run {
        MTLog.w(this, "No valid Twitter bearer token provided!")
        null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadAgencyNewsDataFromWWW(context: Context): ArrayList<News>? {
        // @Suppress("ConstantConditionIf")
        // if (true) {
        // return null // TOO expensive $$$
        // }
        val twitterApi = getTwitterApi() ?: return null
        try {
            val newNews = ArrayList<News>()
            val maxValidityInMs = newsMaxValidityInMs
            val authority = _authority
            for ((i, userName) in _userNames.withIndex()) {
                loadUserTimeline(
                    context,
                    twitterApi,
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
        twitterApi: TwitterApi,
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
        // 1- load user ID
        val userId = _userNamesId.getOrNull(i)?.takeIf { it.isNotBlank() } // provided (optional)
            ?: TwitterStorage.getUserNameId(context, username, StringUtils.EMPTY)
                .takeIf { it.isNotBlank() }
            ?: loadUserNameIdFromApi(twitterApi, username)
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
        val startTime: OffsetDateTime? = null
        // val startTime: OffsetDateTime? = OffsetDateTime.now().minusDays(31L) // format issue???
        // https://developer.x.com/en/docs/x-api/tweets/timelines/api-reference/get-users-id-tweets
        val response = twitterApi.tweets().usersIdTweets(userId)
            .maxResults(API_MAX_RESULT)
            .sinceId(sinceId)
            .startTime(startTime)
            .exclude(TWEETS_EXCLUDE)
            .tweetFields(TWEET_FIELDS)
            .expansions(TWEET_EXPANSIONS)
            .mediaFields(MEDIA_FIELDS)
            .userFields(USER_FIELDS)
            .execute()
        val target = _userNamesTarget[i]
        val severity = _userNamesSeverity[i]
        val noteworthyInMs = _userNamesNoteworthy[i]
        var loadedNewsCount = 0
        val tweetsResp = response.data
        val tweetsRespIncludedExpansions = response.includes
        var newSinceId: String? = null
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
                    target,
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
        newSinceId?.let { TwitterStorage.saveUserNameSinceId(context, username, it) }
        MTLog.i(this, "Loaded $loadedNewsCount news for '@$username'.")
    }

    // https://developer.x.com/en/docs/x-api/users/lookup/api-reference/get-users-by-username-username
    // https://github.com/xdevplatform/twitter-api-java-sdk/blob/main/docs/UsersApi.md#finduserbyusername
    private fun loadUserNameIdFromApi(twitterApi: TwitterApi, username: String): String? {
        val response: Get2UsersByUsernameUsernameResponse? = twitterApi.users().findUserByUsername(username).execute()
        return response?.data?.id?.takeIf { it.isNotBlank() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readNews(
        tweet: Tweet,
        includedExpansions: Expansions?,
        authority: String,
        severity: Int,
        noteworthyInMs: Long,
        newLastUpdateInMs: Long,
        maxValidityInMs: Long,
        target: String,
        userId: String,
        userName: String,
        userLang: String,
    ): News? {
        if (tweet.inReplyToUserId != null && tweet.inReplyToUserId != userId) {
            MTLog.d(this, "readNews() > SKIP (in reply to screen name: '%s').", tweet.inReplyToUserId)
            return null
        }
        val author = includedExpansions?.users?.find { it.id == tweet.authorId }
        val authorUserName = author?.username ?: userName // "montransit"
        val authorName = author?.name ?: authorUserName // "MonTransit"
        val userProfileImageUrl = author?.profileImageUrl?.toString() ?: ""

        val link = getNewsWebURL(tweet, authorUserName)
        val textHTMLSb = java.lang.StringBuilder()
        textHTMLSb.append(getHTMLText(tweet, includedExpansions, REMOVE_IMAGE_FROM_TEXT))
        if (!TextUtils.isEmpty(link)) {
            if (textHTMLSb.isNotEmpty()) {
                textHTMLSb.append(HtmlUtils.BR).append(HtmlUtils.BR)
            }
            textHTMLSb.append(HtmlUtils.linkify(link))
        }
        val lang: String = getLang(tweet, userLang)
        val createdAtInMs = tweet.createdAt?.toInstant()?.toEpochMilli()
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
            target,
            getColor(authorUserName),
            authorName,
            getUserName(authorUserName),
            userProfileImageUrl,
            getAuthorProfileURL(authorUserName),  //
            StringUtils.oneLineOneSpace(HtmlUtils.fromHtml(tweet.text).toString()),  //
            textHTMLSb.toString(),  //
            link,
            lang,
            AGENCY_SOURCE_ID,
            AGENCY_SOURCE_LABEL,
            getImageUrls(tweet, includedExpansions)
        )
    }

    // TODO later add retweeted media
    private fun getImageUrls(tweet: Tweet, tweetsRespExpansions: Expansions?) =
        tweetsRespExpansions?.media
            ?.filter { tweet.attachments?.mediaKeys?.contains(it.mediaKey) == true }
            ?.mapNotNull { media ->
                when (media) {
                    is Photo -> media.url.toString()
                    is Video -> media.previewImageUrl.toString()
                    is AnimatedGif -> media.previewImageUrl.toString()
                    else -> {
                        MTLog.w(this, "Unexpected media type '${media.type}'!")
                        null
                    }
                }
            }.orEmpty()

    private fun getHTMLText(
        status: Tweet,
        includedExpansions: Expansions?,
        @Suppress("SameParameterValue", "UNUSED_PARAMETER", "unused") removeImageUrls: Boolean, // TODO later?
    ): String {
        try {
            var textHTML = status.text
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
            status.entities?.hashtags?.forEach { hashTagEntity ->
                val hashTag = "#${hashTagEntity.tag}"
                textHTML = textHTML.replace(
                    hashTag,
                    getURL(getHashTagURL(hashTagEntity.tag), hashTag)
                )
            }
            status.entities?.mentions?.forEach {
                val userMention = "@${it.username}"
                textHTML = textHTML.replace(
                    userMention,
                    getURL(getAuthorProfileURL(it.username), userMention)
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

    private fun appendVideoAndGIF(includedExpansions: Expansions?, mediaKeys: List<String>?) = buildString {
        includedExpansions?.media
            ?.filterIsInstance<Video>()
            ?.filter { media -> mediaKeys?.contains(media.mediaKey) == true }
            ?.forEach { media ->
                val variants = media.variants
                variants ?: return@forEach
                // if (true) {
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
            ?.filterIsInstance<AnimatedGif>()
            ?.filter { media -> mediaKeys?.contains(media.mediaKey) == true }
            ?.forEach { media ->
                val variants = media.variants ?: return@forEach
                val variant = pickBestVideoVariant(variants)
                val variantUrlString = variant?.url?.toString()
                if (!variantUrlString.isNullOrEmpty()) {
                    if (isEmpty()) {
                        append(HtmlUtils.BR)
                    }
                    append(HtmlUtils.BR)
                    append(getURL(variantUrlString, NewsTextFormatter.getGifPlayText(includeGif = true)))
                }
            }
    }

    private fun pickBestVideoVariant(variants: List<Variant>?): Variant? {
        variants?.takeIf { it.isNotEmpty() } ?: return null
        if (variants.size == 1) return variants[0]
        var selected: Variant? = null
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