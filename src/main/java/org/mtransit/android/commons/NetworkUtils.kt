package org.mtransit.android.commons

import java.net.URLConnection


@Suppress("MemberVisibilityCanBePrivate")
object NetworkUtils {

    const val CONNECT_TIMEOUT_IN_SEC = 10_000

    const val READ_TIMEOUT_IN_SEC = 10_000

    @JvmStatic
    fun setupUrlConnection(urlConnection: URLConnection) {
        urlConnection.connectTimeout = CONNECT_TIMEOUT_IN_SEC
        urlConnection.readTimeout = READ_TIMEOUT_IN_SEC
    }
}