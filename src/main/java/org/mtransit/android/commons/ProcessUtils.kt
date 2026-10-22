package org.mtransit.android.commons

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

object ProcessUtils {

    fun Context.isMainProcess() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageName == Application.getProcessName()
        } else {
            val pid = android.os.Process.myPid()
            getSystemService<ActivityManager>()
                ?.runningAppProcesses
                ?.any { runningProcess ->
                    runningProcess.pid == pid && runningProcess.processName == packageName
                }
                ?: false
        }
}
