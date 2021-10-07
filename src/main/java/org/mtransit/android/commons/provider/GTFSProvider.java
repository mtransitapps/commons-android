package org.mtransit.android.commons.provider;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.collection.ArrayMap;

import org.mtransit.android.commons.AppUpdateUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.ScheduleTimestamps;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.mtransit.commons.FeatureFlags.F_APP_UPDATE;

@SuppressLint("Registered")
public class GTFSProvider extends AgencyProvider implements POIProviderContract, StatusProviderContract, ScheduleTimestampsProviderContract,
		GTFSProviderContract {

	private static final String LOG_TAG = GTFSProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	protected static final int ROUTE_LOGO = 10;

	@Nullable
	private static GTFSProviderDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	private static UriMatcher getURIMATCHER(@NonNull Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		//noinspection ConstantConditions // TODO requireContext()
		return getURIMATCHER(getContext());
	}

	@NonNull
	public static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = AgencyProvider.getNewUriMatcher(authority);
		GTFSStatusProvider.append(URI_MATCHER, authority);
		GTFSPOIProvider.append(URI_MATCHER, authority);
		GTFSScheduleTimestampsProvider.append(URI_MATCHER, authority);
		GTFSRTSProvider.append(URI_MATCHER, authority);
		//
		URI_MATCHER.addURI(authority, GTFSProviderContract.ROUTE_LOGO_PATH, ROUTE_LOGO);
		return URI_MATCHER;
	}

	@Nullable
	private static String authority = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	public static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.gtfs_rts_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITYURI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String areaMinLat = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	protected static String getAREA_MIN_LAT(@NonNull Context context) {
		GTFSCurrentNextProvider.checkForNextData(context);
		if (areaMinLat == null) {
			if (GTFSCurrentNextProvider.hasCurrentData(context)) {
				if (GTFSCurrentNextProvider.isNextData(context)) {
					areaMinLat = context.getResources().getString(R.string.next_gtfs_rts_area_min_lat);
				} else { // CURRENT = default
					areaMinLat = context.getResources().getString(R.string.current_gtfs_rts_area_min_lat);
				}
			} else {
				areaMinLat = context.getResources().getString(R.string.gtfs_rts_area_min_lat);
			}
		}
		return areaMinLat;
	}

	@Nullable
	private static String areaMaxLat = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	protected static String getAREA_MAX_LAT(@NonNull Context context) {
		GTFSCurrentNextProvider.checkForNextData(context);
		if (areaMaxLat == null) {
			if (GTFSCurrentNextProvider.hasCurrentData(context)) {
				if (GTFSCurrentNextProvider.isNextData(context)) {
					areaMaxLat = context.getResources().getString(R.string.next_gtfs_rts_area_max_lat);
				} else { // CURRENT = default
					areaMaxLat = context.getResources().getString(R.string.current_gtfs_rts_area_max_lat);
				}
			} else {
				areaMaxLat = context.getResources().getString(R.string.gtfs_rts_area_max_lat);
			}
		}
		return areaMaxLat;
	}

	@Nullable
	private static String areaMinLng = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	protected static String getAREA_MIN_LNG(@NonNull Context context) {
		GTFSCurrentNextProvider.checkForNextData(context);
		if (areaMinLng == null) {
			if (GTFSCurrentNextProvider.hasCurrentData(context)) {
				if (GTFSCurrentNextProvider.isNextData(context)) {
					areaMinLng = context.getResources().getString(R.string.next_gtfs_rts_area_min_lng);
				} else { // CURRENT = default
					areaMinLng = context.getResources().getString(R.string.current_gtfs_rts_area_min_lng);
				}
			} else {
				areaMinLng = context.getResources().getString(R.string.gtfs_rts_area_min_lng);
			}
		}
		return areaMinLng;
	}

	@Nullable
	private static String areaMaxLng = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	protected static String getAREA_MAX_LNG(@NonNull Context context) {
		GTFSCurrentNextProvider.checkForNextData(context);
		if (areaMaxLng == null) {
			if (GTFSCurrentNextProvider.hasCurrentData(context)) {
				if (GTFSCurrentNextProvider.isNextData(context)) {
					areaMaxLng = context.getResources().getString(R.string.next_gtfs_rts_area_max_lng);
				} else { // CURRENT = default
					areaMaxLng = context.getResources().getString(R.string.current_gtfs_rts_area_max_lng);
				}
			} else {
				areaMaxLng = context.getResources().getString(R.string.gtfs_rts_area_max_lng);
			}
		}
		return areaMaxLng;
	}

	public static void onCurrentNextDataChange(@NonNull Context context) {
		areaMinLat = null;
		areaMaxLat = null;
		areaMinLng = null;
		areaMaxLng = null;
		if (dbHelper != null) {
			dbHelper.close();
			dbHelper = null;
		}
		SqlUtils.deleteDb(context, GTFSProviderDbHelper.DB_NAME);
	}

	@Override
	public boolean onCreateMT() {
		ping();
		return super.onCreateMT();
	}

	@Override
	public void ping() {
		// DO NOTHING
	}

	@NonNull
	private GTFSProviderDbHelper getDBHelper(@NonNull Context context) {
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
		//noinspection ConstantConditions // TODO requireContext()
		return getDBHelper(getContext());
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

	@Nullable
	@Override
	public POIStatus getNewStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		return GTFSStatusProvider.getNewStatus(this, statusFilter);
	}

	@NonNull
	@Override
	public ScheduleTimestamps getScheduleTimestamps(@NonNull ScheduleTimestampsProviderContract.Filter filter) {
		return GTFSScheduleTimestampsProvider.getScheduleTimestamps(this, filter);
	}

	@Override
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		GTFSStatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Nullable
	@Override
	public POIStatus getCachedStatus(@NonNull StatusProviderContract.Filter statusFilter) {
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

	@NonNull
	@Override
	public String getStatusDbTableName() {
		return GTFSStatusProvider.getStatusDbTableName(this);
	}

	@NonNull
	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITYURI(getContext());
	}

	@Nullable
	@Override
	public Cursor queryMT(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
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
		//noinspection ConstantConditions // TODO requireContext()
		switch (getURIMATCHER(getContext()).match(uri)) {
		case ROUTE_LOGO:
			MTLog.v(this, "query>ROUTE_LOGO");
			return getRouteLogo();
		default:
			throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
		}
	}

	@Nullable
	@Override
	public Cursor getSearchSuggest(@Nullable String query) {
		return GTFSPOIProvider.getSearchSuggest(this, query);
	}

	@Nullable
	@Override
	public String getSearchSuggestTable() {
		return GTFSPOIProvider.getSearchSuggestTable(this);
	}

	@Nullable
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

	@Nullable
	@Override
	public Cursor getPOI(@Nullable POIProviderContract.Filter poiFilter) {
		return GTFSPOIProvider.getPOI(this, poiFilter);
	}

	@Nullable
	@Override
	public Cursor getPOIFromDB(@Nullable POIProviderContract.Filter poiFilter) {
		return GTFSPOIProvider.getPOIFromDB(this, poiFilter);
	}

	@NonNull
	@Override
	public String[] getPOIProjection() {
		return GTFSPOIProvider.getPOIProjection(this);
	}

	@NonNull
	@Override
	public ArrayMap<String, String> getPOIProjectionMap() {
		return GTFSPOIProvider.getPOIProjectionMap(this);
	}

	@NonNull
	@Override
	public String getPOITable() {
		return GTFSPOIProvider.getPOITable(this);
	}

	@Nullable
	@Override
	public String getSortOrder(@NonNull Uri uri) {
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
		//noinspection ConstantConditions // TODO requireContext()
		switch (getURIMATCHER(getContext()).match(uri)) {
		case ROUTE_LOGO:
			return null;
		default:
			return super.getSortOrder(uri);
		}
	}

	@Nullable
	@Override
	public String getTypeMT(@NonNull Uri uri) {
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
		//noinspection ConstantConditions // TODO requireContext()
		switch (getURIMATCHER(getContext()).match(uri)) {
		case ROUTE_LOGO:
			return null;
		default:
			return super.getTypeMT(uri);
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

	@Override
	public boolean isAgencyDeployed() {
		//noinspection ConstantConditions // TODO requireContext()
		return SqlUtils.isDbExist(getContext(), getDbName());
	}

	@Override
	public boolean isAgencySetupRequired() {
		boolean setupRequired = false;
		if (currentDbVersion > 0 && currentDbVersion != getCurrentDbVersion()) {
			setupRequired = true; // live update required => update
		} else if (!isAgencyDeployed()) {
			setupRequired = true; // not deployed => initialization
		} else if (SqlUtils.getCurrentDbVersion(getContext(), getDbName()) != getCurrentDbVersion()) {
			setupRequired = true; // update required => update
		}
		return setupRequired;
	}

	@NonNull
	@Override
	public UriMatcher getAgencyUriMatcher() {
		//noinspection ConstantConditions // TODO requireContext()
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
	@NonNull
	public String getDbName() {
		return GTFSProviderDbHelper.DB_NAME;
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@StringRes
	@Override
	public int getAgencyLabelResId() {
		return R.string.gtfs_rts_label;
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	@Override
	public String getAgencyColorString(@NonNull Context context) {
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
	@NonNull
	@Override
	public LocationUtils.Area getAgencyArea(@NonNull Context context) {
		String minLatS = getAREA_MIN_LAT(context);
		double minLat = TextUtils.isEmpty(minLatS) ? LocationUtils.MIN_LAT : Double.parseDouble(minLatS);
		String maxLatS = getAREA_MAX_LAT(context);
		double maxLat = TextUtils.isEmpty(maxLatS) ? LocationUtils.MAX_LAT : Double.parseDouble(maxLatS);
		String minLngS = getAREA_MIN_LNG(context);
		double minLng = TextUtils.isEmpty(minLngS) ? LocationUtils.MIN_LNG : Double.parseDouble(minLngS);
		String maxLngS = getAREA_MAX_LNG(context);
		double maxLng = TextUtils.isEmpty(maxLngS) ? LocationUtils.MAX_LNG : Double.parseDouble(maxLngS);
		return new LocationUtils.Area(minLat, maxLat, minLng, maxLng);
	}

	@Override
	public int getAgencyMaxValidSec(@NonNull Context context) {
		return GTFSCurrentNextProvider.getLAST_LAST_DEPARTURE_IN_SEC(context);
	}

	@Override
	public int getAvailableVersionCode(@NonNull Context context, @Nullable String filterS) {
		if (!F_APP_UPDATE) {
			return -1;
		}
		return AppUpdateUtils.getAvailableVersionCode(context, filterS);
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		//noinspection ConstantConditions // TODO requireContext()
		return GTFSProviderDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	public GTFSProviderDbHelper getNewDbHelper(@NonNull Context context) {
		return new GTFSProviderDbHelper(context.getApplicationContext());
	}

	@Nullable
	private static String routeLogo = null;

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@Nullable
	private Cursor getRouteLogo() {
		if (routeLogo == null) {
			routeLogo = readRouteLogo();
		}
		if (routeLogo.length() == 0) {
			return null;
		}
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{"routeLogo"});
		matrixCursor.addRow(new Object[]{routeLogo});
		return matrixCursor;
	}

	/**
	 * Override if multiple {@link GTFSProvider} implementations in same app.
	 */
	@NonNull
	private String readRouteLogo() {
		BufferedReader br = null;
		try {
			StringBuilder routeLogoSb = new StringBuilder();
			if (getContext() != null) {
				String line;
				br = new BufferedReader(new InputStreamReader(getContext().getResources().openRawResource(R.raw.gtfs_rts_route_logo), FileUtils.getUTF8()), 8192);
				while ((line = br.readLine()) != null) {
					routeLogoSb.append(line);
				}
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
