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

    @JvmStatic
    fun List<GtfsRealtime.FeedEntity>.toAlerts(): List<GtfsRealtime.Alert> = this.filter { it.hasAlert() }.map { it.alert }

    @JvmStatic
    fun List<GtfsRealtime.Alert>.sort(now: Long = TimeUtils.currentTimeMillis()): List<GtfsRealtime.Alert> {
        return this.sortedBy {
            (it.getActivePeriod(now)
                ?: it.getFirstActivePeriod())?.start
                ?: Long.MAX_VALUE // no active period == displayed as long as in the feed (probably less important?)
        }
    }

    @JvmStatic
    fun GtfsRealtime.Alert.getActivePeriod(now: Long = TimeUtils.currentTimeMillis()) = this.activePeriodList
        .filter { it.hasStart() && it.hasEnd() }
        .singleOrNull {
            now in it.start..it.end
        }

    @JvmStatic
    fun GtfsRealtime.Alert.getFirstActivePeriod() = this.activePeriodList
        .firstOrNull {
            it.hasStart() && it.hasEnd()
        }
}