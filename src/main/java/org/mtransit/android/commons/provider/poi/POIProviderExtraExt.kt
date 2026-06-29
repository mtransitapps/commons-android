package org.mtransit.android.commons.provider.poi

val POIProviderContract.Filter.avoidLoadingOrDefault: Boolean
    get() = this.getExtraBoolean(POIProviderContract.POI_FILTER_EXTRA_AVOID_LOADING, false)
