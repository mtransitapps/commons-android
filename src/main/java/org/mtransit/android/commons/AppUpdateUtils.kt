package org.mtransit.android.commons

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import org.json.JSONObject
import org.mtransit.commons.StringUtils
import java.util.concurrent.TimeUnit

object AppUpdateUtils : MTLog.Loggable {

    val LOG_TAG: String = AppUpdateUtils::class.java.simpleName

    private const val FORCE_UPDATE_AVAILABLE = false
    // private const val FORCE_UPDATE_AVAILABLE = true // DEBUG

    override fun getLogTag(): String = LOG_TAG

    private const val PREF_KEY_AVAILABLE_VERSION_CODE = "pAvailableVersionCode"

    private const val PREF_KEY_AVAILABLE_VERSION_CODE_LAST_CHECK_IN_MS = "pAvailableVersionCodeLastCheckInMs"

    @JvmStatic
    @JvmOverloads
    fun getAvailableVersionCode(
        context: Context,
        filter: AppUpdateFilter? = null,
    ): Int {
        if (filter?.forceRefresh == true) {
            refreshAppUpdateInfo(context, filter)
        }
        return getLastAvailableVersionCode(context)
    }

    private fun getLastAvailableVersionCode(
        context: Context,
        defaultValue: Int = PackageManagerUtils.getAppVersionCode(context)
    ): Int {
        return PreferenceUtils.getPrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE, defaultValue)
    }

    private fun setAvailableVersionCode(
        context: Context,
        versionCode: Int = PackageManagerUtils.getAppVersionCode(context),
        sync: Boolean = false
    ) {
        MTLog.v(this, "setAvailableVersionCode($versionCode)")
        PreferenceUtils.savePrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE, versionCode, sync)
    }

    private fun getLastCheckInMs(
        context: Context,
        defaultValue: Long = -1L
    ): Long {
        return PreferenceUtils.getPrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE_LAST_CHECK_IN_MS, defaultValue)
    }

    private fun setLastCheckInMs(
        context: Context,
        lastCheckInMs: Long = TimeUtils.currentTimeMillis(),
        sync: Boolean = false
    ) {
        MTLog.v(this, "setLastCheckInMs($lastCheckInMs)")
        PreferenceUtils.savePrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE_LAST_CHECK_IN_MS, lastCheckInMs, sync)
    }

    private fun setAvailableVersionCodeAndLastCheckInMs(
        context: Context,
        versionCode: Int = PackageManagerUtils.getAppVersionCode(context),
        lastCheckInMs: Long = TimeUtils.currentTimeMillis(),
        sync: Boolean = false
    ) {
        MTLog.v(this, "setAvailableVersionCodeAndLastCheckInMs($versionCode, $lastCheckInMs)")
        setAvailableVersionCode(context, versionCode, sync)
        setLastCheckInMs(context, lastCheckInMs, sync)
    }

    @JvmOverloads
    @JvmStatic
    fun refreshAppUpdateInfo(
        context: Context?,
        filter: AppUpdateFilter? = null
    ) {
        if (context == null) {
            MTLog.w(this, "refreshAppUpdateInfo() > SKIP (no context)")
            return
        }
        val currentAvailableVersionCode = getLastAvailableVersionCode(context, -1)
        val currentVersionCode = PackageManagerUtils.getAppVersionCode(context)
        if (currentVersionCode in 1 until currentAvailableVersionCode) {
            MTLog.d(this, "refreshAppUpdateInfo() > SKIP (new version code already available ($currentAvailableVersionCode > $currentVersionCode))")
            return
        }
        val lastCheckInMs = getLastCheckInMs(context)
        MTLog.d(this, "lastCheckInMs: $lastCheckInMs")
        val inFocus: Boolean = filter?.inFocus ?: false
        val shortTimeAgo = TimeUtils.currentTimeMillis() - TimeUnit.HOURS.toMillis(if (inFocus) 6L else 24L)
        MTLog.d(this, "shortTimeAgo: $shortTimeAgo")
        if (shortTimeAgo < lastCheckInMs) {
            val timeLapsedInHours = TimeUnit.MILLISECONDS.toHours(TimeUtils.currentTimeMillis() - lastCheckInMs)
            MTLog.d(this, "refreshAppUpdateInfo() > SKIP (last successful refresh too recent ($timeLapsedInHours hours)")
            return
        }
        if (FORCE_UPDATE_AVAILABLE) {
            val availableVersionCode = 1 + if (currentAvailableVersionCode > 0) currentAvailableVersionCode else currentVersionCode
            MTLog.d(this, "refreshAppUpdateInfo() > FORCE_UPDATE_AVAILABLE to $availableVersionCode.")
            setAvailableVersionCodeAndLastCheckInMs(context, availableVersionCode, TimeUtils.currentTimeMillis())
            return
        }
        val appUpdateManager = AppUpdateManagerFactory.create(context)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                if (BuildConfig.DEBUG) {
                    MTLog.d(this, task.exception, "App update info did NOT complete successfully!")
                } else {
                    MTLog.w(this, task.exception, "App update info did NOT complete successfully!")
                }
                return@addOnCompleteListener
            }
            val appUpdateInfo = task.result
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    MTLog.d(this, "Update in progress already triggered by developer")
                }
                UpdateAvailability.UNKNOWN -> {
                    MTLog.d(this, "Update status unknown")
                }
                UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                    MTLog.d(this, "Update NOT available")
                    if (currentVersionCode > 0) {
                        setAvailableVersionCodeAndLastCheckInMs(context, currentVersionCode, TimeUtils.currentTimeMillis())
                    }
                }
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    MTLog.d(this, "Update available")
                    val availableVersionCode = appUpdateInfo.availableVersionCode()
                    if (availableVersionCode > 0) {
                        setAvailableVersionCodeAndLastCheckInMs(context, availableVersionCode, TimeUtils.currentTimeMillis())
                    }
                }
            }
        }
    }

    data class AppUpdateFilter(
        val forceRefresh: Boolean = false,
        val inFocus: Boolean = false
    ) {
        companion object {
            private const val JSON_FORCE_REFRESH = "force_refresh"
            private const val JSON_IN_FOCUS = "in_focus"

            @JvmStatic
            fun fromString(filterS: String?): AppUpdateFilter {
                try {
                    if (!filterS.isNullOrBlank()) {
                        val json = JSONObject(filterS)
                        return AppUpdateFilter(
                            forceRefresh = json.optBoolean(JSON_FORCE_REFRESH, false),
                            inFocus = json.optBoolean(JSON_IN_FOCUS, false)
                        )
                    }
                } catch (e: Exception) {
                    MTLog.w(LOG_TAG, e, "Error while parsing app update filter '$filterS'!")
                }
                return AppUpdateFilter() // DEFAULT VALUES
            }

            @JvmStatic
            fun toString(filter: AppUpdateFilter?): String {
                return try {
                    JSONObject().apply {
                        put(JSON_FORCE_REFRESH, filter?.forceRefresh)
                        put(JSON_IN_FOCUS, filter?.inFocus)
                    }.toString()
                } catch (e: Exception) {
                    MTLog.w(LOG_TAG, e, "Error while serializing app update filter '$filter'!")
                    StringUtils.EMPTY
                }
            }
        }
    }
}