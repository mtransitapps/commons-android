package org.mtransit.android.commons.provider.news.rss

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import org.mtransit.android.commons.PreferenceUtils

class RssStorage @JvmOverloads constructor(
    context: Context,
    private val prefix: String? = null,
) {

    companion object {
        private const val PREF_KEY_AGENCY_LAST_UPDATE_MS = "pRSSNewsLastUpdate"

        private const val PREF_KEY_AGENCY_LAST_UPDATE_LANG = "pRSSNewsLastUpdateLang"
    }

    private val prefLcl: SharedPreferences by lazy { PreferenceUtils.getPrefLcl(context.applicationContext) }

    private fun prefKey(key: String) = prefix?.let { "$it:$key" } ?: key

    @WorkerThread
    fun getLastUpdateMs(default: Long) =
        prefLcl.getLong(prefKey(PREF_KEY_AGENCY_LAST_UPDATE_MS), default)

    @WorkerThread
    fun saveLastUpdateMs(lastUpdateInMs: Long?) {
        prefLcl.edit {
            saveLastUpdateMs(lastUpdateInMs)
        }
    }

    private fun SharedPreferences.Editor.saveLastUpdateMs(lastUpdateInMs: Long?) {
        lastUpdateInMs?.let { putLong(prefKey(PREF_KEY_AGENCY_LAST_UPDATE_MS), it) }
            ?: run { remove(prefKey(PREF_KEY_AGENCY_LAST_UPDATE_MS)) }
    }

    @WorkerThread
    fun getLastUpdateLang(default: String) =
        prefLcl.getString(prefKey(PREF_KEY_AGENCY_LAST_UPDATE_LANG), default) ?: default

    @WorkerThread
    fun saveLastUpdateLang(lang: String?) {
        prefLcl.edit {
            saveLastUpdateLang(lang)
        }
    }

    private fun SharedPreferences.Editor.saveLastUpdateLang(lang: String?) {
        lang?.let { putString(prefKey(PREF_KEY_AGENCY_LAST_UPDATE_LANG), lang) }
            ?: run { remove(prefKey(PREF_KEY_AGENCY_LAST_UPDATE_LANG)) }
    }

    @WorkerThread
    fun saveLastUpdate(lastUpdateInMs: Long?, lang: String?) {
        prefLcl.edit {
            saveLastUpdateMs(lastUpdateInMs)
            saveLastUpdateLang(lang)
        }
    }
}
