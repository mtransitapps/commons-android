package org.mtransit.android.commons.location

data class AroundDiff(
    var ad: Double = AD_DEFAULT,
    var increment: Double = DEFAULT_INCREMENT,
    val incrementFactor: Double = DEFAULT_INCREMENT_FACTOR,
) {

    companion object {
        const val DEFAULT_INCREMENT = 0.01

        const val DEFAULT_INCREMENT_FACTOR = 2.0

        const val AD_MINIMUM = 0.01
        const val AD_DEFAULT = AD_MINIMUM
    }

    fun increment() {
        ad += increment
        increment *= incrementFactor // warning, might return a huge chunk of data if far away (all POIs or none)
    }
}
