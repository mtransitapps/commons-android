package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.CleanUtils;
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
import org.mtransit.android.commons.data.Trip;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

@SuppressLint("Registered")
public class GreaterSudburyProvider extends MTContentProvider implements StatusProviderContract {

	private static final String LOG_TAG = GreaterSudburyProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
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
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.greater_sudbury_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String authToken = null;

	/**
	 * Override if multiple {@link GreaterSudburyProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTH_TOKEN(@NonNull Context context) {
		if (authToken == null) {
			authToken = context.getResources().getString(R.string.greater_sudbury_auth_token);
		}
		return authToken;
	}

	private static final long MYBUS_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long MYBUS_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long MYBUS_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long MYBUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long MYBUS_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

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
			MTLog.w(this, "getCachedStatus() > Can't find new schedule without schedule filter!");
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

	private static String getAgencyCall(@NonNull RouteTripStop rts) {
		return POI.POIUtils.getUUID(rts.getAuthority(), rts.getStop().getCode());
	}

	private static String getAgencyRouteStopTargetUUID(@NonNull RouteTripStop rts) {
		return getAgencyRouteStopTargetUUID(rts.getAuthority(), rts.getRoute().getShortName(), extractTripId(rts.getTrip().getId()), rts.getStop().getCode());
	}

	private static String getAgencyRouteStopTargetUUID(String agencyAuthority, String routeShortName, long tripId, String stopCode) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName, tripId, stopCode);
	}

	private static long extractTripId(long id) {
		return id % 10L;
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
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule without schedule filter!");
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

	@Nullable
	private static String getRealTimeStatusUrlString(@NonNull Context context, @NonNull RouteTripStop rts) {
		if (TextUtils.isEmpty(rts.getStop().getCode())) {
			MTLog.w(LOG_TAG, "Can't create real-time status URL (no stop code) for %s", rts);
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
			if (TextUtils.isEmpty(urlString)) {
				return;
			}
			MTLog.i(this, "Loading from '%s' for stop '%s'...", REAL_TIME_URL_PART_1_BEFORE_STOP_CODE, rts.getStop().getCode());
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				Collection<? extends POIStatus> statuses = parseAgencyJSON(jsonString, rts, newLastUpdateInMs);
				if (statuses != null && statuses.size() > 0) {
					HashSet<String> targetUUIDs = new HashSet<>();
					for (POIStatus status : statuses) {
						targetUUIDs.add(status.getTargetUUID());
					}
					StatusProvider.deleteCachedStatus(this, targetUUIDs);
					for (POIStatus status : statuses) {
						StatusProvider.cacheStatusS(this, status);
					}
					StatusProvider.deleteCachedStatus(this, Collections.singletonList(getAgencyCall(rts)));
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
			MTLog.w(LOG_TAG, se, "No Internet Connection!");
		} catch (Exception e) { // Unknown error
			MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception");
		}
	}

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	private static final String JSON_STOP = "stop";
	private static final String JSON_NAME = "name";
	private static final String JSON_NUMBER = "number";
	private static final String JSON_CALLS = "calls";
	private static final String JSON_PASSING_TIME = "passing_time";
	private static final String JSON_ROUTE = "route";
	private static final String JSON_DESTINATION = "destination";

	private static final ThreadSafeDateFormatter DATE_FORMATTER = new ThreadSafeDateFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.ENGLISH);

	private Collection<? extends POIStatus> parseAgencyJSON(@Nullable String jsonString, @NonNull RouteTripStop rts, long newLastUpdateInMs) {
		try {
			ArrayMap<String, Schedule> result = new ArrayMap<>();
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
								Schedule.Timestamp timestamp = new Schedule.Timestamp(t);
								try {
									if (jDestination.has(JSON_NAME)) {
										String jDestinationName = jDestination.getString(JSON_NAME);
										if (!TextUtils.isEmpty(jDestinationName)) {
											timestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, cleanTripHeadsign(jDestinationName));
										}
									}
								} catch (Exception e) {
									MTLog.w(this, e, "Error while adding destination name %s!", jDestination);
								}
								result.get(targetUUID).addTimestampWithoutSort(timestamp);
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

	private static final String LAURENTIAN_UNIVERSITY = "Laurentian U";
	private static final String LAURENTIAN_UNIVERSITY_FR = "U Laurentienne";

	private static final Pattern CLEAN_UNIVERISITY = Pattern.compile("((^|\\W){1}(laurentian university)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_UNIVERISITY_REPLACEMENT = "$2" + LAURENTIAN_UNIVERSITY + "$4";

	private static final Pattern CLEAN_UNIVERISITY_FR = Pattern.compile("((^|\\W){1}(universit[e|é] laurentienne)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_UNIVERISITY_FR_REPLACEMENT = "$2" + LAURENTIAN_UNIVERSITY_FR + "$4";

	private static final Pattern SUDBURY_SHOPPING_CENTER = Pattern.compile("(subdury shopping centre)", Pattern.CASE_INSENSITIVE);
	private static final String SUDBURY_SHOPPING_CENTER_REPLACEMENT = "Subdury centre";

	private String cleanTripHeadsign(String tripHeadsign) {
		try {
			tripHeadsign = SUDBURY_SHOPPING_CENTER.matcher(tripHeadsign).replaceAll(SUDBURY_SHOPPING_CENTER_REPLACEMENT);
			tripHeadsign = CLEAN_UNIVERISITY.matcher(tripHeadsign).replaceAll(CLEAN_UNIVERISITY_REPLACEMENT);
			tripHeadsign = CLEAN_UNIVERISITY_FR.matcher(tripHeadsign).replaceAll(CLEAN_UNIVERISITY_FR_REPLACEMENT);
			tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
			tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
			tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
			tripHeadsign = CleanUtils.removePoints(tripHeadsign);
			return CleanUtils.cleanLabel(tripHeadsign);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadsign);
			return tripHeadsign;
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

	@Nullable
	private GreaterSudburyDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private GreaterSudburyDbHelper getDBHelper(@NonNull Context context) {
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
	@NonNull
	public GreaterSudburyDbHelper getNewDbHelper(@NonNull Context context) {
		return new GreaterSudburyDbHelper(context.getApplicationContext());
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	@NonNull
	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITY_URI(getContext());
	}

	@NonNull
	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	@Override
	public Cursor queryMT(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor cursor = StatusProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
	}

	@Override
	public String getTypeMT(@NonNull Uri uri) {
		String type = StatusProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
	}

	@Override
	public int deleteMT(@NonNull Uri uri, String selection, String[] selectionArgs) {
		MTLog.w(this, "The delete method is not available.");
		return 0;
	}

	@Override
	public int updateMT(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		MTLog.w(this, "The update method is not available.");
		return 0;
	}

	@Override
	public Uri insertMT(@NonNull Uri uri, ContentValues values) {
		MTLog.w(this, "The insert method is not available.");
		return null;
	}

	public static class GreaterSudburyDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = GreaterSudburyDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return LOG_TAG;
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
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.greater_sudbury_db_version);
			}
			return dbVersion;
		}

		public GreaterSudburyDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_MYBUS_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_MYBUS_STATUS_SQL_CREATE);
		}
	}
}
