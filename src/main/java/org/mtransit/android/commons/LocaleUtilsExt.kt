package org.mtransit.android.commons

import android.annotation.SuppressLint
import android.content.Context
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewOutcomeReceiver
import androidx.webkit.WebViewStartUpConfig
import androidx.webkit.WebViewStartUpResult
import androidx.webkit.WebViewStartupException
import org.mtransit.android.commons.ProcessUtils.isMainProcess
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Preload webview with the right Locale as early as possible (before Ads SDK Webview) w/o blocking main thread
 *
 * The main issue appears when the user has this OS language setting:
 * - an unsupported 1st language (like Spanish)
 * - a non-default app language (like French)
 * - then (maybe) the app default language (English)
 * The app UI/Locale should be in French (not English) in this case.
 */
@Suppress("TooGenericExceptionCaught")
@SuppressLint("SetJavaScriptEnabled")
fun fixWebViewLocale(context: Context) {
    if (!context.isMainProcess()) {
        // RuntimeException: Using WebView from more than one process at once with the same data directory is not supported. https://crbug.com/558377 :
        // Current process org.mtransit.android (pid 1111), lock owner org.mtransit.android (pid 2222) is a zombie
        return
    }
    val backgroundExecutor = Executors.newSingleThreadExecutor()
    try {
        WebViewCompat.startUpWebView(
            context,
            WebViewStartUpConfig.Builder(
                backgroundExecutor,
            ).build(),
            object : WebViewOutcomeReceiver<WebViewStartUpResult, WebViewStartupException> {
                override fun onResult(result: WebViewStartUpResult) {
                    try {
                        restoreLocale()
                    } finally {
                        backgroundExecutor.shutdown()
                    }
                }

                override fun onError(error: WebViewStartupException) {
                    try {
                        restoreLocale()
                    } finally {
                        backgroundExecutor.shutdown()
                    }
                }

                private fun restoreLocale() {
                    Locale.setDefault(LocaleUtils.getDefaultLocale())
                }
            }
        )
    } catch (e: Exception) {
        MTLog.w(LocaleUtils.LOG_TAG, e, "Crash while fixing web view locale!")
        backgroundExecutor.shutdown()
    }
}
