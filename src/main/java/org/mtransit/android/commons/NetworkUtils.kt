package org.mtransit.android.commons

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

    @JvmStatic
    fun setupUrlConnection(urlConnection: URLConnection) {
        urlConnection.connectTimeout = CONNECT_TIMEOUT_IN_MS
        urlConnection.readTimeout = READ_TIMEOUT_IN_MS
    }

    @JvmStatic
    fun setupDefaultInterceptors(okhttpBuild: OkHttpClient.Builder): OkHttpClient.Builder {
        okhttpBuild.addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE // do NOT leak token in URLs
                }
            }
        )
        return okhttpBuild
    }

    @JvmStatic
    fun makeNewOkHttpClientWithInterceptor(): OkHttpClient {
        return OkHttpClient().newBuilder()
            .connectTimeout(CONNECT_TIMEOUT_IN_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_IN_MS.toLong(), TimeUnit.MILLISECONDS)
            .setupDefaultInterceptors()
            .build()
    }

    @JvmStatic
    @JvmOverloads
    fun makeNewRetrofitWithGson(
        baseHostUrl: String,
        okHttpClient: OkHttpClient = makeNewOkHttpClientWithInterceptor(),
        dateFormat: String? = null,
    ): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(baseHostUrl)
            .addConverterFactory(
                GsonConverterFactory.create(GsonBuilder().apply {
                    dateFormat?.let { this.setDateFormat(it) }
                }.create())
            )
            .build()
    }
}

fun OkHttpClient.Builder.setupDefaultInterceptors(): OkHttpClient.Builder = NetworkUtils.setupDefaultInterceptors(this)