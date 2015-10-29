package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.CleanUtils;
import org.mtransit.android.commons.FileUtils;
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

@SuppressLint("Registered")
public class CaLTCOnlineProvider extends MTContentProvider implements StatusProviderContract {

	private static final String TAG = CaLTCOnlineProvider.class.getSimpleName();

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
	 * Override if multiple {@link CaLTCOnlineProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link CaLTCOnlineProvider} implementations in same app.
	 */
	private static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.ca_ltconline_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link CaLTCOnlineProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static final long WEB_WATCH_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long WEB_WATCH_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long WEB_WATCH_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long WEB_WATCH_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long WEB_WATCH_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

	@Override
	public long getStatusMaxValidityInMs() {
		return WEB_WATCH_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return WEB_WATCH_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return WEB_WATCH_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return WEB_WATCH_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return WEB_WATCH_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
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
		String agencyTargetUUID = getAgencyTargetUUID(rts);
		POIStatus status = StatusProvider.getCachedStatusS(this, agencyTargetUUID);
		if (status != null) {
			status.setTargetUUID(rts.getUUID());
			if (status instanceof Schedule) {
				((Schedule) status).setDescentOnly(rts.isDescentOnly());
			}
		}
		return status;
	}

	private String getAgencyTargetUUID(RouteTripStop rts) {
		return POI.POIUtils.getUUID(rts.getAuthority(), rts.getRoute().getShortName(), getAgencyDirectionId(rts), rts.getStop().getCode());
	}

	private static int getAgencyDirectionId(RouteTripStop rts) {
		if (rts.getTrip().getHeadsignType() == Trip.HEADSIGN_TYPE_STRING) {
			return (int) (rts.getTrip().getId() % 10);
		}
		MTLog.w(TAG, "Unexpected trip direction for '%s'!", rts);
		return 0;
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
		return CaLTCOnlineDbHelper.T_WEB_WATCH_STATUS;
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

	private static final TimeZone LONDON_TZ = TimeZone.getTimeZone("America/Toronto");

	private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

	private static final ThreadSafeDateFormatter DATE_FORMATTER_UTC;
	static {
		ThreadSafeDateFormatter dateFormatter = new ThreadSafeDateFormatter("hh:mm a");
		dateFormatter.setTimeZone(UTC_TZ);
		DATE_FORMATTER_UTC = dateFormatter;
	}

	private static final String REAL_TIME_URL_PART_1_BEFORE_ROUTE_SHORT_NAME = "http://www.ltconline.ca/WebWatch/MobileAda.aspx?r=";
	private static final String REAL_TIME_URL_PART_2_BEFORE_DIRECTION_ID = "&d=";
	private static final String REAL_TIME_URL_PART_3_BEFORE_STOP_CODE = "&s=";

	private static String getRealTimeStatusUrlString(Context context, RouteTripStop rts) {
		return new StringBuilder() //
				.append(REAL_TIME_URL_PART_1_BEFORE_ROUTE_SHORT_NAME) //
				.append(rts.getRoute().getShortName()) //
				.append(REAL_TIME_URL_PART_2_BEFORE_DIRECTION_ID) //
				.append(getAgencyDirectionId(rts)) //
				.append(REAL_TIME_URL_PART_3_BEFORE_STOP_CODE) //
				.append(rts.getStop().getCode()) //
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
				String htmlString = FileUtils.getString(urlc.getInputStream());
				Collection<POIStatus> statuses = parseAgencyHTML(htmlString, rts, newLastUpdateInMs);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(new String[] { getAgencyTargetUUID(rts) }));
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

	private static final Pattern NO_SCHEDULE = Pattern.compile("(no further buses scheduled for this stop)", Pattern.CASE_INSENSITIVE);

	private static final Pattern REMOVE_BEFORE = Pattern.compile("(next [\\d]+ vehicles arrive at:)", Pattern.CASE_INSENSITIVE);
	private static final Pattern REMOVE_AFTER = Pattern.compile("(last updated [\\d]+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern SPLIT_RESULTS = Pattern.compile("([\\s]*<br[/]?>[\\s]*)", Pattern.CASE_INSENSITIVE);
	private static final Pattern SPLIT_PARTS = Pattern.compile("([\\s]+to[\\s]+)", Pattern.CASE_INSENSITIVE);

	private static final Pattern CLEAN_AM = Pattern.compile("(a\\.m\\.)", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_AM_REPLACEMENT = "am";

	private static final Pattern CLEAN_PM = Pattern.compile("(p\\.m\\.)", Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_PM_REPLACEMENT = "pm";

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10);

	private Collection<POIStatus> parseAgencyHTML(String htmlString, RouteTripStop rts, long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> result = new ArrayList<POIStatus>();
			if (NO_SCHEDULE.matcher(htmlString).find()) {
				return null;
			}
			Matcher matcher = REMOVE_BEFORE.matcher(htmlString);
			if (!matcher.find()) {
				MTLog.w(this, "parseAgencyHTML() > impossible to remove HTML before '%s'!", htmlString);
				return null;
			}
			htmlString = htmlString.substring(matcher.end());
			matcher = REMOVE_AFTER.matcher(htmlString);
			if (!matcher.find()) {
				MTLog.w(this, "parseAgencyHTML() > impossible to remove HTML after '%s'!", htmlString);
				return null;
			}
			htmlString = htmlString.substring(0, matcher.start());
			String[] lines = SPLIT_RESULTS.split(htmlString);
			Schedule newSchedule = new Schedule(getAgencyTargetUUID(rts), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
					PROVIDER_PRECISION_IN_MS, false);
			Calendar beginningOfTodayCal = Calendar.getInstance(LONDON_TZ);
			beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, 0);
			beginningOfTodayCal.set(Calendar.MINUTE, 0);
			beginningOfTodayCal.set(Calendar.SECOND, 0);
			beginningOfTodayCal.set(Calendar.MILLISECOND, 0);
			long beginningOfTodayMs = beginningOfTodayCal.getTimeInMillis();
			long after = newLastUpdateInMs - TimeUnit.HOURS.toMillis(1);
			HashSet<String> scheduleTimestamps = new HashSet<String>();
			for (String line : lines) {
				if (TextUtils.isEmpty(line)) {
					continue;
				}
				String[] parts = SPLIT_PARTS.split(line);
				if (parts.length < 2) {
					MTLog.w(this, "parseAgencyHTML() > unexpected parts '%s' int line '%s'!", parts, line);
					return null;
				}
				String timeString = parts[0];
				timeString = CLEAN_AM.matcher(timeString).replaceAll(CLEAN_AM_REPLACEMENT);
				timeString = CLEAN_PM.matcher(timeString).replaceAll(CLEAN_PM_REPLACEMENT);
				long t = beginningOfTodayMs + TimeUtils.timeToTheTensSecondsMillis(DATE_FORMATTER_UTC.parseThreadSafe(timeString).getTime());
				if (t < after) {
					t += TimeUnit.DAYS.toMillis(1); // TOMORROW
				}
				Schedule.Timestamp timestamp = new Schedule.Timestamp(t);
				try {
					String headsign = parts[1];
					if (!TextUtils.isEmpty(headsign)) {
						timestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, cleanTripHeadsign(headsign, rts));
					}
				} catch (Exception e) {
					MTLog.w(this, e, "Error while adding destination name %s!", line);
				}
				if (scheduleTimestamps.contains(timestamp.toString())) {
					continue;
				}
				newSchedule.addTimestampWithoutSort(timestamp);
			}
			newSchedule.sortTimestamps();
			result.add(newSchedule);
			return result;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing HTML '%s'!", htmlString);
			return null;
		}
	}

