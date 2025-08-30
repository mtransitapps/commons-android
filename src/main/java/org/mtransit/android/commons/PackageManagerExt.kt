@file:Suppress("unused")

package org.mtransit.android.commons

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.core.content.pm.PackageInfoCompat

const val LOG_TAG = "PackageManagerExt"

// https://developer.android.com/reference/androidx/core/content/PackageManagerCompat
// https://developer.android.com/reference/androidx/core/content/pm/PackageInfoCompat
// https://developers.google.com/android/reference/com/google/android/gms/instantapps/PackageManagerCompat
fun PackageManager.getPackageInfoCompat(pkg: String, flags: Int): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        this.getPackageInfo(pkg, flags)
    }

@SuppressLint("QueryPermissionsNeeded")
fun PackageManager.getInstalledPackagesCompat(flags: Int): List<PackageInfo> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        this.getInstalledPackages(flags)
    }

fun PackageManager.getApplicationInfoCompat(pkg: String, flags: Int): ApplicationInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
    } else {
        this.getApplicationInfo(pkg, flags)
    }

fun PackageManager.getAppName(context: Context) = getAppName(context.packageName)

fun PackageManager.getAppName(pkg: String) = try {
    this.getApplicationLabel(this.getApplicationInfoCompat(pkg, 0))
} catch (e: PackageManager.NameNotFoundException) {
    null
}

fun PackageManager.isAppInstalled(pkg: String) = try {
    this.getPackageInfoCompat(pkg, PackageManager.GET_ACTIVITIES)
    true
} catch (e: PackageManager.NameNotFoundException) {
    false
}

fun PackageManager.getAppEnabledSetting(pkg: String) = try {
    this.getApplicationEnabledSetting(pkg)
} catch (e: IllegalArgumentException) {
    -1 // app does not exist;
}

fun PackageManager.isAppEnabled(pkg: String) =
    this.getAppEnabledSetting(pkg) in listOf(PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)

fun PackageManager.getAppLongVersionCode(context: Context) = getAppLongVersionCode(context.packageName)

fun PackageManager.getAppLongVersionCode(pkg: String, default: Long = -1L) = try {
    PackageInfoCompat.getLongVersionCode(this.getPackageInfoCompat(pkg, 0))
} catch (e: PackageManager.NameNotFoundException) {
    MTLog.d(LOG_TAG, "Pkg '$pkg' not found while reading long version code!")
    default
}

fun PackageManager.getAppVersionName(context: Context) = getAppVersionName(context.packageName)

fun PackageManager.getAppVersionName(pkg: String): String? = try {
    this.getPackageInfoCompat(pkg, 0).versionName
} catch (e: PackageManager.NameNotFoundException) {
    MTLog.d(LOG_TAG, "Pkg '$pkg' not found while reading version name!")
    null
}

fun PackageManager.getPackageProvidersWithMetaData(pkg: String) = try {
    this.getPackageInfoCompat(pkg, PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA)
} catch (e: PackageManager.NameNotFoundException) {
    MTLog.d(LOG_TAG, "Pkg '$pkg' not found while reading providers metadata!")
    null
}

@WorkerThread
@Deprecated("NOT efficient")
@SuppressLint("QueryPermissionsNeeded")
fun PackageManager.getAllInstalledProvidersWithMetaData(): Collection<PackageInfo> = try {
    this.getInstalledPackagesCompat(PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA)
} catch (e: Exception) {
    MTLog.w(LOG_TAG, e, "Error while reading installed providers w/ meta-data!") // #Android5 #Android6
    emptySet()
}

fun PackageManager.getInstalledProvidersWithMetaData(pkg: String): Collection<ProviderInfo>? {
    return getPackageProvidersWithMetaData(pkg)
        ?.providers
        ?.toSet()
}

fun PackageManager.getInstalledProviderWithMetaData(pkg: String, providerAuthority: String): ProviderInfo? =
    getInstalledProvidersWithMetaData(pkg)
        ?.singleOrNull { providerInfo ->
            providerInfo.authority == providerAuthority
        }
