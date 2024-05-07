package org.mtransit.android.commons.provider.gbfs.data.api.common

import com.google.gson.annotations.SerializedName
import java.util.Date

typealias GBFSIDApiType = String
typealias GBFSLanguageApiType = String
typealias GBFSCountryCodeApiType = String
typealias GBFSURLApiType = String
typealias GBFSURIApiType = String
typealias GBFSPhoneNumberApiType = String // E.164 format (e.g. +14155552671)
typealias GBFSEmailApiType = String
typealias GBFSTimezoneApiType = String
typealias GBFSDateApiType = String // ISO 8601

typealias GBFSTimestampApiType = Date // RFC3339 (as of v3.0)
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
