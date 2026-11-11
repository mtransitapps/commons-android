package org.mtransit.android.commons

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.core.content.getSystemService
import java.io.BufferedReader
import java.io.FileReader

val Context.currentProcessName: String?
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }
        // Fallback for Android 8.1 and below: Read /proc/self/cmdline (Fast & No IPC)
        try {
            BufferedReader(FileReader("/proc/self/cmdline")).use { reader ->
                val processName = reader.readLine()
                if (!processName.isNullOrBlank()) {
                    return processName.trim()
                }
            }
        } catch (_: Exception) {
        }
        // ELSE slow IPC call
        val pid = Process.myPid()
        return getSystemService<ActivityManager>()?.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName
    }

val Context.isMainProcess: Boolean
    get() = currentProcessName == packageName
