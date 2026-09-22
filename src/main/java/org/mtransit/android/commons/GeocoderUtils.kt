package org.mtransit.android.commons

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeocoderUtils {

    private var _geocoder: Geocoder? = null

    private fun getGeocoder(context: Context): Geocoder? {
        if (_geocoder == null && Geocoder.isPresent()) {
            _geocoder = Geocoder(context)
        }
        return _geocoder
    }

    suspend fun getLocationAddress(context: Context, location: Location): Address? = withContext(Dispatchers.IO) {
        @Suppress("DEPRECATION") // requires min SDK 33
        getGeocoder(context)?.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
    }
}

suspend fun Location.toAddress(context: Context): Address? = GeocoderUtils.getLocationAddress(context, this)
