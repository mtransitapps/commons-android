package org.mtransit.android.commons.provider.serviceupdate

import org.mtransit.android.commons.MTLog
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
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optRouteType
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.agencyTag
import org.mtransit.android.commons.provider.gtfs.getTargetUUIDs
import org.mtransit.android.commons.provider.gtfs.getTripIds
import org.mtransit.android.commons.provider.gtfs.parseRouteId
import org.mtransit.android.commons.provider.gtfs.parseStopId
import org.mtransit.android.commons.provider.gtfs.parseTripId
import com.google.transit.realtime.GtfsRealtime.EntitySelector as GEntitySelector

object GTFSRealTimeServiceAlertsProvider {

    @JvmStatic
    fun GTFSRealTimeProvider.getCached(filter: ServiceUpdateProviderContract.Filter) =
        filter.getTargetUUIDs(this, includeAgencyTag = true, includeRouteType = true, includeStopTags = true)
            ?.let { targetUUIDs ->
                val tripIds = filter.targetAuthority?.let { targetAuthority ->
                    filter.routeId?.let { routeId ->
                        context?.getTripIds(targetAuthority, routeId, filter.directionId)
                    }
                }
                targetUUIDs to tripIds?.takeIf { it.isNotEmpty() } // trip IDs not required for GTFS Alerts
            }?.let { (targetUUIDs, tripIds) ->
                getCached(targetUUIDs, tripIds)
            }

    fun GTFSRealTimeProvider.getCached(targetUUIDs: Map<String, String>, tripIds: List<String>?) = buildList {
        tripIds?.let {
            // trip IDs preferred for all result filtered correctly
            getCachedServiceUpdatesS(targetUUIDs.keys, tripIds)?.takeIf { it.isNotEmpty() }
        } ?: run {
            // fall back to showing all w/o filtering trip IDs
            getCachedServiceUpdatesS(targetUUIDs.keys)
        }?.let {
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
    fun GTFSRealTimeProvider.parseTargetTripId(gEntitySelector: GEntitySelector) =
        gEntitySelector.optTrip?.let { parseTripId(it) }

    @JvmStatic
    fun GTFSRealTimeProvider.parseProviderTargetUUID(gEntitySelector: GEntitySelector, ignoreDirection: Boolean): String? {
        parseRouteId(gEntitySelector)?.let { routeId ->
            gEntitySelector.optDirectionId?.takeIf { !ignoreDirection }?.let { directionId ->
                parseStopId(gEntitySelector)?.let { stopId ->
                    return getAgencyRouteDirectionStopTagTargetUUID(agencyTag, routeId, directionId, stopId)
                } // no stop
                return getAgencyRouteDirectionTagTargetUUID(agencyTag, routeId, directionId)
            } // no direction
            parseStopId(gEntitySelector)?.let { stopId ->
                return getAgencyRouteStopTagTargetUUID(agencyTag, routeId, stopId)
            }
            return getAgencyRouteTagTargetUUID(agencyTag, routeId)
        }
        parseStopId(gEntitySelector)?.let { stopId ->
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
