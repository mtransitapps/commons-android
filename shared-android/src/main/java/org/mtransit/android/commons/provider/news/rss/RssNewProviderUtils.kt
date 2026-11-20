package org.mtransit.android.commons.provider.news.rss

import android.content.Context
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLogK
import org.mtransit.android.commons.R
import org.mtransit.android.commons.StringUtils.EMPTY
import org.mtransit.android.commons.provider.agency.AgencyUtils
import org.mtransit.commons.dropWhile
import java.net.URL

object RssNewProviderUtils : MTLogK.Loggable {

    private val LOG_TAG: String = RssNewProviderUtils::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    private var _color: String? = null

    private fun getColor(context: Context) = _color
        ?: context.resources.getString(R.string.rss_color)
            .also { _color = it }

    private var _feedsColors: List<String>? = null

    private fun getFeedsColors(context: Context) = _feedsColors
        ?: context.resources.getStringArray(
            R.array.rss_feeds_colors
        ).toList()
            .also { _feedsColors = it }

    @JvmStatic
    @JvmOverloads
    fun pickColor(context: Context, i: Int, config: RssNewsConfig? = null) =
        (config?.feedsColors ?: getFeedsColors(context)).getOrNull(i)?.takeIf { it.isNotBlank() }
            ?: config?.color
            ?: getColor(context).takeIf { it.isNotBlank() }
            ?: AgencyUtils.getAgencyColor(context)
            ?: ColorUtils.BLACK
                .also {
                    MTLogK.w(this, "No color found for '$i'! (used fallback)")
                }

    private var _feedsAuthorName: List<String>? = null

    private fun getFeedsAuthorName(context: Context) = _feedsAuthorName
        ?: context.resources.getStringArray(
            R.array.rss_feeds_author_name
        ).toList()
            .also { _feedsAuthorName = it }

    @JvmStatic
    @JvmOverloads
    fun pickAuthorName(context: Context, i: Int, config: RssNewsConfig? = null) =
        (config?.feedsAuthorName ?: getFeedsAuthorName(context)).getOrNull(i)?.takeIf { it.isNotBlank() }
            ?: if (config == null) AgencyUtils.getAgencyShortName(context) else config.defaultAuthorName

    @Suppress("SpellCheckingInspection")
    private val LABEL_BLACK_LIST = listOf(
        "api",
        "assets",
        "azurefd",
        "azure-api",
        "data",
        "opendata",
        "www",
    )

    @JvmStatic
    fun pickLabel(urlString: String): String {
        try {
            val url = URL(urlString)
            return pickLabel(url)
        } catch (e: Exception) {
            MTLogK.w(this, "Error while parsing rss label from URL '$urlString'!", e)
            return ""
        }
    }

    @JvmStatic
    fun pickLabel(url: URL): String {
        try {
            return url.host
                ?.split('.')?.takeLast(3)
                ?.dropWhile(minSize = 2) { LABEL_BLACK_LIST.contains(it) } // drop 1st elements if in black list
                ?.joinToString(separator = ".")
                .orEmpty()
        } catch (e: Exception) {
            MTLogK.w(this, "Error while parsing rss label from URL $url!", e)
            return ""
        }
    }

    private var _feedsLang: List<String>? = null

    private fun getFeedsLang(context: Context) = _feedsLang
        ?: context.resources.getStringArray(
            R.array.rss_feeds_lang
        ).toList()
            .also { _feedsLang = it }

    @JvmStatic
    @JvmOverloads
    fun pickLang(context: Context, i: Int, config: RssNewsConfig? = null): String {
        return (config?.feedsLang ?: getFeedsLang(context)).getOrNull(i)
            ?: LocaleUtils.UNKNOWN
    }

    private var _targetAuthority: String? = null

    private fun getTargetAuthority(context: Context) = _targetAuthority
        ?: context.resources.getString(R.string.rss_target_for_poi_authority)
            .also { _targetAuthority = it }

    private var _feedsTarget: List<String>? = null

    private fun getFeedsTarget(context: Context) = _feedsTarget
        ?: context.resources.getStringArray(
            R.array.rss_feeds_target
        ).toList()
            .also { _feedsTarget = it }

    @JvmStatic
    @JvmOverloads
    fun pickTarget(context: Context, i: Int, config: RssNewsConfig? = null, targetAuthority: String? = null): String? {
        if (config != null) {
            return config.feedsTarget.getOrNull(i)?.takeIf { it.isNotBlank() } ?: targetAuthority
        }
        return getFeedsTarget(context).getOrNull(i)?.takeIf { it.isNotBlank() }
            ?: getTargetAuthority(context).takeIf { it.isNotBlank() }
            ?: AgencyUtils.getAgencyAuthority(context)
            ?: EMPTY.takeIf { context.packageName == Constants.MAIN_APP_PACKAGE_NAME } // target all allowed for main app
    }

