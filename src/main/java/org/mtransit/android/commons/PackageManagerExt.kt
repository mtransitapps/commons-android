@file:Suppress("unused")

package org.mtransit.android.commons

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

const val LOG_TAG = "PackageManagerExt"

// https://developer.android.com/reference/androidx/core/content/PackageManagerCompat
// https://developer.android.com/reference/androidx/core/content/pm/PackageInfoCompat
// https://developers.google.com/android/reference/com/google/android/gms/instantapps/PackageManagerCompat
fun PackageManager.getPackageInfoCompat(pkg: String, flags: Int): PackageInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        this.getPackageInfo(pkg, flags)
    }
}

@SuppressLint("QueryPermissionsNeeded")
fun PackageManager.getInstalledPackagesCompat(flags: Int): List<PackageInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        this.getInstalledPackages(flags)
    }
}

fun PackageManager.isAppInstalled(pkg: String): Boolean {
    return try {
        this.getPackageInfoCompat(pkg, PackageManager.GET_ACTIVITIES)
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
        PackageInfoCompat.getLongVersionCode(this.getPackageInfoCompat(pkg, 0))
    } catch (e: PackageManager.NameNotFoundException) {
        MTLog.w(LOG_TAG, e, "Error while looking up '%s' version code!", pkg)
        default
    }
}

@SuppressLint("QueryPermissionsNeeded")
fun PackageManager.getAllInstalledProvidersWithMetaData(): List<PackageInfo> {
    return try {
        this.getInstalledPackagesCompat(PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA).toList()
    } catch (e: Exception) {
        MTLog.w(LOG_TAG, e, "Error while reading installed providers w/ meta-data!") // #Android5 #Android6
        emptyList()
    }
}

fun PackageManager.getInstalledProvidersWithMetaData(pkg: String): List<ProviderInfo>? {
    return getInstalledProvidersWithMetaDataArray(pkg)
        ?.toList()
}

fun PackageManager.getInstalledProvidersWithMetaDataArray(pkg: String): Array<out ProviderInfo>? {
    return getAllInstalledProvidersWithMetaData()
        .firstOrNull { packageInfo ->
            packageInfo.packageName == pkg
        }?.providers
}

fun PackageManager.getInstalledProviderWithMetaData(pkg: String, providerAuthority: String): ProviderInfo? {
    return getInstalledProvidersWithMetaDataArray(pkg)
        ?.singleOrNull { providerInfo ->
            providerInfo.authority == providerAuthority
        }
}