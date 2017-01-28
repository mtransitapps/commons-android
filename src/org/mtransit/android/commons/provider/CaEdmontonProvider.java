package org.mtransit.android.commons.provider;

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

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.CleanUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
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
import android.util.SparseArray;

@SuppressLint("Registered")
public class CaEdmontonProvider extends MTContentProvider implements StatusProviderContract {

	private static final String TAG = CaEdmontonProvider.class.getSimpleName();

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
	 * Override if multiple {@link CaEdmontonProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link CaEdmontonProvider} implementations in same app.
	 */
	private static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.ca_edmonton_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link CaEdmontonProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static final long ETSLIVE_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long ETSLIVE_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long ETSLIVE_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long ETSLIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long ETSLIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

	@Override
	public long getStatusMaxValidityInMs() {
		return ETSLIVE_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return ETSLIVE_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return ETSLIVE_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return ETSLIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return ETSLIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
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
		return CaEdmontonDbHelper.T_ETSLIVE_STATUS;
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
		if (rts == null || TextUtils.isEmpty(rts.getStop().getCode()) || TextUtils.isEmpty(rts.getRoute().getShortName())) {
			return null;
		}
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	private static final String ETSLIVE_URL = "https://etslive.edmonton.ca/InfoWeb";

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

	private static String getJSONPostParameters(Context context, RouteTripStop rts) {
		if (TextUtils.isEmpty(rts.getStop().getCode())) {
			MTLog.w(TAG, "Can't create real-time status JSON (no stop code) for %s", rts);
			return null;
		}
		if (TextUtils.isEmpty(rts.getRoute().getShortName())) {
			MTLog.w(TAG, "Can't create real-time status JSON (no route short name) for %s", rts);
			return null;
		}
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_VERSION, JSON_VERSION_1_1);
			json.put(JSON_METHOD, JSON_METHOD_GET_BUS_TIMES);
			JSONObject jParams = new JSONObject();
			jParams.put(JSON_STOP_ABBR, Integer.parseInt(rts.getStop().getCode()));
			jParams.put(JSON_LINE_ABBR, Integer.parseInt(rts.getRoute().getShortName()));
			jParams.put(JSON_NUM_TIMES_PER_LINE, JSON_NUM_TIMES_PER_LINE_COUNT);
			jParams.put(JSON_NUM_STOP_TIMES, JSON_NUM_STOP_TIMES_COUNT);
			json.put(JSON_PARAMS, jParams);
			return json.toString();
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while creating JSON POST parameters for '%s'!", rts);
			return null;
		}
	}

	private void loadRealTimeStatusFromWWW(RouteTripStop rts) {
		try {
			String urlString = ETSLIVE_URL;
			String jsonPostParams = getJSONPostParameters(getContext(), rts);
			if (TextUtils.isEmpty(jsonPostParams)) {
				MTLog.w(this, "loadPredictionsFromWWW() > skip (invalid JSON post parameters!)");
				return;
			}
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			try {
				httpUrlConnection.setDoOutput(true);
				httpUrlConnection.setRequestMethod("POST");
				httpUrlConnection.addRequestProperty("Content-Type", "application/json");
				OutputStream os = httpUrlConnection.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, FileUtils.UTF_8));
				writer.write(jsonPostParams);
				writer.flush();
				writer.close();
				os.close();
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(httpUrlConnection.getInputStream());
				Collection<POIStatus> statuses = parseAgencyJSON(jsonString, rts, newLastUpdateInMs);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(getAgencyRouteStopTargetUUID(rts)));
				if (statuses != null) {
					for (POIStatus status : statuses) {
						StatusProvider.cacheStatusS(this, status);
					}
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
			MTLog.w(TAG, se, "No Internet Connection!");
		} catch (Exception e) { // Unknown error
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
		}
	}

	private static final TimeZone EDMONTON_TZ = TimeZone.getTimeZone("America/Edmonton");

	private Calendar getNewBeginningOfTodayCal() {
		Calendar beginningOfTodayCal = Calendar.getInstance(EDMONTON_TZ);
		beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, 0);
		beginningOfTodayCal.set(Calendar.MINUTE, 0);
		beginningOfTodayCal.set(Calendar.SECOND, 0);
		beginningOfTodayCal.set(Calendar.MILLISECOND, 0);
		return beginningOfTodayCal;
	}

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10);

	private static final String JSON_RESULT = "result";
	private static final String JSON_STOP_TIME_RESULT = "StopTimeResult";
	private static final String JSON_STOP_TIMES = "StopTimes";
	private static final String JSON_TRIP_ID = "TripId";
	private static final String JSON_DESTINATION_SIGN = "DestinationSign";
	private static final String JSON_REAL_TIME_RESULTS = "RealTimeResults";
	private static final String JSON_REAL_TIME = "RealTime";

	private Collection<POIStatus> parseAgencyJSON(String jsonString, RouteTripStop rts, long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> result = new ArrayList<POIStatus>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_RESULT)) {
				JSONArray jResults = json.getJSONArray(JSON_RESULT);
				if (jResults != null && jResults.length() > 0) {
					long beginningOfTodayInMs = getNewBeginningOfTodayCal().getTimeInMillis();
					Schedule newSchedule = new Schedule(getAgencyRouteStopTargetUUID(rts), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
							PROVIDER_PRECISION_IN_MS, false);
					for (int r = 0; r < jResults.length(); r++) {
						JSONObject jResult = jResults.getJSONObject(r);
						SparseArray<String> tripIdDestinationSigns = extractTripIdDestinations(jResult);
						if (jResult != null && jResult.has(JSON_REAL_TIME_RESULTS)) {
							JSONArray jRealTimeResults = jResult.getJSONArray(JSON_REAL_TIME_RESULTS);
							if (jRealTimeResults != null && jRealTimeResults.length() > 0) {
								for (int rtr = 0; rtr < jRealTimeResults.length(); rtr++) {
									JSONObject jRealTimeResult = jRealTimeResults.getJSONObject(rtr);
									if (jRealTimeResult != null && jRealTimeResult.has(JSON_REAL_TIME)) {
										int nbSecondsSinceMorning = jRealTimeResult.getInt(JSON_REAL_TIME);
										long t = beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(nbSecondsSinceMorning);
										Schedule.Timestamp timestamp = new Schedule.Timestamp(TimeUtils.timeToTheTensSecondsMillis(t));
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
			return result;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	private SparseArray<String> extractTripIdDestinations(JSONObject jResult) {
		SparseArray<String> tripIdDestinationSigns = new SparseArray<String>();
		try {
			if (jResult != null && jResult.has(JSON_STOP_TIME_RESULT)) {
				JSONArray jStopTimeResults = jResult.getJSONArray(JSON_STOP_TIME_RESULT);
				if (jStopTimeResults != null && jStopTimeResults.length() > 0) {
					for (int str = 0; str < jStopTimeResults.length(); str++) {
						JSONObject jStopTimeResult = jStopTimeResults.getJSONObject(str);
						if (jStopTimeResult != null && jStopTimeResult.has(JSON_STOP_TIMES)) {
							JSONArray jStopTimes = jStopTimeResult.getJSONArray(JSON_STOP_TIMES);
							if (jStopTimes != null && jStopTimes.length() > 0) {
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

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^[\\d]+\\s)", Pattern.CASE_INSENSITIVE);

	private static final Pattern WEST_EDMONTON_MALL = Pattern.compile("((^|\\W){1}(west edmonton mall)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String WEST_EDMONTON_MALL_REPLACEMENT = "$2WEM$4";

	private static final Pattern EDMONTON = Pattern.compile("((^|\\W){1}(edmonton)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String EDMONTON_REPLACEMENT = "$2Edm$4";

	private static final Pattern TRANSIT_CENTER = Pattern.compile("((^|\\W){1}(transit center|transit centre)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String TRANSIT_CENTER_REPLACEMENT = "$2TC$4";

	private static final Pattern TOWN_CENTER = Pattern.compile("((^|\\W){1}(town center|town centre)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String TOWN_CENTER_REPLACEMENT = "$2TC$4";

	private static final String VIA = " via ";

	private String cleanTripHeadsign(String tripHeadsign) {
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

	private static CaEdmontonDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private CaEdmontonDbHelper getDBHelper(Context context) {
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
		return CaEdmontonDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link CaEdmontonProvider} implementations in same app.
	 */
	public CaEdmontonDbHelper getNewDbHelper(Context context) {
		return new CaEdmontonDbHelper(context.getApplicationContext());
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

	public static class CaEdmontonDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = CaEdmontonDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link CaEdmontonDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "ca_edmonton.db";

		public static final String T_ETSLIVE_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_ETSLIVE_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_ETSLIVE_STATUS).build();

		private static final String T_ETSLIVE_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_ETSLIVE_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link CaEdmontonDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.ca_edmonton_db_version);
			}
			return dbVersion;
		}

		public CaEdmontonDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_ETSLIVE_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_ETSLIVE_STATUS_SQL_CREATE);
		}
	}
}