    private var _feedsSeverity: List<Int>? = null

    private fun getFeedsSeverity(context: Context) = _feedsSeverity
        ?: context.resources.getIntArray(
            R.array.rss_feeds_severity
        ).toList()
            .also { _feedsSeverity = it }

    @JvmStatic
    @JvmOverloads
    fun pickSeverity(context: Context, i: Int, config: RssNewsConfig? = null): Int {
        return (config?.feedsSeverity ?: getFeedsSeverity(context)).getOrNull(i)
            ?: config?.defaultSeverity
            ?: context.resources.getInteger(R.integer.news_provider_severity_info_agency)
    }

    private var _feedsNoteworthy: List<Long>? = null

    private fun getFeedsNoteworthy(context: Context) = _feedsNoteworthy
        ?: context.resources.getStringArray(
            R.array.rss_feeds_noteworthy
        ).toList().map { it.toLong() }
            .also { _feedsNoteworthy = it }

    @JvmStatic
    @JvmOverloads
    fun pickNoteworthy(context: Context, i: Int, config: RssNewsConfig? = null): Long {
        return (config?.feedsNoteworthy ?: getFeedsNoteworthy(context)).getOrNull(i)
            ?: config?.defaultNoteworthy
            ?: context.resources.getString(R.string.news_provider_noteworthy_long_term).toLong()
    }

    private const val IGNORE_GUID_DEFAULT = false

    private var _feedsIgnoreGUID: List<Boolean>? = null

    private fun getFeedsIgnoreGUID(context: Context) = _feedsIgnoreGUID
        ?: context.resources.getStringArray(
            R.array.rss_feeds_ignore_guid
        ).toList().map { it.toBoolean() }
            .also { _feedsIgnoreGUID = it }

    @JvmStatic
    @JvmOverloads
    fun pickIgnoreGUID(context: Context, i: Int, config: RssNewsConfig? = null) =
        (config?.feedsIgnoreGuid ?: getFeedsIgnoreGUID(context)).getOrNull(i)
            ?: IGNORE_GUID_DEFAULT

    private const val IGNORE_LINK_DEFAULT = false

    private var _feedsIgnoreLink: List<Boolean>? = null

    private fun getFeedsIgnoreLink(context: Context) = _feedsIgnoreLink
        ?: context.resources.getStringArray(
            R.array.rss_feeds_ignore_link
        ).toList().map { it.toBoolean() }
            .also { _feedsIgnoreLink = it }

    @JvmStatic
    @JvmOverloads
    fun pickIgnoreLink(context: Context, i: Int, config: RssNewsConfig? = null) =
        (config?.feedsIgnoreLink ?: getFeedsIgnoreLink(context)).getOrNull(i)
            ?: IGNORE_LINK_DEFAULT

    private const val ENCODING_DEFAULT = "UTF-8"

    private var _encoding: String? = null

    private fun getEncoding(context: Context) = _encoding
        ?: context.resources.getString(R.string.rss_encoding)
            .also { _encoding = it }

    @JvmStatic
    @JvmOverloads
    fun pickEncoding(context: Context, config: RssNewsConfig? = null) =
        (config?.encoding ?: getEncoding(context)).takeIf { it.isNotBlank() }
            ?: ENCODING_DEFAULT

    private var _feedsDateLinkFallback: List<Pair<String, String>>? = null

    private fun getFeedsDateLinkFallback(context: Context) = _feedsDateLinkFallback
        ?: context.resources.getStringArray(R.array.rss_feeds_date_link_fallback_regex).toList()
            .zip(context.resources.getStringArray(R.array.rss_feeds_date_link_fallback_format).toList())
            .map { (regex, formatting) ->
                Pair(regex, formatting)
            }
            .also { _feedsDateLinkFallback = it }

    @JvmStatic
    @JvmOverloads
    fun pickDateLinkFallback(context: Context, i: Int, config: RssNewsConfig? = null): Pair<String, String>? {
        if (config != null) {
            val regex = config.feedsDateLinkFallbackRegex.getOrNull(i) ?: return null
            val format = config.feedsDateLinkFallbackFormat.getOrNull(i) ?: return null
            return regex to format
        }
        return getFeedsDateLinkFallback(context).getOrNull(i)
    }
}
