package org.mtransit.android.commons

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability

object AppUpdateUtils : MTLog.Loggable {

    val LOG_TAG: String = AppUpdateUtils::class.java.simpleName

    override fun getLogTag(): String = LOG_TAG

    private const val PREF_KEY_AVAILABLE_VERSION_CODE = "pAvailableVersionCode"

    @JvmStatic
    fun getAvailableVersionCode(context: Context): Int {
        return PreferenceUtils.getPrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE, PackageManagerUtils.getAppVersionCode(context))
    }

    @JvmStatic
    private fun setAvailableVersionCode(context: Context, versionCode: Int, sync: Boolean = false) {
        PreferenceUtils.savePrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE, versionCode, sync)
    }

    @JvmStatic
    fun refreshAppUpdateInfo(context: Context?) {
        if (context == null) {
            MTLog.w(this, "refreshAppUpdateInfo() > SKIP (no context)")
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
                    val appVersionCode = PackageManagerUtils.getAppVersionCode(context)
                    if (appVersionCode > 0) {
                        setAvailableVersionCode(context, appVersionCode)
                    }
                }
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    MTLog.d(this, "Update available")
                    val availableVersionCode = appUpdateInfo.availableVersionCode()
                    if (availableVersionCode > 0) {
                        setAvailableVersionCode(context, availableVersionCode)
                    }
                }
            }
        }
    }
}