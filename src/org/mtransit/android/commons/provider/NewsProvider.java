package org.mtransit.android.commons.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.News;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

@SuppressLint("Registered")
public abstract class NewsProvider extends MTContentProvider implements NewsProviderContract {

	private static final String TAG = NewsProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final String NEWS_CONTENT_DIRECTORY = "news";

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	public static void append(UriMatcher uriMatcher, String authority) {
		uriMatcher.addURI(authority, "ping", ContentProviderConstants.PING);
		uriMatcher.addURI(authority, NEWS_CONTENT_DIRECTORY, ContentProviderConstants.NEWS);
	}

	public static final String[] PROJECTION_NEWS = new String[] { //
	NewsColumns.T_NEWS_K_ID, //
			NewsColumns.T_NEWS_K_AUTHORITY_META, //
			NewsColumns.T_NEWS_K_UUID, //
			NewsColumns.T_NEWS_K_LAST_UPDATE, //
			NewsColumns.T_NEWS_K_MAX_VALIDITY_IN_MS, //
			NewsColumns.T_NEWS_K_CREATED_AT, //
			NewsColumns.T_NEWS_K_TARGET_UUID, //
			NewsColumns.T_NEWS_K_COLOR, //
			NewsColumns.T_NEWS_K_AUTHOR_NAME, //
			NewsColumns.T_NEWS_K_AUTHOR_USERNAME, //
			NewsColumns.T_NEWS_K_AUTHOR_PICTURE_URL, //
			NewsColumns.T_NEWS_K_AUTHOR_PROFILE_URL, //
			NewsColumns.T_NEWS_K_TEXT, //
			NewsColumns.T_NEWS_K_TEXT_HTML, //
			NewsColumns.T_NEWS_K_WEB_URL, //
			NewsColumns.T_NEWS_K_LANGUAGE, //
			NewsColumns.T_NEWS_K_SOURCE_ID, //
			NewsColumns.T_NEWS_K_SOURCE_LABEL,
	};


	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link NewsProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link NewsProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.news_authority);
		}
		return authority;
	}

	@Override
	public boolean onCreateMT() {
		ping();
		return true;
	}

	@Override
	public void ping() {
	}

	private static NewsDbHelper dbHelper;
	private static int currentDbVersion = -1;

	private NewsDbHelper getDBHelper(Context context) {
		if (dbHelper == null) { // initialize
			dbHelper = getNewDbHelper(context);
			currentDbVersion = getCurrentDbVersion();
		} else { // reset
			try {
				if (currentDbVersion != getCurrentDbVersion()) {
					dbHelper.close();
					dbHelper = null;
					return getDBHelper(context);
				}
			} catch (Exception e) { // fail if locked, will try again later
				MTLog.w(this, e, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	/**
	 * Override if multiple {@link NewsProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return NewsDbHelper.getDbVersion();
	}

	/**
	 * Override if multiple {@link NewsProvider} implementations in same app.
	 */
	public NewsDbHelper getNewDbHelper(Context context) {
		return new NewsDbHelper(context.getApplicationContext());
	}

	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	@Override
	public Context getContentProviderContext() {
		return getContext();
	}

	@Override
	public Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return queryS(this, uri, selection);
	}

	public static Cursor queryS(NewsProviderContract provider, Uri uri, String selection) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
			provider.ping();
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		case ContentProviderConstants.NEWS:
			return getNews(provider, selection);
		default:
			return null; // not processed
		}
	}

	private static Cursor getNews(NewsProviderContract provider, String selection) {
		NewsFilter newsFilter = NewsFilter.fromJSONString(selection);
		if (newsFilter == null) {
			return getNewsCursor(null);
		}
		if (NewsFilter.isUUIDFilter(newsFilter)) {
			return provider.getNewsFromDB(newsFilter);
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		ArrayList<News> cachedNews = provider.getCachedNews(newsFilter);
		boolean purgeNecessary = false;
		if (cachedNews != null) {
			Iterator<News> it = cachedNews.iterator();
			while (it.hasNext()) {
				News news = it.next();
				if (news.getLastUpdateInMs() + provider.getNewsMaxValidityInMs() < nowInMs) {
					it.remove();
					purgeNecessary = true;
				}
			}
		}
		if (purgeNecessary) {
			provider.purgeUselessCachedNews();
		}
		if (cachedNews != null) {
			Iterator<News> it = cachedNews.iterator();
			while (it.hasNext()) {
				News news = it.next();
				if (!news.isUseful()) {
					provider.deleteCachedNews(news.getId());
					it.remove();
				}
			}
		}
		if (newsFilter.isCacheOnlyOrDefault()) {
			return getNewsCursor(cachedNews);
		}
		long cacheValidityInMs = provider.getNewsValidityInMs(newsFilter.isInFocusOrDefault());
		Long filterCacheValidityInMs = newsFilter.getCacheValidityInMsOrNull();
		if (filterCacheValidityInMs != null && filterCacheValidityInMs > provider.getMinDurationBetweenNewsRefreshInMs(newsFilter.isInFocusOrDefault())) {
			cacheValidityInMs = filterCacheValidityInMs;
		}
		boolean loadNewNews = false;
		if (CollectionUtils.getSize(cachedNews) == 0) {
			loadNewNews = true;
		} else if (cachedNews != null) {
			for (News oneCachedNews : cachedNews) {
				if (oneCachedNews.getLastUpdateInMs() + cacheValidityInMs < nowInMs) {
					loadNewNews = true;
					break;
				}
			}
		}
		if (loadNewNews) {
			ArrayList<News> newNews = provider.getNewNews(newsFilter);
			if (CollectionUtils.getSize(newNews) != 0) {
				return getNewsCursor(newNews);
			}
		}
		return getNewsCursor(cachedNews);
	}

	@Override
	public Cursor getNewsFromDB(NewsFilter newsFilter) {
		return getDefaultNewsFromDB(newsFilter, this);
	}

	public static Cursor getDefaultNewsFromDB(NewsFilter newsFilter, NewsProviderContract provider) {
		SQLiteDatabase db = null;
		try {
			if (newsFilter == null || provider == null) {
				return null;
			}
			String selection = newsFilter.getSqlSelection(NewsColumns.T_NEWS_K_UUID);
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getNewsDbTableName());
			qb.setProjectionMap(provider.getNewsProjectionMap());
			db = provider.getDBHelper().getReadableDatabase();
			return qb.query(db, provider.getNewsProjection(), selection, null, null, null, null, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while loading news '%s'!", newsFilter);
			return null;
		} finally {
			SqlUtils.closeQuietly(db);
		}
	}

	public static Cursor getNewsCursor(ArrayList<News> news) {
		if (news == null) {
			return ContentProviderConstants.EMPTY_CURSOR;
		}
		MatrixCursor matrixCursor = new MatrixCursor(PROJECTION_NEWS);
		for (News oneNews : news) {
			matrixCursor.addRow(oneNews.getCursorRow());
		}
		return matrixCursor;
	}

	@Override
	public String getNewsDbTableName() {
		return NewsDbHelper.T_NEWS;
	}

	@Override
	public String[] getNewsProjection() {
		return PROJECTION_NEWS;
	}

	private static HashMap<String, String> newsProjectionMap;

	@Override
	public HashMap<String, String> getNewsProjectionMap() {
		if (newsProjectionMap == null) {
			newsProjectionMap = getNewNewsProjectionMap(getAUTHORITY(getContext()));
		}
		return newsProjectionMap;
	}

	public static HashMap<String, String> getNewNewsProjectionMap(String authority) {
		HashMap<String, String> newsProjectionMap = new HashMap<String, String>();
		newsProjectionMap.put(NewsColumns.T_NEWS_K_AUTHORITY_META,//
				"'" + authority + "'" //
						+ " AS " + NewsColumns.T_NEWS_K_AUTHORITY_META);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_ID, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_ID + " AS " + NewsColumns.T_NEWS_K_ID);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_UUID, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_UUID + " AS " + NewsColumns.T_NEWS_K_UUID);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_LAST_UPDATE, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_LAST_UPDATE + " AS "
				+ NewsColumns.T_NEWS_K_LAST_UPDATE);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_MAX_VALIDITY_IN_MS, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_MAX_VALIDITY_IN_MS + " AS "
				+ NewsColumns.T_NEWS_K_MAX_VALIDITY_IN_MS);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_CREATED_AT, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_CREATED_AT + " AS "
				+ NewsColumns.T_NEWS_K_CREATED_AT);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_TARGET_UUID, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_TARGET_UUID + " AS "
				+ NewsColumns.T_NEWS_K_TARGET_UUID);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_COLOR, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_COLOR + " AS " + NewsColumns.T_NEWS_K_COLOR);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_AUTHOR_NAME, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_AUTHOR_NAME + " AS "
				+ NewsColumns.T_NEWS_K_AUTHOR_NAME);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_AUTHOR_USERNAME, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_AUTHOR_USERNAME + " AS "
				+ NewsColumns.T_NEWS_K_AUTHOR_USERNAME);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_AUTHOR_PICTURE_URL, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_AUTHOR_PICTURE_URL + " AS "
				+ NewsColumns.T_NEWS_K_AUTHOR_PICTURE_URL);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_AUTHOR_PROFILE_URL, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_AUTHOR_PROFILE_URL + " AS "
				+ NewsColumns.T_NEWS_K_AUTHOR_PROFILE_URL);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_TEXT, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_TEXT + " AS " + NewsColumns.T_NEWS_K_TEXT);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_TEXT_HTML, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_TEXT_HTML + " AS "
				+ NewsColumns.T_NEWS_K_TEXT_HTML);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_WEB_URL, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_WEB_URL + " AS " + NewsColumns.T_NEWS_K_WEB_URL);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_LANGUAGE, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_LANGUAGE + " AS "
				+ NewsColumns.T_NEWS_K_LANGUAGE);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_SOURCE_ID, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_SOURCE_ID + " AS "
				+ NewsColumns.T_NEWS_K_SOURCE_ID);
		newsProjectionMap.put(NewsColumns.T_NEWS_K_SOURCE_LABEL, NewsDbHelper.T_NEWS + "." + NewsDbHelper.T_NEWS_K_SOURCE_LABEL + " AS "
				+ NewsColumns.T_NEWS_K_SOURCE_LABEL);
		return newsProjectionMap;
	}

	@Override
	public String getTypeMT(Uri uri) {
		return getTypeS(this, uri);
	}

	public static String getTypeS(NewsProviderContract provider, Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.NEWS:
			return StringUtils.EMPTY; // empty string = processed
		default:
			return null; // not processed
		}
	}

	@Override
	public int deleteMT(Uri uri, String selection, String[] selectionArgs) {
		MTLog.w(this, "The delete method is not available.");
		return 0;
	}

	@Override
	public int updateMT(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		MTLog.w(this, "The update method is not available.");
		return 0;
	}

	@Override
	public Uri insertMT(Uri uri, ContentValues values) {
		MTLog.w(this, "The insert method is not available.");
		return null;
	}

	public static synchronized int cacheNewsS(NewsProviderContract provider, ArrayList<News> newNews) {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			db.beginTransaction(); // start the transaction
			if (newNews != null) {
				for (News oneNews : newNews) {
					long rowId = db.insert(provider.getNewsDbTableName(), NewsDbHelper.T_NEWS_K_ID, oneNews.toContentValues());
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

	public static void cacheNewsS(NewsProviderContract provider, News newNews) {
		SQLiteDatabase db = null;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			db.insert(provider.getNewsDbTableName(), NewsDbHelper.T_NEWS_K_ID, newNews.toContentValues());
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while inserting '%s' into cache!", newNews);
		} finally {
			SqlUtils.closeQuietly(db);
		}
	}

	public static ArrayList<News> getCachedNewsS(NewsProviderContract provider, String targetUUID) {
		Uri uri = getNewsContentUri(provider);
		String selection = new StringBuilder() //
				.append(NewsColumns.T_NEWS_K_TARGET_UUID).append("='").append(targetUUID).append("'") //
				.toString();
		return getCachedNewsS(provider, uri, selection);
	}

	private static ArrayList<News> getCachedNewsS(NewsProviderContract provider, Uri uri, String selection) {
		ArrayList<News> cache = new ArrayList<News>();
		Cursor cursor = null;
		SQLiteDatabase db = null;
		try {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getNewsDbTableName());
			qb.setProjectionMap(provider.getNewsProjectionMap());
			db = provider.getDBHelper().getReadableDatabase();
			cursor = qb.query(db, PROJECTION_NEWS, selection, null, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						cache.add(News.fromCursor(cursor));
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

	public static Uri getNewsContentUri(NewsProviderContract provider) {
		return Uri.withAppendedPath(provider.getAuthorityUri(), NEWS_CONTENT_DIRECTORY);
	}

	public static boolean deleteCachedNews(NewsProviderContract provider, Integer newsId) {
		if (newsId == null) {
			return false;
		}
		String selection = new StringBuilder() //
				.append(NewsColumns.T_NEWS_K_ID).append("=").append(newsId) //
				.toString();
		SQLiteDatabase db = null;
		int deletedRows = 0;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			deletedRows = db.delete(provider.getNewsDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while deleting cached news '%s'!", newsId);
		} finally {
			SqlUtils.closeQuietly(db);
		}
		return deletedRows > 0;
	}

	public static boolean purgeUselessCachedNews(NewsProviderContract provider) {
		long oldestLastUpdate = TimeUtils.currentTimeMillis() - provider.getNewsMaxValidityInMs();
		String selection = new StringBuilder() //
				.append(NewsColumns.T_NEWS_K_LAST_UPDATE).append(" < ").append(oldestLastUpdate) //
				.toString();
		SQLiteDatabase db = null;
		int deletedRows = 0;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			deletedRows = db.delete(provider.getNewsDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while deleting cached news!");
		} finally {
			SqlUtils.closeQuietly(db);
		}
		return deletedRows > 0;
	}

	public static class NewsColumns {
		public static final String T_NEWS_K_ID = BaseColumns._ID;
		public static final String T_NEWS_K_AUTHORITY_META = "authority";
		public static final String T_NEWS_K_UUID = "uuid";
		public static final String T_NEWS_K_LAST_UPDATE = "last_update";
		public static final String T_NEWS_K_CREATED_AT = "created_at";
		public static final String T_NEWS_K_MAX_VALIDITY_IN_MS = "max_validity";
		public static final String T_NEWS_K_TARGET_UUID = "target";
		public static final String T_NEWS_K_COLOR = "color";
		public static final String T_NEWS_K_AUTHOR_NAME = "author_name";
		public static final String T_NEWS_K_AUTHOR_USERNAME = "author_username";
		public static final String T_NEWS_K_AUTHOR_PICTURE_URL = "author_picture_url";
		public static final String T_NEWS_K_AUTHOR_PROFILE_URL = "author_profile_url";
		public static final String T_NEWS_K_TEXT = "text";
		public static final String T_NEWS_K_TEXT_HTML = "text_html";
		public static final String T_NEWS_K_WEB_URL = "web_url";
		public static final String T_NEWS_K_LANGUAGE = "lang";
		public static final String T_NEWS_K_SOURCE_ID = "source_id";
		public static final String T_NEWS_K_SOURCE_LABEL = "source_label";
	}

	public static class NewsFilter implements MTLog.Loggable {

		private static final String TAG = NewsFilter.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private static final boolean CACHE_ONLY_DEFAULT = false;

		private static final boolean IN_FOCUS_DEFAULT = false;

		private Collection<String> uuids;
		private Boolean cacheOnly = null;
		private Long cacheValidityInMs = null;
		private Boolean inFocus = null;

		public NewsFilter() {
		}

		public NewsFilter(Collection<String> uuids) {
			if (uuids == null || uuids.size() == 0) {
				throw new UnsupportedOperationException("Need at least 1 uuid!");
			}
			this.uuids = uuids;
		}

		@Override
		public String toString() {
			return new StringBuilder(NewsFilter.class.getSimpleName()).append('{') //
					.append("uuids:").append(this.uuids) //
					.append(',') //
					.append("cacheOnly:").append(this.cacheOnly) //
					.append(',') //
					.append("inFocus:").append(this.inFocus) //
					.append(',') //
					.append("cacheValidityInMs:").append(this.cacheValidityInMs) //
					.append('}').toString();
		}

		public static boolean isUUIDFilter(NewsFilter newsFilter) {
			return newsFilter != null && newsFilter.uuids != null && newsFilter.uuids.size() > 0;
		}

		public String getSqlSelection(String uuidTableColumn) {
			if (isUUIDFilter(this)) {
				StringBuilder qb = new StringBuilder();
				for (String uid : this.uuids) {
					if (qb.length() == 0) {
						qb.append(uuidTableColumn).append(" IN (");
					} else {
						qb.append(',');
					}
					qb.append('\'').append(uid).append('\'');
				}
				qb.append(')');
				return qb.toString();
			} else {
				return null;
			}
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

		public static NewsFilter fromJSONString(String jsonString) {
			try {
				return jsonString == null ? null : fromJSON(new JSONObject(jsonString));
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON string '%s'", jsonString);
				return null;
			}
		}

		private static final String JSON_UUIDS = "uuids";
		private static final String JSON_CACHE_ONLY = "cacheOnly";
		private static final String JSON_IN_FOCUS = "inFocus";
		private static final String JSON_CACHE_VALIDITY_IN_MS = "cacheValidityInMs";

		public static NewsFilter fromJSON(JSONObject json) {
			try {
				NewsFilter newsFilter;
				JSONArray jUUIDs = json.optJSONArray(JSON_UUIDS);
				if (jUUIDs != null && jUUIDs.length() > 0) {
					HashSet<String> uuids = new HashSet<String>();
					for (int i = 0; i < jUUIDs.length(); i++) {
						uuids.add(jUUIDs.getString(i));
					}
					newsFilter = new NewsFilter(uuids);
				} else {
					newsFilter = new NewsFilter();
				}
				if (json.has(JSON_CACHE_ONLY)) {
					newsFilter.cacheOnly = json.getBoolean(JSON_CACHE_ONLY);
				}
				if (json.has(JSON_IN_FOCUS)) {
					newsFilter.inFocus = json.getBoolean(JSON_IN_FOCUS);
				}
				if (json.has(JSON_CACHE_VALIDITY_IN_MS)) {
					newsFilter.cacheValidityInMs = json.getLong(JSON_CACHE_VALIDITY_IN_MS);
				}
				return newsFilter;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", json);
				return null;
			}
		}

		public String toJSONString() {
			return toJSONString(this);
		}

		public static String toJSONString(NewsFilter newsFilter) {
			try {
				JSONObject json = toJSON(newsFilter);
				return json == null ? null : json.toString();
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while generating JSON string '%s'", newsFilter);
				return null;
			}
		}

		public static JSONObject toJSON(NewsFilter newsFilter) throws JSONException {
			try {
				JSONObject json = new JSONObject();
				if (newsFilter.getCacheOnlyOrNull() != null) {
					json.put(JSON_CACHE_ONLY, newsFilter.getCacheOnlyOrNull());
				}
				if (newsFilter.getInFocusOrNull() != null) {
					json.put(JSON_IN_FOCUS, newsFilter.getInFocusOrNull());
				}
				if (newsFilter.getCacheValidityInMsOrNull() != null) {
					json.put(JSON_CACHE_VALIDITY_IN_MS, newsFilter.getCacheValidityInMsOrNull());
				}
				if (isUUIDFilter(newsFilter)) {
					JSONArray jUUIDs = new JSONArray();
					for (String uuid : newsFilter.uuids) {
						jUUIDs.put(uuid);
					}
					json.put(JSON_UUIDS, jUUIDs);
				}
				return json;
			} catch (JSONException jsone) {
				MTLog.w(TAG, jsone, "Error while parsing JSON object '%s'", newsFilter);
				return null;
			}
		}

	}

	public static class NewsDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = NewsDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link NewsDbHelper} implementations in same app.
		 */
		public static final String DB_NAME = "news.db";

		/**
		 * Override if multiple {@link NewsDbHelper} implementations in same app.
		 */
		protected static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pNewsLastUpdate";

		public static final int DB_VERSION = 1;

		public static final String T_NEWS = "news";
		public static final String T_NEWS_K_ID = BaseColumns._ID;
		public static final String T_NEWS_K_UUID = "uuid";
		public static final String T_NEWS_K_LAST_UPDATE = "last_update";
		public static final String T_NEWS_K_MAX_VALIDITY_IN_MS = "max_validity";
		public static final String T_NEWS_K_CREATED_AT = "created_at";
		public static final String T_NEWS_K_TARGET_UUID = "target";
		public static final String T_NEWS_K_COLOR = "color";
		public static final String T_NEWS_K_AUTHOR_NAME = "author_name";
		public static final String T_NEWS_K_AUTHOR_USERNAME = "author_username";
		public static final String T_NEWS_K_AUTHOR_PICTURE_URL = "author_picture_url";
		public static final String T_NEWS_K_AUTHOR_PROFILE_URL = "author_profile_url";
		public static final String T_NEWS_K_TEXT = "text";
		public static final String T_NEWS_K_TEXT_HTML = "text_html";
		public static final String T_NEWS_K_WEB_URL = "web_url";
		public static final String T_NEWS_K_LANGUAGE = "lang";
		public static final String T_NEWS_K_SOURCE_ID = "source_id";
		public static final String T_NEWS_K_SOURCE_LABEL = "source_label";

		public static final String T_NEWS_SQL_CREATE = getSqlCreate(T_NEWS);
		public static final String T_NEWS_SQL_INSERT = getSqlInsert(T_NEWS);
		public static final String T_NEWS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_NEWS);

		public NewsDbHelper(Context context) {
			this(context, DB_NAME, DB_VERSION);
		}

		public NewsDbHelper(Context context, String dbName, int dbVersion) {
			super(context, dbName, null, dbVersion);
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_NEWS_SQL_DROP);
			initAllDbTables(db);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_NEWS_SQL_CREATE);
		}

		/**
		 * Override if multiple {@link NewsDbHelper} implementations in same app.
		 */
		public String getDbName() {
			return DB_NAME;
		}

		/**
		 * Override if multiple {@link NewsDbHelper} in same app.
		 */
		public static int getDbVersion() {
			return DB_VERSION;
		}

		public static String getFkColumnName(String columnName) {
			return "fk" + "_" + columnName;
		}

		public static String getSqlCreate(String table, String... createLines) {
			StringBuilder sqlCreateSb = new StringBuilder(SqlUtils.CREATE_TABLE_IF_NOT_EXIST).append(table).append(" (") //
					.append(T_NEWS_K_ID).append(SqlUtils.INT_PK) //
					.append(", ") //
					.append(T_NEWS_K_UUID).append(SqlUtils.TXT) //
					.append(", ") //
					.append(T_NEWS_K_LAST_UPDATE).append(SqlUtils.INT) //
					.append(", ") //
					.append(T_NEWS_K_MAX_VALIDITY_IN_MS).append(SqlUtils.INT) //
					.append(", ") //
					.append(T_NEWS_K_CREATED_AT).append(SqlUtils.INT) //
					.append(", ") //
					.append(T_NEWS_K_TARGET_UUID).append(SqlUtils.TXT) //
					.append(", ") //
					.append(T_NEWS_K_COLOR).append(SqlUtils.TXT) //
					.append(", ") //
					.append(T_NEWS_K_AUTHOR_NAME).append(SqlUtils.TXT) //
					.append(", ") //
					.append(T_NEWS_K_AUTHOR_USERNAME).append(SqlUtils.TXT) //
					.append(", ") //
					.append(T_NEWS_K_AUTHOR_PICTURE_URL).append(SqlUtils.TXT) //
					.append(", ") //
					.append(T_NEWS_K_AUTHOR_PROFILE_URL).append(SqlUtils.TXT) //
					.append(", ") //
					.append(T_NEWS_K_TEXT).append(SqlUtils.TXT) //
					.append(", ") //
					.append(T_NEWS_K_TEXT_HTML).append(SqlUtils.TXT) //
					.append(", ") //
					.append(T_NEWS_K_WEB_URL).append(SqlUtils.TXT) //
					.append(", ") //
					.append(T_NEWS_K_LANGUAGE).append(SqlUtils.TXT) //
					.append(", ") //
					.append(T_NEWS_K_SOURCE_ID).append(SqlUtils.TXT) //
					.append(", ") //
					.append(T_NEWS_K_SOURCE_LABEL).append(SqlUtils.TXT) //
			;
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

		public static String getSqlInsert(String table, String... columns) {
			StringBuilder sqlInsertSb = new StringBuilder("INSERT INTO ").append(table).append(" (") //
					.append(T_NEWS_K_ID) //
					.append(",") //
					.append(T_NEWS_K_UUID) //
					.append(",") //
					.append(T_NEWS_K_LAST_UPDATE) //
					.append(",") //
					.append(T_NEWS_K_MAX_VALIDITY_IN_MS) //
					.append(",") //
					.append(T_NEWS_K_CREATED_AT) //
					.append(",") //
					.append(T_NEWS_K_TARGET_UUID) //
					.append(",") //
					.append(T_NEWS_K_COLOR) //
					.append(",") //
					.append(T_NEWS_K_AUTHOR_NAME) //
					.append(",") //
					.append(T_NEWS_K_AUTHOR_USERNAME) //
					.append(",") //
					.append(T_NEWS_K_AUTHOR_PICTURE_URL) //
					.append(",") //
					.append(T_NEWS_K_AUTHOR_PROFILE_URL) //
					.append(",") //
					.append(T_NEWS_K_TEXT) //
					.append(",") //
					.append(T_NEWS_K_TEXT_HTML) //
					.append(",") //
					.append(T_NEWS_K_WEB_URL) //
					.append(",") //
					.append(T_NEWS_K_LANGUAGE) //
					.append(",") //
					.append(T_NEWS_K_SOURCE_ID) //
					.append(",") //
					.append(T_NEWS_K_SOURCE_LABEL) //
			;
			if (columns != null) {
				for (String column : columns) {
					if (sqlInsertSb.length() > 0) {
						sqlInsertSb.append(",");
					}
					sqlInsertSb.append(column);
				}
			}
			sqlInsertSb.append(") VALUES(%s)");
			return sqlInsertSb.toString();
		}
	}
}
