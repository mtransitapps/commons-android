package org.mtransit.android.commons.provider;

import java.util.HashMap;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Schedule;

import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

public abstract class StatusProvider extends MTContentProvider implements StatusProviderContract {

	private static final String TAG = StatusProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static void append(UriMatcher uriMatcher, String authority) {
		uriMatcher.addURI(authority, "ping", ContentProviderConstants.PING);
		uriMatcher.addURI(authority, STATUS_CONTENT_DIRECTORY, ContentProviderConstants.STATUS);
	}

	public static final String STATUS_CONTENT_DIRECTORY = "status";

	public static final String[] PROJECTION_STATUS = new String[] { StatusColumns.T_STATUS_K_ID, StatusColumns.T_STATUS_K_TYPE,
			StatusColumns.T_STATUS_K_TARGET_UUID, StatusColumns.T_STATUS_K_LAST_UDPDATE, StatusColumns.T_STATUS_K_MAX_VALIDITY_IN_MS,
			StatusColumns.T_STATUS_K_EXTRAS };

	public static final HashMap<String, String> STATUS_PROJECTION_MAP;
	static {
		HashMap<String, String> map;

		map = new HashMap<String, String>();
		map.put(StatusColumns.T_STATUS_K_ID, StatusDbHelper.T_STATUS + "." + StatusDbHelper.T_STATUS_K_ID + " AS " + StatusColumns.T_STATUS_K_ID);
		map.put(StatusColumns.T_STATUS_K_TYPE, StatusDbHelper.T_STATUS + "." + StatusDbHelper.T_STATUS_K_TYPE + " AS " + StatusColumns.T_STATUS_K_TYPE);
		map.put(StatusColumns.T_STATUS_K_TARGET_UUID, StatusDbHelper.T_STATUS + "." + StatusDbHelper.T_STATUS_K_TARGET_UUID + " AS "
				+ StatusColumns.T_STATUS_K_TARGET_UUID);
		map.put(StatusColumns.T_STATUS_K_LAST_UDPDATE, StatusDbHelper.T_STATUS + "." + StatusDbHelper.T_STATUS_K_LAST_UPDATE + " AS "
				+ StatusColumns.T_STATUS_K_LAST_UDPDATE);
		map.put(StatusColumns.T_STATUS_K_MAX_VALIDITY_IN_MS, StatusDbHelper.T_STATUS + "." + StatusDbHelper.T_STATUS_K_MAX_VALIDITY + " AS "
				+ StatusColumns.T_STATUS_K_MAX_VALIDITY_IN_MS);
		map.put(StatusColumns.T_STATUS_K_EXTRAS, StatusDbHelper.T_STATUS + "." + StatusDbHelper.T_STATUS_K_EXTRAS + " AS " + StatusColumns.T_STATUS_K_EXTRAS);
		STATUS_PROJECTION_MAP = map;
	}

	public static Cursor queryS(StatusProviderContract provider, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		switch (provider.getURIMATCHER().match(uri)) {
		case ContentProviderConstants.PING:
			provider.ping();
			return new MatrixCursor(new String[] {}); // empty cursor = processed
		case ContentProviderConstants.STATUS:
			return getStatus(provider, selection);
		default:
			return null; // not processed
		}
	}

	public static String getSortOrderS(StatusProviderContract provider, Uri uri) {
		switch (provider.getURIMATCHER().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.STATUS:
			return ""; // empty string = processed
		default:
			return null; // not processed
		}
	}

	public static String getTypeS(StatusProviderContract provider, Uri uri) {
		switch (provider.getURIMATCHER().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.STATUS:
			return ""; // empty string = processed
		default:
			return null; // not processed
		}
	}

	private static Cursor getStatus(StatusProviderContract provider, String selection) {
		final StatusFilter statusFilter = extractStatusFilter(provider, selection);
		if (statusFilter == null) {
			MTLog.w(TAG, "Error while parsing status filter '%s'!", statusFilter);
			return getStatusCursor(null);
		}
		// 1 - check if cached status available and usable (< max validity)
		POIStatus cachedStatus = provider.getCachedStatus(statusFilter.getTargetUUID());
		if (cachedStatus != null && cachedStatus.getLastUpdateInMs() + provider.getStatusMaxValidityInMs() < TimeUtils.currentTimeMillis()) {
			// cache too old => delete
			provider.purgeUselessCachedStatuses();
			// not use cache
			cachedStatus = null;
		}
		// 2 - check if using cache only
		if (statusFilter.isCacheOnlyOrDefault()) {
			return getStatusCursor(cachedStatus);
		}
		// 3 - check if usable cache still valid (or if it could be refreshed)
		long cacheValidity = provider.getStatusValidityInMs();
		if (statusFilter.hasCacheValidityInMs()) {
			long statusFilterCacheValidityInMs = statusFilter.getCacheValidityInMsOrNull().longValue();
			if (statusFilterCacheValidityInMs > provider.getMinDurationBetweenRefreshInMs()) {
				cacheValidity = statusFilterCacheValidityInMs;
			}
		}
		if (cachedStatus == null || cachedStatus.getLastUpdateInMs() + cacheValidity < TimeUtils.currentTimeMillis()) {
			// try to refresh
			final POIStatus newStatus = provider.getNewStatus(statusFilter);
			if (newStatus != null) {
				provider.cacheStatus(newStatus);
				return getStatusCursor(newStatus);
			}
		}
		// 4 - cache is still valid OR no new status returned from provider
		return getStatusCursor(cachedStatus);
	}

