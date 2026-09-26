package org.mtransit.android.commons.location

import android.location.Location as AndroidLocation

fun AndroidLocation.toStringSimple() = buildString {
    append("Location[")
    provider?.let { append("provider: ").append(it).append(", ") }
    append("lat: ").append(latitude).append(", ")
    append("lng: ").append(longitude).append(", ")
    append("acc: ").append(accuracy).append(", ")
    append("]")
}
