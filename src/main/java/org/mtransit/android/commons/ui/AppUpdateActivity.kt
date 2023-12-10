package org.mtransit.android.commons.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import org.mtransit.android.commons.AppUpdateUtils
import org.mtransit.android.commons.AppUpdateUtils.canInstallAppUpdate
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.R

class AppUpdateActivity : Activity(),
    MTLog.Loggable {

    companion object {
        val LOG_TAG: String = AppUpdateActivity::class.java.simpleName

        private const val APP_UPDATE_IN_PROGRESS = "app_update_in_progress"
        private const val APP_UPDATE_IN_PROGRESS_DEFAULT = false
    }

    override fun getLogTag(): String = LOG_TAG

    private val progressBar: View by lazy { findViewById(R.id.progress_bar) }

    private var appUpdateInProgress: Boolean = APP_UPDATE_IN_PROGRESS_DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_update)

        if (savedInstanceState != null) {
            this.appUpdateInProgress = savedInstanceState.getBoolean(APP_UPDATE_IN_PROGRESS, APP_UPDATE_IN_PROGRESS_DEFAULT)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!this.appUpdateInProgress) {
            startAppUpdate()
        }
    }

    private fun onAppUpdateStarted() {
        progressBar.isVisible = true
        this.appUpdateInProgress = true
    }

    private fun onAppUpdateStopped() {
        progressBar.isVisible = false
        this.appUpdateInProgress = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppUpdateUtils.RC_APP_UPDATE) {
            if (resultCode == RESULT_OK) {
                MTLog.i(this, "Update flow success. Result code: $resultCode")
            } else {
                MTLog.w(this, "Update flow failed! Result code: $resultCode")
                if (resultCode == RESULT_CANCELED) {
                    finish()
                }
            }
        }
    }

    private fun startAppUpdate() {
        onAppUpdateStarted()
        AppUpdateUtils.getLastAppUpdateInfo(this) { latestAppUpdateInfo ->
            latestAppUpdateInfo?.takeIf { it.canInstallAppUpdate() }?.let { updatableAppInfo ->
                AppUpdateUtils.startAppUpdate(this, updatableAppInfo)
                finish()
            } ?: run {
                onAppUpdateStopped()
            }
        }

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        this.appUpdateInProgress = savedInstanceState.getBoolean(APP_UPDATE_IN_PROGRESS, APP_UPDATE_IN_PROGRESS_DEFAULT)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(APP_UPDATE_IN_PROGRESS, this.appUpdateInProgress)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppUpdateUtils.clearListeners()
    }

}