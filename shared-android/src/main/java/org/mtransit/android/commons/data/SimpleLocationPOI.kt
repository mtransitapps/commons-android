package org.mtransit.android.commons.data

data class SimpleLocationPOI(
    override val poi: POI,
    override var distance: Float = -1f,
    override var distanceString: CharSequence? = null,
) : LocationPOI {
    override val hasLocation: Boolean get() = poi.hasLocation()
    override val lat: Double get() = poi.lat
    override val lng: Double get() = poi.lng
}
