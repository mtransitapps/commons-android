@file:Suppress("unused")

package org.mtransit.android.commons

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

@JvmOverloads
fun Context.dimensionFromAttribute(attribute: Int, defaultValue: Int = -1): Int {
    val attributes = this.obtainStyledAttributes(intArrayOf(attribute))
    val dimension = attributes.getDimensionPixelSize(0, defaultValue)
    attributes.recycle()
    return dimension
}

fun Context.registerReceiverCompat(receiver: BroadcastReceiver, filter: IntentFilter, flags: Int): Intent? {
    return ContextCompat.registerReceiver(this, receiver, filter, flags)
}

fun Context.getStringRes(@StringRes stringResId: Int, vararg formatArgsStringResIds: Int): String {
    return this.getString(stringResId, formatArgsStringResIds.map { this.getString(it) })
}

fun PowerManager.isIgnoringBatteryOpt(packageName: String): Boolean? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        this.isIgnoringBatteryOptimizations(packageName)
    } else {
        return null
    }
}