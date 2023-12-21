package org.mtransit.android.commons

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URLConnection
import java.util.concurrent.TimeUnit


@Suppress("MemberVisibilityCanBePrivate")
object NetworkUtils {

    const val CONNECT_TIMEOUT_IN_MS = 10_000

    const val READ_TIMEOUT_IN_MS = 10_000

    const val HTTP_LOG_TAG = "HTTP"

    @JvmStatic
    @JvmOverloads
    fun setupUrlConnection(urlConnection: URLConnection, factor: Int = 1) {
        urlConnection.connectTimeout = CONNECT_TIMEOUT_IN_MS * factor
        urlConnection.readTimeout = READ_TIMEOUT_IN_MS * factor
    }

    @JvmStatic
    fun setupDefaultInterceptors(okhttpBuild: OkHttpClient.Builder, context: Context?) = okhttpBuild.apply {
        addInterceptor(getHttpLoggingInterceptor())
        getHttpLoggingInterceptor(context)?.let { addInterceptor(it) }

    }

    @JvmStatic
    fun getHttpLoggingInterceptor() =
        HttpLoggingInterceptor(logger = { MTLog.d(HTTP_LOG_TAG, it) }).apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
                // HttpLoggingInterceptor.Level.HEADERS
                // HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE // do NOT leak token in URLs
            }
        }

    @JvmStatic
    fun getHttpLoggingInterceptor(context: Context?) = context?.let {
        ChuckerInterceptor(it)
    }

    @JvmStatic
    fun makeNewOkHttpClientWithInterceptor(context: Context?): OkHttpClient {
        return OkHttpClient().newBuilder()
            .setupTimeouts()
            .setupDefaultInterceptors(context)
            .build()
    }

    @JvmStatic
    @JvmOverloads
    fun makeNewRetrofitWithGson(
        baseHostUrl: String,
        context: Context? = null,
        okHttpClient: OkHttpClient = makeNewOkHttpClientWithInterceptor(context),
        dateFormat: String? = null,
    ): Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(baseHostUrl)
        .addConverterFactory(
            GsonConverterFactory.create(GsonBuilder().apply {
                dateFormat?.let { this.setDateFormat(it) }
            }.create())
        )
        .build()

    @JvmStatic
    fun setupTimeouts(okhttpBuild: OkHttpClient.Builder) = okhttpBuild.apply {
        connectTimeout(CONNECT_TIMEOUT_IN_MS.toLong(), TimeUnit.MILLISECONDS)
        readTimeout(READ_TIMEOUT_IN_MS.toLong(), TimeUnit.MILLISECONDS)
    }
}

fun OkHttpClient.Builder.setupTimeouts() = NetworkUtils.setupTimeouts(this)

fun OkHttpClient.Builder.setupDefaultInterceptors(context: Context?): OkHttpClient.Builder = NetworkUtils.setupDefaultInterceptors(this, context)
