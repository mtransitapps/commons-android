package org.mtransit.android.commons.data

import org.mtransit.android.commons.MTLog

class DirectionHeadSignComparator : Comparator<Direction>, MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = DirectionHeadSignComparator::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    override fun compare(o1: Direction, o2: Direction): Int {
        val headsign1 = o1.heading
            ?: return o1.headsignValue.compareTo(o2.headsignValue) // IF some heading null DO fallback to head-sign value directly
        val headsign2 = o2.heading
            ?: return o1.headsignValue.compareTo(o2.headsignValue) // IF some heading null DO fallback to head-sign value directly
        return headsign1.compareTo(headsign2)
    }

    fun areDifferent(o1: Direction, o2: Direction) =
        o1.headsignType != o2.headsignType
            || o1.headsignValue != o2.headsignValue

    fun areComparable(@Suppress("unused") o1: Direction, @Suppress("unused") o2: Direction) =
        true // always comparable
}
