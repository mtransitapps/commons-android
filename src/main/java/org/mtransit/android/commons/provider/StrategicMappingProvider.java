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
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@SuppressLint("Registered")
public class StrategicMappingProvider extends MTContentProvider implements StatusProviderContract {

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

	@Nullable
	private static String apiSearchTerm = null;

	/**
	 * Override if multiple {@link StrategicMappingProvider} implementations in same app.
	 */
	@NonNull
	private static String getAPI_SEARCH_TERM(@NonNull Context context) {
		if (apiSearchTerm == null) {
			apiSearchTerm = context.getResources().getString(R.string.strategic_mapping_api_search_term);
		}
		return apiSearchTerm;
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
		if (TextUtils.isEmpty(rts.getStop().getCode())
				|| rts.getTrip().getId() < 0L
				|| TextUtils.isEmpty(rts.getRoute().getShortName())) {
			MTLog.w(this, "Trying to get cached status w/o stop code OR trip id OR route short name '%s'! #ShouldNotHappen", rts);
			return null;
		}
		String uuid = getAgencyRouteStopTargetUUID(rts);
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, uuid);
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom tag
			if (rts.isNoPickup()) {
				if (cachedStatus instanceof Schedule) {
					Schedule schedule = (Schedule) cachedStatus;
					schedule.setNoPickup(true); // API doesn't know about "descent only"
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

	@NonNull
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
		if (TextUtils.isEmpty(rts.getStop().getCode())
				|| rts.getTrip().getId() < 0L
				|| TextUtils.isEmpty(rts.getRoute().getShortName())) {
			MTLog.w(this, "Trying to get new status w/o stop code OR trip id OR route short name '%s'! #ShouldNotHappen", rts);
			return null;
		}
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	@NonNull
	private static String getStopUrlString(@NonNull Context context, @NonNull String apiUrl, @NonNull String stopCode) {
		return apiUrl + //
				"/" + //
				getAPI_SEARCH_TERM(context) +
				"?term=" + //
				stopCode;
	}

	@NonNull
	private static String getPredictionDataUrlString(@NonNull String apiUrl, @NonNull String stopId) {
		return apiUrl + //
				"/PredictionData?stopid=" + //
				stopId + //
				"&shouldLog=false";
	}

	private void loadRealTimeStatusFromWWW(@NonNull RouteTripStop rts) {
		Context context = getContext();
		if (context == null) {
			MTLog.w(this, "Trying to real-time status w/o context! #ShouldNotHappen");
			return;
		}
		String apiUrl = getAPI_URL(context);
		// 1 - FIND STOP ID
		String stopId = loadStopIdFromWWW(context, apiUrl, rts.getStop().getCode());
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
				MTLog.d(this, "loadRealTimeStatusFromWWW() > jsonString: %s.", jsonString);
				Collection<POIStatus> statuses = parseAgencyJSON(context, jsonString, rts, newLastUpdateInMs);
				MTLog.i(this, "Loaded %d schedule status.", (statuses == null ? 0 : statuses.size()));
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
	private String loadStopIdFromWWW(@NonNull Context context, @NonNull String apiUrl, @NonNull String stopCode) {
		try {
			String urlString = getStopUrlString(context, apiUrl, stopCode);
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadStopIdFromWWW() > jsonString: %s.", jsonString);
				return parseStopIdAgencyJSON(jsonString, stopCode);
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

	private static final String JSON_STOP_CODE = "stopCode";
	private static final String JSON_STOP_CODE_OLD = "StopCode";
	private static final String JSON_STOP_ID = "stopID";
	private static final String JSON_STOP_ID_OLD = "StopID";

	@Nullable
	private String parseStopIdAgencyJSON(@Nullable String jsonString, @NonNull String stopCode) {
		try {
			JSONArray json = jsonString == null ? null : new JSONArray(jsonString);
			if (json != null && json.length() > 0) {
				for (int r = 0; r < json.length(); r++) {
					JSONObject jStop = json.getJSONObject(r);
					if (jStop != null) {
						if (jStop.has(JSON_STOP_CODE)) {
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
						if (jStop.has(JSON_STOP_CODE_OLD)) {
							String jStopCode = jStop.getString(JSON_STOP_CODE_OLD);
							if (jStopCode.equalsIgnoreCase(stopCode)) {
								if (jStop.has(JSON_STOP_ID_OLD)) {
									int stopId = jStop.getInt(JSON_STOP_ID_OLD);
									if (stopId > 0) {
										return String.valueOf(stopId);
									}
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

	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	private static final String JSON_ROUTE_CODE = "routeCode";
	private static final String JSON_PREDICTIONS = "predictions";
	private static final String JSON_PREDICTIONS_OLD = "Predictions";
	private static final String JSON_GROUP_BY_PATTERN = "grpByPtrn";
	private static final String JSON_DIRECTION_NAME = "directName";
	private static final String JSON_PREDICT_TIME = "predictTime";
	private static final String JSON_PREDICT_TIME_OLD = "PredictTime";
	private static final String JSON_ROUTE_NAME = "routeName";
	private static final String JSON_SCHEDULE_TIME = "scheduleTime";
	private static final String JSON_SCHEDULE_TIME_OLD = "ScheduleTime";
	private static final String JSON_PREDICTION_TYPE = "predictionType";
	private static final String JSON_PREDICTION_TYPE_OLD = "PredictionType";
	private static final String JSON_PREDICTION_TYPE_VALUE_PREDICTED = "Predicted";
	private static final String JSON_PREDICTION_TYPE_VALUE_PREDICTED_DELAYED = "PredictedDelayed";
	private static final String JSON_PREDICTION_TYPE_VALUE_SCHEDULED = "Scheduled";
	private static final String JSON_PREDICTION_TYPE_VALUE_TOMORROW = "Tomorrow";
	private static final String JSON_PREDICTION_TYPE_VALUE_SCHEDULED_AND_TOMORROW = "Scheduled&Tomorrow";
	private static final String JSON_SEQ_NO = "seqNo";
	private static final String JSON_SEQ_NO_OLD = "SeqNo";

	@Nullable
	private Collection<POIStatus> parseAgencyJSON(@NonNull Context context, String jsonString, @NonNull RouteTripStop rts, long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> poiStatuses = new ArrayList<>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_GROUP_BY_PATTERN)) {
				JSONArray jGroups = json.optJSONArray(JSON_GROUP_BY_PATTERN);
				if (jGroups != null && jGroups.length() > 0) {
					Schedule newSchedule = new Schedule(getAgencyRouteStopTargetUUID(rts), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
							PROVIDER_PRECISION_IN_MS, false);
					for (int g = 0; g < jGroups.length(); g++) {
						JSONObject jGroup = jGroups.getJSONObject(g);
						if (jGroup == null
								|| !(jGroup.has(JSON_PREDICTIONS) || jGroup.has(JSON_PREDICTIONS_OLD))
								|| !jGroup.has(JSON_ROUTE_CODE)) {
							MTLog.w(this, "Trying to parse incomplete Group '%s' ! #ShouldNotHappen", jGroup);
							continue;
						}
						String jRouteCode = jGroup.getString(JSON_ROUTE_CODE); // ex: 0
						String jRouteName = jGroup.optString(JSON_ROUTE_NAME); // ex: Abcd & Efgh
						if (TextUtils.isEmpty(jRouteCode)) {
							MTLog.w(this, "Trying to parse Predictions w/o route code! #ShouldNotHappen");
							continue;
						}
						JSONArray jPredictions = null;
						if (jGroup.has(JSON_PREDICTIONS)) {
							jPredictions = jGroup.getJSONArray(JSON_PREDICTIONS);
						} else if (jGroup.has(JSON_PREDICTIONS_OLD)) {
							jPredictions = jGroup.getJSONArray(JSON_PREDICTIONS_OLD);
						}
						if (jPredictions == null || jPredictions.length() == 0) {
							MTLog.w(this, "Trying to parse empty Predictions! #ShouldNotHappen");
							continue;
						}
						String jDirectName = jGroup.optString(JSON_DIRECTION_NAME); // ex: Outbound | Inbound | Clockwise
						if (TextUtils.isEmpty(jDirectName)) {
							MTLog.d(this, "Trying to parse Predictions w/o direction name.");
							continue;
						}
						if (!jRouteCode.equalsIgnoreCase(rts.getRoute().getShortName())) {
							if (!jRouteName.equalsIgnoreCase(rts.getRoute().getShortName())) {
								MTLog.d(this, "Trying to parse Predictions for other route ('%s' & '%s' != '%s')! #ShouldNotHappen", jRouteCode, jRouteName, rts.getRoute());
								continue;
							}
						}
						boolean circleRoute = false;
						final String tripId = String.valueOf(rts.getTrip().getId());
						if ("Inbound".equalsIgnoreCase(jDirectName)) {
							if (!tripId.endsWith("00")) {
								continue;
							}
						} else if ("Outbound".equalsIgnoreCase(jDirectName)) {
							//noinspection DuplicateExpressions
							if (!tripId.endsWith("01")
									&& !tripId.endsWith("010")
									&& !tripId.endsWith("011")) {
								continue;
							}
						} else if ("East".equalsIgnoreCase(jDirectName) //
								|| "Eastbound".equalsIgnoreCase(jDirectName)) {
							//noinspection DuplicateExpressions
							if (!tripId.endsWith("01")
									&& !tripId.endsWith("010")
									&& !tripId.endsWith("011")) {
								continue;
							}
						} else if ("Westbound".equalsIgnoreCase(jDirectName) //
								|| "West".equalsIgnoreCase(jDirectName)) {
							if (!tripId.endsWith("02")
									&& !tripId.endsWith("020")
									&& !tripId.endsWith("021")) {
								continue;
							}
						} else if ("Northbound".equalsIgnoreCase(jDirectName) //
								|| "North".equalsIgnoreCase(jDirectName)) {
							if (!tripId.endsWith("03")
									&& !tripId.endsWith("030")
									&& !tripId.endsWith("031")) {
								continue;
							}
						} else if ("Southbound".equalsIgnoreCase(jDirectName) //
								|| "South".equalsIgnoreCase(jDirectName)) {
							if (!tripId.endsWith("04")
									&& !tripId.endsWith("040")
									&& !tripId.endsWith("041")) {
								continue;
							}
						} else if ("Clockwise".equalsIgnoreCase(jDirectName)) {
							circleRoute = true;
						} else if ("Counterclockwise".equalsIgnoreCase(jDirectName)) {
							circleRoute = true;
						} else {
							MTLog.d(this, "Trying to parse Predictions with unpredictable direction name '%s'! #ShouldNotHappen", jDirectName);
						}
						boolean isFirstAndLastInCircle = false;
						if (circleRoute) {
							for (int p = 0; p < jPredictions.length(); p++) {
								JSONObject jPrediction = jPredictions.getJSONObject(p);
								if (jPrediction != null) {
									if (jPrediction.has(JSON_SEQ_NO)) {
										int jSeqNo = jPrediction.getInt(JSON_SEQ_NO);
										if (jSeqNo == 1) {
											isFirstAndLastInCircle = true;
											break;
										}
									} else if (jPrediction.has(JSON_SEQ_NO_OLD)) {
										int jSeqNo = jPrediction.getInt(JSON_SEQ_NO_OLD);
										if (jSeqNo == 1) {
											isFirstAndLastInCircle = true;
											break;
										}
									}
								}
							}
						}
						for (int p = 0; p < jPredictions.length(); p++) {
							JSONObject jPrediction = jPredictions.getJSONObject(p);
							if (jPrediction != null) {
								if (rts.isNoPickup()) {
									int jSeqNo = jPrediction.optInt(JSON_SEQ_NO, -1);
									if (jSeqNo == -1) {
										jSeqNo = jPrediction.optInt(JSON_SEQ_NO_OLD, -1);
									}
									if (jSeqNo == 1) {
										continue;
									}
								} else if (isFirstAndLastInCircle) {
									int jSeqNo = jPrediction.optInt(JSON_SEQ_NO, -1);
									if (jSeqNo == -1) {
										jSeqNo = jPrediction.optInt(JSON_SEQ_NO_OLD, -1);
									}
									if (jSeqNo > 1) {
										if (!rts.isNoPickup()) {
											continue;
										}
									}
								}
								String time = null;
								if (jPrediction.has(JSON_PREDICT_TIME)) {
									time = jPrediction.getString(JSON_PREDICT_TIME);
								} else if (jPrediction.has(JSON_PREDICT_TIME_OLD)) {
									time = jPrediction.getString(JSON_PREDICT_TIME_OLD);
								}
								if (time == null || time.isEmpty()) {
									if (jPrediction.has(JSON_SCHEDULE_TIME)) {
										time = jPrediction.getString(JSON_SCHEDULE_TIME);
									} else if (jPrediction.has(JSON_SCHEDULE_TIME_OLD)) {
										time = jPrediction.getString(JSON_SCHEDULE_TIME_OLD);
									}
								}
								if (time == null) {
									MTLog.d(this, "SKIP prediction w/o time! (%s)", jPrediction);
									continue;
								}
								final Date date = getTimeFormatter(context).parseThreadSafe(time);
								if (date == null) {
									MTLog.d(this, "SKIP prediction w/ unreadable time! (%s)", jPrediction);
									continue;
								}
								final long t = date.getTime();
								String jPredictionType = jPrediction.optString(JSON_PREDICTION_TYPE); // ? VehicleAtStop, Predicted, Scheduled, PredictedDelayed
								if (jPredictionType.isEmpty()) {
									jPredictionType = jPrediction.optString(JSON_PREDICTION_TYPE_OLD);
								}
								Boolean isRealTime;
								if (JSON_PREDICTION_TYPE_VALUE_PREDICTED.equalsIgnoreCase(jPredictionType)) {
									isRealTime = true;
								} else if (JSON_PREDICTION_TYPE_VALUE_PREDICTED_DELAYED.equalsIgnoreCase(jPredictionType)) {
									isRealTime = true; // old real-time
								} else if (JSON_PREDICTION_TYPE_VALUE_SCHEDULED.equalsIgnoreCase(jPredictionType)) {
									isRealTime = false;
								} else if (JSON_PREDICTION_TYPE_VALUE_TOMORROW.equalsIgnoreCase(jPredictionType)) {
									isRealTime = false;
								} else if (JSON_PREDICTION_TYPE_VALUE_SCHEDULED_AND_TOMORROW.equalsIgnoreCase(jPredictionType)) {
									isRealTime = false;
								} else {
									MTLog.w(this, "Unexpected prediction type '%s'!", jPredictionType);
									isRealTime = null; // not scheduled
								}
								Schedule.Timestamp timestamp = new Schedule.Timestamp(TimeUtils.timeToTheTensSecondsMillis(t));
								timestamp.setRealTime(isRealTime);
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

	@Nullable
	private static ThreadSafeDateFormatter timeFormatter = null;

	@NonNull
	private ThreadSafeDateFormatter getTimeFormatter(@NonNull Context context) {
		if (timeFormatter == null) {
			timeFormatter = new ThreadSafeDateFormatter("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
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
		getTimeFormatter(context); // force init before 1st usage
	}

	@Nullable
	private StrategicMappingDbHelper dbHelper = null;

	private static int currentDbVersion = -1;

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
		if (getContext() == null) {
			MTLog.w(this, "Trying to read current DB version w/o context! #ShouldNotHappen");
			return -1;
		}
		return getCurrentDbVersion(getContext());
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
		//noinspection ConstantConditions // TODO requireContext()
		return getURI_MATCHER(getContext());
	}

	@NonNull
	public UriMatcher getURI_MATCHER(@NonNull Context context) {
		return getSTATIC_URI_MATCHER(context);
	}

	@Deprecated
	@NonNull
	@Override
	public Uri getAuthorityUri() {
		//noinspection ConstantConditions // TODO requireContext()
		return getAuthorityUri(getContext());
	}

	@NonNull
	public Uri getAuthorityUri(@NonNull Context context) {
		return getAUTHORITY_URI(context);
	}

	@NonNull
	private SQLiteOpenHelper getDBHelper() {
		//noinspection ConstantConditions // TODO requireContext()
		return getDBHelper(getContext());
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

		static final String T_STRATEGIC_MAPPING_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

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

		StrategicMappingDbHelper(@NonNull Context context) {
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
