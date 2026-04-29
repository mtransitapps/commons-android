package org.mtransit.android.commons.pref

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

// https://github.com/Jintin/PreferencesExtension/blob/master/preferences/src/main/java/com/jintin/preferencesextension/PreferenceLiveData.kt
abstract class PreferenceLiveData<T>(
    private val preferences: SharedPreferences,
    private val key: String,
    private val notifyInitValue: Boolean
) : LiveData<T>() {

    private val listener by lazy {
        PreferenceListener(
            preferences,
            notifyInitValue,
            key,
            ::getPreferencesValue,
            ::setValue,
            ::getValue
        )
    }

    override fun onActive() {
        super.onActive()
        listener.register()
    }

    override fun onInactive() {
        super.onInactive()
        listener.unregister()
    }

    abstract fun getPreferencesValue(): T

}
