@file:Suppress("unused")

package org.mtransit.android.commons

import android.content.Context
import android.os.PowerManager
import androidx.annotation.StringRes

@JvmOverloads
fun Context.dimensionFromAttribute(attribute: Int, defaultValue: Int = -1): Int {
    val attributes = this.obtainStyledAttributes(intArrayOf(attribute))
    val dimension = attributes.getDimensionPixelSize(0, defaultValue)
    attributes.recycle()
    return dimension
}

fun Context.getStringRes(@StringRes stringResId: Int, vararg formatArgsStringResIds: Int): String {
    return this.getString(stringResId, formatArgsStringResIds.map { this.getString(it) })
}

fun PowerManager.isIgnoringBatteryOpt(packageName: String): Boolean {
    return this.isIgnoringBatteryOptimizations(packageName)
}