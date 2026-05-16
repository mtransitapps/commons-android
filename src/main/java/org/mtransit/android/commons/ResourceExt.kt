package org.mtransit.android.commons

import android.content.res.Resources
import androidx.annotation.DimenRes
import androidx.annotation.Px

@get:Px
val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Resources.getDimensionInt(@DimenRes id: Int): Int {
    return this.getDimension(id).toInt()
}
