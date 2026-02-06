package org.mtransit.android.commons.provider.serviceupdate;

import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.provider.common.ContentProviderConstants;
import org.mtransit.android.commons.provider.common.MTContentProvider;
import org.mtransit.commons.CollectionUtils;

import java.util.Iterator;
import java.util.List;

public abstract class ServiceUpdateProvider extends MTContentProvider implements ServiceUpdateProviderContract {

	private static final String LOG_TAG = ServiceUpdateProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static void append(@NonNull UriMatcher uriMatcher, @NonNull String authority) {
		uriMatcher.addURI(authority, ServiceUpdateProviderContract.PING_PATH, ContentProviderConstants.PING);
		uriMatcher.addURI(authority, ServiceUpdateProviderContract.SERVICE_UPDATE_PATH, ContentProviderConstants.SERVICE_UPDATE);
	}

	// @formatter:off
	public static final ArrayMap<String, String> SERVICE_UPDATE_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_ID, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_ID) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_TARGET_UUID, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TARGET_UUID) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_TARGET_TRIP_ID, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TARGET_TRIP_ID) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_LAST_UPDATE, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LAST_UPDATE) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_SEVERITY, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SEVERITY) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_TEXT, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TEXT) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_TEXT_HTML, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TEXT_HTML) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_LANGUAGE, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LANGUAGE) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_ORIGINAL_ID, Columns.T_SERVICE_UPDATE_K_ORIGINAL_ID)
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_SOURCE_LABEL, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_LABEL) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_SOURCE_ID, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_ID) //
			.build();
	// @formatter:on

	@Nullable
	public static Cursor queryS(@NonNull ServiceUpdateProviderContract provider, @NonNull Uri uri, @Nullable String selection) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		case ContentProviderConstants.SERVICE_UPDATE:
			return getServiceUpdates(provider, selection);
		default:
			return null; // not processed
		}
	}

	@Nullable
	public static String getTypeS(@NonNull ServiceUpdateProviderContract provider, @NonNull Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.SERVICE_UPDATE:
			return StringUtils.EMPTY; // empty string = processed
		default:
			return null; // not processed
		}
	}

	private static Cursor getServiceUpdates(ServiceUpdateProviderContract provider, String selection) {
		final ServiceUpdateProviderContract.Filter serviceUpdateFilter = ServiceUpdateProviderContract.Filter.fromJSONString(selection);
		if (serviceUpdateFilter == null) {
			MTLog.w(LOG_TAG, "Error while parsing service update filter! (%s)", selection);
			return getServiceUpdateCursor(null);
		}
		final long nowInMs = TimeUtils.currentTimeMillis();
		final List<ServiceUpdate> cachedServiceUpdates = provider.getCachedServiceUpdates(serviceUpdateFilter);
		boolean purgeNecessary = false;
		if (cachedServiceUpdates != null) {
			Iterator<ServiceUpdate> it = cachedServiceUpdates.iterator();
			while (it.hasNext()) {
				ServiceUpdate cachedServiceUpdate = it.next();
				if (cachedServiceUpdate.getLastUpdateInMs() + provider.getServiceUpdateMaxValidityInMs() < nowInMs) {
					it.remove();
					purgeNecessary = true;
				}
			}
		}
		if (purgeNecessary) {
			provider.purgeUselessCachedServiceUpdates();
		}
		if (cachedServiceUpdates != null) {
			Iterator<ServiceUpdate> it = cachedServiceUpdates.iterator();
			while (it.hasNext()) {
				ServiceUpdate cachedServiceUpdate = it.next();
				if (!cachedServiceUpdate.isUseful()) {
					if (cachedServiceUpdate.getId() != null) {
						provider.deleteCachedServiceUpdate(cachedServiceUpdate.getId());
					}
					it.remove();
				}
			}
		}
		if (serviceUpdateFilter.isCacheOnlyOrDefault()) {
			if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
				MTLog.w(LOG_TAG, "getServiceUpdates() > No useful cache found!");
			}
			return getServiceUpdateCursor(cachedServiceUpdates);
		}
		long cacheValidityInMs = provider.getServiceUpdateValidityInMs(serviceUpdateFilter.isInFocusOrDefault());
		Long filterCacheValidityInMs = serviceUpdateFilter.getCacheValidityInMsOrNull();
		if (filterCacheValidityInMs != null
				&& filterCacheValidityInMs > provider.getMinDurationBetweenServiceUpdateRefreshInMs(serviceUpdateFilter.isInFocusOrDefault())) {
			cacheValidityInMs = filterCacheValidityInMs;
		}
		boolean loadNewServiceUpdates = false;
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			loadNewServiceUpdates = true;
		} else if (cachedServiceUpdates != null) {
			for (ServiceUpdate cachedServiceUpdate : cachedServiceUpdates) {
				if (cachedServiceUpdate.getLastUpdateInMs() + cacheValidityInMs < nowInMs) {
					loadNewServiceUpdates = true;
					break;
				}
			}
		}
		if (loadNewServiceUpdates) {
			final List<ServiceUpdate> newServiceUpdates = provider.getNewServiceUpdates(serviceUpdateFilter);
			if (CollectionUtils.getSize(newServiceUpdates) != 0) {
				return getServiceUpdateCursor(newServiceUpdates);
			}
		}
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			MTLog.w(LOG_TAG, "getServiceUpdates() > no cache & no data from provider for %s!", serviceUpdateFilter.getUUID());
		}
		return getServiceUpdateCursor(cachedServiceUpdates);
	}

	@NonNull
	private static Cursor getServiceUpdateCursor(@Nullable List<ServiceUpdate> serviceUpdates) {
		if (serviceUpdates == null) {
			return ContentProviderConstants.EMPTY_CURSOR;
		}
		MatrixCursor matrixCursor = new MatrixCursor(ServiceUpdateProviderContract.PROJECTION_SERVICE_UPDATE);
		for (ServiceUpdate serviceUpdate : serviceUpdates) {
			matrixCursor.addRow(serviceUpdate.getCursorRow());
		}
		return matrixCursor;
	}

	@SuppressWarnings("UnusedReturnValue")
	public static synchronized int cacheServiceUpdatesS(@NonNull ServiceUpdateProviderContract provider, @Nullable List<ServiceUpdate> newServiceUpdates) {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = provider.getWriteDB();
			db.beginTransaction(); // start the transaction
			if (newServiceUpdates != null) {
				for (ServiceUpdate serviceUpdate : newServiceUpdates) {
					long rowId = db
							.insert(provider.getServiceUpdateDbTableName(), ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_ID, serviceUpdate.toContentValues());
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

	@SuppressWarnings("unused")
	public static void cacheServiceUpdateS(@NonNull ServiceUpdateProviderContract provider, @NonNull ServiceUpdate newServiceUpdate) {
		try {
			provider.getWriteDB()
					.insert(provider.getServiceUpdateDbTableName(), ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_ID, newServiceUpdate.toContentValues());
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while inserting '%s' into cache!", newServiceUpdate);
		}
	}

	public static boolean deleteCachedServiceUpdate(@NonNull ServiceUpdateProviderContract provider, @NonNull Integer serviceUpdateId) {
		final String selection = SqlUtils.getWhereEquals(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_ID, serviceUpdateId);
		int deletedRows = 0;
		try {
			deletedRows = provider.getWriteDB().delete(provider.getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while deleting cached service update '%s'!", serviceUpdateId);
		}
		return deletedRows > 0;
	}

	public static boolean deleteCachedServiceUpdate(@NonNull ServiceUpdateProviderContract provider, @NonNull String targetUUID, @NonNull String sourceId) {
		if (TextUtils.isEmpty(targetUUID) || TextUtils.isEmpty(sourceId)) {
			return false;
		}
		String selection = //
				SqlUtils.getWhereEqualsString(Columns.T_SERVICE_UPDATE_K_TARGET_UUID, targetUUID) + //
						SqlUtils.AND + //
						SqlUtils.getWhereEqualsString(Columns.T_SERVICE_UPDATE_K_SOURCE_ID, sourceId) //
				;
		int deletedRows = 0;
		try {
			deletedRows = provider.getWriteDB().delete(provider.getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while deleting cached service update(s) target '%s' source '%s' !", targetUUID, sourceId);
		}
		return deletedRows > 0;
	}

	public static boolean purgeUselessCachedServiceUpdates(@NonNull ServiceUpdateProviderContract provider) {
		long oldestLastUpdate = TimeUtils.currentTimeMillis() - provider.getServiceUpdateMaxValidityInMs();
		String selection = SqlUtils.getWhereInferior(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LAST_UPDATE, oldestLastUpdate);
		int deletedRows = 0;
		try {
			deletedRows = provider.getWriteDB().delete(provider.getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while deleting cached service updates!");
		}
		return deletedRows > 0;
	}
}
