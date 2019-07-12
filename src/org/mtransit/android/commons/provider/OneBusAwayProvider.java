package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

@SuppressLint("Registered")
public class OneBusAwayProvider extends MTContentProvider implements StatusProviderContract {

	private static final String LOG_TAG = OneBusAwayProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
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
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.one_bus_away_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String predictionUrl = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	@NonNull
	private static String getPREDICTION_URL(@NonNull Context context) {
		if (predictionUrl == null) {
			predictionUrl = context.getResources().getString(R.string.one_bus_away_prediction_url_and_stop_tag_and_api_key);
		}
		return predictionUrl;
	}

	@Nullable
	private static Boolean agencyStopTagIsStopCode = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	private static boolean isAGENCY_STOP_TAG_IS_STOP_CODE(@NonNull Context context) {
		if (agencyStopTagIsStopCode == null) {
			agencyStopTagIsStopCode = context.getResources().getBoolean(R.bool.one_bus_away_stop_tag_is_stop_code);
		}
		return agencyStopTagIsStopCode;
	}

	@Nullable
	private static String apiKey = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	@NonNull
	private static String getAPI_KEY(@NonNull Context context) {
		if (apiKey == null) {
			apiKey = context.getResources().getString(R.string.one_bus_away_api_key);
		}
		return apiKey;
	}

	@Nullable
	private static java.util.List<String> scheduleHeadsignCleanRegex = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCHEDULE_HEADSIGN_CLEAN_REGEX(@NonNull Context context) {
		if (scheduleHeadsignCleanRegex == null) {
			scheduleHeadsignCleanRegex = Arrays.asList(context.getResources().getStringArray(R.array.one_bus_away_schedule_head_sign_clean_regex));
		}
		return scheduleHeadsignCleanRegex;
	}

	@Nullable
	private static java.util.List<String> scheduleHeadsignCleanReplacement = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCHEDULE_HEADSIGN_CLEAN_REPLACEMENT(@NonNull Context context) {
		if (scheduleHeadsignCleanReplacement == null) {
			scheduleHeadsignCleanReplacement = Arrays.asList(context.getResources().getStringArray(R.array.one_bus_away_schedule_head_sign_clean_replacement));
		}
		return scheduleHeadsignCleanReplacement;
	}

