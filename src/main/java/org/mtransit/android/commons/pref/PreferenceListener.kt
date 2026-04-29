package org.mtransit.android.commons.pref

import android.content.SharedPreferences

class PreferenceListener<T>(
    private val preferences: SharedPreferences,
    notifyInitValue: Boolean,
    private val valueKey: String,
    private val getPreferencesValue: () -> T,
    private val setValue: (T) -> Unit,
    private val getValue: () -> T,
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
        setValue(newValue)
    }
}
