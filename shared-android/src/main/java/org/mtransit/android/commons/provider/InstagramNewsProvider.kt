package org.mtransit.android.commons.provider

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import androidx.core.content.ContentProviderCompat
import androidx.core.text.toHtml
import androidx.core.text.toSpanned
import com.google.gson.annotations.SerializedName
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
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.StringUtils.EMPTY
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.data.News
import org.mtransit.android.commons.provider.InstagramNewsProvider.InstagramApi.JEdgeOwnerToTimelineMediaNode
import org.mtransit.android.commons.provider.InstagramNewsProvider.InstagramApi.JProfileUser
import org.mtransit.android.commons.provider.agency.AgencyUtils
import org.mtransit.android.commons.provider.common.ProviderConfig
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.commons.provider.news.ConfigurableNewsProvider
import org.mtransit.android.commons.provider.news.NewsProviderContract
import org.mtransit.android.commons.provider.news.instagram.InstagramNewsConfig
import org.mtransit.android.commons.provider.news.instagram.InstagramNewsDbHelper
import org.mtransit.android.commons.provider.news.instagram.InstagramStorage
import retrofit2.Call
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

// DO NOT MOVE: referenced in modules AndroidManifest.xml
@SuppressLint("Registered")
class InstagramNewsProvider : ConfigurableNewsProvider() {

    companion object {
        private val LOG_TAG: String = InstagramNewsProvider::class.java.simpleName

        private const val MENTION_AND_SCREEN_NAME = "@%s"

        private const val AGENCY_SOURCE_ID = "instagram"

        private const val AGENCY_SOURCE_LABEL = "Instagram"

        private const val BASE_HOST = "instagram.com"
        private const val BASE_HOST_URL = "https://www.$BASE_HOST"

        fun createInstagramApi(context: Context): InstagramApi {
            val retrofit = NetworkUtils
                .makeNewRetrofitWithGson(BASE_HOST_URL, context)

            return retrofit.create()
        }
    }

    private val _authority: String by lazy {
        importedProviderConfig?.authority
            ?: ContentProviderCompat.requireContext(this).getString(R.string.instagram_authority)
    }

    private val _authorityUri: Uri by lazy {
        UriUtils.newContentUri(_authority)
    }

    private val color: String by lazy {
        if (importedProviderConfig != null) return@lazy importedInstagramConfig?.color.orEmpty()
        ContentProviderCompat.requireContext(this).getString(R.string.instagram_color)
    }

