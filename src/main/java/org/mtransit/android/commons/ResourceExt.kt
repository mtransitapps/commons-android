package org.mtransit.android.commons

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.Px
import androidx.core.text.HtmlCompat
import kotlin.math.roundToInt

@get:Px
val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()

val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).roundToInt()

fun Resources.getQuantityText(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any?): CharSequence {
    val value = getQuantityString(id, quantity, *formatArgs)
    return HtmlUtils.fromHtmlCompact(value)
}
