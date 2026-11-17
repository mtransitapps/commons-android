package org.mtransit.android.commons.pref

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

class PreferenceChangeLiveData(
    private val preferences: SharedPreferences,
) : LiveData<String?>() {

    private val listener: SharedPreferences.OnSharedPreferenceChangeListener by lazy {
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            postValue(key)
        }
    }

    override fun onActive() {
        super.onActive()
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onInactive() {
        super.onInactive()
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}

fun SharedPreferences.preferenceChangeLiveData(): LiveData<String?> = PreferenceChangeLiveData(this)
