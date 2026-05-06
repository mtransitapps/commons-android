package org.mtransit.android.commons.data

import org.mtransit.android.commons.millisToInstant
import kotlin.time.Instant

val POIStatus.readFromSource: Instant? get() = this.readFromSourceAtInMs.takeIf { it > 0 }?.millisToInstant()
