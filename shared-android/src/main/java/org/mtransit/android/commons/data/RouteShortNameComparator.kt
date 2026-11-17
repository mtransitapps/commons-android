package org.mtransit.android.commons.data

import androidx.core.text.isDigitsOnly
import org.mtransit.android.commons.ComparatorUtils
import org.mtransit.android.commons.MTLog

class RouteShortNameComparator : Comparator<Route>, MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = RouteShortNameComparator::class.java.simpleName

        private val DIGITS = "\\d+".toRegex()

        private val STARTS_WITH_LETTERS = "^\\p{L}+".toRegex()
    }

    override fun getLogTag() = LOG_TAG

    override fun compare(o1: Route, o2: Route): Int {
        val shortName1 = o1.shortName
        val shortName2 = o2.shortName
        if (shortName1 == shortName2) {
            return ComparatorUtils.SAME
        }
        try {
            val empty1 = shortName1.isEmpty()
            val empty2 = shortName2.isEmpty()
            if (empty1 || empty2) {
                return when {
                    empty1 && empty2 -> ComparatorUtils.SAME
                    empty1 -> ComparatorUtils.BEFORE
                    else -> ComparatorUtils.AFTER
                }
            }
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
            val digits1 = shortName1.takeIf { it.isDigitsOnly() }
                ?: DIGITS.find(shortName1)?.value
            val startsWithLetters1 = shortName1.takeUnless { it.isDigitsOnly() }?.let {
                STARTS_WITH_LETTERS.find(it)?.value
            }.orEmpty()
            val digits2 = shortName2.takeIf { it.isDigitsOnly() }
                ?: DIGITS.find(shortName2)?.value
            val startsWithLetters2 = shortName2.takeUnless { it.isDigitsOnly() }?.let {
                STARTS_WITH_LETTERS.find(it)?.value
            }.orEmpty()
            val digitsCompare = when {
                digits1 == null && digits2 == null -> ComparatorUtils.SAME
                digits1 == null -> ComparatorUtils.BEFORE
                digits2 == null -> ComparatorUtils.AFTER
                else -> {
                    val d1 = digits1.trimStart('0')
                    val d2 = digits2.trimStart('0')
                    if (d1.length != d2.length) {
                        d1.length.compareTo(d2.length)
                    } else if (d1 != d2) {
                        d1.compareTo(d2)
                    } else {
                        digits1.length.compareTo(digits2.length)
                    }
                }
            }
            if (startsWithLetters1 != startsWithLetters2) {
                return startsWithLetters1.compareTo(startsWithLetters2)
            }
            if (digitsCompare != ComparatorUtils.SAME) {
                return digitsCompare
            }
        } catch (e: Exception) {
            MTLog.w(this, e, "Error while sorting routes!")
        }
        return shortName1.compareTo(shortName2)
    }

    fun areDifferent(o1: Route, o2: Route) = o1.shortName != o2.shortName

    @Suppress("FunctionOnlyReturningConstant")
    fun areComparable(@Suppress("unused") o1: Route, @Suppress("unused") o2: Route) =
        true // always comparable
}
