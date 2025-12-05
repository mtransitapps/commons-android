package org.mtransit.android.commons.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mtransit.android.commons.provider.gtfs.GTFSStatusProvider
import org.mtransit.commons.GTFSCommons
import androidx.core.util.Pair as androidXPair

class GTFSStatusProviderTest {

    @Test
    fun test_filterServiceIdOrInts_simple() {
        val serviceIdAndExceptionTypes: Set<androidXPair<String, Int?>> = setOf(
            androidXPair("TRAIN-A23-Blocks-Semaine-09", GTFSCommons.EXCEPTION_TYPE_DEFAULT), // 20231221 // from calendar.txt
        )
        val usingAnotherDate = false

        val result = GTFSStatusProvider.filterServiceIdOrInts(serviceIdAndExceptionTypes, usingAnotherDate)

        assertEquals(1, result.size)
        assertTrue(result.contains("TRAIN-A23-Blocks-Semaine-09"))
    }

    @Test
    fun test_filterServiceIdOrInts_simple_usingAnotherDay() {
        val serviceIdAndExceptionTypes: Set<androidXPair<String, Int?>> = setOf(
            androidXPair("TRAIN-A23-Blocks-Semaine-09", GTFSCommons.EXCEPTION_TYPE_DEFAULT), // 20231221 // from calendar.txt
        )
        val usingAnotherDate = true

        val result = GTFSStatusProvider.filterServiceIdOrInts(serviceIdAndExceptionTypes, usingAnotherDate)

        assertEquals(1, result.size)
        assertTrue(result.contains("TRAIN-A23-Blocks-Semaine-09"))
    }

    @Test
    fun test_filterServiceIdOrInts_noCalendarTxt() {
        val serviceIdAndExceptionTypes: Set<androidXPair<String, Int?>> = setOf(
            androidXPair("TRAIN-A23-Blocks-Semaine-09", GTFSCommons.EXCEPTION_TYPE_ADDED), // 20231221 // from calendar_dates.txt
        )
        val usingAnotherDate = false

        val result = GTFSStatusProvider.filterServiceIdOrInts(serviceIdAndExceptionTypes, usingAnotherDate)

        assertEquals(1, result.size)
        assertTrue(result.contains("TRAIN-A23-Blocks-Semaine-09"))
    }

    @Test
    fun test_filterServiceIdOrInts_noCalendarTxt_usingAnotherDay() {
        val serviceIdAndExceptionTypes: Set<androidXPair<String, Int?>> = setOf(
            androidXPair("TRAIN-A23-Blocks-Semaine-09", GTFSCommons.EXCEPTION_TYPE_ADDED), // 20231221 // from calendar_dates.txt
        )
        val usingAnotherDate = true

        val result = GTFSStatusProvider.filterServiceIdOrInts(serviceIdAndExceptionTypes, usingAnotherDate)

        assertEquals(1, result.size)
        assertTrue(result.contains("TRAIN-A23-Blocks-Semaine-09"))
    }

    @Test
    fun test_filterServiceIdOrInts_exceptionDate() {
        val serviceIdAndExceptionTypes: Set<androidXPair<String, Int?>> = setOf(
            androidXPair("TRAIN-A23-Blocks-Dimanche-03", GTFSCommons.EXCEPTION_TYPE_DEFAULT), // 20231224 // from calendar.txt
            androidXPair("TRAIN-A23-Blocks-Dimanche-03", GTFSCommons.EXCEPTION_TYPE_REMOVED), // 20231224 // from calendar_dates.txt
            androidXPair("TRAIN-A23-Blocks-Fête-1-03", GTFSCommons.EXCEPTION_TYPE_ADDED), // 20231224 // from calendar_dates.txt
        )
        val usingAnotherDate = false

        val result = GTFSStatusProvider.filterServiceIdOrInts(serviceIdAndExceptionTypes, usingAnotherDate)

        assertEquals(1, result.size)
        assertTrue(result.contains("TRAIN-A23-Blocks-Fête-1-03"))
    }

