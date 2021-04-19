package org.mtransit.android.commons

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import org.json.JSONObject
import org.mtransit.android.commons.provider.GTFSProvider
import org.mtransit.android.commons.receiver.DataChange
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
            triggerAsyncRefreshAppUpdateInfo(context, filter)
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
        lastVersionCode: Int = -1,
        newVersionCode: Int = PackageManagerUtils.getAppVersionCode(context),
        sync: Boolean = false
    ) {
        MTLog.v(this, "setAvailableVersionCode($newVersionCode)") // DEBUG
        if (lastVersionCode == newVersionCode) {
            MTLog.d(this, "setAvailableVersionCode() > SKIP (same version code)")
            return
        }
        PreferenceUtils.savePrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE, newVersionCode, sync)
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
        MTLog.v(this, "setLastCheckInMs($lastCheckInMs)") // DEBUG
        PreferenceUtils.savePrefLcl(context, PREF_KEY_AVAILABLE_VERSION_CODE_LAST_CHECK_IN_MS, lastCheckInMs, sync)
    }

    private fun setAvailableVersionCodeAndLastCheckInMs(
        context: Context,
        lastVersionCode: Int = -1,
        newVersionCode: Int = PackageManagerUtils.getAppVersionCode(context),
        lastCheckInMs: Long = TimeUtils.currentTimeMillis(),
        sync: Boolean = false
    ) {
        MTLog.v(this, "setAvailableVersionCodeAndLastCheckInMs($lastVersionCode, $newVersionCode, $lastCheckInMs)") // DEBUG
        setAvailableVersionCode(context, lastVersionCode, newVersionCode, sync)
        setLastCheckInMs(context, lastCheckInMs, sync)
    }

    @JvmOverloads
    @JvmStatic
    fun triggerAsyncRefreshAppUpdateInfo(
        context: Context?,
        filter: AppUpdateFilter? = null
    ) {
        if (context == null) {
            MTLog.w(this, "triggerAsyncRefreshAppUpdateInfo() > SKIP (no context)")
            return
        }
        val lastAvailableVersionCode = getLastAvailableVersionCode(context, -1)
        val currentVersionCode = PackageManagerUtils.getAppVersionCode(context)
        if (currentVersionCode in 1 until lastAvailableVersionCode) {
            MTLog.d(this, "triggerAsyncRefreshAppUpdateInfo() > SKIP (new version code already available ($lastAvailableVersionCode > $currentVersionCode))")
            return
        }
        val lastCheckInMs = getLastCheckInMs(context)
        MTLog.d(this, "lastCheckInMs: $lastCheckInMs") // DEBUG
        val inFocus: Boolean = filter?.inFocus ?: false
        val shortTimeAgo = TimeUtils.currentTimeMillis() - TimeUnit.HOURS.toMillis(if (inFocus) 6L else 24L)
        MTLog.d(this, "shortTimeAgo: $shortTimeAgo") // DEBUG
        if (shortTimeAgo < lastCheckInMs) {
            val timeLapsedInHours = TimeUnit.MILLISECONDS.toHours(TimeUtils.currentTimeMillis() - lastCheckInMs)
            MTLog.d(this, "triggerAsyncRefreshAppUpdateInfo() > SKIP (last successful refresh too recent ($timeLapsedInHours hours)")
            return
        }
        if (FORCE_UPDATE_AVAILABLE) {
            val newAvailableVersionCode = 1 + if (lastAvailableVersionCode > 0) lastAvailableVersionCode else currentVersionCode
            MTLog.d(this, "triggerAsyncRefreshAppUpdateInfo() > FORCE_UPDATE_AVAILABLE to $newAvailableVersionCode.")
            setAvailableVersionCodeAndLastCheckInMs(context, lastAvailableVersionCode, newAvailableVersionCode, TimeUtils.currentTimeMillis())
            return
        }
        val appUpdateManager = AppUpdateManagerFactory.create(context)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnCompleteListener { task -> // ASYNC
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
                        setAvailableVersionCodeAndLastCheckInMs(context, lastAvailableVersionCode, currentVersionCode, TimeUtils.currentTimeMillis())
                    }
                }
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    MTLog.d(this, "Update available")
                    val newAvailableVersionCode = appUpdateInfo.availableVersionCode()
                    if (newAvailableVersionCode > 0) {
                        setAvailableVersionCodeAndLastCheckInMs(context, lastAvailableVersionCode, newAvailableVersionCode, TimeUtils.currentTimeMillis())
                        if ((lastAvailableVersionCode == -1 || lastAvailableVersionCode == currentVersionCode) // last was unknown OR same as current
                            && newAvailableVersionCode > lastAvailableVersionCode // AND new is newer than last => available update just discovered
                        ) {
                            DataChange.broadcastDataChange(context, GTFSProvider.getAUTHORITY(context), context.packageName, true) // trigger update in MT
                        }
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