package org.mtransit.android.commons.provider;

import static org.mtransit.commons.Constants.EMPTY;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Accessibility;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.SourceUtils;
import org.mtransit.commons.provider.CaVancouverTransLinkProviderCommons;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

// The RTTI Open API is deprecated and will no longer be available after December 2, 2024.
// https://www.translink.ca/about-us/doing-business-with-translink/app-developer-resources/rtti
@Deprecated
@SuppressLint("Registered")
public class CaTransLinkProvider extends MTContentProvider implements StatusProviderContract {

	private static final String LOG_TAG = CaTransLinkProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link CaTransLinkProvider} implementations in same app.
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
	 * Override if multiple {@link CaTransLinkProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.ca_translink_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link CaTransLinkProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String apiKey = null;

	/**
	 * Override if multiple {@link CaTransLinkProvider} implementations in same app.
	 */
	@NonNull
	private static String getAPI_KEY(@NonNull Context context) {
		if (apiKey == null) {
			apiKey = context.getResources().getString(R.string.ca_translink_api_key);
		}
		return apiKey;
	}

	private static final long RTTI_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long RTTI_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long RTTI_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long RTTI_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long RTTI_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

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
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Nullable
	@Override
	public POIStatus getCachedStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule without schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, getAgencyRouteStopTargetUUID(rts));
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom TransLink tags
			if (rts.isNoPickup()) {
				if (cachedStatus instanceof Schedule) {
					Schedule schedule = (Schedule) cachedStatus;
					schedule.setNoPickup(true); // API doesn't know about "descent only"
				}
			}
		}
		return cachedStatus;
	}

	@NonNull
	private String getAgencyRouteStopTargetUUID(@NonNull RouteTripStop rts) {
		return getAgencyRouteStopTargetUUID(rts.getAuthority(), rts.getRoute().getShortName(), rts.getStop().getCode());
	}

	@NonNull
	protected static String getAgencyRouteStopTargetUUID(@NonNull String agencyAuthority, @NonNull String routeShortName, @NonNull String stopCode) {
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

	@NonNull
	@Override
	public String getStatusDbTableName() {
		return CaTransLinkDbHelper.T_RTTI_STATUS;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	@Nullable
	@Override
	public POIStatus getNewStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule without schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	// curl -H "Accept: application/json" 'https://api.translink.ca/rttiapi/v1/stops/50030/estimates?apikey=API_KEY&timeframe=720' > output.json
	private static final String REAL_TIME_URL_PART_1_BEFORE_STOP_ID = "https://api.translink.ca/rttiapi/v1/stops/";
	private static final String REAL_TIME_URL_PART_2_AFTER_STOP_ID = "/estimates";
	private static final String REAL_TIME_URL_PART_3_BEFORE_API_KEY = "?apikey=";
	private static final String REAL_TIME_URL_PART_4_BEFORE_TIME_FRAME = "&timeframe=";

	private static final long TIME_FRAME = TimeUnit.HOURS.toMinutes(12L);

	@NonNull
	private static String getRealTimeStatusUrlString(@NonNull String apiKey, @NonNull RouteTripStop rts) {
		return REAL_TIME_URL_PART_1_BEFORE_STOP_ID + //
				rts.getStop().getCode() + //
				REAL_TIME_URL_PART_2_AFTER_STOP_ID + //
				REAL_TIME_URL_PART_3_BEFORE_API_KEY + //
				apiKey + //
				REAL_TIME_URL_PART_4_BEFORE_TIME_FRAME + //
				TIME_FRAME //
				;
	}

	private static final String APPLICATION_JSON = "application/JSON";
	private static final String ACCEPT = "accept";

	private void loadRealTimeStatusFromWWW(@NonNull RouteTripStop rts) {
		try {
			final Context context = requireContextCompat();
			String urlString = getRealTimeStatusUrlString(getAPI_KEY(context), rts);
			String sourceLabel = SourceUtils.getSourceLabel(REAL_TIME_URL_PART_1_BEFORE_STOP_ID);
			MTLog.i(this, "Loading from '%s'...", getRealTimeStatusUrlString("API_KEY", rts));
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			urlc.addRequestProperty(ACCEPT, APPLICATION_JSON);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadRealTimeStatusFromWWW() > jsonString: %s.", jsonString);
				Collection<POIStatus> statuses = parseAgencyJSON(context, jsonString, rts, newLastUpdateInMs, sourceLabel);
				if (statuses != null && !statuses.isEmpty()) {
					HashSet<String> uuids = new HashSet<>();
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
			MTLog.w(LOG_TAG, se, "No Internet Connection!");
		} catch (Exception e) { // Unknown error
			MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception");
		}
	}

	private static final String JSON_ROUTE_NO = "RouteNo";
	private static final String JSON_SCHEDULES = "Schedules";
	private static final String JSON_EXPECTED_LEAVE_TIME = "ExpectedLeaveTime";
	private static final String JSON_DESTINATION = "Destination";
	private static final String JSON_SCHEDULE_STATUS = "ScheduleStatus";
	private static final String JSON_SCHEDULE_STATUS_PLANNED = "*"; // "* indicates scheduled time"

	private static final TimeZone VANCOUVER_TZ = TimeZone.getTimeZone("America/Vancouver");

	private static final ThreadSafeDateFormatter DATE_FORMATTER_WITH_DATE;

	static {
		ThreadSafeDateFormatter dateFormatter = new ThreadSafeDateFormatter("h:mma yyyy-MM-dd", Locale.ENGLISH);
		dateFormatter.setTimeZone(VANCOUVER_TZ);
		DATE_FORMATTER_WITH_DATE = dateFormatter;
	}

	private static final ThreadSafeDateFormatter DATE_FORMATTER_DATE;

	static {
		ThreadSafeDateFormatter dateFormatter = new ThreadSafeDateFormatter("yyyy-MM-dd", Locale.ENGLISH);
		dateFormatter.setTimeZone(VANCOUVER_TZ);
		DATE_FORMATTER_DATE = dateFormatter;
	}

	@NonNull
	private Calendar getNewBeginningOfTodayCal() {
		Calendar beginningOfTodayCal = Calendar.getInstance(VANCOUVER_TZ);
		beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, 0);
		beginningOfTodayCal.set(Calendar.MINUTE, 0);
		beginningOfTodayCal.set(Calendar.SECOND, 0);
		beginningOfTodayCal.set(Calendar.MILLISECOND, 0);
		return beginningOfTodayCal;
	}

	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	@Nullable
	private Collection<POIStatus> parseAgencyJSON(@NonNull Context context, String jsonString, @NonNull RouteTripStop rts, long newLastUpdateInMs, @Nullable String sourceLabel) {
		try {
			ArrayList<POIStatus> result = new ArrayList<>();
			JSONArray jRoutes = jsonString == null ? null : new JSONArray(jsonString);
			if (jRoutes != null && jRoutes.length() > 0) {
				long beginningOfTodayMs = getNewBeginningOfTodayCal().getTimeInMillis();
				for (int nb = 0; nb < jRoutes.length(); nb++) {
					JSONObject jRoute = jRoutes.getJSONObject(nb);
					parseRouteJSON(context, jRoute, beginningOfTodayMs, result, rts, newLastUpdateInMs, sourceLabel);
				}
			}
			return result;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	private void parseRouteJSON(@NonNull Context context, JSONObject jRoute, long beginningOfTodayMs, ArrayList<POIStatus> result, @NonNull RouteTripStop rts, long newLastUpdateInMs, @Nullable String sourceLabel) {
		try {
			if (jRoute == null) {
				return;
			}
			String routeShortName = jRoute.getString(JSON_ROUTE_NO);
			if (TextUtils.isDigitsOnly(routeShortName)) {
				routeShortName = String.valueOf(Integer.parseInt(routeShortName)); // RSN leading '0' removed in parser
			}
			String uuid = getAgencyRouteStopTargetUUID(rts.getAuthority(), routeShortName, rts.getStop().getCode());
			JSONArray jSchedules = jRoute.getJSONArray(JSON_SCHEDULES);
			parseSchedulesJSON(context, jSchedules, beginningOfTodayMs, uuid, result, rts, newLastUpdateInMs, sourceLabel);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while route JSON '%s'!", jRoute);
		}
	}

	private void parseSchedulesJSON(@NonNull Context context, JSONArray jSchedules, long beginningOfTodayMs, String uuid, ArrayList<POIStatus> result, @NonNull RouteTripStop rts,
									long newLastUpdateInMs, @Nullable String sourceLabel) {
		try {
			if (jSchedules == null || jSchedules.length() == 0) {
				return;
			}
			Schedule newSchedule = new Schedule(
					null,
					uuid,
					newLastUpdateInMs,
					getStatusMaxValidityInMs(),
					newLastUpdateInMs,
					PROVIDER_PRECISION_IN_MS,
					false,
					sourceLabel,
					false
			);
			long after = newLastUpdateInMs - TimeUnit.HOURS.toMillis(1L);
			for (int s = 0; s < jSchedules.length(); s++) {
				JSONObject jSchedule = jSchedules.getJSONObject(s);
				parseScheduleJSON(context, jSchedule, beginningOfTodayMs, after, newSchedule, rts);
			}
			newSchedule.sortTimestamps();
			result.add(newSchedule);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while schedules JSON '%s'!", jSchedules);
		}
	}

	private void parseScheduleJSON(@NonNull Context context, JSONObject jSchedule, long beginningOfTodayMs, long after, Schedule newSchedule, @NonNull RouteTripStop rts) {
		try {
			final String expectedLeaveTime = jSchedule.getString(JSON_EXPECTED_LEAVE_TIME);
			final boolean withDate = expectedLeaveTime.length() > 8;
			Date date;
			if (withDate) {
				date = DATE_FORMATTER_WITH_DATE.parseThreadSafe(expectedLeaveTime);
			} else {
				String newExpectedLeaveTime = expectedLeaveTime + " " + DATE_FORMATTER_DATE.formatThreadSafe(beginningOfTodayMs);
				date = DATE_FORMATTER_WITH_DATE.parseThreadSafe(newExpectedLeaveTime);
			}
			if (date == null) {
				return;
			}
			long t;
			//noinspection IfStatementWithIdenticalBranches
			if (withDate) {
				t = date.getTime();
			} else {
				if (date.getTime() < after) {
					String newExpectedLeaveTime = expectedLeaveTime + " " + DATE_FORMATTER_DATE.formatThreadSafe(beginningOfTodayMs + TimeUnit.DAYS.toMillis(1L));
					date = DATE_FORMATTER_WITH_DATE.parseThreadSafe(newExpectedLeaveTime);
					if (date == null) {
						return;
					}
				}
				t = date.getTime();
			}
			Schedule.Timestamp newTimestamp = new Schedule.Timestamp(TimeUtils.timeToTheTensSecondsMillis(t));
			String destination = cleanTripHeadsign(context, parseDestinationJSON(jSchedule), rts);
			if (!TextUtils.isEmpty(destination)) {
				newTimestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, destination);
			}
			if (jSchedule.has(JSON_SCHEDULE_STATUS)) {
				String scheduleStatus = jSchedule.optString(JSON_SCHEDULE_STATUS);
				newTimestamp.setRealTime(!JSON_SCHEDULE_STATUS_PLANNED.equals(scheduleStatus));
			}
			if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
				newTimestamp.setAccessible(Accessibility.UNKNOWN); // no info available on https://www.translink.ca/next-bus
			}
			newSchedule.addTimestampWithoutSort(newTimestamp);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while processing schedule %s!", jSchedule);
		}
	}

	@Nullable
	private String parseDestinationJSON(JSONObject jSchedule) {
		try {
			if (jSchedule != null && jSchedule.has(JSON_DESTINATION)) {
				return jSchedule.getString(JSON_DESTINATION);
			}
			return null;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing destination form '%s'!", jSchedule);
			return null;
		}
	}

	@Nullable
	private String cleanTripHeadsign(@NonNull Context context, @Nullable String tripHeadsign, @NonNull RouteTripStop rts) {
		try {
			if (TextUtils.isEmpty(tripHeadsign)) {
				return tripHeadsign;
			}
			tripHeadsign = CaVancouverTransLinkProviderCommons.cleanTripHeadsign(tripHeadsign);
			final String tripHeading = rts.getTrip().getHeading(context);
			final String routeLongName = rts.getRoute().getLongName();
			tripHeadsign = CleanUtils.removeStrings(tripHeadsign, tripHeading, routeLongName);
			tripHeadsign = CleanUtils.keepOrRemoveVia(tripHeadsign, string ->
					Trip.isSameHeadsign(string, tripHeading)
							|| Trip.isSameHeadsign(string, routeLongName)
			);
			tripHeadsign = CaVancouverTransLinkProviderCommons.REMOVE_DASH_START_END.matcher(tripHeadsign).replaceAll(EMPTY);
			return tripHeadsign;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadsign);
			return tripHeadsign;
		}
	}

	@MainThread
	@Override
	public boolean onCreateMT() {
		ping();
		return true;
	}

	@Override
	public void ping() {
		// DO NOTHING
	}

	@Nullable
	private CaTransLinkDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private CaTransLinkDbHelper getDBHelper(@NonNull Context context) {
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
		return CaTransLinkDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link CaTransLinkProvider} implementations in same app.
	 */
	@NonNull
	public CaTransLinkDbHelper getNewDbHelper(@NonNull Context context) {
		return new CaTransLinkDbHelper(context.getApplicationContext());
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(requireContextCompat());
	}

	@NonNull
	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITY_URI(requireContextCompat());
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

	@Nullable
	@Override
	public Cursor queryMT(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		Cursor cursor = StatusProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
	}

	@Nullable
	@Override
	public String getTypeMT(@NonNull Uri uri) {
		String type = StatusProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
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

	public static class CaTransLinkDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = CaTransLinkDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link CaTransLinkDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "catranslink.db";

		static final String T_RTTI_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_RTTI_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_RTTI_STATUS).build();

		private static final String T_RTTI_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_RTTI_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link CaTransLinkDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.ca_translink_db_version);
			}
			return dbVersion;
		}

		CaTransLinkDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_RTTI_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_RTTI_STATUS_SQL_CREATE);
		}
	}
}
