package org.mtransit.android.commons.provider;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.ScheduleTimestamps;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

@SuppressLint("Registered")
public class GTFSProvider extends AgencyProvider implements POIProviderContract, StatusProviderContract, ScheduleTimestampsProviderContract,
		GTFSProviderContract {

	private static final String TAG = GTFSProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	protected static final int ROUTE_LOGO = 10;

	private static GTFSProviderDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = AgencyProvider.getNewUriMatcher(authority);
		GTFSStatusProvider.append(URI_MATCHER, authority);
		GTFSPOIProvider.append(URI_MATCHER, authority);
		GTFSScheduleTimestampsProvider.append(URI_MATCHER, authority);
		GTFSRTSProvider.append(URI_MATCHER, authority);
		//
		URI_MATCHER.addURI(authority, GTFSProviderContract.ROUTE_LOGO_PATH, ROUTE_LOGO);
		return URI_MATCHER;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.gtfs_rts_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public static Uri getAUTHORITYURI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
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

	private GTFSProviderDbHelper getDBHelper(Context context) {
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

	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		return GTFSStatusProvider.getStatusValidityInMs(inFocus);
	}

	@Override
	public long getStatusMaxValidityInMs() {
		return GTFSStatusProvider.getStatusMaxValidityInMs();
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		return GTFSStatusProvider.getMinDurationBetweenRefreshInMs(inFocus);
	}

	@Override
	public POIStatus getNewStatus(StatusProviderContract.Filter statusFilter) {
		return GTFSStatusProvider.getNewStatus(this, statusFilter);
	}

	@Override
	public ScheduleTimestamps getScheduleTimestamps(ScheduleTimestampsProviderContract.Filter filter) {
		return GTFSScheduleTimestampsProvider.getScheduleTimestamps(this, filter);
	}

	@Override
	public void cacheStatus(POIStatus newStatusToCache) {
		GTFSStatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Override
	public POIStatus getCachedStatus(StatusProviderContract.Filter statusFilter) {
		return GTFSStatusProvider.getCachedStatus(this, statusFilter);
	}

	@Override
	public boolean purgeUselessCachedStatuses() {
		return GTFSStatusProvider.purgeUselessCachedStatuses(this);
	}

	@Override
	public boolean deleteCachedStatus(int cachedStatusId) {
		return GTFSStatusProvider.deleteCachedStatus(this, cachedStatusId);
	}

	@Override
	public String getStatusDbTableName() {
		return GTFSStatusProvider.getStatusDbTableName(this);
	}

	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITYURI(getContext());
	}

	@Override
	public Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor cursor = super.queryMT(uri, projection, selection, selectionArgs, sortOrder);
		if (cursor != null) {
			return cursor;
		}
		cursor = GTFSPOIProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		cursor = GTFSStatusProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		cursor = GTFSScheduleTimestampsProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		cursor = GTFSRTSProvider.queryS(this, uri, projection, selection, selectionArgs, sortOrder);
		if (cursor != null) {
			return cursor;
		}
		switch (getURIMATCHER(getContext()).match(uri)) {
		case ROUTE_LOGO:
			MTLog.v(this, "query>ROUTE_LOGO");
			return getRouteLogo();
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
		}
	}

	@Override
	public Cursor getSearchSuggest(String query) {
		return GTFSPOIProvider.getSearchSuggest(this, query);
	}

	@Override
	public String getSearchSuggestTable() {
		return GTFSPOIProvider.getSearchSuggestTable(this);
	}

	@Override
	public ArrayMap<String, String> getSearchSuggestProjectionMap() {
		return GTFSPOIProvider.getSearchSuggestProjectionMap(this);
	}

	@Override
	public long getPOIMaxValidityInMs() {
		return GTFSPOIProvider.getPOIMaxValidityInMs(this);
	}

	@Override
	public long getPOIValidityInMs() {
		return GTFSPOIProvider.getPOIValidityInMs(this);
	}

	@Override
	public Cursor getPOI(POIProviderContract.Filter poiFilter) {
		return GTFSPOIProvider.getPOI(this, poiFilter);
	}

	@Override
	public Cursor getPOIFromDB(POIProviderContract.Filter poiFilter) {
		return GTFSPOIProvider.getPOIFromDB(this, poiFilter);
	}

	@Override
	public String[] getPOIProjection() {
		return GTFSPOIProvider.getPOIProjection(this);
	}

	@Override
	public ArrayMap<String, String> getPOIProjectionMap() {
		return GTFSPOIProvider.getPOIProjectionMap(this);
	}

	@Override
	public String getPOITable() {
		return GTFSPOIProvider.getPOITable(this);
	}

	@Override
	public String getSortOrder(Uri uri) {
		String sortOrder = GTFSPOIProvider.getSortOrderS(this, uri);
		if (sortOrder != null) {
			return sortOrder;
		}
		sortOrder = GTFSStatusProvider.getSortOrderS(this, uri);
		if (sortOrder != null) {
			return sortOrder;
		}
		sortOrder = GTFSRTSProvider.getSortOrderS(this, uri);
		if (sortOrder != null) {
			return sortOrder;
		}
		switch (getURIMATCHER(getContext()).match(uri)) {
		case ROUTE_LOGO:
			return null;
		default:
			return super.getSortOrder(uri);
		}
	}

	@Override
	public String getTypeMT(Uri uri) {
		String type = GTFSPOIProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		type = GTFSStatusProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		type = GTFSRTSProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		switch (getURIMATCHER(getContext()).match(uri)) {
		case ROUTE_LOGO:
			return null;
		default:
			return super.getTypeMT(uri);
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

	@Override
	public boolean isAgencyDeployed() {
		return SqlUtils.isDbExist(getContext(), getDbName());
	}

	@Override
	public boolean isAgencySetupRequired() {
		boolean setupRequired = false;
		if (currentDbVersion > 0 && currentDbVersion != getCurrentDbVersion()) {
			setupRequired = true; // live update required => update
		} else if (!SqlUtils.isDbExist(getContext(), getDbName())) {
			setupRequired = true; // not deployed => initialization
		} else if (SqlUtils.getCurrentDbVersion(getContext(), getDbName()) != getCurrentDbVersion()) {
			setupRequired = true; // update required => update
		}
		return setupRequired;
	}

	@Override
	public UriMatcher getAgencyUriMatcher() {
		return getURIMATCHER(getContext());
	}

	@Override
	public int getStatusType() {
		return GTFSStatusProvider.getStatusType(this);
	}

	@Override
	public int getAgencyVersion() {
		return getCurrentDbVersion();
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	public String getDbName() {
		return GTFSProviderDbHelper.DB_NAME;
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@Override
	public int getAgencyLabelResId() {
		return R.string.gtfs_rts_label;
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@Override
	public String getAgencyColorString(Context context) {
		return context.getString(R.string.gtfs_rts_color);
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@Override
	public int getAgencyShortNameResId() {
		return R.string.gtfs_rts_short_name;
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@Override
	public LocationUtils.Area getAgencyArea(Context context) {
		String minLatS = context.getString(R.string.gtfs_rts_area_min_lat);
		double minLat = TextUtils.isEmpty(minLatS) ? LocationUtils.MIN_LAT : Double.parseDouble(minLatS);
		String maxLatS = context.getString(R.string.gtfs_rts_area_max_lat);
		double maxLat = TextUtils.isEmpty(maxLatS) ? LocationUtils.MAX_LAT : Double.parseDouble(maxLatS);
		String minLngS = context.getString(R.string.gtfs_rts_area_min_lng);
		double minLng = TextUtils.isEmpty(minLngS) ? LocationUtils.MIN_LNG : Double.parseDouble(minLngS);
		String maxLngS = context.getString(R.string.gtfs_rts_area_max_lng);
		double maxLng = TextUtils.isEmpty(maxLngS) ? LocationUtils.MAX_LNG : Double.parseDouble(maxLngS);
		return new LocationUtils.Area(minLat, maxLat, minLng, maxLng);
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return GTFSProviderDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	public GTFSProviderDbHelper getNewDbHelper(Context context) {
		return new GTFSProviderDbHelper(context.getApplicationContext());
	}

	private static String routeLogo = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	private Cursor getRouteLogo() {
		if (routeLogo == null) {
			routeLogo = readRouteLogo();
		}
		if (routeLogo == null || routeLogo.length() == 0) {
			return null;
		}
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "routeLogo" });
		matrixCursor.addRow(new Object[] { routeLogo });
		return matrixCursor;
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	private String readRouteLogo() {
		BufferedReader br = null;
		try {
			StringBuilder routeLogoSb = new StringBuilder();
			String line;
			br = new BufferedReader(new InputStreamReader(getContext().getResources().openRawResource(R.raw.gtfs_rts_route_logo), FileUtils.UTF_8), 8192);
			while ((line = br.readLine()) != null) {
				routeLogoSb.append(line);
			}
			return routeLogoSb.toString();
		} catch (Exception e) {
			MTLog.w(this, e, "Error while loading route logo!");
			return StringUtils.EMPTY; // empty string = done
		} finally {
			FileUtils.closeQuietly(br);
		}
	}
}
