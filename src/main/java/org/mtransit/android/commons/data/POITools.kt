package org.mtransit.android.commons.data

object POITools {

    @JvmStatic
    fun getCoverageArea(pois: Iterable<POI>): Area? {
        if (pois.none()) return null
        val minLat = pois.minOf { it.getLat() }
        val maxLat = pois.maxOf { it.getLat() }
        val minLon = pois.minOf { it.getLng() }
        val maxLon = pois.maxOf { it.getLng() }
        return Area(minLat, maxLat, minLon, maxLon)
    }
}