	private static final long ONE_BUS_WAY_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long ONE_BUS_WAY_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long ONE_BUS_WAY_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long ONE_BUS_WAY_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(5L);
	private static final long ONE_BUS_WAY_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return ONE_BUS_WAY_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return ONE_BUS_WAY_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return ONE_BUS_WAY_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return ONE_BUS_WAY_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusMaxValidityInMs() {
		return ONE_BUS_WAY_STATUS_MAX_VALIDITY_IN_MS;
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
		String targetUUID = getAgencyRouteStopTagTargetUUID(rts);
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, targetUUID);
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom OneBusAway Route & Stop tags
			if (cachedStatus instanceof Schedule) {
				((Schedule) cachedStatus).setDescentOnly(rts.isDescentOnly());
			}
		}
		return cachedStatus;
	}

	private String getAgencyRouteStopTagTargetUUID(@NonNull RouteTripStop rts) {
		return rts.getUUID();
	}

	private String getStopTag(@NonNull RouteTripStop rts) {
		if (isAGENCY_STOP_TAG_IS_STOP_CODE(getContext())) {
			return rts.getStop().getCode();
		}
		return String.valueOf(rts.getStop().getId());
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
		return OneBusAwayDbHelper.T_ONE_BUS_AWAY_STATUS;
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
		loadPredictionsFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	private String getStopPredictionsUrlString(@NonNull Context context, @NonNull RouteTripStop rts) {
		return String.format(getPREDICTION_URL(context), getStopTag(rts), getAPI_KEY(context));
	}

	private void loadPredictionsFromWWW(@NonNull RouteTripStop rts) {
		try {
			String urlString = getStopPredictionsUrlString(getContext(), rts);
			MTLog.i(this, "Loading from '%s' for stop '%s'...", getPREDICTION_URL(getContext()), getStopTag(rts));
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				Collection<POIStatus> statuses = parseAgencyJSON(jsonString, rts, newLastUpdateInMs);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(getAgencyRouteStopTagTargetUUID(rts)));
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

	private static final String JSON_DATA = "data";
	private static final String JSON_ENTRY = "entry";
	private static final String JSON_ARRIVALS_AND_DEPARTURES = "arrivalsAndDepartures";
	private static final String JSON_PREDICTED = "predicted";
	private static final String JSON_PREDICTED_DEPARTURE_TIME = "predictedDepartureTime";
	private static final String JSON_PREDICTED_ARRIVAL_TIME = "predictedArrivalTime";
	private static final String JSON_SCHEDULED_DEPARTURE_TIME = "scheduledDepartureTime";
	private static final String JSON_SCHEDULED_ARRIVAL_TIME = "scheduledArrivalTime";
	private static final String JSON_ROUTE_ID = "routeId";
	private static final String JSON_ROUTE_LONG_NAME = "routeLongName";
	private static final String JSON_ROUTE_SHORT_NAME = "routeShortName";
	private static final String JSON_TRIP_HEADSIGN = "tripHeadsign";
	private static final String JSON_DEPARTURE_ENABLED = "departureEnabled";

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	private Collection<POIStatus> parseAgencyJSON(@Nullable String jsonString, @NonNull RouteTripStop rts, long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> result = new ArrayList<POIStatus>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_DATA)) {
				JSONObject jData = json.optJSONObject(JSON_DATA);
				if (jData != null && jData.has(JSON_ENTRY)) {
					JSONObject jEntry = jData.getJSONObject(JSON_ENTRY);
					if (jEntry != null && jEntry.has(JSON_ARRIVALS_AND_DEPARTURES)) {
						JSONArray jArrivalsAndDepartures = jEntry.getJSONArray(JSON_ARRIVALS_AND_DEPARTURES);
						Schedule newSchedule =
								new Schedule(getAgencyRouteStopTagTargetUUID(rts), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
										PROVIDER_PRECISION_IN_MS, false);
						for (int l = 0; l < jArrivalsAndDepartures.length(); l++) {
							JSONObject jArrivalsAndDeparture = jArrivalsAndDepartures.getJSONObject(l);
							String jRoutId = jArrivalsAndDeparture.getString(JSON_ROUTE_ID);
							String jRouteLongName = jArrivalsAndDeparture.getString(JSON_ROUTE_LONG_NAME);
							String jRouteShortName = jArrivalsAndDeparture.getString(JSON_ROUTE_SHORT_NAME);
							boolean sameRoute = isSameRoute(rts, jRoutId, jRouteLongName, jRouteShortName);
							if (!sameRoute) {
								continue;
							}
							long timestamp = getTimestamp(jArrivalsAndDeparture);
							if (timestamp <= 0L) {
								continue;
							}
							boolean isRealTime = jArrivalsAndDeparture.optBoolean(JSON_PREDICTED, false);
							Schedule.Timestamp newTimestamp = new Schedule.Timestamp(TimeUtils.timeToTheTensSecondsMillis(timestamp));
							try {
								String tripHeadsign = jArrivalsAndDeparture.getString(JSON_TRIP_HEADSIGN);
								if (!TextUtils.isEmpty(tripHeadsign)) {
									boolean sameTrip = isSameTrip(rts, tripHeadsign);
									if (!sameTrip) {
										continue;
									}
									tripHeadsign = cleanTripHeadsign(tripHeadsign);
									tripHeadsign = cleanTripHeadsign(tripHeadsign, rts); // remove rts trip / route from head sign
									newTimestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, tripHeadsign);
								}
							} catch (Exception e) {
								MTLog.w(this, e, "Error while reading trip headsign in '%s'!", jArrivalsAndDeparture);
							}
							newSchedule.addTimestampWithoutSort(newTimestamp);

						}
						if (newSchedule.getTimestampsCount() > 0) {
							newSchedule.sortTimestamps();
							result.add(newSchedule);
						}
					}
				}
			}
			return result;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	private String cleanTripHeadsign(String tripHeadsign) {
		try {
			for (int c = 0; c < getSCHEDULE_HEADSIGN_CLEAN_REGEX(getContext()).size(); c++) {
				try {
					tripHeadsign = Pattern.compile(getSCHEDULE_HEADSIGN_CLEAN_REGEX(getContext()).get(c), Pattern.CASE_INSENSITIVE).matcher(tripHeadsign)
							.replaceAll(getSCHEDULE_HEADSIGN_CLEAN_REPLACEMENT(getContext()).get(c));
				} catch (Exception e) {
					MTLog.w(this, e, "Error while cleaning trip head sign %s for %s cleaning configuration!", tripHeadsign, c);
				}
			}
			tripHeadsign = CleanUtils.cleanSlashes(tripHeadsign);
			tripHeadsign = CleanUtils.removePoints(tripHeadsign);
			tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
			tripHeadsign = CleanUtils.cleanLabel(tripHeadsign);
			return tripHeadsign;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadsign);
			return tripHeadsign;
		}
	}

	protected String cleanTripHeadsign(String tripHeadsign, @NonNull RouteTripStop rts) {
		try {
			String cleanRTSTripHeading = getContext() == null ? rts.getTrip().getHeading() : rts.getTrip().getHeading(getContext());
			String cleanedRTSRouteLongName = rts.getRoute().getLongName();
			tripHeadsign = Pattern.compile("((^|\\W){1}(" + cleanRTSTripHeading + "|" + cleanedRTSRouteLongName + ")(\\W|$){1})", Pattern.CASE_INSENSITIVE)
					.matcher(tripHeadsign).replaceAll("$2$4");
			tripHeadsign = CleanUtils.cleanLabel(tripHeadsign);
			return tripHeadsign;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadsign);
			return tripHeadsign;
		}
	}

	protected boolean isSameRoute(@NonNull RouteTripStop rts, String jRouteId, String jRouteLongName, String jRouteShortName) { // YRT Viva ONLY
		boolean same = false;
		if (!TextUtils.isEmpty(jRouteShortName)
				&& jRouteShortName.equals(rts.getRoute().getShortName())) {
			same = true;
		} else if (!StringUtils.hasDigits(jRouteShortName) //
				&& !TextUtils.isEmpty(jRouteId) //
				&& jRouteId.endsWith(String.valueOf(rts.getRoute().getId()))) {
			same = true;
		}
		return same;
	}

	private static final String WB = " - wb";
	private static final String EB = " - eb";
	private static final String SB = " - sb";
	private static final String NB = " - nb";

	private static final String MO = " - mo";
	private static final String AF = " - af";
	public static final int MORNING = 11;
	public static final int AFTERNOON = 13;

	public static final int EAST = 1;
	public static final int WEST = 2;
	public static final int NORTH = 3;
	public static final int SOUTH = 4;

	public static final int NORTH_SPLITTED_CIRCLE = 3000;
	public static final int SOUTH_SPLITTED_CIRCLE = 4000;

	protected boolean isSameTrip(@NonNull RouteTripStop rts, @NonNull String jTripHeadsign) { // YRT Viva ONLY
		String jTripHeadsignLC = jTripHeadsign.toLowerCase(Locale.ENGLISH);
		String tripId = String.valueOf(rts.getTrip().getId());
		switch (rts.getTrip().getHeadsignType()) {
		case Trip.HEADSIGN_TYPE_STRING:
			if (jTripHeadsignLC.endsWith(MO)) {
				return tripId.endsWith("0" + MORNING) //
						|| tripId.endsWith("000");
			} else if (jTripHeadsignLC.endsWith(AF)) {
				return tripId.endsWith("0" + AFTERNOON) //
						|| tripId.endsWith("000");
			}
			if (jTripHeadsignLC.endsWith(NB)) {
				return tripId.endsWith("0" + NORTH) //
						|| tripId.endsWith("000");
			} else if (jTripHeadsignLC.endsWith(SB)) {
				return tripId.endsWith("0" + SOUTH) //
						|| tripId.endsWith("000");
			} else if (jTripHeadsignLC.endsWith(EB)) {
				return tripId.endsWith("0" + EAST) //
						|| tripId.endsWith("000");
			} else if (jTripHeadsignLC.endsWith(WB)) {
				return tripId.endsWith("0" + WEST) //
						|| tripId.endsWith("000");
			}
			break;
		}
		return true; // unknown?
	}

	private long getTimestamp(JSONObject jArrivalsAndDeparture) {
		try {
			long timestamp = jArrivalsAndDeparture.optLong(JSON_PREDICTED_DEPARTURE_TIME, 0L);
			if (timestamp > 0L) {
				return timestamp;
			}
			timestamp = jArrivalsAndDeparture.optLong(JSON_PREDICTED_ARRIVAL_TIME, 0L);
			if (timestamp > 0L) {
				return timestamp;
			}
			timestamp = jArrivalsAndDeparture.optLong(JSON_SCHEDULED_DEPARTURE_TIME, 0L);
			if (timestamp > 0L) {
				return timestamp;
			}
			return jArrivalsAndDeparture.optLong(JSON_SCHEDULED_ARRIVAL_TIME, 0L);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while reading timestamp!");
			return 0L;
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
	private OneBusAwayDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private OneBusAwayDbHelper getDBHelper(Context context) {
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

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return OneBusAwayDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	public OneBusAwayDbHelper getNewDbHelper(@NonNull Context context) {
		return new OneBusAwayDbHelper(context.getApplicationContext());
	}

	public static class OneBusAwayDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = OneBusAwayDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link OneBusAwayDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "oba.db";

		public static final String T_ONE_BUS_AWAY_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_ONE_BUS_AWAY_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_ONE_BUS_AWAY_STATUS).build();

		private static final String T_ONE_BUS_AWAY_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_ONE_BUS_AWAY_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link OneBusAwayDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.one_bus_away_db_version);
			}
			return dbVersion;
		}

		public OneBusAwayDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_ONE_BUS_AWAY_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_ONE_BUS_AWAY_STATUS_SQL_CREATE);
		}
	}
}
