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

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.ResourceUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Accessibility;
import org.mtransit.android.commons.data.Direction;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.provider.agency.AgencyUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.SourceUtils;
import org.mtransit.commons.provider.OneBusAwayProviderCommons;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@SuppressLint("Registered")
public class OneBusAwayProvider extends MTContentProvider implements StatusProviderContract {

	private static final String LOG_TAG = OneBusAwayProvider.class.getSimpleName();

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
	private static java.util.List<Pattern> scheduleHeadsignCleanRegex = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<Pattern> getSCHEDULE_HEADSIGN_CLEAN_REGEX(@NonNull Context context) {
		if (scheduleHeadsignCleanRegex == null) {
			scheduleHeadsignCleanRegex = ResourceUtils.getRegexPatternArray(context,
					R.array.one_bus_away_schedule_head_sign_clean_regex,
					Pattern.CASE_INSENSITIVE);
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

	@Nullable
	private static java.util.List<Pattern> directionHeadSignMatchOBARegex = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<Pattern> getDIRECTION_HEAD_SIGN_MATCH_OBA_REGEX(@NonNull Context context) {
		if (directionHeadSignMatchOBARegex == null) {
			directionHeadSignMatchOBARegex = ResourceUtils.getRegexPatternArray(context,
					R.array.one_bus_away_trip_head_sign_match_oba_regex,
					Pattern.CASE_INSENSITIVE);
		}
		return directionHeadSignMatchOBARegex;
	}

	@Nullable
	private static java.util.List<Pattern> directionHeadSignMatchGTFSRegex = null;

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<Pattern> getDIRECTION_HEAD_SIGN_MATCH_GTFS_REGEX(@NonNull Context context) {
		if (directionHeadSignMatchGTFSRegex == null) {
			directionHeadSignMatchGTFSRegex = ResourceUtils.getRegexPatternArray(context,
					R.array.one_bus_away_trip_head_sign_match_gtfs_regex,
					Pattern.CASE_INSENSITIVE);
		}
		return directionHeadSignMatchGTFSRegex;
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
		String targetUUID = getAgencyRouteStopTagTargetUUID(rds);
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, targetUUID);
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rds.getUUID()); // target RDS UUID instead of custom OneBusAway Route & Stop tags
			if (rds.isNoPickup()) {
				if (cachedStatus instanceof Schedule) {
					Schedule schedule = (Schedule) cachedStatus;
					schedule.setNoPickup(true); // API doesn't know about "descent only"
				}
			}
		}
		return cachedStatus;
	}

	private String getAgencyRouteStopTagTargetUUID(@NonNull RouteDirectionStop rds) {
		return rds.getUUID();
	}

	private String getStopTag(@NonNull Context context, @NonNull RouteDirectionStop rds) {
		if (isAGENCY_STOP_TAG_IS_STOP_CODE(context)) {
			return rds.getStop().getCode();
		}
		return String.valueOf(rds.getStop().getId());
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
		return OneBusAwayDbHelper.T_ONE_BUS_AWAY_STATUS;
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
		loadPredictionsFromWWW(requireContextCompat(), rds);
		return getCachedStatus(statusFilter);
	}

	// http://developer.onebusaway.org/modules/onebusaway-application-modules/1.1.14/api/where/methods/arrivals-and-departures-for-stop.html
	// http://developer.onebusaway.org/modules/onebusaway-application-modules/1.1.14/api/where/elements/arrival-and-departure.html
	@NonNull
	private String getStopPredictionsUrlString(@NonNull Context context, @NonNull String apiKey, @NonNull RouteDirectionStop rds) {
		return String.format(getPREDICTION_URL(context), getStopTag(context, rds), apiKey);
	}

	private void loadPredictionsFromWWW(@NonNull Context context, @NonNull RouteDirectionStop rds) {
		try {
			String urlString = getStopPredictionsUrlString(context, getAPI_KEY(context), rds);
			MTLog.i(this, "Loading from '%s'...", getStopPredictionsUrlString(context, "API_KEY", rds));
			String sourceLabel = SourceUtils.getSourceLabel(getPREDICTION_URL(context));
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadPredictionsFromWWW() > jsonString: %s.", jsonString);
				Collection<POIStatus> statuses = parseAgencyJSON(context, jsonString, rds, sourceLabel, newLastUpdateInMs);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(getAgencyRouteStopTagTargetUUID(rds)));
				MTLog.i(this, "Found %d schedule statuses.", (statuses == null ? 0 : statuses.size()));
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
	@SuppressWarnings("unused")
	private static final String JSON_ROUTE_LONG_NAME = "routeLongName";
	private static final String JSON_ROUTE_SHORT_NAME = "routeShortName";
	private static final String JSON_TRIP_HEADSIGN = "tripHeadsign";
	@SuppressWarnings("unused") // TODO ?
	private static final String JSON_DEPARTURE_ENABLED = "departureEnabled";

	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	private Collection<POIStatus> parseAgencyJSON(@NonNull Context context, @Nullable String jsonString, @NonNull RouteDirectionStop rds, @Nullable String sourceLabel, long newLastUpdateInMs) {
		try {
			final String localTimeZoneId = AgencyUtils.getRDSAgencyTimeZone(context);
			ArrayList<POIStatus> result = new ArrayList<>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_DATA)) {
				JSONObject jData = json.optJSONObject(JSON_DATA);
				if (jData != null && jData.has(JSON_ENTRY)) {
					JSONObject jEntry = jData.getJSONObject(JSON_ENTRY);
					if (jEntry.has(JSON_ARRIVALS_AND_DEPARTURES)) {
						JSONArray jArrivalsAndDepartures = jEntry.getJSONArray(JSON_ARRIVALS_AND_DEPARTURES);
						Schedule newSchedule = new Schedule(
								null,
								getAgencyRouteStopTagTargetUUID(rds),
								newLastUpdateInMs,
								getStatusMaxValidityInMs(),
								newLastUpdateInMs,
								PROVIDER_PRECISION_IN_MS,
								false,
								sourceLabel,
								false
						);
						for (int l = 0; l < jArrivalsAndDepartures.length(); l++) {
							JSONObject jArrivalsAndDeparture = jArrivalsAndDepartures.getJSONObject(l);
							String jRoutId = jArrivalsAndDeparture.getString(JSON_ROUTE_ID);
							String jRouteShortName = jArrivalsAndDeparture.getString(JSON_ROUTE_SHORT_NAME);
							boolean sameRoute = isSameRoute(rds, jRoutId, jRouteShortName);
							if (!sameRoute) {
								continue;
							}
							long timestamp = getTimestamp(jArrivalsAndDeparture);
							if (timestamp <= 0L) {
								continue;
							}
							boolean isRealTime = jArrivalsAndDeparture.optBoolean(JSON_PREDICTED, false);
							Schedule.Timestamp newTimestamp = new Schedule.Timestamp(TimeUtils.timeToTheTensSecondsMillis(timestamp), localTimeZoneId);
							try {
								String jTripHeadsign = jArrivalsAndDeparture.getString(JSON_TRIP_HEADSIGN);
								if (!TextUtils.isEmpty(jTripHeadsign)) {
									boolean sameTrip = isSameDirection(context, rds, jTripHeadsign);
									if (!sameTrip) {
										continue;
									}
									jTripHeadsign = cleanDirectionHeadsign(context, jTripHeadsign);
									jTripHeadsign = cleanDirectionHeadsign(context, jTripHeadsign, rds); // remove rds trip head-sign / route from head sign
									boolean isDepartureEnabled = jArrivalsAndDeparture.optBoolean(JSON_DEPARTURE_ENABLED, true);
									if (!isDepartureEnabled) {
										newTimestamp.setHeadsign(Direction.HEADSIGN_TYPE_NO_PICKUP, null);
									} else {
										newTimestamp.setHeadsign(Direction.HEADSIGN_TYPE_STRING, jTripHeadsign);
									}
								}
							} catch (Exception e) {
								MTLog.w(this, e, "Error while reading trip headsign in '%s'!", jArrivalsAndDeparture);
							}
							newTimestamp.setRealTime(isRealTime);
							if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
								newTimestamp.setAccessible(Accessibility.UNKNOWN); // no information available
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

	private String cleanDirectionHeadsign(@NonNull Context context, @NonNull String tripHeadsign) {
		try {
			final List<Pattern> patterns = getSCHEDULE_HEADSIGN_CLEAN_REGEX(context);
			final List<String> replacements = getSCHEDULE_HEADSIGN_CLEAN_REPLACEMENT(context);
			for (int c = 0; c < patterns.size(); c++) {
				try {
					final Pattern pattern = patterns.get(c);
					final String replacement = replacements.get(c);
					if (pattern == null || replacement == null) {
						continue; // skip invalid pattern & replacement
					}
					tripHeadsign = pattern.matcher(tripHeadsign).replaceAll(replacement);
				} catch (Exception e) {
					MTLog.w(this, e, "Error while cleaning trip head sign %s for %s cleaning configuration!", tripHeadsign, c);
				}
			}
			tripHeadsign = OneBusAwayProviderCommons.cleanTripHeadsign(tripHeadsign); // TODO keep via if same to?
			return tripHeadsign;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadsign);
			return tripHeadsign;
		}
	}

	@NonNull
	protected String cleanDirectionHeadsign(@NonNull Context context, @NonNull String tripHeadsign, @NonNull RouteDirectionStop rds) {
		try {
			final String rdsDirectionHeading = rds.getDirection().getHeading(context);
			final String routeLongName = rds.getRoute().getLongName();
			tripHeadsign = CleanUtils.removeStrings(tripHeadsign, rdsDirectionHeading, routeLongName);
			tripHeadsign = CleanUtils.cleanLabel(tripHeadsign);
			return tripHeadsign;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadsign);
			return tripHeadsign;
		}
	}

	protected boolean isSameRoute(@NonNull RouteDirectionStop rds, @NonNull String jRouteId, @NonNull String jRouteShortName) { // YRT Viva ONLY
		boolean same = false;
		if (!TextUtils.isEmpty(jRouteShortName)
				&& jRouteShortName.equals(rds.getRoute().getShortName())) {
			same = true; // same route short name
		} else if (!StringUtils.hasDigits(jRouteShortName) // route short name != digits
				&& !TextUtils.isEmpty(jRouteId) //
				&& jRouteId.endsWith(String.valueOf(rds.getRoute().getId()))) {
			same = true; // JSON route ID ends with GTFS route ID (ex: YRT_603 & 603)
		}
		return same;
	}

	private boolean isSameDirection(@NonNull Context context, @NonNull RouteDirectionStop rds, @NonNull String jTripHeadsign) {
		switch (rds.getDirection().getHeadsignType()) {
		case Direction.HEADSIGN_TYPE_STRING:
			final String gtfsDirectionHeadSign = rds.getDirection().getHeadsignValue();
			final List<Pattern> obaRegexList = getDIRECTION_HEAD_SIGN_MATCH_OBA_REGEX(context);
			final List<Pattern> gtfsRegexList = getDIRECTION_HEAD_SIGN_MATCH_GTFS_REGEX(context);
			if (obaRegexList.isEmpty() && gtfsRegexList.isEmpty()) {
				return true; // no check = all trips match
			}
			boolean matchAtLeastOneObaRegex = false;
			for (int c = 0; c < obaRegexList.size(); c++) {
				final Pattern obaRegex = obaRegexList.get(c);
				final Pattern gtfsRegex = c >= gtfsRegexList.size() ? null : gtfsRegexList.get(c);
				if (obaRegex == null || gtfsRegex == null) {
					continue; // skip invalid regex
				}
				try {
					if (obaRegex.matcher(jTripHeadsign).find()) {
						//noinspection UnusedAssignment
						matchAtLeastOneObaRegex = true;
						return gtfsRegex.matcher(gtfsDirectionHeadSign).find();
					}
				} catch (Exception e) {
					MTLog.w(this, e, "Error while matching pattern '%s' for %s cleaning configuration!", obaRegex, gtfsRegex, c);
				}
			}
			if (!matchAtLeastOneObaRegex) {
				MTLog.d(this, "No checks for trip head-sign '%s'.", gtfsDirectionHeadSign);
				return true; // no check for this kind of trip head-sign
			}
		case Direction.HEADSIGN_TYPE_NO_PICKUP:
		case Direction.HEADSIGN_TYPE_DIRECTION:
		case Direction.HEADSIGN_TYPE_INBOUND:
		case Direction.HEADSIGN_TYPE_NONE:
		case Direction.HEADSIGN_TYPE_STOP_ID:
			break;
		}
		MTLog.w(this, "Unexpected trip '%s' to match with '%s'!", jTripHeadsign, rds);
		return true; // unknown?
	}

	private long getTimestamp(@NonNull JSONObject jArrivalsAndDeparture) {
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

	@MainThread
	@Override
	public boolean onCreateMT() {
		return true;
	}

	@Nullable
	private OneBusAwayDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private OneBusAwayDbHelper getDBHelper(@NonNull Context context) {
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

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return OneBusAwayDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link OneBusAwayProvider} implementations in same app.
	 */
	@NonNull
	public OneBusAwayDbHelper getNewDbHelper(@NonNull Context context) {
		return new OneBusAwayDbHelper(context.getApplicationContext());
	}

	public static class OneBusAwayDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = OneBusAwayDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link OneBusAwayDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "oba.db";

		static final String T_ONE_BUS_AWAY_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

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

		OneBusAwayDbHelper(@NonNull Context context) {
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
