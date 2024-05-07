package org.mtransit.android.commons

import com.google.gson.Gson

inline fun <reified T> Gson.fromJson(jsonString: String?): T {
    return fromJson(jsonString, T::class.java)
}