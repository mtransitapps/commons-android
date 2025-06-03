package org.mtransit.android.commons.provider.common

import android.content.ContentProvider
import android.content.Context
import androidx.core.content.ContentProviderCompat

val ContentProvider.requiredContext: Context
    get() = ContentProviderCompat.requireContext(this)
