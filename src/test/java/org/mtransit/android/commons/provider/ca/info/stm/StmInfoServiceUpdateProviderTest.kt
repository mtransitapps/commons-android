package org.mtransit.android.commons.provider.ca.info.stm

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mtransit.android.commons.provider.ca.info.stm.StmInfoServiceUpdateProvider.parseTranslations
import org.mtransit.android.commons.provider.ca.info.stm.StmInfoServiceUpdateProvider.toServiceUpdates
import org.mtransit.android.commons.secsToInstant
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.days

class StmInfoServiceUpdateProviderTest {

    companion object {
        private val NOW = 1772722800L.secsToInstant() // 2026-03-06 10:00
        private val MAX_VALIDITY = 1.days
        private const val SOURCE_LABEL = "stm.info"
    }

    @Test
    fun test_parseTranslations() {
        buildList {
            add(EtatServiceResponse.Alert.TranslatedText(language = "fr", text = "Titre"))
            add(EtatServiceResponse.Alert.TranslatedText(language = "en", text = null))
        }.parseTranslations().let { result ->
            assertNotNull(result)
            assertEquals(2, result.size)
            assertEquals("Titre", result["fr"])
            assertEquals("Titre", result["en"])
        }
    }

    @Test
    fun testToServiceUpdates_deduplicatesDuplicateAlerts() {
        // Two identical alerts for route "34" -> should produce 2 service updates (one per language)
        val alert = makeAlert(routeShortName = "34")
        val response = EtatServiceResponse(
            header = null,
            alerts = listOf(alert, alert),
        )

        val result = response.toServiceUpdates(
            maxValidity = MAX_VALIDITY,
            sourceLabel = SOURCE_LABEL,
            now = NOW,
        )

        // 2 languages (fr, en) x 1 unique targetUUID -> 2 distinct service updates (not 4)
        assertEquals(2, result.size)
    }

    @Test
    fun testToServiceUpdates_distinctLanguagesPerAlert() {
        // Alert has both "fr" and "en" in both header and description (potential duplicates in languages list)
        val alert = EtatServiceResponse.Alert(
            activePeriods = null,
            cause = null,
            effect = null,
            informedEntities = listOf(
                EtatServiceResponse.Alert.InformedEntity(routeShortName = "14", directionId = null, stopCode = null),
            ),
            headerTexts = listOf(
                EtatServiceResponse.Alert.TranslatedText(language = "fr", text = "Titre"),
                EtatServiceResponse.Alert.TranslatedText(language = "en", text = "Title"),
            ),
            descriptionTexts = listOf(
                // Same languages as headerTexts - should not produce duplicate entries
                EtatServiceResponse.Alert.TranslatedText(language = "fr", text = "Desc FR"),
                EtatServiceResponse.Alert.TranslatedText(language = "en", text = "Desc EN"),
            ),
        )
        val response = EtatServiceResponse(header = null, alerts = listOf(alert))

        val result = response.toServiceUpdates(
            maxValidity = MAX_VALIDITY,
            sourceLabel = SOURCE_LABEL,
            now = NOW,
        )

        // Should produce exactly 2: one for "fr", one for "en"
        assertEquals(2, result.size)
        assertEquals(result.size, result.distinctBy { it.targetUUID to it.language }.size)
    }

    @Test
    fun testToServiceUpdates_nullResponse_returnsEmpty() {
        val result = null.toServiceUpdates(
            maxValidity = MAX_VALIDITY,
            sourceLabel = SOURCE_LABEL,
            now = NOW,
        )

        assertEquals(0, result.size)
    }

    @Test
    fun testToServiceUpdates_multipleDistinctAlerts() {
        // Alerts for two different routes -> should produce 4 service updates (2 per route, one per language)
        val alert34 = makeAlert(routeShortName = "34")
        val alert35 = makeAlert(routeShortName = "35")
        val response = EtatServiceResponse(header = null, alerts = listOf(alert34, alert35))

        val result = response.toServiceUpdates(
            maxValidity = MAX_VALIDITY,
            sourceLabel = SOURCE_LABEL,
            now = NOW,
        )

        assertEquals(4, result.size)
        assertEquals(result.size, result.distinctBy { it.targetUUID to it.language }.size)
    }

    private fun makeAlert(
        routeShortName: String,
        directionId: String? = null,
        stopCode: String? = null,
        startInSec: Long? = null,
        endInSec: Long? = null,
        headerFr: String = "Titre",
        headerEn: String = "Title",
        descFr: String = "Description FR",
        descEn: String = "Description EN",
    ) = EtatServiceResponse.Alert(
        activePeriods = EtatServiceResponse.Alert.ActivePeriods(
            startInSec = startInSec,
            endInSec = endInSec,
        ),
        cause = null,
        effect = null,
        // The STM API sends each attribute (route, direction, stop) as a separate InformedEntity object
        informedEntities = listOfNotNull(
            EtatServiceResponse.Alert.InformedEntity(
                routeShortName = routeShortName,
                directionId = null,
                stopCode = null,
            ),
            directionId?.let {
                EtatServiceResponse.Alert.InformedEntity(
                    routeShortName = null,
                    directionId = it,
                    stopCode = null,
                )
            },
            stopCode?.let {
                EtatServiceResponse.Alert.InformedEntity(
                    routeShortName = null,
                    directionId = null,
                    stopCode = it,
                )
            },
        ),
        headerTexts = listOf(
            EtatServiceResponse.Alert.TranslatedText(language = "fr", text = headerFr),
            EtatServiceResponse.Alert.TranslatedText(language = "en", text = headerEn),
        ),
        descriptionTexts = listOf(
            EtatServiceResponse.Alert.TranslatedText(language = "fr", text = descFr),
            EtatServiceResponse.Alert.TranslatedText(language = "en", text = descEn),
        ),
    )
}
