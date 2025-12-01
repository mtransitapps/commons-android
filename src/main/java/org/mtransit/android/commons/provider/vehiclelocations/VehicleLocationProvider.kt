package org.mtransit.android.commons.provider.vehiclelocations

import android.content.UriMatcher
import org.mtransit.android.commons.provider.ContentProviderConstants
import org.mtransit.android.commons.provider.MTContentProvider

abstract class VehicleLocationProvider : MTContentProvider(),
    VehicleLocationProviderContract
{
    companion object {
        val LOG_TAG: String = VehicleLocationProvider::class.java.simpleName

        fun getNewUriMatcher(authority: String) = UriMatcher(UriMatcher.NO_MATCH).apply {
            append(this, authority)
        }

        fun append(uriMatcher: UriMatcher, authority: String) {
            uriMatcher.addURI(authority, VehicleLocationProviderContract.PING_PATH, ContentProviderConstants.PING)
            uriMatcher.addURI(authority, VehicleLocationProviderContract.VEHICLE_LOCATION_PATH, ContentProviderConstants.VEHICLE_LOCATION)
        }
    }

    override fun getLogTag() = LOG_TAG



}