package org.mtransit.android.commons.provider

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.mtransit.android.commons.data.RouteDirectionStop

fun GTFSRealTimeProvider.setupProviderForRDS(rds: RouteDirectionStop) {
    whenever { getRouteTag(eq(rds.route)) } doReturn rds.route.originalIdHash.toString()
    whenever { getDirectionTag(eq(rds.direction)) } doReturn rds.direction.originalDirectionIdOrNull
    whenever { getStopTag(eq(rds.stop)) } doReturn rds.stop.originalIdHashString
}

fun stringIdToHash(originalId: String) = originalId.hashCode().toString()
