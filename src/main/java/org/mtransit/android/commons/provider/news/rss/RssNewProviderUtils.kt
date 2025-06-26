package org.mtransit.android.commons.provider.news.rss

import android.content.Context
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.Constants
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.R
import org.mtransit.android.commons.provider.agency.AgencyUtils
import org.mtransit.commons.dropWhile
import java.net.URL

object RssNewProviderUtils : MTLog.Loggable {

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
    fun pickColor(context: Context, i: Int) =
        getFeedsColors(context).getOrNull(i)?.takeIf { it.isNotBlank() }
            ?: getColor(context).takeIf { it.isNotBlank() }
            ?: AgencyUtils.getAgencyColor(context)
            ?: ColorUtils.BLACK
                .also {
                    MTLog.w(this, "No color found for '$i'! (used fallback)")
                }

    private var _feedsAuthorName: List<String>? = null

    private fun getFeedsAuthorName(context: Context) = _feedsAuthorName
        ?: context.resources.getStringArray(
            R.array.rss_feeds_author_name
        ).toList()
            .also { _feedsAuthorName = it }

    @JvmStatic
    fun pickAuthorName(context: Context, i: Int) =
        getFeedsAuthorName(context).getOrNull(i)?.takeIf { it.isNotBlank() }
            ?: AgencyUtils.getAgencyShortName(context)

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
            MTLog.w(this, e, "Error while parsing rss label from URL '$urlString'!")
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
            MTLog.w(this, e, "Error while parsing rss label from URL $url!")
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
    fun pickLang(context: Context, i: Int): String {
        return getFeedsLang(context).getOrNull(i)
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
    fun pickTarget(context: Context, i: Int): String? {
        return getFeedsTarget(context).getOrNull(i)?.takeIf { it.isNotBlank() }
            ?: getTargetAuthority(context).takeIf { it.isNotBlank() }
            ?: AgencyUtils.getAgencyAuthority(context)
                .takeIf { context.packageName != Constants.MAIN_APP_PACKAGE_NAME }.orEmpty()
    }

    private var _feedsSeverity: List<Int>? = null

    private fun getFeedsSeverity(context: Context) = _feedsSeverity
        ?: context.resources.getIntArray(
            R.array.rss_feeds_severity
        ).toList()
            .also { _feedsSeverity = it }

    @JvmStatic
    fun pickSeverity(context: Context, i: Int): Int {
        return getFeedsSeverity(context).getOrNull(i)
            ?: context.resources.getInteger(R.integer.news_provider_severity_info_agency)
    }

    private var _feedsNoteworthy: List<Long>? = null

    private fun getFeedsNoteworthy(context: Context) = _feedsNoteworthy
        ?: context.resources.getStringArray(
            R.array.rss_feeds_noteworthy
        ).toList().map { it.toLong() }
            .also { _feedsNoteworthy = it }

    @JvmStatic
    fun pickNoteworthy(context: Context, i: Int): Long {
        return getFeedsNoteworthy(context).getOrNull(i)
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
    fun pickIgnoreGUID(context: Context, i: Int) =
        getFeedsIgnoreGUID(context).getOrNull(i)
            ?: IGNORE_GUID_DEFAULT

    private const val IGNORE_LINK_DEFAULT = false

    private var _feedsIgnoreLink: List<Boolean>? = null

    private fun getFeedsIgnoreLink(context: Context) = _feedsIgnoreLink
        ?: context.resources.getStringArray(
            R.array.rss_feeds_ignore_link
        ).toList().map { it.toBoolean() }
            .also { _feedsIgnoreLink = it }

    @JvmStatic
    fun pickIgnoreLink(context: Context, i: Int) =
        getFeedsIgnoreLink(context).getOrNull(i)
            ?: IGNORE_LINK_DEFAULT

    private const val ENCODING_DEFAULT = "UTF-8"

    private var _encoding: String? = null

    private fun getEncoding(context: Context) = _encoding
        ?: context.resources.getString(R.string.rss_encoding)
            .also { _encoding = it }

    @JvmStatic
    fun pickEncoding(context: Context) =
        getEncoding(context).takeIf { it.isNotBlank() }
            ?: ENCODING_DEFAULT
}