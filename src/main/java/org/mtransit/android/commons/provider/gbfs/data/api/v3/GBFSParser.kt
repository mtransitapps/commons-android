package org.mtransit.android.commons.provider.gbfs.data.api.v3

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.Date

object GBFSParser {

    val gson: Gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Date::class.java, GBFSDateAdapter())
            .create()
    }

}