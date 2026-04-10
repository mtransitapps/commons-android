package org.mtransit.android.commons.data

import org.mtransit.android.commons.provider.GTFSRealTimeProvider

fun makeRDS(
    authority: String = "authority",
    routeId: Long = 1L,
    routeOriginalIdHash: Int? = routeId.toString().hashCode(),
    routeType: Int = 3,
    originalDirectionId: Int? = 1,
    directionId: Long = originalDirectionId?.let { routeId * 100L + it } ?: (routeId * 100L + 9L),
    stopId: Int = 1,
    stopOriginalIdHash: Int? = stopId.toString().hashCode() // stopId, // "$stopId".hashCode()
) = RouteDirectionStop(
    1,
    Route(
        authority,
        routeId,
        "#$routeId",
        "route $routeId",
        "color",
        routeOriginalIdHash,
        routeType,
    ),
    Direction(
        authority,
        directionId,
        Direction.HEADSIGN_TYPE_STRING,
        "Head-Sign $originalDirectionId",
        routeId,
    ),
    Stop(
        stopId,
        "#$stopId",
        "Stop #$stopId",
        1.0,
        2.0,
        Accessibility.DEFAULT,
        stopOriginalIdHash,
    ),
    false,
    false,
)

fun Route.getGTFSRTTargetUUID(): String =
    GTFSRealTimeProvider.getAgencyRouteTagTargetUUID(
        authority,
        originalIdHash.toString()
    )

fun RouteDirection.getGTFSRTTargetUUID(): String =
    GTFSRealTimeProvider.getAgencyRouteDirectionTagTargetUUID(
        authority,
        route.originalIdHash.toString(),
        direction.originalDirectionIdOrNull,
    ) ?: GTFSRealTimeProvider.getAgencyRouteTagTargetUUID(
        authority,
        route.originalIdHash.toString()
    )

fun RouteDirectionStop.getGTFSRTTargetUUID(includeStopTags: Boolean): String =
    if (!includeStopTags) this.toRouteDirection().getGTFSRTTargetUUID()
    else GTFSRealTimeProvider.getAgencyRouteDirectionStopTagTargetUUID(
        authority,
        route.originalIdHash.toString(),
        direction.originalDirectionIdOrNull,
        stop.originalIdHashString
    ) ?: GTFSRealTimeProvider.getAgencyRouteStopTagTargetUUID(
        authority,
        route.originalIdHash.toString(),
        stop.originalIdHashString
    ) ?: GTFSRealTimeProvider.getAgencyRouteTagTargetUUID(
        authority,
        route.originalIdHash.toString()
    )
