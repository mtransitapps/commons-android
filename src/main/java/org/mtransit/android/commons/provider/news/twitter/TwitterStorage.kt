package org.mtransit.android.commons.provider.news.twitter

import android.content.Context
import androidx.annotation.WorkerThread
import org.mtransit.android.commons.PreferenceUtils
import org.mtransit.android.commons.provider.TwitterNewsProvider

object TwitterStorage {
    /**
     * Override if multiple [TwitterNewsProvider] implementations in same app.
     */
    private const val PREF_KEY_AGENCY_LAST_UPDATE_MS = "pTwitterNewsLastUpdate"

    /**
     * Override if multiple [TwitterNewsProvider] implementations in same app.
     */
    private const val PREF_KEY_AGENCY_LAST_UPDATE_LANG = "pTwitterNewsLastUpdateLang"

    /**
     * Override if multiple [TwitterNewsDbHelper] implementations in same app.
     */
    private const val PREF_KEY_AGENCY_USER_NAME_ID = "pTwitterNewsUserNameId_"

    /**
     * Override if multiple [TwitterNewsDbHelper] implementations in same app.
     */
    private const val PREF_KEY_AGENCY_USER_NAME_SINCE_ID = "pTwitterNewsUserNameSinceId_"

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

    @WorkerThread
    fun saveUserNameId(context: Context, userName: String, id: String) {
        PreferenceUtils.savePrefLclSync(context, "$PREF_KEY_AGENCY_USER_NAME_ID$userName", id)
    }

    @WorkerThread
    fun getUserNameId(context: Context, userName: String, default: String): String {
        return PreferenceUtils.getPrefLclNN(context, "$PREF_KEY_AGENCY_USER_NAME_ID$userName", default)
    }

    @WorkerThread
    fun saveUserNameSinceId(context: Context, userName: String, sinceId: String) {
        PreferenceUtils.savePrefLclSync(context, "$PREF_KEY_AGENCY_USER_NAME_SINCE_ID$userName", sinceId)
    }

    @WorkerThread
    fun getUserNameSinceId(context: Context, userName: String, default: String): String {
        return PreferenceUtils.getPrefLclNN(context, "$PREF_KEY_AGENCY_USER_NAME_SINCE_ID$userName", default)
    }
}