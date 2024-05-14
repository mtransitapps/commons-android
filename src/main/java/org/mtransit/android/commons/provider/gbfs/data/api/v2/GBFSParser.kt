package org.mtransit.android.commons.provider.gbfs.data.api.v2

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object GBFSParser {

    val gson: Gson by lazy {
        GsonBuilder()
            .create()
    }

}