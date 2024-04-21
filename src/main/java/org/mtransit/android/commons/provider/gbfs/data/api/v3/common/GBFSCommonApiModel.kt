package org.mtransit.android.commons.provider.gbfs.data.api.v3.common

import com.google.gson.annotations.SerializedName
import java.util.Date

// https://gbfs.org/specification/reference/#output-format
abstract class GBFSCommonApiModel<DataType> {

    companion object {
        const val LAST_UPDATED = "last_updated"
        const val TTL = "ttl"
        const val VERSION = "version"
        const val DATA = "data"
    }

    @get:SerializedName(LAST_UPDATED)
    abstract val lastUpdated: Date

    @get:SerializedName(TTL)
    abstract val ttlInSec: Int

    @get:SerializedName(VERSION)
    abstract val version: String

    @get:SerializedName(DATA)
    abstract val data: DataType
}
