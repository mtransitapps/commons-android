package org.mtransit.android.commons

import java.util.Locale

fun String.capitalize(): String {
    return capitalize(Locale.getDefault())
}

fun String.capitalize(locale: Locale) = replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }