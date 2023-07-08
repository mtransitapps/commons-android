package org.mtransit.android.commons

import com.google.transit.realtime.GtfsRealtime

object GtfsRealtimeExt {

    @JvmStatic
    fun List<GtfsRealtime.TranslatedString.Translation>.filterUseless(): List<GtfsRealtime.TranslatedString.Translation> {
        return if (this.size <= 1) {
            this
        } else {
            this.filterNot { it.text.isNullOrBlank() }
        }
    }
}