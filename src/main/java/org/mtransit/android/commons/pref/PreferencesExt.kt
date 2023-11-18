package org.mtransit.android.commons.pref

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

// https://github.com/Jintin/PreferencesExtension/blob/master/preferences/src/main/java/com/jintin/preferencesextension/PreferencesExtension.kt
inline fun <reified T> SharedPreferences.get(key: String, defaultValue: T? = null): T? {
    val value: Any? = if (contains(key)) when (T::class) {
        Boolean::class -> if (contains(key)) getBoolean(key, defaultValue as? Boolean ?: false) else defaultValue
        Int::class -> getInt(key, defaultValue as? Int ?: -1)
        Long::class -> getLong(key, defaultValue as? Long ?: -1L)
        Float::class -> getFloat(key, defaultValue as? Float ?: -1f)
        String::class -> getString(key, defaultValue as? String ?: "")
        Set::class -> {
            @Suppress("UNCHECKED_CAST") val df: Set<String> = defaultValue as? Set<String> ?: emptySet()
            getStringSet(key, df)
        }

        else -> throw RuntimeException("Not support type: ${T::class} for SharedPreferences.liveData")
    } else defaultValue
    return value as T
}

inline fun <reified T> SharedPreferences.liveDataN(
    key: String,
    defaultValue: T? = null,
    notifyInitValue: Boolean = true,
): LiveData<T?> =
    object : PreferenceLiveData<T?>(this, key, notifyInitValue) {
        override fun getPreferencesValue(): T? = get(key, defaultValue)
    }

inline fun <reified T> SharedPreferences.liveData(
    key: String,
    defaultValue: T,
    notifyInitValue: Boolean = true,
): LiveData<T> =
    object : PreferenceLiveData<T>(this, key, notifyInitValue) {
        override fun getPreferencesValue(): T = get(key, defaultValue) ?: defaultValue
    }