	private static final Pattern AREA = Pattern.compile("((^|\\W){1}(area)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final String INDUSTRIAL_SHORT = "Ind";
	private static final Pattern INDUSTRIAL = Pattern.compile("((^|\\W){1}(industrial)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String INDUSTRIAL_REPLACEMENT = "$2" + INDUSTRIAL_SHORT + "$4";

	private static final Pattern ONLY = Pattern.compile("((^|\\W){1}(only)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	private static final String UWO = "UWO";
	private static final Pattern UNIVERSITY_OF_WESTERN_ONTARIO = Pattern.compile("((^|\\W){1}(univ western ontario|western university)(\\W|$){1})",
			Pattern.CASE_INSENSITIVE);
	private static final String UNIVERSITY_OF_WESTERN_ONTARIO_REPLACEMENT = "$2" + UWO + "$4";

	private String cleanTripHeadsign(String tripHeadsign, RouteTripStop optRTS) {
		try {
			tripHeadsign = AREA.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
			tripHeadsign = INDUSTRIAL.matcher(tripHeadsign).replaceAll(INDUSTRIAL_REPLACEMENT);
			tripHeadsign = ONLY.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
			tripHeadsign = UNIVERSITY_OF_WESTERN_ONTARIO.matcher(tripHeadsign).replaceAll(UNIVERSITY_OF_WESTERN_ONTARIO_REPLACEMENT);
			tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
			tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
			tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
			tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
			tripHeadsign = CleanUtils.removePoints(tripHeadsign);
			if (optRTS != null) {
				tripHeadsign = Pattern.compile("(^[\\s]*" + optRTS.getRoute().getShortName() + ")", Pattern.CASE_INSENSITIVE).matcher(tripHeadsign)
						.replaceAll(StringUtils.EMPTY);
				tripHeadsign = Pattern
						.compile("((^|\\W){1}(" + optRTS.getTrip().getHeading(getContext()) + "|" + optRTS.getRoute().getLongName() + ")(\\W|$){1})",
								Pattern.CASE_INSENSITIVE).matcher(tripHeadsign).replaceAll(" ");
			}
			tripHeadsign = CleanUtils.cleanLabel(tripHeadsign);
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
		PackageManagerUtils.removeModuleLauncherIcon(getContext());
	}

	private static CaLTCOnlineDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private CaLTCOnlineDbHelper getDBHelper(Context context) {
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
	 * Override if multiple {@link CaLTCOnlineProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return CaLTCOnlineDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link CaLTCOnlineProvider} implementations in same app.
	 */
	public CaLTCOnlineDbHelper getNewDbHelper(Context context) {
		return new CaLTCOnlineDbHelper(context.getApplicationContext());
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

	public static class CaLTCOnlineDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = CaLTCOnlineDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link CaLTCOnlineDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "caltconline.db";

		public static final String T_WEB_WATCH_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_WEB_WATCH_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_WEB_WATCH_STATUS).build();

		private static final String T_WEB_WATCH_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_WEB_WATCH_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link CaLTCOnlineDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.ca_ltconline_db_version);
			}
			return dbVersion;
		}

		public CaLTCOnlineDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_WEB_WATCH_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_WEB_WATCH_STATUS_SQL_CREATE);
		}
	}

}
