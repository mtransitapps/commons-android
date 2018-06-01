package org.mtransit.android.commons.provider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.CleanUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.RegexUtils;
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

@SuppressLint("Registered")
public class BCTransitNextRideProvider extends MTContentProvider implements StatusProviderContract {

	private static final String LOG_TAG = BCTransitNextRideProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link BCTransitNextRideProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(@NonNull Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link BCTransitNextRideProvider} implementations in same app.
	 */
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.bc_transit_next_ride_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link BCTransitNextRideProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String apiUrl = null;

	/**
	 * Override if multiple {@link BCTransitNextRideProvider} implementations in same app.
	 */
	private static String getAPI_URL(@NonNull Context context) {
		if (apiUrl == null) {
			apiUrl = context.getResources().getString(R.string.bc_transit_next_ride_api_url);
		}
		return apiUrl;
	}

	private static String apiTimeZone = null;

	/**
	 * Override if multiple {@link BCTransitNextRideProvider} implementations in same app.
	 */
	private static String getAPI_TIME_ZONE(@NonNull Context context) {
		if (apiTimeZone == null) {
			apiTimeZone = context.getResources().getString(R.string.bc_transit_next_ride_api_timezone);
		}
		return apiTimeZone;
	}

	private static final long BC_TRANSIT_NEXT_RIDE_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long BC_TRANSIT_NEXT_RIDE_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long BC_TRANSIT_NEXT_RIDE_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long BC_TRANSIT_NEXT_RIDE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long BC_TRANSIT_NEXT_RIDE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getStatusMaxValidityInMs() {
		return BC_TRANSIT_NEXT_RIDE_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return BC_TRANSIT_NEXT_RIDE_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return BC_TRANSIT_NEXT_RIDE_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return BC_TRANSIT_NEXT_RIDE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return BC_TRANSIT_NEXT_RIDE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Override
	public POIStatus getCachedStatus(Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "Trying to get cached schedule status w/o schedule filter '%s'! #ShouldNotHappen", statusFilter);
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (rts == null //
				|| TextUtils.isEmpty(rts.getStop().getCode()) //
				|| TextUtils.isEmpty(rts.getTrip().getHeadsignValue()) //
				|| TextUtils.isEmpty(rts.getRoute().getShortName())) {
			MTLog.w(this, "Trying to get cached status w/o stop code OR trip headsign OR route short name '%s'! #ShouldNotHappen", rts);
			return null;
		}
		String uuid = getAgencyRouteStopTargetUUID(rts);
		POIStatus status = StatusProvider.getCachedStatusS(this, uuid);
		if (status != null) {
			status.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom tag
			if (status instanceof Schedule) {
				((Schedule) status).setDescentOnly(rts.isDescentOnly());
			}
		}
		return status;
	}

	private static String getAgencyRouteStopTargetUUID(@NonNull RouteTripStop rts) {
		return getAgencyRouteStopTargetUUID(rts.getAuthority(), rts.getRoute().getShortName(), rts.getTrip().getHeadsignValue(), rts.getStop().getCode());
	}

