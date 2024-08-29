package org.mtransit.android.commons.provider.gtfs.alert

import com.google.transit.realtime.GtfsRealtime.Alert.Effect
import com.google.transit.realtime.GtfsRealtime.EntitySelector
import org.mtransit.android.commons.data.ServiceUpdate

object GTFSRTAlertsManager {

    @JvmStatic
    fun parseSeverity(
        gEntitySelector: EntitySelector,
        gEffect: Effect
    ): Int {
        if (gEntitySelector.hasStopId()) {
            return parseEffectSeverity(gEffect, ServiceUpdate.SEVERITY_INFO_POI, ServiceUpdate.SEVERITY_WARNING_POI)
        } else if (gEntitySelector.hasRouteId() || gEntitySelector.hasRouteType()) {
            return parseEffectSeverity(gEffect, ServiceUpdate.SEVERITY_INFO_RELATED_POI, ServiceUpdate.SEVERITY_WARNING_RELATED_POI)
        } else if (gEntitySelector.hasAgencyId()) {
            return parseEffectSeverity(gEffect, ServiceUpdate.SEVERITY_INFO_AGENCY, ServiceUpdate.SEVERITY_WARNING_AGENCY)
        }
        return parseEffectSeverity(gEffect, ServiceUpdate.SEVERITY_INFO_UNKNOWN, ServiceUpdate.SEVERITY_WARNING_UNKNOWN)
    }

    // https://gtfs.org/documentation/realtime/feed_entities/service-alerts/#effect
    private fun parseEffectSeverity(gEffect: Effect, infoSeverity: Int, warningSeverity: Int): Int = when (gEffect) {
        Effect.ADDITIONAL_SERVICE -> infoSeverity
        Effect.MODIFIED_SERVICE -> infoSeverity
        Effect.REDUCED_SERVICE -> warningSeverity
        Effect.NO_SERVICE -> warningSeverity

        Effect.SIGNIFICANT_DELAYS -> warningSeverity

        Effect.DETOUR -> warningSeverity
        Effect.STOP_MOVED -> warningSeverity

        Effect.ACCESSIBILITY_ISSUE -> infoSeverity

        Effect.OTHER_EFFECT -> infoSeverity
        Effect.UNKNOWN_EFFECT -> infoSeverity
        Effect.NO_EFFECT -> infoSeverity
    }
}