package org.mtransit.android.commons

import android.os.Bundle

fun Bundle?.isKeyMT(key: String): Boolean {
    return this?.getString(key) == key
}