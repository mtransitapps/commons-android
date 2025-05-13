package org.mtransit.android.commons.provider;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
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

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@SuppressLint("Registered")
public class CaEdmontonProvider extends MTContentProvider implements StatusProviderContract {

	private static final String LOG_TAG = CaEdmontonProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link CaEdmontonProvider} implementations in same app.
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
	 * Override if multiple {@link CaEdmontonProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.ca_edmonton_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link CaEdmontonProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static final long ETS_LIVE_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long ETS_LIVE_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long ETS_LIVE_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long ETS_LIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long ETS_LIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getStatusMaxValidityInMs() {
		return ETS_LIVE_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return ETS_LIVE_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return ETS_LIVE_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return ETS_LIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return ETS_LIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
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
		if (TextUtils.isEmpty(rts.getStop().getCode()) || TextUtils.isEmpty(rts.getRoute().getShortName())) {
			return null;
		}
		String uuid = getAgencyRouteStopTargetUUID(rts);
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, uuid);
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom Clever Devices tags
			if (rts.isNoPickup()) {
				if (cachedStatus instanceof Schedule) {
					Schedule schedule = (Schedule) cachedStatus;
					schedule.setNoPickup(true); // API doesn't know about "descent only" & doesn't return drop off time for last stop
				}
			}
		}
		return cachedStatus;
	}

	@NonNull
	private static String getAgencyRouteStopTargetUUID(@NonNull RouteTripStop rts) {
		return getAgencyRouteStopTargetUUID(rts.getAuthority(), rts.getRoute().getShortName(), rts.getStop().getCode());
	}

	@NonNull
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

	@NonNull
	@Override
	public String getStatusDbTableName() {
		return CaEdmontonDbHelper.T_ETS_LIVE_STATUS;
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
		if (TextUtils.isEmpty(rts.getStop().getCode()) || TextUtils.isEmpty(rts.getRoute().getShortName())) {
			return null;
		}
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	private static final String ETS_LIVE_URL = "https://etslive.edmonton.ca/InfoWeb";

	private static final String JSON_VERSION = "version";
	private static final String JSON_METHOD = "method";
	private static final String JSON_PARAMS = "params";
	private static final String JSON_STOP_ABBR = "StopAbbr";
	private static final String JSON_LINE_ABBR = "LineAbbr";
	private static final String JSON_NUM_TIMES_PER_LINE = "NumTimesPerLine";
	private static final String JSON_NUM_STOP_TIMES = "NumStopTimes";

	private static final String JSON_VERSION_1_1 = "1.1";
	private static final String JSON_METHOD_GET_BUS_TIMES = "GetBusTimes";
	private static final int JSON_NUM_TIMES_PER_LINE_COUNT = 15;
	private static final int JSON_NUM_STOP_TIMES_COUNT = 40;

	@Nullable
	private static String getJSONPostParameters(@NonNull RouteTripStop rts) {
		String stopCode = rts.getStop().getCode();
		String rsn = rts.getRoute().getShortName();
		if (TextUtils.isEmpty(stopCode)) {
			MTLog.w(LOG_TAG, "Can't create real-time status JSON (invalid stop code) for %s", rts);
			return null;
		}
		if (TextUtils.isEmpty(rsn)) {
			MTLog.w(LOG_TAG, "Can't create real-time status JSON (invalid route short name) for %s", rts);
			return null;
		}
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_VERSION, JSON_VERSION_1_1);
			json.put(JSON_METHOD, JSON_METHOD_GET_BUS_TIMES);
			JSONObject jParams = new JSONObject();
			jParams.put(JSON_STOP_ABBR, stopCode);
			jParams.put(JSON_LINE_ABBR, rsn);
			jParams.put(JSON_NUM_TIMES_PER_LINE, JSON_NUM_TIMES_PER_LINE_COUNT);
			jParams.put(JSON_NUM_STOP_TIMES, JSON_NUM_STOP_TIMES_COUNT);
			json.put(JSON_PARAMS, jParams);
			return json.toString();
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while creating JSON POST parameters for '%s'!", rts);
			return null;
		}
	}

	private void loadRealTimeStatusFromWWW(@NonNull RouteTripStop rts) {
		try {
			//noinspection UnnecessaryLocalVariable
			String urlString = ETS_LIVE_URL;
			String sourceLabel = SourceUtils.getSourceLabel(urlString);
			String jsonPostParams = getJSONPostParameters(rts);
			MTLog.i(this, "Loading from '%s' for stop '%s'...", ETS_LIVE_URL, rts.getStop().getCode());
			MTLog.d(this, "loadRealTimeStatusFromWWW() > jsonPostParams: %s.", jsonPostParams);
			if (TextUtils.isEmpty(jsonPostParams)) {
				MTLog.w(this, "loadPredictionsFromWWW() > skip (invalid JSON post parameters!)");
				return;
			}
			URL url = new URL(urlString);
			URLConnection urlConnect = url.openConnection();
			NetworkUtils.setupUrlConnection(urlConnect);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlConnect;
			try {
				httpUrlConnection.setDoOutput(true);
				httpUrlConnection.setRequestMethod("POST");
				httpUrlConnection.addRequestProperty("Content-Type", "application/json");
				OutputStream os = httpUrlConnection.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, FileUtils.getUTF8()));
				writer.write(jsonPostParams);
				writer.flush();
				writer.close();
				os.close();
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(httpUrlConnection.getInputStream());
				MTLog.d(this, "loadRealTimeStatusFromWWW() > jsonString: %s.", jsonString);
				Collection<POIStatus> statuses = parseAgencyJSON(jsonString, rts, newLastUpdateInMs, sourceLabel);
				MTLog.i(this, "Found %d statuses.", statuses.size());
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(getAgencyRouteStopTargetUUID(rts)));
				for (POIStatus status : statuses) {
					StatusProvider.cacheStatusS(this, status);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while posting query!");
			} finally {
				httpUrlConnection.disconnect();
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

	private static final TimeZone EDMONTON_TZ = TimeZone.getTimeZone("America/Edmonton");

	@NonNull
	private Calendar getNewBeginningOfTodayCal() {
		Calendar beginningOfTodayCal = Calendar.getInstance(EDMONTON_TZ);
		beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, 0);
		beginningOfTodayCal.set(Calendar.MINUTE, 0);
		beginningOfTodayCal.set(Calendar.SECOND, 0);
		beginningOfTodayCal.set(Calendar.MILLISECOND, 0);
		return beginningOfTodayCal;
	}

	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	private static final String JSON_RESULT = "result";
	private static final String JSON_STOP_TIME_RESULT = "StopTimeResult";
	private static final String JSON_STOP_TIMES = "StopTimes";
	private static final String JSON_TRIP_ID = "TripId";
	private static final String JSON_DESTINATION_SIGN = "DestinationSign";
	private static final String JSON_REAL_TIME_RESULTS = "RealTimeResults";
	private static final String JSON_REAL_TIME = "RealTime";
	private static final String JSON_IGNORE_ADHERENCE = "IgnoreAdherence";

	@NonNull
	private Collection<POIStatus> parseAgencyJSON(@Nullable String jsonString, @NonNull RouteTripStop rts, long newLastUpdateInMs, @Nullable String sourceLabel) {
		ArrayList<POIStatus> result = new ArrayList<>();
		try {
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_RESULT)) {
				JSONArray jResults = json.getJSONArray(JSON_RESULT);
				if (jResults.length() > 0) {
					final long beginningOfTodayInMs = getNewBeginningOfTodayCal().getTimeInMillis();
					final Schedule newSchedule = new Schedule(
							null,
							getAgencyRouteStopTargetUUID(rts),
							newLastUpdateInMs,
							getStatusMaxValidityInMs(),
							newLastUpdateInMs,
							PROVIDER_PRECISION_IN_MS,
							false,
							sourceLabel,
							false
					);
					for (int r = 0; r < jResults.length(); r++) {
						JSONObject jResult = jResults.getJSONObject(r);
						SparseArray<String> tripIdDestinationSigns = extractTripIdDestinations(jResult);
						if (jResult != null && jResult.has(JSON_REAL_TIME_RESULTS)) {
							JSONArray jRealTimeResults = jResult.getJSONArray(JSON_REAL_TIME_RESULTS);
							if (jRealTimeResults.length() > 0) {
								for (int rtr = 0; rtr < jRealTimeResults.length(); rtr++) {
									JSONObject jRealTimeResult = jRealTimeResults.getJSONObject(rtr);
									if (jRealTimeResult != null && jRealTimeResult.has(JSON_REAL_TIME)) {
										int nbSecondsSinceMorning = jRealTimeResult.getInt(JSON_REAL_TIME);
										long t = beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(nbSecondsSinceMorning);
										Schedule.Timestamp timestamp = new Schedule.Timestamp(TimeUtils.timeToTheTensSecondsMillis(t), EDMONTON_TZ);
										try {
											if (jRealTimeResult.has(JSON_TRIP_ID)) {
												int tripId = jRealTimeResult.getInt(JSON_TRIP_ID);
												String destinationSign = tripIdDestinationSigns.get(tripId);
												if (!TextUtils.isEmpty(destinationSign)) {
													timestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, cleanTripHeadsign(destinationSign));
												}
											}
										} catch (Exception e) {
											MTLog.w(this, e, "Error while adding destination sign %s!", jRealTimeResult);
										}
										if (jRealTimeResult.has(JSON_IGNORE_ADHERENCE)) {
											timestamp.setRealTime(!jRealTimeResult.optBoolean(JSON_IGNORE_ADHERENCE, true));
										}
										if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
											timestamp.setAccessible(Accessibility.UNKNOWN); // no info available on https://etslive.edmonton.ca/
										}
										newSchedule.addTimestampWithoutSort(timestamp);
									}
								}
							}
						}
					}
					newSchedule.sortTimestamps();
					result.add(newSchedule);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
		}
		return result;
	}

	@NonNull
	private SparseArray<String> extractTripIdDestinations(@Nullable JSONObject jResult) {
		SparseArray<String> tripIdDestinationSigns = new SparseArray<>();
		try {
			if (jResult != null && jResult.has(JSON_STOP_TIME_RESULT)) {
				JSONArray jStopTimeResults = jResult.getJSONArray(JSON_STOP_TIME_RESULT);
				if (jStopTimeResults.length() > 0) {
					for (int str = 0; str < jStopTimeResults.length(); str++) {
						JSONObject jStopTimeResult = jStopTimeResults.getJSONObject(str);
						if (jStopTimeResult != null && jStopTimeResult.has(JSON_STOP_TIMES)) {
							JSONArray jStopTimes = jStopTimeResult.getJSONArray(JSON_STOP_TIMES);
							if (jStopTimes.length() > 0) {
								for (int st = 0; st < jStopTimes.length(); st++) {
									JSONObject jStopTime = jStopTimes.getJSONObject(st);
									try {
										if (jStopTime != null && jStopTime.has(JSON_TRIP_ID) && jStopTime.has(JSON_DESTINATION_SIGN)) {
											tripIdDestinationSigns.put(jStopTime.getInt(JSON_TRIP_ID), jStopTime.getString(JSON_DESTINATION_SIGN));
										}
									} catch (Exception e) {
										MTLog.w(this, e, "Error while parsing trip destination %s!", jStopTime);
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing trip destinations!");
		}
		return tripIdDestinationSigns;
	}

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^\\d+\\s)", Pattern.CASE_INSENSITIVE);

	private static final Pattern WEST_EDMONTON_MALL = Pattern.compile("((^|\\W)(west edmonton mall)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String WEST_EDMONTON_MALL_REPLACEMENT = "$2" + "WEM" + "$4";

	private static final Pattern EDMONTON = Pattern.compile("((^|\\W)(edmonton)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String EDMONTON_REPLACEMENT = "$2" + "Edm" + "$4";

	private static final Pattern TRANSIT_CENTER = Pattern.compile("((^|\\W)(transit center|transit centre)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String TRANSIT_CENTER_REPLACEMENT = "$2" + "TC" + "$4";

	private static final Pattern TOWN_CENTER = Pattern.compile("((^|\\W)(town center|town centre)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String TOWN_CENTER_REPLACEMENT = "$2" + "TC" + "$4";

	private static final String VIA = " via ";

	@NonNull
	private String cleanTripHeadsign(@NonNull String tripHeadsign) {
		try {
			int indexOfVIA = tripHeadsign.toLowerCase(Locale.ENGLISH).indexOf(VIA);
			if (indexOfVIA >= 0) {
				tripHeadsign = tripHeadsign.substring(0, indexOfVIA);
			}
			tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
			tripHeadsign = WEST_EDMONTON_MALL.matcher(tripHeadsign).replaceAll(WEST_EDMONTON_MALL_REPLACEMENT);
			tripHeadsign = EDMONTON.matcher(tripHeadsign).replaceAll(EDMONTON_REPLACEMENT);
			tripHeadsign = TRANSIT_CENTER.matcher(tripHeadsign).replaceAll(TRANSIT_CENTER_REPLACEMENT);
			tripHeadsign = TOWN_CENTER.matcher(tripHeadsign).replaceAll(TOWN_CENTER_REPLACEMENT);
			tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
			tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
			return CleanUtils.cleanLabel(Locale.ENGLISH, tripHeadsign);
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
	private CaEdmontonDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private CaEdmontonDbHelper getDBHelper(@NonNull Context context) {
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
	 * Override if multiple {@link CaEdmontonProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return CaEdmontonDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link CaEdmontonProvider} implementations in same app.
	 */
	@NonNull
	public CaEdmontonDbHelper getNewDbHelper(@NonNull Context context) {
		return new CaEdmontonDbHelper(context.getApplicationContext());
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

	public static class CaEdmontonDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = CaEdmontonDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link CaEdmontonDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "ca_edmonton.db";

		static final String T_ETS_LIVE_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_ETS_LIVE_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_ETS_LIVE_STATUS).build();

		private static final String T_ETS_LIVE_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_ETS_LIVE_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link CaEdmontonDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.ca_edmonton_db_version);
			}
			return dbVersion;
		}

		CaEdmontonDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_ETS_LIVE_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_ETS_LIVE_STATUS_SQL_CREATE);
		}
	}
}
