package org.mtransit.android.commons.provider.news.youtube

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import org.mtransit.android.commons.PreferenceUtils

class YouTubeStorage(
    context: Context,
) {

    companion object {
        private const val PREF_KEY_AGENCY_LAST_UPDATE_MS = "pYouTubeNewsLastUpdate"
        private const val PREF_KEY_AGENCY_LAST_UPDATE_LANG = "pYouTubeNewsLastUpdateLang"
    }

    private val prefLcl: SharedPreferences by lazy { PreferenceUtils.getPrefLcl(context) }

    @WorkerThread
    fun getLastUpdateMs(default: Long) =
        prefLcl.getLong(PREF_KEY_AGENCY_LAST_UPDATE_MS, default)

    @WorkerThread
    fun saveLastUpdateMs(lastUpdateInMs: Long?) {
        prefLcl.edit { lastUpdateInMs?.let { putLong(PREF_KEY_AGENCY_LAST_UPDATE_MS, it) } ?: remove(PREF_KEY_AGENCY_LAST_UPDATE_MS) }
    }

    @WorkerThread
    fun getLastUpdateLang(default: String) =
        prefLcl.getString(PREF_KEY_AGENCY_LAST_UPDATE_LANG, default) ?: default

    @WorkerThread
    fun saveLastUpdateLang(lang: String?) {
        prefLcl.edit { lang?.let { putString(PREF_KEY_AGENCY_LAST_UPDATE_LANG, it) } ?: remove(PREF_KEY_AGENCY_LAST_UPDATE_LANG) }
    }
}
