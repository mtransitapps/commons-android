package org.mtransit.android.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewOutcomeReceiver
import androidx.webkit.WebViewStartUpConfig
import androidx.webkit.WebViewStartUpResult
import androidx.webkit.WebViewStartupException
import org.mtransit.android.commons.LocaleUtils.getDefaultLocale
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
    val osLocales = getSystemLocales()
    if (!shouldPreloadWebViewForLocaleBug(
            systemLocales = osLocales,
            appDefaultLocale = getDefaultLocale(),
        )
    ) {
        MTLog.d(LocaleUtils.LOG_TAG, "fixWebViewLocale() > SKIP (OS and app have same primary language")
        return
    }
    if (!context.isMainProcess()) {
        // RuntimeException: Using WebView from more than one process at once with the same data directory is not supported. https://crbug.com/558377 :
        // Current process org.mtransit.android (pid 1111), lock owner org.mtransit.android (pid 2222) is a zombie
        return
    }
    MTLog.d(LocaleUtils.LOG_TAG, "fixWebViewLocale() > OS lang '${osLocales.firstLocale}' != app lang '${getDefaultLocale()}' > launching webview...'")
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
                    MTLog.d(LocaleUtils.LOG_TAG, "restoreLocale() > restoring locale from '${Locale.getDefault()}' to '${getDefaultLocale()}'.")
                    Locale.setDefault(getDefaultLocale())
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
    appDefaultLocale: Locale = getDefaultLocale(),
): Boolean {
    val primarySystemLanguage = systemLocales.firstLocale?.language ?: return true
    return appDefaultLocale.language != primarySystemLanguage
}

private val LocaleListCompat.firstLocale get() = this.takeIf { !it.isEmpty }?.get(0)

private fun getSystemLocales() = ConfigurationCompat.getLocales(
    Resources.getSystem().configuration // using Resources.getSystem() to get OS config, not app config
)

private fun Context.getAppLocales() = ConfigurationCompat.getLocales(
    this.resources.configuration
)

internal fun getPrimarySupportedDefaultLocale(context: Context) =
    getPrimarySupportedDefaultLocale(context.getAppLocales())

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
