package org.mtransit.android.commons.provider;

import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.AppStatus;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.commons.sql.SQLCreateBuilder;

import java.util.Collection;

public abstract class StatusProvider extends MTContentProvider implements StatusProviderContract {

	private static final String LOG_TAG = StatusProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static void append(@NonNull UriMatcher uriMatcher, @NonNull String authority) {
		uriMatcher.addURI(authority, StatusProviderContract.PING_PATH, ContentProviderConstants.PING);
		uriMatcher.addURI(authority, StatusProviderContract.STATUS_PATH, ContentProviderConstants.STATUS);
	}

	// @formatter:off
	public static final ArrayMap<String, String> STATUS_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
			.appendTableColumn(StatusDbHelper.T_STATUS, StatusDbHelper.T_STATUS_K_ID, StatusProviderContract.Columns.T_STATUS_K_ID)
			.appendTableColumn(StatusDbHelper.T_STATUS, StatusDbHelper.T_STATUS_K_TYPE, StatusProviderContract.Columns.T_STATUS_K_TYPE)
			.appendTableColumn(StatusDbHelper.T_STATUS, StatusDbHelper.T_STATUS_K_TARGET_UUID, StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID) //
			.appendTableColumn(StatusDbHelper.T_STATUS, StatusDbHelper.T_STATUS_K_LAST_UPDATE, StatusProviderContract.Columns.T_STATUS_K_LAST_UPDATE) //
			.appendTableColumn(StatusDbHelper.T_STATUS, StatusDbHelper.T_STATUS_K_MAX_VALIDITY, StatusProviderContract.Columns.T_STATUS_K_MAX_VALIDITY_IN_MS) //
			.appendTableColumn(StatusDbHelper.T_STATUS, StatusDbHelper.T_STATUS_K_READ_FROM_SOURCE_AT_IN_MS, StatusProviderContract.Columns.T_STATUS_K_READ_FROM_SOURCE_AT_IN_MS) //
			.appendTableColumn(StatusDbHelper.T_STATUS, StatusDbHelper.T_STATUS_K_EXTRAS, StatusProviderContract.Columns.T_STATUS_K_EXTRAS) //
			.build();
	// @formatter:on