    @Test
    fun test_filterServiceIdOrInts_exceptionDate_usingAnotherDay() {
        val serviceIdAndExceptionTypes: Set<androidXPair<String, Int?>> = setOf(
            androidXPair("TRAIN-A23-Blocks-Dimanche-03", GTFSCommons.EXCEPTION_TYPE_DEFAULT), // 20231224 // from calendar.txt
            androidXPair("TRAIN-A23-Blocks-Dimanche-03", GTFSCommons.EXCEPTION_TYPE_REMOVED), // 20231224 // from calendar_dates.txt
            androidXPair("TRAIN-A23-Blocks-Fête-1-03", GTFSCommons.EXCEPTION_TYPE_ADDED), // 20231224 // from calendar_dates.txt
        )
        val usingAnotherDate = true

        val result = GTFSStatusProvider.filterServiceIdOrInts(serviceIdAndExceptionTypes, usingAnotherDate)

        assertEquals(1, result.size)
        assertTrue(result.contains("TRAIN-A23-Blocks-Dimanche-03"))
    }

    @Test
    fun test_filterServiceIds_exceptionDate_serviceAdded() {
        val serviceIdAndExceptionTypes: Set<androidXPair<String, Int?>> = setOf(
            androidXPair("TRAIN-A23-Blocks-Dimanche-03", GTFSCommons.EXCEPTION_TYPE_DEFAULT), // 20231224 // from calendar.txt
            androidXPair("TRAIN-A23-Blocks-Fête-1-03", GTFSCommons.EXCEPTION_TYPE_ADDED), // 20231224 // from calendar_dates.txt
        )
        val usingAnotherDate = false

        val result = GTFSStatusProvider.filterServiceIdOrInts(serviceIdAndExceptionTypes, usingAnotherDate)

        assertEquals(2, result.size)
        assertTrue(result.contains("TRAIN-A23-Blocks-Fête-1-03"))
        assertTrue(result.contains("TRAIN-A23-Blocks-Dimanche-03"))
    }

    @Test
    fun test_filterServiceIds_exceptionDate_serviceAdded_usingAnotherDay() {
        val serviceIdAndExceptionTypes: Set<androidXPair<String, Int?>> = setOf(
            androidXPair("TRAIN-A23-Blocks-Dimanche-03", GTFSCommons.EXCEPTION_TYPE_DEFAULT), // 20231224 // from calendar.txt
            androidXPair("TRAIN-A23-Blocks-Fête-1-03", GTFSCommons.EXCEPTION_TYPE_ADDED), // 20231224 // from calendar_dates.txt
        )
        val usingAnotherDate = true

        val result = GTFSStatusProvider.filterServiceIdOrInts(serviceIdAndExceptionTypes, usingAnotherDate)

        assertEquals(1, result.size)
        assertTrue(result.contains("TRAIN-A23-Blocks-Dimanche-03"))
    }

    @Test
    fun test_filterServiceIds_exceptionDate_serviceRemoved() {
        val serviceIdAndExceptionTypes: Set<androidXPair<String, Int?>> = setOf(
            androidXPair("TRAIN-A23-Blocks-Dimanche-03", GTFSCommons.EXCEPTION_TYPE_DEFAULT), // 20231224 // from calendar.txt
            androidXPair("TRAIN-A23-Blocks-Dimanche-03", GTFSCommons.EXCEPTION_TYPE_REMOVED), // 20231224 // from calendar_dates.txt
        )
        val usingAnotherDate = false

        val result = GTFSStatusProvider.filterServiceIdOrInts(serviceIdAndExceptionTypes, usingAnotherDate)

        assertEquals(0, result.size)
    }

    @Test
    fun test_filterServiceIds_exceptionDate_serviceRemoved_usingAnotherDay() {
        val serviceIdAndExceptionTypes: Set<androidXPair<String, Int?>> = setOf(
            androidXPair("TRAIN-A23-Blocks-Dimanche-03", GTFSCommons.EXCEPTION_TYPE_DEFAULT), // 20231224 // from calendar.txt
            androidXPair("TRAIN-A23-Blocks-Dimanche-03", GTFSCommons.EXCEPTION_TYPE_REMOVED), // 20231224 // from calendar_dates.txt
        )
        val usingAnotherDate = true

        val result = GTFSStatusProvider.filterServiceIdOrInts(serviceIdAndExceptionTypes, usingAnotherDate)

        assertEquals(1, result.size)
        assertTrue(result.contains("TRAIN-A23-Blocks-Dimanche-03"))
    }
}