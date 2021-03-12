package org.mtransit.android.commons

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import androidx.core.content.pm.PackageInfoCompat

const val LOG_TAG = "PackageManagerExt"

fun PackageManager.isAppInstalled(pkg: String): Boolean {
    return try {
        this.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

fun PackageManager.isAppEnabled(pkg: String): Boolean {
    return try {
        val appEnabledSetting: Int = this.getApplicationEnabledSetting(pkg)
        return (appEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                || appEnabledSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    } catch (e: IllegalArgumentException) {
        false // app does not exist
    }
}

fun PackageManager.getAppLongVersionCode(pkg: String, default: Long = -1L): Long {
    return try {
        PackageInfoCompat.getLongVersionCode(this.getPackageInfo(pkg, 0))
    } catch (e: PackageManager.NameNotFoundException) {
        MTLog.w(LOG_TAG, e, "Error while looking up '%s' version code!", pkg)
        default
    }
}

@SuppressLint("QueryPermissionsNeeded")
fun PackageManager.getInstalledProvidersWithMetaData(pkg: String): List<ProviderInfo>? {
    return this.getInstalledPackages(PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA)
        .firstOrNull {
            it.packageName == pkg
        }?.providers?.toList()
}