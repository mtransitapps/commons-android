package org.mtransit.android.commons

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.core.content.getSystemService
import java.io.File

@Volatile
private var cachedProcessName: String? = null

val Context.currentProcessName: String?
    get() {
        cachedProcessName?.let { return it }
        val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            // Fallback for Android 8.1 and below: Read /proc/self/cmdline (Fast & No IPC)
            try {
                File("/proc/self/cmdline").readText().substringBefore('\u0000').trim().takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                null
            } ?: run {
                // ELSE slow IPC call
                val pid = Process.myPid()
                getSystemService<ActivityManager>()?.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName
            }
        }
        if (!processName.isNullOrBlank()) {
            cachedProcessName = processName
        }
        return processName
    }

val Context.isMainProcess: Boolean
    get() = currentProcessName == packageName
