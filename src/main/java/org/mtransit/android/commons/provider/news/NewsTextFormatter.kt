package org.mtransit.android.commons.provider.news

import org.mtransit.android.commons.HtmlUtils
import org.mtransit.commons.StringUtils.EMPTY
import java.util.Locale

object NewsTextFormatter {

    private const val TITLE_USE_H= true

    @JvmStatic
    fun formatHTMLTitle(title: String): String {
        if (TITLE_USE_H) {
            return "<H1>$title</H1>"
        }
        return HtmlUtils.applyBold(title)
    }

    @JvmStatic
    fun getHTMLAfterTitleSpace(length: Int): String {
        if (length <= 0) {
            return EMPTY
        }
        if (TITLE_USE_H) {
            return EMPTY
        }
        return buildString {
            append(HtmlUtils.BR) // after bold
            append(HtmlUtils.BR) // empty line
        }
    }

    fun appendVideoLinkToHTMLText(
        autoUrl: String? = null,
        qualityToUrlList: List<Pair<String, String>> = emptyList(),
        locale: Locale = Locale.getDefault(),
    ): String {
        return buildString {
            if (autoUrl == null && qualityToUrlList.isEmpty()) {
                return@buildString // no videos
            }
            if (autoUrl != null && qualityToUrlList.isEmpty()) {
                append(getVideoPlayText(locale))
                append(HtmlUtils.linkify(autoUrl, getOneVideoText(locale)))
                return@buildString // 1 video (auto)
            }
            if (autoUrl == null && qualityToUrlList.size == 1) {
                qualityToUrlList.firstOrNull()?.let { (_, url) ->
                    append(getVideoPlayText(locale))
                    append(HtmlUtils.linkify(url, getOneVideoText(locale)))
                    return@buildString // 1 video (mp4)
                }
            }
            append(getVideoPlayText(locale, includeVideo = true))
            autoUrl?.let { url ->
                append(HtmlUtils.linkify(url, getAutoVideoText(locale)))
            }
            if (qualityToUrlList.isNotEmpty()) {
                if (autoUrl != null) {
                    append(" (")
                }
                // TODO ? append("MP4: ")
                qualityToUrlList.forEachIndexed { index, (quality, url) ->
                    if (index > 0) {
                        append(" | ")
                    }
                    append(HtmlUtils.linkify(url, quality))
                }
                if (autoUrl != null) {
                    append(")")
                }
            }
        }
    }

    @Suppress("SameReturnValue")
    fun getAutoVideoText(locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            Locale.FRENCH.language -> "Auto"
            else -> "Auto"
        }
    }

    fun getLowVideoText(locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            Locale.FRENCH.language -> "Basse"
            else -> "Low"
        }
    }

    fun getMediumVideoText(locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            Locale.FRENCH.language -> "Moyenne"
            else -> "Medium"
        }
    }

    fun getHighVideoText(locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            Locale.FRENCH.language -> "Haute"
            else -> "High"
        }
    }

    fun getOneVideoText(locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            Locale.FRENCH.language -> "vidéo"
            else -> "video"
        }
    }

    fun getVideoPlayText(locale: Locale = Locale.getDefault(), includeVideo: Boolean = false) = buildString {
        append("▶ ")
        when (locale.language) {
            Locale.FRENCH.language -> append("Jouer")
            else -> append("Play")
        }
        if (includeVideo) {
            append(" ")
            append(getOneVideoText(locale))
        }
        when (locale.language) {
            Locale.FRENCH.language -> append(" : ")
            else -> append(": ")
        }
    }

    fun getGifPlayText(locale: Locale = Locale.getDefault(), includeGif: Boolean = false) = buildString {
        append("✨ ")
        when (locale.language) {
            Locale.FRENCH.language -> append("Jouer")
            else -> append("Play")
        }
        if (includeGif) {
            append(" ")
            append(getOneGIFText(locale))
        }
        when (locale.language) {
            Locale.FRENCH.language -> append(" : ")
            else -> append(": ")
        }
    }

    @Suppress("SameReturnValue")
    fun getOneGIFText(locale: Locale = Locale.getDefault()): String {
        return when (locale.language) {
            Locale.FRENCH.language -> "GIF"
            else -> "GIF"
        }
    }
}