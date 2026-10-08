@file:Suppress("unused")

package org.mtransit.android.commons

import org.mtransit.android.commons.LocationUtils.LocationPOI
import org.mtransit.android.commons.LocationUtils.SimpleLocationPOI
import org.mtransit.commons.keepFirst
import org.mtransit.commons.sortWithAnd
import android.location.Location as AndroidLocation

fun <POI : LocationPOI> List<POI>.filterTooFar(maxDistanceInMeters: Float): List<POI> {
    return toMutableList().removeTooFar(maxDistanceInMeters)
}

fun <POI : LocationPOI> MutableList<POI>.removeTooFar(maxDistanceInMeters: Float): MutableList<POI> {
    removeAll { maxDistanceInMeters < it.distance }
    return this
}

fun <POI : LocationPOI> List<POI>.filterTooMuchWhenNotInCoverage(minCoverageInMeters: Float, maxSize: Int): List<POI> {
    return toMutableList().removeTooMuchWhenNotInCoverage(minCoverageInMeters, maxSize)
}

fun <POI : LocationPOI> MutableList<POI>.removeTooMuchWhenNotInCoverage(minCoverageInMeters: Float, maxSize: Int): MutableList<POI> {
    return try {
        sortWithAnd(LocationUtils.POI_DISTANCE_COMPARATOR)
        keepFirst(maxSize) { minCoverageInMeters < it.distance }
    } catch (iae: IllegalArgumentException) { // FIXME POI list not immutable (distance can be updated from another thread)
        MTLog.w(this, iae, "Error while looking for closest POIs")
        this
    }
}

fun <POI : LocationPOI> MutableList<POI>.updateDistanceM(lat: Double, lng: Double): MutableList<POI> {
    this.forEach { poi ->
        if (poi.hasLocation()) {
            poi.distance = LocationUtils.distanceToInMeters(lat, lng, poi.lat, poi.lng)
        }
    }
    return this
}

fun <POI : LocationPOI> List<POI>.updateDistance(lat: Double, lng: Double): List<POI> {
    LocationUtils.updateDistance(this, lat, lng)
    return this
}

fun <POI : LocationPOI> List<POI>.updateDistance(location: AndroidLocation?): List<POI> {
    LocationUtils.updateDistance(this, location)
    return this
}

fun <POI : LocationPOI> Iterable<POI>.toSimplePOIListClone(): MutableList<SimpleLocationPOI> {
    return LocationUtils.toSimplePOIListClone(this)
}

fun <POI : LocationPOI> Iterable<POI>.findClosestPOIUuids() = findClosestPOIIdxUuids().map { (_, uuid) -> uuid }

fun <POI : LocationPOI> Iterable<POI>.findClosestPOIIdxUuids() = buildList<Pair<Int, String>> {
    try {
        this@findClosestPOIIdxUuids.toSimplePOIListClone() // need to create a new list to NOT sort the original list
            .sortWithAnd(LocationUtils.POI_DISTANCE_COMPARATOR) // do NOT sort original list
            .firstOrNull { it.distanceOrNull != null }?.distanceOrNull?.let { theClosestDistance ->
                for ((index, poim) in this@findClosestPOIIdxUuids.withIndex()) { // need to go through the entire original list to get the right indexes
                    poim.distanceOrNull?.let { distance ->
                        if (distance <= theClosestDistance) {
                            add(index to poim.poi.uuid)
                        }
                    }
                }
            }
    } catch (iae: IllegalArgumentException) { // FIXME POI list not immutable (distance can be updated from another thread)
        MTLog.w(this, iae, "Error while looking for closest POIs")
    }
}

val LocationPOI.distanceOrNull: Float? get() = this.distance.takeIf { it >= 0f }

val Float.milesToFeet: Float get() = this * LocationUtils.FEET_PER_MILE
val Float.metersToFeet: Float get() = this * LocationUtils.FEET_PER_METER
val Float.kilometersToMeter: Float get() = this * LocationUtils.METER_PER_KM
