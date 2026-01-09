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

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Accessibility;
import org.mtransit.android.commons.data.Direction;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.provider.common.MTContentProvider;
import org.mtransit.android.commons.provider.common.MTSQLiteOpenHelper;
import org.mtransit.android.commons.provider.status.StatusProvider;
import org.mtransit.android.commons.provider.status.StatusProviderContract;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.SourceUtils;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

// DO NOT MOVE: referenced in modules AndroidManifest.xml
@SuppressLint("Registered")
public class ReginaTransitProvider extends MTContentProvider implements StatusProviderContract {

	private static final String LOG_TAG = ReginaTransitProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	public static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link ReginaTransitProvider} implementations in same app.
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
	 * Override if multiple {@link ReginaTransitProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.regina_transit_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link ReginaTransitProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static final long TRANSIT_LIVE_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long TRANSIT_LIVE_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long TRANSIT_LIVE_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long TRANSIT_LIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long TRANSIT_LIVE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

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
		RouteDirectionStop rds = scheduleStatusFilter.getRouteDirectionStop();
		POIStatus status = StatusProvider.getCachedStatusS(this, rds.getUUID());
		if (status != null) {
			status.setTargetUUID(rds.getUUID()); // target RDS UUID instead of custom provider tags
			if (status instanceof Schedule) {
				((Schedule) status).setNoPickup(rds.isNoPickup());
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

	@NonNull
	@Override
	public String getStatusDbTableName() {
		return ReginaTransitDbHelper.T_TRANSIT_LIVE_STATUS;
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
		RouteDirectionStop rds = scheduleStatusFilter.getRouteDirectionStop();
		loadRealTimeStatusFromWWW(rds);
		return getCachedStatus(statusFilter);
	}

	private static final TimeZone REGINA_TZ = TimeZone.getTimeZone("America/Regina");

	private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

	private static final ThreadSafeDateFormatter DATE_FORMATTER_UTC;

	static {
		ThreadSafeDateFormatter dateFormatter = new ThreadSafeDateFormatter("hh:mm a", Locale.ENGLISH);
		dateFormatter.setTimeZone(UTC_TZ);
		DATE_FORMATTER_UTC = dateFormatter;
	}

	private static final String REAL_TIME_URL_PART_1_BEFORE_STOP_CODE = "https://transitlive.com/ajax/livemap.php?action=stop_times&stop=";
	private static final String REAL_TIME_URL_PART_2_BEFORE_ROUTE_SHORT_NAME = "&routes=";
	private static final String REAL_TIME_URL_PART_3 = "&lim=21";

	private static String getRealTimeStatusUrlString(@NonNull RouteDirectionStop rds) {
		return REAL_TIME_URL_PART_1_BEFORE_STOP_CODE + //
				rds.getStop().getCode() + //
				REAL_TIME_URL_PART_2_BEFORE_ROUTE_SHORT_NAME + //
				rds.getRoute().getShortName() + //
				REAL_TIME_URL_PART_3;
	}

	private void loadRealTimeStatusFromWWW(@NonNull RouteDirectionStop rds) {
		try {
			String urlString = getRealTimeStatusUrlString(rds);
			MTLog.i(this, "Loading from '%s'...", urlString);
			String sourceLabel = SourceUtils.getSourceLabel(REAL_TIME_URL_PART_1_BEFORE_STOP_CODE);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadRealTimeStatusFromWWW() > jsonString: %s.", jsonString);
				Collection<POIStatus> statuses = parseAgencyJSON(jsonString, rds, sourceLabel, newLastUpdateInMs);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(rds.getUUID()));
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
			MTLog.w(LOG_TAG, se, "No Internet Connection!");
		} catch (Exception e) { // Unknown error
			MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception");
		}
	}

	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	private static final long ONE_DAY = TimeUnit.DAYS.toMillis(1L);
	private static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1L);

	private static final String JSON_PRED_TIME = "pred_time";
	private static final String JSON_LINE_NAME = "line_name";
	private static final String JSON_BUS_ID = "bus_id";
	private static final String JSON_LAST_STOP = "last_stop";

	@Nullable
	private Collection<POIStatus> parseAgencyJSON(String jsonString, RouteDirectionStop rds, @Nullable String sourceLabel, long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> result = new ArrayList<>();
			JSONArray json = jsonString == null ? null : new JSONArray(jsonString);
			if (json != null && json.length() > 0) {
				Schedule newSchedule = new Schedule(
						null,
						rds.getUUID(),
						newLastUpdateInMs,
						getStatusMaxValidityInMs(),
						newLastUpdateInMs,
						PROVIDER_PRECISION_IN_MS,
						false,
						sourceLabel,
						false
				);
				Calendar beginningOfTodayCal = Calendar.getInstance(REGINA_TZ);
				beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, 0);
				beginningOfTodayCal.set(Calendar.MINUTE, 0);
				beginningOfTodayCal.set(Calendar.SECOND, 0);
				beginningOfTodayCal.set(Calendar.MILLISECOND, 0);
				long beginningOfTodayMs = beginningOfTodayCal.getTimeInMillis();
				long after = newLastUpdateInMs - ONE_HOUR;
				for (int s = 0; s < json.length(); s++) {
					JSONObject j = json.getJSONObject(s);
					if (j == null) {
						continue;
					}
					if (j.has(JSON_PRED_TIME)) {
						final long time = parseTime(j.getString(JSON_PRED_TIME));
						long t = beginningOfTodayMs + TimeUtils.timeToTheTensSecondsMillis(time);
						if (t < after) {
							t += ONE_DAY; // TOMORROW
						}
						Schedule.Timestamp timestamp = new Schedule.Timestamp(t, REGINA_TZ);
						try {
							if (j.has(JSON_LINE_NAME)) {
								String jDestinationName = j.getString(JSON_LINE_NAME);
								if (!TextUtils.isEmpty(jDestinationName)) {
									timestamp.setHeadsign(Direction.HEADSIGN_TYPE_STRING, cleanTripHeadsign(jDestinationName));
								}
							}
						} catch (Exception e) {
							MTLog.w(this, e, "Error while adding destination name %s!", j);
						}
						if (j.has(JSON_BUS_ID)) {
							final String jBusId = j.optString(JSON_BUS_ID, StringUtils.EMPTY);
							timestamp.setRealTime(!jBusId.isEmpty()); // no bus ID = scheduled = not real-time
						}
						timestamp.setAccessible(Accessibility.UNKNOWN); // no info available on https://transitlive.com/mobile/
						if (j.has(JSON_LAST_STOP)) {
							final String lastStopS = j.optString(JSON_LAST_STOP);
							if (lastStopS.equals(rds.getStop().getCode())) {
								timestamp.setHeadsign(Direction.HEADSIGN_TYPE_NO_PICKUP, null);
							}
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

	@VisibleForTesting
	protected static long parseTime(@Nullable String predictionTimeS) throws ParseException {
		if (predictionTimeS == null || predictionTimeS.isEmpty()) {
			return -1L;
		}
		final Date date = DATE_FORMATTER_UTC.parseThreadSafe(predictionTimeS);
		if (date == null) {
			return -1L;
		}
		return date.getTime();
	}

	private static final Pattern ALBERT_NORTH = Pattern.compile("((^|\\W)(albert north)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ALBERT_NORTH_REPLACEMENT = "$2North$4";

	private static final Pattern ALBERT_SOUTH = Pattern.compile("((^|\\W)(albert south)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String ALBERT_SOUTH_REPLACEMENT = "$2South$4";

	private static final String DOWNTOWN = "Downtown";

	private static final Pattern DOWN = Pattern.compile("((^|\\W)(down)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String DOWN_REPLACEMENT = "$2" + DOWNTOWN + "$4";

	private static final Pattern INDUST = Pattern.compile("((^|\\W)(indust)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String INDUST_REPLACEMENT = "$2" + "industrial" + "$4";

	private static final Pattern LAND = Pattern.compile("((^|\\W)(land)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String LAND_REPLACEMENT = "$2Landing$4";

	private static final Pattern MED = Pattern.compile("((^|\\W)(med)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String MED_REPLACEMENT = "$2Meadows$4";

	private static final Pattern NOR_HEIGHTS = Pattern.compile("((^|\\W)(nor\\.heights)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String NOR_HEIGHTS_REPLACEMENT = "$2Normandy Heights$4";

	private static final Pattern NOR = Pattern.compile("((^|\\W)(nor)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String NOR_REPLACEMENT = "$2Normandy$4";

	private static final Pattern VIC_DOWNTOWN = Pattern.compile("((^|\\W)(vic downtown)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String VIC_DOWNTOWN_REPLACEMENT = "$2" + DOWNTOWN + "$4";

	private static final Pattern VIC_EAST = Pattern.compile("((^|\\W)(vic east)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String VIC_EAST_REPLACEMENT = "$2East$4";

	private static final Pattern WHIT = Pattern.compile("((^|\\W)(whit)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String WHIT_REPLACEMENT = "$2Whitmore$4";

	private static final Pattern WOOD = Pattern.compile("((^|\\W)(wood)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String WOOD_REPLACEMENT = "$2woodland$4";

	@NonNull
	private String cleanTripHeadsign(@NonNull String tripHeadsign) {
		try {
			tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
			tripHeadsign = ALBERT_NORTH.matcher(tripHeadsign).replaceAll(ALBERT_NORTH_REPLACEMENT);
			tripHeadsign = ALBERT_SOUTH.matcher(tripHeadsign).replaceAll(ALBERT_SOUTH_REPLACEMENT);
			tripHeadsign = DOWN.matcher(tripHeadsign).replaceAll(DOWN_REPLACEMENT);
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
			return CleanUtils.cleanLabel(Locale.ENGLISH, tripHeadsign);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadsign);
			return tripHeadsign;
		}
	}

	@MainThread
	@Override
	public boolean onCreateMT() {
		return true;
	}

	@Nullable
	private ReginaTransitDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private ReginaTransitDbHelper getDBHelper(@NonNull Context context) {
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
		return ReginaTransitDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link ReginaTransitProvider} implementations in same app.
	 */
	@NonNull
	public ReginaTransitDbHelper getNewDbHelper(@NonNull Context context) {
		return new ReginaTransitDbHelper(context.getApplicationContext());
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

	public static class ReginaTransitDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = ReginaTransitDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link ReginaTransitDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "reginatransit.db";

		static final String T_TRANSIT_LIVE_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_TRANSIT_LIVE_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_TRANSIT_LIVE_STATUS).build();

		private static final String T_TRANSIT_LIVE_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_TRANSIT_LIVE_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link ReginaTransitDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.regina_transit_db_version);
			}
			return dbVersion;
		}

		ReginaTransitDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_TRANSIT_LIVE_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_TRANSIT_LIVE_STATUS_SQL_CREATE);
		}
	}
}
