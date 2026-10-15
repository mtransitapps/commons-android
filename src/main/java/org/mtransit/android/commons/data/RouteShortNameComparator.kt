package org.mtransit.android.commons.data

import androidx.core.text.isDigitsOnly
import org.mtransit.android.commons.ComparatorUtils
import org.mtransit.android.commons.MTLog

class RouteShortNameComparator : Comparator<Route>, MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = RouteShortNameComparator::class.java.simpleName

        private val DIGITS = "\\d+".toRegex()

        private val STARTS_WITH_LETTERS = "^[A-Za-z]+".toRegex(option = RegexOption.IGNORE_CASE)
    }

    override fun getLogTag() = LOG_TAG

    override fun compare(o1: Route, o2: Route): Int {
        val shortName1 = o1.shortName
        val shortName2 = o2.shortName
        if (shortName1 == shortName2) {
            return ComparatorUtils.SAME
        }
        try {
            if (shortName1.isNotEmpty() && shortName2.isNotEmpty()) {
                val noDigits1 = shortName1.none { it.isDigit() }
                val noDigits2 = shortName2.none { it.isDigit() }
                if (noDigits2 || noDigits1) {
                    return if (!noDigits2) {
                        ComparatorUtils.BEFORE
                    } else if (!noDigits1) {
                        ComparatorUtils.AFTER
                    } else { // both no digits -> A-Z compare
                        shortName1.compareTo(shortName2)
                    }
                }
                val digits1 = shortName1.toIntOrNull()
                    ?: DIGITS.find(shortName1)?.value?.toIntOrNull()
                    ?: -1
                val startsWithLetters1 = shortName1.takeUnless { it.isDigitsOnly() }?.let {
                    STARTS_WITH_LETTERS.find(it)?.value
                }.orEmpty()
                val digits2 = shortName2.toIntOrNull()
                    ?: DIGITS.find(shortName2)?.value?.toIntOrNull()
                    ?: -1
                val startsWithLetters2 = shortName2.takeUnless { it.isDigitsOnly() }?.let {
                    STARTS_WITH_LETTERS.find(it)?.value
                }.orEmpty()
                if (digits1 != digits2) {
                    if (startsWithLetters1 != startsWithLetters2) {
                        return startsWithLetters1.compareTo(startsWithLetters2)
                    }
                    return digits1.compareTo(digits2)
                }
            }
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while sorting routes!")
        }
        return shortName1.compareTo(shortName2)
    }

    fun areDifferent(o1: Route, o2: Route) = o1.shortName != o2.shortName

    fun areComparable(o1: Route, o2: Route) = o1.shortName.isNotEmpty() && o2.shortName.isNotEmpty()
}
