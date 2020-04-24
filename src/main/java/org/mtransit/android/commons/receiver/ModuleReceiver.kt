package org.mtransit.android.commons.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.Build
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.R
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.UriUtils
import org.mtransit.android.commons.provider.AgencyProviderContract

class ModuleReceiver : BroadcastReceiver(), MTLog.Loggable {

    companion object {
        @Suppress("DEPRECATION")
        private val ACTIONS_PACKAGE =
            listOf(
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_DATA_CLEARED,
                Intent.ACTION_PACKAGE_FIRST_LAUNCH,
                Intent.ACTION_PACKAGE_FULLY_REMOVED,
                Intent.ACTION_PACKAGE_INSTALL,
                Intent.ACTION_PACKAGE_NEEDS_VERIFICATION,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_REPLACED,
                Intent.ACTION_PACKAGE_RESTARTED
            ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                listOf(Intent.ACTION_PACKAGE_VERIFIED)
            } else {
                emptyList()
            }
        private val ACTIONS_MY_PACKAGE =
            listOf(
                Intent.ACTION_MY_PACKAGE_REPLACED
            ) + if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                listOf(
                    Intent.ACTION_MY_PACKAGE_SUSPENDED,
                    Intent.ACTION_MY_PACKAGE_UNSUSPENDED
                )
            } else {
                emptyList()
            }
        private val ACTIONS_SUPPORTED = ACTIONS_PACKAGE + ACTIONS_MY_PACKAGE
    }

    override fun getLogTag(): String {
        return ModuleReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null) {
            MTLog.w(this, "Module broadcast receiver with null context ignored!")
            return
        }
        if (intent == null) {
            MTLog.w(this, "Module broadcast receiver with null intent ignored!")
            return
        }
        val action = intent.action
        if (!ACTIONS_SUPPORTED.contains(action)) {
            MTLog.w(this, "Module broadcast receiver with unexpected action '$action' ignored!")
            return
        }
        val isMyPackage: Boolean = isMyPackage(context, intent)
        if (!isMyPackage) {
            MTLog.d(this, "Module broadcast receiver for another package '$intent' ignored.")
            return
        }
        MTLog.i(this, "Received broadcast $action for ${context.packageName}.")
        ping(context)
    }

    private fun ping(context: Context) {
        val agencyProviderMetaData = context.getString(R.string.agency_provider)
        val agencyProvider =
            PackageManagerUtils.findContentProvidersWithMetaData(context, context.packageName)
                ?.first { provider ->
                    agencyProviderMetaData == provider.metaData?.getString(agencyProviderMetaData)
                } ?: return
        MTLog.i(this, "Ping: ${agencyProvider.authority}")
        ping(context, agencyProvider)
    }

    private fun ping(
        context: Context,
        agencyProvider: ProviderInfo
    ) {
        val authorityUri = UriUtils.newContentUri(agencyProvider.authority)
        var cursor: Cursor? = null
        try {
            val pingUri = Uri.withAppendedPath(authorityUri, AgencyProviderContract.PING_PATH)
            cursor = context.contentResolver.query(pingUri, null, null, null, null)
        } catch (e: Exception) {
            MTLog.w(this, e, "Error!")
        } finally {
            SqlUtils.closeQuietly(cursor)
        }
    }

    private fun isMyPackage(context: Context, intent: Intent): Boolean {
        if (ACTIONS_MY_PACKAGE.contains(intent.action)) {
            return true
        }
        if (ACTIONS_PACKAGE.contains(intent.action)) {
            return context.packageName == intent.data?.schemeSpecificPart
        }
        MTLog.w(this, "Unexpected intent '$intent'!")
        return false
    }
}