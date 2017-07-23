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
import org.json.JSONException;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.CleanUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
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
public class OneBusAwayProvider extends MTContentProvider implements StatusProviderContract {

	private static final String TAG = OneBusAwayProvider.class.getSimpleName();

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
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	private static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.one_bus_away_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String predictionUrl = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	private static String getPREDICTION_URL(Context context) {
		if (predictionUrl == null) {
			predictionUrl = context.getResources().getString(R.string.one_bus_away_prediction_url_and_stop_tag_and_api_key);
		}
		return predictionUrl;
	}

	private static Boolean agencyStopTagIsStopCode = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	private static boolean isAGENCY_STOP_TAG_IS_STOP_CODE(Context context) {
		if (agencyStopTagIsStopCode == null) {
			agencyStopTagIsStopCode = context.getResources().getBoolean(R.bool.one_bus_away_stop_tag_is_stop_code);
		}
		return agencyStopTagIsStopCode;
	}

	private static String apiKey = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	private static String getAPI_KEY(Context context) {
		if (apiKey == null) {
			apiKey = context.getResources().getString(R.string.one_bus_away_api_key);
		}
		return apiKey;
	}

	private static java.util.List<String> scheduleHeadsignCleanRegex = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCHEDULE_HEADSIGN_CLEAN_REGEX(Context context) {
		if (scheduleHeadsignCleanRegex == null) {
			scheduleHeadsignCleanRegex = Arrays.asList(context.getResources().getStringArray(R.array.one_bus_away_schedule_head_sign_clean_regex));
		}
		return scheduleHeadsignCleanRegex;
	}

	private static java.util.List<String> scheduleHeadsignCleanReplacement = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCHEDULE_HEADSIGN_CLEAN_REPLACEMENT(Context context) {
		if (scheduleHeadsignCleanReplacement == null) {
			scheduleHeadsignCleanReplacement = Arrays.asList(context.getResources().getStringArray(R.array.one_bus_away_schedule_head_sign_clean_replacement));
		}
		return scheduleHeadsignCleanReplacement;
	}

	private static final long ONE_BUS_WAY_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long ONE_BUS_WAY_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long ONE_BUS_WAY_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long ONE_BUS_WAY_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(5);
	private static final long ONE_BUS_WAY_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

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
			MTLog.w(this, "getNewStatus() > Can't find new schecule whithout schedule filter!");
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

	private String getAgencyRouteStopTagTargetUUID(RouteTripStop rts) {
		return rts.getUUID();
	}

