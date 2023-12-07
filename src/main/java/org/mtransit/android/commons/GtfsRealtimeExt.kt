package org.mtransit.android.commons

import com.google.transit.realtime.GtfsRealtime
import org.mtransit.android.commons.GtfsRealtimeExt.originalIdToHash
import org.mtransit.commons.FeatureFlags
import org.mtransit.commons.GTFSCommons

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

    @JvmStatic
    fun GtfsRealtime.EntitySelector.getRouteIdHash(): String {
        if (!FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
            return this.routeId
        }
        return this.routeId.originalIdToHash()
    }

    @JvmStatic
    fun GtfsRealtime.EntitySelector.getStopIdHash(): String {
        if (!FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
            return this.stopId
        }
        return this.stopId.originalIdToHash()
    }

    @JvmStatic
    fun String.originalIdToHash(): String {
        if (!FeatureFlags.F_EXPORT_GTFS_ID_HASH_INT) {
            return this
        }
        return GTFSCommons.stringIdToHash(this).toString()
    }
}