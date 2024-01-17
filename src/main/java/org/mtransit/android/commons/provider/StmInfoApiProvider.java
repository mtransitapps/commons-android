package org.mtransit.android.commons.provider;

import static org.mtransit.commons.RegexUtils.DIGIT_CAR;
import static org.mtransit.commons.RegexUtils.END;
import static org.mtransit.commons.RegexUtils.except;
import static org.mtransit.commons.RegexUtils.group;
import static org.mtransit.commons.RegexUtils.oneOrMore;
import static org.mtransit.commons.RegexUtils.zeroOrMore;

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

import androidx.annotation.MainThread;
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
import org.mtransit.android.commons.data.Accessibility;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.commons.Cleaner;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.FeatureFlags;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

@SuppressLint("Registered")
public class StmInfoApiProvider extends MTContentProvider implements StatusProviderContract, ServiceUpdateProviderContract, ProviderInstaller.ProviderInstallListener {

	private static final String LOG_TAG = StmInfoApiProvider.class.getSimpleName();

	private static final boolean STORE_EMPTY_SERVICE_MESSAGE = false;

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
			MTLog.w(this, "getCachedStatus() > Can't find new schedule without schedule filter!");
			return null;
		}
		final Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		final RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (rts.getStop().getCode().isEmpty() //
				|| rts.getTrip().getHeadsignValue().isEmpty() //
				|| rts.getRoute().getShortName().isEmpty()) {
			return null;
		}
		final POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, getStopStatusTargetUUID(rts));
		if (cachedStatus != null) {
			cachedStatus.setTargetUUID(rts.getUUID()); // target RTS UUID instead of custom tag
			if (rts.isNoPickup()) {
				if (cachedStatus instanceof Schedule) {
					((Schedule) cachedStatus).setNoPickup(true); // API doesn't know about "descent only" & doesn't return drop off time for last stop
				}
			}
		}
		// MTLog.d(this, "getCachedStatus() > %s", cachedStatus);
		return cachedStatus;
	}

	@Nullable
	@Override
	public ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (!(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getCachedServiceUpdates() > no service update (poi null or not RTS)");
			return null;
		}
		final Context context = requireContextCompat();
		final RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		final ArrayList<ServiceUpdate> cachedServiceUpdates = new ArrayList<>();
		CollectionUtils.addAllN(cachedServiceUpdates, ServiceUpdateProvider.getCachedServiceUpdatesS(this, getStopServiceUpdateTargetUUID(rts)));
		if (STORE_EMPTY_SERVICE_MESSAGE) {
			if (cachedServiceUpdates.isEmpty()) {
				return cachedServiceUpdates; // need to get NEW service update from WWW for this STOP
			}
		}
		final ArrayList<ServiceUpdate> routeCachedServiceUpdatesS = ServiceUpdateProvider.getCachedServiceUpdatesS(this, getRouteServiceUpdateTargetUUID(rts));
		if (routeCachedServiceUpdatesS != null) {
			cachedServiceUpdates.addAll(routeCachedServiceUpdatesS);
		}
		enhanceRTServiceUpdatesForStop(context, cachedServiceUpdates, rts);
		// if (org.mtransit.commons.Constants.DEBUG) {
		// MTLog.d(this, "getCachedServiceUpdates() > %s service updates for %s.", cachedServiceUpdates.size(), rts.getUUID());
		// for (ServiceUpdate serviceUpdate : cachedServiceUpdates) {
		// MTLog.d(this, "getCachedServiceUpdates() > - %s", serviceUpdate.toString());
		// }
		// }
		return cachedServiceUpdates;
	}

	private void enhanceRTServiceUpdatesForStop(@NonNull Context context, ArrayList<ServiceUpdate> serviceUpdates, RouteTripStop rts) {
		try {
			if (CollectionUtils.getSize(serviceUpdates) > 0) {
				final Cleaner stop = LocaleUtils.isFR() ? STOP_FR : STOP;
				final boolean isSeverityAlreadyWarning = ServiceUpdate.isSeverityWarning(serviceUpdates);
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					enhanceRTServiceUpdateForStop(context, serviceUpdate, isSeverityAlreadyWarning, rts, stop);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update for stop!");
		}
	}

	private void enhanceRTServiceUpdateForStop(@NonNull Context context,
											   ServiceUpdate serviceUpdate,
											   boolean isSeverityAlreadyWarning,
											   RouteTripStop rts,
											   Cleaner stop) {
		try {
			if (serviceUpdate.getSeverity() > ServiceUpdate.SEVERITY_NONE) {
				String originalHtml = serviceUpdate.getTextHTML();
				if (!isSeverityAlreadyWarning && !STORE_EMPTY_SERVICE_MESSAGE) { // DO increase severity for stop code because stop service update might not have been loaded/returned to UI yet
					int severity = findRTSSeverity(serviceUpdate.getText(), rts, stop);
					if (severity > serviceUpdate.getSeverity()) {
						serviceUpdate.setSeverity(severity);
					}
				}
				serviceUpdate.setTextHTML(
						enhanceRTTextForStop(context, originalHtml, rts, serviceUpdate.getSeverity())
				);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update '%s' for stop!", serviceUpdate);
		}
	}

	private String enhanceRTTextForStop(@NonNull Context context, String originalHtml, RouteTripStop rts, int severity) {
		if (originalHtml == null || originalHtml.isEmpty()) {
			return originalHtml;
		}
		try {
			String html = originalHtml;
			html = enhanceHtmlRts(rts, html, severity);
			html = enhanceHtmlSeverity(severity, html);
			html = enhanceHtmlDateTime(context, html);
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update HTML '%s' for stop!", originalHtml);
			return originalHtml;
		}
	}

	private static final Cleaner CLEAN_TIME = new Cleaner("([\\d]{1,2})[\\s]*[:|h][\\s]*([\\d]{2})([\\s]*([a|p]m))?", true);

	private static final Cleaner CLEAN_DATE = new Cleaner("(\\d{1,2}\\s*[a-zA-Z]+\\s*\\d{4})");

	private static final ThreadSafeDateFormatter PARSE_TIME = new ThreadSafeDateFormatter("HH:mm", Locale.ENGLISH);

	private static final ThreadSafeDateFormatter PARSE_TIME_AM_PM = new ThreadSafeDateFormatter("hh:mm a", Locale.ENGLISH);

	private static final ThreadSafeDateFormatter FORMAT_DATE = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM);

	private static final String PARSE_DATE_REGEX = "dd MMMM yyyy";

	private static final TimeZone TZ = TimeZone.getTimeZone("America/Montreal");

	private String enhanceHtmlDateTime(@NonNull Context context, String html) throws ParseException {
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
				PARSE_TIME_AM_PM.setTimeZone(TZ);
				timeD = PARSE_TIME_AM_PM.parseThreadSafe(hours + ":" + minutes + " " + amPm);
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

	private static String getStopStatusTargetUUID(@NonNull RouteTripStop rts) {
		return getStopStatusTargetUUID(
				rts.getAuthority(),
				rts.getRoute().getShortName(),
				rts.getTrip().getHeadsignValue(),
				rts.getStop().getCode()
		);
	}

	private static String getStopStatusTargetUUID(String agencyAuthority, String routeShortName, String tripHeadsign, String stopCode) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName, tripHeadsign, stopCode);
	}

	private String getRouteServiceUpdateTargetUUID(@NonNull RouteTripStop rts) {
		return getRouteServiceUpdateTargetUUID(
				rts.getAuthority(),
				rts.getRoute().getShortName(),
				rts.getTrip().getHeadsignValue()
		);
	}

	@NonNull
	protected static String getRouteServiceUpdateTargetUUID(@NonNull String targetAuthority, @NonNull String routeShortName, @NonNull String tripHeadsignValue) {
		return POI.POIUtils.getUUID(targetAuthority, routeShortName, tripHeadsignValue);
	}

	private String getStopServiceUpdateTargetUUID(@NonNull RouteTripStop rts) {
		return getStopServiceUpdateTargetUUID(
				rts.getAuthority(),
				rts.getRoute().getShortName(),
				rts.getTrip().getHeadsignValue(),
				rts.getStop().getCode()
		);
	}

	@NonNull
	private static String getStopServiceUpdateTargetUUID(@NonNull String targetAuthority, @NonNull String routeShortName, @NonNull String tripHeadsignValue, @NonNull String stopCode) {
		return POI.POIUtils.getUUID(targetAuthority, routeShortName, tripHeadsignValue, stopCode);
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
		final Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		final RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		if (rts.getStop().getCode().isEmpty()
				|| rts.getRoute().getShortName().isEmpty()) {
			return null;
		}
		loadRealTimeStatusFromWWW(requireContextCompat(), rts, statusFilter, null);
		return getCachedStatus(statusFilter);
	}

	@Nullable
	@Override
	public ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (!(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getNewServiceUpdates() > no new service update (filter null or poi null or not RTS): %s", serviceUpdateFilter);
			return null;
		}
		final RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		if (rts.getTrip().getHeadsignValue().isEmpty()
				|| rts.getRoute().getShortName().isEmpty()) {
			MTLog.d(this, "getNewServiceUpdates() > skip (stop w/o code OR route w/o short name: %s)", rts);
			return null;
		}
		// USING same feed as real-time POI status schedule
		loadRealTimeStatusFromWWW(requireContextCompat(), rts, null, serviceUpdateFilter);
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
	private static final String REAL_TIME_URL_PART_5_BEFORE_LIMIT = FeatureFlags.F_ACCESSIBILITY_PRODUCER ? "&wheelchair=0"
			+ "&web_mips=1&limit="
			: "&web_mips=1&limit=";

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

	private static final ConcurrentHashMap<String, Object> synchronizedLock = new ConcurrentHashMap<>();

	private void loadRealTimeStatusFromWWW(@NonNull Context context,
										   @NonNull RouteTripStop rts,
										   @Nullable StatusProviderContract.Filter statusFilter,
										   @Nullable ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		final String uuid = rts.getUUID();
		synchronizedLock.putIfAbsent(uuid, uuid);
		synchronized (CollectionUtils.getOrDefault(synchronizedLock, uuid, uuid)) {
			if (statusFilter != null || !STORE_EMPTY_SERVICE_MESSAGE) { // IF is loading status OR empty service update not stored
				final POIStatus cachedStopStatus = StatusProvider.getCachedStatusS(this, getStopStatusTargetUUID(rts));
				if (cachedStopStatus != null) { // DO check status update
					MTLog.d(this, "loadRealTimeStatusFromWWW() > SKIP (status already in cache for %s)", uuid);
					return;
				}
			}
			if (serviceUpdateFilter != null && STORE_EMPTY_SERVICE_MESSAGE) { // IF loading service update AND storing empty service update
				final ArrayList<ServiceUpdate> cachedStopServiceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, getStopServiceUpdateTargetUUID(rts));
				if (cachedStopServiceUpdates != null && cachedStopServiceUpdates.size() > 0) { // DO check service update
					MTLog.d(this, "loadRealTimeStatusFromWWW() > SKIP (service update already in cache for %s)", uuid);
					return;
				}
			}
			loadRealTimeStatusFromWWW(context, rts);
		}
	}

	private void loadRealTimeStatusFromWWW(@NonNull Context context, @NonNull RouteTripStop rts) {
		try {
			final String urlString = getRealTimeStatusUrlString(rts);
			MTLog.i(this, "Loading from '%s'...", urlString);
			final URL url = new URL(urlString);
			final URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			urlc.addRequestProperty("Origin", "https://stm.info");
			urlc.addRequestProperty(ACCEPT, APPLICATION_JSON);
			final HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				final long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				final String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadRealTimeStatusFromWWW() > jsonString: %s.", jsonString);
				final JArrivals jArrivals = parseAgencyJSONArrivals(jsonString);
				final ArrayList<POIStatus> statuses = parseAgencyJSONArrivalsStatuses(
						context.getResources(),
						jArrivals.getResults(),
						rts,
						newLastUpdateInMs
				);
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(getStopStatusTargetUUID(rts)));
				if (statuses != null) {
					for (POIStatus status : statuses) {
						StatusProvider.cacheStatusS(this, status);
					}
				}
				MTLog.i(this, "Found %d schedule statuses for '%s'.", (statuses == null ? 0 : statuses.size()), rts.toStringShort());
				// if (org.mtransit.commons.Constants.DEBUG) {
				// if (statuses != null) {
				// for (POIStatus status : statuses) {
				// MTLog.d(this, "- %s", status.toString());
				// }
				// }
				// }
				final ArrayList<ServiceUpdate> serviceUpdates = parseAgencyJSONArrivalsServiceUpdates(
						context,
						jArrivals.getMessages(),
						rts,
						newLastUpdateInMs
				);
				MTLog.i(this, "Found %d service updates for '%s'.", serviceUpdates == null ? null : serviceUpdates.size(), rts.toStringShort());
				// if (org.mtransit.commons.Constants.DEBUG) {
				// if (serviceUpdates != null) {
				// for (ServiceUpdate serviceUpdate : serviceUpdates) {
				// MTLog.d(this, "- %s", serviceUpdate.toString());
				// }
				// }
				// }
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

	@Nullable
	private ArrayList<ServiceUpdate> parseAgencyJSONArrivalsServiceUpdates(@NonNull Context context,
																		   @NonNull JArrivals.JMessages jMessages,
																		   @NonNull RouteTripStop rts,
																		   long newLastUpdateInMs) {
		try {
			final long maxValidityInMs = getServiceUpdateMaxValidityInMs();
			final String language = getServiceUpdateLanguage();
			final ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<>();
			final String rtTargetUUID = getRouteServiceUpdateTargetUUID(rts);
			final String rtsTargetUUID = getStopServiceUpdateTargetUUID(rts);
			int rtsServiceUpdateAdded = 0;
			// ROUTE messages
			for (JArrivals.JMessages.JMessage line : jMessages.getLines()) {
				final int severity = ServiceUpdate.SEVERITY_INFO_RELATED_POI; // service updates target this route stops
				final String routeLink = makeRouteServiceUpdateLink(rts.getRoute());
				final String routeTitle = rts.getRoute().getShortName() + " " + rts.getTrip().getHeading(context);
				String text = line.text;
				text = HtmlUtils.fixTextViewBR(text); // remove <div/>
				//noinspection ConstantValue
				serviceUpdates.add(new ServiceUpdate(
						null,
						rtTargetUUID,
						newLastUpdateInMs,
						maxValidityInMs,
						ServiceUpdateCleaner.makeText(
								routeTitle,
								HtmlUtils.fromHtml(text)
						),
						ServiceUpdateCleaner.makeTextHTML(
								routeTitle,
								enhanceHtml(text, null, null), // no severity|stop based enhancement here,
								routeLink
						),
						severity,
						SERVICE_UPDATE_SOURCE_ID,
						SERVICE_UPDATE_SOURCE_LABEL,
						language
				));
			}
			// STOPS messages
			for (JArrivals.JMessages.JMessage stopPoint : jMessages.getStopPoints()) {
				final int severity = ServiceUpdate.SEVERITY_WARNING_POI;
				final String stopLink = null;
				final String stopTitle = rts.getStop().getCode() + " (" + rts.getStop().getName() + ")";
				String text = stopPoint.text;
				text = HtmlUtils.fixTextViewBR(text); // remove <div/>
				//noinspection ConstantValue
				serviceUpdates.add(new ServiceUpdate(
						null,
						rtsTargetUUID,
						newLastUpdateInMs,
						maxValidityInMs,
						ServiceUpdateCleaner.makeText(
								stopTitle,
								text
						),
						ServiceUpdateCleaner.makeTextHTML(
								stopTitle,
								enhanceHtml(text, null, null), // no severity|stop based enhancement here,
								stopLink
						),
						severity,
						SERVICE_UPDATE_SOURCE_ID,
						SERVICE_UPDATE_SOURCE_LABEL,
						language
				));
				rtsServiceUpdateAdded++;
			}
			if (STORE_EMPTY_SERVICE_MESSAGE && rtsServiceUpdateAdded == 0) {
				MTLog.d(this, "No messages found, return empty severity none message #ServiceUpdate");
				serviceUpdates.add(new ServiceUpdate(
						null,
						rtsTargetUUID, // mark service update for RTS as loaded
						newLastUpdateInMs,
						maxValidityInMs,
						null,
						null,
						ServiceUpdate.SEVERITY_NONE,
						SERVICE_UPDATE_SOURCE_ID,
						SERVICE_UPDATE_SOURCE_LABEL,
						language
				));
			}
			return serviceUpdates;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON results '%s'!", jMessages);
			return null;
		}
	}

	private static final String ROUTE_SERVICE_UPDATE_FORMAT_EN = "https://www.stm.info/en/info/service-updates/bus?bus_line=%s#status-bus-result-title";
	private static final String ROUTE_SERVICE_UPDATE_FORMAT_FR = "https://www.stm.info/fr/infos/etat-du-service/bus?bus_line=%s#status-bus-result-title";

	@Nullable
	private String makeRouteServiceUpdateLink(@NonNull Route route) {
		//noinspection ConstantValue
		if (true) {
			return null; // IGNORE: desktop links
		}
		try {
			if (LocaleUtils.isFR()) {
				return String.format(Locale.FRENCH, ROUTE_SERVICE_UPDATE_FORMAT_FR, route.getShortName());
			}
			return String.format(Locale.ENGLISH, ROUTE_SERVICE_UPDATE_FORMAT_EN, route.getShortName());
		} catch (Exception e) {
			MTLog.w(this, e, "Error while formatting link for route %s!", route);
			return null;
		}
	}

	private static final String REAL_TIME_SERVICE_UPDATE_URL_PART_1_BEFORE_LANG = "https://api.stm.info/pub/i3/v1c/api/";
	private static final String REAL_TIME_SERVICE_UPDATE_URL_PART_2_BEFORE_ROUTE_SHORT_NAME = "/lines/";
	private static final String REAL_TIME_SERVICE_UPDATE_URL_PART_3 = "/messages?type=Bus&web_mips=1";

	@Deprecated
	private static String getRealTimeServiceUpdateUrlString(@NonNull RouteTripStop rts) {
		return REAL_TIME_SERVICE_UPDATE_URL_PART_1_BEFORE_LANG + //
				(LocaleUtils.isFR() ? "fr" : "en") + //
				REAL_TIME_SERVICE_UPDATE_URL_PART_2_BEFORE_ROUTE_SHORT_NAME + //
				rts.getRoute().getShortName() + //
				REAL_TIME_SERVICE_UPDATE_URL_PART_3;
	}

	// USING same feed as real-time POI status schedule
	@SuppressWarnings({"DeprecatedIsStillUsed", "unused"})
	@Deprecated
	private void loadRealTimeServiceUpdateFromWWW(RouteTripStop rts) {
		try {
			String urlString = getRealTimeServiceUpdateUrlString(rts);
			URL url = new URL(urlString);
			MTLog.i(this, "Loading from '%s'...", url);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			urlc.addRequestProperty("Origin", "https://stm.info");
			urlc.addRequestProperty(ACCEPT, APPLICATION_JSON);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadRealTimeServiceUpdateFromWWW() > jsonString: %s.", jsonString);
				JMessages jMessages = parseAgencyJSONMessages(jsonString);
				List<JMessages.JResult> jResults = jMessages.getResults();
				ArrayList<ServiceUpdate> serviceUpdates = parseAgencyJSONMessageResults(
						jResults,
						rts, newLastUpdateInMs);
				MTLog.i(this, "Found %d service updates.", serviceUpdates == null ? null : serviceUpdates.size());
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

	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
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

	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
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
								MTLog.d(this, "Skip because not usable trip head-sign from '%s' or '%s'.", direction, directionName);
								continue;
							}
							if (!text.isEmpty()) {
								int severity = ServiceUpdate.SEVERITY_INFO_RELATED_POI; // service updates target this route stops
								if (JMessages.JResult.JResultRoute.CODE_NORMAL.equals(code)) {
									severity = ServiceUpdate.SEVERITY_NONE; // Normal service
								}
								String title = routeShortName + " " + directionName;
								String targetUUID = getRouteServiceUpdateTargetUUID(rts.getAuthority(), routeShortName, tripHeadsignValue);
								String fText = ServiceUpdateCleaner.makeText(
										title,
										text
								);
								String textHtml = ServiceUpdateCleaner.makeTextHTML(
										title,
										enhanceHtml(text, null, null) // no severity|stop based enhancement here
								);
								serviceUpdates.add(new ServiceUpdate(
										null,
										targetUUID,
										newLastUpdateInMs,
										maxValidityInMs,
										fText,
										textHtml,
										severity,
										SERVICE_UPDATE_SOURCE_ID,
										SERVICE_UPDATE_SOURCE_LABEL,
										language
								));
							}
						}
					}
				}
			}
			if (serviceUpdates.isEmpty()) {
				if (CollectionUtils.getSize(serviceUpdates) == 0) {
					MTLog.d(this, "No messages found, return empty severity none message  #ServiceUpdate");
					String targetUUID = getRouteServiceUpdateTargetUUID(rts.getAuthority(), rts.getRoute().getShortName(), rts.getTrip().getHeadsignValue());
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

	private static final String BUS_STOP = "bus stop\\S*";
	private static final String BUS_STOP_FR = "arr[ê|e]t\\S*";

	protected static final Cleaner STOP = new Cleaner("(" + BUS_STOP + ")", true);
	protected static final Cleaner STOP_FR = new Cleaner("(" + BUS_STOP_FR + ")", true);

	private static final String POINT = "\\.";
	private static final String PARENTHESES1 = "\\(";
	private static final String PARENTHESES2 = "\\)";
	private static final String SLASH = "/";
	private static final String ANY_STOP_CODE = oneOrMore(DIGIT_CAR);

	// this old complex regex does NOT work with:
	// 61611 (Station Berri-UQAM (1621 Berri))
	// 50110 (Gare Montréal-Ouest (Elmhurst / Sherbrooke))
	@SuppressWarnings("ConstantConditionalExpression") // DISABLED for now, default text display is good enough
	private static final Cleaner CLEAN_STOP_CODE_AND_NAME = new Cleaner(
			true ? group(ANY_STOP_CODE + zeroOrMore(except("<" + END)))
					: "(" + ANY_STOP_CODE + ")[\\s]*" + PARENTHESES1 + "([^" + SLASH + "]*)" + SLASH
					+ "([^" + PARENTHESES2 + "]*)" + PARENTHESES2 + "([" + PARENTHESES2 + "]*)" + "([,]*)([.]*)",
			true ? "- $1"
					: "- $2" + SLASH + "$3$4 " + PARENTHESES1 + "$1" + PARENTHESES2 + "$5$6",
			false
	);

	private static final Cleaner CLEAN_BR = new Cleaner(
			"(" + PARENTHESES2 + ",|" + POINT + "|:)\\s+",
			"$1" + HtmlUtils.BR,
			false
	);

	@SuppressWarnings("ConstantConditionalExpression") // DISABLED for now, default text display is good enough
	private static final String CLEAN_THAT_STOP_CODE_FORMAT =
			true ? group("%s" + zeroOrMore(except("<" + END)))
					: "(\\-[\\s]+)" + "([^" + SLASH + "]*" + SLASH + "[^" + PARENTHESES1 + "]*" + PARENTHESES1 + "%s" + PARENTHESES2 + ")";

	@Nullable
	@SuppressWarnings("SameParameterValue")
	private static String enhanceHtml(@Nullable String originalHtml, @Nullable RouteTripStop rts, @Nullable Integer severity) {
		if (originalHtml == null || originalHtml.isEmpty()) {
			return originalHtml;
		}
		try {
			String html = originalHtml;
			html = CLEAN_BR.clean(html);
			html = CLEAN_STOP_CODE_AND_NAME.clean(html);
			if (rts != null) {
				html = enhanceHtmlRts(rts, html, severity);
			}
			if (severity != null) {
				html = enhanceHtmlSeverity(severity, html);
			}
			return html;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while trying to enhance HTML '%s' (using original)!", originalHtml);
			return originalHtml;
		}
	}

	private static String enhanceHtmlRts(@NonNull RouteTripStop rts, String html, @Nullable Integer severity) {
		if (html == null || html.isEmpty()) {
			return html;
		}
		String replacement = "$1" + HtmlUtils.applyBold("$2");
		//noinspection ConstantValue // DISABLED for now, default text display is good enough
		if (true) {
			replacement = ServiceUpdateCleaner.getReplacement(severity);
		}
		if (replacement == null) {
			return html;
		}
		return new Cleaner(
				String.format(CLEAN_THAT_STOP_CODE_FORMAT, rts.getStop().getCode()),
				replacement
		).clean(html);
	}

	private static final Cleaner CLEAN_BOLD = ServiceUpdateCleaner.make(BUS_STOP);
	private static final Cleaner CLEAN_BOLD_FR = ServiceUpdateCleaner.make(BUS_STOP_FR);

	private static String enhanceHtmlSeverity(int severity, String html) {
		if (html == null || html.isEmpty()) {
			return html;
		}
		final String replacement = ServiceUpdateCleaner.getReplacement(severity);
		if (replacement != null) {
			html = ServiceUpdateCleaner.clean(html, replacement);
			return (LocaleUtils.isFR() ? CLEAN_BOLD_FR : CLEAN_BOLD).clean(html, replacement);
		}
		return html;
	}

	protected int findRTSSeverity(@Nullable String text, @NonNull RouteTripStop rts, @NonNull Cleaner stop) {
		if (text != null && !text.isEmpty()) {
			if (text.contains(rts.getStop().getCode())) {
				return ServiceUpdate.SEVERITY_WARNING_POI;
			} else if (stop.find(text)) {
				return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
			}
		}
		MTLog.d(this, "findRTSSeverity() > Cannot find RTS severity for '%s'. (%s)", text, rts);
		return ServiceUpdate.SEVERITY_INFO_UNKNOWN;
	}

	@Nullable
	protected ArrayList<POIStatus> parseAgencyJSONArrivalsStatuses(@NonNull Resources res,
																   @NonNull List<JArrivals.JResult> jResults,
																   @NonNull RouteTripStop rts,
																   long newLastUpdateInMs) {
		try {
			final ArrayList<POIStatus> statuses = new ArrayList<>();
			Calendar nowCal = Calendar.getInstance(MONTREAL_TZ);
			nowCal.setTimeInMillis(newLastUpdateInMs);
			nowCal.add(Calendar.HOUR_OF_DAY, -1);
			boolean hasRealTime = false;
			boolean hasCongestion = false;
			boolean hasOnRequestedDay = false;
			final Schedule newSchedule = new Schedule(
					getStopStatusTargetUUID(rts),
					newLastUpdateInMs,
					getStatusMaxValidityInMs(),
					newLastUpdateInMs,
					PROVIDER_PRECISION_IN_MS,
					false);
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
					if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
						timestamp.setAccessible(jResult.isRampCancelled ? Accessibility.NOT_POSSIBLE : Accessibility.POSSIBLE);
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
				statuses.add(newSchedule);
			} // ELSE => dismissed because only returned planned schedule data which isn't trustworthy #767
			return statuses;
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
	private static final String JSON_STOP_POINT = "stopPoint";
	private static final String JSON_START_DATE = "start_date";
	private static final String JSON_TEXT = "text";
	private static final String JSON_CODE = "code";
	private static final String JSON_RESULT = "result";
	private static final String JSON_TIME = "time";
	private static final String JSON_IS_REAL = "is_real";
	private static final String JSON_IS_CONGESTION = "is_congestion";
	private static final String JSON_IS_RAMP_CANCELLED = "is_ramp_cancelled";
	private static final String JSON_IS_ON_REQUESTED_DAY = "is_on_requested_day";

	@NonNull
	private JArrivals parseAgencyJSONArrivals(@Nullable String jsonString) {
		List<JArrivals.JMessages.JMessage> lines = new ArrayList<>();
		List<JArrivals.JMessages.JMessage> stopPoints = new ArrayList<>();
		List<JArrivals.JResult> results = new ArrayList<>();
		try {
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			parseAgencyJSONArrivalsMessages(lines, stopPoints, json);
			parseAgencyJSONArrivalsResults(results, json);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
		}
		return new JArrivals(new JArrivals.JMessages(lines, stopPoints), results);
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
							boolean isRampCancelled = false;
							if (jResult.has(JSON_IS_RAMP_CANCELLED)) {
								isRampCancelled = jResult.getBoolean(JSON_IS_RAMP_CANCELLED);
							}
							boolean isOnRequestedDay = true;
							if (jResult.has(JSON_IS_ON_REQUESTED_DAY)) {
								isOnRequestedDay = jResult.getBoolean(JSON_IS_ON_REQUESTED_DAY);
							}
							results.add(new JArrivals.JResult(jTime, isReal, isCongestion, isRampCancelled, isOnRequestedDay));
						}
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", json);
		}
	}

	private void parseAgencyJSONArrivalsMessages(
			@NonNull List<JArrivals.JMessages.JMessage> lines,
			@NonNull List<JArrivals.JMessages.JMessage> stopPoints,
			@Nullable JSONObject json) {
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
								lines.add(new JArrivals.JMessages.JMessage(jText, jStartDate));
							}
						}
					}
				}
				if (jMessages != null && jMessages.has(JSON_STOP_POINT)) {
					JSONArray jStopPoints = jMessages.getJSONArray(JSON_STOP_POINT);
					for (int l = 0; l < jStopPoints.length(); l++) {
						JSONObject jStopPoint = jStopPoints.getJSONObject(l);
						if (jStopPoint != null && jStopPoint.has(JSON_TEXT)) {
							String jText = jStopPoint.getString(JSON_TEXT);
							if (!jText.isEmpty()) {
								String jStartDate = jStopPoint.optString(JSON_START_DATE);
								stopPoints.add(new JArrivals.JMessages.JMessage(jText, jStartDate));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", json);
		}
	}

	@MainThread
	@Override
	public boolean onCreateMT() {
		ping();
		updateSecurityProviderIfNeeded(getContext()); // cannot call requireContext() in onCreate()
		return true;
	}

	@Override
	public void ping() {
		// DO NOTHING
	}

	@MainThread
	private void updateSecurityProviderIfNeeded(@Nullable Context context) {
		try {
			if (context != null) {
				ProviderInstaller.installIfNeededAsync(context, this);
			}
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
		return StmInfoApiDbHelper.getDbVersion(requireContextCompat());
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
			private final boolean isRampCancelled;
			private final boolean isOnRequestedDay;

			public JResult(@NonNull String time, boolean isReal, boolean isCongestion, boolean isRampCancelled, boolean isOnRequestedDay) {
				this.time = time;
				this.isReal = isReal;
				this.isCongestion = isCongestion;
				this.isRampCancelled = isRampCancelled;
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

			public boolean isRampCancelled() {
				return isRampCancelled;
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
						"isRampCancelled=" + isRampCancelled + "," +
						"isOnRequestedDay=" + isOnRequestedDay + "," +
						'}';
			}
		}

		public static class JMessages {
			@NonNull
			private final List<JMessage> lines;

			@NonNull
			private final List<JMessage> stopPoints;

			public JMessages(@NonNull List<JMessage> lines, @NonNull List<JMessage> stopPoints) {
				this.lines = lines;
				this.stopPoints = stopPoints;
			}

			@NonNull
			public List<JMessage> getLines() {
				return lines;
			}

			@NonNull
			public List<JMessage> getStopPoints() {
				return stopPoints;
			}

			@NonNull
			@Override
			public String toString() {
				return "JMessages{" +
						"lines=" + lines +
						", stopPoints=" + stopPoints +
						'}';
			}

			public static class JMessage {
				@NonNull
				private final String text;
				@NonNull
				private final String startDate;

				public JMessage(@NonNull String text, @NonNull String startDate) {
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
					return "JMessage{" +
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
