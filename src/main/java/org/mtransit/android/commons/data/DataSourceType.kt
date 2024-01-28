package org.mtransit.android.commons.data

import androidx.annotation.IntDef

object DataSourceTypeId {

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        INVALID,
        LIGHT_RAIL, SUBWAY, RAIL, BUS, FERRY,
        EX_TRAM,
        BIKE, PLACE, MODULE,
        FAVORITE, NEWS,
    )
    annotation class DataSourceType

    const val INVALID = -1

    const val LIGHT_RAIL = 0
    const val SUBWAY = 1
    const val RAIL = 2
    const val BUS = 3
    const val FERRY = 4

    const val EX_TRAM = 900 // Tram service - Streetcar

    const val BIKE = 100
    const val PLACE = 666
    const val MODULE = 999

    const val FAVORITE = 777
    const val NEWS = 888

}