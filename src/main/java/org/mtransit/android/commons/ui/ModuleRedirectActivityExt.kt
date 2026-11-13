package org.mtransit.android.commons.ui

import android.os.Bundle
import androidx.core.view.isVisible
import org.mtransit.android.commons.R
import org.mtransit.android.commons.databinding.ActivityModuleRedirectBinding
import org.mtransit.android.commons.getInstalledProvidersWithMetaData
import org.mtransit.android.commons.isKeyMT

internal fun ModuleRedirectActivity.loadProvidersInfo() {
    val agencyProviderMetaData = getString(R.string.agency_provider)

    val statusProviderMetaData = getString(R.string.status_provider)
    val scheduleProviderMetaData = getString(R.string.schedule_provider)
    val serviceUpdateProviderMetaData = getString(R.string.service_update_provider)
    val vehicleLocationProviderMetaData = getString(R.string.vehicle_location_provider)
    val newsProviderMetaData = getString(R.string.news_provider)

    val agencyProviderTypeMetaData = getString(R.string.agency_provider_type)
    val rdsProviderMetaData = getString(R.string.rts_provider)

    agencyProviderAuthority = null
    agencyProviderType = null
    hasRDSProvider = null
    hasScheduleProvider = null
    hasNonStaticStatusProvider = null
    hasServiceUpdateProvider = null
    hasVehicleLocationProvider = null
    hasNewsProvider = null

    packageManager.getInstalledProvidersWithMetaData(packageName)?.forEach { providerInfo ->
        val providerMetaData: Bundle = providerInfo.metaData ?: return@forEach
        if (providerMetaData.isKeyMT(agencyProviderMetaData)) {
            agencyProviderAuthority = providerInfo.authority
            agencyProviderType = providerMetaData.getInt(agencyProviderTypeMetaData, -1)
        }
        if (providerMetaData.isKeyMT(rdsProviderMetaData)) {
            hasRDSProvider = true
        }
        if (providerMetaData.isKeyMT(scheduleProviderMetaData)) {
            hasScheduleProvider = true
        } else if (providerMetaData.isKeyMT(statusProviderMetaData)) { // IF non-schedule provider & status provider THEN real-time status provider
            hasNonStaticStatusProvider = true
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

internal fun ModuleRedirectActivity.updateAgencyInfo(binding: ActivityModuleRedirectBinding) = binding.apply {
    val appFeatureTexts = computeAppFeatureTexts()
    listOfNotNull(
        moduleFeature1Text,
        moduleFeature2Text,
        moduleFeature3Text,
        moduleFeature4Text,
        moduleFeature5Text,
        moduleFeature6Text,
    ).forEachIndexed { index, textView ->
        val text = appFeatureTexts.getOrNull(index)
        textView.text = text
        textView.isVisible = !text.isNullOrBlank()
    }
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
