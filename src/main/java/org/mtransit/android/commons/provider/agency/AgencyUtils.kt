package org.mtransit.android.commons.provider.agency

import android.content.Context
import org.mtransit.android.commons.R

object AgencyUtils {

    fun getAgencyColor(context: Context) =
        context.getString(R.string.gtfs_rts_color).takeIf { it.isNotBlank() }
            ?: context.getString(R.string.bike_station_color).takeIf { it.isNotBlank() }

    fun getAgencyAuthority(context: Context) =
        context.getString(R.string.gtfs_rts_authority).takeIf { it.isNotBlank() }
            ?: context.getString(R.string.bike_station_authority).takeIf { it.isNotBlank() }
}