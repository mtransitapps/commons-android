package org.mtransit.android.commons.provider.gbfs.data.api.v3

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.v3.GBFSSystemInformationApiModel.GBFSSystemInformationDataApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSDateApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSEmailApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSIDApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSLanguageApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSLocalizedStringApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSPhoneNumberApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSTimezoneApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSURIApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v3.common.GBFSURLApiType

// https://gbfs.org/specification/reference/#system_informationjson
data class GBFSSystemInformationApiModel(
    @SerializedName(LAST_UPDATED)
    override val lastUpdated: GBFSTimestampApiType,
    @SerializedName(TTL)
    override val ttlInSec: Int,
    @SerializedName(VERSION)
    override val version: String,
    @SerializedName(DATA)
    override val data: GBFSSystemInformationDataApiModel,
) : GBFSCommonApiModel<GBFSSystemInformationDataApiModel>() {

    data class GBFSSystemInformationDataApiModel(
        @SerializedName("system_id")
        val systemId: GBFSIDApiType,
        @SerializedName("languages") // (added of v3.0)
        val languages: List<GBFSLanguageApiType>,
        @SerializedName("name") // (added of v3.0)
        val name: List<GBFSLocalizedStringApiModel>,
        @SerializedName("opening_hours") // (added of v3.0)
        val openingHours: String,
        @SerializedName("short_name") // (added of v3.0)
        val shortName: List<GBFSLocalizedStringApiModel>?,
        @SerializedName("operator") // (added of v3.0)
        val operator: List<GBFSLocalizedStringApiModel>?,
        @SerializedName("url")
        val url: GBFSURLApiType?,
        @SerializedName("purchase_url")
        val purchaseUrl: GBFSURLApiType?,
        @SerializedName("start_date")
        val startDate: GBFSDateApiType?,
        @SerializedName("termination_date") // (added in v3.0)
        val terminationDate: GBFSDateApiType?,
        @SerializedName("phone_number") // (added in v3.0)
        val phoneNumber: GBFSPhoneNumberApiType?,
        @SerializedName("email")
        val email: GBFSEmailApiType?,
        @SerializedName("feed_contact_email")
        val feedContactEmail: GBFSEmailApiType?,
        @SerializedName("manifest_url")
        val manifestUrl: GBFSURLApiType?,
        @SerializedName("timezone")
        val timezone: GBFSTimezoneApiType?,
        @SerializedName("license_id")
        val licenseId: String?,
        @SerializedName("license_url")
        val licenseUrl: GBFSURLApiType?,
        @SerializedName("attribution_organization_name")
        val attributionOrganizationName: List<GBFSLocalizedStringApiModel>,
        @SerializedName("attribution_url")
        val attributionUrl: GBFSURLApiType?,
        @SerializedName("brand_assets") // A JSON element consisting of key-value pairs (fields).
        val brandAssets: GBFSSystemBrandAssetsApiModel?,
        @SerializedName("terms_url")
        val termsUrl: List<GBFSLocalizedStringApiModel>?,
        @SerializedName("terms_last_updated")
        val termsLastUpdated: GBFSDateApiType?,
        @SerializedName("privacy_url")
        val privacyUrl: List<GBFSLocalizedStringApiModel>?,
        @SerializedName("privacy_last_updated")
        val privacyLastUpdated: GBFSDateApiType?,
        @SerializedName("rental_apps")
        val rentalApps: GBFSRentalAppsApiModel?,
    ) {
        data class GBFSSystemBrandAssetsApiModel(
            @SerializedName("brand_last_modified")
            val brandLastModified: GBFSDateApiType?,
            @SerializedName("brand_terms_url")
            val brandTermsUrl: GBFSURLApiType?,
            @SerializedName("brand_image_url")
            val brandImageUrl: GBFSURLApiType?,
            @SerializedName("brand_image_url_dark")
            val brandImageUrlDark: GBFSURLApiType?,
            @SerializedName("color") // (added in v2.3)
            val color: String?,
        )

        data class GBFSRentalAppsApiModel(
            @SerializedName("android")
            val android: GBFSRentalAppApiModel?,
            @SerializedName("ios")
            val ios: GBFSRentalAppApiModel?,
        ) {
            data class GBFSRentalAppApiModel(
                @SerializedName("store_uri")
                val storeUri: GBFSURIApiType?,
                @SerializedName("discovery_uri")
                val discoveryUri: GBFSURIApiType?,
            )
        }
    }
}
