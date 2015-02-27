package org.mtransit.android.commons.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.ServiceUpdate;

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

public abstract class ServiceUpdateProvider extends MTContentProvider implements ServiceUpdateProviderContract {

	private static final String TAG = ServiceUpdateProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static void append(UriMatcher uriMatcher, String authority) {
		uriMatcher.addURI(authority, "ping", ContentProviderConstants.PING);
		uriMatcher.addURI(authority, SERVICE_UPDATE_CONTENT_DIRECTORY, ContentProviderConstants.SERVICE_UPDATE);
	}

	public static final String SERVICE_UPDATE_CONTENT_DIRECTORY = "service";

	public static final String[] PROJECTION_SERVICE_UPDATE = new String[] { ServiceUpdateColumns.T_SERVICE_UPDATE_K_ID,
			ServiceUpdateColumns.T_SERVICE_UPDATE_K_TARGET_UUID, ServiceUpdateColumns.T_SERVICE_UPDATE_K_LAST_UPDATE,
			ServiceUpdateColumns.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS, ServiceUpdateColumns.T_SERVICE_UPDATE_K_SEVERITY,
			ServiceUpdateColumns.T_SERVICE_UPDATE_K_TEXT, ServiceUpdateColumns.T_SERVICE_UPDATE_K_TEXT_HTML, ServiceUpdateColumns.T_SERVICE_UPDATE_K_LANGUAGE,
			ServiceUpdateColumns.T_SERVICE_UPDATE_K_SOURCE_LABEL, ServiceUpdateColumns.T_SERVICE_UPDATE_K_SOURCE_ID };

