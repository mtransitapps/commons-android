package org.mtransit.android.commons.pref

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

// https://github.com/Jintin/PreferencesExtension/blob/master/preferences/src/main/java/com/jintin/preferencesextension/PreferencesExtension.kt
inline fun <reified T> SharedPreferences.get(key: String, defaultValue: T? = null): T {
    val value: Any? = when (T::class) {
        Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false)
        Int::class -> getInt(key, defaultValue as? Int ?: 0)
        Long::class -> getLong(key, defaultValue as? Long ?: 0L)
        Float::class -> getFloat(key, defaultValue as? Float ?: 0f)
        String::class -> getString(key, defaultValue as? String ?: "")
        Set::class -> {
            @Suppress("UNCHECKED_CAST") val df: Set<String> = defaultValue as? Set<String> ?: emptySet()
            getStringSet(key, df)
        }
        else -> throw RuntimeException("Not support type: ${T::class} for SharedPreferences.liveData")
    }
    return value as T
}

inline fun <reified T> SharedPreferences.liveData(
    key: String,
    defaultValue: T? = null,
    notifyInitValue: Boolean = true,
): LiveData<T> =
    object : PreferenceLiveData<T>(this, key, notifyInitValue) {
        override fun getPreferencesValue(): T = get(key, defaultValue)
    }