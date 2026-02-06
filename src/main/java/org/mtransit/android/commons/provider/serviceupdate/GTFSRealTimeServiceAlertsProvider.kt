package org.mtransit.android.commons.provider.serviceupdate

import com.google.transit.realtime.GtfsRealtime
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.Direction
import org.mtransit.android.commons.data.Route
import org.mtransit.android.commons.data.RouteDirection
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTypeTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyTagTargetUUID
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.originalIdToHash
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt

object GTFSRealTimeServiceAlertsProvider {

    @JvmStatic
    fun GTFSRealTimeProvider.parseTargetTripId(gEntitySelector: GtfsRealtime.EntitySelector): String? {
        return gEntitySelector.optTrip?.optTripId
    }

    @JvmStatic
    fun GTFSRealTimeProvider.parseProviderTargetUUID(agencyTag: String, gEntitySelector: GtfsRealtime.EntitySelector, ignoreDirection: Boolean): String? {
        if (gEntitySelector.hasRouteId()) {
            if (gEntitySelector.hasDirectionId() && !ignoreDirection) {
                if (gEntitySelector.hasStopId()) {
                    return getAgencyRouteDirectionStopTagTargetUUID(
                        agencyTag,
                        gEntitySelector.routeId.originalIdToHash(routeIdCleanupPattern),
                        gEntitySelector.directionId,
                        gEntitySelector.stopId.originalIdToHash(stopIdCleanupPattern)
                    )
                } else { // no stop
                    return getAgencyRouteDirectionTagTargetUUID(
                        agencyTag,
                        gEntitySelector.routeId.originalIdToHash(routeIdCleanupPattern),
                        gEntitySelector.directionId
                    )
                }
            } else { // no direction
                if (gEntitySelector.hasStopId()) {
                    return getAgencyRouteStopTagTargetUUID(
                        agencyTag,
                        gEntitySelector.routeId.originalIdToHash(routeIdCleanupPattern),
                        gEntitySelector.stopId.originalIdToHash(stopIdCleanupPattern)
                    )
                }
            }
            return getAgencyRouteTagTargetUUID(
                agencyTag,
                gEntitySelector.routeId.originalIdToHash(routeIdCleanupPattern)
            )
        } else if (gEntitySelector.hasStopId()) {
            return getAgencyStopTagTargetUUID(
                agencyTag,
                gEntitySelector.stopId.originalIdToHash(stopIdCleanupPattern)
            )
        } else if (gEntitySelector.hasRouteType()) {
            return getAgencyRouteTypeTagTargetUUID(
                agencyTag,
                gEntitySelector.routeType
            )
        } else if (gEntitySelector.hasAgencyId()) {
            return getAgencyTagTargetUUID(agencyTag)
        }
        MTLog.w(this, "parseTargetUUID() > unexpected entity selector: %s (IGNORED)", gEntitySelector.toStringExt())
        return null
    }

    private val GTFSRealTimeProvider.routeIdCleanupPattern get() = getRouteIdCleanupPattern(requireContextCompat())
    private val GTFSRealTimeProvider.tripIdCleanupPattern get() = getTripIdCleanupPattern(requireContextCompat())
    private val GTFSRealTimeProvider.stopIdCleanupPattern get() = getStopIdCleanupPattern(requireContextCompat())

    private val GTFSRealTimeProvider.agencyTag get() = getAgencyTag(requireContextCompat())

    private fun Route.getRouteTag(provider: GTFSRealTimeProvider) = provider.getRouteTag(this)
    private fun Direction.getDirectionTag(provider: GTFSRealTimeProvider) = provider.getDirectionTag(this)

    private fun RouteDirection.getRouteTag(provider: GTFSRealTimeProvider) = this.route.getRouteTag(provider)
    private fun RouteDirection.getDirectionTag(provider: GTFSRealTimeProvider) = this.direction.getDirectionTag(provider)

    private fun RouteDirectionStop.getRouteTag(provider: GTFSRealTimeProvider) = this.route.getRouteTag(provider)
    private fun RouteDirectionStop.getDirectionTag(provider: GTFSRealTimeProvider) = this.direction.getDirectionTag(provider)
}
