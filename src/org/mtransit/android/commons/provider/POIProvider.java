package org.mtransit.android.commons.provider;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI.POIUtils;

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

public class POIProvider extends MTContentProvider implements POIProviderContract {

	private static final String TAG = POIProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final String POI_CONTENT_DIRECTORY = "poi";

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	public static void append(UriMatcher uriMatcher, String authority) {
		uriMatcher.addURI(authority, "ping", ContentProviderConstants.PING);
		uriMatcher.addURI(authority, POI_CONTENT_DIRECTORY, ContentProviderConstants.POI);
	}

	public static final String[] PROJECTION_POI_ALL_COLUMNS = null; // null = return all columns

	public static final String[] PROJECTION_POI = new String[] { POIColumns.T_POI_K_UUID_META, POIColumns.T_POI_K_ID, POIColumns.T_POI_K_NAME,
			POIColumns.T_POI_K_LAT, POIColumns.T_POI_K_LNG, POIColumns.T_POI_K_TYPE, POIColumns.T_POI_K_STATUS_TYPE, POIColumns.T_POI_K_ACTIONS_TYPE };

	private static POIDbHelper dbHelper;
	private static int currentDbVersion = -1;

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.poi_authority);
		}
		return authority;
	}

	@Override
	public UriMatcher getURIMATCHER() {
		return getURIMATCHER(getContext());
	}

	@Override
	public boolean onCreateMT() {
		ping();
		return true;
	}

	@Override
	public void ping() {
		PackageManagerUtils.removeModuleLauncherIcon(getContext());
	}

	private POIDbHelper getDBHelper(Context context) {
		if (dbHelper == null) {
			dbHelper = getNewDbHelper(context);
			currentDbVersion = getCurrentDbVersion();
		} else {
			try {
				if (currentDbVersion != getCurrentDbVersion()) {
					dbHelper.close();
					dbHelper = null;
					return getDBHelper(context);
				}
			} catch (Throwable t) {
				MTLog.d(this, t, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return POIDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link POIProvider} implementations in same app.
	 */
	public POIDbHelper getNewDbHelper(Context context) {
		return new POIDbHelper(context.getApplicationContext());
	}

	@Override
	public Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return queryS(this, uri, projection, selection, selectionArgs, sortOrder);
	}

	public static Cursor queryS(POIProviderContract provider, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		switch (provider.getURIMATCHER().match(uri)) {
		case ContentProviderConstants.PING:
			provider.ping();
			return new MatrixCursor(new String[] {}); // empty cursor = processed
		case ContentProviderConstants.POI:
			return getPOI(provider, selection);
		default:
			return null; // not processed
		}
	}

	private static Cursor getPOI(POIProviderContract provider, String selection) {
		final POIFilter poiFilter = POIFilter.fromJSONString(selection);
		return provider.getPOI(poiFilter);
	}

	@Override
	public Cursor getPOI(POIFilter poiFilter) {
		return getPOIFromDB(poiFilter, this);
	}

	@Override
	public Cursor getPOIFromDB(POIFilter poiFilter) {
		return getPOIFromDB(poiFilter, this);
	}

	public static Cursor getPOIFromDB(POIFilter poiFilter, POIProviderContract provider) {
		try {
			final String selection = poiFilter.getSqlSelection(POIColumns.T_POI_K_UUID_META, POIColumns.T_POI_K_LAT, POIColumns.T_POI_K_LNG);
			SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
			qb.setTables(provider.getPOITable());
			qb.setProjectionMap(provider.getPOIProjectionMap());
			Cursor cursor = qb.query(provider.getDBHelper().getReadableDatabase(), PROJECTION_POI, selection, null, null, null, null, null);
			return cursor;
		} catch (Throwable t) {
			MTLog.w(TAG, t, "Error while loading POIs '%s'!", poiFilter);
			return null;
		}
	}

	private static Map<String, String> poiProjectionMap;

	@Override
	public Map<String, String> getPOIProjectionMap() {
		if (poiProjectionMap == null) {
			poiProjectionMap = getNewPoiProjectionMap(getAUTHORITY(getContext()));
		}
		return poiProjectionMap;
	}

	public static Map<String, String> getNewPoiProjectionMap(String authority) {
		HashMap<String, String> poiProjectionMap = new HashMap<String, String>();
		poiProjectionMap.put(POIColumns.T_POI_K_UUID_META, SqlUtils.concatenate("'" + POIUtils.UID_SEPARATOR + "'", //
				"'" + authority + "'", //
				POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_ID //
		) + " AS " + POIColumns.T_POI_K_UUID_META);
		poiProjectionMap.put(POIColumns.T_POI_K_ID, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_ID + " AS " + POIColumns.T_POI_K_ID);
		poiProjectionMap.put(POIColumns.T_POI_K_NAME, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_NAME + " AS " + POIColumns.T_POI_K_NAME);
		poiProjectionMap.put(POIColumns.T_POI_K_LAT, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_LAT + " AS " + POIColumns.T_POI_K_LAT);
		poiProjectionMap.put(POIColumns.T_POI_K_LNG, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_LNG + " AS " + POIColumns.T_POI_K_LNG);
		poiProjectionMap.put(POIColumns.T_POI_K_TYPE, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_TYPE + " AS " + POIColumns.T_POI_K_TYPE);
		poiProjectionMap.put(POIColumns.T_POI_K_STATUS_TYPE, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_STATUS_TYPE + " AS "
				+ POIColumns.T_POI_K_STATUS_TYPE);
		poiProjectionMap.put(POIColumns.T_POI_K_ACTIONS_TYPE, POIDbHelper.T_POI + "." + POIDbHelper.T_POI_K_ACTIONS_TYPE + " AS "
				+ POIColumns.T_POI_K_ACTIONS_TYPE);
		return poiProjectionMap;
	}

	@Override
	public String getPOITable() {
		return POIDbHelper.T_POI;
	}

	public static String getSortOrderS(POIProviderContract provider, Uri uri) {
		switch (provider.getURIMATCHER().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.POI:
			return ""; // empty string = processed
		default:
			return null; // not processed
		}
	}

	@Override
	public String getTypeMT(Uri uri) {
		return getTypeS(this, uri);
	}

	public static String getTypeS(POIProviderContract provider, Uri uri) {
		switch (provider.getURIMATCHER().match(uri)) {
		case ContentProviderConstants.PING:
		case ContentProviderConstants.POI:
			return ""; // empty string = processed
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

	protected static synchronized int insertDefaultPOIs(POIProviderContract provider, Collection<DefaultPOI> defaultPOIs) {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = provider.getDBHelper().getWritableDatabase();
			db.beginTransaction(); // start the transaction
			if (defaultPOIs != null) {
				for (DefaultPOI defaultPOI : defaultPOIs) {
					final long rowId = db.insert(provider.getPOITable(), POIDbHelper.T_POI_K_ID, defaultPOI.toContentValues());
					if (rowId > 0) {
						affectedRows++;
					}
				}
			}
			db.setTransactionSuccessful(); // mark the transaction as successful
		} catch (Exception e) {
			MTLog.w(TAG, e, "ERROR while applying batch update to the database!");
		} finally {
			try {
				if (db != null) {
					db.endTransaction(); // end the transaction
					db.close();
				}
			} catch (Exception e) {
				MTLog.w(TAG, e, "ERROR while closing the new database!");
			}
		}
		return affectedRows;
	}

	public static class POIColumns {

		public static final String T_POI_K_ID = BaseColumns._ID;
		public static final String T_POI_K_UUID_META = "uuid";
		public static final String T_POI_K_NAME = "name";
		public static final String T_POI_K_LAT = "lat";
		public static final String T_POI_K_LNG = "lng";
		public static final String T_POI_K_TYPE = "type";
		public static final String T_POI_K_STATUS_TYPE = "statustype";
		public static final String T_POI_K_ACTIONS_TYPE = "actionstype";

	}

}