	@Nullable
	public static Cursor queryS(@NonNull StatusProviderContract provider, @NonNull Uri uri, @Nullable String selection) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
			provider.ping();
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		case ContentProviderConstants.STATUS:
			return getStatus(provider, selection);
		default:
			return null; // not processed
		}
	}

	@Nullable
	public static String getSortOrderS(@NonNull StatusProviderContract provider, @NonNull Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.STATUS:
			return StringUtils.EMPTY; // empty string = processed
		default:
			return null; // not processed
		}
	}

	@Nullable
	public static String getTypeS(@NonNull StatusProviderContract provider, @NonNull Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.STATUS:
			return StringUtils.EMPTY; // empty string = processed
		default:
			return null; // not processed
		}
	}

	@NonNull
	private static Cursor getStatus(@NonNull StatusProviderContract provider, @Nullable String selection) {
		final StatusProviderContract.Filter statusFilter = extractStatusFilter(selection);
		if (statusFilter == null) {
			MTLog.w(LOG_TAG, "Error while parsing status filter! (%s)", selection);
			return getStatusCursor(null);
		}
		final long now = TimeUtils.currentTimeMillis();
		// 1 - check if cached status available and usable (< max validity)
		POIStatus cachedStatus = provider.getCachedStatus(statusFilter);
		if (cachedStatus != null && cachedStatus.getLastUpdateInMs() + provider.getStatusMaxValidityInMs() < now) {
			provider.purgeUselessCachedStatuses(); // cache too old => delete
			cachedStatus = null; // do not use cache
		}
		if (cachedStatus != null && !cachedStatus.isUseful() && !cachedStatus.isNoData()) {
			if (cachedStatus.getId() != null) {
				provider.deleteCachedStatus(cachedStatus.getId()); // cache not useful => delete
			}
			cachedStatus = null; // do not use cache
		}
		// 2 - check if using cache only
		if (statusFilter.isCacheOnlyOrDefault()) {
			return getStatusCursor(cachedStatus);
		}
		// 3 - check if usable cache still valid (or if it could be refreshed)
		long cacheValidityInMs = provider.getStatusValidityInMs(statusFilter.isInFocusOrDefault());
		Long filterCacheValidityInMs = statusFilter.getCacheValidityInMsOrNull();
		if (filterCacheValidityInMs != null && filterCacheValidityInMs > provider.getMinDurationBetweenRefreshInMs(statusFilter.isInFocusOrDefault())) {
			cacheValidityInMs = filterCacheValidityInMs;
		}
		if (cachedStatus == null || cachedStatus.getLastUpdateInMs() + cacheValidityInMs < now) {
			final POIStatus newStatus = provider.getNewStatus(statusFilter); // try to refresh
			if (newStatus != null) {
				provider.cacheStatus(newStatus);
				return getStatusCursor(newStatus);
			}
		}
		// 4 - cache is still valid OR no new status returned from provider
		return getStatusCursor(cachedStatus);
	}

	@Nullable
	private static StatusProviderContract.Filter extractStatusFilter(@Nullable String selection) {
		int type = StatusProviderContract.Filter.getTypeFromJSONString(selection);
		StatusProviderContract.Filter statusFilter;
		switch (type) {
		case POI.ITEM_STATUS_TYPE_NONE:
			statusFilter = null;
			break;
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			statusFilter = Schedule.ScheduleStatusFilter.fromJSONString(selection);
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			statusFilter = AvailabilityPercent.AvailabilityPercentStatusFilter.fromJSONString(selection);
			break;
		case POI.ITEM_STATUS_TYPE_APP:
			statusFilter = AppStatus.AppStatusFilter.fromJSONString(selection);
			break;
		default:
			MTLog.w(LOG_TAG, "Unexpected status filter type '%s'!", type);
			statusFilter = null;
		}
		return statusFilter;
	}

	@NonNull
	public static Cursor getStatusCursor(@Nullable POIStatus status) {
		if (status == null) {
			return ContentProviderConstants.EMPTY_CURSOR;
		}
		return status.toCursor();
	}

	@NonNull
	public static Uri getStatusContentUri(@NonNull StatusProviderContract provider) {
		return Uri.withAppendedPath(provider.getAuthorityUri(), StatusProviderContract.STATUS_PATH);
	}

	@SuppressWarnings("UnusedReturnValue")
	public static synchronized int cacheAllStatusesBulkLockDB(@NonNull StatusProviderContract provider, @Nullable Collection<POIStatus> newStatuses) {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = provider.getWriteDB();
			db.beginTransaction(); // start the transaction
			if (newStatuses != null) {
				for (POIStatus status : newStatuses) {
					long rowId = db.insert(provider.getStatusDbTableName(), StatusDbHelper.T_STATUS_K_ID, status.toContentValues());
					if (rowId > 0) {
						affectedRows++;
					}
				}
			}
			db.setTransactionSuccessful(); // mark the transaction as successful
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "ERROR while applying batch update to the database!");
		} finally {
			SqlUtils.endTransaction(db);
		}
		return affectedRows;
	}

	public static void cacheStatusS(@NonNull StatusProviderContract provider, @NonNull POIStatus newStatus) {
		try {
			provider.getWriteDB().insert(provider.getStatusDbTableName(), StatusDbHelper.T_STATUS_K_ID, newStatus.toContentValues());
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while inserting '%s' into cache!", newStatus);
		}
	}

	private static final String STATUS_LIMIT = "1";
	private static final String STATUS_SORT_ORDER = SqlUtils.getSortOrderDescending(Columns.T_STATUS_K_LAST_UPDATE);

	@Nullable
	private static POIStatus getCachedStatusS(@NonNull StatusProviderContract provider,
											  @SuppressWarnings("unused") Uri uri,
											  String selection) {
		POIStatus cache = null;
		Cursor cursor = null;
		try {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getStatusDbTableName());
			qb.setProjectionMap(STATUS_PROJECTION_MAP);
			cursor = qb.query(provider.getReadDB(), PROJECTION_STATUS, selection, null, null, null, STATUS_SORT_ORDER, STATUS_LIMIT);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					final int type = POIStatus.getTypeFromCursor(cursor);
					switch (type) {
					case POI.ITEM_STATUS_TYPE_NONE:
						break;
					case POI.ITEM_STATUS_TYPE_SCHEDULE:
						cache = Schedule.fromCursorWithExtra(cursor);
						break;
					case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
						cache = AvailabilityPercent.fromCursorWithExtra(cursor);
						break;
					case POI.ITEM_STATUS_TYPE_APP:
						cache = AppStatus.fromCursorWithExtra(cursor);
						break;
					default:
						MTLog.w(LOG_TAG, "Status type '%s' not expected", type);
						break;
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
		return cache;
	}

	@Nullable
	public static POIStatus getCachedStatusS(@NonNull StatusProviderContract provider, @NonNull String targetUUID) {
		return getCachedStatusS(
				provider,
				getStatusContentUri(provider),
				SqlUtils.getWhereEqualsString(StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID, targetUUID)
		);
	}

	public static boolean deleteCachedStatus(@NonNull StatusProviderContract provider, int cachedStatusId) {
		String selection = SqlUtils.getWhereEquals(StatusProviderContract.Columns.T_STATUS_K_ID, cachedStatusId);
		int deletedRows = 0;
		try {
			deletedRows = provider.getWriteDB().delete(provider.getStatusDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while deleting cached statuses!");
		}
		return deletedRows > 0;
	}

	public static int deleteCachedStatus(@NonNull StatusProviderContract provider, @Nullable Collection<String> targetUUIDs) {
		if (targetUUIDs == null || targetUUIDs.size() == 0) {
			return 0;
		}
		String selection = SqlUtils.getWhereInString(StatusProviderContract.Columns.T_STATUS_K_TARGET_UUID, targetUUIDs);
		int deletedRows = 0;
		try {
			deletedRows = provider.getWriteDB().delete(provider.getStatusDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while deleting cached statuses!");
		}
		return deletedRows;
	}

	public static boolean purgeUselessCachedStatuses(@NonNull StatusProviderContract provider) {
		int type = provider.getStatusType();
		long oldestLastUpdate = TimeUtils.currentTimeMillis() - provider.getStatusMaxValidityInMs();
		String selection = SqlUtils.getWhereEquals(Columns.T_STATUS_K_TYPE, type) + //
				SqlUtils.AND + //
				SqlUtils.getWhereInferior(Columns.T_STATUS_K_LAST_UPDATE, oldestLastUpdate);
		int deletedRows = 0;
		try {
			deletedRows = provider.getWriteDB().delete(provider.getStatusDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while deleting cached statuses!");
		}
		return deletedRows > 0;
	}

	public static abstract class StatusDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = StatusDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		public static final String T_STATUS = "status";
		static final String T_STATUS_K_ID = BaseColumns._ID;
		static final String T_STATUS_K_TYPE = "type";
		static final String T_STATUS_K_TARGET_UUID = "target";
		static final String T_STATUS_K_LAST_UPDATE = "last_update";
		static final String T_STATUS_K_MAX_VALIDITY = "max_validity";
		static final String T_STATUS_K_READ_FROM_SOURCE_AT_IN_MS = "read_from_source_at";
		static final String T_STATUS_K_EXTRAS = "extras";
		@SuppressWarnings("unused")
		public static final String T_STATUS_SQL_CREATE = getSqlCreateBuilder(T_STATUS).build();
		@SuppressWarnings("unused")
		public static final String T_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STATUS);

		public StatusDbHelper(@NonNull Context context, @NonNull String dbName, int dbVersion) {
			super(context, dbName, null, dbVersion);
		}

		@SuppressWarnings("unused")
		@NonNull
		public abstract String getDbName();

		@SuppressWarnings("unused")
		@NonNull
		public static String getFkColumnName(@NonNull String columnName) {
			return "fk" + "_" + columnName;
		}

		@NonNull
		public static SQLCreateBuilder getSqlCreateBuilder(@NonNull String table) {
			return SQLCreateBuilder.getNew(table) //
					.appendColumn(T_STATUS_K_ID, SqlUtils.INT_PK) //
					.appendColumn(T_STATUS_K_TYPE, SqlUtils.INT) //
					.appendColumn(T_STATUS_K_TARGET_UUID, SqlUtils.TXT) //
					.appendColumn(T_STATUS_K_LAST_UPDATE, SqlUtils.INT) //
					.appendColumn(T_STATUS_K_MAX_VALIDITY, SqlUtils.INT) //
					.appendColumn(T_STATUS_K_READ_FROM_SOURCE_AT_IN_MS, SqlUtils.INT) //
					.appendColumn(T_STATUS_K_EXTRAS, SqlUtils.TXT) //
					;
		}
	}
}
