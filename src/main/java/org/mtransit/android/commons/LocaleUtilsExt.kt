package org.mtransit.android.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
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
    val savedLocales = AppCompatDelegate.getApplicationLocales()
    val systemResConfig = Resources.getSystem().configuration // not using app context to get OS config
    if (!shouldPreloadWebViewForLocaleBug(ConfigurationCompat.getLocales(systemResConfig), savedLocales)) {
        return
    }
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

/**
 * Determines whether the WebView locale bug could affect the current configuration.
 */
@VisibleForTesting
internal fun shouldPreloadWebViewForLocaleBug(
    systemLocales: LocaleListCompat,
    savedAppLocales: LocaleListCompat
): Boolean {
    val primarySystemLanguage = systemLocales.takeIf { !it.isEmpty }?.get(0)?.language ?: return true
    if (savedAppLocales.isEmpty) {
        return false // App follows system, no preload needed.
    }
    val primarySavedAppLanguage = savedAppLocales.get(0)?.language
    return primarySavedAppLanguage != null && primarySavedAppLanguage != primarySystemLanguage
}

internal fun getPrimarySupportedDefaultLocale(context: Context) =
    getPrimarySupportedDefaultLocale(ConfigurationCompat.getLocales(context.resources.configuration))

internal fun getPrimarySupportedDefaultLocale(
    defaultLocales: LocaleListCompat,
): Locale {
    for (idx in 0 until defaultLocales.size()) {
        val defaultLocale = defaultLocales.get(idx) ?: continue
        val language = defaultLocale.language
        if (LocaleUtils.isFR(language) || LocaleUtils.isEN(language)) {
            return defaultLocale
        }
    }
    return Locale.ENGLISH
}
