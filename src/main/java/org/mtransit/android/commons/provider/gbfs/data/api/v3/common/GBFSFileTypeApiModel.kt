package org.mtransit.android.commons.provider.gbfs.data.api.v3.common

import com.google.gson.annotations.SerializedName

enum class GBFSFileTypeApiModel {
    @SerializedName("system_information")
    SYSTEM_INFORMATION,
    @SerializedName("station_information")
    STATION_INFORMATION,
    // TODO...
}