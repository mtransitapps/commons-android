package org.mtransit.android.commons.provider.serviceupdate

import com.google.transit.realtime.GtfsRealtime
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.data.RouteDirectionStop
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.android.commons.data.makeServiceUpdateNoneList
import org.mtransit.android.commons.provider.GTFSRealTimeProvider
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.AGENCY_SOURCE_ID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteDirectionTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyRouteTypeTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyStopTagTargetUUID
import org.mtransit.android.commons.provider.GTFSRealTimeProvider.getAgencyTagTargetUUID
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optAgencyId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDirectionId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optRouteId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optRouteType
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optStopId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTripId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.originalIdToHash
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.agencyTag
import org.mtransit.android.commons.provider.gtfs.getTargetUUIDs
import org.mtransit.android.commons.provider.gtfs.getTripsIds
import org.mtransit.android.commons.provider.gtfs.routeIdCleanupPattern
import org.mtransit.android.commons.provider.gtfs.stopIdCleanupPattern
import org.mtransit.android.commons.provider.gtfs.tripIdCleanupPattern
import org.mtransit.commons.FeatureFlags

object GTFSRealTimeServiceAlertsProvider {

    @JvmStatic
    fun GTFSRealTimeProvider.getCached(filter: ServiceUpdateProviderContract.Filter) =
        ((filter.poi as? RouteDirectionStop)?.getTargetUUIDs(this, includeAgencyTag = true, includeRouteType = true, includeStopTags = true)
            ?: filter.routeDirection?.getTargetUUIDs(this, includeAgencyTag = true, includeRouteType = true)
            ?: filter.route?.getTargetUUIDs(this, includeAgencyTag = true, includeRouteType = true))
            ?.let { targetUUIDs ->
                var tripIds: List<String>? = null
                if (FeatureFlags.F_PROVIDER_READS_TRIP_ID_DIRECTLY) {
                    tripIds = filter.targetAuthority?.let { targetAuthority ->
                        filter.routeId?.let { routeId ->
                            context?.getTripsIds(targetAuthority, routeId, filter.directionId)
                        }
                    }
                }
                targetUUIDs to tripIds // trip IDs not required for GTFS Alerts
            }?.let { (targetUUIDs, tripIds) ->
                getCached(targetUUIDs, tripIds)
            }

    fun GTFSRealTimeProvider.getCached(targetUUIDs: Map<String, String>, tripIds: List<String>?) = buildList {
        getCachedServiceUpdatesS(targetUUIDs.keys, tripIds)?.let {
            addAll(it)
        }
    }.map { it.apply { targetUUID = targetUUIDs[it.targetUUID] ?: it.targetUUID } }

    @JvmStatic
    fun GTFSRealTimeProvider.getNew(filter: ServiceUpdateProviderContract.Filter): List<ServiceUpdate>? {
        updateAgencyServiceUpdateDataIfRequired(requireContextCompat(), filter.isInFocusOrDefault)
        return getCached(filter)
            ?: filter.target?.let { enhanceServiceUpdate(makeServiceUpdateNoneList(it, AGENCY_SOURCE_ID)) }
    }

    @JvmStatic
    fun GTFSRealTimeProvider.parseTargetTripId(gEntitySelector: GtfsRealtime.EntitySelector) =
        gEntitySelector.optTrip?.optTripId?.originalIdToHash(tripIdCleanupPattern)

    @JvmStatic
    fun GTFSRealTimeProvider.parseProviderTargetUUID(gEntitySelector: GtfsRealtime.EntitySelector, ignoreDirection: Boolean): String? {
        gEntitySelector.optRouteId?.originalIdToHash(routeIdCleanupPattern)?.let { routeId ->
            gEntitySelector.optDirectionId?.takeIf { !ignoreDirection }?.let { directionId ->
                gEntitySelector.optStopId?.originalIdToHash(stopIdCleanupPattern)?.let { stopId ->
                    return getAgencyRouteDirectionStopTagTargetUUID(agencyTag, routeId, directionId, stopId)
                } // no stop
                return getAgencyRouteDirectionTagTargetUUID(agencyTag, routeId, directionId)
            } // no direction
            gEntitySelector.optStopId?.originalIdToHash(stopIdCleanupPattern)?.let { stopId ->
                return getAgencyRouteStopTagTargetUUID(agencyTag, routeId, stopId)
            }
            return getAgencyRouteTagTargetUUID(agencyTag, routeId)
        }
        gEntitySelector.optStopId?.originalIdToHash(stopIdCleanupPattern)?.let { stopId ->
            return getAgencyStopTagTargetUUID(agencyTag, stopId)
        }
        gEntitySelector.optRouteType?.let { routeType ->
            return getAgencyRouteTypeTagTargetUUID(agencyTag, routeType)
        }
        gEntitySelector.optAgencyId?.let { _ ->
            return getAgencyTagTargetUUID(agencyTag)
        }
        MTLog.w(this, "parseTargetUUID() > unexpected entity selector: %s (IGNORED)", gEntitySelector.toStringExt())
        return null
    }

    @JvmStatic
    fun GTFSRealTimeProvider.enhanceServiceUpdate(cachedServiceUpdates: List<ServiceUpdate>?) =
        cachedServiceUpdates?.map { serviceUpdate ->
            serviceUpdate.apply {
                setTextHTML(enhanceHtmlDateTime(requireContextCompat(), serviceUpdate.textHTML))
            }
        }
}
