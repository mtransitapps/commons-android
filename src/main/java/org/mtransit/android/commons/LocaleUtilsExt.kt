package org.mtransit.android.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import org.mtransit.android.commons.ProcessUtils.isMainProcess

/**
 * Preload webview with the right Locale as early as possible w/o blocking main thread
 */
@Suppress("TooGenericExceptionCaught")
@SuppressLint("SetJavaScriptEnabled")
fun fixWebViewLocale(context: Context) {
    try {
        if (!context.isMainProcess()) {
            // RuntimeException: Using WebView from more than one process at once with the same data directory is not supported. https://crbug.com/558377 :
            // Current process org.mtransit.android (pid 1111), lock owner org.mtransit.android (pid 2222) is a zombie
            return
        }
        val contextWrapper = MutableContextWrapper(context.applicationContext)
        Handler(Looper.getMainLooper()).post {
            try {
                val webView = WebView(contextWrapper)
                webView.post {
                    try {
                        webView.destroy()
                    } catch (e: Exception) {
                        MTLog.w(LocaleUtils.LOG_TAG, e, "Crash while destroying web view!")
                    }
                }
            } catch (e: Exception) {
                MTLog.w(LocaleUtils.LOG_TAG, e, "Crash while loading & destroying web view!")
            }
        }
    } catch (e: Exception) {
        MTLog.w(LocaleUtils.LOG_TAG, e, "Crash while fixing web view locale!")
    }
}
