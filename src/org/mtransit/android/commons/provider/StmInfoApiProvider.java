package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.Trip;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;

@SuppressLint("Registered")
public class StmInfoApiProvider extends MTContentProvider implements StatusProviderContract {

	private static final String TAG = StmInfoApiProvider.class.getSimpleName();

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
	 * Override if multiple {@link StmInfoApiProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link StmInfoApiProvider} implementations in same app.
	 */
	private static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.stm_info_api_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link StmInfoApiProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static final long STM_INFO_API_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(30L);
	private static final long STM_INFO_API_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(5L);
	private static final long STM_INFO_API_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.SECONDS.toMillis(30L);
	private static final long STM_INFO_API_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.SECONDS.toMillis(30L);
	private static final long STM_INFO_API_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.SECONDS.toMillis(30L);

	@Override
	public long getStatusMaxValidityInMs() {
		return STM_INFO_API_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return STM_INFO_API_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return STM_INFO_API_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return STM_INFO_API_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return STM_INFO_API_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Override
	public POIStatus getCachedStatus(Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schecule whithout schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (rts == null || TextUtils.isEmpty(rts.getStop().getCode()) || TextUtils.isEmpty(rts.getRoute().getShortName())) {
			return null;
		}
		String uuid = getAgencyRouteStopTargetUUID(rts);
		POIStatus status = StatusProvider.getCachedStatusS(this, uuid);
		if (status != null) {
			status.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom Clever Devices tags
			if (status instanceof Schedule) {
				((Schedule) status).setDescentOnly(rts.isDescentOnly());
			}
		}
		return status;
	}

	private static String getAgencyRouteStopTargetUUID(RouteTripStop rts) {
		return getAgencyRouteStopTargetUUID(rts.getAuthority(), rts.getRoute().getShortName(), rts.getStop().getCode());
	}

	private static String getAgencyRouteStopTargetUUID(String agencyAuthority, String routeShortName, String stopCode) {
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
		return StmInfoApiDbHelper.T_STM_INFO_API_STATUS;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	@Override
	public POIStatus getNewStatus(Filter statusFilter) {
		if (statusFilter == null || !(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schecule whithout schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (rts == null || TextUtils.isEmpty(rts.getStop().getCode()) || TextUtils.isEmpty(rts.getRoute().getShortName())) {
			return null;
		}
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	private static final String NORTH = "N";
	private static final String SOUTH = "S";
	private static final String EAST = "E";
	private static final String WEST = "W";

	private static final String REAL_TIME_URL_PART_1_BEFORE_LANG = "https://api.stm.info/pub/i3/v1c/api/";
	private static final String REAL_TIME_URL_PART_2_BEFORE_ROUTE_SHORT_NAME = "/lines/";
	private static final String REAL_TIME_URL_PART_3_BEFORE_STOP_CODE = "/stops/";
	private static final String REAL_TIME_URL_PART_4_BEFORE_DIRECTION = "/arrivals?direction=";
	private static final String REAL_TIME_URL_PART_5_BEFORE_LIMIT = "&limit=";

	private static String getRealTimeStatusUrlString(RouteTripStop rts) {
		return new StringBuilder() //
				.append(REAL_TIME_URL_PART_1_BEFORE_LANG) //
				.append(LocaleUtils.isFR() ? "fr" : "en") //
				.append(REAL_TIME_URL_PART_2_BEFORE_ROUTE_SHORT_NAME) //
				.append(rts.getRoute().getShortName()) //
				.append(REAL_TIME_URL_PART_3_BEFORE_STOP_CODE) //
				.append(rts.getStop().getCode()) //
				.append(REAL_TIME_URL_PART_4_BEFORE_DIRECTION) //
				.append(getDirection(rts.getTrip())) //
				.append(REAL_TIME_URL_PART_5_BEFORE_LIMIT) //
				.append(10) //
				.toString();
	}

	private static String getDirection(Trip trip) {
		if (trip.getHeadsignType() == Trip.HEADSIGN_TYPE_DIRECTION) {
			if (Trip.HEADING_EAST.equals(trip.getHeadsignValue())) {
				return EAST;
			} else if (Trip.HEADING_WEST.equals(trip.getHeadsignValue())) {
				return WEST;
			} else if (Trip.HEADING_NORTH.equals(trip.getHeadsignValue())) {
				return NORTH;
			} else if (Trip.HEADING_SOUTH.equals(trip.getHeadsignValue())) {
				return SOUTH;
			}
		}
		MTLog.w(TAG, "Unexpected direction for trip '%s'!", true);
		return StringUtils.EMPTY;
	}

	private static final String APPLICATION_JSON = "application/JSON";
	private static final String ACCEPT = "accept";

	private void loadRealTimeStatusFromWWW(RouteTripStop rts) {
		try {
			String urlString = getRealTimeStatusUrlString(rts);
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			urlc.addRequestProperty("Origin", "http://beta.stm.info");
			urlc.addRequestProperty(ACCEPT, APPLICATION_JSON);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				Collection<POIStatus> statuses = parseAgencyJSON(jsonString, rts, newLastUpdateInMs);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(getAgencyRouteStopTargetUUID(rts)));
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

	private static final TimeZone MONTREAL_TZ = TimeZone.getTimeZone("America/Montreal");

	private Calendar getNewBeginningOfTodayCal() {
		Calendar beginningOfTodayCal = Calendar.getInstance(MONTREAL_TZ);
		beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, 0);
		beginningOfTodayCal.set(Calendar.MINUTE, 0);
		beginningOfTodayCal.set(Calendar.SECOND, 0);
		beginningOfTodayCal.set(Calendar.MILLISECOND, 0);
		return beginningOfTodayCal;
	}

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	private static final String JSON_RESULT = "result";
	private static final String JSON_TIME = "time";
	private static final String JSON_IS_REAL = "is_real";

	private static final ThreadSafeDateFormatter PARSE_TIME = new ThreadSafeDateFormatter("HHmm");

	private Collection<POIStatus> parseAgencyJSON(String jsonString, RouteTripStop rts, long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> result = new ArrayList<POIStatus>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_RESULT)) {
				JSONArray jResults = json.getJSONArray(JSON_RESULT);
				if (jResults != null && jResults.length() > 0) {
					Calendar nowCal = Calendar.getInstance(MONTREAL_TZ);
					long beginningOfTodayInMs = getNewBeginningOfTodayCal().getTimeInMillis();
					Schedule newSchedule = new Schedule(getAgencyRouteStopTargetUUID(rts), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
							PROVIDER_PRECISION_IN_MS, false);
					for (int r = 0; r < jResults.length(); r++) {
						JSONObject jResult = jResults.getJSONObject(r);
						if (jResult != null && jResult.has(JSON_TIME)) {
							String jTime = jResult.getString(JSON_TIME);
							boolean isReal;
							if (jResult.has(JSON_IS_REAL)) {
								isReal = jResult.getBoolean(JSON_IS_REAL);
							} else {
								isReal = jTime.length() != 4;
							}
							long t;
							if (isReal) {
								long countdownInMs = TimeUnit.MINUTES.toMillis(Long.parseLong(jTime));
								t = newLastUpdateInMs + countdownInMs;
								if (countdownInMs == 0L) {
									t += 30L * DateUtils.SECOND_IN_MILLIS; // rounding to the minutes, don't want to hide in countdown
								} else if (t % DateUtils.MINUTE_IN_MILLIS >= 30L * DateUtils.SECOND_IN_MILLIS) {
									t += 30L * DateUtils.SECOND_IN_MILLIS; // rounding to the minutes, don't want to hide in countdown
								}
							} else {
								Calendar beginningOfTodayCal = getNewBeginningOfTodayCal();
								int hour = Integer.parseInt(jTime.substring(0, 2));
								beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, hour);
								int minutes = Integer.parseInt(jTime.substring(2, 4));
								beginningOfTodayCal.set(Calendar.MINUTE, minutes);
								if (beginningOfTodayCal.before(nowCal)) {
									beginningOfTodayCal.add(Calendar.DATE, 1);
								}
								if (rts.getRoute().getId() >= 400L && rts.getRoute().getId() <= 499L) {
									int dayOfTheWeek = beginningOfTodayCal.get(Calendar.DAY_OF_WEEK);
									if (dayOfTheWeek == Calendar.SATURDAY) {
										beginningOfTodayCal.add(Calendar.DATE, 2);
									} else if (dayOfTheWeek == Calendar.SUNDAY) {
										beginningOfTodayCal.add(Calendar.DATE, 1);
									}
								}
								t = beginningOfTodayCal.getTimeInMillis();
							}
							Schedule.Timestamp timestamp = new Schedule.Timestamp(TimeUtils.timeToTheMinuteMillis(t));
							newSchedule.addTimestampWithoutSort(timestamp);
						}
					}
					newSchedule.sortTimestamps();
					result.add(newSchedule);
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

	private static StmInfoApiDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private StmInfoApiDbHelper getDBHelper(Context context) {
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
	 * Override if multiple {@link StmInfoApiProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return StmInfoApiDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link StmInfoApiProvider} implementations in same app.
	 */
	public StmInfoApiDbHelper getNewDbHelper(Context context) {
		return new StmInfoApiDbHelper(context.getApplicationContext());
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

	public static class StmInfoApiDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = StmInfoApiDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link StmInfoApiDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "stm_info_api.db";

		public static final String T_STM_INFO_API_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_STM_INFO_API_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_STM_INFO_API_STATUS).build();

		private static final String T_STM_INFO_API_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STM_INFO_API_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link StmInfoApiDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.stm_info_api_db_version);
			}
			return dbVersion;
		}

		public StmInfoApiDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_STM_INFO_API_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_STM_INFO_API_STATUS_SQL_CREATE);
		}
	}
}
