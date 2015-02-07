package org.mtransit.android.commons.provider;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.mtransit.android.commons.LocationUtils.Area;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.WordUtils;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

public abstract class BikeStationProvider extends AgencyProvider implements POIProviderContract, StatusProviderContract {

	private static final String TAG = BikeStationProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final long BIKE_STATION_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(7);
	private static final long BIKE_STATION_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1);

	private static final long BIKE_STATION_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(30);
	private static final long BIKE_STATION_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(5);
	private static final long BIKE_STATION_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long BIKE_STATION_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(2);
	private static final long BIKE_STATION_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);


	private static BikeStationDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.bike_station_authority);
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

	private static String dataUrl = null;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public static String getDATA_URL(Context context) {
		if (dataUrl == null) {
			dataUrl = context.getResources().getString(R.string.bike_station_data_url);
		}
		return dataUrl;
	}

	private static int value1Color = -1;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public static int getValue1Color(Context context) {
		if (value1Color < 0) {
			value1Color = context.getResources().getColor(R.color.bike_station_value1_color);
		}
		return value1Color;
	}

	private static int value1ColorBg = -1;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public static int getValue1ColorBg(Context context) {
		if (value1ColorBg < 0) {
			value1ColorBg = context.getResources().getColor(R.color.bike_station_value1_color_bg);
		}
		return value1ColorBg;
	}

	private static int value2Color = -1;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public static int getValue2Color(Context context) {
		if (value2Color < 0) {
			value2Color = context.getResources().getColor(R.color.bike_station_value2_color);
		}
		return value2Color;
	}

	private static int value2ColorBg = -1;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public static int getValue2ColorBg(Context context) {
		if (value2ColorBg < 0) {
			value2ColorBg = context.getResources().getColor(R.color.bike_station_value2_color_bg);
		}
		return value2ColorBg;
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

	private BikeStationDbHelper getDBHelper(Context context) {
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
			} catch (Exception e) {
				MTLog.d(this, e, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	@Override
	public Context getContentProviderContext() {
		return getContext();
	}

	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	@Override
	public Cursor getSearchSuggest(String query) {
		return POIProvider.getDefaultSearchSuggest(query, this);
	}

	@Override
	public Cursor getPOI(POIFilter poiFilter) {
		return getPOIBikeStations(poiFilter);
	}

	public abstract Cursor getPOIBikeStations(POIFilter poiFilter);

	@Override
	public Cursor getPOIFromDB(POIFilter poiFilter) {
		return POIProvider.getDefaultPOIFromDB(poiFilter, this);
	}

	@Override
	public POIStatus getNewStatus(StatusFilter statusFilter) {
		if (!(statusFilter instanceof AvailabilityPercent.AvailabilityPercentStatusFilter)) {
			return null;
		}
		AvailabilityPercent.AvailabilityPercentStatusFilter availabilityPercentStatusFilter = (AvailabilityPercent.AvailabilityPercentStatusFilter) statusFilter;
		return getNewBikeStationStatus(availabilityPercentStatusFilter);
	}

	public abstract POIStatus getNewBikeStationStatus(AvailabilityPercent.AvailabilityPercentStatusFilter filter);

	@Override
	public void cacheStatus(POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Override
	public POIStatus getCachedStatus(StatusFilter statusFilter) {
		return StatusProvider.getCachedStatusS(this, statusFilter.getTargetUUID());
	}

	@Override
	public boolean purgeUselessCachedStatuses() {
		return StatusProvider.purgeUselessCachedStatuses(this);
	}

	@Override
	public boolean deleteCachedStatus(int cachedStatusId) {
		return StatusProvider.deleteCachedStatus(this, cachedStatusId);
	}

	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITYURI(getContext());
	}

	@Override
	public Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		try {
			Cursor cursor = super.queryMT(uri, projection, selection, selectionArgs, sortOrder);
			if (cursor != null) {
				return cursor;
			}
			cursor = POIProvider.queryS(this, uri, selection);
			if (cursor != null) {
				return cursor;
			}
			cursor = StatusProvider.queryS(this, uri, selection);
			if (cursor != null) {
				return cursor;
			}
			throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
		} catch (Exception e) {
			MTLog.w(this, e, "Error while resolving query '%s'!", uri);
			return null;
		}
	}

	public abstract void updateBikeStationDataIfRequired();

	public abstract void updateBikeStationStatusDataIfRequired(StatusFilter statusFilter);

	@Override
	public String getSortOrder(Uri uri) {
		String sortOrder = POIProvider.getSortOrderS(this, uri);
		if (sortOrder != null) {
			return sortOrder;
		}
		sortOrder = StatusProvider.getSortOrderS(this, uri);
		if (sortOrder != null) {
			return sortOrder;
		}
		return super.getSortOrder(uri);
	}

	@Override
	public String getTypeMT(Uri uri) {
		String type = POIProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		type = StatusProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		return super.getTypeMT(uri);
	}

	@Override
	public int deleteMT(Uri uri, String selection, String[] selectionArgs) {
		MTLog.w(this, "The delete method is not available.");
		return 0;
	}

	protected int deleteAllBikeStationData() {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = getDBHelper(getContext()).getWritableDatabase();
			affectedRows = db.delete(BikeStationDbHelper.T_BIKE_STATION, null, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all bike station data!");
		} finally {
			SqlUtils.closeQuietly(db);
		}
		return affectedRows;
	}

	protected int deleteAllBikeStationStatusData() {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = getDBHelper(getContext()).getWritableDatabase();
			affectedRows = db.delete(BikeStationDbHelper.T_BIKE_STATION_STATUS, null, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all bike station status data!");
		} finally {
			SqlUtils.closeQuietly(db);
		}
		return affectedRows;
	}

	@Override
	public String getStatusDbTableName() {
		return BikeStationDbHelper.T_BIKE_STATION_STATUS;
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



	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = AgencyProvider.getNewUriMatcher(authority);
		StatusProvider.append(URI_MATCHER, authority);
		POIProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Override
	public boolean isAgencyDeployed() {
		return SqlUtils.isDbExist(getContext(), getDbName());
	}

	@Override
	public boolean isAgencySetupRequired() {
		boolean setupRequired = false;
		if (currentDbVersion > 0 && currentDbVersion != getCurrentDbVersion()) {
			// live update required => update
			setupRequired = true;
		} else if (!SqlUtils.isDbExist(getContext(), getDbName())) {
			// not deployed => initialization
			setupRequired = true;
		} else if (SqlUtils.getCurrentDbVersion(getContext(), getDbName()) != getCurrentDbVersion()) {
			// update required => update
			setupRequired = true;
		}
		return setupRequired;
	}

	@Override
	public UriMatcher getAgencyUriMatcher() {
		return getURIMATCHER(getContext());
	}


	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT;
	}

	@Override
	public int getAgencyVersion() {
		return getCurrentDbVersion();
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@Override
	public int getAgencyLabelResId() {
		return R.string.bike_station_label;
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@Override
	public String getAgencyColorString(Context context) {
		return context.getString(R.string.bike_station_color);
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@Override
	public int getAgencyShortNameResId() {
		return R.string.bike_station_short_name;
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@Override
	public Area getAgencyArea(Context context) {
		String minLatS = context.getString(R.string.bike_station_area_min_lat);
		if (TextUtils.isEmpty(minLatS)) {
			return null;
		}
		double minLat = Double.parseDouble(minLatS);
		String maxLatS = context.getString(R.string.bike_station_area_max_lat);
		if (TextUtils.isEmpty(maxLatS)) {
			return null;
		}
		double maxLat = Double.parseDouble(maxLatS);
		String minLngS = context.getString(R.string.bike_station_area_min_lng);
		if (TextUtils.isEmpty(minLngS)) {
			return null;
		}
		double minLng = Double.parseDouble(minLngS);
		String maxLngS = context.getString(R.string.bike_station_area_max_lng);
		if (TextUtils.isEmpty(maxLngS)) {
			return null;
		}
		double maxLng = Double.parseDouble(maxLngS);
		return new Area(minLat, maxLat, minLng, maxLng);
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public String getDbName() {
		return BikeStationDbHelper.DB_NAME;
	}

	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	private static int agencyTypeId = -1;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public static int getAGENCY_TYPE_ID(Context context) {
		if (agencyTypeId < 0) {
			agencyTypeId = context.getResources().getInteger(R.integer.bike_station_agency_type);
		}
		return agencyTypeId;
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return BikeStationDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public BikeStationDbHelper getNewDbHelper(Context context) {
		return new BikeStationDbHelper(context.getApplicationContext());
	}

	public long getBIKE_STATION_MAX_VALIDITY_IN_MS() {
		return BIKE_STATION_MAX_VALIDITY_IN_MS;
	}

	public long getBIKE_STATION_VALIDITY_IN_MS() {
		return BIKE_STATION_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusMaxValidityInMs() {
		return BIKE_STATION_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return BIKE_STATION_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return BIKE_STATION_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return BIKE_STATION_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return BIKE_STATION_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	private static HashMap<String, String> poiProjectionMap;

	@Override
	public HashMap<String, String> getPOIProjectionMap() {
		if (poiProjectionMap == null) {
			poiProjectionMap = POIProvider.getNewPoiProjectionMap(getAUTHORITY(getContext()), getAGENCY_TYPE_ID(getContext()));
		}
		return poiProjectionMap;
	}

	@Override
	public String[] getPOIProjection() {
		return POIProvider.PROJECTION_POI;
	}

	@Override
	public String getPOITable() {
		return BikeStationDbHelper.T_BIKE_STATION;
	}

	@Override
	public String getSearchSuggestTable() {
		return getPOITable();
	}

	@Override
	public HashMap<String, String> getSearchSuggestProjectionMap() {
		return POIProvider.POI_SEARCH_SUGGEST_PROJECTION_MAP;
	}

	protected static final Pattern CLEAN_SLASHES = Pattern.compile("(\\w)[\\s]*[/][\\s]*(\\w)");
	protected static final String CLEAN_SLASHES_REPLACEMENT = "$1 / $2";

	private static final Pattern CLEAN_DOUBLE_SPACES = Pattern.compile("\\s+");
	private static final String CLEAN_DOUBLE_SPACES_REPLACEMENT = " ";

	protected static final String PARENTHESE1 = "\\(";
	protected static final String PARENTHESE2 = "\\)";

	private static final Pattern CLEAN_PARENTHESE1 = Pattern.compile("[" + PARENTHESE1 + "][\\s]*(\\w)");
	private static final String CLEAN_PARENTHESE1_REPLACEMENT = PARENTHESE1 + "$1";
	private static final Pattern CLEAN_PARENTHESE2 = Pattern.compile("(\\w)[\\s]*[" + PARENTHESE2 + "]");
	private static final String CLEAN_PARENTHESE2_REPLACEMENT = "$1" + PARENTHESE2;

	public static String cleanBikeStationName(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		// clean "/" => " / "
		name = CLEAN_SLASHES.matcher(name).replaceAll(CLEAN_SLASHES_REPLACEMENT);
		name = CLEAN_PARENTHESE1.matcher(name).replaceAll(CLEAN_PARENTHESE1_REPLACEMENT);
		name = CLEAN_PARENTHESE2.matcher(name).replaceAll(CLEAN_PARENTHESE2_REPLACEMENT);
		// remove double white-spaces
		name = CLEAN_DOUBLE_SPACES.matcher(name).replaceAll(CLEAN_DOUBLE_SPACES_REPLACEMENT);
		// cLean-Up tHe caPItalIsaTIon
		name = WordUtils.capitalize(name.toLowerCase(Locale.ENGLISH), new char[] { ' ', '-', '/', '\'', '(' });
		return name.trim();
	}

	@Override
	public String toString() {
		return new StringBuilder(getClass().getSimpleName()).append('[')//
				.append("authority:").append(getAUTHORITY(getContext()))//
				.append(']').toString();
	}
}
