package org.mtransit.android.commons.provider.status

import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.data.POIStatus

private val LOG_TAG: String = StatusProvider::class.java.simpleName

@JvmOverloads
fun <P : StatusProviderContract> P.getCachedStatusS(
    targetUUID: String,
    tripIds: List<String>? = null
) = getCachedStatusS(listOf(targetUUID), tripIds)

@JvmOverloads
fun <P : StatusProviderContract> P.getCachedStatusS(
    targetUUIDs: Collection<String>,
    @Suppress("unused") tripIds: List<String>? = null
): List<POIStatus>? {
    return getCachedStatusS(
        this.contentUri,
        buildString {
            append(SqlUtils.getWhereInString(StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID, targetUUIDs))
            // TODO ? if (FeatureFlags.F_USE_TRIP_IS_FOR_STATUSES) {
            //     tripIds?.takeIf { it.isNotEmpty() }?.let {
            //         append(SqlUtils.AND)
            //         append(
            //             SqlUtils.getWhereGroup(
            //                 SqlUtils.OR,
            //                 SqlUtils.getWhereInString(StatusProviderContract.Columns.T_STATUS_K_TARGET_TRIP_ID, it),
            //                 SqlUtils.getWhereColumnIsNull(StatusProviderContract.Columns.T_STATUS_K_TARGET_TRIP_ID),
            //             )
            //         )
            //     }
            // }
        }
    )
}

private fun <P : StatusProviderContract> P.getCachedStatusS(
    @Suppress("unused") uri: Uri?,
    selection: String?,
): List<POIStatus>? =
    try {
        SQLiteQueryBuilder()
            .apply {
                tables = dbTableName
                projectionMap = StatusProvider.STATUS_PROJECTION_MAP
            }.query(
                getReadDB(), StatusProviderContract.PROJECTION_STATUS, selection, null, null, null, null, null
            ).use { cursor ->
                buildList {
                    if (cursor != null && cursor.count > 0) {
                        if (cursor.moveToFirst()) {
                            do {
                                add(POIStatus.fromCursor(cursor))
                            } while (cursor.moveToNext())
                        }
                    }
                }
            }
    } catch (e: Exception) {
        MTLog.w(LOG_TAG, e, "Error!")
        null
    }

private val StatusProviderContract.contentUri: Uri
    get() = Uri.withAppendedPath(this.authorityUri, StatusProviderContract.STATUS_PATH)

private val StatusProviderContract.dbTableName: String
    get() = this.statusDbTableName
