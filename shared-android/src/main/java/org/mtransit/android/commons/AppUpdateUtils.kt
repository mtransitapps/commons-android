package org.mtransit.android.commons

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.installStatus
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.mtransit.android.commons.AppUpdateUtils.AppUpdateFilter.Companion.toFilter
import org.mtransit.android.commons.provider.GTFSProvider
import org.mtransit.android.commons.provider.common.sharedJson
import org.mtransit.android.commons.receiver.DataChange
import org.mtransit.commons.jsonPrimitiveOrNull
import java.util.concurrent.TimeUnit

object AppUpdateUtils : MTLogK.Loggable {

    private val LOG_TAG: String = AppUpdateUtils::class.java.simpleName

    private const val FORCE_CHECK_IN_DEBUG = false
    // private const val FORCE_CHECK_IN_DEBUG = true // DEBUG

    private const val FORCE_UPDATE_AVAILABLE = false
    // private const val FORCE_UPDATE_AVAILABLE = true // DEBUG

    override fun getLogTag() = LOG_TAG

    private const val PREF_KEY_AVAILABLE_VERSION_CODE = "pAvailableVersionCode"

    private const val PREF_KEY_AVAILABLE_VERSION_CODE_LAST_CHECK_IN_MS = "pAvailableVersionCodeLastCheckInMs"

    private val MIN_DURATION_BETWEEN_APP_VERSION_CHECK_IN_MS = TimeUnit.HOURS.toMillis(12L)
    private val MIN_DURATION_BETWEEN_APP_VERSION_CHECK_IN_FOCUS_IN_MS = TimeUnit.HOURS.toMillis(3L)

    const val RC_APP_UPDATE = 12345

    @JvmStatic
    @JvmOverloads
    fun getAvailableVersionCode(
        context: Context,
        filterS: String? = null,
    ): Int {
        return getLastAvailableVersionCode(context, -1).also { lastAvailableVersionCode ->
            triggerRefreshIfNecessary(
                context = context,
                lastAvailableVersionCode = lastAvailableVersionCode,
                filter = filterS?.toFilter()
                    ?: AppUpdateFilter(), // DEFAULT VALUES
            )
        }
    }

    @Volatile
    private var storage: SharedPreferences? = null

    private fun getStorage(context: Context) = storage ?: synchronized(this) {
        storage ?: PreferenceUtils.getPrefLcl(context.applicationContext).also { storage = it }
    }

    @Suppress("unused")
    private fun hasLastAvailableVersionCode(context: Context) =
        getStorage(context).contains(PREF_KEY_AVAILABLE_VERSION_CODE)

    @Suppress("SameParameterValue")
    private fun getLastAvailableVersionCode(
        context: Context,
        defaultValue: Int = PackageManagerUtils.getAppVersionCode(context)
    ) = getStorage(context).getInt(PREF_KEY_AVAILABLE_VERSION_CODE, defaultValue)

    private fun setAvailableVersionCode(
        context: Context,
        lastVersionCode: Int = -1,
        newVersionCode: Int = PackageManagerUtils.getAppVersionCode(context),
    ) {
        if (lastVersionCode == newVersionCode) {
            MTLogK.d(this, "setAvailableVersionCode() > SKIP (same version code)")
            return
        }
        getStorage(context).edit { putInt(PREF_KEY_AVAILABLE_VERSION_CODE, newVersionCode) }
    }

    private fun getLastCheckInMs(context: Context, defaultValue: Long = -1L) =
        getStorage(context).getLong(PREF_KEY_AVAILABLE_VERSION_CODE_LAST_CHECK_IN_MS, defaultValue)

    private fun setLastCheckInMs(context: Context, lastCheckInMs: Long = TimeUtils.currentTimeMillis()) {
        getStorage(context).edit { putLong(PREF_KEY_AVAILABLE_VERSION_CODE_LAST_CHECK_IN_MS, lastCheckInMs) }
    }

    private fun setAvailableVersionCodeAndLastCheckInMs(
        context: Context,
        lastVersionCode: Int = -1,
        newVersionCode: Int = PackageManagerUtils.getAppVersionCode(context),
        lastCheckInMs: Long = TimeUtils.currentTimeMillis(),
    ) {
        setAvailableVersionCode(context, lastVersionCode, newVersionCode)
        setLastCheckInMs(context, lastCheckInMs)
    }

