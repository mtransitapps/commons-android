package org.mtransit.android.commons.provider.news.twitter

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import org.mtransit.android.commons.PreferenceUtils

class TwitterStorage(
    context: Context,
) {

    companion object {
        private const val PREF_KEY_AGENCY_LAST_UPDATE_MS = "pTwitterNewsLastUpdate"

        private const val PREF_KEY_AGENCY_LAST_UPDATE_LANG = "pTwitterNewsLastUpdateLang"

        private const val PREF_KEY_AGENCY_USER_NAME_ID = "pTwitterNewsUserNameId_"

        private const val PREF_KEY_AGENCY_USER_NAME_SINCE_ID = "pTwitterNewsUserNameSinceId_"
    }

    private val prefLcl: SharedPreferences by lazy { PreferenceUtils.getPrefLcl(context.applicationContext) }

    @WorkerThread
    fun getLastUpdateMs(default: Long) =
        prefLcl.getLong(PREF_KEY_AGENCY_LAST_UPDATE_MS, default)

    @WorkerThread
    fun saveLastUpdateMs(lastUpdateInMs: Long) {
        prefLcl.edit { putLong(PREF_KEY_AGENCY_LAST_UPDATE_MS, lastUpdateInMs) }
    }

    @WorkerThread
    fun getLastUpdateLang(default: String) =
        prefLcl.getString(PREF_KEY_AGENCY_LAST_UPDATE_LANG, default) ?: default

    @WorkerThread
    fun saveLastUpdateLang(lang: String) {
        prefLcl.edit { putString(PREF_KEY_AGENCY_LAST_UPDATE_LANG, lang) }
    }

    @WorkerThread
    fun saveUserNameId(userName: String, id: String) {
        prefLcl.edit { putString("$PREF_KEY_AGENCY_USER_NAME_ID$userName", id) }
    }

    @WorkerThread
    fun getUserNameId(userName: String, default: String) =
        prefLcl.getString("$PREF_KEY_AGENCY_USER_NAME_ID$userName", default) ?: default

    @WorkerThread
    fun saveUserNameSinceId(userName: String, sinceId: String) {
        prefLcl.edit { putString("$PREF_KEY_AGENCY_USER_NAME_SINCE_ID$userName", sinceId) }
    }

    @WorkerThread
    fun getUserNameSinceId(userName: String, default: String) =
        prefLcl.getString("$PREF_KEY_AGENCY_USER_NAME_SINCE_ID$userName", default) ?: default
}