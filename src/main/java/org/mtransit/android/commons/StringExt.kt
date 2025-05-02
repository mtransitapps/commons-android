package org.mtransit.android.commons

import java.util.Locale

@JvmOverloads
fun String.capitalize(locale: Locale = Locale.getDefault()) = replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }