package org.mtransit.android.commons

import androidx.core.util.PatternsCompat

fun String.linkifyAllURLs(): String {
    try {
        return this.replace(PatternsCompat.WEB_URL.toRegex(), "<a href=\"$0\">$0</a>")
    } catch (e: Exception) {
        MTLog.w(this, e, "Error while linkify-ing all URLs in '$this'!")
        return this
    }
}