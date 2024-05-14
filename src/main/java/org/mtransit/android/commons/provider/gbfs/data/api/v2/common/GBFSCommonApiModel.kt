package org.mtransit.android.commons.provider.gbfs.data.api.v2.common

import com.google.gson.annotations.SerializedName
import java.util.Date

// https://github.com/MobilityData/gbfs/blob/v2.0/gbfs.md#field-types
// https://github.com/MobilityData/gbfs/blob/v2.1/gbfs.md#field-types
// https://github.com/MobilityData/gbfs/blob/v2.2/gbfs.md#field-types
// https://github.com/MobilityData/gbfs/blob/v2.3/gbfs.md#field-types
typealias GBFSIDApiType = String
typealias GBFSLanguageApiType = String
typealias GBFSCountryCodeApiType = String
typealias GBFSURLApiType = String
typealias GBFSURIApiType = String
typealias GBFSPhoneNumberApiType = String // E.164 format (e.g. +14155552671)
typealias GBFSEmailApiType = String
typealias GBFSTimezoneApiType = String
typealias GBFSDateApiType = String // ISO 8601 YYYY-MM-DD

typealias GBFSTimestampApiType = Long // POSIX time in SECONDS
typealias GBFSDateTimeApiType = Date // (added in v2.3) ISO 8601

typealias GBFSLatitudeApiType = Double
typealias GBFSLongitudeApiType = Double

// https://gbfs.org/specification/reference/#output-format
abstract class GBFSCommonApiModel<DataType> {

    companion object {
        const val LAST_UPDATED = "last_updated"
        const val TTL = "ttl"
        const val VERSION = "version"
        const val DATA = "data"
    }

    @get:SerializedName(LAST_UPDATED)
    abstract val lastUpdated: GBFSTimestampApiType

    @get:SerializedName(TTL)
    abstract val ttlInSec: Int

    @get:SerializedName(VERSION)
    abstract val version: String

    @get:SerializedName(DATA)
    abstract val data: DataType
}