	public static final HashMap<String, String> SERVICE_UPDATE_PROJECTION_MAP;
	static {
		HashMap<String, String> map;

		map = new HashMap<String, String>();
		map.put(ServiceUpdateColumns.T_SERVICE_UPDATE_K_ID, ServiceUpdateDbHelper.T_SERVICE_UPDATE + "." + ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_ID + " AS "
				+ ServiceUpdateColumns.T_SERVICE_UPDATE_K_ID);
		map.put(ServiceUpdateColumns.T_SERVICE_UPDATE_K_TARGET_UUID, ServiceUpdateDbHelper.T_SERVICE_UPDATE + "."
				+ ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_TARGET_UUID + " AS " + ServiceUpdateColumns.T_SERVICE_UPDATE_K_TARGET_UUID);
		map.put(ServiceUpdateColumns.T_SERVICE_UPDATE_K_LAST_UPDATE, ServiceUpdateDbHelper.T_SERVICE_UPDATE + "."
				+ ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_LAST_UPDATE + " AS " + ServiceUpdateColumns.T_SERVICE_UPDATE_K_LAST_UPDATE);
		map.put(ServiceUpdateColumns.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS, ServiceUpdateDbHelper.T_SERVICE_UPDATE + "."
				+ ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS + " AS " + ServiceUpdateColumns.T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS);
		map.put(ServiceUpdateColumns.T_SERVICE_UPDATE_K_SEVERITY, ServiceUpdateDbHelper.T_SERVICE_UPDATE + "."
				+ ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_SEVERITY + " AS " + ServiceUpdateColumns.T_SERVICE_UPDATE_K_SEVERITY);
		map.put(ServiceUpdateColumns.T_SERVICE_UPDATE_K_TEXT, ServiceUpdateDbHelper.T_SERVICE_UPDATE + "." + ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_TEXT
				+ " AS " + ServiceUpdateColumns.T_SERVICE_UPDATE_K_TEXT);
		map.put(ServiceUpdateColumns.T_SERVICE_UPDATE_K_TEXT_HTML, ServiceUpdateDbHelper.T_SERVICE_UPDATE + "."
				+ ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_TEXT_HTML + " AS " + ServiceUpdateColumns.T_SERVICE_UPDATE_K_TEXT_HTML);
		map.put(ServiceUpdateColumns.T_SERVICE_UPDATE_K_LANGUAGE, ServiceUpdateDbHelper.T_SERVICE_UPDATE + "."
				+ ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_LANGUAGE + " AS " + ServiceUpdateColumns.T_SERVICE_UPDATE_K_LANGUAGE);
		map.put(ServiceUpdateColumns.T_SERVICE_UPDATE_K_SOURCE_LABEL, ServiceUpdateDbHelper.T_SERVICE_UPDATE + "."
				+ ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_SOURCE_LABEL + " AS " + ServiceUpdateColumns.T_SERVICE_UPDATE_K_SOURCE_LABEL);
		map.put(ServiceUpdateColumns.T_SERVICE_UPDATE_K_SOURCE_ID, ServiceUpdateDbHelper.T_SERVICE_UPDATE + "."
				+ ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_SOURCE_ID + " AS " + ServiceUpdateColumns.T_SERVICE_UPDATE_K_SOURCE_ID);
		SERVICE_UPDATE_PROJECTION_MAP = map;
	}

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
		ServiceUpdateFilter serviceUpdateFilter = ServiceUpdateFilter.fromJSONString(selection);
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
		MatrixCursor matrixCursor = new MatrixCursor(PROJECTION_SERVICE_UPDATE);
		for (ServiceUpdate serviceUpdate : serviceUpdates) {
			matrixCursor.addRow(serviceUpdate.getCursorRow());
		}
		return matrixCursor;
	}

	public static synchronized int cacheServiceUpdatesS(ServiceUpdateProviderContract provider, ArrayList<ServiceUpdate> newServiceUpdates) {
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
			SqlUtils.endTransactionAndCloseQuietly(db);
		}
		return affectedRows;
	}

	public static void cacheServiceUpdateS(ServiceUpdateProviderContract provider, ServiceUpdate newServiceUpdate) {
		SQLiteDatabase db = null;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			db.insert(provider.getServiceUpdateDbTableName(), ServiceUpdateDbHelper.T_SERVICE_UPDATE_K_ID, newServiceUpdate.toContentValues());
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while inserting '%s' into cache!", newServiceUpdate);
		} finally {
			SqlUtils.closeQuietly(db);
		}
	}

	public static ArrayList<ServiceUpdate> getCachedServiceUpdatesS(ServiceUpdateProviderContract provider, String targetUUID) {
		Uri uri = getServiceUpdateContentUri(provider);
		String selection = new StringBuilder() //
				.append(ServiceUpdateColumns.T_SERVICE_UPDATE_K_TARGET_UUID).append("='").append(targetUUID).append("'") //
				.append(" AND ") //
				.append(ServiceUpdateColumns.T_SERVICE_UPDATE_K_LANGUAGE).append("='").append(provider.getServiceUpdateLanguage()).append("'")//
				.toString();
		return getCachedServiceUpdatesS(provider, uri, selection);
	}

	private static ArrayList<ServiceUpdate> getCachedServiceUpdatesS(ServiceUpdateProviderContract provider, Uri uri, String selection) {
		ArrayList<ServiceUpdate> cache = new ArrayList<ServiceUpdate>();
		Cursor cursor = null;
		SQLiteDatabase db = null;
		try {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getServiceUpdateDbTableName());
			qb.setProjectionMap(SERVICE_UPDATE_PROJECTION_MAP);
			db = provider.getDBHelper().getReadableDatabase();
			cursor = qb.query(db, PROJECTION_SERVICE_UPDATE, selection, null, null, null, null, null);
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
			SqlUtils.closeQuietly(db);
		}
	}

	public static Uri getServiceUpdateContentUri(ServiceUpdateProviderContract provider) {
		return Uri.withAppendedPath(provider.getAuthorityUri(), SERVICE_UPDATE_CONTENT_DIRECTORY);
	}

	public static boolean deleteCachedServiceUpdate(ServiceUpdateProviderContract provider, Integer serviceUpdateId) {
		if (serviceUpdateId == null) {
			return false;
		}
		String selection = new StringBuilder() //
				.append(ServiceUpdateColumns.T_SERVICE_UPDATE_K_ID).append("=").append(serviceUpdateId) //
				.toString();
		SQLiteDatabase db = null;
		int deletedRows = 0;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			deletedRows = db.delete(provider.getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while deleting cached service update '%s'!", serviceUpdateId);
		} finally {
			SqlUtils.closeQuietly(db);
		}
		return deletedRows > 0;
	}

	public static boolean deleteCachedServiceUpdate(ServiceUpdateProviderContract provider, String targetUUID, String sourceId) {
		if (TextUtils.isEmpty(targetUUID) || TextUtils.isEmpty(sourceId)) {
			return false;
		}
		String selection = new StringBuilder() //
				.append(ServiceUpdateColumns.T_SERVICE_UPDATE_K_TARGET_UUID).append("=").append('\'').append(targetUUID).append('\'') //
				.append(" AND ") //
				.append(ServiceUpdateColumns.T_SERVICE_UPDATE_K_SOURCE_ID).append("=").append('\'').append(sourceId).append('\'') //
				.toString();
		SQLiteDatabase db = null;
		int deletedRows = 0;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			deletedRows = db.delete(provider.getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while deleting cached service update(s) target '%s' source '%s' !", targetUUID, sourceId);
		} finally {
			SqlUtils.closeQuietly(db);
		}
		return deletedRows > 0;
	}

	public static boolean purgeUselessCachedServiceUpdates(ServiceUpdateProviderContract provider) {
		long oldestLastUpdate = TimeUtils.currentTimeMillis() - provider.getServiceUpdateMaxValidityInMs();
		String selection = new StringBuilder() //
				.append(ServiceUpdateColumns.T_SERVICE_UPDATE_K_LAST_UPDATE).append(" < ").append(oldestLastUpdate) //
				.toString();
		SQLiteDatabase db = null;
		int deletedRows = 0;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			deletedRows = db.delete(provider.getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while deleting cached service updates!");
		} finally {
			SqlUtils.closeQuietly(db);
		}
		return deletedRows > 0;
	}

	public static class ServiceUpdateColumns {
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

		public static final String T_SERVICE_UPDATE_SQL_CREATE = getSqlCreate(T_SERVICE_UPDATE);

		public static final String T_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_SERVICE_UPDATE);

		public ServiceUpdateDbHelper(Context context, String dbName, CursorFactory factory, int dbVersion) {
			super(context, dbName, factory, dbVersion);
		}

		public abstract String getDbName();

		public static String getFkColumnName(String columnName) {
			return "fk" + "_" + columnName;
		}

		public static String getSqlCreate(String table, String... createLines) {
			StringBuilder sqlCreateSb = new StringBuilder(SqlUtils.CREATE_TABLE_IF_NOT_EXIST).append(table).append(" (") //
					.append(T_SERVICE_UPDATE_K_ID).append(SqlUtils.INT_PK_AUTO).append(", ") //
					.append(T_SERVICE_UPDATE_K_TARGET_UUID).append(SqlUtils.TXT).append(", ") //
					.append(T_SERVICE_UPDATE_K_LAST_UPDATE).append(SqlUtils.INT).append(", ") //
					.append(T_SERVICE_UPDATE_K_MAX_VALIDITY_IN_MS).append(SqlUtils.INT).append(", ") //
					.append(T_SERVICE_UPDATE_K_SEVERITY).append(SqlUtils.INT).append(", ") //
					.append(T_SERVICE_UPDATE_K_TEXT).append(SqlUtils.TXT).append(", ") //
					.append(T_SERVICE_UPDATE_K_TEXT_HTML).append(SqlUtils.TXT).append(", ") //
					.append(T_SERVICE_UPDATE_K_LANGUAGE).append(SqlUtils.TXT).append(", ") //
					.append(T_SERVICE_UPDATE_K_SOURCE_LABEL).append(SqlUtils.TXT).append(", ") //
					.append(T_SERVICE_UPDATE_K_SOURCE_ID).append(SqlUtils.TXT); //
			if (createLines != null) {
				for (String createLine : createLines) {
					if (sqlCreateSb.length() > 0) {
						sqlCreateSb.append(", ");
					}
					sqlCreateSb.append(createLine);
				}
			}
			sqlCreateSb.append(")");
			return sqlCreateSb.toString();
		}
	}

	public static class ServiceUpdateFilter implements MTLog.Loggable {

		private static final String TAG = ServiceUpdateFilter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private static final boolean CACHE_ONLY_DEFAULT = false;

		private static final boolean IN_FOCUS_DEFAULT = false;

		private POI poi;
		private Boolean cacheOnly = null;
		private Long cacheValidityInMs = null;
		private Boolean inFocus = null;

		public ServiceUpdateFilter(POI poi) {
			this.poi = poi;
		}

		@Override
		public String toString() {
			return new StringBuilder(ServiceUpdateFilter.class.getSimpleName())//
					.append("cacheOnly:").append(this.cacheOnly) //
					.append(',') //
					.append("inFocus:").append(this.inFocus) //
					.append(',') //
					.append("cacheValidityInMs:").append(this.cacheValidityInMs) //
					.append(',') //
					.append("poi:").append(this.poi) //
					.toString();
		}

		public POI getPoi() {
			return poi;
		}

		public void setCacheOnly(Boolean cacheOnly) {
			this.cacheOnly = cacheOnly;
		}

		public boolean isCacheOnlyOrDefault() {
			return this.cacheOnly == null ? CACHE_ONLY_DEFAULT : this.cacheOnly;
		}

		public Boolean getCacheOnlyOrNull() {
			return this.cacheOnly;
		}

		public void setInFocus(Boolean inFocus) {
			this.inFocus = inFocus;
		}

		public boolean isInFocusOrDefault() {
			return this.inFocus == null ? IN_FOCUS_DEFAULT : this.inFocus;
		}

		public Boolean getInFocusOrNull() {
			return this.inFocus;
		}

		public Long getCacheValidityInMsOrNull() {
			return this.cacheValidityInMs;
		}

		public boolean hasCacheValidityInMs() {
			return this.cacheValidityInMs != null && this.cacheValidityInMs > 0;
		}

		public void setCacheValidityInMs(Long cacheValidityInMs) {
			this.cacheValidityInMs = cacheValidityInMs;
		}

		public static ServiceUpdateFilter fromJSONString(String jsonString) {
			try {
				return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		private static final String JSON_POI = "poi";
		private static final String JSON_CACHE_ONLY = "cacheOnly";
		private static final String JSON_IN_FOCUS = "inFocus";
		private static final String JSON_CACHE_VALIDITY_IN_MS = "cacheValidityInMs";

		public static ServiceUpdateFilter fromJSON(JSONObject json) {
			try {
				POI poi = DefaultPOI.fromJSONStatic(json.getJSONObject(JSON_POI));
				ServiceUpdateFilter serviceUpdateFilter = new ServiceUpdateFilter(poi);
				if (json.has(JSON_CACHE_ONLY)) {
					serviceUpdateFilter.cacheOnly = json.getBoolean(JSON_CACHE_ONLY);
				}
				if (json.has(JSON_IN_FOCUS)) {
					serviceUpdateFilter.inFocus = json.getBoolean(JSON_IN_FOCUS);
				}
				if (json.has(JSON_CACHE_VALIDITY_IN_MS)) {
					serviceUpdateFilter.cacheValidityInMs = json.getLong(JSON_CACHE_VALIDITY_IN_MS);
				}
				return serviceUpdateFilter;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		public String toJSONString() {
			return toJSONString(this);
		}

		public static String toJSONString(ServiceUpdateFilter serviceUpdateFilter) {
			try {
				JSONObject json = toJSON(serviceUpdateFilter);
				return json == null ? null : json.toString();
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while generating JSON string '%s'", serviceUpdateFilter);
				return null;
			}
		}

		public static JSONObject toJSON(ServiceUpdateFilter serviceUpdateFilter) throws JSONException {
			try {
				JSONObject json = new JSONObject();
				json.put(JSON_POI, serviceUpdateFilter.poi.toJSON());
				if (serviceUpdateFilter.getCacheOnlyOrNull() != null) {
					json.put(JSON_CACHE_ONLY, serviceUpdateFilter.getCacheOnlyOrNull());
				}
				if (serviceUpdateFilter.getInFocusOrNull() != null) {
					json.put(JSON_IN_FOCUS, serviceUpdateFilter.getInFocusOrNull());
				}
				if (serviceUpdateFilter.getCacheValidityInMsOrNull() != null) {
					json.put(JSON_CACHE_VALIDITY_IN_MS, serviceUpdateFilter.getCacheValidityInMsOrNull());
				}
				return json;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", serviceUpdateFilter);
				return null;
			}
		}
	}
}
