package org.mtransit.android.commons.data

import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.StringUtils
import org.mtransit.android.commons.TimeUtils
import org.mtransit.android.commons.provider.ServiceUpdateProviderContract

fun ServiceUpdate.syncTargetUUID(targetUUIDs: Map<String, String>?) {
    targetUUIDs?.takeIf { it.isNotEmpty() } ?: return
    this.targetUUID = targetUUIDs[this.targetUUID]
        ?: run {
            MTLog.w("ServiceUpdate", "No target UUID in '$targetUUIDs' for service update: $this!")
            return
        }
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
        getServiceUpdateLanguage()
    )
}
