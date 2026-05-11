package org.mtransit.android.commons

import android.content.res.Resources
import androidx.annotation.DimenRes
import androidx.annotation.Px

@get:Px
val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

@get:Px
val Int.dpToPx get() = this.dp

fun Resources.getDimensionInt(@DimenRes id: Int): Int {
    return this.getDimension(id).toInt()
}
