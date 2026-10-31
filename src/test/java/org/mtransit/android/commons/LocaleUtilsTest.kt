package org.mtransit.android.commons

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocaleUtilsTest {

    @Test
    fun test_shouldPreloadWebViewForLocaleBug_userNeverPickedCustomAppLocale() {
        shouldPreloadWebViewForLocaleBug(
            appLocalesEmpty = true,
            primaryAppLanguage = "en",
            primarySystemLanguage = "en"
        ).let { result ->
            assertFalse(result)
        }
    }

    @Test
    fun test_shouldPreloadWebViewForLocaleBug_usePickedDefaultOSLangForAppLang() {
        shouldPreloadWebViewForLocaleBug(
            appLocalesEmpty = false,
            primaryAppLanguage = "en",
            primarySystemLanguage = "en"
        ).let { result ->
            assertFalse(result)
        }
    }

    @Test
    fun test_shouldPreloadWebViewForLocaleBug_usePickedFRForAppLang() {
        shouldPreloadWebViewForLocaleBug(
            appLocalesEmpty = false,
            primaryAppLanguage = "fr",
            primarySystemLanguage = "fr"
        ).let { result ->
            assertFalse(result)
        }
    }

    @Test
    fun test_shouldPreloadWebViewForLocaleBug_usePickedSecondaryOSLangForAppLang() {
        shouldPreloadWebViewForLocaleBug(
            appLocalesEmpty = false,
            primaryAppLanguage = "en",
            primarySystemLanguage = "es"
        ).let { result ->
            assertTrue(result)
        }
    }
}
