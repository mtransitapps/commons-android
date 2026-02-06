package org.mtransit.android.commons.provider.serviceupdate

import com.google.transit.realtime.GtfsRealtime
import org.mtransit.android.commons.MTLog
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
    fun parseTargetTripId(gEntitySelector: GtfsRealtime.EntitySelector) =
        gEntitySelector.optTrip?.optTripId

    @JvmStatic
    fun GTFSRealTimeProvider.parseProviderTargetUUID(gEntitySelector: GtfsRealtime.EntitySelector, ignoreDirection: Boolean): String? {
        if (gEntitySelector.hasRouteId()) {
            if (gEntitySelector.hasDirectionId() && !ignoreDirection) {
                return if (gEntitySelector.hasStopId()) {
                    getAgencyRouteDirectionStopTagTargetUUID(
                        agencyTag,
                        gEntitySelector.routeId.originalIdToHash(routeIdCleanupPattern),
                        gEntitySelector.directionId,
                        gEntitySelector.stopId.originalIdToHash(stopIdCleanupPattern)
                    )
                } else { // no stop
                    getAgencyRouteDirectionTagTargetUUID(
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

    private val GTFSRealTimeProvider.stopIdCleanupPattern get() = getStopIdCleanupPattern(requireContextCompat())

    private val GTFSRealTimeProvider.agencyTag get() = getAgencyTag(requireContextCompat())

}
