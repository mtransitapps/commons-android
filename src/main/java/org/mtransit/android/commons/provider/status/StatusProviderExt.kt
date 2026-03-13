package org.mtransit.android.commons.provider.status

import android.net.Uri
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.data.POIStatus

@JvmOverloads
fun <P : StatusProviderContract> P.getCachedStatusS(
    targetUUID: String,
    tripIds: List<String>? = null
) = getCachedStatusS(listOf(targetUUID), tripIds)

@JvmOverloads
fun <P : StatusProviderContract> P.getCachedStatusS(
    targetUUIDs: Collection<String>,
    @Suppress("unused") tripIds: List<String>? = null
): POIStatus? {
    return StatusProvider.getCachedStatusS(
        this,
        this.contentUri,
        buildString {
            append(SqlUtils.getWhereInString(StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID, targetUUIDs))
        }
    )
}

private val StatusProviderContract.contentUri: Uri
    get() = Uri.withAppendedPath(this.authorityUri, StatusProviderContract.STATUS_PATH)

private val StatusProviderContract.dbTableName: String
    get() = this.statusDbTableName
