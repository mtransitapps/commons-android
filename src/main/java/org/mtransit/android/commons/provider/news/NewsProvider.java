package org.mtransit.android.commons.provider.news;

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

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.provider.common.ContentProviderConstants;
import org.mtransit.android.commons.provider.common.MTContentProvider;
import org.mtransit.android.commons.provider.common.MTSQLiteOpenHelper;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.sql.SQLCreateBuilder;
import org.mtransit.commons.sql.SQLInsertBuilder;

import java.util.ArrayList;
import java.util.Iterator;

@SuppressLint("Registered")
public abstract class NewsProvider extends MTContentProvider implements NewsProviderContract {

	private static final String LOG_TAG = NewsProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	public static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	public static void append(@NonNull UriMatcher uriMatcher, @NonNull String authority) {
		uriMatcher.addURI(authority, NewsProviderContract.PING_PATH, ContentProviderConstants.PING);
		uriMatcher.addURI(authority, NewsProviderContract.NEWS_PATH, ContentProviderConstants.NEWS);
	}

	@Nullable
	private static String authority = null;

	/**
	 * Override if multiple {@link NewsProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.news_authority);
		}
		return authority;
	}

	@MainThread
	@Override
	public boolean onCreateMT() {
		return true;
	}

	@Nullable
	private NewsDbHelper dbHelper;
	private static int currentDbVersion = -1;

	@NonNull
	private NewsDbHelper getDBHelper(@NonNull Context context) {
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
		return NewsDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link NewsProvider} implementations in same app.
	 */
	@NonNull
	public NewsDbHelper getNewDbHelper(@NonNull Context context) {
		return new NewsDbHelper(context.getApplicationContext());
	}

	@NonNull
	private SQLiteOpenHelper getDBHelper() {
		return getDBHelper(requireContextCompat());
	}

	@NonNull
	@Override
	public SQLiteDatabase getReadDB() {
		return getDBHelper().getReadableDatabase();
	}

	@NonNull
	@Override
	public SQLiteDatabase getWriteDB() {
		return getDBHelper().getWritableDatabase();
	}

	@Nullable
	@Override
	public Cursor queryMT(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		return queryS(this, uri, selection);
	}

