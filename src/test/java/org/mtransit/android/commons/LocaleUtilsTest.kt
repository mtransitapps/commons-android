package org.mtransit.android.commons

import androidx.core.os.LocaleListCompat
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocaleUtilsTest {

    @Test
    fun test_shouldPreloadWebViewForLocaleBug_sameLanguage() {
        LocaleUtils.setDefaultLocale(Locale.CANADA)
        val result = shouldPreloadWebViewForLocaleBug(
            systemLocales = LocaleListCompat.create(Locale.US, Locale.CANADA_FRENCH),
        )
        assertFalse(result)
    }

    @Test
    fun test_shouldPreloadWebViewForLocaleBug_differentLanguage() {
        LocaleUtils.setDefaultLocale(Locale.CANADA_FRENCH)
        val result = shouldPreloadWebViewForLocaleBug(
            systemLocales = LocaleListCompat.create(Locale.US, Locale.CANADA_FRENCH),
        )
        assertTrue(result)
    }

    @Test
    fun test_shouldPreloadWebViewForLocaleBug_differentLanguage_OtherFirst() {
        LocaleUtils.setDefaultLocale(Locale.CANADA_FRENCH)
        val result = shouldPreloadWebViewForLocaleBug(
            systemLocales = LocaleListCompat.create(Locale.GERMANY, Locale.US, Locale.CANADA_FRENCH),
        )
        assertTrue(result)
    }
}
