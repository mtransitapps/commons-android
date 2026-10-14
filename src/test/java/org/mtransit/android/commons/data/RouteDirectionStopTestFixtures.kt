package org.mtransit.android.commons.data

import org.mtransit.android.commons.provider.GTFSRealTimeProvider

fun makeDefaultPOI(
    authority: String = "authority",
    id: Int = 1,
    @DataSourceTypeId.DataSourceType dataSourceTypeId: Int = DataSourceTypeId.INVALID,
    @POI.ItemViewType type: Int = POI.ITEM_VIEW_TYPE_BASIC_POI,
    @POI.ItemStatusType statusType: Int = POI.ITEM_STATUS_TYPE_NONE,
    @POI.ItemActionType actionsType: Int = POI.ITEM_ACTION_TYPE_NONE,
    name: String = "name #$id",
    lat: Double = 1.0,
    lng: Double = 2.0,
) = DefaultPOI(
    authority,
    id,
    dataSourceTypeId,
    type,
    statusType,
    actionsType,
    lat,
    lng,
    name
)

fun makeBikeStation(
    authority: String = "authority",
    @DataSourceTypeId.DataSourceType dataSourceTypeId: Int = DataSourceTypeId.BIKE,
    id: Int = 1,
    name: String = "name #$id",
    lat: Double = 1.0,
    lng: Double = 2.0,
) = makeDefaultPOI(
    authority = authority,
    id = id,
    dataSourceTypeId = dataSourceTypeId,
    statusType = POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT,
    actionsType = POI.ITEM_ACTION_TYPE_FAVORITABLE,
    name = name,
    lat = lat,
    lng = lng,
)

fun makeRDS(
    authority: String = "authority",
    @DataSourceTypeId.DataSourceType dataSourceTypeId: Int = DataSourceTypeId.INVALID,
    routeId: Long = 1L,
    routeOriginalIdHash: Int? = routeId.toString().hashCode(),
    routeType: Int? = null, // custom route type != agency type
    originalDirectionId: Int? = 1,
    directionId: Long = originalDirectionId?.let { routeId * 100L + it } ?: (routeId * 100L + 9L),
    stopId: Int = 1,
    stopOriginalIdHash: Int? = stopId.toString().hashCode(), // stopId, // "$stopId".hashCode()
    stopName: String = "Stop #$stopId",
    stopLat: Double = 1.0,
    stopLng: Double = 2.0,
    stopTimeZoneId: String? = "UTC",
    isNoPickup: Boolean = false,
    alwaysLastTripStop: Boolean = false,
) = RouteDirectionStop(
    dataSourceTypeId,
    makeRoute(
        authority = authority,
        id = routeId,
        originalIdHash = routeOriginalIdHash,
        type = routeType,
    ),
    makeDirection(
        authority = authority,
        originalId = originalDirectionId,
        id = directionId,
        routeId = routeId,
    ),
    makeStop(
        stopId = stopId,
        stopName = stopName,
        stopLat = stopLat,
        stopLng = stopLng,
        stopOriginalIdHash = stopOriginalIdHash,
        stopTimeZoneId = stopTimeZoneId,
    ),
    isNoPickup,
    alwaysLastTripStop,
)

fun makeRoute(
    authority: String = "authority",
    id: Long = 1L,
    shortName: String = "#$id",
    longName: String = "route $id",
    color: String = "color",
    originalIdHash: Int? = id.toString().hashCode(),
    type: Int? = null, // custom route type != agency type
) = Route(
    authority,
    id,
    shortName,
    longName,
    color,
    originalIdHash,
    type,
)

fun makeDirection(
    authority: String = "authority",
    routeId: Long = 1L,
    originalId: Int? = 1,
    id: Long = originalId?.let { routeId * 100L + it } ?: (routeId * 100L + 9L),
    headsign: String = "Head-Sign $originalId",
    headsignType: Int = Direction.HEADSIGN_TYPE_STRING,
) = Direction(
    authority,
    id,
    headsignType,
    headsign,
    routeId,
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
