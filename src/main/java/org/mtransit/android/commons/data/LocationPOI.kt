package org.mtransit.android.commons.data

import org.mtransit.android.commons.ComparatorUtils
import org.mtransit.android.commons.distanceOrNull

interface LocationPOI {

    companion object {
        @JvmStatic
        val ALPHA_COMPARATOR = Comparator<LocationPOI> { o1: LocationPOI, o2: LocationPOI ->
            POI_ALPHA_COMPARATOR.compare(o1.poi, o2.poi)
        }

        @JvmStatic
        val DISTANCE_COMPARATOR = Comparator<LocationPOI> { o1: LocationPOI, o2: LocationPOI ->
            val distance1 = o1.distanceOrNull
            val distance2 = o2.distanceOrNull
            if (distance1 == null && distance2 != null) {
                ComparatorUtils.AFTER
            } else if (distance1 != null && distance2 == null) {
                ComparatorUtils.BEFORE
            } else {
                o1.distance.compareTo(o2.distance)
            }
        }
    }

    val poi: POI

    val lat: Double

    val lng: Double

    val hasLocation: Boolean

    var distanceString: CharSequence?

    var distance: Float
}
