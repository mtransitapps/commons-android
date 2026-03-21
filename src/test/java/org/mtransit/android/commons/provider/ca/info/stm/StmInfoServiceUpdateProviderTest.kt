package org.mtransit.android.commons.provider.ca.info.stm

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class StmInfoServiceUpdateProviderTest {

    private val now = Instant.fromEpochSeconds(1_000_000L)
    private val maxValidity = 1.days
    private val sourceLabel = "stm.info"

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

    @Test
    fun testParseServiceUpdates_deduplicatesDuplicateAlerts() {
        // Two identical alerts for route "34" -> should produce 2 service updates (one per language)
        val alert = makeAlert(routeShortName = "34")
        val response = EtatServiceResponse(
            header = null,
            alerts = listOf(alert, alert),
        )

        val result = StmInfoServiceUpdateProvider.parseServiceUpdates(
            etatServiceResponse = response,
            headerTimestamp = now,
            maxValidity = maxValidity,
            sourceLabel = sourceLabel,
        )

        // 2 languages (fr, en) x 1 unique targetUUID -> 2 distinct service updates (not 4)
        assertEquals(2, result.size)
    }

    @Test
    fun testParseServiceUpdates_distinctLanguagesPerAlert() {
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

        val result = StmInfoServiceUpdateProvider.parseServiceUpdates(
            etatServiceResponse = response,
            headerTimestamp = now,
            maxValidity = maxValidity,
            sourceLabel = sourceLabel,
        )

        // Should produce exactly 2: one for "fr", one for "en"
        assertEquals(2, result.size)
        assertEquals(result.size, result.distinctBy { it.targetUUID to it.language }.size)
    }

    @Test
    fun testParseServiceUpdates_nullResponse_returnsEmpty() {
        val result = StmInfoServiceUpdateProvider.parseServiceUpdates(
            etatServiceResponse = null,
            headerTimestamp = now,
            maxValidity = maxValidity,
            sourceLabel = sourceLabel,
        )

        assertEquals(0, result.size)
    }

    @Test
    fun testParseServiceUpdates_multipleDistinctAlerts() {
        // Alerts for two different routes -> should produce 4 service updates (2 per route, one per language)
        val alert34 = makeAlert(routeShortName = "34")
        val alert35 = makeAlert(routeShortName = "35")
        val response = EtatServiceResponse(header = null, alerts = listOf(alert34, alert35))

        val result = StmInfoServiceUpdateProvider.parseServiceUpdates(
            etatServiceResponse = response,
            headerTimestamp = now,
            maxValidity = maxValidity,
            sourceLabel = sourceLabel,
        )

        assertEquals(4, result.size)
        assertEquals(result.size, result.distinctBy { it.targetUUID to it.language }.size)
    }
}
