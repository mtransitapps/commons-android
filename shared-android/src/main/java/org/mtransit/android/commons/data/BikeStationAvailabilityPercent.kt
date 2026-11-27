package org.mtransit.android.commons.data

/**
 * R8: [org.mtransit.android.commons.R.raw.org_mtransit_android_keep_poi_status_availability]]
 */
@Suppress("LongParameterList")
fun makeBikeStationAvailabilityPercent(
    id: Int? = null,
    targetUUID: String,
    lastUpdateMs: Long,
    maxValidityInMs: Long,
    readFromSourceAtInMs: Long,
    sourceLabel: String? = null,
    value1: AvailabilityPercentValue,
    value1SubValue1: AvailabilityPercentValue? = null,
    value2: AvailabilityPercentValue,
    isInstalled: Boolean
) = AvailabilityPercent(
    id,
    targetUUID,
    lastUpdateMs,
    maxValidityInMs,
    readFromSourceAtInMs,
    sourceLabel,
    false,
    value1,
    "no_default_bikes", // WARNING: R8
    "default_bikes_quantity", // WARNING: R8
    value1SubValue1,
    value2,
    if (isInstalled) AvailabilityPercent.STATUS_OK else AvailabilityPercent.STATUS_NOT_INSTALLED
)

fun makeValue2(
    value2: Int,
    value2Color: Int,
    value2ColorBg: Int,
) = AvailabilityPercentValue(
    value2,
    "no_docks", // WARNING: R8
    "docks_quantity", // WARNING: R8
    value2Color,
    value2ColorBg
)

fun makeValue1(
    value1: Int,
    value1Color: Int,
    value1ColorBg: Int,
) = AvailabilityPercentValue(
    value1,
    "no_bikes", // WARNING: R8,
    "bikes_quantity", // WARNING: R8,
    value1Color,
    value1ColorBg
)

fun makeValue1SubValue1(
    value1SubValue1: Int?,
    value1SubValue1Color: Int?,
    value1SubValue1ColorBg: Int?
): AvailabilityPercentValue? {
    return AvailabilityPercentValue(
        value1SubValue1 ?: return null,
        "no_e_bikes", // WARNING: R8
        "e_bikes_quantity", // WARNING: R8
        value1SubValue1Color ?: return null,
        value1SubValue1ColorBg ?: return null,
    )
}
