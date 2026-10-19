package org.mtransit.android.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.os.Handler
import android.os.Looper
import android.webkit.WebView

/**
 * Preload webview with the right Locale as early as possible w/o blocking main thread
 */
@Suppress("TooGenericExceptionCaught")
@SuppressLint("SetJavaScriptEnabled")
fun fixWebViewLocale(context: Context) {
    try {
        val contextWrapper = MutableContextWrapper(context.applicationContext)
        Handler(Looper.getMainLooper()).post {
            val webView = WebView(contextWrapper)
            webView.post {
                try {
                    webView.destroy()
                } catch (e: Exception) {
                    MTLog.w(LocaleUtils.LOG_TAG, e, "Crash while destroying web view!")
                }
            }
        }
    } catch (e: Exception) {
        MTLog.w(LocaleUtils.LOG_TAG, e, "Crash while loading & destroying web view!")
    }
}
