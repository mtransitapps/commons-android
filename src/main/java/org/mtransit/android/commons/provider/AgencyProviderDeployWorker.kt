package org.mtransit.android.commons.provider

import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.PackageManagerUtils
import org.mtransit.android.commons.R
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.UriUtils

class AgencyProviderDeployWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), MTLog.Loggable {

    companion object {
        @JvmStatic
        fun makeWorkRequest() = OneTimeWorkRequest.Builder(AgencyProviderDeployWorker::class.java)
            .addTag(WORK_MANAGER_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build()
            )
            .build()

        val LOG_TAG: String = AgencyProviderDeployWorker::class.java.simpleName

        const val WORK_MANAGER_TAG = "agency_deploy"
        const val FROM_WORKER = "from_worker"
    }

    override fun getLogTag(): String = LOG_TAG

    override suspend fun doWork() = withContext(Dispatchers.IO) {
        val agencyProviderMetaData = context.getString(R.string.agency_provider)
        val agencyProvider =
            PackageManagerUtils.findContentProvidersWithMetaData(context, context.packageName)
                ?.first { provider ->
                    agencyProviderMetaData == provider.metaData?.getString(agencyProviderMetaData)
                } ?: return@withContext Result.failure(Data.Builder().putString("reason", "no agency provider!").build())
        ping(agencyProvider)
        Result.success()
    }

    private fun ping(agencyProvider: ProviderInfo) {
        val authorityUri = UriUtils.newContentUri(agencyProvider.authority)
        var cursor: Cursor? = null
        try {
            val pingUri = Uri.withAppendedPath(authorityUri, AgencyProviderContract.PING_PATH)
            val selection = FROM_WORKER
            MTLog.i(this, "Ping: ${agencyProvider.authority} ($selection)")
            cursor = context.contentResolver.query(pingUri, null, selection, null, null)
        } catch (e: Exception) {
            MTLog.w(this, e, "Error!")
        } finally {
            SqlUtils.closeQuietly(cursor)
        }
    }
}