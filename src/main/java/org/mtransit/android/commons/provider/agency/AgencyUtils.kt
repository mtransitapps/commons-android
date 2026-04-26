package org.mtransit.android.commons.provider.agency

import android.content.Context
import org.mtransit.android.commons.R
import java.util.TimeZone

object AgencyUtils {

    fun getAgencyShortName(context: Context) =
        context.getAgencyString(
            R.string.poi_agency_short_name,
            R.string.gtfs_rts_short_name, // do not change to avoid breaking compat w/ old modules
            R.string.bike_station_short_name,
        )

    fun getAgencyColor(context: Context) =
        context.getAgencyString(
            R.string.poi_agency_color,
            R.string.gtfs_rts_color, // do not change to avoid breaking compat w/ old modules
            R.string.bike_station_color,
        )

    fun getAgencyAuthority(context: Context) =
        context.getAgencyString(
            R.string.poi_agency_authority,
            R.string.gtfs_rts_authority, // do not change to avoid breaking compat w/ old modules
            R.string.bike_station_authority,
        )

    private fun Context.getAgencyString(vararg resIds: Int): String? =
        resIds.asSequence()
            .map { getString(it) }
            .firstOrNull { it.isNotBlank() }

    private val _defaultTimeZoneId: String by lazy { TimeZone.getDefault().id }

    @JvmStatic
    fun getRDSAgencyTimeZoneId(context: Context) =
        context.getAgencyString(
            R.string.poi_agency_timezone,
            R.string.gtfs_rts_timezone, // do not change to avoid breaking compat w/ old modules
            R.string.bike_station_timezone,
        ) ?: _defaultTimeZoneId
}