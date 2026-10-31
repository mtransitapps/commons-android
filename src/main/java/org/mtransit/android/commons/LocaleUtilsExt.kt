package org.mtransit.android.commons

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewOutcomeReceiver
import androidx.webkit.WebViewStartUpConfig
import org.mtransit.android.commons.ProcessUtils.isMainProcess
import java.util.concurrent.Executors

/**
 * Preload webview with the right Locale as early as possible w/o blocking main thread
 */
@Suppress("TooGenericExceptionCaught")
@SuppressLint("SetJavaScriptEnabled")
fun fixWebViewLocale(context: Context) {
    try {
        if (!shouldPreloadWebViewForLocaleBug()) return
        if (!context.isMainProcess()) {
            // RuntimeException: Using WebView from more than one process at once with the same data directory is not supported. https://crbug.com/558377 :
            // Current process org.mtransit.android (pid 1111), lock owner org.mtransit.android (pid 2222) is a zombie
            return
        }
        val savedLocales = AppCompatDelegate.getApplicationLocales()
        WebViewCompat.startUpWebView(
            context,
            WebViewStartUpConfig.Builder(
                Executors.newSingleThreadExecutor()
            ).build(),
            WebViewOutcomeReceiver {
                if (!savedLocales.isEmpty) {
                    AppCompatDelegate.setApplicationLocales(savedLocales)
                }
            }
        )
    } catch (e: Exception) {
        MTLog.w(LocaleUtils.LOG_TAG, e, "Crash while fixing web view locale!")
    }
}

/**
 * Determines whether the WebView locale bug could affect the current configuration.
 */
private fun shouldPreloadWebViewForLocaleBug(): Boolean {
    // 1. If the user has never picked a custom in-app language, the bug won't trigger.
    val savedLocales = AppCompatDelegate.getApplicationLocales()
    if (savedLocales.isEmpty) {
        return false
    }
    // 2. Even if a custom language exists, if it matches the current OS default language,
    // the Chromium engine asset injection won't alter the app's active strings.
    val primaryAppLanguage = savedLocales.get(0)?.language
    val primarySystemLanguage = LocaleListCompat.getDefault().get(0)?.language

    return primaryAppLanguage != primarySystemLanguage
}