	@Nullable
	public static Cursor queryS(@NonNull NewsProviderContract provider, @NonNull Uri uri, @Nullable String selection) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		case ContentProviderConstants.NEWS:
			return getNews(provider, selection);
		default:
			return null; // not processed
		}
	}

	@Nullable
	private static Cursor getNews(@NonNull NewsProviderContract provider, @Nullable String selection) {
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

	@Nullable
	@Override
	public Cursor getNewsFromDB(@NonNull NewsProviderContract.Filter newsFilter) {
		return getDefaultNewsFromDB(newsFilter, this);
	}

	private static final String LATEST_NEWS_SORT_ORDER = SqlUtils.getSortOrderDescending(NewsProviderContract.Columns.T_NEWS_K_CREATED_AT);
	private static final String LATEST_NEWS_LIMIT = "100";

	@Nullable
	public static Cursor getDefaultNewsFromDB(@NonNull NewsProviderContract.Filter newsFilter, @NonNull NewsProviderContract provider) {
		try {
			String selection = newsFilter.getSqlSelection(
					NewsProviderContract.Columns.T_NEWS_K_UUID,
					NewsProviderContract.Columns.T_NEWS_K_TARGET_UUID,
					NewsProviderContract.Columns.T_NEWS_K_CREATED_AT
			);
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getNewsDbTableName());
			qb.setProjectionMap(provider.getNewsProjectionMap());
			return qb.query(provider.getReadDB(), provider.getNewsProjection(), selection, null, null, null, LATEST_NEWS_SORT_ORDER,
					LATEST_NEWS_LIMIT);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while loading news '%s'!", newsFilter);
			return null;
		}
	}

	private static Cursor getNewsCursor(ArrayList<News> news) {
		if (news == null) {
			return ContentProviderConstants.EMPTY_CURSOR;
		}
		MatrixCursor matrixCursor = new MatrixCursor(NewsProviderContract.PROJECTION_NEWS);
		for (News oneNews : news) {
			matrixCursor.addRow(oneNews.getCursorRow());
		}
		return matrixCursor;
	}

	@NonNull
	@Override
	public String getNewsDbTableName() {
		return NewsDbHelper.T_NEWS;
	}

	@NonNull
	@Override
	public String[] getNewsProjection() {
		return NewsProviderContract.PROJECTION_NEWS;
	}

	@Nullable
	private static ArrayMap<String, String> newsProjectionMap;

	@NonNull
	@Override
	public ArrayMap<String, String> getNewsProjectionMap() {
		if (newsProjectionMap == null) {
			newsProjectionMap = getNewNewsProjectionMap(getAUTHORITY(requireContextCompat()));
		}
		return newsProjectionMap;
	}

	public static ArrayMap<String, String> getNewNewsProjectionMap(String authority) {
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
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_IMAGE_URLS_COUNT, Columns.T_NEWS_K_IMAGE_URLS_COUNT) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_IMAGE_URL_INDEX + 0, Columns.T_NEWS_K_IMAGE_URL_INDEX + 0) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_IMAGE_URL_INDEX + 1, Columns.T_NEWS_K_IMAGE_URL_INDEX + 1) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_IMAGE_URL_INDEX + 2, Columns.T_NEWS_K_IMAGE_URL_INDEX + 2) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_IMAGE_URL_INDEX + 3, Columns.T_NEWS_K_IMAGE_URL_INDEX + 3) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_IMAGE_URL_INDEX + 4, Columns.T_NEWS_K_IMAGE_URL_INDEX + 4) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_IMAGE_URL_INDEX + 5, Columns.T_NEWS_K_IMAGE_URL_INDEX + 5) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_IMAGE_URL_INDEX + 6, Columns.T_NEWS_K_IMAGE_URL_INDEX + 6) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_IMAGE_URL_INDEX + 7, Columns.T_NEWS_K_IMAGE_URL_INDEX + 7) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_IMAGE_URL_INDEX + 8, Columns.T_NEWS_K_IMAGE_URL_INDEX + 8) //
				.appendTableColumn(NewsDbHelper.T_NEWS, NewsDbHelper.T_NEWS_K_IMAGE_URL_INDEX + 9, Columns.T_NEWS_K_IMAGE_URL_INDEX + 9) //
				.build();
	}

	@Nullable
	@Override
	public String getTypeMT(@NonNull Uri uri) {
		return getTypeS(this, uri);
	}

	@Nullable
	public static String getTypeS(@NonNull NewsProviderContract provider, @NonNull Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.NEWS:
			return StringUtils.EMPTY; // empty string = processed
		default:
			return null; // not processed
		}
	}

	@Override
	public int deleteMT(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
		MTLog.w(this, "The delete method is not available.");
		return 0;
	}

	@Override
	public int updateMT(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
		MTLog.w(this, "The update method is not available.");
		return 0;
	}

	@Nullable
	@Override
	public Uri insertMT(@NonNull Uri uri, @Nullable ContentValues values) {
		MTLog.w(this, "The insert method is not available.");
		return null;
	}

	@SuppressWarnings("UnusedReturnValue")
	public static synchronized int cacheNewsS(NewsProviderContract provider, ArrayList<News> newNews) {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = provider.getWriteDB();
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
			MTLog.w(LOG_TAG, e, "ERROR while applying batch update to the database!");
		} finally {
			SqlUtils.endTransaction(db);
		}
		return affectedRows;
	}

	@SuppressWarnings("unused")
	public static void cacheNewsS(@NonNull NewsProviderContract provider, @NonNull News newNews) {
		try {
			provider.getWriteDB().insert(provider.getNewsDbTableName(), NewsDbHelper.T_NEWS_K_ID, newNews.toContentValues());
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while inserting '%s' into cache!", newNews);
		}
	}

	@Nullable
	public static ArrayList<News> getCachedNewsS(@NonNull NewsProviderContract provider, @NonNull Filter newFilter) {
		Uri uri = getNewsContentUri(provider);
		String filterSelection = newFilter.getSqlSelection(
				NewsProviderContract.Columns.T_NEWS_K_UUID,
				NewsProviderContract.Columns.T_NEWS_K_TARGET_UUID,
				NewsProviderContract.Columns.T_NEWS_K_CREATED_AT
		);
		StringBuilder sqlSelectionSb = new StringBuilder();
		if (!TextUtils.isEmpty(filterSelection)) {
			sqlSelectionSb.append(filterSelection).append(SqlUtils.AND);
		}
		sqlSelectionSb.append(SqlUtils.getWhereInString(NewsProviderContract.Columns.T_NEWS_K_LANGUAGE, provider.getNewsLanguages()));
		return getCachedNewsS(provider, uri, sqlSelectionSb.toString());
	}

	@Nullable
	private static ArrayList<News> getCachedNewsS(@NonNull NewsProviderContract provider, @SuppressWarnings("unused") Uri uri, String selection) {
		ArrayList<News> cache = new ArrayList<>();
		Cursor cursor = null;
		try {
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getNewsDbTableName());
			qb.setProjectionMap(provider.getNewsProjectionMap());
			cursor = qb.query(provider.getReadDB(), provider.getNewsProjection(), selection, null, null, null, LATEST_NEWS_SORT_ORDER,
					LATEST_NEWS_LIMIT);
			if (cursor != null && cursor.getCount() > 0) {
				if (cursor.moveToFirst()) {
					do {
						cache.add(News.fromCursorStatic(cursor, provider.getAuthority()));
					} while (cursor.moveToNext());
				}
			}
			return cache;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error!");
			return null;
		} finally {
			SqlUtils.closeQuietly(cursor);
		}
	}

	private static Uri getNewsContentUri(@NonNull NewsProviderContract provider) {
		return Uri.withAppendedPath(provider.getAuthorityUri(), NewsProviderContract.NEWS_PATH);
	}

	public static boolean deleteCachedNews(@NonNull NewsProviderContract provider, @Nullable Integer newsId) {
		if (newsId == null) {
			return false;
		}
		int deletedRows = 0;
		try {
			String selection = SqlUtils.getWhereEquals(NewsProviderContract.Columns.T_NEWS_K_ID, newsId);
			deletedRows = provider.getWriteDB().delete(provider.getNewsDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while deleting cached news '%s'!", newsId);
		}
		return deletedRows > 0;
	}

	public static boolean purgeUselessCachedNews(@NonNull NewsProviderContract provider) {
		long oldestLastUpdate = TimeUtils.currentTimeMillis() - provider.getNewsMaxValidityInMs();
		String selection = SqlUtils.getWhereInferior(NewsProviderContract.Columns.T_NEWS_K_LAST_UPDATE, oldestLastUpdate);
		int deletedRows = 0;
		try {
			deletedRows = provider.getWriteDB().delete(provider.getNewsDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while deleting cached news!");
		}
		return deletedRows > 0;
	}

	public static class NewsDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = NewsDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link NewsDbHelper} implementations in same app.
		 */
		public static final String DB_NAME = "news.db";

		/**
		 * Override if multiple {@link NewsDbHelper} implementations in same app.
		 */
		@SuppressWarnings("unused")
		protected static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pNewsLastUpdate";

		public static final String T_NEWS = "news";
		static final String T_NEWS_K_ID = BaseColumns._ID;
		static final String T_NEWS_K_UUID = "uuid";
		static final String T_NEWS_K_SEVERITY = "severity";
		static final String T_NEWS_K_NOTEWORTHY = "noteworthy";
		static final String T_NEWS_K_LAST_UPDATE = "last_update";
		static final String T_NEWS_K_MAX_VALIDITY_IN_MS = "max_validity";
		static final String T_NEWS_K_CREATED_AT = "created_at";
		static final String T_NEWS_K_TARGET_UUID = "target";
		static final String T_NEWS_K_COLOR = "color";
		static final String T_NEWS_K_AUTHOR_NAME = "author_name";
		static final String T_NEWS_K_AUTHOR_USERNAME = "author_username";
		static final String T_NEWS_K_AUTHOR_PICTURE_URL = "author_picture_url";
		static final String T_NEWS_K_AUTHOR_PROFILE_URL = "author_profile_url";
		static final String T_NEWS_K_TEXT = "text";
		static final String T_NEWS_K_TEXT_HTML = "text_html";
		static final String T_NEWS_K_WEB_URL = "web_url";
		static final String T_NEWS_K_LANGUAGE = "lang";
		static final String T_NEWS_K_SOURCE_ID = "source_id";
		static final String T_NEWS_K_SOURCE_LABEL = "source_label";
		static final String T_NEWS_K_IMAGE_URLS_COUNT = "image_urls_count";
		static final String T_NEWS_K_IMAGE_URL_INDEX = "image_urls_";

		static final String T_NEWS_SQL_CREATE = getSqlCreateBuilder(T_NEWS).build();
		@SuppressWarnings("unused")
		static final String T_NEWS_SQL_INSERT = getSqlInsertBuilder(T_NEWS).build();
		static final String T_NEWS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_NEWS);

		public NewsDbHelper(Context context) {
			this(context, DB_NAME, getDbVersion(context));
		}

		public NewsDbHelper(Context context, String dbName, int dbVersion) {
			super(context, dbName, null, dbVersion);
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_NEWS_SQL_DROP);
			initAllDbTables(db);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_NEWS_SQL_CREATE);
		}

		/**
		 * Override if multiple {@link NewsDbHelper} implementations in same app.
		 */
		@NonNull
		public String getDbName() {
			return DB_NAME;
		}

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link NewsDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.news_db_version);
			}
			return dbVersion;
		}

		@SuppressWarnings("unused")
		@NonNull
		public static String getFkColumnName(@NonNull String columnName) {
			return "fk" + "_" + columnName;
		}

		@NonNull
		public static SQLCreateBuilder getSqlCreateBuilder(@NonNull String table) {
			return SQLCreateBuilder.getNew(table) //
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
					.appendColumn(T_NEWS_K_IMAGE_URLS_COUNT, SqlUtils.INT) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 0, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 1, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 2, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 3, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 4, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 5, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 6, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 7, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 8, SqlUtils.TXT) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 9, SqlUtils.TXT) //
					;
		}

		@NonNull
		static SQLInsertBuilder getSqlInsertBuilder(@NonNull String table) {
			return SQLInsertBuilder.getNew(table) //
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
					.appendColumn(T_NEWS_K_IMAGE_URLS_COUNT) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 0) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 1) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 2) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 3) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 4) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 5) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 6) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 7) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 8) //
					.appendColumn(T_NEWS_K_IMAGE_URL_INDEX + 9) //
					;
		}
	}
}
