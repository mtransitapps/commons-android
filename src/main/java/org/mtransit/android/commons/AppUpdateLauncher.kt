package org.mtransit.android.commons

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import org.mtransit.android.commons.ui.AppUpdateActivity

object AppUpdateLauncher : MTLog.Loggable {

    val LOG_TAG: String = AppUpdateLauncher::class.java.simpleName

    override fun getLogTag(): String = LOG_TAG

    @JvmStatic
    fun launchAppUpdate(context: Context, pkg: String) {
        var activityOpened = false
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setPackage(pkg)
                setClassName(pkg, AppUpdateActivity.CLASS_NAME)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                activityOpened = true
            }
        } catch (e: ActivityNotFoundException) {
            MTLog.d(this, e, "App update activity not found!")
            activityOpened = false
        }
        if (!activityOpened) {
            StoreUtils.viewAppPage(context, pkg, context.getString(R.string.google_play))
        }
    }
}