package org.mtransit.android.commons.data

import androidx.annotation.ColorInt

data class AvailabilityPercentValue(
    val value: Int,
    val emptyRes: String,
    val quantityRes: String,
    @ColorInt val color: Int,
    @ColorInt val colorBg: Int,
)
