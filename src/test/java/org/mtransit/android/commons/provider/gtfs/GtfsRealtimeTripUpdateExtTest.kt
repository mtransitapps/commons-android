package org.mtransit.android.commons.provider.gtfs

import com.google.transit.realtime.TripUpdateKt.stopTimeUpdate
import org.mtransit.android.commons.data.makeRDS
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GtfsRealtimeTripUpdateExtTest {

    private val parseStopId: (String) -> String = { it }

    // region stop time update - same stop

    @Test
    fun test_isSameStop() {
        // STU - no stop ID / SEQ filter
        val rdsStop1234 = makeRDS(stopOriginalIdHash = 1234)
        assertFalse { stopTimeUpdate { }.isSameStop(rdsStop1234, 1, parseStopId) }
        assertFalse { stopTimeUpdate { }.isSameStop(null, 1, parseStopId) }
        // STU - stop ID only
        assertTrue { stopTimeUpdate { stopId = "1234" }.isSameStop(rdsStop1234, 1, parseStopId) }
        assertFalse { stopTimeUpdate { stopId = "5678" }.isSameStop(rdsStop1234, 1, parseStopId) }
        // STU - stop SEQ only
        assertTrue { stopTimeUpdate { stopSequence = 7 }.isSameStop(rdsStop1234, 7, parseStopId) }
        assertFalse { stopTimeUpdate { stopSequence = 1 }.isSameStop(rdsStop1234, 7, parseStopId) }
        // STU - stop ID & stop SEQ
        assertTrue { stopTimeUpdate { stopId = "1234"; stopSequence = 7 }.isSameStop(rdsStop1234, 7, parseStopId) }
        assertFalse { stopTimeUpdate { stopId = "1234"; stopSequence = 7 }.isSameStop(rdsStop1234, 1, parseStopId) }
        assertFalse { stopTimeUpdate { stopId = "5678"; stopSequence = 7 }.isSameStop(rdsStop1234, 7, parseStopId) }
        assertFalse { stopTimeUpdate { stopId = "5678"; stopSequence = 7 }.isSameStop(rdsStop1234, 1, parseStopId) }
    }

    // endregion stop time update - same stop

    // region stop time update - match

    @Test
    fun test_match() {
        val rdsStop1234 = makeRDS(stopOriginalIdHash = 1234)
        val rdsStop1234List = listOf(rdsStop1234)
        val findStopTimestamp: (String, Int) -> Boolean = { _, gSTUStopSeq -> gSTUStopSeq == 7 }
        // STU - no stop ID / SEQ filter
        assertFalse { stopTimeUpdate { }.match(rdsStop1234List, findStopTimestamp, parseStopId) }
        assertFalse { stopTimeUpdate { }.match(emptyList(), findStopTimestamp, parseStopId) }
        // STU - stop ID only
        assertTrue { stopTimeUpdate { stopId = "1234" }.match(rdsStop1234List, findStopTimestamp, parseStopId) }
        assertFalse { stopTimeUpdate { stopId = "5678" }.match(rdsStop1234List, findStopTimestamp, parseStopId) }
        // STU - stop SEQ only
        assertTrue { stopTimeUpdate { stopSequence = 7 }.match(rdsStop1234List, findStopTimestamp, parseStopId) }
        assertFalse { stopTimeUpdate { stopSequence = 1 }.match(rdsStop1234List, findStopTimestamp, parseStopId) }
        // STU - stop ID & stop SEQ
        assertTrue { stopTimeUpdate { stopId = "1234"; stopSequence = 7 }.match(rdsStop1234List, findStopTimestamp, parseStopId) }
        assertFalse { stopTimeUpdate { stopId = "1234"; stopSequence = 1 }.match(rdsStop1234List, findStopTimestamp, parseStopId) }
        assertFalse { stopTimeUpdate { stopId = "5678"; stopSequence = 7 }.match(rdsStop1234List, findStopTimestamp, parseStopId) }
        assertFalse { stopTimeUpdate { stopId = "5678"; stopSequence = 1 }.match(rdsStop1234List, findStopTimestamp, parseStopId) }
    }

    // endregion top time update - match
}
