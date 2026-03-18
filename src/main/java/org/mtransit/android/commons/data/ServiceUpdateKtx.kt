package org.mtransit.android.commons.data

import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateProviderContract
import org.mtransit.android.commons.toMillis
import kotlin.time.Duration
import kotlin.time.Instant

fun ServiceUpdate.syncTargetUUID(targetUUIDs: Map<String, String>?) {
    targetUUIDs?.takeIf { it.isNotEmpty() } ?: return
    this.targetUUID = targetUUIDs[this.targetUUID]
        ?: run {
            MTLog.w("ServiceUpdate", "No target UUID in '$targetUUIDs' for service update: $this!")
            return
        }
}

@Suppress("unused") // main app only
fun Iterable<ServiceUpdate>?.isSeverityWarningInfo(): Pair<Boolean, Boolean> {
    this ?: return false to false
    if (any { it.isSeverityWarning }) return true to false
    if (any { it.isSeverityInfo }) return false to true
    return false to false
}

@Suppress("unused") // main app only
fun Iterable<ServiceUpdate>.distinctByOriginalId() =
    this.distinctBy { it.originalId ?: it.id } // keep 1st occurrence from sorted list (in *Manager)

fun ServiceUpdateProviderContract.makeServiceUpdateNoneList(targetable: Targetable, sourceId: String) =
    buildList {
        add(makeServiceUpdateNone(targetable.uuid, sourceId))
    }

fun ServiceUpdateProviderContract.makeServiceUpdateNone(targetUUID: String, sourceId: String) =
    makeServiceUpdate(
        targetUUID = targetUUID,
        lastUpdateMs = TimeUtils.currentTimeMillis(),
        maxValidityMs = getServiceUpdateMaxValidityInMs(),
        text = StringUtils.EMPTY,
        severity = ServiceUpdate.SEVERITY_NONE,
        sourceId = sourceId,
        sourceLabel = StringUtils.EMPTY,
        language = getServiceUpdateLanguage(),
    )

fun makeServiceUpdate(
    optId: Int? = null,
    targetUUID: String,
    targetTripId: String? = null,
    lastUpdate: Instant,
    maxValidity: Duration,
    text: String,
    optTextHTML: String? = null,
    severity: Int,
    sourceId: String,
    sourceLabel: String,
    originalId: String? = null,
    language: String
) = makeServiceUpdate(
    optId,
    targetUUID,
    targetTripId,
    lastUpdate.toMillis(),
    maxValidity.inWholeMilliseconds,
    text,
    optTextHTML,
    severity,
    sourceId,
    sourceLabel,
    originalId,
    language,
)

fun makeServiceUpdate(
    optId: Int? = null,
    targetUUID: String,
    targetTripId: String? = null,
    lastUpdateMs: Long,
    maxValidityMs: Long,
    text: String,
    optTextHTML: String? = null,
    severity: Int,
    sourceId: String,
    sourceLabel: String,
    originalId: String? = null,
    language: String
) = ServiceUpdate(
    optId,
    targetUUID,
    targetTripId,
    lastUpdateMs,
    maxValidityMs,
    text,
    optTextHTML,
    severity,
    sourceId,
    sourceLabel,
    originalId,
    language,
)
