package org.mtransit.android.commons.provider;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI.POIUtils;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.sql.SQLCreateBuilder;
import org.mtransit.commons.sql.SQLInsertBuilder;

import java.util.Collection;

@SuppressLint("Registered")
public class POIProvider extends MTContentProvider implements POIProviderContract {

	private static final String LOG_TAG = POIProvider.class.getSimpleName();

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
		uriMatcher.addURI(authority, POIProviderContract.PING_PATH, ContentProviderConstants.PING);
		uriMatcher.addURI(authority, POIProviderContract.POI_PATH, ContentProviderConstants.POI);
		uriMatcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY, ContentProviderConstants.SEARCH_SUGGEST_EMPTY);
		uriMatcher.addURI(authority, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", ContentProviderConstants.SEARCH_SUGGEST_QUERY);
	}

	private static final String[] SUGGEST_SEARCHABLE_COLUMNS = new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1};

	private static final String[] SEARCHABLE_LIKE_COLUMNS = new String[]{POIProviderContract.Columns.T_POI_K_NAME};

	private static final String[] SEARCHABLE_EQUALS_COLUMNS = new String[]{};

	public static final ArrayMap<String, String> POI_SEARCH_SUGGEST_PROJECTION_MAP = SqlUtils.ProjectionMapBuilder.getNew() //
			.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_NAME, SearchManager.SUGGEST_COLUMN_TEXT_1) //
			.build();

	private POIDbHelper dbHelper;
	private static int currentDbVersion = -1;

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(@NonNull Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.poi_authority);
		}
		return authority;
	}

	private static Integer dataSourceTypeId = null;

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	private static int getTYPE_ID(Context context) {
		if (dataSourceTypeId == null) {
			dataSourceTypeId = context.getResources().getInteger(R.integer.poi_agency_type);
		}
		return dataSourceTypeId;
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(requireContextCompat());
	}

	@MainThread
	@Override
	public boolean onCreateMT() {
		return true;
	}

	@NonNull
	private POIDbHelper getDBHelper(@NonNull Context context) {
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

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return POIDbHelper.getDbVersion();
	}

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	@NonNull
	public POIDbHelper getNewDbHelper(@NonNull Context context) {
		return new POIDbHelper(context.getApplicationContext());
	}

	@Nullable
	@Override
	public Cursor queryMT(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		return queryS(this, uri, selection);
	}

	@Nullable
	public static Cursor queryS(@NonNull POIProviderContract provider, @NonNull Uri uri, @Nullable String selection) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
			return ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		case ContentProviderConstants.POI:
			return getPOI(provider, selection);
		case ContentProviderConstants.SEARCH_SUGGEST_EMPTY:
			return getSearchSuggest(provider, null);
		case ContentProviderConstants.SEARCH_SUGGEST_QUERY:
			return getSearchSuggest(provider, uri.getLastPathSegment());
		default:
			return null; // not processed
		}
	}

	@NonNull
	private static Cursor getSearchSuggest(@NonNull POIProviderContract provider, @Nullable String query) {
		Cursor cursor = provider.getSearchSuggest(query);
		if (cursor == null) {
			cursor = ContentProviderConstants.EMPTY_CURSOR; // empty cursor = processed
		}
		return cursor;
	}

	private static final long POI_MAX_VALIDITY_IN_MS = ProviderContract.MAX_CACHE_VALIDITY_MS;

	private static final long POI_VALIDITY_IN_MS = ProviderContract.MAX_CACHE_VALIDITY_MS;

	@Override
	public long getPOIMaxValidityInMs() {
		return POI_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getPOIValidityInMs() {
		return POI_VALIDITY_IN_MS;
	}

	@Nullable
	@Override
	public Cursor getSearchSuggest(String query) {
		return getDefaultSearchSuggest(query, this);
	}

	@Nullable
	public static Cursor getDefaultSearchSuggest(@Nullable String query, @NonNull POIProviderContract provider) {
		try {
			String selection = POIProviderContract.Filter.getSearchSelection(new String[]{query}, SUGGEST_SEARCHABLE_COLUMNS, null);
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getSearchSuggestTable());
			qb.setProjectionMap(provider.getSearchSuggestProjectionMap());
			return qb.query(provider.getReadDB(), PROJECTION_POI_SEARCH_SUGGEST, selection, null, null, null, null, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while loading search suggests '%s'!", query);
			return null;
		}
	}

	@Nullable
	private static Cursor getPOI(@NonNull POIProviderContract provider, @Nullable String selection) {
		final POIProviderContract.Filter poiFilter = POIProviderContract.Filter.fromJSONString(selection);
		return provider.getPOI(poiFilter);
	}

	@Override
	public Cursor getPOI(POIProviderContract.Filter poiFilter) {
		return getDefaultPOIFromDB(poiFilter, this);
	}

	@Nullable
	@Override
	public Cursor getPOIFromDB(@Nullable POIProviderContract.Filter poiFilter) {
		return getDefaultPOIFromDB(poiFilter, this);
	}

	@Nullable
	public static Cursor getDefaultPOIFromDB(@Nullable POIProviderContract.Filter poiFilter, @NonNull POIProviderContract provider) {
		try {
			if (poiFilter == null) {
				return null;
			}
			String selection = poiFilter.getSqlSelection(POIProviderContract.Columns.T_POI_K_UUID_META, POIProviderContract.Columns.T_POI_K_LAT,
					POIProviderContract.Columns.T_POI_K_LNG, SEARCHABLE_LIKE_COLUMNS, SEARCHABLE_EQUALS_COLUMNS);
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getPOITable());
			ArrayMap<String, String> poiProjectionMap = provider.getPOIProjectionMap();
			if (POIProviderContract.Filter.isSearchKeywords(poiFilter) && poiFilter.getSearchKeywords() != null) {
				SqlUtils.appendProjection(poiProjectionMap,
						POIProviderContract.Filter.getSearchSelectionScore(poiFilter.getSearchKeywords(), SEARCHABLE_LIKE_COLUMNS, SEARCHABLE_EQUALS_COLUMNS),
						POIProviderContract.Columns.T_POI_K_SCORE_META_OPT);
			}
			qb.setProjectionMap(poiProjectionMap);
			String[] poiProjection = provider.getPOIProjection();
			if (POIProviderContract.Filter.isSearchKeywords(poiFilter)) {
				poiProjection = ArrayUtils.addAll(poiProjection, new String[]{POIProviderContract.Columns.T_POI_K_SCORE_META_OPT});
			}
			String groupBy = null;
			if (POIProviderContract.Filter.isSearchKeywords(poiFilter)) {
				groupBy = POIProviderContract.Columns.T_POI_K_UUID_META;
			}
			String sortOrder = poiFilter.getExtraString(POIProviderContract.POI_FILTER_EXTRA_SORT_ORDER, null);
			if (POIProviderContract.Filter.isSearchKeywords(poiFilter)) {
				sortOrder = SqlUtils.getSortOrderDescending(POIProviderContract.Columns.T_POI_K_SCORE_META_OPT);
			}
			return qb.query(provider.getReadDB(), poiProjection, selection, null, groupBy, null, sortOrder, null);
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while loading POIs '%s'!", poiFilter);
			return null;
		}
	}

	@NonNull
	@Override
	public String[] getPOIProjection() {
		return PROJECTION_POI;
	}

	private static ArrayMap<String, String> poiProjectionMap;

	@NonNull
	@Override
	public ArrayMap<String, String> getPOIProjectionMap() {
		if (poiProjectionMap == null) {
			poiProjectionMap = getNewPoiProjectionMap(getAUTHORITY(requireContextCompat()), getTYPE_ID(requireContextCompat()));
		}
		return poiProjectionMap;
	}

	@NonNull
	public static ArrayMap<String, String> getNewPoiProjectionMap(@NonNull String authority, int dataSourceTypeId) {
		final SqlUtils.ProjectionMapBuilder builder = SqlUtils.ProjectionMapBuilder.getNew() //
				.appendValue(SqlUtils.concatenate( //
						SqlUtils.escapeString(POIUtils.UID_SEPARATOR), //
						SqlUtils.escapeString(authority), //
						SqlUtils.getTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_ID) //
				), Columns.T_POI_K_UUID_META) //
				.appendValue(dataSourceTypeId, Columns.T_POI_K_DST_ID_META) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_ID, Columns.T_POI_K_ID) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_NAME, Columns.T_POI_K_NAME) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_LAT, Columns.T_POI_K_LAT) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_LNG, Columns.T_POI_K_LNG) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_TYPE, Columns.T_POI_K_TYPE) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_STATUS_TYPE, Columns.T_POI_K_STATUS_TYPE) //
				.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_ACTIONS_TYPE, Columns.T_POI_K_ACTIONS_TYPE) //
				;
		if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
			builder.appendTableColumn(POIDbHelper.T_POI, POIDbHelper.T_POI_K_ACCESSIBLE, Columns.T_POI_K_ACCESSIBLE); //
		}
		return builder.build();
	}

	@NonNull
	@Override
	public String getPOITable() {
		return POIDbHelper.T_POI;
	}

	@Override
	public String getSearchSuggestTable() {
		return getPOITable();
	}

	@Nullable
	@Override
	public ArrayMap<String, String> getSearchSuggestProjectionMap() {
		return POIProvider.POI_SEARCH_SUGGEST_PROJECTION_MAP;
	}

	public static String getSortOrderS(POIProviderContract provider, Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.POI:
		case ContentProviderConstants.SEARCH_SUGGEST_EMPTY:
		case ContentProviderConstants.SEARCH_SUGGEST_QUERY:
			return StringUtils.EMPTY; // empty string = processed
		default:
			return null; // not processed
		}
	}

	@Override
	public String getTypeMT(@NonNull Uri uri) {
		return getTypeS(this, uri);
	}

	public static String getTypeS(POIProviderContract provider, Uri uri) {
		switch (provider.getURI_MATCHER().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.POI:
			return StringUtils.EMPTY; // empty string = processed
		case ContentProviderConstants.SEARCH_SUGGEST_EMPTY:
		case ContentProviderConstants.SEARCH_SUGGEST_QUERY:
			return SearchManager.SUGGEST_MIME_TYPE;
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
	public Uri insertMT(@NonNull Uri uri, ContentValues values) {
		MTLog.w(this, "The insert method is not available.");
		return null;
	}

	protected static synchronized int insertDefaultPOIs(@NonNull POIProviderContract provider, Collection<DefaultPOI> defaultPOIs) {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = provider.getWriteDB();
			db.beginTransaction(); // start the transaction
			if (defaultPOIs != null) {
				for (DefaultPOI defaultPOI : defaultPOIs) {
					long rowId = db.insert(provider.getPOITable(), POIDbHelper.T_POI_K_ID, defaultPOI.toContentValues());
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

	@SuppressWarnings({"WeakerAccess", "unused"})
	public static class POIDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = POIDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link POIDbHelper} implementations in same app.
		 */
		public static final String DB_NAME = "poi.db";

		public static final int DB_VERSION = 3;

		public static final String T_POI = "poi";
		public static final String T_POI_K_ID = BaseColumns._ID;
		public static final String T_POI_K_NAME = "name";
		public static final String T_POI_K_LAT = "lat";
		public static final String T_POI_K_LNG = "lng";
		public static final String T_POI_K_ACCESSIBLE = "a11y";
		public static final String T_POI_K_TYPE = "type";
		public static final String T_POI_K_STATUS_TYPE = "statustype";
		public static final String T_POI_K_ACTIONS_TYPE = "actionstype";

		public static final String T_POI_SQL_CREATE = getSqlCreateBuilder(T_POI).build();
		public static final String T_POI_SQL_INSERT = getSqlInsertBuilder(T_POI).build();
		public static final String T_POI_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_POI);

		public POIDbHelper(@Nullable Context context) {
			this(context, DB_NAME, DB_VERSION);
		}

		public POIDbHelper(@Nullable Context context, @Nullable String dbName, int dbVersion) {
			super(context, dbName, null, dbVersion);
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_POI_SQL_DROP);
			initAllDbTables(db);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_POI_SQL_CREATE);
		}

		/**
		 * Override if multiple {@link POIDbHelper} implementations in same app.
		 */
		@NonNull
		public String getDbName() {
			return DB_NAME;
		}

		/**
		 * Override if multiple {@link POIDbHelper} in same app.
		 */
		public static int getDbVersion() {
			return DB_VERSION;
		}

		@NonNull
		public static String getFkColumnName(@NonNull String columnName) {
			return "fk" + "_" + columnName;
		}

		@NonNull
		public static SQLCreateBuilder getSqlCreateBuilder(@NonNull String table) {
			final SQLCreateBuilder builder = SQLCreateBuilder.getNew(table) //
					.appendColumn(T_POI_K_ID, SqlUtils.INT_PK) //
					.appendColumn(T_POI_K_NAME, SqlUtils.TXT) //
					.appendColumn(T_POI_K_LAT, SqlUtils.REAL) //
					.appendColumn(T_POI_K_LNG, SqlUtils.REAL) //
					.appendColumn(T_POI_K_TYPE, SqlUtils.INT) //
					.appendColumn(T_POI_K_STATUS_TYPE, SqlUtils.INT) //
					.appendColumn(T_POI_K_ACTIONS_TYPE, SqlUtils.INT);
			if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
				builder.appendColumn(T_POI_K_ACCESSIBLE, SqlUtils.INT); //
			}
			return builder;
		}

		@NonNull
		public static SQLInsertBuilder getSqlInsertBuilder(@NonNull String table) {
			final SQLInsertBuilder builder = SQLInsertBuilder.getNew(table) //
					.appendColumn(T_POI_K_ID) //
					.appendColumn(T_POI_K_NAME)//
					.appendColumn(T_POI_K_LAT) //
					.appendColumn(T_POI_K_LNG) //
					.appendColumn(T_POI_K_TYPE) //
					.appendColumn(T_POI_K_STATUS_TYPE) //
					.appendColumn(T_POI_K_ACTIONS_TYPE);
			if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
				builder.appendColumn(T_POI_K_ACCESSIBLE); //
			}
			return builder;
		}
	}
}
