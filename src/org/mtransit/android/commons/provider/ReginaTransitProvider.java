package org.mtransit.android.commons.provider;

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
import android.text.TextUtils;

@SuppressLint("Registered")
public class ReginaTransitProvider extends MTContentProvider implements StatusProviderContract {

	private static final String TAG = ReginaTransitProvider.class.getSimpleName();

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
	 * Override if multiple {@link ReginaTransitProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link ReginaTransitProvider} implementations in same app.
	 */
	private static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.regina_transit_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link ReginaTransitProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static final long TRANSIT_LIVE_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long TRANSIT_LIVE_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long TRANSIT_LIVE_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long TRANSIT_LIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long TRANSIT_LIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

	@Override
	public long getStatusMaxValidityInMs() {
		return TRANSIT_LIVE_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return TRANSIT_LIVE_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return TRANSIT_LIVE_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return TRANSIT_LIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return TRANSIT_LIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
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
		POIStatus status = StatusProvider.getCachedStatusS(this, rts.getUUID());
		if (status != null) {
			status.setTargetUUID(rts.getUUID());
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
		return ReginaTransitDbHelper.T_TRANSIT_LIVE_STATUS;
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

	private static final TimeZone REGINA_TZ = TimeZone.getTimeZone("America/Regina");


	private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

	private static final ThreadSafeDateFormatter DATE_FORMATTER_UTC;
	static {
		ThreadSafeDateFormatter dateFormatter = new ThreadSafeDateFormatter("hh:mm a");
		dateFormatter.setTimeZone(UTC_TZ);
		DATE_FORMATTER_UTC = dateFormatter;
	}

	private static final String REAL_TIME_URL_PART_1_BEFORE_STOP_CODE = "https://transitlive.com/ajax/livemap.php?action=stop_times&stop=";
	private static final String REAL_TIME_URL_PART_2_BEFORE_ROUTE_SHORT_NAME = "&routes=";
	private static final String REAL_TIME_URL_PART_3 = "&lim=7";

	private static String getRealTimeStatusUrlString(Context context, RouteTripStop rts) {
		return new StringBuilder() //
				.append(REAL_TIME_URL_PART_1_BEFORE_STOP_CODE) //
				.append(rts.getStop().getCode()) //
				.append(REAL_TIME_URL_PART_2_BEFORE_ROUTE_SHORT_NAME) //
				.append(rts.getRoute().getShortName()) //
				.append(REAL_TIME_URL_PART_3) //
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

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10);

	private static final String JSON_PRED_TIME = "pred_time";
	private static final String JSON_LINE_NAME = "line_name";

	private Collection<POIStatus> parseAgencyJSON(String jsonString, RouteTripStop rts, long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> result = new ArrayList<POIStatus>();
			JSONArray json = jsonString == null ? null : new JSONArray(jsonString);
			if (json != null && json.length() > 0) {
				Schedule newSchedule = new Schedule(rts.getUUID(), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs, PROVIDER_PRECISION_IN_MS,
						false);
				Calendar beginningOfTodayCal = Calendar.getInstance(REGINA_TZ);
				beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, 0);
				beginningOfTodayCal.set(Calendar.MINUTE, 0);
				beginningOfTodayCal.set(Calendar.SECOND, 0);
				beginningOfTodayCal.set(Calendar.MILLISECOND, 0);
				long beginningOfTodayMs = beginningOfTodayCal.getTimeInMillis();
				long after = newLastUpdateInMs - TimeUnit.HOURS.toMillis(1);
				for (int s = 0; s < json.length(); s++) {
					JSONObject j = json.getJSONObject(s);
					if (j == null) {
						continue;
					}
					if (j.has(JSON_PRED_TIME)) {
						String predictionTimeS = j.getString(JSON_PRED_TIME);
						if (TextUtils.isEmpty(predictionTimeS)) {
							continue;
						}
						long t = beginningOfTodayMs + TimeUtils.timeToTheTensSecondsMillis(DATE_FORMATTER_UTC.parseThreadSafe(predictionTimeS).getTime());
						if (t < after) {
							t += TimeUnit.DAYS.toMillis(1); // TOMORROW
						}
						Schedule.Timestamp timestamp = new Schedule.Timestamp(t);
						try {
							if (j.has(JSON_LINE_NAME)) {
								String jDestinationName = j.getString(JSON_LINE_NAME);
								if (!TextUtils.isEmpty(jDestinationName)) {
									timestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, cleanTripHeadsign(jDestinationName));
								}
							}
						} catch (Exception e) {
							MTLog.w(this, e, "Error while adding destination name %s!", j);
						}
						newSchedule.addTimestampWithoutSort(timestamp);
					}
				}
				newSchedule.sortTimestamps();
				result.add(newSchedule);
			}
			return result;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	private static final Pattern ALBERT_NORTH = Pattern.compile("((^|\\W){1}(albert north)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String ALBERT_NORTH_REPLACEMENT = "$2North$4";

	private static final Pattern ALBERT_SOUTH = Pattern.compile("((^|\\W){1}(albert south)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String ALBERT_SOUTH_REPLACEMENT = "$2South$4";

	private static final String DOWNTOWN = "Downtown";

	private static final Pattern DOWN = Pattern.compile("((^|\\W){1}(down)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String DOWN_REPLACEMENT = "$2" + DOWNTOWN + "$4";

	private static final String INDUSTRIAL_SHORT = "Ind";

	private static final Pattern INDUST = Pattern.compile("((^|\\W){1}(indust)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String INDUST_REPLACEMENT = "$2" + INDUSTRIAL_SHORT + "$4";

	private static final Pattern INDUSTRIAL = Pattern.compile("((^|\\W){1}(industrial)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String INDUSTRIAL_REPLACEMENT = "$2" + INDUSTRIAL_SHORT + "$4";

	private static final Pattern LAND = Pattern.compile("((^|\\W){1}(land)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String LAND_REPLACEMENT = "$2Landing$4";

	private static final Pattern MED = Pattern.compile("((^|\\W){1}(med)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String MED_REPLACEMENT = "$2Meadows$4";

	private static final Pattern NOR_HEIGHTS = Pattern.compile("((^|\\W){1}(nor\\.heights)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String NOR_HEIGHTS_REPLACEMENT = "$2Normandy Heights$4";

	private static final Pattern NOR = Pattern.compile("((^|\\W){1}(nor)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String NOR_REPLACEMENT = "$2Normandy$4";

	private static final Pattern VIC_DOWNTOWN = Pattern.compile("((^|\\W){1}(vic downtown)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String VIC_DOWNTOWN_REPLACEMENT = "$2" + DOWNTOWN + "$4";

	private static final Pattern VIC_EAST = Pattern.compile("((^|\\W){1}(vic east)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String VIC_EAST_REPLACEMENT = "$2East$4";

	private static final Pattern WHIT = Pattern.compile("((^|\\W){1}(whit)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String WHIT_REPLACEMENT = "$2Whitmore$4";

	private static final Pattern WOOD = Pattern.compile("((^|\\W){1}(wood)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String WOOD_REPLACEMENT = "$2woodland$4";

	private String cleanTripHeadsign(String tripHeadsign) {
		try {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
			tripHeadsign = ALBERT_NORTH.matcher(tripHeadsign).replaceAll(ALBERT_NORTH_REPLACEMENT);
			tripHeadsign = ALBERT_SOUTH.matcher(tripHeadsign).replaceAll(ALBERT_SOUTH_REPLACEMENT);
			tripHeadsign = DOWN.matcher(tripHeadsign).replaceAll(DOWN_REPLACEMENT);
			tripHeadsign = INDUSTRIAL.matcher(tripHeadsign).replaceAll(INDUSTRIAL_REPLACEMENT);
			tripHeadsign = INDUST.matcher(tripHeadsign).replaceAll(INDUST_REPLACEMENT);
			tripHeadsign = LAND.matcher(tripHeadsign).replaceAll(LAND_REPLACEMENT);
			tripHeadsign = MED.matcher(tripHeadsign).replaceAll(MED_REPLACEMENT);
			tripHeadsign = NOR_HEIGHTS.matcher(tripHeadsign).replaceAll(NOR_HEIGHTS_REPLACEMENT);
			tripHeadsign = NOR.matcher(tripHeadsign).replaceAll(NOR_REPLACEMENT);
			tripHeadsign = VIC_DOWNTOWN.matcher(tripHeadsign).replaceAll(VIC_DOWNTOWN_REPLACEMENT);
			tripHeadsign = VIC_EAST.matcher(tripHeadsign).replaceAll(VIC_EAST_REPLACEMENT);
			tripHeadsign = WHIT.matcher(tripHeadsign).replaceAll(WHIT_REPLACEMENT);
			tripHeadsign = WOOD.matcher(tripHeadsign).replaceAll(WOOD_REPLACEMENT);
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
		// DO NOTHING
	}

	private static ReginaTransitDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private ReginaTransitDbHelper getDBHelper(Context context) {
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
	 * Override if multiple {@link ReginaTransitProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return ReginaTransitDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link ReginaTransitProvider} implementations in same app.
	 */
	public ReginaTransitDbHelper getNewDbHelper(Context context) {
		return new ReginaTransitDbHelper(context.getApplicationContext());
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

	public static class ReginaTransitDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = ReginaTransitDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link ReginaTransitDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "reginatransit.db";

		public static final String T_TRANSIT_LIVE_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_TRANSIT_LIVE_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_TRANSIT_LIVE_STATUS).build();

		private static final String T_TRANSIT_LIVE_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_TRANSIT_LIVE_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link ReginaTransitDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.regina_transit_db_version);
			}
			return dbVersion;
		}

		public ReginaTransitDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_TRANSIT_LIVE_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_TRANSIT_LIVE_STATUS_SQL_CREATE);
		}
	}
}
