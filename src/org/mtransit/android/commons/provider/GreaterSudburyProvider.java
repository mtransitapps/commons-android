package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
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
public class GreaterSudburyProvider extends MTContentProvider implements StatusProviderContract {

	private static final String TAG = GreaterSudburyProvider.class.getSimpleName();

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
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	private static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.greater_sudbury_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String authToken = null;

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	private static String getAUTH_TOKEN(Context context) {
		if (authToken == null) {
			authToken = context.getResources().getString(R.string.greater_sudbury_auth_token);
		}
		return authToken;
	}

	private static final long MYBUS_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long MYBUS_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long MYBUS_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long MYBUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long MYBUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

	@Override
	public long getStatusMaxValidityInMs() {
		return MYBUS_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return MYBUS_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return MYBUS_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return MYBUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return MYBUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Override
	public POIStatus getCachedStatus(StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getCachedStatus() > Can't find new schecule whithout schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (rts == null || TextUtils.isEmpty(rts.getStop().getCode())) {
			return null;
		}
		POIStatus status = StatusProvider.getCachedStatusS(this, getAgencyRouteStopTargetUUID(rts));
		if (status == null) {
			status = StatusProvider.getCachedStatusS(this, getAgencyCall(rts));
		}
		if (status != null) {
			status.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom Clever Devices tags
			if (status instanceof Schedule) {
				((Schedule) status).setDescentOnly(rts.isDescentOnly());
			}
		}
		return status;
	}

	private static String getAgencyCall(RouteTripStop rts) {
		return POI.POIUtils.getUUID(rts.getAuthority(), rts.getStop().getCode());
	}

	private static String getAgencyRouteStopTargetUUID(RouteTripStop rts) {
		return getAgencyRouteStopTargetUUID(rts.getAuthority(), rts.getRoute().getShortName(), extractTripId(rts.getTrip().getId()), rts.getStop().getCode());
	}

	private static String getAgencyRouteStopTargetUUID(String agencyAuthority, String routeShortName, long tripId, String stopCode) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName, tripId, stopCode);
	}

	private static long extractTripId(long id) {
		return id % 10l;
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
		return GreaterSudburyDbHelper.T_MYBUS_STATUS;
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
		if (rts == null || TextUtils.isEmpty(rts.getStop().getCode())) {
			return null;
		}
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	private static final String REAL_TIME_URL_PART_1_BEFORE_STOP_CODE = "http://mybus.greatersudbury.ca/api/v2/stops/";
	private static final String REAL_TIME_URL_PART_2_BEFORE_AUTH_TOKEN = "?auth_token=";

	private static String getRealTimeStatusUrlString(Context context, RouteTripStop rts) {
		if (TextUtils.isEmpty(rts.getStop().getCode())) {
			MTLog.w(TAG, "Can't create real-time status URL (no stop code) for %s", rts);
			return null;
		}
		return new StringBuilder() //
				.append(REAL_TIME_URL_PART_1_BEFORE_STOP_CODE) //
				.append(rts.getStop().getCode()) //
				.append(REAL_TIME_URL_PART_2_BEFORE_AUTH_TOKEN) //
				.append(getAUTH_TOKEN(context)) //
				.toString();
	}

	private void loadRealTimeStatusFromWWW(RouteTripStop rts) {
		try {
			String urlString = getRealTimeStatusUrlString(getContext(), rts);
			MTLog.i(this, "Loading from '%s'...", new StringBuilder() //
					.append(REAL_TIME_URL_PART_1_BEFORE_STOP_CODE) //
					.append(rts.getStop().getCode()) //
					.toString());
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			httpUrlConnection.setInstanceFollowRedirects(true);
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				Collection<? extends POIStatus> statuses = parseAgencyJSON(jsonString, rts, newLastUpdateInMs);
				if (statuses != null) {
					HashSet<String> targetUUIDs = new HashSet<String>();
					for (POIStatus status : statuses) {
						targetUUIDs.add(status.getTargetUUID());
					}
					StatusProvider.deleteCachedStatus(this, targetUUIDs);
					for (POIStatus status : statuses) {
						StatusProvider.cacheStatusS(this, status);
					}
					StatusProvider.deleteCachedStatus(this, Arrays.asList(getAgencyCall(rts)));
					StatusProvider.cacheStatusS(this, new Schedule(getAgencyCall(rts), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
							PROVIDER_PRECISION_IN_MS, false).setNoData(true));
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

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10);

	private static final String JSON_STOP = "stop";
	private static final String JSON_NUMBER = "number";
	private static final String JSON_CALLS = "calls";
	private static final String JSON_PASSING_TIME = "passing_time";
	private static final String JSON_ROUTE = "route";
	private static final String JSON_DESTINATION = "destination";

	private static final ThreadSafeDateFormatter DATE_FORMATTER = new ThreadSafeDateFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.ENGLISH);

	private Collection<? extends POIStatus> parseAgencyJSON(String jsonString, RouteTripStop rts, long newLastUpdateInMs) {
		try {
			HashMap<String, Schedule> result = new HashMap<String, Schedule>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_STOP)) {
				JSONObject jStop = json.getJSONObject(JSON_STOP);
				if (jStop != null && jStop.has(JSON_CALLS)) {
					JSONArray jCalls = jStop.getJSONArray(JSON_CALLS);
					for (int l = 0; l < jCalls.length(); l++) {
						JSONObject jCall = jCalls.getJSONObject(l);
						if (jCall != null && jCall.has(JSON_PASSING_TIME)) {
							if (!jCall.has(JSON_DESTINATION)) {
								continue;
							}
							JSONObject jDestination = jCall.getJSONObject(JSON_DESTINATION);
							if (jDestination == null || !jDestination.has(JSON_NUMBER)) {
								continue;
							}
							long jDestinationNumber = jDestination.getLong(JSON_NUMBER);
							String routeShortName = jCall.getString(JSON_ROUTE);
							String jPassingTime = jCall.getString(JSON_PASSING_TIME);
							try {
								long t = TimeUtils.timeToTheTensSecondsMillis(DATE_FORMATTER.parseThreadSafe(jPassingTime).getTime());
								String targetUUID = getAgencyRouteStopTargetUUID(rts.getAuthority(), routeShortName, extractTripId(jDestinationNumber), rts
										.getStop().getCode());
								if (!result.containsKey(targetUUID)) {
									result.put(targetUUID, new Schedule(targetUUID, newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
											PROVIDER_PRECISION_IN_MS, false));
								}
								result.get(targetUUID).addTimestampWithoutSort(new Schedule.Timestamp(t));
							} catch (Exception e) {
								MTLog.w(this, e, "Error while parsing time %s!", jPassingTime);
							}
						}
					}
				}
			}
			for (Schedule schedule : result.values()) {
				schedule.sortTimestamps();
			}
			return result.values();
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

	private static GreaterSudburyDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private GreaterSudburyDbHelper getDBHelper(Context context) {
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
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return GreaterSudburyDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	public GreaterSudburyDbHelper getNewDbHelper(Context context) {
		return new GreaterSudburyDbHelper(context.getApplicationContext());
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

	public static class GreaterSudburyDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = GreaterSudburyDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link GreaterSudburyDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "greatersudbury.db";

		public static final String T_MYBUS_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_MYBUS_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_MYBUS_STATUS).build();

		private static final String T_MYBUS_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_MYBUS_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link GreaterSudburyDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.greater_sudbury_db_version);
			}
			return dbVersion;
		}

		public GreaterSudburyDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_MYBUS_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_MYBUS_STATUS_SQL_CREATE);
		}
	}
}
