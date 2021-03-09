package org.mtransit.android.commons.provider;

import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.ServiceUpdate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public abstract class ServiceUpdateProvider extends MTContentProvider implements ServiceUpdateProviderContract {

	private static final String TAG = ServiceUpdateProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	public static void append(UriMatcher uriMatcher, String authority) {
		uriMatcher.addURI(authority, ServiceUpdateProviderContract.PING_PATH, ContentProviderConstants.PING);
		uriMatcher.addURI(authority, ServiceUpdateProviderContract.SERVICE_UPDATE_PATH, ContentProviderConstants.SERVICE_UPDATE);
	}

	// @formatter:off
	public static final ArrayMap<String, String> SERVICE_UPDATE_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew()
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_ID, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_ID) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_TARGET_UUID, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TARGET_UUID) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_LAST_UPDATE, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LAST_UPDATE) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_SEVERITY, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SEVERITY) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_TEXT, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TEXT) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_TEXT_HTML, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TEXT_HTML) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_LANGUAGE, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LANGUAGE) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_SOURCE_LABEL, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_LABEL) //
			.appendTableColumn(ServiceUpdateDbHelper.T_SERVICE_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_SOURCE_ID, ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_ID) //
			.build();
	// @formatter:on

	public static Cursor queryS(ServiceUpdateProviderContract provider, Uri uri, String selection) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
			provider.ping();
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		case ContentProviderConstants.SERVICE_UPDATE:
			return getServiceUpdates(provider, selection);
		default:
			return null; // not processed
		}
	}

	public static String getTypeS(ServiceUpdateProviderContract provider, Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.SERVICE_UPDATE:
			return StringUtils.EMPTY; // empty string = processed
		default:
			return null; // not processed
		}
	}

	private static Cursor getServiceUpdates(ServiceUpdateProviderContract provider, String selection) {
		ServiceUpdateProviderContract.Filter serviceUpdateFilter = ServiceUpdateProviderContract.Filter.fromJSONString(selection);
		if (serviceUpdateFilter == null) {
			MTLog.w(TAG, "Error while parsing status filter!");
			return getServiceUpdateCursor(null);
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		ArrayList<ServiceUpdate> cachedServiceUpdates = provider.getCachedServiceUpdates(serviceUpdateFilter);
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
					provider.deleteCachedServiceUpdate(cachedServiceUpdate.getId());
					it.remove();
				}
			}
		}
		if (serviceUpdateFilter.isCacheOnlyOrDefault()) {
			if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
				MTLog.w(TAG, "getServiceUpdates() > No useful cache found!");
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
			ArrayList<ServiceUpdate> newServiceUpdates = provider.getNewServiceUpdates(serviceUpdateFilter);
			if (CollectionUtils.getSize(newServiceUpdates) != 0) {
				return getServiceUpdateCursor(newServiceUpdates);
			}
		}
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			MTLog.w(TAG, "getServiceUpdates() > no cache & no data from provider!");
		}
		return getServiceUpdateCursor(cachedServiceUpdates);
	}

	public static Cursor getServiceUpdateCursor(ArrayList<ServiceUpdate> serviceUpdates) {
		if (serviceUpdates == null) {
			return ContentProviderConstants.EMPTY_CURSOR;
		}
		MatrixCursor matrixCursor = new MatrixCursor(ServiceUpdateProviderContract.PROJECTION_SERVICE_UPDATE);
		for (ServiceUpdate serviceUpdate : serviceUpdates) {
			matrixCursor.addRow(serviceUpdate.getCursorRow());
		}
		return matrixCursor;
	}

	public static synchronized int cacheServiceUpdatesS(@NonNull ServiceUpdateProviderContract provider, @Nullable ArrayList<ServiceUpdate> newServiceUpdates) {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = provider.getDBHelper().getWritableDatabase();
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
			MTLog.w(TAG, e, "ERROR while applying batch update to the database!");
		} finally {
			SqlUtils.endTransaction(db);
		}
		return affectedRows;
	}

	public static void cacheServiceUpdateS(ServiceUpdateProviderContract provider, ServiceUpdate newServiceUpdate) {
		try {
			provider.getDBHelper().getWritableDatabase()
					.insert(provider.getServiceUpdateDbTableName(), ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_ID, newServiceUpdate.toContentValues());
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while inserting '%s' into cache!", newServiceUpdate);
		}
	}

	public static ArrayList<ServiceUpdate> getCachedServiceUpdatesS(ServiceUpdateProviderContract provider, Collection<String> targetUUIDs) {
		Uri uri = getServiceUpdateContentUri(provider);
		String selection = new StringBuilder() //
				.append(SqlUtils.getWhereInString(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TARGET_UUID, targetUUIDs)) //
				.append(SqlUtils.AND) //
				.append(SqlUtils.getWhereEqualsString(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LANGUAGE, provider.getServiceUpdateLanguage()))//
				.toString();
		return getCachedServiceUpdatesS(provider, uri, selection);
	}

	public static ArrayList<ServiceUpdate> getCachedServiceUpdatesS(ServiceUpdateProviderContract provider, String targetUUID) {
		Uri uri = getServiceUpdateContentUri(provider);
		String selection = new StringBuilder() //
				.append(SqlUtils.getWhereEqualsString(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_TARGET_UUID, targetUUID)) //
				.append(SqlUtils.AND) //
				.append(SqlUtils.getWhereEqualsString(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LANGUAGE, provider.getServiceUpdateLanguage()))//
				.toString();
		return getCachedServiceUpdatesS(provider, uri, selection);
	}

	private static ArrayList<ServiceUpdate> getCachedServiceUpdatesS(ServiceUpdateProviderContract provider, Uri uri, String selection) {
		ArrayList<ServiceUpdate> cache = new ArrayList<>();
		Cursor cursor = null;
		try {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getServiceUpdateDbTableName());
			qb.setProjectionMap(SERVICE_UPDATE_PROJECTION_MAP);
			cursor = qb.query(provider.getDBHelper().getReadableDatabase(), ServiceUpdateProviderContract.PROJECTION_SERVICE_UPDATE, selection, null, null,
					null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						cache.add(ServiceUpdate.fromCursor(cursor));
					} while (cursor.moveToNext());
				}
			}
			return cache;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	public static Uri getServiceUpdateContentUri(ServiceUpdateProviderContract provider) {
		return Uri.withAppendedPath(provider.getAuthorityUri(), ServiceUpdateProviderContract.SERVICE_UPDATE_PATH);
	}

	public static boolean deleteCachedServiceUpdate(ServiceUpdateProviderContract provider, Integer serviceUpdateId) {
		if (serviceUpdateId == null) {
			return false;
		}
		String selection = SqlUtils.getWhereEquals(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_ID, serviceUpdateId);
		int deletedRows = 0;
		try {
			deletedRows = provider.getDBHelper().getWritableDatabase().delete(provider.getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while deleting cached service update '%s'!", serviceUpdateId);
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
			deletedRows = provider.getDBHelper().getWritableDatabase().delete(provider.getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while deleting cached service update(s) target '%s' source '%s' !", targetUUID, sourceId);
		}
		return deletedRows > 0;
	}

	public static boolean purgeUselessCachedServiceUpdates(ServiceUpdateProviderContract provider) {
		long oldestLastUpdate = TimeUtils.currentTimeMillis() - provider.getServiceUpdateMaxValidityInMs();
		String selection = SqlUtils.getWhereInferior(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_LAST_UPDATE, oldestLastUpdate);
		int deletedRows = 0;
		try {
			deletedRows = provider.getDBHelper().getWritableDatabase().delete(provider.getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while deleting cached service updates!");
		}
		return deletedRows > 0;
	}

	public static abstract class ServiceUpdateDbHelper extends MTSQLiteOpenHelper {

		public static final String T_SERVICE_UPDATE = "service_update";
		public static final String T_SERVICE_UPDATE_K_ID = BaseColumns._ID;
		public static final String T_SERVICE_UPDATE_K_TARGET_UUID = "target";
		public static final String T_SERVICE_UPDATE_K_LAST_UPDATE = "last_update";
		public static final String T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS = "max_validity";
		public static final String T_SERVICE_UPDATE_K_SEVERITY = "severity";
		public static final String T_SERVICE_UPDATE_K_TEXT = "text";
		public static final String T_SERVICE_UPDATE_K_TEXT_HTML = "text_html";
		public static final String T_SERVICE_UPDATE_K_LANGUAGE = "lang";
		public static final String T_SERVICE_UPDATE_K_SOURCE_LABEL = "source_label";
		public static final String T_SERVICE_UPDATE_K_SOURCE_ID = "source_id";

		public static final String T_SERVICE_UPDATE_SQL_CREATE = getSqlCreateBuilder(T_SERVICE_UPDATE).build();

		public static final String T_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_SERVICE_UPDATE);

		public ServiceUpdateDbHelper(Context context, String dbName, CursorFactory factory, int dbVersion) {
			super(context, dbName, factory, dbVersion);
		}

		public abstract String getDbName();

		public static String getFkColumnName(String columnName) {
			return "fk" + "_" + columnName;
		}

		public static SqlUtils.SQLCreateBuilder getSqlCreateBuilder(String table) {
			return SqlUtils.SQLCreateBuilder.getNew(table) //
					.appendColumn(T_SERVICE_UPDATE_K_ID, SqlUtils.INT_PK_AUTO) //
					.appendColumn(T_SERVICE_UPDATE_K_TARGET_UUID, SqlUtils.TXT) //
					.appendColumn(T_SERVICE_UPDATE_K_LAST_UPDATE, SqlUtils.INT) //
					.appendColumn(T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS, SqlUtils.INT) //
					.appendColumn(T_SERVICE_UPDATE_K_SEVERITY, SqlUtils.INT) //
					.appendColumn(T_SERVICE_UPDATE_K_TEXT, SqlUtils.TXT) //
					.appendColumn(T_SERVICE_UPDATE_K_TEXT_HTML, SqlUtils.TXT) //
					.appendColumn(T_SERVICE_UPDATE_K_LANGUAGE, SqlUtils.TXT) //
					.appendColumn(T_SERVICE_UPDATE_K_SOURCE_LABEL, SqlUtils.TXT) //
					.appendColumn(T_SERVICE_UPDATE_K_SOURCE_ID, SqlUtils.TXT) //
			;
		}
	}
}
