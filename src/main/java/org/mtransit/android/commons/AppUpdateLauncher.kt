package org.mtransit.android.commons

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import org.mtransit.android.commons.ui.AppUpdateActivity

object AppUpdateLauncher : MTLog.Loggable {

    private val LOG_TAG: String = AppUpdateLauncher::class.java.simpleName

    override fun getLogTag() = LOG_TAG

    @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions")
    @JvmStatic
    fun launchAppUpdate(context: Context, pkg: String) {
        val activityOpened = try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setPackage(pkg)
                setClassName(pkg, AppUpdateActivity.CLASS_NAME)
            }
            // DISABLED FOR NOW > works randomly
            if (false && intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: ActivityNotFoundException) {
            MTLog.d(this, e, "App update activity not found!")
            false
        }
        if (!activityOpened) {
            StoreUtils.viewAppPage(context, pkg, context.getString(R.string.google_play))
        }
    }
}