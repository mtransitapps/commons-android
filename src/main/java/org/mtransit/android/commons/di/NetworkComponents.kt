package org.mtransit.android.commons.di

import android.content.Context
import okhttp3.OkHttpClient
import org.mtransit.android.commons.setupDefaultInterceptors
import org.mtransit.android.commons.setupTimeouts

object NetworkComponents {

    private var _okHttpClient: OkHttpClient? = null

    fun getOkHttpClient(context: Context): OkHttpClient {
        return _okHttpClient ?: makeOkHttpClient(context).also { _okHttpClient = it }
    }

    private fun makeOkHttpClient(context: Context) =
        OkHttpClient.Builder()
            .setupTimeouts()
            .setupDefaultInterceptors(context)
            .build()
}