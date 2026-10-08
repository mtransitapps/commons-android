package org.mtransit.android.commons.data

import org.mtransit.android.commons.provider.GTFSRealTimeProvider

fun makeRDS(
    authority: String = "authority",
    routeId: Long = 1L,
    routeOriginalIdHash: Int? = routeId.toString().hashCode(),
    @DataSourceTypeId.DataSourceType type: Int = 3,
    routeType: Int? = null, // custom route type != agency type
    originalDirectionId: Int? = 1,
    directionId: Long = originalDirectionId?.let { routeId * 100L + it } ?: (routeId * 100L + 9L),
    stopId: Int = 1,
    stopOriginalIdHash: Int? = stopId.toString().hashCode(), // stopId, // "$stopId".hashCode()
    stopLat: Double = 1.0,
    stopLng: Double = 2.0,
    stopTimeZoneId: String? = "UTC",
    isNoPickup: Boolean = false,
    alwaysLastTripStop: Boolean = false,
) = RouteDirectionStop(
    type,
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
    makeStop(
        stopId = stopId,
        stopLat = stopLat,
        stopLng = stopLng,
        stopOriginalIdHash = stopOriginalIdHash,
        stopTimeZoneId = stopTimeZoneId,
    ),
    isNoPickup,
    alwaysLastTripStop,
)

fun makeStop(
    stopId: Int = 1,
    stopCode: String = "#$stopId",
    stopName: String = "Stop #$stopId",
    stopLat: Double = 1.0,
    stopLng: Double = 2.0,
    stopAccessibility: Int = Accessibility.DEFAULT,
    stopOriginalIdHash: Int? = stopId.toString().hashCode(), // stopId, // "$stopId".hashCode()
    stopTimeZoneId: String? = "UTC",
) = Stop(
    stopId,
    stopCode,
    stopName,
    stopLat,
    stopLng,
    stopAccessibility,
    stopOriginalIdHash,
    stopTimeZoneId,
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
    if (!includeStopTags) {
        this.toRouteDirection().getGTFSRTTargetUUID()
    } else {
        GTFSRealTimeProvider.getAgencyRouteDirectionStopTagTargetUUID(
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
    }
