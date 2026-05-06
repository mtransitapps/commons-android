package org.mtransit.android.commons.data

import org.mtransit.android.commons.TimeUtilsK
import org.mtransit.android.commons.toMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

fun makeServiceUpdate(
    optId: Int? = null,
    targetUUID: String = "uuid",
    targetTripId: String? = null,
    lastUpdate: Instant = TimeUtilsK.currentInstant(),
    maxValidity: Duration = 1.hours,
    text: String = "The text",
    optTextHTML: String? = null,
    severity: Int = ServiceUpdate.SEVERITY_NONE,
    noService: Boolean? = null,
    sourceId: String = "source_id",
    sourceLabel: String = "example.org",
    originalId: String? = null,
    language: String = "en"
) = makeServiceUpdate(
    optId = optId,
    targetUUID = targetUUID,
    targetTripId = targetTripId,
    lastUpdateMs = lastUpdate.toMillis(),
    maxValidityMs = maxValidity.inWholeMilliseconds,
    text = text,
    optTextHTML = optTextHTML,
    severity = severity,
    noService = noService,
    sourceId = sourceId,
    sourceLabel = sourceLabel,
    originalId = originalId,
    language = language,
)

fun ServiceUpdate.clone() = makeServiceUpdate(
    optId = id,
    targetUUID = targetUUID,
    targetTripId = targetTripId,
    lastUpdateMs = lastUpdateInMs,
    maxValidityMs = maxValidityInMs,
    text = text,
    optTextHTML = textHTML,
    severity = severity,
    noService = isNoService,
    sourceId = sourceId,
    sourceLabel = sourceLabel,
    originalId = originalId,
    language = language
)
