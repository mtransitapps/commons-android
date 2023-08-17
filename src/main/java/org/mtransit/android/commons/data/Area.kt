package org.mtransit.android.commons.data

import android.database.Cursor
import androidx.room.Ignore
import org.mtransit.android.commons.LocationUtils
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.provider.AgencyProviderContract
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * latitude = south <> north = horizontal lines
 * longitude = west <> east = vertical lines
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
data class Area(
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double,
) : MTLog.Loggable {

    override fun getLogTag(): String = LOG_TAG

    @Ignore
    val northLat = this.maxLat

    @Ignore
    val southLat = this.minLat

    // FIXME not always: -180...0...+180 (In Pacific Ocean, E of NZ...)
    @Ignore
    val eastLng = this.maxLng

    // FIXME not always: -180...0...+180 (In Pacific Ocean, E of NZ...)
    @Ignore
    val westLng = this.minLng

    @Ignore
    val centerLat = this.minLat + abs(this.minLat - this.maxLat) / 2.0

    @Ignore
    val centerLng = this.minLng + abs(this.minLng - this.maxLng) / 2.0

    @Ignore
    val center: String = "$centerLat, $centerLng"

    fun isEntirelyInside(otherArea: Area?): Boolean {
        if (otherArea == null) {
            return false
        }
        if (!isInside(minLat, minLng, otherArea)) {
            return false // min lat, min lng
        }
        if (!isInside(minLat, maxLng, otherArea)) {
            return false // min lat, max lng
        }
        if (!isInside(maxLat, minLng, otherArea)) {
            return false // max lat, min lng
        }
        if (!isInside(maxLat, maxLng, otherArea)) {
            return false // max lat, max lng
        }
        return true
    }

    companion object {

        private val LOG_TAG = Area::class.java.simpleName

        const val MAX_LAT: Double = 90.0
        const val MIN_LAT: Double = -90.0
        const val MAX_LNG: Double = 180.0
        const val MIN_LNG: Double = -180.0

        @JvmStatic
        val THE_WORLD = Area(MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG)

        fun getArea(lat: Double, lng: Double, aroundDiff: Double): Area {
            val latTrunc = abs(lat)
            val latBefore = sign(lat) * LocationUtils.truncAround(latTrunc - aroundDiff).toDouble()
            val latAfter = sign(lat) * LocationUtils.truncAround(latTrunc + aroundDiff).toDouble()
            val lngTrunc = abs(lng)
            val lngBefore = sign(lng) * LocationUtils.truncAround(lngTrunc - aroundDiff).toDouble()
            val lngAfter = sign(lng) * LocationUtils.truncAround(lngTrunc + aroundDiff).toDouble()
            var minLat = min(latBefore, latAfter)
            if (minLat < LocationUtils.MIN_LAT) {
                minLat = LocationUtils.MIN_LAT
            }
            var maxLat = max(latBefore, latAfter)
            if (maxLat > LocationUtils.MAX_LAT) {
                maxLat = LocationUtils.MAX_LAT
            }
            var minLng = min(lngBefore, lngAfter)
            if (minLng < LocationUtils.MIN_LNG) {
                minLng = LocationUtils.MIN_LNG
            }
            var maxLng = max(lngBefore, lngAfter)
            if (maxLng > LocationUtils.MAX_LNG) {
                maxLng = LocationUtils.MAX_LNG
            }
            return Area(minLat, maxLat, minLng, maxLng)
        }

        fun isInside(lat: Double, lng: Double, area: Area?): Boolean {
            return if (area == null) {
                false
            } else isInside(lat, lng, area.minLat, area.maxLat, area.minLng, area.maxLng)
        }

        fun isInside(lat: Double, lng: Double, minLat: Double, maxLat: Double, minLng: Double, maxLng: Double): Boolean {
            return lat in minLat..maxLat && lng in minLng..maxLng
        }

        fun areOverlapping(area1: Area?, area2: Area?): Boolean {
            if (area1 == null || area2 == null) {
                return false // no data to compare
            }
            // AREA1 (at least partially) INSIDE AREA2
            if (isInside(area1.minLat, area1.minLng, area2)) {
                return true // min lat, min lng
            }
            if (isInside(area1.minLat, area1.maxLng, area2)) {
                return true // min lat, max lng
            }
            if (isInside(area1.maxLat, area1.minLng, area2)) {
                return true // max lat, min lng
            }
            if (isInside(area1.maxLat, area1.maxLng, area2)) {
                return true // max lat, max lng
            }
            // AREA2 (at least partially) INSIDE AREA1
            if (isInside(area2.minLat, area2.minLng, area1)) {
                return true // min lat, min lng
            }
            if (isInside(area2.minLat, area2.maxLng, area1)) {
                return true // min lat, max lng
            }
            if (isInside(area2.maxLat, area2.minLng, area1)) {
                return true // max lat, min lng
            }
            if (isInside(area2.maxLat, area2.maxLng, area1)) {
                return true // max lat, max lng
            }
            // OVERLAPPING
            return areCompletelyOverlapping(area1, area2)
        }

        /**
         * <pre>
         *        +--+
         *        |  |
         *   +----+--+----+
         *   |    |  |    |
         *   +----+--+----+
         *        |  |
         *        +--+
         * </pre>
         */
        private fun areCompletelyOverlapping(area1: Area, area2: Area): Boolean {
            if (area1.minLat >= area2.minLat && area1.maxLat <= area2.maxLat) {
                if (area2.minLng >= area1.minLng && area2.maxLng <= area1.maxLng) {
                    return true // area 1 wider than area 2 but area 2 higher than area 1
                }
            }
            if (area2.minLat >= area1.minLat && area2.maxLat <= area1.maxLat) {
                if (area1.minLng >= area2.minLng && area1.maxLng <= area2.maxLng) {
                    return true // area 2 wider than area 1 but area 1 higher than area 2
                }
            }
            return false
        }

        @JvmStatic
        fun fromCursor(cursor: Cursor?): Area? {
            return if (cursor == null) {
                null
            } else try {
                fromCursorNN(cursor)
            } catch (e: Exception) {
                MTLog.w(LOG_TAG, e, "Error while reading cursor!")
                null
            }
        }

        @JvmStatic
        @Throws(IllegalArgumentException::class)
        fun fromCursorNN(cursor: Cursor): Area {
            val minLat = cursor.getDouble(cursor.getColumnIndexOrThrow(AgencyProviderContract.AREA_MIN_LAT))
            val maxLat = cursor.getDouble(cursor.getColumnIndexOrThrow(AgencyProviderContract.AREA_MAX_LAT))
            val minLng = cursor.getDouble(cursor.getColumnIndexOrThrow(AgencyProviderContract.AREA_MIN_LNG))
            val maxLng = cursor.getDouble(cursor.getColumnIndexOrThrow(AgencyProviderContract.AREA_MAX_LNG))
            return Area(minLat, maxLat, minLng, maxLng)
        }
    }
}
