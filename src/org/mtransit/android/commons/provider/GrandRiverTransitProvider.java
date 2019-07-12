package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;

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
import androidx.annotation.NonNull;
import android.text.TextUtils;

@SuppressLint("Registered")
public class GrandRiverTransitProvider extends MTContentProvider implements StatusProviderContract {

	private static final String LOG_TAG = GrandRiverTransitProvider.class.getSimpleName();

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
	 * Override if multiple {@link GrandRiverTransitProvider} implementations in same app.
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
	 * Override if multiple {@link GrandRiverTransitProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.grand_river_transit_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link GrandRiverTransitProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static final long REAL_TIME_MAP_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long REAL_TIME_MAP_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long REAL_TIME_MAP_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long REAL_TIME_MAP_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long REAL_TIME_MAP_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getStatusMaxValidityInMs() {
		return REAL_TIME_MAP_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return REAL_TIME_MAP_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return REAL_TIME_MAP_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return REAL_TIME_MAP_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return REAL_TIME_MAP_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Override
	public POIStatus getCachedStatus(StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule without schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		POIStatus status = StatusProvider.getCachedStatusS(this, rts.getUUID());
		if (status != null) {
			if (status instanceof Schedule) {
				((Schedule) status).setDescentOnly(rts.isDescentOnly());
			}
		}
		return status;
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
		return GrandRiverTransitDbHelper.T_REAL_TIME_MAP_STATUS;
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
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	private static final String REAL_TIME_URL_PART_1_BEFORE_STOP_ID = "https://realtimemap.grt.ca/Stop/GetStopInfo?stopId=";
	private static final String REAL_TIME_URL_PART_2_BEFORE_ROUTE_ID = "&routeId=";

	private static String getRealTimeStatusUrlString(@NonNull RouteTripStop rts) {
		return new StringBuilder() //
				.append(REAL_TIME_URL_PART_1_BEFORE_STOP_ID) //
				.append(rts.getStop().getCode()) //
				.append(REAL_TIME_URL_PART_2_BEFORE_ROUTE_ID) //
				.append(rts.getRoute().getShortName()) //
				.toString();
	}

	private void loadRealTimeStatusFromWWW(@NonNull RouteTripStop rts) {
		try {
			String urlString = getRealTimeStatusUrlString(rts);
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				Collection<POIStatus> statuses = parseAgencyJSON(getContext(),
						parseAgencyJSON(jsonString),
						rts, newLastUpdateInMs);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(rts.getUUID()));
				for (POIStatus status : statuses) {
					StatusProvider.cacheStatusS(this, status);
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

	private static final String JSON_STOP_TIMES = "stopTimes";
	private static final String JSON_ARRIVAL_DATE_TIME = "ArrivalDateTime";
	private static final String JSON_HEAD_SIGN = "HeadSign";

	@NonNull
	private List<JStopTime> parseAgencyJSON(@Nullable String jsonString) {
		List<JStopTime> stopTimes = new ArrayList<>();
		try {
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_STOP_TIMES)) {
				JSONArray jStopTimes = json.getJSONArray(JSON_STOP_TIMES);
				for (int l = 0; l < jStopTimes.length(); l++) {
					JSONObject jStopTime = jStopTimes.getJSONObject(l);
					stopTimes.add(new JStopTime(
							jStopTime.optString(JSON_HEAD_SIGN),
							jStopTime.optString(JSON_ARRIVAL_DATE_TIME)
					));
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
		}
		return stopTimes;
	}

	private static final Pattern DIGITS = Pattern.compile("[\\d]+");

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	@NonNull
	protected Collection<POIStatus> parseAgencyJSON(@Nullable Context context, @Nullable List<JStopTime> stopTimes, @NonNull RouteTripStop rts, long newLastUpdateInMs) {
		ArrayList<POIStatus> result = new ArrayList<>();
		try {
			if (stopTimes != null && stopTimes.size() > 0) {
				Schedule newSchedule =
						new Schedule(rts.getUUID(), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs, PROVIDER_PRECISION_IN_MS, false);
				for (JStopTime stopTime : stopTimes) {
					if (stopTime == null || TextUtils.isEmpty(stopTime.arrivalDateTime)) {
						continue;
					}
					Matcher matcher = DIGITS.matcher(stopTime.arrivalDateTime);
					if (!matcher.find()) {
						MTLog.w(this, "parseAgencyJSONArrivalsResults() > Unexpected stop date time: %s", stopTime);
						continue;
					}
					long arrivalDateTimeTs = Long.parseLong(matcher.group());
					long t = TimeUtils.timeToTheTensSecondsMillis(arrivalDateTimeTs);
					Schedule.Timestamp newTimestamp = new Schedule.Timestamp(t);
					if (rts.isDescentOnly()) {
						if (!TextUtils.isEmpty(stopTime.headSign)) {
							String headsignValue = cleanTripHeadsignOriginal(stopTime.headSign);
							if (rts.getTrip().getHeadsignValue().equals(headsignValue)) { // schedule for this descent-only stop
							} else { // schedule for same stop on the other direction (probably not descent only)
								continue;
							}
						}
					} else {
						if (!TextUtils.isEmpty(stopTime.headSign)) {
							if (stopTime.headSign.toLowerCase(Locale.ENGLISH) //
									.contains(rts.getStop().getName().toLowerCase(Locale.ENGLISH))) {
								continue;
							}
							String headsignValue = cleanTripHeadsign(context, stopTime.headSign, rts);
							newTimestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, headsignValue);
						}
					}
					newSchedule.addTimestampWithoutSort(newTimestamp);
				}
				newSchedule.sortTimestamps();
				result.add(newSchedule);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing '%s'!", stopTimes);
		}
		return result;
	}
	private static final Pattern BUS_PLUS = Pattern.compile("( bus plus$)", Pattern.CASE_INSENSITIVE);
	private static final String BUS_PLUS_REPLACEMENT = " BusPlus";

	private static final Pattern STARTS_WITH_RSN = Pattern.compile("(^[\\d]+[\\s]*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_IXPRESS = Pattern.compile("(^IXpress )", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENDS_WITH_EXPRESS = Pattern.compile("( express$)", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENDS_WITH_BUSPLUS = Pattern.compile("( busplus$)", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENDS_WITH_SPECIAL = Pattern.compile("( special$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern TO = Pattern.compile("((^|\\W){1}(to)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final Pattern VIA = Pattern.compile("((^|\\W){1}(via)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final String INDUSTRIAL_SHORT = "Ind";
	private static final Pattern INDUSTRIAL = Pattern.compile("(industrial)", Pattern.CASE_INSENSITIVE);
	private static final String INDUSTRIAL_REPLACEMENT = INDUSTRIAL_SHORT;

	private String cleanTripHeadsign(@Nullable Context context, String tripHeadsign, @NonNull RouteTripStop rts) {
		try {
			String heading = context == null ? rts.getTrip().getHeading() : rts.getTrip().getHeading(context);
			tripHeadsign = Pattern.compile("((^|\\W){1}(" + rts.getRoute().getLongName() + "|" + heading + ")(\\W|$){1})", Pattern.CASE_INSENSITIVE)
					.matcher(tripHeadsign).replaceAll(StringUtils.SPACE_STRING);
			tripHeadsign = cleanTripHeadsignCommon(tripHeadsign);
			Matcher matcherTO = TO.matcher(tripHeadsign);
			if (matcherTO.find()) {
				tripHeadsign = tripHeadsign.substring(matcherTO.end());
			}
			Matcher matcherVIA = VIA.matcher(tripHeadsign);
			if (matcherVIA.find()) {
				String tripHeadsignBeforeVIA = tripHeadsign.substring(0, matcherVIA.start());
				String tripHeadsignAfterVIA = tripHeadsign.substring(matcherVIA.end());
				if (Trip.isSameHeadsign(tripHeadsignBeforeVIA, heading) //
						|| Trip.isSameHeadsign(tripHeadsignBeforeVIA, rts.getRoute().getLongName())) {
					tripHeadsign = tripHeadsignAfterVIA;
				} else if (Trip.isSameHeadsign(tripHeadsignAfterVIA, heading) //
						|| Trip.isSameHeadsign(tripHeadsignAfterVIA, rts.getRoute().getLongName())) {
					tripHeadsign = tripHeadsignBeforeVIA;
				} else {
					tripHeadsign = tripHeadsignBeforeVIA;
				}
			}
			return tripHeadsign;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadsign);
			return tripHeadsign;
		}
	}

	@NonNull
	private String cleanTripHeadsignOriginal(String tripHeadsign) {
		Matcher matcherTO = TO.matcher(tripHeadsign);
		if (matcherTO.find()) {
			tripHeadsign = tripHeadsign.substring(matcherTO.end());
		}
		Matcher matcherVIA = VIA.matcher(tripHeadsign);
		if (matcherVIA.find()) {
			tripHeadsign = tripHeadsign.substring(0, matcherVIA.start());
		}
		tripHeadsign = cleanTripHeadsignCommon(tripHeadsign);
		return tripHeadsign;
	}

	@NonNull
	private String cleanTripHeadsignCommon(String tripHeadsign) {
		tripHeadsign = STARTS_WITH_RSN.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STARTS_WITH_IXPRESS.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = BUS_PLUS.matcher(tripHeadsign).replaceAll(BUS_PLUS_REPLACEMENT);
		tripHeadsign = ENDS_WITH_EXPRESS.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = ENDS_WITH_BUSPLUS.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = ENDS_WITH_SPECIAL.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = INDUSTRIAL.matcher(tripHeadsign).replaceAll(INDUSTRIAL_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.removePoints(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanLabel(tripHeadsign);
		return tripHeadsign;
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
	private GrandRiverTransitDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private GrandRiverTransitDbHelper getDBHelper(Context context) {
		if (dbHelper == null) { // initialize
			MTLog.d(this, "Initialize DB...");
			dbHelper = getNewDbHelper(context);
			currentDbVersion = getCurrentDbVersion();
		} else { // reset
			try {
				if (currentDbVersion != getCurrentDbVersion()) {
					MTLog.d(this, "Update DB...");
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
	 * Override if multiple {@link GrandRiverTransitProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return GrandRiverTransitDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link GrandRiverTransitProvider} implementations in same app.
	 */
	public GrandRiverTransitDbHelper getNewDbHelper(Context context) {
		return new GrandRiverTransitDbHelper(context.getApplicationContext());
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

	public static class JStopTime {
		@Nullable
		String headSign;
		@Nullable
		String arrivalDateTime;

		public JStopTime(@Nullable String headSign, @Nullable String arrivalDateTime) {
			this.headSign = headSign;
			this.arrivalDateTime = arrivalDateTime;
		}

		@NonNull
		@Override
		public String toString() {
			return JStopTime.class.getSimpleName() + "{" +
					"headSign:" + headSign + ", " + //
					"arrivalDateTime:" + arrivalDateTime + ", " + //
					"}";
		}
	}

	public static class GrandRiverTransitDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = GrandRiverTransitDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link GrandRiverTransitDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "grandrivertransit.db";

		public static final String T_REAL_TIME_MAP_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_REAL_TIME_MAP_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_REAL_TIME_MAP_STATUS).build();

		private static final String T_REAL_TIME_MAP_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_REAL_TIME_MAP_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link GrandRiverTransitDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.grand_river_transit_db_version);
			}
			return dbVersion;
		}

		public GrandRiverTransitDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_REAL_TIME_MAP_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_REAL_TIME_MAP_STATUS_SQL_CREATE);
		}
	}
}
