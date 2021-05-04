package org.mtransit.android.commons

import android.content.res.Resources
import androidx.annotation.DimenRes

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()
val Int.px: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

fun Resources.getDimensionInt(@DimenRes id: Int): Int {
    return this.getDimension(id).toInt()
}
