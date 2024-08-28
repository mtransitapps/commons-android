package org.mtransit.android.commons.provider.news.twitter

import android.content.Context
import androidx.annotation.WorkerThread
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.provider.TwitterNewsProvider

object TwitterStorage {
    /**
     * Override if multiple [TwitterNewsProvider] implementations in same app.
     */
    private const val PREF_KEY_AGENCY_LAST_UPDATE_MS = TwitterNewsDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS

    /**
     * Override if multiple [TwitterNewsProvider] implementations in same app.
     */
    private const val PREF_KEY_AGENCY_LAST_UPDATE_LANG = TwitterNewsDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_LANG

    @WorkerThread
    fun getLastUpdateMs(context: Context, default: Long): Long {
        return PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, default)
    }

    @WorkerThread
    fun saveLastUpdateMs(context: Context, lastUpdateInMs: Long) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, lastUpdateInMs)
    }

    @WorkerThread
    fun getLastUpdateLang(context: Context, default: String): String {
        return PreferenceUtils.getPrefLclNN(context, PREF_KEY_AGENCY_LAST_UPDATE_LANG, default)
    }

    @WorkerThread
    fun saveLastUpdateLang(context: Context, lang: String) {
        PreferenceUtils.savePrefLclSync(context, PREF_KEY_AGENCY_LAST_UPDATE_LANG, lang)
    }
}