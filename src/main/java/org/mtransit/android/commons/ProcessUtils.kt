package org.mtransit.android.commons

import android.app.ActivityManager
import android.app.Application.getProcessName
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.core.content.getSystemService

val Context.currentProcessName: String?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        getProcessName()
    } else {
        val pid = Process.myPid()
        getSystemService<ActivityManager>()?.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName
    }

val Context.isMainProcess: Boolean
    get() = currentProcessName == packageName
