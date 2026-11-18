package org.mtransit.android.commons

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.Px
import androidx.annotation.StringRes
import kotlin.math.roundToInt

@get:Px
val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).roundToInt()

val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).roundToInt()

fun Resources.getText(@StringRes id: Int, vararg formatArgs: Any?): CharSequence =
    HtmlUtils.fromHtmlCompact(getString(id, *formatArgs))

fun Resources.getQuantityText(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any?): CharSequence =
    HtmlUtils.fromHtmlCompact(getQuantityString(id, quantity, *formatArgs))
