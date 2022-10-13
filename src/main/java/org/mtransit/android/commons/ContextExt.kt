package org.mtransit.android.commons

import android.content.Context

@JvmOverloads
fun Context.dimensionFromAttribute(attribute: Int, defaultValue: Int = -1): Int {
    val attributes = this.obtainStyledAttributes(intArrayOf(attribute))
    val dimension = attributes.getDimensionPixelSize(0, defaultValue)
    attributes.recycle()
    return dimension
}