package org.mtransit.android.commons

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
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
        defaultValue: Int = PackageManagerUtils.getAppVersionCode(context)
    ): Int {
        return PreferenceUtils.getPrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE, defaultValue)
    }

    @JvmStatic
    private fun setAvailableVersionCode(
        context: Context,
        versionCode: Int,
        sync: Boolean = false
    ) {
        PreferenceUtils.savePrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE, versionCode, sync)
    }

    @JvmStatic
    fun refreshAppUpdateInfo(context: Context?) {
        if (context == null) {
            MTLog.w(this, "refreshAppUpdateInfo() > SKIP (no context)")
            return
        }
        val currentAvailableVersionCode = getAvailableVersionCode(context, -1)
        val currentVersionCode = PackageManagerUtils.getAppVersionCode(context)
        if (currentVersionCode in 1 until currentAvailableVersionCode) {
            MTLog.w(this, "refreshAppUpdateInfo() > SKIP (new version code already available ($currentAvailableVersionCode > $currentVersionCode))")
            return
        }
        val lastCheckInMs = PreferenceUtils.getPrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE_LAST_CHECK_IN_MS, -1L)
        val twentyFourHoursAgo = TimeUtils.currentTimeMillis() - TimeUnit.DAYS.toMillis(1L)
        if (twentyFourHoursAgo < lastCheckInMs) {
            MTLog.w(this, "refreshAppUpdateInfo() > SKIP (last successful refresh too recent (${TimeUnit.MILLISECONDS.toHours(lastCheckInMs)} hours))")
            return
        }
        if (FORCE_UPDATE_AVAILABLE) {
            val availableVersionCode = currentVersionCode + 1
            setAvailableVersionCode(context, availableVersionCode)
            PreferenceUtils.savePrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE_LAST_CHECK_IN_MS, TimeUtils.currentTimeMillis(), true) // ASYNC
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
                        setAvailableVersionCode(context, currentVersionCode)
                        PreferenceUtils.savePrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE_LAST_CHECK_IN_MS, TimeUtils.currentTimeMillis(), true) // ASYNC
                    }
                }
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    MTLog.d(this, "Update available")
                    val availableVersionCode = appUpdateInfo.availableVersionCode()
                    if (availableVersionCode > 0) {
                        setAvailableVersionCode(context, availableVersionCode)
                        PreferenceUtils.savePrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE_LAST_CHECK_IN_MS, TimeUtils.currentTimeMillis(), true) // ASYNC
                    }
                }
            }
        }
    }
}