    private var _appUpdateManager: AppUpdateManager? = null

    private fun getAppUpdateManager(context: Context): AppUpdateManager {
        return _appUpdateManager ?: AppUpdateManagerFactory.create(context).also { _appUpdateManager = it }
    }

    private fun triggerRefreshIfNecessary(
        context: Context,
        lastAvailableVersionCode: Int,
        filter: AppUpdateFilter? = null
    ) {
        @Suppress("SimplifyBooleanWithConstants")
        if (!FORCE_CHECK_IN_DEBUG && BuildConfig.DEBUG) {
            MTLogK.d(this, "triggerRefreshIfNecessary() > SKIP (DEBUG build)")
            return // NO WORKING FOR DEBUG BUILDS
        }
        val currentVersionCode = PackageManagerUtils.getAppVersionCode(context)
        if (currentVersionCode in 1 until lastAvailableVersionCode) { // IF current valid & current < last DO
            MTLogK.d(this, "triggerRefreshIfNecessary() > SKIP (new version code already available ($lastAvailableVersionCode > $currentVersionCode))")
            return // UPDATE ALREADY AVAILABLE
        }
        val lastCheckInMs = getLastCheckInMs(context)
        MTLogK.d(this, "lastCheckInMs: $lastCheckInMs") // DEBUG
        val shortTimeAgo = TimeUtils.currentTimeMillis() -
            if (filter?.inFocus == true) MIN_DURATION_BETWEEN_APP_VERSION_CHECK_IN_FOCUS_IN_MS else MIN_DURATION_BETWEEN_APP_VERSION_CHECK_IN_MS
        MTLogK.d(this, "shortTimeAgo: $shortTimeAgo") // DEBUG
        if (filter?.forceRefresh != true // not force refresh
            && lastAvailableVersionCode > 0 // last = valid
            && shortTimeAgo < lastCheckInMs // too recent
        ) {
            val timeLapsedInHours = TimeUnit.MILLISECONDS.toHours(TimeUtils.currentTimeMillis() - lastCheckInMs)
            MTLogK.d(this, "triggerRefreshIfNecessary() > SKIP (last successful refresh too recent ($timeLapsedInHours hours)")
            return // LAST REFRESH TOO RECENT
        }
        if (FORCE_UPDATE_AVAILABLE) {
            val newAvailableVersionCode = 1 + if (lastAvailableVersionCode > 0) lastAvailableVersionCode else currentVersionCode
            MTLogK.d(this, "triggerRefreshIfNecessary() > FORCE_UPDATE_AVAILABLE to $newAvailableVersionCode.")
            setAvailableVersionCodeAndLastCheckInMs(context, lastAvailableVersionCode, newAvailableVersionCode, TimeUtils.currentTimeMillis())
            broadcastUpdateAvailable(lastAvailableVersionCode, currentVersionCode, newAvailableVersionCode, context)
            return // USE DEBUG FORCE UPDATE++
        }
        getAppUpdateManager(context).appUpdateInfo.addOnCompleteListener { task -> // ASYNC
            if (!task.isSuccessful) {
                if (BuildConfig.DEBUG) {
                    MTLogK.d(this, "App update info did NOT complete successfully!", task.exception)
                } else {
                    MTLogK.w(this, "App update info did NOT complete successfully!", task.exception)
                }
                return@addOnCompleteListener
            }
            val appUpdateInfo = task.result ?: return@addOnCompleteListener
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    MTLogK.d(this, "Update in progress already triggered by developer")
                }

                UpdateAvailability.UNKNOWN -> {
                    MTLogK.d(this, "Update status unknown")
                }

                UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                    MTLogK.d(this, "Update NOT available")
                    if (currentVersionCode > 0) {
                        setAvailableVersionCodeAndLastCheckInMs(context, lastAvailableVersionCode, currentVersionCode, TimeUtils.currentTimeMillis())
                    }
                }

