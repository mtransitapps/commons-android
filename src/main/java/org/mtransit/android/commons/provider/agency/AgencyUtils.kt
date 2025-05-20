package org.mtransit.android.commons.provider.agency

import android.content.Context
import org.mtransit.android.commons.R

object AgencyUtils {

    fun getAgencyShortName(context: Context) =
        context.getString(R.string.gtfs_rts_short_name).takeIf { it.isNotBlank() }
            ?: context.getString(R.string.bike_station_short_name).takeIf { it.isNotBlank() }

    fun getAgencyColor(context: Context) =
        context.getString(R.string.gtfs_rts_color).takeIf { it.isNotBlank() }
            ?: context.getString(R.string.bike_station_color).takeIf { it.isNotBlank() }

    fun getAgencyAuthority(context: Context) =
        context.getString(R.string.gtfs_rts_authority).takeIf { it.isNotBlank() }
            ?: context.getString(R.string.bike_station_authority).takeIf { it.isNotBlank() }


    private var _timeZone: String? = null

    @JvmStatic
    fun getRtsAgencyTimeZone(context: Context) =
        _timeZone ?: context.getString(R.string.gtfs_rts_timezone).also { _timeZone = it }
}