package org.mtransit.android.commons.provider

import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.commons.Cleaner
import org.mtransit.commons.RegexUtils.groupOr
import org.mtransit.commons.RegexUtils.matchGroup
import org.mtransit.commons.RegexUtils.maybe
import java.util.Locale

@Suppress("MemberVisibilityCanBePrivate", "unused")
object ServiceUpdateCleaner {

    @JvmField
    val NO_REPLACEMENT = matchGroup(0)

    @JvmField
    val SKIP_REPLACEMENT: String? = null

    @JvmField
    val BOLD_REPLACEMENT = HtmlUtils.applyBold(matchGroup(1))

    @JvmField
    val UNDERLINE_REPLACEMENT = HtmlUtils.applyUnderline(matchGroup(1))

    const val DEFAULT_IGNORE_CASE = true

    @JvmField
    val DEFAULT_REPLACEMENT = NO_REPLACEMENT

    @JvmField
    val DEFAULT_REPLACEMENT_WARNING = BOLD_REPLACEMENT

    @JvmField
    val DEFAULT_REPLACEMENT_INFO = UNDERLINE_REPLACEMENT

    private val WORDS = make(
        "cancel" + maybe("led"),
        "closed",
        "delay" + maybe("s"),
        "detour" + maybe("s"),
        "disruption" + maybe("s"),
        "interrupted",
        "late",
        "moved",
        "relocate" + maybe("d"),
        groupOr("no", "out of") + " service",
        "service " + groupOr("delay" + maybe("s"), "disruption"),
        "shift" + maybe("ed"),
        "unavailable",
        ignoreCase = true,
    )

    private val WORDS_POI = make(
        "route" + maybe("s"),
        "station" + maybe("s"),
        "stop" + maybe("s"),
        ignoreCase = true,
    )

    private val WORDS_FR = make(
        "annul[é|e]" + maybe("e") + maybe("s"),
        "d[é|e]lai" + maybe("s"),
        "d[é|e]plac[é|e]" + maybe("e") + maybe("s"),
        "d[é|e]tour" + maybe("s"),
        "ferm[é|e]" + maybe("e") + maybe("s"),
        "interrompu" + maybe("e") + maybe("s"),
        groupOr("non", "pas", "plus", "ne (peut|sera)( pas| plus)?( [ê|e]tre)?") + " desservi" + maybe("s"),
        "ralentissement" + maybe("s"),
        "relocalis[é|e]" + maybe("e") + maybe("s"),
        "retard" + maybe("s"),
        groupOr("pas de", "hors", "ralentissement de", "interruption de") + " service",
        ignoreCase = true,
    )

    private val WORDS_POI_FR = make(
        "arr[ê|e]t" + maybe("s"),
        "gare" + maybe("s"),
        "ligne" + maybe("s"),
        ignoreCase = true,
    )

    @JvmStatic
    @JvmOverloads
    fun make(
        vararg wordsRegex: String,
        ignoreCase: Boolean = DEFAULT_IGNORE_CASE,
    ) = Cleaner(
        regex = Cleaner.matchWords(*wordsRegex),
        replacement = DEFAULT_REPLACEMENT,
        ignoreCase = ignoreCase,
    )

    @JvmStatic
    @JvmOverloads
    fun make(
        wordRegex: String,
        ignoreCase: Boolean = DEFAULT_IGNORE_CASE,
    ) = make(*arrayOf(wordRegex), ignoreCase = ignoreCase)

    @JvmStatic
    @JvmOverloads
    fun clean(
        input: CharSequence,
        replacement: String?, // null to skip
        language: String = Locale.getDefault().language,
    ): String = clean(input, replacement, isFr = LocaleUtils.isFR(language))

    @JvmStatic
    @JvmOverloads
    fun clean(
        input: CharSequence,
        replacement: String?, // null to skip
        isFr: Boolean = LocaleUtils.isFR(),
    ): String = replacement?.let { (if (isFr) WORDS_FR else WORDS).clean(input, it) } ?: input.toString()

    @JvmStatic
    fun getReplacement(severity: Int?) = severity?.let {
        if (ServiceUpdate.isSeverityWarning(severity)) {
            DEFAULT_REPLACEMENT_WARNING
        } else if (ServiceUpdate.isSeverityInfo(severity)) {
            DEFAULT_REPLACEMENT_INFO
        } else {
            SKIP_REPLACEMENT
        }
    } ?: SKIP_REPLACEMENT

    @JvmStatic
    @JvmOverloads
    fun makeText(title: String? = null, description: CharSequence?) = buildString {
        if (title?.isNotBlank() == true) append(title)
        if (description?.isNotBlank() == true) {
            if (this.isNotEmpty()) append(": ")
            append(description)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun makeTextHTML(title: String? = null, description: CharSequence?, url: String? = null) = buildString {
        if (title?.isNotBlank() == true) {
            append(HtmlUtils.applyBold(title))
        }
        if (description?.isNotBlank() == true) {
            if (this.isNotEmpty()) append(HtmlUtils.BR)
            append(description)
        }
        if (url?.isNotBlank() == true) {
            if (this.isNotEmpty()) append(HtmlUtils.BR)
            append(url)
        }
    }
}