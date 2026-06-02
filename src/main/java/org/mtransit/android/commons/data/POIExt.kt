package org.mtransit.android.commons.data

import org.mtransit.commons.GTFSCommons

fun RouteDirectionStop.toRouteDirection() = makeRouteDirection(
    route = route,
    direction = direction
)

fun makeRoute(
    authority: String,
    id: Long,
    shortName: String,
    longName: String,
    color: String,
    routeOriginalIdHash: Int? = GTFSCommons.DEFAULT_ID_HASH,
    type: Int? = GTFSCommons.DEFAULT_ROUTE_TYPE,
) = Route(
    authority,
    id,
    shortName,
    longName,
    color,
    routeOriginalIdHash,
    type,
)

fun makeDirection(
    authority: String,
    id: Long,
    headsignType: Int,
    headsignValue: String,
    routeId: Long,
) = Direction(
    authority,
    id,
    headsignType,
    headsignValue,
    routeId,
)

fun makeRouteDirection(
    route: Route,
    direction: Direction,
) = RouteDirection(
    route,
    direction,
)