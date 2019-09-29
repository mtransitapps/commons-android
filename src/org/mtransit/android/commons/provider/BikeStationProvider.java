package org.mtransit.android.commons.provider;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.WordUtils;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;

import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.collection.ArrayMap;
import androidx.core.content.res.ResourcesCompat;
import android.text.TextUtils;

public abstract class BikeStationProvider extends AgencyProvider implements POIProviderContract, StatusProviderContract {

	private static final String LOG_TAG = BikeStationProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final long BIKE_STATION_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(7L);
	private static final long BIKE_STATION_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L);

	private static final long BIKE_STATION_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(30L);
	private static final long BIKE_STATION_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(5L);
	private static final long BIKE_STATION_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long BIKE_STATION_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(2L);
	private static final long BIKE_STATION_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Nullable
	private BikeStationDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@NonNull
	private static UriMatcher getURIMATCHER(@NonNull Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	@Nullable
	private static String authority = null;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@NonNull
	protected static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.bike_station_authority);
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
	private static String dataUrl = null;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@NonNull
	protected static String getDATA_URL(@NonNull Context context) {
		if (dataUrl == null) {
			dataUrl = context.getResources().getString(R.string.bike_station_data_url);
		}
		return dataUrl;
	}

	@Nullable
	private static String timeZone = null;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@SuppressWarnings("unused")
	@NonNull
	public static String getTIME_ZONE(@NonNull Context context) {
		if (timeZone == null) {
			timeZone = context.getResources().getString(R.string.bike_station_timezone);
		}
		return timeZone;
	}

	private static int value1Color = -1;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	protected static int getValue1Color(@NonNull Context context) {
		if (value1Color < 0) {
			value1Color = ResourcesCompat.getColor(context.getResources(), R.color.bike_station_value1_color, null);
		}
		return value1Color;
	}

	private static int value1ColorBg = -1;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	protected static int getValue1ColorBg(@NonNull Context context) {
		if (value1ColorBg < 0) {
			value1ColorBg = ResourcesCompat.getColor(context.getResources(), R.color.bike_station_value1_color_bg, null);
		}
		return value1ColorBg;
	}

	private static int value2Color = -1;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	protected static int getValue2Color(@NonNull Context context) {
		if (value2Color < 0) {
			value2Color = ResourcesCompat.getColor(context.getResources(), R.color.bike_station_value2_color, null);
		}
		return value2Color;
	}

	private static int value2ColorBg = -1;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	protected static int getValue2ColorBg(@NonNull Context context) {
		if (value2ColorBg < 0) {
			value2ColorBg = ResourcesCompat.getColor(context.getResources(), R.color.bike_station_value2_color_bg, null);
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
		// DO NOTHING
	}

	@NonNull
	private BikeStationDbHelper getDBHelper(@NonNull Context context) {
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
	@Override
	public SQLiteOpenHelper getDBHelper() {
		//noinspection ConstantConditions // TODO requireContext()
		return getDBHelper(getContext());
	}

	@Nullable
	@Override
	public Cursor getSearchSuggest(@Nullable String query) {
		return POIProvider.getDefaultSearchSuggest(query, this);
	}

	@Nullable
	@Override
	public Cursor getPOI(@Nullable POIProviderContract.Filter poiFilter) {
		if (poiFilter != null && poiFilter.getExtraBoolean(POIProviderContract.POI_FILTER_EXTRA_AVOID_LOADING, false)) {
			if (getLastUpdateInMs() + getPOIMaxValidityInMs() > TimeUtils.currentTimeMillis()) { // not too old to display
				Cursor cursor = getPOIFromDB(poiFilter);
				if (cursor != null && cursor.getCount() > 0) {
					return cursor; // returned cached results instead of loading while user is waiting
				}
			}
		}
		return getPOIBikeStations(poiFilter);
	}

	@Nullable
	public abstract Cursor getPOIBikeStations(@Nullable POIProviderContract.Filter poiFilter);

	public abstract long getLastUpdateInMs();

	@Nullable
	@Override
	public Cursor getPOIFromDB(@Nullable POIProviderContract.Filter poiFilter) {
		return POIProvider.getDefaultPOIFromDB(poiFilter, this);
	}

	@Nullable
	@Override
	public POIStatus getNewStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof AvailabilityPercent.AvailabilityPercentStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule without AvailabilityPercentStatusFilter!");
			return null;
		}
		AvailabilityPercent.AvailabilityPercentStatusFilter availabilityPercentStatusFilter = (AvailabilityPercent.AvailabilityPercentStatusFilter) statusFilter;
		return getNewBikeStationStatus(availabilityPercentStatusFilter);
	}

	@Nullable
	public abstract POIStatus getNewBikeStationStatus(@NonNull AvailabilityPercent.AvailabilityPercentStatusFilter filter);

	@Override
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Nullable
	@Override
	public POIStatus getCachedStatus(@NonNull StatusProviderContract.Filter statusFilter) {
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

	@NonNull
	@Override
	public Uri getAuthorityUri() {
		//noinspection ConstantConditions // TODO requireContext()
		return getAUTHORITYURI(getContext());
	}

	@Nullable
	@Override
	public Cursor queryMT(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
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

	public abstract void updateBikeStationStatusDataIfRequired(@NonNull StatusProviderContract.Filter statusFilter);

	@Nullable
	public String getSortOrder(@NonNull Uri uri) {
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

	@Nullable
	@Override
	public String getTypeMT(@NonNull Uri uri) {
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
	public int deleteMT(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
		MTLog.w(this, "The delete method is not available.");
		return 0;
	}

	@SuppressWarnings("UnusedReturnValue")
	protected int deleteAllBikeStationData() {
		int affectedRows = 0;
		try {
			//noinspection ConstantConditions // TODO requireContext()
			affectedRows = getDBHelper(getContext()).getWritableDatabase().delete(BikeStationDbHelper.T_BIKE_STATION, null, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all bike station data!");
		}
		return affectedRows;
	}

	@SuppressWarnings("UnusedReturnValue")
	protected int deleteAllBikeStationStatusData() {
		int affectedRows = 0;
		try {
			//noinspection ConstantConditions // TODO requireContext()
			affectedRows = getDBHelper(getContext()).getWritableDatabase().delete(BikeStationDbHelper.T_BIKE_STATION_STATUS, null, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all bike station status data!");
		}
		return affectedRows;
	}

	@NonNull
	@Override
	public String getStatusDbTableName() {
		return BikeStationDbHelper.T_BIKE_STATION_STATUS;
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

	@NonNull
	public static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = AgencyProvider.getNewUriMatcher(authority);
		StatusProvider.append(URI_MATCHER, authority);
		POIProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
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
			// live update required => update
			setupRequired = true;
		} else //noinspection ConstantConditions // TODO requireContext()
			if (!SqlUtils.isDbExist(getContext(), getDbName())) {
				// not deployed => initialization
				setupRequired = true;
			} else if (SqlUtils.getCurrentDbVersion(getContext(), getDbName()) != getCurrentDbVersion()) {
				// update required => update
				setupRequired = true;
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
		return POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT;
	}

	@Override
	public int getAgencyVersion() {
		return getCurrentDbVersion();
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@StringRes
	@Override
	public int getAgencyLabelResId() {
		return R.string.bike_station_label;
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@NonNull
	@Override
	public String getAgencyColorString(@NonNull Context context) {
		return context.getString(R.string.bike_station_color);
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@StringRes
	@Override
	public int getAgencyShortNameResId() {
		return R.string.bike_station_short_name;
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@NonNull
	@Override
	public LocationUtils.Area getAgencyArea(@NonNull Context context) {
		String minLatS = context.getString(R.string.bike_station_area_min_lat);
		double minLat = TextUtils.isEmpty(minLatS) ? LocationUtils.MIN_LAT : Double.parseDouble(minLatS);
		String maxLatS = context.getString(R.string.bike_station_area_max_lat);
		double maxLat = TextUtils.isEmpty(maxLatS) ? LocationUtils.MAX_LAT : Double.parseDouble(maxLatS);
		String minLngS = context.getString(R.string.bike_station_area_min_lng);
		double minLng = TextUtils.isEmpty(minLngS) ? LocationUtils.MIN_LNG : Double.parseDouble(minLngS);
		String maxLngS = context.getString(R.string.bike_station_area_max_lng);
		double maxLng = TextUtils.isEmpty(maxLngS) ? LocationUtils.MAX_LNG : Double.parseDouble(maxLngS);
		return new LocationUtils.Area(minLat, maxLat, minLng, maxLng);
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@NonNull
	public String getDbName() {
		return BikeStationDbHelper.DB_NAME;
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		//noinspection ConstantConditions // TODO requireContext()
		return getURIMATCHER(getContext());
	}

	private static int agencyTypeId = -1;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public static int getAGENCY_TYPE_ID(@NonNull Context context) {
		if (agencyTypeId < 0) {
			agencyTypeId = context.getResources().getInteger(R.integer.bike_station_agency_type);
		}
		return agencyTypeId;
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		//noinspection ConstantConditions // TODO requireContext()
		return BikeStationDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	@NonNull
	public BikeStationDbHelper getNewDbHelper(@NonNull Context context) {
		return new BikeStationDbHelper(context.getApplicationContext());
	}

	@Override
	public long getPOIMaxValidityInMs() {
		return BIKE_STATION_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getPOIValidityInMs() {
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

	@Nullable
	private static ArrayMap<String, String> poiProjectionMap;

	@NonNull
	@Override
	public ArrayMap<String, String> getPOIProjectionMap() {
		if (poiProjectionMap == null) {
			//noinspection ConstantConditions // TODO requireContext()
			poiProjectionMap = POIProvider.getNewPoiProjectionMap(getAUTHORITY(getContext()), getAGENCY_TYPE_ID(getContext()));
		}
		return poiProjectionMap;
	}

	@NonNull
	@Override
	public String[] getPOIProjection() {
		return POIProvider.PROJECTION_POI;
	}

	@NonNull
	@Override
	public String getPOITable() {
		return BikeStationDbHelper.T_BIKE_STATION;
	}

	@NonNull
	@Override
	public String getSearchSuggestTable() {
		return getPOITable();
	}

	@NonNull
	@Override
	public ArrayMap<String, String> getSearchSuggestProjectionMap() {
		return POIProvider.POI_SEARCH_SUGGEST_PROJECTION_MAP;
	}

	protected static final Pattern CLEAN_SLASHES = Pattern.compile("(\\w)[\\s]*[/][\\s]*(\\w)");
	protected static final String CLEAN_SLASHES_REPLACEMENT = "$1 / $2";

	private static final Pattern CLEAN_DOUBLE_SPACES = Pattern.compile("\\s+");
	private static final String CLEAN_DOUBLE_SPACES_REPLACEMENT = " ";

	protected static final String PARENTHESES_1 = "\\(";
	protected static final String PARENTHESES_2 = "\\)";

	private static final Pattern CLEAN_PARENTHESES_1 = Pattern.compile("[" + PARENTHESES_1 + "][\\s]*(\\w)");
	private static final String CLEAN_PARENTHESES_1_REPLACEMENT = PARENTHESES_1 + "$1";
	private static final Pattern CLEAN_PARENTHESES_2 = Pattern.compile("(\\w)[\\s]*[" + PARENTHESES_2 + "]");
	private static final String CLEAN_PARENTHESES_2_REPLACEMENT = "$1" + PARENTHESES_2;

	@Nullable
	public static String cleanBikeStationName(@Nullable String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		name = CLEAN_SLASHES.matcher(name).replaceAll(CLEAN_SLASHES_REPLACEMENT);
		name = CLEAN_PARENTHESES_1.matcher(name).replaceAll(CLEAN_PARENTHESES_1_REPLACEMENT);
		name = CLEAN_PARENTHESES_2.matcher(name).replaceAll(CLEAN_PARENTHESES_2_REPLACEMENT);
		name = CLEAN_DOUBLE_SPACES.matcher(name).replaceAll(CLEAN_DOUBLE_SPACES_REPLACEMENT);
		name = WordUtils.capitalize(name.toLowerCase(Locale.ENGLISH), ' ', '-', '/', '\'', '(');
		return name.trim();
	}

	@NonNull
	@Override
	public String toString() {
		return getClass().getSimpleName() + '[' +//
				"authority:" + (getContext() == null ? null : getAUTHORITY(getContext())) +//
				']';
	}
}
