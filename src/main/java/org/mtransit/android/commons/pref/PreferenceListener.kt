package org.mtransit.android.commons.pref

import android.content.SharedPreferences
import androidx.annotation.AnyThread
import androidx.annotation.MainThread

class PreferenceListener<T>(
    private val preferences: SharedPreferences,
    notifyInitValue: Boolean,
    private val valueKey: String,
    @AnyThread
    private val getPreferencesValue: () -> T,
    @MainThread
    private val setValue: (T) -> Unit,
    @AnyThread
    private val postValue: (T) -> Unit,
    @AnyThread
    private val getValue: () -> T?,
) : SharedPreferences.OnSharedPreferenceChangeListener {
    private var needCheckWhenRegister = false

    init {
        if (notifyInitValue) {
            setValue(getPreferencesValue())
        }
    }

    fun register() {
        preferences.registerOnSharedPreferenceChangeListener(this)
        if (needCheckWhenRegister) {
            needCheckWhenRegister = false
            updateValue(getPreferencesValue())
        }
    }

    fun unregister() {
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        needCheckWhenRegister = true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key != valueKey) return
        updateValue(getPreferencesValue())
    }

    private fun updateValue(newValue: T) {
        if (getValue() == newValue) return
        postValue(newValue)
    }
}
