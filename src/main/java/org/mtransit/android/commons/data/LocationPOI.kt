package org.mtransit.android.commons.data

interface LocationPOI {

    val poi: POI

    val lat: Double

    val lng: Double

    val hasLocation: Boolean

    var distanceString: CharSequence?

    var distance: Float
}
