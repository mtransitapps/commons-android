package org.mtransit.android.commons

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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