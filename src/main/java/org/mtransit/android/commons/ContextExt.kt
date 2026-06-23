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

fun Context.getStringRes(@StringRes resId: Int, vararg formatArgsStringRes: Int): String =
    if (formatArgsStringRes.isEmpty()) getString(resId)
    else getString(resId, *Array(formatArgsStringRes.size) { getString(formatArgsStringRes[it]) })

fun Context.getText(@StringRes resId: Int, vararg formatArgs: Any?): CharSequence =
    if (formatArgs.isEmpty()) resources.getText(resId)
    else resources.getText(resId, *formatArgs)

fun PowerManager.isIgnoringBatteryOpt(packageName: String): Boolean {
    return this.isIgnoringBatteryOptimizations(packageName)
}