                UpdateAvailability.UPDATE_AVAILABLE -> {
                    MTLogK.d(this, "Update available")
                    val newAvailableVersionCode = appUpdateInfo.availableVersionCode()
                    if (newAvailableVersionCode > 0) {
                        setAvailableVersionCodeAndLastCheckInMs(context, lastAvailableVersionCode, newAvailableVersionCode, TimeUtils.currentTimeMillis())
                        broadcastUpdateAvailable(lastAvailableVersionCode, currentVersionCode, newAvailableVersionCode, context)
                    }
                }
            }
        }
    }

    private fun broadcastUpdateAvailable(
        lastAvailableVersionCode: Int,
        currentVersionCode: Int,
        newAvailableVersionCode: Int,
        context: Context
    ) {
        if ((lastAvailableVersionCode == -1 || lastAvailableVersionCode == currentVersionCode) // last was unknown OR same as current
            && newAvailableVersionCode > lastAvailableVersionCode // AND new is newer than last => available update just discovered
        ) {
            DataChange.broadcastDataChange(context, GTFSProvider.getAUTHORITY(context), context.packageName, true) // trigger update in MT
        }
    }

    data class AppUpdateFilter(
        val forceRefresh: Boolean = false,
        val inFocus: Boolean = false
    ) {
        companion object {
            private const val JSON_FORCE_REFRESH = "force_refresh"
            private const val JSON_IN_FOCUS = "in_focus"

            fun String.toFilter() = try {
                sharedJson.parseToJsonElement(this).jsonObject.toFilter()
            } catch (se: SerializationException) {
                MTLogK.w(LOG_TAG, "Error while parsing JSON string '$this'", se)
                throw se
            }

            fun JsonObject.toFilter() = try {
                AppUpdateFilter(
                    forceRefresh = this[JSON_FORCE_REFRESH]?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
                    inFocus = this[JSON_IN_FOCUS]?.jsonPrimitiveOrNull?.booleanOrNull ?: false,
                )
            } catch (se: SerializationException) {
                MTLogK.w(LOG_TAG, "Error while parsing app update filter '$this'!", se)
                throw se
            }

            fun AppUpdateFilter.toJson() = try {
                buildJsonObject {
                    put(JSON_FORCE_REFRESH, this@toJson.forceRefresh)
                    put(JSON_IN_FOCUS, this@toJson.inFocus)
                }
            } catch (se: SerializationException) {
                MTLogK.w(this, "Error while making JSON object '$this'", se)
                throw se
            }
        }

        fun toJsonString() = sharedJson.encodeToString(this.toJson())
    }

    fun getLastAppUpdateInfo(context: Context, onAppUpdateInfoLoaded: (AppUpdateInfo?) -> Unit) {
        @Suppress("SimplifyBooleanWithConstants")
        if (!FORCE_CHECK_IN_DEBUG && BuildConfig.DEBUG) {
            MTLogK.d(this, "getLastAppUpdateInfo() > SKIP (DEBUG build)")
            onAppUpdateInfoLoaded(null)
            return // NO WORKING FOR DEBUG BUILDS
        }
        getAppUpdateManager(context).appUpdateInfo.addOnCompleteListener {
            onAppUpdateInfoLoaded(it.result)
        }
    }

    fun AppUpdateInfo.canInstallAppUpdate(): Boolean {
        return this.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            && this.isFlexibleUpdateAllowed
    }

    fun startAppUpdate(activity: Activity, appUpdateInfo: AppUpdateInfo) {
        getAppUpdateManager(activity).registerListener(listener)
        getAppUpdateManager(activity).startUpdateFlowForResult(appUpdateInfo, activity, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE), RC_APP_UPDATE)
    }

    fun clearListeners() {
        _appUpdateManager?.unregisterListener(listener)
    }

    private val listener: InstallStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus) {
            InstallStatus.CANCELED -> MTLogK.d(this, "Cancelled app update.")
            InstallStatus.FAILED -> MTLogK.d(this, "Failed app update.")
            InstallStatus.PENDING -> MTLogK.d(this, "Pending app update.")
            InstallStatus.DOWNLOADING -> MTLogK.d(this, "Downloading app update... (${state.bytesDownloaded()} / ${state.totalBytesToDownload()})")

            InstallStatus.DOWNLOADED -> {
                MTLogK.d(this, "Downloaded app update.")
                _appUpdateManager?.completeUpdate()
            }

            InstallStatus.INSTALLING -> MTLogK.d(this, "Installing app update...")
            InstallStatus.INSTALLED -> MTLogK.d(this, "Installed app update.")
            InstallStatus.UNKNOWN -> MTLogK.d(this, "Unknown app update.")
            else -> MTLogK.d(this, "Install status '${state.installStatus}' for app update.")
        }
    }
}
