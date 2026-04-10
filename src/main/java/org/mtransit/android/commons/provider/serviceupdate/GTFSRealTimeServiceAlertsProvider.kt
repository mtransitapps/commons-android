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
import org.mtransit.android.commons.provider.gtfs.GtfsRealTimeStorage
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optAgencyId
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optDirectionIdValid
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optRouteType
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.optTrip
import org.mtransit.android.commons.provider.gtfs.GtfsRealtimeExt.toStringExt
import org.mtransit.android.commons.provider.gtfs.agencyTag
import org.mtransit.android.commons.provider.gtfs.getTargetUUIDs
import org.mtransit.android.commons.provider.gtfs.getTripIds
import org.mtransit.android.commons.provider.gtfs.getTrips
import org.mtransit.android.commons.provider.gtfs.ignoreDirection
import org.mtransit.android.commons.provider.gtfs.parseRouteId
import org.mtransit.android.commons.provider.gtfs.parseStopId
import org.mtransit.android.commons.provider.gtfs.parseTripId
import org.mtransit.android.commons.provider.gtfs.targetAuthority
import com.google.transit.realtime.GtfsRealtime.EntitySelector as GEntitySelector

object GTFSRealTimeServiceAlertsProvider : MTLog.Loggable {

    internal val LOG_TAG: String = GTFSRealTimeServiceAlertsProvider::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    private var _tripIdsOutOfSync: Boolean? = null

    private fun GTFSRealTimeProvider.getTripIdOutOfSync() = _tripIdsOutOfSync
        ?: context?.let { GtfsRealTimeStorage.getServiceUpdateTripIdsOutOfSync(it, false) }.also {
            _tripIdsOutOfSync = it
        }

    @JvmStatic
    fun GTFSRealTimeProvider.getCached(filter: ServiceUpdateProviderContract.Filter) =
        getCached(
            filter = filter,
            tripIdsOutOfSync = getTripIdOutOfSync(),
            getTripIds = { authority, routeId, directionId ->
                context?.getTripIds(authority, routeId, directionId)
            },
            getCachedServiceUpdates = { targetUUIDs, tripIds ->
                getCachedServiceUpdatesS(targetUUIDs, tripIds)
            },
        )

    fun GTFSRealTimeProvider.getCached(
        filter: ServiceUpdateProviderContract.Filter,
        tripIdsOutOfSync: Boolean?,
        getTripIds: (authority: String, routeId: Long, directionId: Long?) -> List<String>?,
        getCachedServiceUpdates: (targetUUIDs: Collection<String>, tripIds: List<String>?) -> List<ServiceUpdate>?,
    ): List<ServiceUpdate>? {
        val tripIdsOutOfSync = tripIdsOutOfSync == true
        return filter.getTargetUUIDs(this, includeAgencyTag = !tripIdsOutOfSync, includeRouteType = true, includeStopTags = true)
            ?.let { targetUUIDs ->
                val tripIds = if (tripIdsOutOfSync) null
                else filter.targetAuthority?.let { targetAuthority ->
                    filter.routeId?.let { routeId ->
                        getTripIds(targetAuthority, routeId, filter.directionId)
                    }
                }
                targetUUIDs to tripIds?.takeIf { it.isNotEmpty() } // trip IDs not required for GTFS Alerts
            }?.let { (targetUUIDs, tripIds) ->
                getCached(targetUUIDs, tripIds, getCachedServiceUpdates)
            }
    }

    private fun getCached(
        targetUUIDs: Map<String, String>,
        tripIds: List<String>?,
        getCachedServiceUpdates: (targetUUIDs: Collection<String>, tripIds: List<String>?) -> List<ServiceUpdate>?,
    ) = buildList {
        (
                getCachedServiceUpdates(targetUUIDs.keys, tripIds)?.takeIf { it.isNotEmpty() }
                )
            ?.let {
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

    @JvmOverloads
    @JvmStatic
    fun GTFSRealTimeProvider.parseProviderTargetUUID(
        gEntitySelector: GEntitySelector,
        ignoreDirection: Boolean = this.ignoreDirection,
    ): String? {
        parseRouteId(gEntitySelector)?.let { routeId ->
            gEntitySelector.optDirectionIdValid?.takeIf { !ignoreDirection }?.let { directionId ->
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
        MTLog.w(LOG_TAG, "parseTargetUUID() > unexpected entity selector: %s (IGNORED)", gEntitySelector.toStringExt())
        return null
    }

    @JvmStatic
    fun GTFSRealTimeProvider.enhanceServiceUpdate(cachedServiceUpdates: List<ServiceUpdate>?) =
        cachedServiceUpdates?.map { serviceUpdate ->
            serviceUpdate.apply {
                setTextHTML(enhanceHtmlDateTime(requireContextCompat(), serviceUpdate.textHTML))
            }
        }

    @JvmStatic
    fun GTFSRealTimeProvider.setServiceUpdatesTripIdsOutOfSync(serviceUpdates: List<ServiceUpdate>) {
        val context = context ?: return
        val rtTripId = serviceUpdates.firstOrNull { it.targetTripId != null }?.targetTripId
        val tripIdsOutOfSync = rtTripId?.let {
            context.getTrips(targetAuthority, tripIds = listOf(it))?.size == 0 // no trip ID matches == out-of-sync
        } ?: false // no real-time trip ID == not out-of-sync
        GtfsRealTimeStorage.saveServiceUpdateTripIdsOutOfSync(context, tripIdsOutOfSync)
        _tripIdsOutOfSync = tripIdsOutOfSync
    }
}
