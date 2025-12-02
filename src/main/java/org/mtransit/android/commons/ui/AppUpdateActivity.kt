package org.mtransit.android.commons.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.view.isVisible
import org.mtransit.android.commons.AppUpdateUtils
import org.mtransit.android.commons.AppUpdateUtils.canInstallAppUpdate
import org.mtransit.android.commons.BuildConfig
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.R

class AppUpdateActivity : Activity(),
    MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = AppUpdateActivity::class.java.simpleName

        private const val APP_UPDATE_IN_PROGRESS = "app_update_in_progress"
        private const val APP_UPDATE_IN_PROGRESS_DEFAULT = false

        val CLASS_NAME: String = AppUpdateActivity::class.java.canonicalName ?: "org.mtransit.android.commons.ui.AppUpdateActivity"
    }

    override fun getLogTag() = LOG_TAG

    private val progressBar: View by lazy { findViewById(R.id.progress_bar) }

    private var appUpdateInProgress: Boolean = APP_UPDATE_IN_PROGRESS_DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_update)

        if (savedInstanceState != null) {
            this.appUpdateInProgress = savedInstanceState.getBoolean(APP_UPDATE_IN_PROGRESS, APP_UPDATE_IN_PROGRESS_DEFAULT)
        }
        findViewById<View>(android.R.id.content)?.rootView?.setOnTouchListener { view, _ ->
            view.performClick()
            close()
            true
        }
        if (BuildConfig.DEBUG) {
            findViewById<View>(android.R.id.content)?.rootView?.setBackgroundColor(Color.CYAN)
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

    private fun close() {
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppUpdateUtils.RC_APP_UPDATE) {
            if (resultCode == RESULT_OK) {
                MTLog.i(this, "Update flow success. Result code: $resultCode")
            } else {
                MTLog.w(this, "Update flow failed! Result code: $resultCode")
                if (resultCode == RESULT_CANCELED) {
                    close()
                }
            }
        }
    }

    private fun startAppUpdate() {
        onAppUpdateStarted()
        AppUpdateUtils.getLastAppUpdateInfo(this) { latestAppUpdateInfo ->
            latestAppUpdateInfo?.takeIf { it.canInstallAppUpdate() }?.let { updatableAppInfo ->
                AppUpdateUtils.startAppUpdate(this, updatableAppInfo)
                close()
            } ?: run {
                onAppUpdateStopped()
                close()
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