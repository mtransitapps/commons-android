package org.mtransit.android.commons.provider.gtfs.alert

import org.mtransit.android.commons.data.ServiceUpdate
import com.google.transit.realtime.GtfsRealtime.Alert.Effect as GAEffect
import com.google.transit.realtime.GtfsRealtime.EntitySelector as GEntitySelector

object GTFSRTAlertsManager {

    @JvmStatic
    fun parseSeverity(
        gEntitySelector: GEntitySelector,
        gEffect: GAEffect
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
    private fun parseEffectSeverity(gEffect: GAEffect, infoSeverity: Int, warningSeverity: Int): Int = when (gEffect) {
        GAEffect.ADDITIONAL_SERVICE -> infoSeverity
        GAEffect.MODIFIED_SERVICE -> infoSeverity
        GAEffect.REDUCED_SERVICE -> warningSeverity
        GAEffect.NO_SERVICE -> warningSeverity

        GAEffect.SIGNIFICANT_DELAYS -> warningSeverity

        GAEffect.DETOUR -> warningSeverity
        GAEffect.STOP_MOVED -> warningSeverity

        GAEffect.ACCESSIBILITY_ISSUE -> infoSeverity

        GAEffect.OTHER_EFFECT -> infoSeverity
        GAEffect.UNKNOWN_EFFECT -> infoSeverity
        GAEffect.NO_EFFECT -> infoSeverity
    }
}