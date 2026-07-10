package org.mtransit.android.commons.provider.gtfs

import org.mtransit.android.commons.provider.GTFSRealTimeProvider

object GTFSRealTimeProviderShared {

    private var _directionIdsMismatch: Boolean? = null

    @JvmStatic
    fun GTFSRealTimeProvider.setDirectionIdsMismatch(mismatch: Boolean) {
        if (_directionIdsMismatch == mismatch) return
        _directionIdsMismatch = mismatch
        storage.saveDirectionIdsMismatch(mismatch)
    }

    @JvmStatic
    fun GTFSRealTimeProvider.hasDirectionIdsMismatch(): Boolean {
        return areDirectionIdsMismatch() != null
    }

    @JvmStatic
    fun GTFSRealTimeProvider.areDirectionIdsMismatch() =
        _directionIdsMismatch
            ?: storage.getDirectionIdsMismatch(false)?.also { _directionIdsMismatch = it }
}
