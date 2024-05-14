package org.mtransit.android.commons.provider.gbfs.data.api.v3

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.v3.GBFSSystemPricingPlansApiModel.GBFSSystemPricingPlansDataApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSIDApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSLocalizedStringApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSURLApiType

// https://gbfs.org/specification/reference/#system_pricing_plansjson
data class GBFSSystemPricingPlansApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSSystemPricingPlansDataApiModel,
) : GBFSCommonApiModel<GBFSSystemPricingPlansDataApiModel>() {
    data class GBFSSystemPricingPlansDataApiModel(
        @SerializedName("plans")
        val plans: List<GBFSPlanApiModel>,
    ) {
        data class GBFSPlanApiModel(
            @SerializedName("plan_id")
            val planId: GBFSIDApiType,
            @SerializedName("url")
            val url: GBFSURLApiType,
            @SerializedName("name")
            val name: List<GBFSLocalizedStringApiModel>?,
            @SerializedName("currency")
            val currency: String,
            @SerializedName("price")
            val price: Float,
            @SerializedName("is_taxable")
            val isTaxable: Boolean,
            @SerializedName("description")
            val description: List<GBFSLocalizedStringApiModel>?,
            @SerializedName("per_km_pricing")
            val perKmPricing: List<GBFSPricingApiModel>?,
            @SerializedName("per_min_pricing")
            val perMinPricing: List<GBFSPricingApiModel>?,
            @SerializedName("surge_pricing")
            val surgePricing: Boolean?,
        ) {
            data class GBFSPricingApiModel(
                @SerializedName("start")
                val start: Int,
                @SerializedName("rate")
                val rate: Float,
                @SerializedName("interval")
                val interval: Int,
                @SerializedName("end")
                val end: Int,
            )
        }
    }
}