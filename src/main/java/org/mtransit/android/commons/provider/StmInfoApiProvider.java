package org.mtransit.android.commons.provider;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.security.ProviderInstaller;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
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
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.commons.CollectionUtils;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint("Registered")
public class StmInfoApiProvider extends MTContentProvider implements StatusProviderContract, ServiceUpdateProviderContract, ProviderInstaller.ProviderInstallListener {

	private static final String LOG_TAG = StmInfoApiProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		ServiceUpdateProvider.append(URI_MATCHER, authority);
		StatusProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link StmInfoApiProvider} implementations in same app.
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
	 * Override if multiple {@link StmInfoApiProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.stm_info_api_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link StmInfoApiProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static final long STM_INFO_API_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long STM_INFO_API_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(5L);
	private static final long STM_INFO_API_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.SECONDS.toMillis(30L);
	private static final long STM_INFO_API_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.SECONDS.toMillis(30L);
	private static final long STM_INFO_API_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.SECONDS.toMillis(30L);

	private static final long STM_INFO_API_SERVICE_UPDATE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L);
	private static final long STM_INFO_API_SERVICE_UPDATE_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long STM_INFO_API_SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long STM_INFO_API_SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long STM_INFO_API_SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getStatusMaxValidityInMs() {
		return STM_INFO_API_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getServiceUpdateMaxValidityInMs() {
		return STM_INFO_API_SERVICE_UPDATE_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return STM_INFO_API_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return STM_INFO_API_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getServiceUpdateValidityInMs(boolean inFocus) {
		if (inFocus) {
			return STM_INFO_API_SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS;
		}
		return STM_INFO_API_SERVICE_UPDATE_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return STM_INFO_API_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return STM_INFO_API_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public long getMinDurationBetweenServiceUpdateRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return STM_INFO_API_SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return STM_INFO_API_SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Override
	public void cacheServiceUpdates(@Nullable ArrayList<ServiceUpdate> newServiceUpdates) {
		ServiceUpdateProvider.cacheServiceUpdatesS(this, newServiceUpdates);
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
		if (rts.getStop().getCode().isEmpty() //
				|| rts.getTrip().getHeadsignValue().isEmpty() //
				|| rts.getRoute().getShortName().isEmpty()) {
			return null;
		}
		String uuid = getAgencyRouteStopTargetUUID(rts);
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, uuid);
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom tag
			if (rts.isNoPickup()) {
				if (cachedStatus instanceof Schedule) {
					((Schedule) cachedStatus).setNoPickup(true); // API doesn't know about "descent only" & doesn't return drop off time for last stop
				}
			}
		}
		return cachedStatus;
	}

	@Nullable
	@Override
	public ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (!(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getCachedServiceUpdates() > no service update (poi null or not RTS)");
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		ArrayList<ServiceUpdate> cachedServiceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this,
				getServiceUpdateTargetUUID(rts) //
		);
		enhanceRTServiceUpdatesForStop(cachedServiceUpdates, rts);
		return cachedServiceUpdates;
	}

	private void enhanceRTServiceUpdatesForStop(ArrayList<ServiceUpdate> serviceUpdates, RouteTripStop rts) {
		try {
			if (CollectionUtils.getSize(serviceUpdates) > 0) {
				Pattern stop = LocaleUtils.isFR() ? STOP_FR : STOP;
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					serviceUpdate.setTargetUUID(rts.getUUID()); // route trip service update targets stop
					enhanceRTServiceUpdateForStop(serviceUpdate, rts, stop);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update for stop!");
		}
	}

	private void enhanceRTServiceUpdateForStop(ServiceUpdate serviceUpdate, RouteTripStop rts, Pattern stop) {
		try {
			if (serviceUpdate.getSeverity() > ServiceUpdate.SEVERITY_NONE) {
				String originalHtml = serviceUpdate.getTextHTML();
				int severity = findRTSSeverity(serviceUpdate.getText(), rts, stop);
				if (severity > serviceUpdate.getSeverity()) {
					serviceUpdate.setSeverity(severity);
				}
				serviceUpdate.setTextHTML(
						enhanceRTTextForStop(originalHtml, rts, serviceUpdate.getSeverity())
				);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update '%s' for stop!", serviceUpdate);
		}
	}

	private String enhanceRTTextForStop(String originalHtml, RouteTripStop rts, int severity) {
		if (originalHtml == null || originalHtml.isEmpty()) {
			return originalHtml;
		}
		try {
			String html = originalHtml;
			html = enhanceHtmlRts(rts, html);
			html = enhanceHtmlSeverity(severity, html);
			html = enhanceHtmlDateTime(html);
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update HTML '%s' for stop!", originalHtml);
			return originalHtml;
		}
	}

	private static final Pattern CLEAN_TIME = Pattern.compile("([\\d]{1,2})[\\s]*[:|h][\\s]*([\\d]{2})([\\s]*([a|p]m))?", Pattern.CASE_INSENSITIVE);

	private static final Pattern CLEAN_DATE = Pattern.compile("([\\d]{1,2}[\\s]*[a-zA-Z]+[\\s]*[\\d]{4})");

	private static final ThreadSafeDateFormatter PARSE_TIME = new ThreadSafeDateFormatter("HH:mm", Locale.ENGLISH);

	private static final ThreadSafeDateFormatter PARSE_TIME_AMPM = new ThreadSafeDateFormatter("hh:mm a", Locale.ENGLISH);

	private static final ThreadSafeDateFormatter FORMAT_DATE = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM);

	private static final String PARSE_DATE_REGEX = "dd MMMM yyyy";

	private static final TimeZone TZ = TimeZone.getTimeZone("America/Montreal");

	private String enhanceHtmlDateTime(String html) throws ParseException {
		final Context context = getContext();
		if (context == null) {
			return html;
		}
		if (html == null || html.isEmpty()) {
			return html;
		}
		Matcher timeMatcher = CLEAN_TIME.matcher(html);
		while (timeMatcher.find()) {
			String time = timeMatcher.group(0);
			if (time == null) {
				continue;
			}
			String hours = timeMatcher.group(1);
			String minutes = timeMatcher.group(2);
			String amPm = StringUtils.trim(timeMatcher.group(3));
			Date timeD;
			if (amPm == null || amPm.isEmpty()) {
				PARSE_TIME.setTimeZone(TZ);
				timeD = PARSE_TIME.parseThreadSafe(hours + ":" + minutes);
			} else {
				PARSE_TIME_AMPM.setTimeZone(TZ);
				timeD = PARSE_TIME_AMPM.parseThreadSafe(hours + ":" + minutes + " " + amPm);
			}
			if (timeD == null) {
				continue;
			}
			String fTime = TimeUtils.formatTime(false, context, timeD);
			html = html.replace(time, fTime);
		}
		Matcher dateMatcher = CLEAN_DATE.matcher(html);
		ThreadSafeDateFormatter parseDate = new ThreadSafeDateFormatter(PARSE_DATE_REGEX, LocaleUtils.isFR() ? Locale.FRENCH : Locale.ENGLISH);
		while (dateMatcher.find()) {
			String date = dateMatcher.group(0);
			if (date == null) {
				continue;
			}
			Date dateD = parseDate.parseThreadSafe(date);
			if (dateD == null) {
				continue;
			}
			String fDate = FORMAT_DATE.formatThreadSafe(dateD);
			html = html.replace(date, fDate);
		}
		return html;
	}

	private static String getAgencyRouteStopTargetUUID(@NonNull RouteTripStop rts) {
		return getAgencyRouteStopTargetUUID(rts.getAuthority(), rts.getRoute().getShortName(), rts.getTrip().getHeadsignValue(), rts.getStop().getCode());
	}

	private static String getAgencyRouteStopTargetUUID(String agencyAuthority, String routeShortName, String tripHeadsign, String stopCode) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName, tripHeadsign, stopCode);
	}

	private String getServiceUpdateTargetUUID(@NonNull RouteTripStop rts) {
		String targetAuthority = rts.getAuthority();
		String routeShortName = rts.getRoute().getShortName();
		String tripHeadsignValue = rts.getTrip().getHeadsignValue();
		return getServiceUpdateTargetUUID(targetAuthority, routeShortName, tripHeadsignValue);
	}

	@NonNull
	protected static String getServiceUpdateTargetUUID(@NonNull String targetAuthority, @NonNull String routeShortName, @NonNull String tripHeadsignValue) {
		return POI.POIUtils.getUUID(targetAuthority, routeShortName, tripHeadsignValue);
	}

	@Override
	public boolean purgeUselessCachedStatuses() {
		return StatusProvider.purgeUselessCachedStatuses(this);
	}

	@Override
	public boolean purgeUselessCachedServiceUpdates() {
		return ServiceUpdateProvider.purgeUselessCachedServiceUpdates(this);
	}

	@Override
	public boolean deleteCachedStatus(int cachedStatusId) {
		return StatusProvider.deleteCachedStatus(this, cachedStatusId);
	}

	@Override
	public boolean deleteCachedServiceUpdate(@NonNull Integer serviceUpdateId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, serviceUpdateId);
	}

	@Override
	public boolean deleteCachedServiceUpdate(@NonNull String targetUUID, @NonNull String sourceId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, targetUUID, sourceId);
	}

	@NonNull
	@Override
	public String getStatusDbTableName() {
		return StmInfoApiDbHelper.T_STM_INFO_API_STATUS;
	}

	@NonNull
	@Override
	public String getServiceUpdateDbTableName() {
		return StmInfoApiDbHelper.T_STM_INFO_API_SERVICE_UPDATE;
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
		if (rts.getStop().getCode().isEmpty()
				|| rts.getRoute().getShortName().isEmpty()) {
			return null;
		}
		loadRealTimeStatusFromWWW(rts);
		return getCachedStatus(statusFilter);
	}

	@Nullable
	@Override
	public ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (!(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getNewServiceUpdates() > no new service update (filter null or poi null or not RTS): %s", serviceUpdateFilter);
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		if (rts.getTrip().getHeadsignValue().isEmpty()
				|| rts.getRoute().getShortName().isEmpty()) {
			MTLog.d(this, "getNewServiceUpdates() > skip (stop w/o code OR route w/o short name: %s)", rts);
			return null;
		}
		loadRealTimeServiceUpdateFromWWW(rts);
		return getCachedServiceUpdates(serviceUpdateFilter);
	}

	@NonNull
	@Override
	public String getServiceUpdateLanguage() {
		return LocaleUtils.isFR() ? Locale.FRENCH.getLanguage() : Locale.ENGLISH.getLanguage();
	}

	private static final String NORTH = "N";
	private static final String SOUTH = "S";
	private static final String EAST = "E";
	private static final String WEST = "W";
	private static final String WEST_FR = "O";

	private static final String REAL_TIME_URL_PART_1_BEFORE_LANG = "https://api.stm.info/pub/i3/v1c/api/";
	private static final String REAL_TIME_URL_PART_2_BEFORE_ROUTE_SHORT_NAME = "/lines/";
	private static final String REAL_TIME_URL_PART_3_BEFORE_STOP_CODE = "/stops/";
	private static final String REAL_TIME_URL_PART_4_BEFORE_DIRECTION = "/arrivals?direction=";
	private static final String REAL_TIME_URL_PART_5_BEFORE_LIMIT = "&limit=";

	@NonNull
	private static String getRealTimeStatusUrlString(@NonNull RouteTripStop rts) {
		return REAL_TIME_URL_PART_1_BEFORE_LANG + //
				(LocaleUtils.isFR() ? "fr" : "en") + //
				REAL_TIME_URL_PART_2_BEFORE_ROUTE_SHORT_NAME + //
				rts.getRoute().getShortName() + //
				REAL_TIME_URL_PART_3_BEFORE_STOP_CODE + //
				rts.getStop().getCode() + //
				REAL_TIME_URL_PART_4_BEFORE_DIRECTION + //
				getDirection(rts.getTrip()) + //
				REAL_TIME_URL_PART_5_BEFORE_LIMIT + //
				20;
	}

	@NonNull
	private static String getDirection(@NonNull Trip trip) {
		if (trip.getHeadsignType() == Trip.HEADSIGN_TYPE_DIRECTION) {
			switch (trip.getHeadsignValue()) {
			case Trip.HEADING_EAST:
				return EAST;
			case Trip.HEADING_WEST:
				return WEST;
			case Trip.HEADING_NORTH:
				return NORTH;
			case Trip.HEADING_SOUTH:
				return SOUTH;
			}
		}
		MTLog.w(LOG_TAG, "Unexpected direction for trip '%s'!", true);
		return StringUtils.EMPTY;
	}

	private static final String SERVICE_UPDATE_SOURCE_ID = "api_stm_info_arrivals_messages";

	private static final String SERVICE_UPDATE_SOURCE_LABEL = "api.stm.info";

	private static final String APPLICATION_JSON = "application/JSON";
	private static final String ACCEPT = "accept";

	private void loadRealTimeStatusFromWWW(@NonNull RouteTripStop rts) {
		try {
			final Context context = getContext();
			if (context == null) {
				return;
			}
			String urlString = getRealTimeStatusUrlString(rts);
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			urlc.addRequestProperty("Origin", "https://stm.info");
			urlc.addRequestProperty(ACCEPT, APPLICATION_JSON);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadRealTimeStatusFromWWW() > jsonString: %s.", jsonString);
				JArrivals jArrivals = parseAgencyJSONArrivals(jsonString);
				List<JArrivals.JResult> jResults = jArrivals.getResults();
				Collection<POIStatus> statuses = parseAgencyJSONArrivalsResults(context.getResources(),
						jResults,
						rts, newLastUpdateInMs);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(getAgencyRouteStopTargetUUID(rts)));
				if (statuses != null) {
					for (POIStatus status : statuses) {
						StatusProvider.cacheStatusS(this, status);
					}
				}
				MTLog.i(this, "Found %d schedule statuses.", (statuses == null ? 0 : statuses.size()));
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

	private static final String REAL_TIME_SERVICE_UPDATE_URL_PART_1_BEFORE_LANG = "https://api.stm.info/pub/i3/v1c/api/";
	private static final String REAL_TIME_SERVICE_UPDATE_URL_PART_2_BEFORE_ROUTE_SHORT_NAME = "/lines/";
	private static final String REAL_TIME_SERVICE_UPDATE_URL_PART_3 = "/messages?type=Bus";

	private static String getRealTimeServiceUpdateUrlString(@NonNull RouteTripStop rts) {
		return REAL_TIME_SERVICE_UPDATE_URL_PART_1_BEFORE_LANG + //
				(LocaleUtils.isFR() ? "fr" : "en") + //
				REAL_TIME_SERVICE_UPDATE_URL_PART_2_BEFORE_ROUTE_SHORT_NAME + //
				rts.getRoute().getShortName() + //
				REAL_TIME_SERVICE_UPDATE_URL_PART_3;
	}

	private void loadRealTimeServiceUpdateFromWWW(RouteTripStop rts) {
		try {
			String urlString = getRealTimeServiceUpdateUrlString(rts);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			urlc.addRequestProperty("Origin", "https://stm.info");
			urlc.addRequestProperty(ACCEPT, APPLICATION_JSON);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				JMessages jMessages = parseAgencyJSONMessages(jsonString);
				List<JMessages.JResult> jResults = jMessages.getResults();
				ArrayList<ServiceUpdate> serviceUpdates = parseAgencyJSONMessageResults(
						jResults,
						rts, newLastUpdateInMs);
				deleteOldAndCacheNewServiceUpdates(serviceUpdates);
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

	private synchronized void deleteOldAndCacheNewServiceUpdates(ArrayList<ServiceUpdate> serviceUpdates) { // SYNC because may have multiple concurrent same route call
		if (serviceUpdates != null) {
			for (ServiceUpdate serviceUpdate : serviceUpdates) {
				deleteCachedServiceUpdate(serviceUpdate.getTargetUUID(), SERVICE_UPDATE_SOURCE_ID);
			}
		}
		cacheServiceUpdates(serviceUpdates);
	}

	private JMessages parseAgencyJSONMessages(String jsonString) {
		List<JMessages.JResult> results = new ArrayList<>();
		try {
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_RESULT)) {
				JSONArray jResults = json.getJSONArray(JSON_RESULT);
				List<Map<String, List<JMessages.JResult.JResultRoute>>> shortNameResultRouteList = new ArrayList<>();
				for (int r = 0; r < jResults.length(); r++) {
					Map<String, List<JMessages.JResult.JResultRoute>> shortNameResultRoutes = new HashMap<>();
					JSONObject jResult = jResults.getJSONObject(r);
					Iterator<String> it = jResult.keys();
					while (it.hasNext()) {
						String jResultKey = it.next();
						ArrayList<JMessages.JResult.JResultRoute> resultRoutes = new ArrayList<>();
						if (jResult.has(jResultKey)) {
							JSONArray jResultRoutes = jResult.getJSONArray(jResultKey);
							for (int rr = 0; rr < jResultRoutes.length(); rr++) {
								JSONObject jResultRoute = jResultRoutes.getJSONObject(rr);
								String direction = jResultRoute.getString(JSON_DIRECTION);
								String directionName = jResultRoute.getString(JSON_DIRECTION_NAME);
								String text = jResultRoute.getString(JSON_TEXT);
								String code = jResultRoute.getString(JSON_CODE);
								String date = jResultRoute.getString(JSON_DATE);
								resultRoutes.add(new JMessages.JResult.JResultRoute(direction, directionName, text, code, date));
							}
						}
						shortNameResultRoutes.put(jResultKey, resultRoutes);
					}
					shortNameResultRouteList.add(shortNameResultRoutes);
				}
				results.add(new JMessages.JResult(shortNameResultRouteList));
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
		}
		return new JMessages(results);
	}

	@Nullable
	protected ArrayList<ServiceUpdate> parseAgencyJSONMessageResults(@NonNull List<JMessages.JResult> jResults,
																	 @NonNull RouteTripStop rts,
																	 long newLastUpdateInMs) {
		try {
			ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<>();
			long maxValidityInMs = getServiceUpdateMaxValidityInMs();
			String language = getServiceUpdateLanguage();
			for (JMessages.JResult jResult : jResults) {
				List<Map<String, List<JMessages.JResult.JResultRoute>>> jShortNameResultRouteList = jResult.getShortNameResultRoutes();
				for (Map<String, List<JMessages.JResult.JResultRoute>> jShortNameResultRoutes : jShortNameResultRouteList) {
					for (Map.Entry<String, List<JMessages.JResult.JResultRoute>> jShortNameResultRoute : jShortNameResultRoutes.entrySet()) {
						String routeShortName = jShortNameResultRoute.getKey();
						if (routeShortName == null || routeShortName.isEmpty()) {
							routeShortName = rts.getRoute().getShortName(); // default to RTS short name
						}
						for (JMessages.JResult.JResultRoute jResultRoute : jShortNameResultRoute.getValue()) {
							String directionName = jResultRoute.getDirectionName();
							String direction = jResultRoute.getDirection();
							String text = jResultRoute.getText();
							String code = jResultRoute.getCode();
							String tripHeadsignValue = parseAgencyTripHeadsignValue(direction);
							if (tripHeadsignValue == null) {
								tripHeadsignValue = parseAgencyTripHeadsignValue(directionName);
							}
							if (tripHeadsignValue == null) {
								continue;
							}
							String targetUUID = getServiceUpdateTargetUUID(rts.getAuthority(), routeShortName, tripHeadsignValue);
							if (!text.isEmpty()) {
								int severity = ServiceUpdate.SEVERITY_INFO_RELATED_POI; // service updates target this route stops
								if (JMessages.JResult.JResultRoute.CODE_NORMAL.equals(code)) {
									severity = ServiceUpdate.SEVERITY_NONE; // Normal service
								}
								String textHtml = enhanceHtml(text, null, null); // no severity|stop based enhancement here
								serviceUpdates.add(new ServiceUpdate( //
										null, targetUUID, newLastUpdateInMs, maxValidityInMs,
										text, textHtml, severity,
										SERVICE_UPDATE_SOURCE_ID, SERVICE_UPDATE_SOURCE_LABEL, language));
							}
						}
					}
				}
			}
			if (serviceUpdates.isEmpty()) {
				if (CollectionUtils.getSize(serviceUpdates) == 0) {
					MTLog.d(this, "No messages found, return empty severity none message  #ServiceUpdate");
					String targetUUID = getServiceUpdateTargetUUID(rts.getAuthority(), rts.getRoute().getShortName(), rts.getTrip().getHeadsignValue());
					ServiceUpdate serviceUpdateNone = new ServiceUpdate( //
							null, targetUUID, newLastUpdateInMs, maxValidityInMs, //
							null, null, //
							ServiceUpdate.SEVERITY_NONE, //
							SERVICE_UPDATE_SOURCE_ID, SERVICE_UPDATE_SOURCE_LABEL, language);
					serviceUpdates.add(serviceUpdateNone);
				}
			}
			return serviceUpdates;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON results '%s'!", jResults);
			return null;
		}
	}

	@Nullable
	private String parseAgencyTripHeadsignValue(String directionName) {
		if (directionName != null && !directionName.isEmpty()) {
			if (directionName.startsWith(NORTH)) { // North / Nord
				return Trip.HEADING_NORTH;
			} else if (directionName.startsWith(SOUTH)) { // South / Sud
				return Trip.HEADING_SOUTH;
			} else if (directionName.startsWith(EAST)) { // Est / East
				return Trip.HEADING_EAST;
			} else if (directionName.startsWith(WEST) || directionName.startsWith(WEST_FR)) { // West / Ouest
				return Trip.HEADING_WEST;
			}
		}
		MTLog.w(this, "parseAgencyTripHeadsignValue() > unexpected direction '%s'!", directionName);
		return null;
	}

	private static final TimeZone MONTREAL_TZ = TimeZone.getTimeZone("America/Montreal");

	private Calendar getNewBeginningOfTodayCal(Calendar nowCal) {
		Calendar beginningOfTodayCal = Calendar.getInstance(nowCal.getTimeZone());
		beginningOfTodayCal.setTimeInMillis(nowCal.getTimeInMillis());
		beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, 0);
		beginningOfTodayCal.set(Calendar.MINUTE, 0);
		beginningOfTodayCal.set(Calendar.SECOND, 0);
		beginningOfTodayCal.set(Calendar.MILLISECOND, 0);
		return beginningOfTodayCal;
	}

	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(60L);

	private static final String BUS_STOP = "bus stop[\\S]*";
	private static final String BUS_STOP_FR = "arr[ê|e]t[\\S]*";

	protected static final Pattern STOP = Pattern.compile("(" + BUS_STOP + ")", Pattern.CASE_INSENSITIVE);
	protected static final Pattern STOP_FR = Pattern.compile("(" + BUS_STOP_FR + ")", Pattern.CASE_INSENSITIVE);

	private static final String POINT = "\\.";
	private static final String PARENTHESES1 = "\\(";
	private static final String PARENTHESES2 = "\\)";
	private static final String SLASH = "/";
	private static final String ANY_STOP_CODE = "[\\d]+";

	private static final Pattern CLEAN_STOP_CODE_AND_NAME = Pattern.compile("(" + ANY_STOP_CODE + ")[\\s]*" + PARENTHESES1 + "([^" + SLASH + "]*)" + SLASH
			+ "([^" + PARENTHESES2 + "]*)" + PARENTHESES2 + "([" + PARENTHESES2 + "]*)" + "([,]*)([.]*)");
	private static final String CLEAN_STOP_CODE_AND_NAME_REPLACEMENT = "- $2" + SLASH + "$3$4 " + PARENTHESES1 + "$1" + PARENTHESES2 + "$5$6";

	private static final Pattern CLEAN_BR = Pattern.compile("(" + PARENTHESES2 + ",|" + POINT + "|:)[\\s]+");
	private static final String CLEAN_BR_REPLACEMENT = "$1" + HtmlUtils.BR;

	private static final String CLEAN_THAT_STOP_CODE = "(\\-[\\s]+)" + "([^" + SLASH + "]*" + SLASH + "[^" + PARENTHESES1 + "]*" + PARENTHESES1 + "%s"
			+ PARENTHESES2 + ")";
	private static final String CLEAN_THAT_STOP_CODE_REPLACEMENT = "$1" + HtmlUtils.applyBold("$2");

	@SuppressWarnings("SameParameterValue")
	private String enhanceHtml(@Nullable String originalHtml, @Nullable RouteTripStop rts, @Nullable Integer severity) {
		if (originalHtml == null || originalHtml.isEmpty()) {
			return originalHtml;
		}
		try {
			String html = originalHtml;
			html = CLEAN_BR.matcher(html).replaceAll(CLEAN_BR_REPLACEMENT);
			html = CLEAN_STOP_CODE_AND_NAME.matcher(html).replaceAll(CLEAN_STOP_CODE_AND_NAME_REPLACEMENT);
			if (rts != null) {
				html = enhanceHtmlRts(rts, html);
			}
			if (severity != null) {
				html = enhanceHtmlSeverity(severity, html);
			}
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance HTML '%s' (using original)!", originalHtml);
			return originalHtml;
		}
	}

	private String enhanceHtmlRts(@NonNull RouteTripStop rts, String html) {
		if (html == null || html.isEmpty()) {
			return html;
		}
		return Pattern.compile(String.format(CLEAN_THAT_STOP_CODE, rts.getStop().getCode())) //
				.matcher(html) //
				.replaceAll(CLEAN_THAT_STOP_CODE_REPLACEMENT);
	}

	private static final Pattern CLEAN_BOLD = Pattern.compile("(" + BUS_STOP + "|relocat[\\S]*|cancel[\\S]*)");
	private static final Pattern CLEAN_BOLD_FR = Pattern.compile("(" + BUS_STOP_FR + "|déplac[\\S]*|annul[\\S]*)");
	private static final String CLEAN_BOLD_REPLACEMENT = HtmlUtils.applyBold("$1");

	private String enhanceHtmlSeverity(int severity, String html) {
		if (html == null || html.isEmpty()) {
			return html;
		}
		if (ServiceUpdate.isSeverityWarning(severity)) {
			if (LocaleUtils.isFR()) {
				return CLEAN_BOLD_FR.matcher(html).replaceAll(CLEAN_BOLD_REPLACEMENT);
			} else {
				return CLEAN_BOLD.matcher(html).replaceAll(CLEAN_BOLD_REPLACEMENT);
			}
		}
		return html;
	}

	protected int findRTSSeverity(@Nullable String text, @NonNull RouteTripStop rts, @NonNull Pattern stop) {
		if (text != null && !text.isEmpty()) {
			if (text.contains(rts.getStop().getCode())) {
				return ServiceUpdate.SEVERITY_WARNING_POI;
			} else if (stop.matcher(text).find()) {
				return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
			}
		}
		MTLog.d(this, "Cannot find RTS severity for '%s'. (%s)", text, rts);
		return ServiceUpdate.SEVERITY_INFO_UNKNOWN;
	}

	@Nullable
	protected Collection<POIStatus> parseAgencyJSONArrivalsResults(@NonNull Resources res,
																   @NonNull List<JArrivals.JResult> jResults,
																   @NonNull RouteTripStop rts,
																   long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> result = new ArrayList<>();
			Calendar nowCal = Calendar.getInstance(MONTREAL_TZ);
			nowCal.setTimeInMillis(newLastUpdateInMs);
			nowCal.add(Calendar.HOUR_OF_DAY, -1);
			boolean hasRealTime = false;
			boolean hasCongestion = false;
			boolean hasOnRequestedDay = false;
			Schedule newSchedule = new Schedule(getAgencyRouteStopTargetUUID(rts), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs,
					PROVIDER_PRECISION_IN_MS, false);
			for (int r = 0; r < jResults.size(); r++) {
				JArrivals.JResult jResult = jResults.get(r);
				if (jResult != null && !jResult.getTime().isEmpty()) {
					String jTime = jResult.getTime();
					boolean isReal = jResult.isReal();
					long t;
					if (jTime.length() != 4) {
						hasRealTime = true;
						long countdownInMs = TimeUnit.MINUTES.toMillis(Long.parseLong(jTime));
						t = newLastUpdateInMs + countdownInMs;
					} else {
						Calendar beginningOfTodayCal = getNewBeginningOfTodayCal(nowCal);
						int hour = Integer.parseInt(jTime.substring(0, 2));
						beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, hour);
						int minutes = Integer.parseInt(jTime.substring(2, 4));
						beginningOfTodayCal.set(Calendar.MINUTE, minutes);
						if (beginningOfTodayCal.before(nowCal)) {
							beginningOfTodayCal.add(Calendar.DATE, 1);
						}
						t = beginningOfTodayCal.getTimeInMillis();
					}
					nowCal.setTimeInMillis(t);
					Schedule.Timestamp timestamp = new Schedule.Timestamp(TimeUtils.timeToTheMinuteMillis(t));
					if (jResult.isCongestion()) {
						hasCongestion = true;
						if (!timestamp.hasHeadsign()) {
							timestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, //
									res.getString(R.string.unknown_delay_short)
											+ StringUtils.SPACE_STRING
											+ res.getString(R.string.traffic_congestion)
							);
						}
					}
					if (jResult.isOnRequestedDay()) {
						hasOnRequestedDay = true;
					}
					timestamp.setRealTime(isReal);
					newSchedule.addTimestampWithoutSort(timestamp);
				}
			}
			newSchedule.sortTimestamps();
			if (hasRealTime || hasCongestion || hasOnRequestedDay) {
				result.add(newSchedule);
			} // ELSE => dismissed because only returned planned schedule data which isn't trustworthy #767
			return result;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON results '%s'!", jResults);
			return null;
		}
	}

	private static final String JSON_DIRECTION = "direction";
	private static final String JSON_DIRECTION_NAME = "direction_name";
	private static final String JSON_DATE = "date";
	private static final String JSON_MESSAGES = "messages";
	private static final String JSON_LINE = "line";
	private static final String JSON_START_DATE = "start_date";
	private static final String JSON_TEXT = "text";
	private static final String JSON_CODE = "code";
	private static final String JSON_RESULT = "result";
	private static final String JSON_TIME = "time";
	private static final String JSON_IS_REAL = "is_real";
	private static final String JSON_IS_CONGESTION = "is_congestion";
	private static final String JSON_IS_ON_REQUESTED_DAY = "is_on_requested_day";

	@NonNull
	private JArrivals parseAgencyJSONArrivals(@Nullable String jsonString) {
		List<JArrivals.JMessages.JLine> lines = new ArrayList<>();
		List<JArrivals.JResult> results = new ArrayList<>();
		try {
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			parseAgencyJSONArrivalsMessages(lines, json);
			parseAgencyJSONArrivalsResults(results, json);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
		}
		return new JArrivals(new JArrivals.JMessages(lines), results);
	}

	private void parseAgencyJSONArrivalsResults(@NonNull List<JArrivals.JResult> results, @Nullable JSONObject json) {
		try {
			if (json != null && json.has(JSON_RESULT)) {
				JSONArray jResults = json.getJSONArray(JSON_RESULT);
				if (jResults.length() > 0) {
					for (int r = 0; r < jResults.length(); r++) {
						JSONObject jResult = jResults.getJSONObject(r);
						if (jResult != null && jResult.has(JSON_TIME)) {
							String jTime = jResult.getString(JSON_TIME);
							boolean isReal;
							if (jResult.has(JSON_IS_REAL)) {
								isReal = jResult.getBoolean(JSON_IS_REAL);
							} else {
								isReal = jTime.length() != 4;
							}
							boolean isCongestion = false;
							if (jResult.has(JSON_IS_CONGESTION)) {
								isCongestion = jResult.getBoolean(JSON_IS_CONGESTION);
							}
							boolean isOnRequestedDay = true;
							if (jResult.has(JSON_IS_ON_REQUESTED_DAY)) {
								isOnRequestedDay = jResult.getBoolean(JSON_IS_ON_REQUESTED_DAY);
							}
							results.add(new JArrivals.JResult(jTime, isReal, isCongestion, isOnRequestedDay));
						}
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", json);
		}
	}

	private void parseAgencyJSONArrivalsMessages(@NonNull List<JArrivals.JMessages.JLine> lines, @Nullable JSONObject json) {
		try {
			if (json != null && json.has(JSON_MESSAGES)) {
				JSONObject jMessages = json.optJSONObject(JSON_MESSAGES); // returns an array[] when no messages!
				if (jMessages != null && jMessages.has(JSON_LINE)) {
					JSONArray jLines = jMessages.getJSONArray(JSON_LINE);
					for (int l = 0; l < jLines.length(); l++) {
						JSONObject jLine = jLines.getJSONObject(l);
						if (jLine != null && jLine.has(JSON_TEXT)) {
							String jText = jLine.getString(JSON_TEXT);
							if (!jText.isEmpty()) {
								String jStartDate = jLine.optString(JSON_START_DATE);
								lines.add(new JArrivals.JMessages.JLine(jText, jStartDate));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", json);
		}
	}

	@Override
	public boolean onCreateMT() {
		if (getContext() == null) {
			return true; // or false?
		}
		ping();
		updateSecurityProviderIfNeeded(getContext());
		return true;
	}

	@Override
	public void ping() {
		// DO NOTHING
	}

	private void updateSecurityProviderIfNeeded(@NonNull Context context) {
		try {
			ProviderInstaller.installIfNeededAsync(context, this);
		} catch (Exception e) {
			MTLog.w(this, e, "Unexpected error while updating security provider!");
		}
	}

	@Override
	public void onProviderInstalled() {
		MTLog.d(this, "Security provider is up-to-date.");
	}

	@Override
	public void onProviderInstallFailed(int i, @Nullable Intent intent) {
		MTLog.w(this, "Unexpected error while updating security provider (%s,%s)!", i, intent);
	}

	@Nullable
	private StmInfoApiDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private StmInfoApiDbHelper getDBHelper(@NonNull Context context) {
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
	 * Override if multiple {@link StmInfoApiProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		//noinspection ConstantConditions // TODO requireContext()
		return StmInfoApiDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link StmInfoApiProvider} implementations in same app.
	 */
	@NonNull
	public StmInfoApiDbHelper getNewDbHelper(@NonNull Context context) {
		return new StmInfoApiDbHelper(context.getApplicationContext());
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		//noinspection ConstantConditions // TODO requireContext()
		return getURIMATCHER(getContext());
	}

	@NonNull
	@Override
	public Uri getAuthorityUri() {
		//noinspection ConstantConditions // TODO requireContext()
		return getAUTHORITY_URI(getContext());
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

	@Nullable
	@Override
	public Cursor queryMT(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		Cursor cursor = ServiceUpdateProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		cursor = StatusProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
	}

	@Nullable
	@Override
	public String getTypeMT(@NonNull Uri uri) {
		String type = ServiceUpdateProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		type = StatusProvider.getTypeS(this, uri);
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

	public static class StmInfoApiDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = StmInfoApiDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link StmInfoApiDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "stm_info_api.db";

		static final String T_STM_INFO_API_SERVICE_UPDATE = ServiceUpdateProvider.ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_STM_INFO_API_SERVICE_UPDATE_SQL_CREATE = ServiceUpdateProvider.ServiceUpdateDbHelper.getSqlCreateBuilder(
				T_STM_INFO_API_SERVICE_UPDATE).build();

		private static final String T_STM_INFO_API_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STM_INFO_API_SERVICE_UPDATE);

		static final String T_STM_INFO_API_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_STM_INFO_API_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_STM_INFO_API_STATUS).build();

		private static final String T_STM_INFO_API_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STM_INFO_API_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link StmInfoApiDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.stm_info_api_db_version);
			}
			return dbVersion;
		}

		StmInfoApiDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_STM_INFO_API_SERVICE_UPDATE_SQL_DROP);
			db.execSQL(T_STM_INFO_API_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_STM_INFO_API_SERVICE_UPDATE_SQL_CREATE);
			db.execSQL(T_STM_INFO_API_STATUS_SQL_CREATE);
		}
	}

	@SuppressWarnings({"unused", "WeakerAccess"})
	public static class JArrivals {
		@NonNull
		private final JMessages messages;
		@NonNull
		private final List<JResult> results;

		public JArrivals(@NonNull JMessages messages, @NonNull List<JResult> results) {
			this.messages = messages;
			this.results = results;
		}

		@NonNull
		public JMessages getMessages() {
			return messages;
		}

		@NonNull
		public List<JResult> getResults() {
			return results;
		}

		@NonNull
		@Override
		public String toString() {
			return "JArrivals{" +
					"messages=" + messages +
					", results=" + results +
					'}';
		}

		public static class JResult {
			@NonNull
			private final String time;
			private final boolean isReal;
			private final boolean isCongestion;
			private final boolean isOnRequestedDay;

			public JResult(@NonNull String time, boolean isReal, boolean isCongestion, boolean isOnRequestedDay) {
				this.time = time;
				this.isReal = isReal;
				this.isCongestion = isCongestion;
				this.isOnRequestedDay = isOnRequestedDay;
			}

			@NonNull
			public String getTime() {
				return time;
			}

			public boolean isReal() {
				return isReal;
			}

			public boolean isCongestion() {
				return isCongestion;
			}

			public boolean isOnRequestedDay() {
				return isOnRequestedDay;
			}

			@NonNull
			@Override
			public String toString() {
				return JResult.class.getSimpleName() + "{" +
						"time='" + time + '\'' + "," +
						"isReal=" + isReal + "," +
						"isCongestion=" + isCongestion + "," +
						"isOnRequestedDay=" + isOnRequestedDay + "," +
						'}';
			}
		}

		public static class JMessages {
			@NonNull
			private final List<JLine> lines;

			public JMessages(@NonNull List<JLine> lines) {
				this.lines = lines;
			}

			@NonNull
			public List<JLine> getLines() {
				return lines;
			}

			@NonNull
			@Override
			public String toString() {
				return "JMessages{" +
						"lines=" + lines +
						'}';
			}

			public static class JLine {
				@NonNull
				private final String text;
				@NonNull
				private final String startDate;

				public JLine(@NonNull String text, @NonNull String startDate) {
					this.text = text;
					this.startDate = startDate;
				}

				@NonNull
				public String getText() {
					return text;
				}

				@NonNull
				public String getStartDate() {
					return startDate;
				}

				@NonNull
				@Override
				public String toString() {
					return "JLine{" +
							"text='" + text + '\'' + "," +
							"startDate='" + startDate + '\'' +
							'}';
				}
			}
		}
	}

	@SuppressWarnings({"unused", "WeakerAccess"})
	public static class JMessages {

		@NonNull
		private final List<JResult> results;

		public JMessages(@NonNull List<JResult> results) {
			this.results = results;
		}

		@NonNull
		public List<JResult> getResults() {
			return results;
		}

		@NonNull
		@Override
		public String toString() {
			return "JMessages{" +
					"results=" + results +
					'}';
		}

		public static class JResult {
			@NonNull
			private final List<Map<String, List<JResultRoute>>> shortNameResultRoutes;

			public JResult(@NonNull List<Map<String, List<JResultRoute>>> shortNameResultRoutes) {
				this.shortNameResultRoutes = shortNameResultRoutes;
			}

			@NonNull
			public List<Map<String, List<JResultRoute>>> getShortNameResultRoutes() {
				return shortNameResultRoutes;
			}

			@NonNull
			@Override
			public String toString() {
				return "JResult{" +
						"shortNameResultRoutes=" + shortNameResultRoutes +
						'}';
			}

			public static class JResultRoute {

				public static final String CODE_NORMAL = "Normal";
				public static final String CODE_MESSAGE = "Message";

				@NonNull
				private final String direction;
				@NonNull
				private final String directionName;
				@NonNull
				private final String text;
				@NonNull
				private final String code;
				@NonNull
				private final String date;

				public JResultRoute(@NonNull String direction, @NonNull String directionName, @NonNull String text, @NonNull String code, @NonNull String date) {
					this.direction = direction;
					this.directionName = directionName;
					this.text = text;
					this.code = code;
					this.date = date;
				}

				@NonNull
				public String getDirection() {
					return direction;
				}

				@NonNull
				public String getDirectionName() {
					return directionName;
				}

				@NonNull
				public String getText() {
					return text;
				}

				@NonNull
				public String getCode() {
					return code;
				}

				@NonNull
				public String getDate() {
					return date;
				}

				@NonNull
				@Override
				public String toString() {
					return "JResultRoute{" +
							"direction='" + direction + '\'' +
							", directionName='" + directionName + '\'' +
							", text='" + text + '\'' +
							", code='" + code + '\'' +
							", date='" + date + '\'' +
							'}';
				}
			}
		}
	}
}
