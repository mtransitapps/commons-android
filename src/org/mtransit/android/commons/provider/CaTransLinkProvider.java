package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

@SuppressLint("Registered")
public class CaTransLinkProvider extends MTContentProvider implements StatusProviderContract {

	private static final String TAG = CaTransLinkProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link CaTransLinkProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link CaTransLinkProvider} implementations in same app.
	 */
	private static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.ca_translink_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link CaTransLinkProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String apiKey = null;

	/**
	 * Override if multiple {@link CaTransLinkProvider} implementations in same app.
	 */
	private static String getAPI_KEY(Context context) {
		if (apiKey == null) {
			apiKey = context.getResources().getString(R.string.ca_translink_api_key);
		}
		return apiKey;
	}

	private static final long RTTI_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long RTTI_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long RTTI_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long RTTI_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long RTTI_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

	@Override
	public long getStatusMaxValidityInMs() {
		return RTTI_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return RTTI_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return RTTI_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return RTTI_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return RTTI_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Override
	public POIStatus getCachedStatus(StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schecule whithout schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		POIStatus status = StatusProvider.getCachedStatusS(this, getAgencyRouteStopTargetUUID(rts));
		if (status != null) {
			status.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom TransLink tags
			if (status instanceof Schedule) {
				((Schedule) status).setDescentOnly(rts.isDescentOnly());
			}
		}
		return status;
	}

	private String getAgencyRouteStopTargetUUID(RouteTripStop rts) {
		return getAgencyRouteStopTargetUUID(rts.getAuthority(), rts.getRoute().getShortName(), rts.getStop().getCode());
	}

	protected static String getAgencyRouteStopTargetUUID(String agencyAuthority, String routeShortName, String stopCode) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName, stopCode);
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
	public String getStatusDbTableName() {
		return CaTransLinkDbHelper.T_RTTI_STATUS;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	@Override
	public POIStatus getNewStatus(StatusProviderContract.Filter statusFilter) {
		if (statusFilter == null || !(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schecule whithout schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	private static final String REAL_TIME_URL_PART_1_BEFORE_STOP_ID = "http://api.translink.ca/rttiapi/v1/stops/";
	private static final String REAL_TIME_URL_PART_2_BEFORE_API_KEY = "/estimates?apikey=";

	private static String getRealTimeStatusUrlString(Context context, RouteTripStop rts) {
		return new StringBuilder() //
				.append(REAL_TIME_URL_PART_1_BEFORE_STOP_ID) //
				.append(rts.getStop().getCode()) //
				.append(REAL_TIME_URL_PART_2_BEFORE_API_KEY) //
				.append(getAPI_KEY(context)) //
				.toString();
	}

	private static final String APPLICATION_JSON = "application/JSON";
	private static final String ACCEPT = "accept";

	private void loadRealTimeStatusFromWWW(RouteTripStop rts) {
		try {
			String urlString = getRealTimeStatusUrlString(getContext(), rts);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			urlc.addRequestProperty(ACCEPT, APPLICATION_JSON);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				Collection<POIStatus> statuses = parseAgencyJSON(jsonString, rts, newLastUpdateInMs);
				if (statuses != null && statuses.size() > 0) {
					HashSet<String> uuids = new HashSet<String>();
					for (POIStatus status : statuses) {
						uuids.add(status.getTargetUUID());
					}
					StatusProvider.deleteCachedStatus(this, uuids);
					for (POIStatus status : statuses) {
						StatusProvider.cacheStatusS(this, status);
					}
				}
				return;
			default:
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpUrlConnection.getResponseCode(),
						httpUrlConnection.getResponseMessage());
			}
		} catch (UnknownHostException uhe) {
			if (MTLog.isLoggable(android.util.Log.DEBUG)) {
				MTLog.w(this, uhe, "No Internet Connection!");
			} else {
				MTLog.w(this, "No Internet Connection!");
			}
		} catch (SocketException se) {
			MTLog.w(TAG, se, "No Internet Connection!");
		} catch (Exception e) { // Unknown error
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
		}
	}

	private static final String JSON_ROUTE_NO = "RouteNo";
	private static final String JSON_SCHEDULES = "Schedules";
	private static final String JSON_EXPECTED_LEAVE_TIME = "ExpectedLeaveTime";

	private static final TimeZone VANCOUVER_TZ = TimeZone.getTimeZone("America/Vancouver");

	private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

	private static final ThreadSafeDateFormatter DATE_FORMATTER_UTC;
	static {
		ThreadSafeDateFormatter dateFormatter = new ThreadSafeDateFormatter("hh:mma");
		dateFormatter.setTimeZone(UTC_TZ);
		DATE_FORMATTER_UTC = dateFormatter;
	}

	private Calendar getNewBeginningOfTodayCal() {
		Calendar beginningOfTodayCal = Calendar.getInstance(VANCOUVER_TZ);
		beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, 0);
		beginningOfTodayCal.set(Calendar.MINUTE, 0);
		beginningOfTodayCal.set(Calendar.SECOND, 0);
		beginningOfTodayCal.set(Calendar.MILLISECOND, 0);
		return beginningOfTodayCal;
	}

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10);

	private Collection<POIStatus> parseAgencyJSON(String jsonString, RouteTripStop rts, long newLastUpdateInMs) {
		try {
			String routeShortName;
			ArrayList<POIStatus> result = new ArrayList<POIStatus>();
			JSONArray jNextBuses = jsonString == null ? null : new JSONArray(jsonString);
			if (jNextBuses != null && jNextBuses.length() > 0) {
				long beginningOfTodayMs = getNewBeginningOfTodayCal().getTimeInMillis();
				for (int nb = 0; nb < jNextBuses.length(); nb++) {
					JSONObject jNextBus = jNextBuses.getJSONObject(nb);
					if (jNextBus != null) {
						routeShortName = jNextBus.getString(JSON_ROUTE_NO);
						if (TextUtils.isDigitsOnly(routeShortName)) {
							routeShortName = String.valueOf(Integer.parseInt(routeShortName)); // RSN leading '0' removed in parser
						}
						String uuid = getAgencyRouteStopTargetUUID(rts.getAuthority(), routeShortName, rts.getStop().getCode());
						JSONArray jSchedules = jNextBus.getJSONArray(JSON_SCHEDULES);
						if (jSchedules != null && jSchedules.length() > 0) {
							Schedule newSchedule = new Schedule(uuid, newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
									PROVIDER_PRECISION_IN_MS, false);
							long after = newLastUpdateInMs - TimeUnit.HOURS.toMillis(1);
							for (int s = 0; s < jSchedules.length(); s++) {
								JSONObject jSchedule = jSchedules.getJSONObject(s);
								String expectedLeaveTime = jSchedule.getString(JSON_EXPECTED_LEAVE_TIME);
								long t = beginningOfTodayMs
										+ TimeUtils.timeToTheTensSecondsMillis(DATE_FORMATTER_UTC.parseThreadSafe(expectedLeaveTime).getTime());
								if (t < after) {
									t += TimeUnit.DAYS.toMillis(1); // TOMORROW
								}
								newSchedule.addTimestampWithoutSort(new Schedule.Timestamp(t));
							}
							newSchedule.sortTimestamps();
							result.add(newSchedule);
						}
					}
				}
			}
			return result;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
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

	private static CaTransLinkDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private CaTransLinkDbHelper getDBHelper(Context context) {
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
	 * Override if multiple {@link CaTransLinkProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return CaTransLinkDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link CaTransLinkProvider} implementations in same app.
	 */
	public CaTransLinkDbHelper getNewDbHelper(Context context) {
		return new CaTransLinkDbHelper(context.getApplicationContext());
	}

	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITY_URI(getContext());
	}

	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	@Override
	public Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor cursor = StatusProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
	}

	@Override
	public String getTypeMT(Uri uri) {
		String type = StatusProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
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

	public static class CaTransLinkDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = CaTransLinkDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link CaTransLinkDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "catranslink.db";

		public static final String T_RTTI_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_RTTI_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_RTTI_STATUS).build();

		private static final String T_RTTI_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_RTTI_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link CaTransLinkDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.ca_translink_db_version);
			}
			return dbVersion;
		}

		public CaTransLinkDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_RTTI_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_RTTI_STATUS_SQL_CREATE);
		}
	}
}
