package org.mtransit.android.commons.data

import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.serviceupdate.ServiceUpdateProviderContract

fun ServiceUpdate.syncTargetUUID(targetUUIDs: Map<String, String>?) {
    targetUUIDs?.takeIf { it.isNotEmpty() } ?: return
    this.targetUUID = targetUUIDs[this.targetUUID]
        ?: run {
            MTLog.w("ServiceUpdate", "No target UUID in '$targetUUIDs' for service update: $this!")
            return
        }
}

fun Iterable<ServiceUpdate>?.isSeverityWarningInfo(): Pair<Boolean, Boolean> {
    this ?: return false to false
    if (any { it.isSeverityWarning }) return true to false
    if (any { it.isSeverityInfo }) return false to true
    return false to false
}

fun Iterable<ServiceUpdate>.distinctByOriginalId() =
    this.distinctBy { it.originalId ?: it.id } // keep 1st occurrence from sorted list (in *Manager)

fun ServiceUpdateProviderContract.makeServiceUpdateNoneList(targetUUID: String, sourceId: String): ArrayList<ServiceUpdate> =
    ArrayList<ServiceUpdate>().apply {
        add(makeServiceUpdateNone(targetUUID, sourceId))
    }

fun ServiceUpdateProviderContract.makeServiceUpdateNone(targetUUID: String, sourceId: String): ServiceUpdate {
    return ServiceUpdate(
        null,
        targetUUID,
        TimeUtils.currentTimeMillis(),
        getServiceUpdateMaxValidityInMs(),
        StringUtils.EMPTY,
        null,
        ServiceUpdate.SEVERITY_NONE,
        sourceId,
        StringUtils.EMPTY,
        null,
        getServiceUpdateLanguage(),
    )
}
