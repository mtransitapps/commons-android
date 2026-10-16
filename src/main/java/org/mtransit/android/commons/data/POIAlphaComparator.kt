package org.mtransit.android.commons.data

val POI_ALPHA_COMPARATOR = Comparator<POI> { o1: POI, o2: POI ->
    o1.compareToAlpha(o2)
}