	private static String getAgencyRouteStopTargetUUID(String agencyAuthority, String routeShortName, String tripHeadsign, String stopCode) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName, tripHeadsign, stopCode);
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
		return BCTransitNextRideDbHelper.T_BC_TRANSIT_NEXT_RIDE_STATUS;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	@Override
	public POIStatus getNewStatus(Filter statusFilter) {
		if (statusFilter == null || !(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "Trying to get new schedule status w/o schedule filter '%s'! #ShouldNotHappen", statusFilter);
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (rts == null //
				|| TextUtils.isEmpty(rts.getStop().getCode()) //
				|| TextUtils.isEmpty(rts.getTrip().getHeadsignValue()) //
				|| TextUtils.isEmpty(rts.getRoute().getShortName())) {
			MTLog.w(this, "Trying to get new status w/o stop code OR trip headsign OR route short name '%s'! #ShouldNotHappen", rts);
			return null;
		}
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	// https://nextride.nanaimo.bctransit.com/api/Stop?term=110169
	private static String getStopUrlString(@NonNull String apiUrl, @NonNull String stopCode) {
		return new StringBuilder() //
				.append(apiUrl) //
				.append("/Stop?term=") //
				.append(stopCode) //
				.toString();
	}

	// https://nextride.nanaimo.bctransit.com/api/PredictionData?stopid=2940&shouldLog=true
	private static String getPredictionDataUrlString(@NonNull String apiUrl, @NonNull String stopId) {
		return new StringBuilder() //
				.append(apiUrl) //
				.append("/PredictionData?stopid=") //
				.append(stopId) //
				.append("&shouldLog=true") //
				.toString();
	}

	private void loadRealTimeStatusFromWWW(RouteTripStop rts) {
		Context context = getContext();
		if (context == null) {
			MTLog.w(this, "Trying to real-time status w/o context! #ShouldNotHappen");
			return;
		}
		String apiUrl = getAPI_URL(context);
		// 1 - FIND STOP ID
		String stopId = loadStopIdFromWWW(apiUrl, rts.getStop().getCode());
		if (stopId == null) {
			MTLog.w(this, "Stop ID not found for %s! #ShouldNotHappen", rts);
			return;
		}
		// 2 - ACTUALLY LOAD PREDICTIONS
		try {
			String urlString = getPredictionDataUrlString(apiUrl, stopId);
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				Collection<POIStatus> statuses = parseAgencyJSON(context, jsonString, rts, newLastUpdateInMs);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(getAgencyRouteStopTargetUUID(rts)));
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

	@Nullable
	private String loadStopIdFromWWW(@NonNull String apiUrl, @NonNull String stopCode) {
		try {
			String urlString = getStopUrlString(apiUrl, stopCode);
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				String jsonString = FileUtils.getString(urlc.getInputStream());
				String stopId = parseStopIdAgencyJSON(jsonString, stopCode);
				return stopId;
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
		return null;
	}

	@Nullable
	private String parseStopIdAgencyJSON(String jsonString, @NonNull String stopCode) {
		try {
			JSONArray json = jsonString == null ? null : new JSONArray(jsonString);
			if (json != null && json.length() > 0) {
				for (int r = 0; r < json.length(); r++) {
					JSONObject jStop = json.getJSONObject(r);
					if (jStop != null && jStop.has("StopCode")) {
						String jStopCode = jStop.getString("StopCode");
						if (jStopCode.equalsIgnoreCase(stopCode)) {
							if (jStop.has("StopID")) {
								int stopId = jStop.getInt("StopID");
								if (stopId > 0) {
									return String.valueOf(stopId);
								}
							}
						}

					}
				}
			}
			return null;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	@Nullable
	private static TimeZone apiTZ = null;

	@NonNull
	private static TimeZone getAPI_TZ(@NonNull Context context) {
		if (apiTZ == null) {
			apiTZ = TimeZone.getTimeZone(getAPI_TIME_ZONE(context));
		}
		return apiTZ;
	}

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	public static final String JSON_ROUTE_CODE = "routeCode";
	public static final String JSON_PREDICTIONS = "Predictions";
	public static final String JSON_GROUP_BY_PATTERN = "grpByPtrn";
	public static final String JSON_DIRECTION_NAME = "directName";
	public static final String JSON_PREDICT_TIME = "PredictTime";
	public static final String JSON_ROUTE_NAME = "routeName";
	public static final String JSON_PATTERN_NAME = "patternName";
	public static final String JSON_SCHEDULE_TIME = "ScheduleTime";
	public static final String JSON_SEQ_NO = "SeqNo";

	private Collection<POIStatus> parseAgencyJSON(@NonNull Context context, String jsonString, RouteTripStop rts, long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> poiStatuses = new ArrayList<>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_GROUP_BY_PATTERN)) {
				JSONArray jGroups = json.getJSONArray(JSON_GROUP_BY_PATTERN);
				if (jGroups != null && jGroups.length() > 0) {
					Schedule newSchedule = new Schedule(getAgencyRouteStopTargetUUID(rts), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
							PROVIDER_PRECISION_IN_MS, false);
					for (int g = 0; g < jGroups.length(); g++) {
						JSONObject jGroup = jGroups.getJSONObject(g);
						if (jGroup == null
								|| !jGroup.has(JSON_PREDICTIONS)
								|| !jGroup.has(JSON_ROUTE_CODE)
								|| !jGroup.has(JSON_DIRECTION_NAME)) {
							MTLog.w(this, "Trying to parse incomplete Group '%s' ! #ShouldNotHappen", jGroup);
							continue;
						}
						String jRouteCode = jGroup.getString(JSON_ROUTE_CODE); // ex: 0
						if (TextUtils.isEmpty(jRouteCode)) {
							MTLog.w(this, "Trying to parse Predictions w/o route code! #ShouldNotHappen");
							continue;
						}
						JSONArray jPredictions = jGroup.getJSONArray(JSON_PREDICTIONS);
						if (jPredictions == null || jPredictions.length() == 0) {
							MTLog.w(this, "Trying to parse empty Predictions! #ShouldNotHappen");
							continue;
						}
						String jDirectName = jGroup.getString(JSON_DIRECTION_NAME); // ex: Outbound | Inbound | Clockwise
						if (TextUtils.isEmpty(jDirectName)) {
							MTLog.w(this, "Trying to parse Predictions w/o direction name! #ShouldNotHappen");
							continue;
						}
						if (!jRouteCode.equalsIgnoreCase(rts.getRoute().getShortName())) {
							MTLog.d(this, "Skipping different route '%s'", jRouteCode);
							continue;
						}
						boolean circleRoute = false;
						String tripId = String.valueOf(rts.getTrip().getId());
						if ("Inbound".equalsIgnoreCase(jDirectName)) {
							if (!tripId.endsWith("00")) {
								MTLog.d(this, "Skipping different route trip '%s' VS '%s'", jDirectName, rts.getTrip());
								continue;
							}
						} else if ("Outbound".equalsIgnoreCase(jDirectName)) {
							if (!tripId.endsWith("01")
									&& !tripId.endsWith("010")
									&& !tripId.endsWith("011")) {
								MTLog.d(this, "Skipping different route trip '%s' VS '%s'", jDirectName, rts.getTrip());
								continue;
							}
						} else if ("Clockwise".equalsIgnoreCase(jDirectName)) {
							circleRoute = true;
						} else if ("Counterclockwise".equalsIgnoreCase(jDirectName)) {
							circleRoute = true;
						} else {
							MTLog.w(this, "Trying to parse Predictions with unpredictable direction name '%s'! #ShouldNotHappen", jDirectName);
						}
						String jRouteName = jGroup.optString(JSON_ROUTE_NAME); // ex: Abcd & Efgh
						String jPatternName = jGroup.optString(JSON_PATTERN_NAME); // ex: 0 - Abcd
						String tripHeadsign = cleanPatternName(jPatternName, jRouteCode, jRouteName);
						tripHeadsign = cleanTripHeadsign(tripHeadsign);
						tripHeadsign = removeStaticHeadsign(tripHeadsign, rts.getTrip().getHeading(context));
						boolean hasTripHeadsign = !TextUtils.isEmpty(tripHeadsign);
						boolean isFirstAndLastInCircle = false;
						if (circleRoute) {
							for (int p = 0; p < jPredictions.length(); p++) {
								JSONObject jPrediction = jPredictions.getJSONObject(p);
								if (jPrediction != null && jPrediction.has(JSON_SEQ_NO)) {
									int jSeqNo = jPrediction.getInt(JSON_SEQ_NO);
									if (jSeqNo == 1) {
										isFirstAndLastInCircle = true;
										break;
									}
								}
							}
						}
						for (int p = 0; p < jPredictions.length(); p++) {
							JSONObject jPrediction = jPredictions.getJSONObject(p);
							if (jPrediction != null) {
								if (rts.isDescentOnly()) {
									int jSeqNo = jPrediction.optInt(JSON_SEQ_NO, -1);
									if (jSeqNo == 1) {
										MTLog.d(this, "Skipping descent only stop '%s' for seqNo '%s'", rts, jSeqNo);
										continue;
									}
								} else if (isFirstAndLastInCircle) {
									int jSeqNo = jPrediction.optInt(JSON_SEQ_NO, -1);
									if (jSeqNo > 1) {
										if (!rts.isDescentOnly()) {
											MTLog.d(this, "Skipping non-descent only stop '%s' for seqNo '%s'", rts, jSeqNo);
											continue;
										}
									}
								}
								String time = null;
								if (jPrediction.has(JSON_PREDICT_TIME)) {
									time = jPrediction.getString(JSON_PREDICT_TIME);
								}
								if (TextUtils.isEmpty(time)) {
									if (jPrediction.has(JSON_SCHEDULE_TIME)) {
										time = jPrediction.getString(JSON_SCHEDULE_TIME);
									}
								}
								Long t = getTimeFormatter(context).parseThreadSafe(time).getTime();
								String jPredictionType = jPrediction.optString("PredictionType"); // "Predicted" => REAL-TIME
								boolean isRealTime = "Predicted".equalsIgnoreCase(jPredictionType);
								Schedule.Timestamp timestamp = new Schedule.Timestamp(TimeUtils.timeToTheMinuteMillis(t));
								if (hasTripHeadsign) {
									timestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, tripHeadsign);
								}
								newSchedule.addTimestampWithoutSort(timestamp);
							}
						}
					}
					newSchedule.sortTimestamps();
					poiStatuses.add(newSchedule);
				}
			}
			return poiStatuses;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	private static final Pattern STARTS_ENDS_WITH_TO = Pattern.compile("(" //
			+ "(^[\\s]*(to)[\\s]*)" //
			+ "|" //
			+ "(( \\- )?[\\s]*(to)[\\s]*$)" //
			+ ")", Pattern.CASE_INSENSITIVE);

	protected String removeStaticHeadsign(String tripHeadsign, String staticHeadsign) {
		// REMOVE STATIC HEADSIGN (start|end)
		Pattern regex = Pattern.compile("(" //
				+ "(^[\\s]?(" + staticHeadsign + ")[\\s]?$)" // "Abcd"
				+ "|" //
				+ "([\\s]?(" + staticHeadsign + ")[\\s]?([\\s]via))" // "Abcd via"
				+ "|" //
				+ "([\\s]?(" + staticHeadsign + ")[\\s]?([\\s]to))" // "Abcd to"
				+ "|" //
				+ "(( \\- )?[\\s]?(via[\\s])[\\s]?(" + staticHeadsign + ")[\\s]?)" // " - via Abcd"
				+ "|" //
				+ "(( \\- )?[\\s]?(to[\\s])[\\s]?(" + staticHeadsign + ")[\\s]?)" // " - to Abcd"
				+ ")", Pattern.CASE_INSENSITIVE);
		StringBuilder replaceWithSb = new StringBuilder();
		if (!TextUtils.isEmpty(RegexUtils.extractMatcherGroup(regex.matcher(tripHeadsign), 6))) {
			replaceWithSb.append("$6");
		}
		if (!TextUtils.isEmpty(RegexUtils.extractMatcherGroup(regex.matcher(tripHeadsign), 9))) {
			replaceWithSb.append("$9");
		}
		tripHeadsign = regex.matcher(tripHeadsign).replaceAll(replaceWithSb.toString());
		// REMOVE TO (start|end)
		tripHeadsign = STARTS_ENDS_WITH_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		return tripHeadsign;
	}

	private static final Pattern STARTS_WITH_TO = Pattern.compile("(^[\\s]*to[\\s]*)", Pattern.CASE_INSENSITIVE);

	protected String cleanPatternName(String jPatternName, String jRouteCode, String jRouteName) {
		String patternName = jPatternName;
		// REMOVE ROUTE SHORT NAME (route code)
		Pattern regex = Pattern.compile("(^" //
				+ "(" + jRouteCode + ")" // "0"
				+ "(([A-Z][\\s])|[\\s])" // "A " | " "
				+ "(([\\s]?[\\w]+[\\s]?)*)?" // " Abcd "
				+ "([\\s]?\\-[\\s]?)?" // " - "
				+ ")", Pattern.CASE_INSENSITIVE);
		StringBuilder replaceWithSb = new StringBuilder();
		if (!TextUtils.isEmpty(RegexUtils.extractMatcherGroup(regex.matcher(patternName), 4))) {
			replaceWithSb.append("$4");
		}
		if (!TextUtils.isEmpty(RegexUtils.extractMatcherGroup(regex.matcher(patternName), 5))) {
			replaceWithSb.append("$5");
		}
		patternName = regex.matcher(patternName).replaceAll(replaceWithSb.toString());
		// REMOVE ROUTE LONG NAME
		patternName = Pattern.compile("(^"
				+ "(" + jRouteName.replaceAll("[\\s]+\\/[\\s]+", " & ") + "([\\s]|$))"
				+ "([\\s]?\\-[\\s]?)?"
				+ ")", Pattern.CASE_INSENSITIVE)
				.matcher(patternName).replaceAll(StringUtils.EMPTY);
		patternName = STARTS_WITH_TO.matcher(patternName).replaceAll(StringUtils.EMPTY);
		return patternName;
	}

	private static final String EXCHANGE_SHORT = "Exch";
	private static final String VI_UNIVERSITY_SHORT = "VIU";

	private static final Pattern EXCHANGE_ = Pattern.compile("((^|\\W){1}(exchange)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = "$2" + EXCHANGE_SHORT + "$4";

	private static final Pattern VI_UNIVERSITY_ = Pattern.compile("((^|\\W){1}(vi university|viu)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String VI_UNIVERSITY_REPLACEMENT = "$2" + VI_UNIVERSITY_SHORT + "$4";

	private static final Pattern CLEAN_P1 = Pattern.compile("[\\s]*\\([\\s]*");
	private static final String CLEAN_P1_REPLACEMENT = " (";
	private static final Pattern CLEAN_P2 = Pattern.compile("[\\s]*\\)[\\s]*");
	private static final String CLEAN_P2_REPLACEMENT = ") ";

	@NonNull
	private String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = EXCHANGE_.matcher(tripHeadsign).replaceAll(EXCHANGE_REPLACEMENT);
		tripHeadsign = VI_UNIVERSITY_.matcher(tripHeadsign).replaceAll(VI_UNIVERSITY_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CLEAN_P1.matcher(tripHeadsign).replaceAll(CLEAN_P1_REPLACEMENT);
		tripHeadsign = CLEAN_P2.matcher(tripHeadsign).replaceAll(CLEAN_P2_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanLabel(tripHeadsign);
		return tripHeadsign;
	}

	@Nullable
	private static ThreadSafeDateFormatter timeFormatter = null;

	@NonNull
	private ThreadSafeDateFormatter getTimeFormatter(@NonNull Context context) {
		if (timeFormatter == null) {
			timeFormatter = new ThreadSafeDateFormatter("yyyy-MM-dd'T'HH:mm:ss");
			timeFormatter.setTimeZone(getAPI_TZ(context));
		}
		return timeFormatter;
	}

	@Override
	public boolean onCreateMT() {
		if (getContext() == null) {
			return true; // or false?
		}
		ping(getContext());
		return true;
	}

	@Override
	public void ping() {
		if (getContext() == null) {
			return;
		}
		ping(getContext());
	}

	public void ping(@NonNull Context context) {
		PackageManagerUtils.removeModuleLauncherIcon(context);
		getTimeFormatter(context); // force init before 1st usage
	}

	private static BCTransitNextRideDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private BCTransitNextRideDbHelper getProviderDBHelper(@NonNull Context context) {
		if (dbHelper == null) { // initialize
			dbHelper = getNewDbHelper(context);
			currentDbVersion = getCurrentDbVersion(context);
		} else { // reset
			try {
				if (currentDbVersion != getCurrentDbVersion(context)) {
					dbHelper.close();
					dbHelper = null;
					return getProviderDBHelper(context);
				}
			} catch (Exception e) { // fail if locked, will try again later
				MTLog.w(this, e, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	/**
	 * Override if multiple {@link BCTransitNextRideProvider} implementations in same app.
	 */
	@Deprecated
	public int getCurrentDbVersion() {
		if (getContext() == null) {
			MTLog.w(this, "Trying to read current DB version w/o context! #ShouldNotHappen");
			return -1;
		}
		return getCurrentDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link BCTransitNextRideProvider} implementations in same app.
	 */
	public int getCurrentDbVersion(@NonNull Context context) {
		return BCTransitNextRideDbHelper.getDbVersion(context);
	}

	/**
	 * Override if multiple {@link BCTransitNextRideProvider} implementations in same app.
	 */
	public BCTransitNextRideDbHelper getNewDbHelper(@NonNull Context context) {
		return new BCTransitNextRideDbHelper(context.getApplicationContext());
	}

	@Deprecated
	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		//noinspection ConstantConditions
		return getURI_MATCHER(getContext());
	}

	@NonNull
	// TODO @Override
	public UriMatcher getURI_MATCHER(@NonNull Context context) {
		return getURIMATCHER(context);
	}

	@Deprecated
	@NonNull
	@Override
	public Uri getAuthorityUri() {
		//noinspection ConstantConditions
		return getAuthorityUri(getContext());
	}

	@NonNull
	// TODO @Override
	public Uri getAuthorityUri(@NonNull Context context) {
		return getAUTHORITY_URI(context);
	}

	@Deprecated
	@NonNull
	@Override
	public SQLiteOpenHelper getDBHelper() {
		//noinspection ConstantConditions
		return getDBHelper(getContext());
	}

	@NonNull
	// TODO @Override
	public SQLiteOpenHelper getDBHelper(@NonNull Context context) {
		return getProviderDBHelper(context);
	}

	@Override
	public Cursor queryMT(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String
			sortOrder) {
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

	public static class BCTransitNextRideDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = BCTransitNextRideDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link BCTransitNextRideDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "bc_transit_next_ride.db";

		public static final String T_BC_TRANSIT_NEXT_RIDE_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_BC_TRANSIT_NEXT_RIDE_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_BC_TRANSIT_NEXT_RIDE_STATUS).build();

		private static final String T_BC_TRANSIT_NEXT_RIDE_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_BC_TRANSIT_NEXT_RIDE_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link BCTransitNextRideDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.bc_transit_next_ride_db_version);
			}
			return dbVersion;
		}

		public BCTransitNextRideDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_BC_TRANSIT_NEXT_RIDE_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_BC_TRANSIT_NEXT_RIDE_STATUS_SQL_CREATE);
		}
	}
}
