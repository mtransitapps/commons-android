package org.mtransit.android.commons.provider

import org.mtransit.android.commons.HtmlUtils
import org.mtransit.android.commons.LocaleUtils
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.commons.Cleaner
import org.mtransit.commons.RegexUtils.group
import org.mtransit.commons.RegexUtils.matchGroup
import org.mtransit.commons.RegexUtils.maybe
import org.mtransit.commons.RegexUtils.or

@Suppress("MemberVisibilityCanBePrivate", "unused")
object ServiceUpdateCleaner {

    @JvmField
    val NO_REPLACEMENT = matchGroup(0)

    @JvmField
    val SKIP_REPLACEMENT: String? = null

    @JvmField
    val BOLD_REPLACEMENT = HtmlUtils.applyBold("$1")

    @JvmField
    val UNDERLINE_REPLACEMENT = HtmlUtils.applyUnderline("$1")

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
        "moved",
        "relocate" + maybe("d"),
    )

    private val WORDS_FR = make(
        "annul[é|e]" + maybe("e") + maybe("s"),
        "d[é|e]plac[é|e]" + maybe("e") + maybe("s"),
        "ferm[é|e]" + maybe("e") + maybe("s"),
        "relocalis[é|e]" + maybe("e") + maybe("s"),
    )

    @JvmStatic
    @JvmOverloads
    fun make(
        vararg wordsRegex: String,
        // replacement: String = NO_REPLACEMENT,
        ignoreCase: Boolean = DEFAULT_IGNORE_CASE,
    ) = Cleaner(
        regex = group(or(*wordsRegex)),
        replacement = DEFAULT_REPLACEMENT,
        ignoreCase = ignoreCase,
    )

    @JvmStatic
    @JvmOverloads
    fun clean(
        input: CharSequence,
        replacement: String?, // null to skip
        isFr: Boolean = LocaleUtils.isFR(),
    ): String = replacement?.let { (if (isFr) WORDS_FR else WORDS).clean(input, it) } ?: input.toString()

    @JvmStatic
    fun getReplacement(severity: Int) =
        if (ServiceUpdate.isSeverityWarning(severity)) {
            DEFAULT_REPLACEMENT_WARNING
        } else if (ServiceUpdate.isSeverityInfo(severity)) {
            DEFAULT_REPLACEMENT_INFO
        } else {
            SKIP_REPLACEMENT
        }
}