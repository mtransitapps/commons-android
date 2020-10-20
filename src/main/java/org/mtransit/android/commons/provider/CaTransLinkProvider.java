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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.CleanUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		POIStatus status = StatusProvider.getCachedStatusS(this, getAgencyRouteStopTargetUUID(rts));
		if (status != null) {
			status.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom TransLink tags
			if (status instanceof Schedule) {
				((Schedule) status).setDescentOnly(rts.isDescentOnly());
			}
		}
		return status;
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
			final Context context = getContext();
			if (context == null) {
				return;
			}
			String urlString = getRealTimeStatusUrlString(getAPI_KEY(context), rts);
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
				Collection<POIStatus> statuses = parseAgencyJSON(jsonString, rts, newLastUpdateInMs);
				if (statuses != null && statuses.size() > 0) {
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

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	private Collection<POIStatus> parseAgencyJSON(String jsonString, @NonNull RouteTripStop rts, long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> result = new ArrayList<>();
			JSONArray jRoutes = jsonString == null ? null : new JSONArray(jsonString);
			if (jRoutes != null && jRoutes.length() > 0) {
				long beginningOfTodayMs = getNewBeginningOfTodayCal().getTimeInMillis();
				for (int nb = 0; nb < jRoutes.length(); nb++) {
					JSONObject jRoute = jRoutes.getJSONObject(nb);
					parseRouteJSON(jRoute, beginningOfTodayMs, result, rts, newLastUpdateInMs);
				}
			}
			return result;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	private void parseRouteJSON(JSONObject jRoute, long beginningOfTodayMs, ArrayList<POIStatus> result, @NonNull RouteTripStop rts, long newLastUpdateInMs) {
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
			parseSchedulesJSON(jSchedules, beginningOfTodayMs, uuid, result, rts, newLastUpdateInMs);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while route JSON '%s'!", jRoute);
		}
	}

	private void parseSchedulesJSON(JSONArray jSchedules, long beginningOfTodayMs, String uuid, ArrayList<POIStatus> result, @NonNull RouteTripStop rts,
									long newLastUpdateInMs) {
		try {
			if (jSchedules == null || jSchedules.length() == 0) {
				return;
			}
			Schedule newSchedule = new Schedule(uuid, newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs, PROVIDER_PRECISION_IN_MS, false);
			long after = newLastUpdateInMs - TimeUnit.HOURS.toMillis(1L);
			for (int s = 0; s < jSchedules.length(); s++) {
				JSONObject jSchedule = jSchedules.getJSONObject(s);
				parseScheduleJSON(jSchedule, beginningOfTodayMs, after, newSchedule, rts);
			}
			newSchedule.sortTimestamps();
			result.add(newSchedule);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while schedules JSON '%s'!", jSchedules);
		}
	}

	private void parseScheduleJSON(JSONObject jSchedule, long beginningOfTodayMs, long after, Schedule newSchedule, @NonNull RouteTripStop rts) {
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
			String destination = cleanTripHeadsign(parseDestinationJSON(jSchedule), rts);
			if (!TextUtils.isEmpty(destination)) {
				newTimestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, destination);
			}
			if (jSchedule.has(JSON_SCHEDULE_STATUS)) {
				String scheduleStatus = jSchedule.optString(JSON_SCHEDULE_STATUS);
				newTimestamp.setRealTime(!JSON_SCHEDULE_STATUS_PLANNED.equals(scheduleStatus));
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

	private static final String UBC = "UBC";
	private static final Pattern U_B_C = Pattern.compile("((^|\\s)(ubc|u b c)(\\s|$))", Pattern.CASE_INSENSITIVE);
	private static final String U_B_C_REPLACEMENT = "$2" + UBC + "$4";

	private static final String SFU = "SFU";
	private static final Pattern S_F_U = Pattern.compile("((^|\\s)(sfu|s f u)(\\s|$))", Pattern.CASE_INSENSITIVE);
	private static final String S_F_U_REPLACEMENT = "$2" + SFU + "$4";

	private static final String VCC = "VCC";
	private static final Pattern V_C_C = Pattern.compile("((^|\\s)(vcc|v c c)(\\s|$))", Pattern.CASE_INSENSITIVE);
	private static final String V_C_C_REPLACEMENT = "$2" + VCC + "$4";

	private static final String UNIVERSITY_SHORT = "U";
	private static final Pattern UNIVERSITY = Pattern.compile("((^|\\W)(university)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String UNIVERSITY_REPLACEMENT = "$2" + UNIVERSITY_SHORT + "$4";

	private static final String PORT_COQUITLAM_SHORT = "PoCo";
	private static final Pattern PORT_COQUITLAM = Pattern.compile("((^|\\W)(port coquitlam|poco)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String PORT_COQUITLAM_REPLACEMENT = "$2" + PORT_COQUITLAM_SHORT + "$4";

	private static final String COQUITLAM_SHORT = "Coq";
	private static final Pattern COQUITLAM = Pattern.compile("((^|\\W)(coquitlam|coq)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String COQUITLAM_REPLACEMENT = "$2" + COQUITLAM_SHORT + "$4";

	private static final String STATION_SHORT = "Sta"; // see @CleanUtils
	private static final Pattern STATION = Pattern.compile("((^|\\W)(stn|sta|station)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String STATION_REPLACEMENT = "$2" + STATION_SHORT + "$4";

	private static final String PORT_SHORT = "Pt"; // like GTFS & real-time API
	private static final Pattern PORT = Pattern.compile("((^|\\W)(port)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String PORT_REPLACEMENT = "$2" + PORT_SHORT + "$4";

	private static final String EXCHANGE_SHORT = "Exch";
	private static final Pattern EXCHANGE = Pattern.compile("((^|\\s)(exchange|exch)(\\s|$))", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = "$2" + EXCHANGE_SHORT + "$4";

	private static final Pattern ENDS_WITH_B_LINE = Pattern.compile("((^|\\s)(- )?(b-line)(\\s|$))", Pattern.CASE_INSENSITIVE);

	private static final Pattern REMOVE_DASH = Pattern.compile("(^(\\s)*-(\\s)*|(\\s)*-(\\s)*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern TO = Pattern.compile("((^|\\W)(to)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final Pattern VIA = Pattern.compile("((^|\\W)(via)(\\W|$))", Pattern.CASE_INSENSITIVE);

	@Nullable
	private String cleanTripHeadsign(@Nullable String tripHeadsign, @NonNull RouteTripStop rts) {
		try {
			if (TextUtils.isEmpty(tripHeadsign)) {
				return tripHeadsign;
			}
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
			tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
			tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
			tripHeadsign = ENDS_WITH_B_LINE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
			tripHeadsign = EXCHANGE.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
			tripHeadsign = S_F_U.matcher(tripHeadsign).replaceAll(S_F_U_REPLACEMENT);
			tripHeadsign = U_B_C.matcher(tripHeadsign).replaceAll(U_B_C_REPLACEMENT);
			tripHeadsign = V_C_C.matcher(tripHeadsign).replaceAll(V_C_C_REPLACEMENT);
			tripHeadsign = UNIVERSITY.matcher(tripHeadsign).replaceAll(UNIVERSITY_REPLACEMENT);
			tripHeadsign = PORT_COQUITLAM.matcher(tripHeadsign).replaceAll(PORT_COQUITLAM_REPLACEMENT);
			tripHeadsign = COQUITLAM.matcher(tripHeadsign).replaceAll(COQUITLAM_REPLACEMENT);
			tripHeadsign = STATION.matcher(tripHeadsign).replaceAll(STATION_REPLACEMENT);
			tripHeadsign = PORT.matcher(tripHeadsign).replaceAll(PORT_REPLACEMENT);
			tripHeadsign = CleanUtils.removePoints(tripHeadsign);
			tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
			tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
			tripHeadsign = CleanUtils.cleanLabel(tripHeadsign);
			String heading = getContext() == null ? rts.getTrip().getHeading() : rts.getTrip().getHeading(getContext());
			tripHeadsign = Pattern.compile("((^|\\W)(" + heading + "|" + rts.getRoute().getLongName() + ")(\\W|$))", Pattern.CASE_INSENSITIVE)
					.matcher(tripHeadsign).replaceAll("$2$4");
			Matcher matcherTO = TO.matcher(tripHeadsign);
			if (matcherTO.find()) {
				tripHeadsign = tripHeadsign.substring(matcherTO.end());
			}
			Matcher matcherVIA = VIA.matcher(tripHeadsign);
			if (matcherVIA.find()) {
				String tripHeadsignBeforeVIA = tripHeadsign.substring(0, matcherVIA.start());
				String tripHeadsignAfterVIA = tripHeadsign.substring(matcherVIA.end());
				if (Trip.isSameHeadsign(tripHeadsignBeforeVIA, heading) || Trip.isSameHeadsign(tripHeadsignBeforeVIA, rts.getRoute().getLongName())) {
					tripHeadsign = tripHeadsignAfterVIA;
				} else if (Trip.isSameHeadsign(tripHeadsignAfterVIA, heading) || Trip
						.isSameHeadsign(tripHeadsignAfterVIA, rts.getRoute().getLongName())) {
					tripHeadsign = tripHeadsignBeforeVIA;
				} else {
					tripHeadsign = tripHeadsignBeforeVIA;
				}
			}
			tripHeadsign = REMOVE_DASH.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
			return tripHeadsign;
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
		//noinspection ConstantConditions // TODO requireContext()
		return CaTransLinkDbHelper.getDbVersion(getContext());
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
		//noinspection ConstantConditions // TODO requireContext()
		return getURIMATCHER(getContext());
	}

	@NonNull
	@Override
	public Uri getAuthorityUri() {
		//noinspection ConstantConditions // TODO requireContext()
		return getAUTHORITY_URI(getContext());
	}

	@NonNull
	@Override
	public SQLiteOpenHelper getDBHelper() {
		//noinspection ConstantConditions // TODO requireContext()
		return getDBHelper(getContext());
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
