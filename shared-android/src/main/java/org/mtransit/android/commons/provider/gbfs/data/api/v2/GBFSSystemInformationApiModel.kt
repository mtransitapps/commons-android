package org.mtransit.android.commons.provider.gbfs.data.api.v2

import com.google.gson.annotations.SerializedName
import org.mtransit.android.commons.provider.gbfs.data.api.v2.GBFSSystemInformationApiModel.GBFSSystemInformationDataApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSCommonApiModel
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSDateApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSEmailApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSIDApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSLanguageApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSPhoneNumberApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSTimestampApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSTimezoneApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSURIApiType
import org.mtransit.android.commons.provider.gbfs.data.api.v2.common.GBFSURLApiType

// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.0/system_informationj.json
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.1/system_informationj.json
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.2/system_informationj.json
// https://github.com/MobilityData/gbfs-json-schema/blob/master/v2.3/system_informationj.json
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
        @SerializedName("language")
        val language: GBFSLanguageApiType,
        @SerializedName("name")
        val name: String,
        @SerializedName("short_name")
        val shortName: String?,
        @SerializedName("operator")
        val operator: String?,
        @SerializedName("url")
        val url: GBFSURLApiType?,
        @SerializedName("purchase_url")
        val purchaseUrl: GBFSURLApiType?,
        @SerializedName("start_date")
        val startDate: GBFSDateApiType?,
        @SerializedName("phone_number")
        val phoneNumber: GBFSPhoneNumberApiType?,
        @SerializedName("email")
        val email: GBFSEmailApiType?,
        @SerializedName("feed_contact_email")
        val feedContactEmail: GBFSEmailApiType?,
        @SerializedName("timezone")
        val timezone: GBFSTimezoneApiType?,
        @SerializedName("license_url")
        val licenseUrl: GBFSURLApiType?,
        @SerializedName("brand_assets") // (added in v2.3)
        val brandAssets: GBFSSystemBrandAssetsApiModel?,
        @SerializedName("terms_url") // (added in v2.3)
        val termsUrl: GBFSURLApiType?,
        @SerializedName("terms_last_updated") // (added in v2.3)
        val termsLastUpdated: GBFSDateApiType?,
        @SerializedName("privacy_url") // (added in v2.3)
        val privacyUrl: GBFSURLApiType?,
        @SerializedName("privacy_last_updated") // (added in v2.3)
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
