package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
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
public class WinnipegTransitProvider extends MTContentProvider implements StatusProviderContract {

	private static final String TAG = WinnipegTransitProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.winnipeg_transit_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	public static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String statusTargetAuthority = null;

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	public static String getSTATUS_TARGET_AUTHORITY(Context context) {
		if (statusTargetAuthority == null) {
			statusTargetAuthority = context.getResources().getString(R.string.winnipeg_transit_status_for_poi_authority);
		}
		return statusTargetAuthority;
	}

	private static String apiKey = null;

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	public static String getAPI_KEY(Context context) {
		if (apiKey == null) {
			apiKey = context.getResources().getString(R.string.winnipeg_transit_api_key);
		}
		return apiKey;
	}

	private static final long WEB_SERVICE_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long WEB_SERVICE_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long WEB_SERVICE_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long WEB_SERVICE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long WEB_SERVICE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

	@Override
	public long getStatusMaxValidityInMs() {
		return WEB_SERVICE_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return WEB_SERVICE_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return WEB_SERVICE_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return WEB_SERVICE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return WEB_SERVICE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
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
		String targetUUID = rts.getUUID();
		return StatusProvider.getCachedStatusS(this, targetUUID);
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
		return WinnipegTransitDbHelper.T_WEB_SERVICE_STATUS;
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

	private static final TimeZone WINNIPEG_TZ = TimeZone.getTimeZone("America/Winnipeg");

	private static final ThreadSafeDateFormatter DATE_FORMATTER;
	static {
		ThreadSafeDateFormatter dateFormatter = new ThreadSafeDateFormatter("yyyy-MM-dd'T'HH:mm:ss");
		dateFormatter.setTimeZone(WINNIPEG_TZ);
		DATE_FORMATTER = dateFormatter;
	}

	private static final String REAL_TIME_URL_PART_1_BEFORE_STOP_ID = " http://api.winnipegtransit.com/v2/stops/";
	private static final String REAL_TIME_URL_PART_2_BEFORE_ROUTE_ID = "/schedule.json?route=";
	private static final String REAL_TIME_URL_PART_3_BEFORE_START = "&start=";
	private static final String REAL_TIME_URL_PART_4_BEFORE_END = "&end=";
	private static final String REAL_TIME_URL_PART_5_BEFORE_API_KEY = "&api-key=";

	private static String getRealTimeStatusUrlString(Context context, RouteTripStop rts) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.HOUR, -1);
		String start = DATE_FORMATTER.formatThreadSafe(c.getTime());
		c.add(Calendar.HOUR, +1 + 12);
		String end = DATE_FORMATTER.formatThreadSafe(c.getTime());
		return new StringBuilder() //
				.append(REAL_TIME_URL_PART_1_BEFORE_STOP_ID) //
				.append(rts.getStop().getCode()) //
				.append(REAL_TIME_URL_PART_2_BEFORE_ROUTE_ID) //
				.append(rts.getRoute().getShortName()) //
				.append(REAL_TIME_URL_PART_3_BEFORE_START) //
				.append(start) //
				.append(REAL_TIME_URL_PART_4_BEFORE_END) //
				.append(end) //
				.append(REAL_TIME_URL_PART_5_BEFORE_API_KEY) //
				.append(getAPI_KEY(context)) //
				.toString();
	}

	private void loadRealTimeStatusFromWWW(RouteTripStop rts) {
		try {
			String urlString = getRealTimeStatusUrlString(getContext(), rts);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				Collection<POIStatus> statuses = parseAgencyJSON(jsonString, rts, newLastUpdateInMs);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(new String[] { rts.getUUID() }));
				if (statuses != null) {
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

	private static final String JSON_STOP_SCHEDULE = "stop-schedule";
	private static final String JSON_ROUTE_SCHEDULES = "route-schedules";
	private static final String JSON_SCHEDULED_STOPS = "scheduled-stops";
	private static final String JSON_TIMES = "times";
	private static final String JSON_DEPARTURE = "departure";
	private static final String JSON_ESTIMATED = "estimated";
	private static final String JSON_SCHEDULED = "scheduled";
	private static final String JSON_ARRIVAL = "arrival";

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10);

	private Collection<POIStatus> parseAgencyJSON(String jsonString, RouteTripStop rts, long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> result = new ArrayList<POIStatus>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_STOP_SCHEDULE)) {
				JSONObject jStopSchedule = json.getJSONObject(JSON_STOP_SCHEDULE);
				if (jStopSchedule != null && jStopSchedule.has(JSON_ROUTE_SCHEDULES)) {
					JSONArray jRouteSchedules = jStopSchedule.getJSONArray(JSON_ROUTE_SCHEDULES);
					if (jRouteSchedules != null && jRouteSchedules.length() > 0) {
						JSONObject jRouteSchedule = jRouteSchedules.getJSONObject(0);
						if (jRouteSchedule != null && jRouteSchedule.has(JSON_SCHEDULED_STOPS)) {
							JSONArray jScheduledStops = jRouteSchedule.getJSONArray(JSON_SCHEDULED_STOPS);
							if (jScheduledStops != null && jScheduledStops.length() > 0) {
								Schedule newSchedule = parseAgencySchedule(rts, newLastUpdateInMs, jScheduledStops);
								result.add(newSchedule);
							}
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

	private Schedule parseAgencySchedule(RouteTripStop rts, long newLastUpdateInMs, JSONArray jScheduledStops) throws JSONException, ParseException {
		try {
			Schedule newSchedule = new Schedule(rts.getUUID(), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs, PROVIDER_PRECISION_IN_MS,
					rts.isDescentOnly());
			for (int s = 0; s < jScheduledStops.length(); s++) {
				JSONObject jScheduledStop = jScheduledStops.getJSONObject(s);
				if (jScheduledStop != null && jScheduledStop.has(JSON_TIMES)) {
					JSONObject jTimes = jScheduledStop.getJSONObject(JSON_TIMES);
					if (jTimes != null) {
						String timeS = getTimeString(jTimes);
						if (!TextUtils.isEmpty(timeS)) {
							newSchedule.addTimestampWithoutSort(new Schedule.Timestamp(TimeUtils.timeToTheTensSecondsMillis(DATE_FORMATTER.parseThreadSafe(
									timeS).getTime())));
						}
					}
				}
			}
			newSchedule.sortTimestamps();
			return newSchedule;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing schedule JSON '%s'!", jScheduledStops);
			return null;
		}
	}

	private String getTimeString(JSONObject jTimes) {
		try {
			String timeS = null;
			if (jTimes.has(JSON_DEPARTURE)) {
				JSONObject jDeparture = jTimes.getJSONObject(JSON_DEPARTURE);
				if (jDeparture != null && jDeparture.has(JSON_ESTIMATED)) {
					timeS = jDeparture.getString(JSON_ESTIMATED);
					if (!TextUtils.isEmpty(timeS)) {
						return timeS;
					}
				}
				if (jDeparture != null && jDeparture.has(JSON_SCHEDULED)) {
					timeS = jDeparture.getString(JSON_SCHEDULED);
					if (!TextUtils.isEmpty(timeS)) {
						return timeS;
					}
				}
			}
			if (jTimes.has(JSON_ARRIVAL)) {
				JSONObject jArrival = jTimes.getJSONObject(JSON_ARRIVAL);
				if (jArrival != null && jArrival.has(JSON_ESTIMATED)) {
					timeS = jArrival.getString(JSON_ESTIMATED);
					if (!TextUtils.isEmpty(timeS)) {
						return timeS;
					}
				}
				if (jArrival != null && jArrival.has(JSON_SCHEDULED)) {
					timeS = jArrival.getString(JSON_SCHEDULED);
					if (!TextUtils.isEmpty(timeS)) {
						return timeS;
					}
				}
			}
			return null;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing time JSON '%s'!", jTimes);
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

	private static WinnipegTransitDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private WinnipegTransitDbHelper getDBHelper(Context context) {
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
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return WinnipegTransitDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	public WinnipegTransitDbHelper getNewDbHelper(Context context) {
		return new WinnipegTransitDbHelper(context.getApplicationContext());
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

	public static class WinnipegTransitDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = WinnipegTransitDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link WinnipegTransitDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "winnipegtransit.db";

		public static final String T_WEB_SERVICE_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_WEB_SERVICE_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_WEB_SERVICE_STATUS).build();

		private static final String T_WEB_SERVICE_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_WEB_SERVICE_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link WinnipegTransitDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.winnipeg_transit_db_version);
			}
			return dbVersion;
		}

		public WinnipegTransitDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_WEB_SERVICE_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_WEB_SERVICE_STATUS_SQL_CREATE);
		}
	}
}