	private static StatusFilter extractStatusFilter(StatusProviderContract provider, String selection) {
		final int type = StatusFilter.getTypeFromJSONString(selection);
		final StatusFilter statusFilter;
		switch (type) {
		case POI.ITEM_STATUS_TYPE_SCHEDULE:
			statusFilter = ScheduleStatusFilter.fromJSONString(selection);
			break;
		case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
			statusFilter = AvailabilityPercentStatusFilter.fromJSONString(selection);
			break;
		default:
			MTLog.w(TAG, "Unexpected status filter type '%s'!", type);
			statusFilter = null;
		}
		return statusFilter;
	}

	public static Cursor getStatusCursor(POIStatus status) {
		if (status == null) {
			return new MatrixCursor(new String[] {});
		}
		return status.toCursor();
	}

	public static Uri getStatusContentUri(StatusProviderContract provider) {
		return Uri.withAppendedPath(provider.getAuthorityUri(), STATUS_CONTENT_DIRECTORY);
	}

	public static void cacheStatusS(Context context, StatusProviderContract provider, POIStatus newStatus) {
		SQLiteDatabase db = null;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			db.insert(provider.getStatusDbTableName(), StatusDbHelper.T_STATUS_K_ID, newStatus.toContentValues());
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while inserting '%s' into cache!", newStatus);
		}
	}

	private static POIStatus getCachedStatusS(StatusProviderContract provider, Uri uri, String selection) {
		POIStatus cache = null;
		Cursor cursor = null;
		try {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getStatusDbTableName());
			qb.setProjectionMap(STATUS_PROJECTION_MAP);
			cursor = qb.query(provider.getDBHelper().getReadableDatabase(), PROJECTION_STATUS, selection, null, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					final int type = POIStatus.getTypeFromCursor(cursor);
					switch (type) {
					case POI.ITEM_STATUS_TYPE_SCHEDULE:
						cache = Schedule.fromCursor(cursor);
						break;
					case POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT:
						cache = AvailabilityPercent.fromCursor(cursor);
						break;
					default:
						MTLog.w(TAG, "Status type '%s' not expected");
						break;
					}
				}
			}
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error!");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return cache;
	}

	public static POIStatus getCachedStatusS(StatusProviderContract provider, String targetUUID) {
		Uri uri = getStatusContentUri(provider);
		String selection = new StringBuilder() //
				.append(StatusColumns.T_STATUS_K_TARGET_UUID).append("='").append(targetUUID).append("'") //
				.toString();
		return getCachedStatusS(provider, uri, selection);
	}

	public static boolean purgeUselessCachedStatuses(Context context, StatusProviderContract provider) {
		final int type = provider.getStatusType();
		long oldestLastUpdate = TimeUtils.currentTimeMillis() - provider.getStatusMaxValidityInMs();
		String selection = new StringBuilder() //
				.append(StatusColumns.T_STATUS_K_TYPE).append("=").append(type) //
				.append(" AND ") //
				.append(StatusColumns.T_STATUS_K_LAST_UDPDATE).append(" < ").append(oldestLastUpdate) //
				.toString();
		SQLiteDatabase db = null;
		int deletedRows = 0;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			deletedRows = db.delete(provider.getStatusDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while deleting cached statuses!");
		}
		return deletedRows > 0;
	}
	
	public static class StatusColumns {

		public static final String T_STATUS_K_ID = BaseColumns._ID;
		public static final String T_STATUS_K_TYPE = "type";
		public static final String T_STATUS_K_TARGET_UUID = "target";
		public static final String T_STATUS_K_EXTRAS = "extras";
		public static final String T_STATUS_K_LAST_UDPDATE = "last_update";
		public static final String T_STATUS_K_MAX_VALIDITY_IN_MS = "max_validity";

	}
}
