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

    @JvmStatic
    fun refreshAppUpdateInfo(context: Context?) {
        if (context == null) {
            MTLog.w(this, "refreshAppUpdateInfo() > SKIP (no context)")
            return
        }
        val currentAvailableVersionCode = getAvailableVersionCode(context, -1)
        val currentVersionCode = PackageManagerUtils.getAppVersionCode(context)
        if (currentVersionCode in 1 until currentAvailableVersionCode) {
            MTLog.d(this, "refreshAppUpdateInfo() > SKIP (new version code already available ($currentAvailableVersionCode > $currentVersionCode))")
            return
        }
        val lastCheckInMs = getLastCheckInMs(context)
        MTLog.d(this, "lastCheckInMs: $lastCheckInMs")
        val twentyFourHoursAgo = TimeUtils.currentTimeMillis() - TimeUnit.DAYS.toMillis(1L)
        MTLog.d(this, "twentyFourHoursAgo: $twentyFourHoursAgo")
        if (twentyFourHoursAgo < lastCheckInMs) {
            val timeLapsedInHours = TimeUnit.MILLISECONDS.toHours(TimeUtils.currentTimeMillis() - lastCheckInMs)
            MTLog.d(this, "refreshAppUpdateInfo() > SKIP (last successful refresh too recent ($timeLapsedInHours hours)")
            return
        }
        if (FORCE_UPDATE_AVAILABLE) {
            val availableVersionCode = (if (currentAvailableVersionCode > 0) currentAvailableVersionCode else currentVersionCode) + 1
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
}