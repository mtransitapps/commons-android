package org.mtransit.android.commons.data

object POITools {

    @JvmStatic
    fun getCoverageArea(pois: Iterable<POI>): Area? {
        if (pois.none()) return null
        return Area(
            minLat = pois.minOf { it.getLat() },
            maxLat = pois.maxOf { it.getLat() },
            minLng = pois.minOf { it.getLng() },
            maxLng = pois.maxOf { it.getLng() },
        )
    }
}
