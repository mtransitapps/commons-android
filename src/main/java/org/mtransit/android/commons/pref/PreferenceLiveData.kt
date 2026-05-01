package org.mtransit.android.commons.pref

import android.content.SharedPreferences
import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData

// https://github.com/Jintin/PreferencesExtension/blob/master/preferences/src/main/java/com/jintin/preferencesextension/PreferenceLiveData.kt
abstract class PreferenceLiveData<T>(
    private val preferences: SharedPreferences,
    private val key: String,
    private val notifyInitValue: Boolean
) : LiveData<T>() {

    private val listener by lazy {
        PreferenceListener(
            preferences = preferences,
            notifyInitValue = notifyInitValue,
            valueKey = key,
            getPreferencesValue = ::getPreferencesValue,
            setValue = ::setValue,
            postValue = ::postValue,
            getValue = ::getValue
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

    @AnyThread
    abstract fun getPreferencesValue(): T

}
