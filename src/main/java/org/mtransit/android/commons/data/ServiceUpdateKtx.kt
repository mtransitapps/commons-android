package org.mtransit.android.commons.data

import org.mtransit.android.commons.MTLog

fun ServiceUpdate.syncTargetUUID(targetUUIDs: Map<String, String>?) {
    targetUUIDs?.takeIf { it.isNotEmpty() } ?: return
    this.targetUUID = targetUUIDs[this.targetUUID]
        ?: run {
            MTLog.w("ServiceUpdate", "No target UUID in '$targetUUIDs' for service update: $this!")
            return
        }
}
