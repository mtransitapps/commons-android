package org.mtransit.android.commons

import android.content.res.Resources
import androidx.annotation.Px
import kotlin.math.roundToInt

@get:Px
val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()

val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).roundToInt()
