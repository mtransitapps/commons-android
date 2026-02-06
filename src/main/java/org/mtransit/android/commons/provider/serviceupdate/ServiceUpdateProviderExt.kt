package org.mtransit.android.commons.provider.serviceupdate

import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import org.mtransit.android.commons.MTLog
import org.mtransit.android.commons.SqlUtils
import org.mtransit.android.commons.data.ServiceUpdate
import org.mtransit.commons.FeatureFlags

private val LOG_TAG: String = ServiceUpdateProvider::class.java.simpleName

@JvmOverloads
fun <P : ServiceUpdateProviderContract> P.getCachedServiceUpdatesS(
    targetUUID: String,
    tripIds: List<String>? = null
) = getCachedServiceUpdatesS(listOf(targetUUID), tripIds)

@JvmOverloads
fun <P : ServiceUpdateProviderContract> P.getCachedServiceUpdatesS(
    targetUUIDs: Collection<String>,
    tripIds: List<String>? = null
): List<ServiceUpdate>? {
    return getCachedServiceUpdatesS(
        this.contentUri,
        buildString {
            append(SqlUtils.getWhereInString(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TARGET_UUID, targetUUIDs))
            append(SqlUtils.AND)
            SqlUtils.getWhereEqualsString(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LANGUAGE, serviceUpdateLanguage)
            if (FeatureFlags.F_USE_TRIP_IS_FOR_SERVICE_UPDATES) {
                tripIds?.takeIf { it.isNotEmpty() }?.let {
                    append(SqlUtils.AND)
                    append(
                        SqlUtils.getWhereGroup(
                            SqlUtils.OR,
                            SqlUtils.getWhereInString(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TARGET_TRIP_ID, it),
                            SqlUtils.getWhereColumnIsNull(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TARGET_TRIP_ID),
                        )
                    )
                }
            }
        }
    )
}

private fun <P : ServiceUpdateProviderContract> P.getCachedServiceUpdatesS(
    @Suppress("unused") uri: Uri?,
    selection: String?,
): List<ServiceUpdate>? =
    try {
        SQLiteQueryBuilder()
            .apply {
                tables = dbTableName
                projectionMap = ServiceUpdateProvider.SERVICE_UPDATE_PROJECTION_MAP
            }.query(
                getReadDB(), ServiceUpdateProviderContract.PROJECTION_SERVICE_UPDATE, selection, null, null,
                null, null, null
            ).use { cursor ->
                buildList {
                    if (cursor != null && cursor.count > 0) {
                        if (cursor.moveToFirst()) {
                            do {
                                add(ServiceUpdate.fromCursor(cursor))
                            } while (cursor.moveToNext())
                        }
                    }
                }
            }
    } catch (e: Exception) {
        MTLog.w(LOG_TAG, e, "Error!")
        null
    }

private val ServiceUpdateProviderContract.contentUri: Uri
    get() = Uri.withAppendedPath(this.authorityUri, ServiceUpdateProviderContract.SERVICE_UPDATE_PATH)

private val ServiceUpdateProviderContract.dbTableName: String
    get() = this.serviceUpdateDbTableName
