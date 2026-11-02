package org.mtransit.android.commons

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocaleUtilsTest {

    @Test
    fun test_shouldPreloadWebViewForLocaleBug_sameLanguage() {
        LocaleUtils.setDefaultLocale(Locale.CANADA)
        val result = shouldPreloadWebViewForLocaleBug(
            primarySystemLanguage = "en"
        )
        assertFalse(result)
    }

    @Test
    fun test_shouldPreloadWebViewForLocaleBug_differentLanguage() {
        LocaleUtils.setDefaultLocale(Locale.CANADA_FRENCH)
        val result = shouldPreloadWebViewForLocaleBug(
            primarySystemLanguage = "en"
        )
        assertTrue(result)
    }
}
