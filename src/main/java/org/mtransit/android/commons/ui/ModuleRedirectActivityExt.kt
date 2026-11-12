package org.mtransit.android.commons.ui

import android.os.Bundle
import androidx.core.view.isVisible
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.R
import org.mtransit.android.commons.getInstalledProvidersWithMetaData
import org.mtransit.android.commons.isKeyMT

internal fun ModuleRedirectActivity.loadProvidersInfo() {
    MTLog.d(this, "updateAgencyInfo()")
    val agencyProviderMetaData by lazy { getString(R.string.agency_provider) }

    val statusProviderMetaData by lazy { getString(R.string.status_provider) }
    val scheduleProviderMetaData by lazy { getString(R.string.schedule_provider) }
    val serviceUpdateProviderMetaData by lazy { getString(R.string.service_update_provider) }
    val vehicleLocationProviderMetaData by lazy { getString(R.string.vehicle_location_provider) }
    val newsProviderMetaData by lazy { getString(R.string.news_provider) }

    val agencyProviderTypeMetaData by lazy { getString(R.string.agency_provider_type) }
    val rdsProviderMetaData by lazy { getString(R.string.rts_provider) }

    agencyProviderAuthority = null
    agencyProviderType = null
    hasRDSProvider = null
    hasScheduleProvider = null
    hasNonStaticStatusProvider = null
    hasServiceUpdateProvider = null
    hasVehicleLocationProvider = null
    hasNewsProvider = null

    packageManager.getInstalledProvidersWithMetaData(packageName)?.forEach { providerInfo ->
        MTLog.d(this, "updateAgencyInfo() > providerInfo: $providerInfo")
        val providerMetaData: Bundle = providerInfo.metaData ?: return@forEach
        MTLog.d(this, "updateAgencyInfo() > providerMetaData: $providerMetaData")
        if (providerMetaData.isKeyMT(agencyProviderMetaData)) {
            agencyProviderAuthority = providerInfo.authority
            agencyProviderType = providerMetaData.getInt(agencyProviderTypeMetaData, -1)
            hasRDSProvider = providerMetaData.isKeyMT(rdsProviderMetaData)
        }
        if (providerMetaData.isKeyMT(rdsProviderMetaData)) {
            hasRDSProvider = true
        }
        if (providerMetaData.isKeyMT(scheduleProviderMetaData)) {
            hasScheduleProvider = true
        } else if (providerMetaData.isKeyMT(statusProviderMetaData)) {
            hasNonStaticStatusProvider = true // non-full static schedule based providers -> real-time next departures providers
        }
        if (providerMetaData.isKeyMT(serviceUpdateProviderMetaData)) {
            hasServiceUpdateProvider = true
        }
        if (providerMetaData.isKeyMT(vehicleLocationProviderMetaData)) {
            hasVehicleLocationProvider = true
        }
        if (providerMetaData.isKeyMT(newsProviderMetaData)) {
            hasNewsProvider = true
        }
    }
}

internal fun ModuleRedirectActivity.updateAgencyInfo() {
    MTLog.d(this, "updateAgencyInfo()")
    val appFeatureTexts = computeAppFeatureTexts()
    MTLog.d(this, "updateAgencyInfo() > appFeatureTexts: [${appFeatureTexts.size}]: $appFeatureTexts")
    appFeatureTexts.getOrNull(0).let { this.appFeature1Tv?.apply { text = it; isVisible = !it.isNullOrBlank() } }
    appFeatureTexts.getOrNull(1).let { this.appFeature2Tv?.apply { text = it; isVisible = !it.isNullOrBlank() } }
    appFeatureTexts.getOrNull(2).let { this.appFeature3Tv?.apply { text = it; isVisible = !it.isNullOrBlank() } }
    appFeatureTexts.getOrNull(3).let { this.appFeature4Tv?.apply { text = it; isVisible = !it.isNullOrBlank() } }
    appFeatureTexts.getOrNull(4).let { this.appFeature5Tv?.apply { text = it; isVisible = !it.isNullOrBlank() } }
    appFeatureTexts.getOrNull(5).let { this.appFeature6Tv?.apply { text = it; isVisible = !it.isNullOrBlank() } }
}

internal fun ModuleRedirectActivity.computeAppFeatureTexts() = buildList<CharSequence> {
    if (hasRDSProvider == true) {
        add(getString(R.string.app_feature_rds_all_info))
    } else {
        add(getString(R.string.app_feature_other_all_info))
    }
    if (hasScheduleProvider == true) {
        add(getString(R.string.app_feature_planned_schedule_offline))
    }
    if (hasNonStaticStatusProvider == true) {
        if (hasRDSProvider == true) {
            add(getString(R.string.app_feature_real_time_next_departures))
        } else {
            add(getString(R.string.app_feature_real_time_bike_availability))
        }
    }
    if (hasVehicleLocationProvider == true) {
        add(getString(R.string.app_feature_vehicle_locations))
    }
    if (hasServiceUpdateProvider == true) {
        add(getString(R.string.app_feature_service_alerts))
    }
    if (hasNewsProvider == true) {
        add(getString(R.string.app_feature_latest_news))
    }
}
