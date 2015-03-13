package org.mtransit.android.commons.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

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
import android.text.TextUtils;

@SuppressLint("Registered")
public abstract class NewsProvider extends MTContentProvider implements NewsProviderContract {

	private static final String TAG = NewsProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	public static void append(UriMatcher uriMatcher, String authority) {
		uriMatcher.addURI(authority, NewsProviderContract.PING_PATH, ContentProviderConstants.PING);
		uriMatcher.addURI(authority, NewsProviderContract.NEWS_PATH, ContentProviderConstants.NEWS);
	}

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
	public void ping() { // do nothing
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
		return NewsDbHelper.getDbVersion(getContext());
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
		NewsProviderContract.Filter newsFilter = NewsProviderContract.Filter.fromJSONString(selection);
		if (newsFilter == null) {
			return getNewsCursor(null);
		}
		if (NewsProviderContract.Filter.isUUIDFilter(newsFilter)) {
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
	public Cursor getNewsFromDB(NewsProviderContract.Filter newsFilter) {
		return getDefaultNewsFromDB(newsFilter, this);
	}

	public static Cursor getDefaultNewsFromDB(NewsProviderContract.Filter newsFilter, NewsProviderContract provider) {
		try {
			if (newsFilter == null || provider == null) {
				return null;
			}
			String selection = newsFilter.getSqlSelection(NewsProviderContract.Columns.T_NEWS_K_UUID, NewsProviderContract.Columns.T_NEWS_K_TARGET_UUID);
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getNewsDbTableName());
			qb.setProjectionMap(provider.getNewsProjectionMap());
			return qb.query(provider.getDBHelper().getReadableDatabase(), provider.getNewsProjection(), selection, null, null, null, null, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while loading news '%s'!", newsFilter);
			return null;
		}
	}

	public static Cursor getNewsCursor(ArrayList<News> news) {
		if (news == null) {
			return ContentProviderConstants.EMPTY_CURSOR;
		}
		MatrixCursor matrixCursor = new MatrixCursor(NewsProviderContract.PROJECTION_NEWS);
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
		return NewsProviderContract.PROJECTION_NEWS;
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
		return SqlUtils.ProjectionMapBuilder.getNew() //
				.appendValue(SqlUtils.escapeString(authority), Columns.T_NEWS_K_AUTHORITY_META) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_ID, Columns.T_NEWS_K_ID) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_UUID, Columns.T_NEWS_K_UUID) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_SEVERITY, Columns.T_NEWS_K_SEVERITY) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_NOTEWORTHY, Columns.T_NEWS_K_NOTEWORTHY) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_LAST_UPDATE, Columns.T_NEWS_K_LAST_UPDATE) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_MAX_VALIDITY_IN_MS, Columns.T_NEWS_K_MAX_VALIDITY_IN_MS) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_CREATED_AT, Columns.T_NEWS_K_CREATED_AT) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_TARGET_UUID, Columns.T_NEWS_K_TARGET_UUID) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_COLOR, Columns.T_NEWS_K_COLOR) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_AUTHOR_NAME, Columns.T_NEWS_K_AUTHOR_NAME) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_AUTHOR_USERNAME, Columns.T_NEWS_K_AUTHOR_USERNAME) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_AUTHOR_PICTURE_URL, Columns.T_NEWS_K_AUTHOR_PICTURE_URL) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_AUTHOR_PROFILE_URL, Columns.T_NEWS_K_AUTHOR_PROFILE_URL) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_TEXT, Columns.T_NEWS_K_TEXT) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_TEXT_HTML, Columns.T_NEWS_K_TEXT_HTML) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_WEB_URL, Columns.T_NEWS_K_WEB_URL) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_LANGUAGE, Columns.T_NEWS_K_LANGUAGE) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_SOURCE_ID, Columns.T_NEWS_K_SOURCE_ID) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_SOURCE_LABEL, Columns.T_NEWS_K_SOURCE_LABEL) //
				.build();
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
			SqlUtils.endTransaction(db);
		}
		return affectedRows;
	}

	public static void cacheNewsS(NewsProviderContract provider, News newNews) {
		try {
			provider.getDBHelper().getWritableDatabase().insert(provider.getNewsDbTableName(), NewsDbHelper.T_NEWS_K_ID, newNews.toContentValues());
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while inserting '%s' into cache!", newNews);
		}
	}

	public static ArrayList<News> getCachedNewsS(NewsProviderContract provider, Filter newFilter) {
		Uri uri = getNewsContentUri(provider);
		StringBuilder sqlSelectionSb = new StringBuilder();
		String filterSelection = newFilter.getSqlSelection(NewsProviderContract.Columns.T_NEWS_K_UUID, NewsProviderContract.Columns.T_NEWS_K_TARGET_UUID);
		if (!TextUtils.isEmpty(filterSelection)) {
			sqlSelectionSb.append(filterSelection);
		}
		if (sqlSelectionSb.length() > 0) {
			sqlSelectionSb.append(SqlUtils.AND); //
		}
		sqlSelectionSb.append(SqlUtils.getWhereInString(NewsProviderContract.Columns.T_NEWS_K_LANGUAGE, provider.getNewsLanguages()));
		return getCachedNewsS(provider, uri, sqlSelectionSb.toString());
	}

	private static ArrayList<News> getCachedNewsS(NewsProviderContract provider, Uri uri, String selection) {
		ArrayList<News> cache = new ArrayList<News>();
		Cursor cursor = null;
		try {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getNewsDbTableName());
			qb.setProjectionMap(provider.getNewsProjectionMap());
			cursor = qb.query(provider.getDBHelper().getReadableDatabase(), NewsProviderContract.PROJECTION_NEWS, selection, null, null, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						cache.add(News.fromCursorStatic(cursor, provider.getAuthority()));
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

	public static Uri getNewsContentUri(NewsProviderContract provider) {
		return Uri.withAppendedPath(provider.getAuthorityUri(), NewsProviderContract.NEWS_PATH);
	}

	public static boolean deleteCachedNews(NewsProviderContract provider, Integer newsId) {
		if (newsId == null) {
			return false;
		}
		int deletedRows = 0;
		try {
			String selection = SqlUtils.getWhereEquals(NewsProviderContract.Columns.T_NEWS_K_ID, newsId);
			deletedRows = provider.getDBHelper().getWritableDatabase().delete(provider.getNewsDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while deleting cached news '%s'!", newsId);
		}
		return deletedRows > 0;
	}

	public static boolean purgeUselessCachedNews(NewsProviderContract provider) {
		long oldestLastUpdate = TimeUtils.currentTimeMillis() - provider.getNewsMaxValidityInMs();
		String selection = SqlUtils.getWhereInferior(NewsProviderContract.Columns.T_NEWS_K_LAST_UPDATE, oldestLastUpdate);
		int deletedRows = 0;
		try {
			deletedRows = provider.getDBHelper().getWritableDatabase().delete(provider.getNewsDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while deleting cached news!");
		}
		return deletedRows > 0;
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

		public static final String T_NEWS = "news";
		public static final String T_NEWS_K_ID = BaseColumns._ID;
		public static final String T_NEWS_K_UUID = "uuid";
		public static final String T_NEWS_K_SEVERITY = "severity";
		public static final String T_NEWS_K_NOTEWORTHY = "noteworthy";
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

		public static final String T_NEWS_SQL_CREATE = getSqlCreateBuilder(T_NEWS).build();
		public static final String T_NEWS_SQL_INSERT = getSqlInsertBuilder(T_NEWS).build();
		public static final String T_NEWS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_NEWS);

		public NewsDbHelper(Context context) {
			this(context, DB_NAME, getDbVersion(context));
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

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link NewsDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.news_db_version);
			}
			return dbVersion;
		}

		public static String getFkColumnName(String columnName) {
			return "fk" + "_" + columnName;
		}

		public static SqlUtils.SQLCreateBuilder getSqlCreateBuilder(String table) {
			SqlUtils.SQLCreateBuilder b = SqlUtils.SQLCreateBuilder.getNew(table) //
					.appendColumn(T_NEWS_K_ID, SqlUtils.INT_PK) //
					.appendColumn(T_NEWS_K_UUID, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_SEVERITY, SqlUtils.INT) //
					.appendColumn(T_NEWS_K_NOTEWORTHY, SqlUtils.INT) //
					.appendColumn(T_NEWS_K_LAST_UPDATE, SqlUtils.INT) //
					.appendColumn(T_NEWS_K_MAX_VALIDITY_IN_MS, SqlUtils.INT) //
					.appendColumn(T_NEWS_K_CREATED_AT, SqlUtils.INT) //
					.appendColumn(T_NEWS_K_TARGET_UUID, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_COLOR, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_AUTHOR_NAME, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_AUTHOR_USERNAME, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_AUTHOR_PICTURE_URL, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_AUTHOR_PROFILE_URL, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_TEXT, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_TEXT_HTML, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_WEB_URL, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_LANGUAGE, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_SOURCE_ID, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_SOURCE_LABEL, SqlUtils.TXT) //
			;
			return b;
		}

		public static SqlUtils.SQLInsertBuilder getSqlInsertBuilder(String table) {
			SqlUtils.SQLInsertBuilder b = SqlUtils.SQLInsertBuilder.getNew(table) //
					.appendColumn(T_NEWS_K_ID) //
					.appendColumn(T_NEWS_K_UUID) //
					.appendColumn(T_NEWS_K_SEVERITY) //
					.appendColumn(T_NEWS_K_NOTEWORTHY) //
					.appendColumn(T_NEWS_K_LAST_UPDATE) //
					.appendColumn(T_NEWS_K_MAX_VALIDITY_IN_MS) //
					.appendColumn(T_NEWS_K_CREATED_AT) //
					.appendColumn(T_NEWS_K_TARGET_UUID) //
					.appendColumn(T_NEWS_K_COLOR) //
					.appendColumn(T_NEWS_K_AUTHOR_NAME) //
					.appendColumn(T_NEWS_K_AUTHOR_USERNAME) //
					.appendColumn(T_NEWS_K_AUTHOR_PICTURE_URL) //
					.appendColumn(T_NEWS_K_AUTHOR_PROFILE_URL) //
					.appendColumn(T_NEWS_K_TEXT) //
					.appendColumn(T_NEWS_K_TEXT_HTML) //
					.appendColumn(T_NEWS_K_WEB_URL) //
					.appendColumn(T_NEWS_K_LANGUAGE) //
					.appendColumn(T_NEWS_K_SOURCE_ID) //
					.appendColumn(T_NEWS_K_SOURCE_LABEL) //
			;
			return b;
		}
	}
}
