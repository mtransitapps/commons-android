package org.mtransit.android.commons

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewOutcomeReceiver
import androidx.webkit.WebViewStartUpConfig
import androidx.webkit.WebViewStartUpResult
import androidx.webkit.WebViewStartupException
import org.mtransit.android.commons.ProcessUtils.isMainProcess
import java.util.concurrent.Executors

/**
 * Preload webview with the right Locale as early as possible w/o blocking main thread
 */
@Suppress("TooGenericExceptionCaught")
@SuppressLint("SetJavaScriptEnabled")
fun fixWebViewLocale(context: Context) {
    if (!shouldPreloadWebViewForLocaleBug()) return
    if (!context.isMainProcess()) {
        // RuntimeException: Using WebView from more than one process at once with the same data directory is not supported. https://crbug.com/558377 :
        // Current process org.mtransit.android (pid 1111), lock owner org.mtransit.android (pid 2222) is a zombie
        return
    }
    val savedLocales = AppCompatDelegate.getApplicationLocales()
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
                        if (!savedLocales.isEmpty) {
                            AppCompatDelegate.setApplicationLocales(savedLocales)
                        }
                    } finally {
                        backgroundExecutor.shutdown()
                    }
                }

                override fun onError(error: WebViewStartupException) {
                    backgroundExecutor.shutdown()
                }
            }
        )
    } catch (e: Exception) {
        MTLog.w(LocaleUtils.LOG_TAG, e, "Crash while fixing web view locale!")
        backgroundExecutor.shutdown()
    }
}

/**
 * Determines whether the WebView locale bug could affect the current configuration.
 */
@VisibleForTesting
internal fun shouldPreloadWebViewForLocaleBug(
    appLocales: LocaleListCompat = AppCompatDelegate.getApplicationLocales(),
    appLocalesEmpty: Boolean = appLocales.isEmpty,
    primaryAppLanguage: String? = appLocales.takeIf { !it.isEmpty }?.get(0)?.language,
    systemLocales: LocaleListCompat = LocaleListCompat.getDefault(),
    primarySystemLanguage: String? = systemLocales.takeIf { !it.isEmpty }?.get(0)?.language
): Boolean {
    // 1. If the user has never picked a custom in-app language, the bug won't trigger.
    if (appLocalesEmpty) return false
    // 2. Even if a custom language exists, if it matches the current OS default language,
    // the Chromium engine asset injection won't alter the app's active strings.
    return primaryAppLanguage != primarySystemLanguage
}