    private val userNames: List<String> by lazy {
        importedInstagramConfig?.userNames
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.instagram_user_names).toList()
    }

    private val userNamesLang: List<String> by lazy {
        importedInstagramConfig?.userNamesLang
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.instagram_user_names_lang).toList()
    }

    private val userNamesColors: List<String> by lazy {
        importedInstagramConfig?.userNamesColors
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.instagram_user_names_colors).toList()
    }

    private val targetAuthority: String by lazy {
        importedProviderConfig?.targetAuthority
            ?: ContentProviderCompat.requireContext(this).getString(R.string.instagram_target_for_poi_authority)
    }

    private val userNamesTarget: List<String> by lazy {
        importedInstagramConfig?.userNamesTarget
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.instagram_user_names_target).toList()
    }

    private val userNamesSeverity: List<Int> by lazy {
        importedInstagramConfig?.userNamesSeverity
            ?: ContentProviderCompat.requireContext(this).resources.getIntArray(R.array.instagram_user_names_severity).toList()
    }

    @get:IntegerRes
    private val userNamesSeverityDefaultResId: Int get() = R.integer.news_provider_severity_info_agency

    private val userNamesNoteworthy: List<Long> by lazy {
        importedInstagramConfig?.userNamesNoteworthy
            ?: ContentProviderCompat.requireContext(this).resources.getStringArray(R.array.instagram_user_names_noteworthy).toList().map { it.toLong() }
    }

    // legacy credentials (not used by current API client): exported encoded only
    private val consumerKey: String by lazy {
        ContentProviderCompat.requireContext(this).getString(R.string.instagram_consumer_key)
    }

    private val consumerSecret: String by lazy {
        ContentProviderCompat.requireContext(this).getString(R.string.instagram_consumer_secret)
    }

    @get:StringRes
    private val userNamesNoteworthyDefaultResId: Int get() = R.string.news_provider_noteworthy_info

    private val languages: List<String> by lazy {
        listOf<String>(
            if (LocaleUtils.isFR()) Locale.FRENCH.language else Locale.ENGLISH.language,
            LocaleUtils.UNKNOWN
        )
    }

    override fun getLogTag() = LOG_TAG

    @Volatile
    private var importedInstagramConfig: InstagramNewsConfig? = null

    override fun configureNewsConfig(config: ProviderConfig) {
        require(config.type == ProviderConfig.TYPE_NEWS_INSTAGRAM) { "Unsupported Instagram config type" }
        importedInstagramConfig = sharedJson.decodeFromJsonElement<InstagramNewsConfig>(config.params)
    }

    override fun getProviderConfig(): ProviderConfig {
        importedProviderConfig?.let { return it }
        val context = requireContextCompat()
        return ProviderConfig.create(
            type = ProviderConfig.TYPE_NEWS_INSTAGRAM,
            authority = _authority,
            targetAuthority = targetAuthority.takeIf { it.isNotBlank() } ?: AgencyUtils.getAgencyAuthority(context).orEmpty(),
            params = sharedJson.encodeToJsonElement(
                InstagramNewsConfig(
                    color = color.takeIf { it.isNotBlank() } ?: AgencyUtils.getAgencyColor(context),
                    userNames = userNames,
                    userNamesLang = userNamesLang,
                    userNamesColors = userNamesColors,
                    userNamesTarget = userNamesTarget,
                    userNamesSeverity = userNamesSeverity,
                    userNamesNoteworthy = userNamesNoteworthy,
                    defaultSeverity = context.resources.getInteger(userNamesSeverityDefaultResId),
                    defaultNoteworthy = context.getString(userNamesNoteworthyDefaultResId).toLong(),
                )
            ).jsonObject,
            secrets = mapOf(
                KeysIds.INSTAGRAM_CONSUMER_KEY to consumerKey,
                KeysIds.INSTAGRAM_CONSUMER_SECRET to consumerSecret,
            ),
        )
    }

    override fun closeConfiguredRuntime() {
        super.closeConfiguredRuntime()
        storage = null
    }

    override fun getCurrentDbVersion() = InstagramNewsDbHelper.getDbVersion(requireContextCompat())

    override fun getNewDbHelper(context: Context) = InstagramNewsDbHelper(
        context.applicationContext,
        getStorage(context),
        dbNamePrefix = importedProviderConfig?.authorityStoragePrefix,
    )

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
        updateAgencyNewsDataIfRequired(requireContextCompat(), newsFilter.isInFocusOrDefault)
        return getCachedNews(newsFilter)
    }

    @Volatile
    private var storage: InstagramStorage? = null

    private fun getStorage(context: Context) = storage ?: synchronized(this) {
        storage ?: InstagramStorage(
            context.applicationContext,
            prefix = importedProviderConfig?.authorityStoragePrefix,
        ).also { storage = it }
    }

    private fun updateAgencyNewsDataIfRequired(context: Context, inFocus: Boolean) {
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
        inFocus: Boolean
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
            getStorage(context).saveLastUpdate(nowInMs, LocaleUtils.getDefaultLanguage())
        } // else keep whatever we have until max validity reached
    }

    private var _instagramApi: InstagramApi? = null

    private fun getInstagramApi(context: Context) =
        _instagramApi ?: createInstagramApi(context).also { _instagramApi = it }

    private fun loadAgencyNewsDataFromWWW(context: Context): ArrayList<News>? {
        try {
            val newNews = ArrayList<News>()
            val maxValidityInMs = newsMaxValidityInMs
            val authority = _authority
            for ((i, userName) in userNames.withIndex()) {
                loadUserTimeline(
                    context,
                    getInstagramApi(context),
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

    private fun loadUserTimeline(
        context: Context,
        instagramApi: InstagramApi,
        newNews: MutableList<News>,
        maxValidityInMs: Long,
        authority: String,
        i: Int,
        username: String
    ) {
        val userLang = userNamesLang.getOrNull(i) ?: LocaleUtils.UNKNOWN
        if (LocaleUtils.MULTIPLE != userLang
            && LocaleUtils.UNKNOWN != userLang
            && LocaleUtils.getDefaultLanguage() != userLang
        ) {
            MTLogK.d(this, "SKIP loading '$username': different language ($userLang).")
            return
        }
        val newLastUpdateInMs = TimeUtils.currentTimeMillis()
        MTLogK.i(this, "Loading '@$username' posts from '$BASE_HOST'...")
        val response = instagramApi.userProfile(username).execute()
        val targetUUID = userNamesTarget.getOrNull(i)?.takeIf { it.isNotBlank() }
            ?: targetAuthority.takeIf { it.isNotBlank() }
            ?: AgencyUtils.getAgencyAuthority(context)
            ?: EMPTY.takeIf { context.packageName == Constants.MAIN_APP_PACKAGE_NAME } // target all allowed for main app
            ?: run {
                MTLogK.w(this, "SKIP loading '$username': no target UUID!")
                return
            }
        val severity = userNamesSeverity.getOrNull(i)
            ?: importedInstagramConfig?.defaultSeverity
            ?: context.resources.getInteger(userNamesSeverityDefaultResId)
        val noteworthyInMs = userNamesNoteworthy.getOrNull(i)
            ?: importedInstagramConfig?.defaultNoteworthy
            ?: context.resources.getString(userNamesNoteworthyDefaultResId).toLong()
        var loadedNewsCount = 0
        if (!response.isSuccessful) {
            throw IOException("ERROR while loading '@$username': HTTP Response Code ${response.code()} (Message: ${response.message()})")
        }
        val responseBody = response.body()
        val user = responseBody?.graphQL?.user
        val timelineMediaEdges = user?.edgeOwnerToTimelineMedia?.edges
        val userColor = userNamesColors.getOrNull(i)
            ?: color.takeIf { it.isNotBlank() }
            ?: AgencyUtils.getAgencyColor(context).takeIf { importedProviderConfig == null } // main app color != module color
            ?: ColorUtils.BLACK
                .also {
                    MTLogK.w(this, "No color found for '$username'! (used fallback)")
                }
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
                    targetUUID,
                    username,
                    user,
                    userLang,
                    userColor,
                ).apply {
                    newNews.add(this)
                    loadedNewsCount++
                }
            }
        MTLogK.i(this, "Loaded $loadedNewsCount news for '@$username'.")
    }

    private fun readNews(
        timelineMedia: JEdgeOwnerToTimelineMediaNode,
        authority: String,
        severity: Int,
        noteworthyInMs: Long,
        newLastUpdateInMs: Long,
        maxValidityInMs: Long,
        targetUUID: String,
        username: String,
        user: JProfileUser,
        userLang: String,
        userColor: String,
    ): News {
        val mediaToCaptions = timelineMedia.edgeMediaToCaption?.edges
        val captionText =
            mediaToCaptions
                ?.map { edge -> edge?.node?.text }
                ?.first { text -> text?.isNotEmpty() == true }
                ?: EMPTY
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
            targetUUID,
            userColor,
            user.fullName ?: user.username ?: username,
            String.format(MENTION_AND_SCREEN_NAME, user.username ?: username),
            user.profilePicUrl,
            "https://www.instagram.com/" + user.username,
            StringUtils.oneLineOneSpace(
                HtmlUtils.fromHtmlLegacy(captionText).toString()
            ),
            textHTMLSb.toString(),
            webURL,
            userLang,
            AGENCY_SOURCE_ID,
            AGENCY_SOURCE_LABEL,
            readImages(timelineMedia, ignoreVideo = false)
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

    private fun readImages(
        timelineMediaNode: JEdgeOwnerToTimelineMediaNode,
        @Suppress("SameParameterValue") ignoreVideo: Boolean,
    ): List<String> {
        timelineMediaNode.edgeSidecarToChildren?.edges?.let { edges ->
            return edges
                .map { edge -> edge?.node }
                .filter { node -> !ignoreVideo || node?.isVideo != true } // no support for video, yet
                .mapNotNull { node -> node?.displayUrl }
                .toList()
        }
        if (ignoreVideo && timelineMediaNode.isVideo == true) {
            return emptyList() // no support for video, yet
        }
        return listOf(
            timelineMediaNode.displayUrl
                ?: timelineMediaNode.thumbnailSrc
                ?: EMPTY
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

    override val newsLanguages: Collection<String> get() = languages
}
