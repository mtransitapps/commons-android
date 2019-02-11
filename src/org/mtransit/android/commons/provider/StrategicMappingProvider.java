package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

@SuppressLint("Registered")
public class StrategicMappingProvider extends ContentProviderExtra implements StatusProviderContract {

	private static final String LOG_TAG = StrategicMappingProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link StrategicMappingProvider} implementations in same app.
	 */
	@NonNull
	private static UriMatcher getSTATIC_URI_MATCHER(@NonNull Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	@Nullable
	private static String authority = null;

	/**
	 * Override if multiple {@link StrategicMappingProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.strategic_mapping_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link StrategicMappingProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String apiUrl = null;

	/**
	 * Override if multiple {@link StrategicMappingProvider} implementations in same app.
	 */
	@NonNull
	private static String getAPI_URL(@NonNull Context context) {
		if (apiUrl == null) {
			apiUrl = context.getResources().getString(R.string.strategic_mapping_api_url);
		}
		return apiUrl;
	}

	@Nullable
	private static String apiTimeZone = null;

	/**
	 * Override if multiple {@link StrategicMappingProvider} implementations in same app.
	 */
	@NonNull
	private static String getAPI_TIME_ZONE(@NonNull Context context) {
		if (apiTimeZone == null) {
			apiTimeZone = context.getResources().getString(R.string.strategic_mapping_api_timezone);
		}
		return apiTimeZone;
	}

	private static final long STRATEGIC_MAPPING_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long STRATEGIC_MAPPING_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long STRATEGIC_MAPPING_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long STRATEGIC_MAPPING_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long STRATEGIC_MAPPING_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getStatusMaxValidityInMs() {
		return STRATEGIC_MAPPING_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return STRATEGIC_MAPPING_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return STRATEGIC_MAPPING_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return STRATEGIC_MAPPING_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return STRATEGIC_MAPPING_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Nullable
	@Override
	public POIStatus getCachedStatus(@NonNull Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "Trying to get cached schedule status w/o schedule filter '%s'! #ShouldNotHappen", statusFilter);
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (rts == null //
				|| TextUtils.isEmpty(rts.getStop().getCode()) //
				|| rts.getTrip().getId() < 0L //
				|| TextUtils.isEmpty(rts.getRoute().getShortName())) {
			MTLog.w(this, "Trying to get cached status w/o stop code OR trip id OR route short name '%s'! #ShouldNotHappen", rts);
			return null;
		}
		String uuid = getAgencyRouteStopTargetUUID(rts);
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, uuid);
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom tag
			if (rts.isDescentOnly()) {
				if (cachedStatus instanceof Schedule) {
					Schedule schedule = (Schedule) cachedStatus;
					schedule.setDescentOnly(true); // API doesn't know about "descent only"
				}
			}
		}
		return cachedStatus;
	}

	@NonNull
	private static String getAgencyRouteStopTargetUUID(@NonNull RouteTripStop rts) {
		return getAgencyRouteStopTargetUUID(rts.getAuthority(), rts.getRoute().getShortName(), rts.getTrip().getId(), rts.getStop().getCode());
	}

	@NonNull
	private static String getAgencyRouteStopTargetUUID(String agencyAuthority, String routeShortName, long tripId, String stopCode) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName, tripId, stopCode);
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
		return StrategicMappingDbHelper.T_STRATEGIC_MAPPING_STATUS;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	@Nullable
	@Override
	public POIStatus getNewStatus(@NonNull Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "Trying to get new schedule status w/o schedule filter '%s'! #ShouldNotHappen", statusFilter);
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (rts == null //
				|| TextUtils.isEmpty(rts.getStop().getCode()) //
				|| rts.getTrip().getId() < 0L //
				|| TextUtils.isEmpty(rts.getRoute().getShortName())) {
			MTLog.w(this, "Trying to get new status w/o stop code OR trip id OR route short name '%s'! #ShouldNotHappen", rts);
			return null;
		}
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	private static String getStopUrlString(@NonNull String apiUrl, @NonNull String stopCode) {
		return new StringBuilder() //
				.append(apiUrl) //
				.append("/Stop?term=") //
				.append(stopCode) //
				.toString();
	}

	private static String getPredictionDataUrlString(@NonNull String apiUrl, @NonNull String stopId) {
		return new StringBuilder() //
				.append(apiUrl) //
				.append("/PredictionData?stopid=") //
				.append(stopId) //
				.append("&shouldLog=false") //
				.toString();
	}

	private void loadRealTimeStatusFromWWW(@NonNull RouteTripStop rts) {
		Context context = requireContext();
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

	public static final String JSON_STOP_CODE = "StopCode";
	public static final String JSON_STOP_ID = "StopID";

	@Nullable
	private String parseStopIdAgencyJSON(String jsonString, @NonNull String stopCode) {
		try {
			JSONArray json = jsonString == null ? null : new JSONArray(jsonString);
			if (json != null && json.length() > 0) {
				for (int r = 0; r < json.length(); r++) {
					JSONObject jStop = json.getJSONObject(r);
					if (jStop != null && jStop.has(JSON_STOP_CODE)) {
						String jStopCode = jStop.getString(JSON_STOP_CODE);
						if (jStopCode.equalsIgnoreCase(stopCode)) {
							if (jStop.has(JSON_STOP_ID)) {
								int stopId = jStop.getInt(JSON_STOP_ID);
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
	public static final String JSON_SCHEDULE_TIME = "ScheduleTime";
	public static final String JSON_PREDICTION_TYPE = "PredictionType";
	public static final String JSON_SEQ_NO = "SeqNo";

	public static final String DIRECT_NAME_INBOUND = "Inbound";
	public static final String DIRECT_NAME_OUTBOUND = "Outbound";
	public static final String DIRECT_NAME_EAST = "East";
	public static final String DIRECT_NAME_EASTBOUND = "Eastbound";
	public static final String DIRECT_NAME_WESTBOUND = "Westbound";
	public static final String DIRECT_NAME_WEST = "West";
	public static final String DIRECT_NAME_NORTHBOUND = "Northbound";
	public static final String DIRECT_NAME_NORTH = "North";
	public static final String DIRECT_NAME_SOUTHBOUND = "Southbound";
	public static final String DIRECT_NAME_SOUTH = "South";
	public static final String DIRECT_NAME_CLOCKWISE = "Clockwise";
	public static final String DIRECT_NAME_COUNTERCLOCKWISE = "Counterclockwise";

	@NonNull
	protected Collection<POIStatus> parseAgencyJSON(@NonNull Context context, String jsonString, RouteTripStop rts, long newLastUpdateInMs) {
		return parseAgencyJSON(getTimeFormatter(context), parseAgencyJSON(jsonString), rts, newLastUpdateInMs);
	}

	@NonNull
	protected Collection<POIStatus> parseAgencyJSON(@NonNull ThreadSafeDateFormatter timeFormatter, Map<Group, List<Prediction>> allGroupPredictions, RouteTripStop rts, long newLastUpdateInMs) {
		ArrayList<POIStatus> poiStatuses = new ArrayList<>();
		try {
			if (allGroupPredictions != null) {
				if (allGroupPredictions.size() > 0) {
					Schedule newSchedule = new Schedule(getAgencyRouteStopTargetUUID(rts), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
							PROVIDER_PRECISION_IN_MS, false);
					boolean hasFirst = false;
					boolean hasOther = false;
					boolean hasCircleRoute = false;
					for (Map.Entry<Group, List<Prediction>> groupPrediction : allGroupPredictions.entrySet()) {
						Group group = groupPrediction.getKey();
						List<Prediction> predictions = groupPrediction.getValue();
						if (group == null
								|| predictions == null || predictions.isEmpty()
								|| TextUtils.isEmpty(group.routeCode)
								|| TextUtils.isEmpty(group.directName)) {
							MTLog.w(this, "Trying to parse incomplete Group '%s' ! #ShouldNotHappen", group);
							continue;
						}
						if (TextUtils.isEmpty(group.routeCode)) {
							MTLog.w(this, "Trying to parse Predictions w/o route code! #ShouldNotHappen");
							continue;
						}
						if (predictions == null || predictions.size() == 0) {
							MTLog.w(this, "Trying to parse empty Predictions! #ShouldNotHappen");
							continue;
						}
						if (TextUtils.isEmpty(group.directName)) {
							MTLog.w(this, "Trying to parse Predictions w/o direction name! #ShouldNotHappen");
							continue;
						}
						if (!group.routeCode.equalsIgnoreCase(rts.getRoute().getShortName())) {
							if (group.routeName == null || !group.routeName.equalsIgnoreCase(rts.getRoute().getShortName())) {
								continue;
							}
						}
						boolean circleRoute = false;
						boolean splittedRoute = false;
						boolean differentTrip = false;
						String tripId = String.valueOf(rts.getTrip().getId());
						if (DIRECT_NAME_INBOUND.equalsIgnoreCase(group.directName)) {
							if (!tripId.endsWith("00")) {
								differentTrip = true;
							}
						} else if (DIRECT_NAME_OUTBOUND.equalsIgnoreCase(group.directName)) {
							if (!tripId.endsWith("01") //
									&& !tripId.endsWith("010") //
									&& !tripId.endsWith("011")) {
								differentTrip = true;
							}
							if (tripId.endsWith("010") //
									|| tripId.endsWith("011")) {
								splittedRoute = true;
							}
						} else if (DIRECT_NAME_EAST.equalsIgnoreCase(group.directName) //
								|| DIRECT_NAME_EASTBOUND.equalsIgnoreCase(group.directName)) {
							if (!tripId.endsWith("01") //
									&& !tripId.endsWith("010") //
									&& !tripId.endsWith("011")) {
								differentTrip = true;
							}
							if (tripId.endsWith("010") //
									|| tripId.endsWith("011")) {
								splittedRoute = true;
							}
						} else if (DIRECT_NAME_WESTBOUND.equalsIgnoreCase(group.directName) //
								|| DIRECT_NAME_WEST.equalsIgnoreCase(group.directName)) {
							if (!tripId.endsWith("02") //
									& !tripId.endsWith("020") //
									&& !tripId.endsWith("021")) {
								differentTrip = true;
							}
							if (tripId.endsWith("020") //
									|| tripId.endsWith("021")) {
								splittedRoute = true;
							}
						} else if (DIRECT_NAME_NORTHBOUND.equalsIgnoreCase(group.directName) //
								|| DIRECT_NAME_NORTH.equalsIgnoreCase(group.directName)) {
							if (!tripId.endsWith("03") //
									&& !tripId.endsWith("030") //
									&& !tripId.endsWith("031")) {
								differentTrip = true;
							}
							if (tripId.endsWith("030") //
									|| tripId.endsWith("031")) {
								splittedRoute = true;
							}
						} else if (DIRECT_NAME_SOUTHBOUND.equalsIgnoreCase(group.directName) //
								|| DIRECT_NAME_SOUTH.equalsIgnoreCase(group.directName)) {
							if (!tripId.endsWith("04") //
									&& !tripId.endsWith("040") //
									&& !tripId.endsWith("041")) {
								differentTrip = true;
							}
							if (tripId.endsWith("040") //
									|| tripId.endsWith("041")) {
								splittedRoute = true;
							}
						} else if (DIRECT_NAME_CLOCKWISE.equalsIgnoreCase(group.directName)) {
							circleRoute = true;
							if (tripId.endsWith("060") //
									|| tripId.endsWith("061")) {
								splittedRoute = true;
							}
						} else if (DIRECT_NAME_COUNTERCLOCKWISE.equalsIgnoreCase(group.directName)) {
							circleRoute = true;
							if (tripId.endsWith("090") //
									|| tripId.endsWith("091")) {
								splittedRoute = true;
							}
						} else {
							MTLog.w(this, "Trying to parse Predictions with unpredictable direction name '%s'! #ShouldNotHappen", group.directName);
						}
						if (circleRoute || splittedRoute) {
							if (!hasCircleRoute) {
								hasCircleRoute = true;
							}
						}
						for (Prediction prediction : predictions) {
							if (prediction != null) {
								if (prediction.seqNo == 1) {
									if (!hasFirst) {
										hasFirst = true;
									}
								} else {
									if (!hasOther) {
										hasOther = true;
									}
								}
							}
							if (hasFirst && hasOther) {
								break;
							}
						}
						if (differentTrip) {
							continue;
						}
						boolean isFirstAndLastInCircle = false;
						if (circleRoute) {
							for (Prediction prediction : predictions) {
								if (prediction != null && prediction.seqNo != -1) {
									if (prediction.seqNo == 1) {
										isFirstAndLastInCircle = true;
										break;
									}
								}
							}
						}
						for (Prediction prediction : predictions) {
							boolean descentOnlyPrediction = false;
							if (prediction != null) {
								if (rts.isDescentOnly()) {
									if (prediction.seqNo == 1) {
										continue;
									} else {
										if (Constants.EXPORT_DESCENT_ONLY) {
											if (isFirstAndLastInCircle) {
												descentOnlyPrediction = true;
											}
										}
									}
								} else if (isFirstAndLastInCircle) { // AND NOT last (descent only) stop DO
									if (prediction.seqNo > 1) { // this prediction is for the last stop, not the first
										if (Constants.EXPORT_DESCENT_ONLY) {
											if (splittedRoute) {
												continue;
											} else {
												descentOnlyPrediction = true;
											}
										} else {
											continue;
										}
									}
								} else if (splittedRoute & hasFirst & hasOther) {
									if (Constants.EXPORT_DESCENT_ONLY) {
										if (prediction.seqNo > 1) { // this prediction is for the last stop, not the first
											continue;
										}
									}
								}
								String time = null;
								if (!TextUtils.isEmpty(prediction.predictTime)) {
									time = prediction.predictTime;
								}
								if (TextUtils.isEmpty(time)) {
									if (!TextUtils.isEmpty(prediction.scheduleTime)) {
										time = prediction.scheduleTime;
									}
								}
								Long t = timeFormatter.parseThreadSafe(time).getTime();
								boolean isRealTime = !"Scheduled".equalsIgnoreCase(prediction.predictionType);
								Schedule.Timestamp timestamp = new Schedule.Timestamp(TimeUtils.timeToTheTensSecondsMillis(t));
								if (Constants.EXPORT_DESCENT_ONLY) {
									if (descentOnlyPrediction) {
										timestamp.setHeadsign(Trip.HEADSIGN_TYPE_DESCENT_ONLY, null);
									}
								}
								newSchedule.addTimestampWithoutSort(timestamp);
							}
						}
					}
					if (Constants.EXPORT_DESCENT_ONLY) {
						if (rts.isDescentOnly()) {
							if (!hasCircleRoute) {
								newSchedule.setDescentOnlyTimestamps(true); // Strategic Mapping doesn't know about "descent only" for non-circle route
							}
							if (hasCircleRoute && hasFirst && hasOther) {
								newSchedule.setDescentOnlyTimestamps(true); // Strategic Mapping doesn't always know about "descent only" for [C]CW routes
							}
						}
					}
					newSchedule.sortTimestamps();
					poiStatuses.add(newSchedule);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", allGroupPredictions);
		}
		return poiStatuses;
	}

	@NonNull
	private Map<Group, List<Prediction>> parseAgencyJSON(String jsonString) {
		Map<Group, List<Prediction>> result = new HashMap<>();
		try {
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_GROUP_BY_PATTERN)) {
				JSONArray jGroups = json.getJSONArray(JSON_GROUP_BY_PATTERN);
				if (jGroups != null && jGroups.length() > 0) {
					for (int g = 0; g < jGroups.length(); g++) {
						JSONObject jGroup = jGroups.getJSONObject(g);
						List<Prediction> predictions = new ArrayList<>();
						JSONArray jPredictions = jGroup.optJSONArray(JSON_PREDICTIONS);
						if (jPredictions != null) {
							for (int p = 0; p < jPredictions.length(); p++) {
								JSONObject jPrediction = jPredictions.getJSONObject(p);
								predictions.add(new Prediction(
										jPrediction.optString(JSON_PREDICT_TIME),
										jPrediction.optString(JSON_SCHEDULE_TIME),
										jPrediction.optString(JSON_PREDICTION_TYPE),
										jPrediction.optInt(JSON_SEQ_NO, -1)
								));
							}
						}
						result.put(new Group(
								jGroup.getString(JSON_DIRECTION_NAME),
								jGroup.optString(JSON_ROUTE_NAME),
								jGroup.optString(JSON_ROUTE_CODE)
						), predictions);
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
		}
		return result;
	}

	public static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

	@Nullable
	private static ThreadSafeDateFormatter timeFormatter = null;

	@NonNull
	private ThreadSafeDateFormatter getTimeFormatter(@NonNull Context context) {
		if (timeFormatter == null) {
			timeFormatter = new ThreadSafeDateFormatter(TIME_FORMAT);
			timeFormatter.setTimeZone(getAPI_TZ(context));
		}
		return timeFormatter;
	}

	@Override
	public boolean onCreateMT() {
		ping(requireContext());
		return true;
	}

	@Override
	public void ping() {
		ping(requireContext());
	}

	public void ping(@NonNull Context context) {
		PackageManagerUtils.removeModuleLauncherIcon(context);
		getTimeFormatter(context); // force init before 1st usage
	}

	@Nullable
	private StrategicMappingDbHelper dbHelper = null;

	private static int currentDbVersion = -1;

	@NonNull
	private StrategicMappingDbHelper getProviderDBHelper(@NonNull Context context) {
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
	 * Override if multiple {@link StrategicMappingProvider} implementations in same app.
	 */
	@Deprecated
	public int getCurrentDbVersion() {
		return getCurrentDbVersion(requireContext());
	}

	/**
	 * Override if multiple {@link StrategicMappingProvider} implementations in same app.
	 */
	public int getCurrentDbVersion(@NonNull Context context) {
		return StrategicMappingDbHelper.getDbVersion(context);
	}

	/**
	 * Override if multiple {@link StrategicMappingProvider} implementations in same app.
	 */
	@NonNull
	public StrategicMappingDbHelper getNewDbHelper(@NonNull Context context) {
		return new StrategicMappingDbHelper(context.getApplicationContext());
	}

	@Deprecated
	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		return getURI_MATCHER(requireContext());
	}

	@NonNull
	public UriMatcher getURI_MATCHER(@NonNull Context context) {
		return getSTATIC_URI_MATCHER(context);
	}

	@Deprecated
	@NonNull
	@Override
	public Uri getAuthorityUri() {
		return getAuthorityUri(requireContext());
	}

	@NonNull
	public Uri getAuthorityUri(@NonNull Context context) {
		return getAUTHORITY_URI(context);
	}

	@Deprecated
	@NonNull
	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(requireContext());
	}

	@NonNull
	public SQLiteOpenHelper getDBHelper(@NonNull Context context) {
		return getProviderDBHelper(context);
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

	@Override
	public Uri insertMT(@NonNull Uri uri, @Nullable ContentValues values) {
		MTLog.w(this, "The insert method is not available.");
		return null;
	}

	public static class Group {

		String directName;
		String routeName;
		String routeCode;

		public Group(String directName, String routeName, String routeCode) {
			this.directName = directName;
			this.routeName = routeName;
			this.routeCode = routeCode;
		}

		@NonNull
		@Override
		public String toString() {
			return Group.class.getSimpleName() + "{" +
					"directName='" + directName + '\'' +
					", routeName='" + routeName + '\'' +
					", routeCode='" + routeCode + '\'' +
					'}';
		}
	}

	public static class Prediction {

		String predictTime;
		String scheduleTime;
		String predictionType;
		int seqNo;

		public Prediction(String predictTime, String scheduleTime, String predictionType, int seqNo) {
			this.predictTime = predictTime;
			this.scheduleTime = scheduleTime;
			this.predictionType = predictionType;
			this.seqNo = seqNo;
		}

		@NonNull
		@Override
		public String toString() {
			return Prediction.class.getSimpleName() + "{" +
					"predictTime='" + predictTime + '\'' +
					", scheduleTime='" + scheduleTime + '\'' +
					", predictionType='" + predictionType + '\'' +
					", seqNo=" + seqNo +
					'}';
		}
	}

	public static class StrategicMappingDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = StrategicMappingDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link StrategicMappingDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "strategic_mapping.db";

		public static final String T_STRATEGIC_MAPPING_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_STRATEGIC_MAPPING_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_STRATEGIC_MAPPING_STATUS).build();

		private static final String T_STRATEGIC_MAPPING_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STRATEGIC_MAPPING_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link StrategicMappingDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.strategic_mapping_db_version);
			}
			return dbVersion;
		}

		public StrategicMappingDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_STRATEGIC_MAPPING_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_STRATEGIC_MAPPING_STATUS_SQL_CREATE);
		}
	}
}