	private String getStopTag(RouteTripStop rts) {
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
		if (statusFilter == null || !(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schecule whithout schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		loadPredictionsFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	private String getStopPredictionsUrlString(Context context, RouteTripStop rts) {
		return String.format(getPREDICTION_URL(context), getStopTag(rts), getAPI_KEY(context));
	}

	private void loadPredictionsFromWWW(RouteTripStop rts) {
		try {
			String urlString = getStopPredictionsUrlString(getContext(), rts);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				Collection<POIStatus> statuses = parseAgencyJSON(jsonString, rts, newLastUpdateInMs);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(new String[]{getAgencyRouteStopTagTargetUUID(rts)}));
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

	private static final String JSON_DATA = "data";
	private static final String JSON_ENTRY = "entry";
	private static final String JSON_ARRIVALS_AND_DEPARTURES = "arrivalsAndDepartures";
	private static final String JSON_PREDICTED_DEPARTURE_TIME = "predictedDepartureTime";
	private static final String JSON_PREDICTED_ARRIVAL_TIME = "predictedArrivalTime";
	private static final String JSON_SCHEDULED_DEPARTURE_TIME = "scheduledDepartureTime";
	private static final String JSON_SCHEDULED_ARRIVAL_TIME = "scheduledArrivalTime";
	private static final String JSON_ROUTE_SHORT_NAME = "routeShortName";
	private static final String JSON_TRIP_HEADSIGN = "tripHeadsign";

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10);

	private Collection<POIStatus> parseAgencyJSON(String jsonString, RouteTripStop rts, long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> result = new ArrayList<POIStatus>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_DATA)) {
				JSONObject jData = json.optJSONObject(JSON_DATA);
				if (jData != null && jData.has(JSON_ENTRY)) {
					JSONObject jEntry = jData.getJSONObject(JSON_ENTRY);
					if (jEntry != null && jEntry.has(JSON_ARRIVALS_AND_DEPARTURES)) {
						JSONArray jArrivalsAndDepartures = jEntry.getJSONArray(JSON_ARRIVALS_AND_DEPARTURES);
						Schedule newSchedule = new Schedule(getAgencyRouteStopTagTargetUUID(rts), newLastUpdateInMs, getStatusMaxValidityInMs(),
								newLastUpdateInMs, PROVIDER_PRECISION_IN_MS, false);
						for (int l = 0; l < jArrivalsAndDepartures.length(); l++) {
							JSONObject jArrivalsAndDeparture = jArrivalsAndDepartures.getJSONObject(l);
							String jRouteShortName = jArrivalsAndDeparture.getString(JSON_ROUTE_SHORT_NAME);
							boolean sameRoute = isSameRoute(rts, jRouteShortName);
							if (!sameRoute) {
								continue;
							}
							long timestamp = getTimestamp(jArrivalsAndDeparture);
							if (timestamp <= 0L) {
								continue;
							}
							Schedule.Timestamp newTimestamp = new Schedule.Timestamp(TimeUtils.timeToTheTensSecondsMillis(timestamp));
							try {
								String tripHeadsign = jArrivalsAndDeparture.getString(JSON_TRIP_HEADSIGN);
								if (!TextUtils.isEmpty(tripHeadsign)) {
									boolean sameTrip = isSameTrip(rts, tripHeadsign);
									if (!sameTrip) {
										continue;
									}
									newTimestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, cleanTripHeadsign(tripHeadsign, rts));
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

	private String cleanTripHeadsign(String tripHeadsign, RouteTripStop optRTS) {
		try {
			for (int c = 0; c < getSCHEDULE_HEADSIGN_CLEAN_REGEX(getContext()).size(); c++) {
				try {
					tripHeadsign = Pattern.compile(getSCHEDULE_HEADSIGN_CLEAN_REGEX(getContext()).get(c), Pattern.CASE_INSENSITIVE).matcher(tripHeadsign)
							.replaceAll(getSCHEDULE_HEADSIGN_CLEAN_REPLACEMENT(getContext()).get(c));
				} catch (Exception e) {
					MTLog.w(this, e, "Error while cleaning trip head sign %s for %s cleaning configuration!", tripHeadsign, c);
				}
			}
			if (optRTS != null) {
				String heading = getContext() == null ? optRTS.getTrip().getHeading() : optRTS.getTrip().getHeading(getContext());
				tripHeadsign = Pattern.compile("((^|\\W){1}(" + heading + "|" + optRTS.getRoute().getLongName() + ")(\\W|$){1})", Pattern.CASE_INSENSITIVE)
						.matcher(tripHeadsign).replaceAll(" ");
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

	private static final String VIVA = "viva";

	private boolean isSameRoute(RouteTripStop rts, String jRouteShortName) throws JSONException { // YRT Viva ONLY
		String jRouteShortNameLC = jRouteShortName.toLowerCase(Locale.ENGLISH);
		boolean same;
		if (jRouteShortNameLC.startsWith(VIVA)) {
			same = jRouteShortNameLC.endsWith(rts.getRoute().getShortName().toLowerCase(Locale.ENGLISH));
		} else {
			same = rts.getRoute().getShortName().equalsIgnoreCase(jRouteShortName);
		}
		return same;
	}

	private static final String WB = " - wb";
	private static final String EB = " - eb";
	private static final String SB = " - sb";
	private static final String NB = " - nb";

	private static final String MO = " - mo";
	private static final String AF = " - af";
	private static final String AM_HEADSIGN = "AM";
	private static final String PM_HEADSIGN = "PM";

	private boolean isSameTrip(RouteTripStop rts, String jTripHeadsign) throws JSONException { // YRT Viva ONLY
		String jTripHeadsignLC = jTripHeadsign.toLowerCase(Locale.ENGLISH);
		switch (rts.getTrip().getHeadsignType()) {
		case Trip.HEADSIGN_TYPE_STRING:
			if (jTripHeadsignLC.endsWith(MO)) {
				return AM_HEADSIGN.equals(rts.getTrip().getHeadsignValue());
			} else if (jTripHeadsignLC.endsWith(AF)) {
				return PM_HEADSIGN.equals(rts.getTrip().getHeadsignValue());
			}
			break;
		case Trip.HEADSIGN_TYPE_DIRECTION:
			if (jTripHeadsignLC.endsWith(NB)) {
				return Trip.HEADING_NORTH.equals(rts.getTrip().getHeadsignValue());
			} else if (jTripHeadsignLC.endsWith(SB)) {
				return Trip.HEADING_SOUTH.equals(rts.getTrip().getHeadsignValue());
			} else if (jTripHeadsignLC.endsWith(EB)) {
				return Trip.HEADING_EAST.equals(rts.getTrip().getHeadsignValue());
			} else if (jTripHeadsignLC.endsWith(WB)) {
				return Trip.HEADING_WEST.equals(rts.getTrip().getHeadsignValue());
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

	private static OneBusAwayDbHelper dbHelper;

	private static int currentDbVersion = -1;

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

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return OneBusAwayDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	public OneBusAwayDbHelper getNewDbHelper(Context context) {
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
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.one_bus_away_db_version);
			}
			return dbVersion;
		}

		public OneBusAwayDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_ONE_BUS_AWAY_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_ONE_BUS_AWAY_STATUS_SQL_CREATE);
		}
	}
}
