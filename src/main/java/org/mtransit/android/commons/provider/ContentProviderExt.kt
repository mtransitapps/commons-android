package org.mtransit.android.commons.provider

import android.content.ContentProvider
import android.content.Context
import androidx.core.content.ContentProviderCompat

fun ContentProvider.requireContext(): Context {
    return ContentProviderCompat.requireContext(this)
}