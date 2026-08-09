package org.mtransit.android.commons.provider.agency

import android.content.Context
import org.mtransit.android.commons.BuildConfig
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.R
import java.util.TimeZone

object AgencyUtils : MTLog.Loggable {

    private val LOG_TAG: String = AgencyUtils::class.java.simpleName

    override fun getLogTag() = LOG_TAG

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

    @JvmStatic
    fun getAgencyTimeZoneId(context: Context): String =
        context.getAgencyString(
            R.string.poi_agency_timezone,
            R.string.gtfs_rts_timezone, // do not change to avoid breaking compat w/ old modules
            R.string.bike_station_timezone,
        ) ?: run {
            if (BuildConfig.DEBUG) {
                throw RuntimeException("No agency timezone configured!")
            }
            MTLog.w(LOG_TAG, "No agency timezone configured (using device timezone)!")
            TimeZone.getDefault().id
        }
}
