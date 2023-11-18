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
import org.mtransit.android.commons.BuildConfig;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Accessibility;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.provider.CaLTCOnlineProvider.JBusTimes.JResult.JRealTimeResult;
import org.mtransit.android.commons.provider.CaLTCOnlineProvider.JBusTimes.JResult.JStopTimeResult;
import org.mtransit.android.commons.provider.CaLTCOnlineProvider.JBusTimes.JResult.JStopTimeResult.JStopTime;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.FeatureFlags;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

// Nov 15, 2020: DOES NOT WORK because Real-Time API stop IDs do not match with GTFS static (DISABLED)
@SuppressLint("Registered")
public class CaLTCOnlineProvider extends MTContentProvider implements StatusProviderContract {

	private static final String LOG_TAG = CaLTCOnlineProvider.class.getSimpleName();

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
	 * Override if multiple {@link CaLTCOnlineProvider} implementations in same app.
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
	 * Override if multiple {@link CaLTCOnlineProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.ca_ltconline_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link CaLTCOnlineProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static final long WEB_WATCH_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long WEB_WATCH_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long WEB_WATCH_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long WEB_WATCH_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long WEB_WATCH_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

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
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		String uuid = getAgencyRouteStopTargetUUID(rts);
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, uuid);
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom provider tags
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
		return getAgencyRouteStopTargetUUID(
				rts.getAuthority(),
				getAgencyRouteId(rts),
				getAgencyTripId(rts),
				getAgencyStopId(rts)
		);
	}

	@NonNull
	protected static String getAgencyRouteStopTargetUUID(@NonNull String agencyAuthority,
														 @NonNull String routeShortName,
														 @Nullable String optTripHeaSignValue,
														 @NonNull String stopId) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName, optTripHeaSignValue, stopId);
	}

	@NonNull
	private static String getAgencyRouteId(@NonNull RouteTripStop rts) {
		return rts.getRoute().getShortName();
	}

	private static final String CA_LONDON_TRANSIT_BUS = BuildConfig.DEBUG ?
			"org.mtransit.android.debug.ca_london_transit_bus.gtfs" :
			"org.mtransit.android.ca_london_transit_bus.gtfs";

	@NonNull
	private static String getAgencyTripId(@NonNull RouteTripStop rts) {
		if (rts.getTrip().getHeadsignType() == Trip.HEADSIGN_TYPE_DIRECTION) {
			return rts.getTrip().getHeadsignValue(); // E | W | N | S
		} else if (rts.getTrip().getHeadsignType() == Trip.HEADSIGN_TYPE_STRING) {
			if (CA_LONDON_TRANSIT_BUS.equals(rts.getAuthority())) {
				String tripIdS = String.valueOf(rts.getTrip().getId());
				if (tripIdS.endsWith("010")) {
					return LTC_CW;
				} else if (tripIdS.endsWith("011")) {
					return LTC_CCW;
				}
				if (tripIdS.endsWith("0101")) {
					return LTC_HURON_AND_BARKER;
				} else if (tripIdS.endsWith("0102")) {
					return LTC_WESTERN;
				}
				if (tripIdS.endsWith("01")) {
					return Trip.HEADING_EAST;
				} else if (tripIdS.endsWith("02")) {
					return Trip.HEADING_NORTH;
				} else if (tripIdS.endsWith("03")) {
					return Trip.HEADING_SOUTH;
				} else if (tripIdS.endsWith("04")) {
					return Trip.HEADING_WEST;
				}
			}
		}
		MTLog.w(LOG_TAG, "Unsupported agency trip filtering for '%s'.", rts);
		return StringUtils.EMPTY; // DO NOT FILTER BY TRIP
	}

	private static String getAgencyStopId(@NonNull RouteTripStop rts) {
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

	@NonNull
	@Override
	public String getStatusDbTableName() {
		return CaLTCOnlineDbHelper.T_WEB_WATCH_STATUS;
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
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	private static final String REAL_TIME_URL = "https://realtime.londontransit.ca/InfoWeb";

	private void loadRealTimeStatusFromWWW(@NonNull RouteTripStop rts) {
		try {
			String urlString = REAL_TIME_URL;
			MTLog.i(this, "Loading from '%s' for '%s'...", urlString, rts.getStop().getId());
			String jsonPostParams = getJSONPostParameters(rts);
			if (TextUtils.isEmpty(jsonPostParams)) {
				MTLog.w(this, "loadPredictionsFromWWW() > skip (invalid JSON post parameters!)");
				return;
			}
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			HttpsURLConnection httpUrlConnection = (HttpsURLConnection) urlc;
			try {
				httpUrlConnection.setDoOutput(true);
				httpUrlConnection.setRequestMethod("POST");
				httpUrlConnection.addRequestProperty("Content-Type", "application/json");
				OutputStream os = httpUrlConnection.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, FileUtils.getUTF8()));
				writer.write(jsonPostParams);
				writer.flush();
				writer.close();
				os.close();
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(httpUrlConnection.getInputStream());
				JBusTimes jBusTimes = parseAgencyJSONBusTimes(jsonString);
				long beginningOfTodayInMs = getNewBeginningOfTodayCal().getTimeInMillis();
				Collection<POIStatus> statuses = parseAgencyJSON(jBusTimes, rts, newLastUpdateInMs, beginningOfTodayInMs);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(getAgencyRouteStopTargetUUID(rts)));
				for (POIStatus status : statuses) {
					StatusProvider.cacheStatusS(this, status);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while posting query!");
			} finally {
				httpUrlConnection.disconnect();
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

	private static final String JSON_ENABLED = "1";
	private static final String JSON_CLIENT = "Client";
	private static final String JSON_GET_STOP_TIMES = "GetStopTimes";
	private static final String JSON_GET_STOP_TRIP_INFO = "GetStopTripInfo";
	private static final String JSON_RADIUS = "Radius";
	private static final String JSON_SUPPRESS_LINES_UNLOAD_ONLY = "SuppressLinesUnloadOnly";
	private static final String JSON_LINES_REQUEST = "LinesRequest";
	private static final String JSON_CLIENT_MOBILE_WEB = "MobileWeb";
	private static final String JSON_GET_STOP_TIMES_ENABLED = JSON_ENABLED;
	private static final String JSON_GET_STOP_TRIP_INFO_ENABLED = JSON_ENABLED;
	private static final String JSON_RADIUS_NONE = "0";
	private static final String JSON_SUPPRESS_LINES_UNLOAD_ONLY_ENABLED = JSON_ENABLED;
	private static final String JSON_VERSION = "version";
	private static final String JSON_METHOD = "method";
	private static final String JSON_PARAMS = "params";
	private static final String JSON_STOP_ID = "StopId";
	private static final String JSON_NUM_STOP_TIMES = "NumStopTimes";
	private static final String JSON_VERSION_1_1 = "1.1";
	private static final String JSON_METHOD_GET_BUS_TIMES = "GetBusTimes";
	private static final String JSON_NUM_STOP_TIMES_COUNT = "200";
	private static final String JSON_RESULT = "result";
	private static final String JSON_STOP_TIME_RESULT = "StopTimeResult";
	private static final String JSON_STOP_TIMES = "StopTimes";
	private static final String JSON_TRIP_ID = "TripId";
	private static final String JSON_IGNORE_ADHERENCE = "IgnoreAdherence";
	private static final String JSON_DESTINATION_SIGN = "DestinationSign";
	private static final String JSON_DIRECTION_NAME = "DirectionName";
	private static final String JSON_REAL_TIME_RESULTS = "RealTimeResults";
	private static final String JSON_REAL_TIME = "RealTime";
	private static final String JSON_E_TIME = "ETime";
	private static final String JSON_LINES = "Lines";
	private static final String JSON_LINE_DIR_ID = "LineDirId";
	private static final String JSON_LINE_ABBR = "LineAbbr";

	@Nullable
	private static String getJSONPostParameters(@NonNull RouteTripStop rts) {
		try {
			JSONObject json = new JSONObject();
			json.put(JSON_VERSION, JSON_VERSION_1_1);
			json.put(JSON_METHOD, JSON_METHOD_GET_BUS_TIMES);
			JSONObject jParams = new JSONObject();
			JSONObject jLinesRequest = new JSONObject();
			jLinesRequest.put(JSON_CLIENT, JSON_CLIENT_MOBILE_WEB);
			jLinesRequest.put(JSON_GET_STOP_TIMES, JSON_GET_STOP_TIMES_ENABLED);
			jLinesRequest.put(JSON_GET_STOP_TRIP_INFO, JSON_GET_STOP_TRIP_INFO_ENABLED);
			jLinesRequest.put(JSON_NUM_STOP_TIMES, JSON_NUM_STOP_TIMES_COUNT);
			jLinesRequest.put(JSON_RADIUS, JSON_RADIUS_NONE);
			jLinesRequest.put(JSON_STOP_ID, getAgencyStopId(rts));
			jLinesRequest.put(JSON_SUPPRESS_LINES_UNLOAD_ONLY, JSON_SUPPRESS_LINES_UNLOAD_ONLY_ENABLED);
			jParams.put(JSON_LINES_REQUEST, jLinesRequest);
			json.put(JSON_PARAMS, jParams);
			return json.toString();
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while creating JSON POST parameters for '%s'!", rts);
			return null;
		}
	}

	@NonNull
	private JBusTimes parseAgencyJSONBusTimes(@Nullable String jsonString) {
		List<JBusTimes.JResult> results = new ArrayList<>();
		try {
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_RESULT)) {
				JSONArray jResults = json.getJSONArray(JSON_RESULT);
				for (int r = 0; r < jResults.length(); r++) {
					JSONObject jResult = jResults.getJSONObject(r);
					results.add(new JBusTimes.JResult(
							parseAgencyJSONBusTimesRealTimeResults(jResult),
							parseAgencyJSONBusTimesStopTimesResults(jResult)
					));
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
		}
		return new JBusTimes(results);
	}

	private List<JStopTimeResult> parseAgencyJSONBusTimesStopTimesResults(@Nullable JSONObject jResult) {
		List<JStopTimeResult> stopTimeResults = new ArrayList<>();
		try {
			if (jResult != null && jResult.has(JSON_STOP_TIME_RESULT)) {
				JSONArray jStopTimeResults = jResult.getJSONArray(JSON_STOP_TIME_RESULT);
				for (int str = 0; str < jStopTimeResults.length(); str++) {
					JSONObject jStopTimeResult = jStopTimeResults.getJSONObject(str);
					stopTimeResults.add(new JStopTimeResult(
							parseAgencyJSONBusTimesLines(jStopTimeResult),
							parseAgencyJSONBusTimesStopTimes(jStopTimeResult)
					));
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jResult);
		}
		return stopTimeResults;
	}

	@NonNull
	private List<JStopTimeResult.JLine> parseAgencyJSONBusTimesLines(@Nullable JSONObject jStopTimeResult) {
		List<JStopTimeResult.JLine> lines = new ArrayList<>();
		try {
			if (jStopTimeResult != null && jStopTimeResult.has(JSON_LINES)) {
				JSONArray jLines = jStopTimeResult.getJSONArray(JSON_LINES);
				for (int l = 0; l < jLines.length(); l++) {
					JSONObject jLine = jLines.getJSONObject(l);
					JStopTimeResult.JLine line = parseAgencyJSONBusTimesLine(jLine);
					if (line != null) {
						lines.add(line);
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jStopTimeResult);
		}
		return lines;
	}

	@Nullable
	private JStopTimeResult.JLine parseAgencyJSONBusTimesLine(@Nullable JSONObject jLine) {
		try {
			if (jLine != null) {
				return new JStopTimeResult.JLine(
						jLine.getString(JSON_DIRECTION_NAME),
						jLine.getString(JSON_LINE_ABBR),
						jLine.getInt(JSON_LINE_DIR_ID),
						jLine.getInt(JSON_STOP_ID)
				);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jLine);
		}
		return null;
	}

	@NonNull
	private List<JStopTime> parseAgencyJSONBusTimesStopTimes(@Nullable JSONObject jStopTimeResult) {
		List<JStopTime> stopTimes = new ArrayList<>();
		try {
			if (jStopTimeResult != null && jStopTimeResult.has(JSON_STOP_TIMES)) {
				JSONArray jStopTimes = jStopTimeResult.getJSONArray(JSON_STOP_TIMES);
				for (int st = 0; st < jStopTimes.length(); st++) {
					JSONObject jStopTime = jStopTimes.getJSONObject(st);
					JStopTime stopTime = parseAgencyJSONBusTimesStopTime(jStopTime);
					if (stopTime != null) {
						stopTimes.add(stopTime);
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jStopTimeResult);
		}
		return stopTimes;
	}

	@Nullable
	private JStopTime parseAgencyJSONBusTimesStopTime(@Nullable JSONObject jStopTime) {
		try {
			if (jStopTime != null) {
				return new JStopTime(
						jStopTime.getString(JSON_DESTINATION_SIGN),
						jStopTime.getInt(JSON_E_TIME),
						jStopTime.getInt(JSON_LINE_DIR_ID),
						jStopTime.getString(JSON_STOP_ID),
						jStopTime.getInt(JSON_TRIP_ID)
				);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jStopTime);
		}
		return null;
	}

	@NonNull
	private List<JRealTimeResult> parseAgencyJSONBusTimesRealTimeResults(@Nullable JSONObject jResult) {
		List<JRealTimeResult> realTimeResults = new ArrayList<>();
		try {
			if (jResult != null && jResult.has(JSON_REAL_TIME_RESULTS)) {
				JSONArray jRealTimeResults = jResult.getJSONArray(JSON_REAL_TIME_RESULTS);
				for (int rt = 0; rt < jRealTimeResults.length(); rt++) {
					JSONObject jRealTimeResult = jRealTimeResults.getJSONObject(rt);
					JRealTimeResult realTimeResult = parseAgencyJSONBusTimesRealTimeResult(jRealTimeResult);
					if (realTimeResult != null) {
						realTimeResults.add(realTimeResult);
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jResult);
		}
		return realTimeResults;
	}

	@Nullable
	private JRealTimeResult parseAgencyJSONBusTimesRealTimeResult(@Nullable JSONObject jRealTimeResult) {
		try {
			if (jRealTimeResult != null) {
				return new JRealTimeResult(
						jRealTimeResult.getInt(JSON_E_TIME),
						jRealTimeResult.getInt(JSON_LINE_DIR_ID),
						jRealTimeResult.getInt(JSON_REAL_TIME),
						jRealTimeResult.getInt(JSON_STOP_ID),
						jRealTimeResult.getInt(JSON_TRIP_ID),
						jRealTimeResult.optBoolean(JSON_IGNORE_ADHERENCE, true) // true == not real-time
				);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jRealTimeResult);
		}
		return null;
	}

	private static final TimeZone LONDON_TZ = TimeZone.getTimeZone("America/Toronto");

	@NonNull
	private Calendar getNewBeginningOfTodayCal() {
		Calendar beginningOfTodayCal = Calendar.getInstance(LONDON_TZ);
		beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, 0);
		beginningOfTodayCal.set(Calendar.MINUTE, 0);
		beginningOfTodayCal.set(Calendar.SECOND, 0);
		beginningOfTodayCal.set(Calendar.MILLISECOND, 0);
		return beginningOfTodayCal;
	}

	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	@NonNull
	protected List<POIStatus> parseAgencyJSON(@NonNull JBusTimes jBusTimes, @NonNull RouteTripStop rts, long newLastUpdateInMs, long beginningOfTodayInMs) {
		List<POIStatus> result = new ArrayList<>();
		try {
			if (jBusTimes.hasResults()) {
				List<JBusTimes.JResult> jResults = jBusTimes.getResults();
				if (jResults != null && !jResults.isEmpty()) {
					for (JBusTimes.JResult jResult : jResults) {
						@SuppressLint("UseSparseArrays")
						Map<Integer, String> lineDirIdTargetUUIDS = new HashMap<>();
						@SuppressLint("UseSparseArrays")
						Map<Integer, List<JStopTime>> lineDirIdStopTimes = new HashMap<>();
						@SuppressLint("UseSparseArrays")
						Map<Integer, List<JRealTimeResult>> lineDirIdRealTimeResults = new HashMap<>();
						if (jResult != null && jResult.hasStopTimeResults()) {
							List<JStopTimeResult> jStopTimesResults = jResult.getStopTimeResults();
							if (jStopTimesResults != null && !jStopTimesResults.isEmpty()) {
								for (JStopTimeResult jStopTimeResult : jStopTimesResults) {
									// LINES
									if (jStopTimeResult != null && jStopTimeResult.hasLines()) {
										List<JStopTimeResult.JLine> jLines = jStopTimeResult.getLines();
										if (jLines != null && !jLines.isEmpty()) {
											for (JStopTimeResult.JLine jLine : jLines) {
												if (jLine.hasLineDirId()) {
													lineDirIdTargetUUIDS.put(
															jLine.getLineDirId(),
															getAgencyRouteStopTargetUUID(
																	rts.getAuthority(),
																	getRouteShortName(jLine),
																	getTripHeadSign(jLine),
																	jLine.getStopIdS()
															)
													);
												}
											}
										}
									}
									// STOP TIMES (STATIC)
									if (jStopTimeResult != null && jStopTimeResult.hasStopTimes()) {
										List<JStopTime> jStopTimes = jStopTimeResult.getStopTimes();
										if (jStopTimes != null && !jStopTimes.isEmpty()) {
											for (JStopTime jStopTime : jStopTimes) {
												if (jStopTime.hasLineDirId()) {
													List<JStopTime> lineDirIdStopTime = lineDirIdStopTimes.get(jStopTime.getLineDirId());
													if (lineDirIdStopTime == null) {
														lineDirIdStopTime = new ArrayList<>();
													}
													lineDirIdStopTime.add(jStopTime);
													lineDirIdStopTimes.put(jStopTime.getLineDirId(), lineDirIdStopTime);
												}
											}
										}

									}
								}
							}
						}
						// REAL TIME (SCHEDULE)
						if (jResult != null && jResult.hasRealTimeResults()) {
							List<JRealTimeResult> jRealTimeResults = jResult.getRealTimeResults();
							if (jRealTimeResults != null && !jRealTimeResults.isEmpty()) {
								for (JRealTimeResult jRealTimeResult : jRealTimeResults) {
									if (jRealTimeResult != null && jRealTimeResult.hasRealTime()) {
										if (jRealTimeResult.hasLineDirId()) {
											lineDirIdRealTimeResults.put(
													jRealTimeResult.getLineDirId(),
													jRealTimeResults
											);
										}
									}
								}
							}
						}
						// MERGE
						for (Map.Entry<Integer, String> lineDirIdTargetUUID : lineDirIdTargetUUIDS.entrySet()) {
							int lineDirId = lineDirIdTargetUUID.getKey();
							String targetUUID = lineDirIdTargetUUID.getValue();
							Schedule newSchedule = new Schedule(targetUUID, newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
									PROVIDER_PRECISION_IN_MS, false);
							List<JStopTime> stopTimes = lineDirIdStopTimes.get(lineDirId);
							List<JRealTimeResult> realTimeResults = lineDirIdRealTimeResults.get(lineDirId);
							if (stopTimes != null) {
								for (JStopTime stopTime : stopTimes) {
									int eTime = stopTime.getETime();
									JRealTimeResult realTime = findRealTime(stopTime, realTimeResults);
									Boolean isRealTime = null;
									if (realTime != null) {
										eTime = realTime.getRealTime();
										if (realTime.getIgnoreAdherence() != null) {
											isRealTime = !realTime.getIgnoreAdherence();
										}
									}
									long t = beginningOfTodayInMs + TimeUnit.SECONDS.toMillis(eTime);
									Schedule.Timestamp timestamp = new Schedule.Timestamp(TimeUtils.timeToTheTensSecondsMillis(t));
									String destinationSign = stopTime.getDestinationSign();
									if (!TextUtils.isEmpty(destinationSign)) {
										destinationSign = cleanTripHeadSign(destinationSign);
										timestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, destinationSign);
									}
									if (isRealTime != null) {
										timestamp.setRealTime(isRealTime);
									}
									if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
										timestamp.setAccessible(Accessibility.UNKNOWN); // no info available on https://realtime.londontransit.ca/
									}
									newSchedule.addTimestampWithoutSort(timestamp);
								}
							}
							newSchedule.sortTimestamps();
							result.add(newSchedule);
						}
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing '%s'!", jBusTimes);
		}
		return result;
	}

	@Nullable
	private JRealTimeResult findRealTime(@NonNull JStopTime stopTime,
										 @Nullable List<JRealTimeResult> realTimeResults) {
		if (realTimeResults != null) {
			for (JRealTimeResult realTimeResult : realTimeResults) {
				if (realTimeResult.getLineDirId() != stopTime.getLineDirId()) {
					continue; // different line
				}
				if (realTimeResult.getTripId() != stopTime.getTripId()) {
					continue; // different trip
				}
				if (realTimeResult.getETime() != stopTime.getETime()) {
					continue; // different scheduled time
				}
				return realTimeResult;
			}
		}
		MTLog.d(this, "No real-time for '%s'", stopTime);
		return null;
	}

	private static final String EASTBOUND = "EASTBOUND";
	private static final String WESTBOUND = "WESTBOUND";
	private static final String NORTHBOUND = "NORTHBOUND";
	private static final String SOUTHBOUND = "SOUTHBOUND";

	private static final String LTC_CW = "10";
	private static final String LTC_CCW = "11";

	private static final String LTC_HURON_AND_BARKER = "101";
	private static final String LTC_WESTERN = "102";

	@NonNull
	private String getTripHeadSign(@NonNull JStopTimeResult.JLine jLine) {
		String jDirectionName = jLine.getDirectionName().trim();
		if (EASTBOUND.equalsIgnoreCase(jDirectionName)) {
			return Trip.HEADING_EAST;
		} else if (WESTBOUND.equalsIgnoreCase(jDirectionName)) {
			return Trip.HEADING_WEST;
		} else if (NORTHBOUND.equalsIgnoreCase(jDirectionName)) {
			return Trip.HEADING_NORTH;
		} else if (SOUTHBOUND.equalsIgnoreCase(jDirectionName)) {
			return Trip.HEADING_SOUTH;
		}
		if ("CLOCKWISE".equalsIgnoreCase(jDirectionName)) {
			return LTC_CW;
		} else if ("COUNTER-CLKWISE".equalsIgnoreCase(jDirectionName)) {
			return LTC_CCW;
		}
		if ("WESTERN".equalsIgnoreCase(jDirectionName)) {
			return LTC_WESTERN;
		} else if ("HURON & BARKER".equalsIgnoreCase(jDirectionName)) {
			return LTC_HURON_AND_BARKER;
		}
		MTLog.w(LOG_TAG, "Unsupported agency line direction for '%s'.", jLine);
		return StringUtils.EMPTY;
	}

	@NonNull
	private String getRouteShortName(@NonNull JStopTimeResult.JLine jLine) {
		return String.valueOf(Integer.parseInt(jLine.getLineAbbr())); // remove leading 0
	}

	private String cleanTripHeadSign(String tripHeadSign) {
		try {
			tripHeadSign = CleanUtils.CLEAN_AT.matcher(tripHeadSign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
			tripHeadSign = CleanUtils.CLEAN_AND.matcher(tripHeadSign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
			tripHeadSign = CleanUtils.cleanNumbers(tripHeadSign);
			tripHeadSign = CleanUtils.cleanStreetTypes(tripHeadSign);
			tripHeadSign = CleanUtils.cleanLabel(tripHeadSign);
			return tripHeadSign;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadSign);
			return tripHeadSign;
		}
	}

	@MainThread
	@Override
	public boolean onCreateMT() {
		ping();
		return true;
	}

	@Override
	public void ping() {
		// DO NOTHING
	}

	@Nullable
	private CaLTCOnlineDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private CaLTCOnlineDbHelper getDBHelper(@NonNull Context context) {
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
		return CaLTCOnlineDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link CaLTCOnlineProvider} implementations in same app.
	 */
	@NonNull
	public CaLTCOnlineDbHelper getNewDbHelper(@NonNull Context context) {
		return new CaLTCOnlineDbHelper(context.getApplicationContext());
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

	public static class CaLTCOnlineDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = CaLTCOnlineDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link CaLTCOnlineDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "caltconline.db";

		static final String T_WEB_WATCH_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_WEB_WATCH_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_WEB_WATCH_STATUS).build();

		private static final String T_WEB_WATCH_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_WEB_WATCH_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link CaLTCOnlineDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.ca_ltconline_db_version);
			}
			return dbVersion;
		}

		CaLTCOnlineDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_WEB_WATCH_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_WEB_WATCH_STATUS_SQL_CREATE);
		}
	}

	protected static class JBusTimes {

		private final List<JResult> results;

		JBusTimes(List<JResult> results) {
			this.results = results;
		}

		public List<JResult> getResults() {
			return results;
		}

		boolean hasResults() {
			return this.results != null && !this.results.isEmpty();
		}

		@NonNull
		@Override
		public String toString() {
			return JBusTimes.class.getSimpleName() + "{" +
					"results=" + results +
					'}';
		}

		protected static class JResult {
			private final List<JRealTimeResult> realTimeResults;
			private final List<JStopTimeResult> stopTimeResults;

			JResult(List<JRealTimeResult> realTimeResults, List<JStopTimeResult> stopTimeResults) {
				this.realTimeResults = realTimeResults;
				this.stopTimeResults = stopTimeResults;
			}

			List<JStopTimeResult> getStopTimeResults() {
				return stopTimeResults;
			}

			boolean hasStopTimeResults() {
				return this.stopTimeResults != null && !this.stopTimeResults.isEmpty();
			}

			List<JRealTimeResult> getRealTimeResults() {
				return realTimeResults;
			}

			boolean hasRealTimeResults() {
				return this.realTimeResults != null && !this.realTimeResults.isEmpty();
			}

			@NonNull
			@Override
			public String toString() {
				return JResult.class.getSimpleName() + "{" +
						"realTimeResults=" + realTimeResults +
						", stopTimeResults=" + stopTimeResults +
						'}';
			}

			protected static class JRealTimeResult {
				private final int eTime;
				private final int lineDirId;
				private final int realTime;
				private final int stopId;
				private final int tripId;
				private final Boolean ignoreAdherence;

				JRealTimeResult(int eTime, int lineDirId, int realTime, int stopId, int tripId, Boolean ignoreAdherence) {
					this.eTime = eTime;
					this.lineDirId = lineDirId;
					this.realTime = realTime;
					this.stopId = stopId;
					this.tripId = tripId;
					this.ignoreAdherence = ignoreAdherence;
				}

				int getETime() {
					return eTime;
				}

				boolean hasLineDirId() {
					return this.lineDirId > 0;
				}

				int getLineDirId() {
					return lineDirId;
				}

				int getRealTime() {
					return realTime;
				}

				boolean hasRealTime() {
					return this.realTime >= 0;
				}

				int getTripId() {
					return tripId;
				}

				Boolean getIgnoreAdherence() {
					return ignoreAdherence;
				}

				@NonNull
				@Override
				public String toString() {
					return JRealTimeResult.class.getSimpleName() + "{" +
							", eTime=" + eTime +
							", lineDirId=" + lineDirId +
							", realTime=" + realTime +
							", stopId=" + stopId +
							", tripId=" + tripId +
							", ignoreAdherence=" + ignoreAdherence +
							'}';
				}
			}

			protected static class JStopTimeResult {
				private final List<JLine> lines;
				private final List<JStopTime> stopTimes;

				JStopTimeResult(List<JLine> lines, List<JStopTime> stopTimes) {
					this.lines = lines;
					this.stopTimes = stopTimes;
				}

				List<JLine> getLines() {
					return lines;
				}

				boolean hasLines() {
					return this.lines != null && !this.lines.isEmpty();
				}

				List<JStopTime> getStopTimes() {
					return stopTimes;
				}

				boolean hasStopTimes() {
					return this.stopTimes != null && !this.stopTimes.isEmpty();
				}

				@NonNull
				@Override
				public String toString() {
					return JStopTimeResult.class.getSimpleName() + "{" +
							"lines=" + lines +
							", stopTimes=" + stopTimes +
							'}';
				}

				protected static class JLine {
					private final String directionName;
					private final String lineAbbr;
					private final int lineDirId;
					private final int stopId;

					JLine(String directionName, String lineAbbr, int lineDirId, int stopId) {
						this.directionName = directionName;
						this.lineAbbr = lineAbbr;
						this.lineDirId = lineDirId;
						this.stopId = stopId;
					}

					String getDirectionName() {
						return directionName;
					}

					String getLineAbbr() {
						return lineAbbr;
					}

					int getLineDirId() {
						return lineDirId;
					}

					boolean hasLineDirId() {
						return this.lineDirId > 0;
					}

					String getStopIdS() {
						return String.valueOf(this.stopId);
					}

					@NonNull
					@Override
					public String toString() {
						return JLine.class.getSimpleName() + "{" +
								"directionName='" + directionName + '\'' +
								", lineAbbr='" + lineAbbr + '\'' +
								", lineDirId=" + lineDirId +
								", stopId=" + stopId +
								'}';
					}
				}

				protected static class JStopTime {
					private final String destinationSign;
					private final int eTime;
					private final int lineDirId;
					private final String stopId;
					private final int tripId;

					JStopTime(String destinationSign, int eTime, int lineDirId, String stopId, int tripId) {
						this.destinationSign = destinationSign;
						this.eTime = eTime;
						this.lineDirId = lineDirId;
						this.stopId = stopId;
						this.tripId = tripId;
					}

					String getDestinationSign() {
						return destinationSign;
					}

					int getETime() {
						return eTime;
					}

					int getLineDirId() {
						return lineDirId;
					}

					boolean hasLineDirId() {
						return this.lineDirId > 0;
					}

					int getTripId() {
						return tripId;
					}

					@NonNull
					@Override
					public String toString() {
						return JStopTime.class.getSimpleName() + "{" +
								", destinationSign='" + destinationSign + '\'' +
								", eTime=" + eTime +
								", lineDirId=" + lineDirId +
								", stopId='" + stopId + '\'' +
								", tripId=" + tripId +
								'}';
					}
				}
			}
		}
	}
}
