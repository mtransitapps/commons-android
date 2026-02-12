package org.mtransit.android.commons.provider.agency

import android.content.Context
import org.mtransit.android.commons.R

object AgencyUtils {

    fun getAgencyShortName(context: Context) =
        context.getString(R.string.poi_agency_short_name).takeIf { it.isNotBlank() }
            ?: context.getString(R.string.gtfs_rts_short_name).takeIf { it.isNotBlank() } // do not change to avoid breaking compat w/ old modules
            ?: context.getString(R.string.bike_station_short_name).takeIf { it.isNotBlank() }

    fun getAgencyColor(context: Context) =
        context.getString(R.string.poi_agency_color).takeIf { it.isNotBlank() }
            ?: context.getString(R.string.gtfs_rts_color).takeIf { it.isNotBlank() } // do not change to avoid breaking compat w/ old modules
            ?: context.getString(R.string.bike_station_color).takeIf { it.isNotBlank() }

    fun getAgencyAuthority(context: Context) =
        context.getString(R.string.poi_agency_authority).takeIf { it.isNotBlank() }
            ?: context.getString(R.string.gtfs_rts_authority).takeIf { it.isNotBlank() } // do not change to avoid breaking compat w/ old modules
            ?: context.getString(R.string.bike_station_authority).takeIf { it.isNotBlank() }


    private var _timeZone: String? = null

    @JvmStatic
    fun getRDSAgencyTimeZone(context: Context) =
        _timeZone ?: context.getString(R.string.gtfs_rts_timezone).also { _timeZone = it } // do not change to avoid breaking compat w/ old modules
}