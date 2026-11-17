package org.mtransit.android.commons

import androidx.core.os.LocaleListCompat
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocaleUtilsTest {

    @Test
    fun test_shouldPreloadWebViewForLocaleBug_sameLanguage() {
        val result = shouldPreloadWebViewForLocaleBug(
            systemLocales = LocaleListCompat.create(Locale.US, Locale.CANADA_FRENCH),
            appDefaultLocale = Locale.CANADA,
        )
        assertFalse(result)
    }

    @Test
    fun test_shouldPreloadWebViewForLocaleBug_differentLanguage() {
        val result = shouldPreloadWebViewForLocaleBug(
            systemLocales = LocaleListCompat.create(Locale.US, Locale.CANADA_FRENCH),
            appDefaultLocale = Locale.CANADA_FRENCH,
        )
        assertTrue(result)
    }

    @Test
    fun test_shouldPreloadWebViewForLocaleBug_differentLanguage_OtherFirst() {
        val result = shouldPreloadWebViewForLocaleBug(
            systemLocales = LocaleListCompat.create(Locale.GERMANY, Locale.US, Locale.CANADA_FRENCH),
            appDefaultLocale = Locale.ENGLISH,
        )
        assertTrue(result)
    }

    @Test
    fun test_getPrimarySupportedDefaultLocale() {
        val result = getPrimarySupportedDefaultLocale(
            defaultLocales = LocaleListCompat.create(Locale.GERMANY, Locale.CANADA, Locale.CANADA_FRENCH)
        )
        assertEquals(Locale.CANADA, result)
    }
}
