package org.mtransit.android.commons.provider.gtfs

import org.mtransit.android.commons.data.POI
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection

interface GTFSRealTimeProviderFilter {
    val poi: POI?
    val routeDirection: RouteDirection?
    val route: Route?
}
