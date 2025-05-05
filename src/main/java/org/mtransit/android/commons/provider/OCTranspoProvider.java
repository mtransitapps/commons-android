package org.mtransit.android.commons.provider;

import static org.mtransit.android.commons.StringUtils.EMPTY;

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
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Accessibility;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.helpers.MTDefaultHandler;
import org.mtransit.android.commons.provider.OCTranspoProvider.JGetNextTripsForStop.JGetNextTripsForStopResult.JRoute.JRouteDirection;
import org.mtransit.android.commons.provider.OCTranspoProvider.JGetNextTripsForStop.JGetNextTripsForStopResult.JRoute.JRouteDirection.JTrips.JTrip;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.Cleaner;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.Constants;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.RegexUtils;
import org.mtransit.commons.SourceUtils;
import org.mtransit.commons.provider.OttawaOCTranspoProviderCommons;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

// https://www.octranspo.com/en/plan-your-trip/travel-tools/developers/dev-doc
// https://www.octranspo.com/en/plan-your-trip/travel-tools/developers/legacy-oc-transpo-api-2.0
// https://octranspo-new.3scale.net/
// "API 2.0 is fully deprecated in Q1, 2025"
@SuppressWarnings({"deprecation", "DeprecatedIsStillUsed"})
@Deprecated
@SuppressLint("Registered")
public class OCTranspoProvider extends MTContentProvider implements StatusProviderContract, ServiceUpdateProviderContract {

	private static final String LOG_TAG = OCTranspoProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS = OCTranspoDbHelper.PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS;

	@NonNull
	public static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		ServiceUpdateProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
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
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.oc_transpo_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String serviceUpdateTargetAuthority = null;

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	@NonNull
	private static String getSERVICE_UPDATE_TARGET_AUTHORITY(@NonNull Context context) {
		if (serviceUpdateTargetAuthority == null) {
			serviceUpdateTargetAuthority = context.getResources().getString(R.string.oc_transpo_service_update_for_poi_authority);
		}
		return serviceUpdateTargetAuthority;
	}

	@Nullable
	private static String appId = null;

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	@NonNull
	private static String getAPP_ID(@NonNull Context context) {
		if (appId == null) {
			appId = context.getResources().getString(R.string.oc_transpo_app_id);
		}
		return appId;
	}

	@Nullable
	private static String apiKey = null;

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	@NonNull
	private static String getAPI_KEY(@NonNull Context context) {
		if (apiKey == null) {
			apiKey = context.getResources().getString(R.string.oc_transpo_api_key);
		}
		return apiKey;
	}

	@Nullable
	private static String timeZone = null;

	/**
	 * Override if multiple {@link GTFSStatusProvider} implementations in same app.
	 */
	@NonNull
	static String getTIME_ZONE(@NonNull Context context) {
		if (timeZone == null) {
			timeZone = context.getResources().getString(R.string.gtfs_rts_timezone);
		}
		return timeZone;
	}

	private static final long LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getStatusMaxValidityInMs() {
		return LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Nullable
	@Override
	public POIStatus getCachedStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, rts.getUUID());
		if (cachedStatus != null) {
			if (rts.isNoPickup()) {
				if (cachedStatus instanceof Schedule) {
					Schedule schedule = (Schedule) cachedStatus;
					schedule.setNoPickup(true); // API doesn't know about "descent only" (do not returns result for drop off only but the other way instead)
				}
			}
		}
		return cachedStatus;
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
		return OCTranspoDbHelper.T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	@Nullable
	@Override
	public POIStatus getNewStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		loadPredictionsFromWWW(requireContextCompat(), rts);
		return getCachedStatus(statusFilter);
	}

	// curl -d "appID={appID}&apiKey={apiKey}&routeNo=1&stopNo=7659" https://api.octranspo1.com/v2.0/GetNextTripsForStop
	// https://api.octranspo1.com/v2.0/GetNextTripsForStop?appID={appID}&apiKey={apiKey}&stopNo=3023&routeNo=1&format=json
	// https://www.octranspo.com/en/plan-your-trip/travel-tools/developers/dev-doc#method-GetNextTripsForStop
	// private static final String GET_NEXT_TRIPS_FOR_STOP_URL = "https://api.octranspo1.com/v2.0/GetNextTripsForStop";
	private static final String GET_NEXT_TRIPS_FOR_STOP_URL = "https://api.octranspo1.com/v2.0/GetNextTripsForStop";

	private static final String URL_POST_PARAM_APP_ID = "appID";
	private static final String URL_POST_PARAM_APP_KEY = "apiKey";
	private static final String URL_POST_PARAM_ROUTE_NUMBER = "routeNo";
	private static final String URL_POST_PARAM_STOP_NUMBER = "stopNo";

	private static final String URL_POST_PARAM_FORMAT = "format";
	private static final String URL_POST_PARAM_FORMAT_JSON = "json";

	private static String getRouteStopPredictionsUrl(@NonNull Context context, @NonNull RouteTripStop rts) {
		return GET_NEXT_TRIPS_FOR_STOP_URL + "?" +//
				URL_POST_PARAM_APP_ID + HtmlUtils.URL_PARAM_EQ + getAPP_ID(context) + //
				HtmlUtils.URL_PARAM_AND + //
				URL_POST_PARAM_APP_KEY + HtmlUtils.URL_PARAM_EQ + getAPI_KEY(context) + //
				HtmlUtils.URL_PARAM_AND + //
				URL_POST_PARAM_ROUTE_NUMBER + HtmlUtils.URL_PARAM_EQ + rts.getRoute().getShortName() + //
				HtmlUtils.URL_PARAM_AND + //
				URL_POST_PARAM_STOP_NUMBER + HtmlUtils.URL_PARAM_EQ + rts.getStop().getCode() + //
				HtmlUtils.URL_PARAM_AND + //
				URL_POST_PARAM_FORMAT + HtmlUtils.URL_PARAM_EQ + URL_POST_PARAM_FORMAT_JSON
				;
	}

	private void loadPredictionsFromWWW(@NonNull Context context, @NonNull RouteTripStop rts) {
		try {
			MTLog.i(this, "Loading from '%s' for stop '%s' & route '%s'...", GET_NEXT_TRIPS_FOR_STOP_URL, rts.getStop().getCode(), rts.getRoute().getShortestName());
			final URL url = new URL(getRouteStopPredictionsUrl(context, rts));
			final String sourceLabel = SourceUtils.getSourceLabel(GET_NEXT_TRIPS_FOR_STOP_URL);
			final URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			HttpsURLConnection httpUrlConnection = (HttpsURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				final long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				final String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadPredictionsFromWWW() > jsonString: %s.", jsonString);
				JGetNextTripsForStop jGetNextTripsForStop = parseAgencyJSONArrivals(jsonString);
				final Collection<POIStatus> statuses = parseAgencyJSONArrivalsResults(context, jGetNextTripsForStop, rts, sourceLabel, newLastUpdateInMs);
				MTLog.i(this, "Loaded %d statuses.", (statuses == null ? -1 : statuses.size()));
				// if (Constants.DEBUG) {
				// 	if (statuses != null) {
				// 		for (POIStatus status : statuses) {
				// 			MTLog.d(this, "loadPredictionsFromWWW() > - %s.", status);
				// 		}
				// 	}
				// }
				StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(rts.getUUID()));
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

	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10L);

	private static final String DATE_FORMAT_PATTERN = "yyyyMMddHHmmss";
	@Nullable
	private static ThreadSafeDateFormatter dateFormat;

	@NonNull
	static ThreadSafeDateFormatter getDateFormat(@NonNull Context context) {
		if (dateFormat == null) {
			dateFormat = new ThreadSafeDateFormatter(DATE_FORMAT_PATTERN, Locale.ENGLISH);
			dateFormat.setTimeZone(TimeZone.getTimeZone(getTIME_ZONE(context)));
		}
		return dateFormat;
	}

	@Nullable
	protected Collection<POIStatus> parseAgencyJSONArrivalsResults(@NonNull Context context,
																   @NonNull JGetNextTripsForStop jGetNextTripsForStop,
																   @NonNull RouteTripStop rts,
																   @Nullable String sourceLabel,
																   long lastUpdateInMs) {
		try {
			final String localeTimeZoneId = getTIME_ZONE(context);
			ArrayList<POIStatus> result = new ArrayList<>();
			final String tripHeading = rts.getTrip().getHeading(context);
			final List<JRouteDirection> jRouteDirections = jGetNextTripsForStop.jGetNextTripsForStopResult.jRoute.jRouteDirections;
			JRouteDirection theJRouteDirection = selectDirection(rts, tripHeading, jRouteDirections);
			if (theJRouteDirection == null) {
				MTLog.d(this, "Skip because no route direction for %s with heading '%s' in %d JSON list.",
						rts.getUUID(), tripHeading, jRouteDirections.size());
				return result;
			}
			// API does not return last stop of trip (drop off only)
			Schedule schedule = new Schedule(
					null,
					rts.getUUID(),
					lastUpdateInMs,
					getStatusMaxValidityInMs(),
					lastUpdateInMs,
					PROVIDER_PRECISION_IN_MS,
					false,
					sourceLabel,
					false
			);
			String jRequestProcessingTime = theJRouteDirection.jRequestProcessingTime;
			if (jRequestProcessingTime == null || jRequestProcessingTime.isEmpty()) {
				MTLog.w(this, "Skip empty request processing time '%s'!", jRequestProcessingTime);
				return result;
			}
			final Date date = getDateFormat(context).parseThreadSafe(jRequestProcessingTime);
			if (date == null) {
				MTLog.w(this, "Skip un read-able date '%s'!", jRequestProcessingTime);
				return result;
			}
			long requestProcessingTimeInMs = date.getTime();
			requestProcessingTimeInMs = TimeUtils.timeToTheTensSecondsMillis(requestProcessingTimeInMs);
			HashSet<String> processedTrips = new HashSet<>();
			for (JTrip jTrip : theJRouteDirection.getJTripList()) {
				String jAdjustedScheduleTime = jTrip.jAdjustedScheduleTime;
				if (jAdjustedScheduleTime == null || jAdjustedScheduleTime.isEmpty()) {
					MTLog.w(this, "Skip empty request processing time '%s'!", jAdjustedScheduleTime);
					continue;
				}
				long t = requestProcessingTimeInMs + TimeUnit.MINUTES.toMillis(Long.parseLong(jAdjustedScheduleTime));
				Schedule.Timestamp newTimestamp = new Schedule.Timestamp(t, localeTimeZoneId);
				try {
					String tripDestination = jTrip.jTripDestination;
					if (tripDestination != null && !tripDestination.isEmpty()) {
						newTimestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING,
								cleanTripHeadsign(tripDestination, tripHeading)
						);
					}
				} catch (Exception e) {
					MTLog.w(this, e, "Error while adding trip destination %s!", jTrip.jTripDestination);
				}
				if (processedTrips.contains(newTimestamp.toString())) {
					continue;
				}
				newTimestamp.setRealTime(jTrip.isRealTime());
				if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
					newTimestamp.setAccessible(Accessibility.POSSIBLE); // ALL "buses and the O-Train are fully accessible" https://www.octranspo.com/en/our-services/accessibility/
				}
				schedule.addTimestampWithoutSort(newTimestamp);
				processedTrips.add(newTimestamp.toString());
			}
			result.add(schedule);
			return result;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON results '%s'!", jGetNextTripsForStop);
			return null;
		}
	}

	@Nullable
	private JRouteDirection selectDirection(@NonNull RouteTripStop rts, String tripHeading, List<JRouteDirection> jRouteDirections) {
		if (jRouteDirections.isEmpty()) {
			return null;
		}
		boolean hasRealTime = false;
		for (JRouteDirection jRouteDirection : jRouteDirections) {
			if (jRouteDirection.hasRealTime()) {
				hasRealTime = true;
				break;
			}
		}
		JRouteDirection theJRouteDirection = null;
		for (JRouteDirection jRouteDirection : jRouteDirections) {
			if (StringUtils.equals(jRouteDirection.jRouteLabel, tripHeading)) {
				theJRouteDirection = jRouteDirection;
				break;
			}
			for (JTrip jTrip : jRouteDirection.getJTripList()) {
				if (StringUtils.equals(jTrip.jTripDestination, tripHeading)) {
					theJRouteDirection = jRouteDirection;
					break;
				}
			}
		}
		if (theJRouteDirection == null && hasRealTime) {
			MTLog.w(this, "Unable to select proper route directions for '%s' (use 1st)!", rts);
			theJRouteDirection = jRouteDirections.get(0); // use this direction (even if it might be the other one #NoPickup)
		}
		return theJRouteDirection;
	}

	@NonNull
	private String cleanTripHeadsign(@NonNull String tripHeadSign, @Nullable String optRTSTripHeadSign) {
		try {
			if (!TextUtils.isEmpty(optRTSTripHeadSign)
					&& Trip.isSameHeadsign(optRTSTripHeadSign, tripHeadSign)) {
				return tripHeadSign; // not cleaned in data parser => keep same as route trip head sign
			}
			tripHeadSign = OttawaOCTranspoProviderCommons.cleanTripHeadsign(tripHeadSign);
			if (!TextUtils.isEmpty(optRTSTripHeadSign)
					&& Trip.isSameHeadsign(optRTSTripHeadSign, tripHeadSign)) {
				return tripHeadSign; // not cleaned in data parser => keep same as route trip head sign
			}
			String to = CleanUtils.keepTo(tripHeadSign);
			if (!TextUtils.isEmpty(optRTSTripHeadSign)
					&& Trip.isSameHeadsign(optRTSTripHeadSign, to)) {
				tripHeadSign = CleanUtils.keepVia(tripHeadSign, true); // same to, keep via
			}
			return tripHeadSign;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadSign);
			return tripHeadSign;
		}
	}

	@NonNull
	private JGetNextTripsForStop parseAgencyJSONArrivals(String jsonString) {
		JSONObject jGetNextTripsForStop = null;
		try {
			jGetNextTripsForStop = jsonString == null ? null : new JSONObject(jsonString);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
		}
		return JGetNextTripsForStop.parseJSON(jGetNextTripsForStop);
	}

	private static final long SERVICE_UPDATE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L);
	private static final long SERVICE_UPDATE_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getMinDurationBetweenServiceUpdateRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public long getServiceUpdateMaxValidityInMs() {
		return SERVICE_UPDATE_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getServiceUpdateValidityInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_VALIDITY_IN_MS;
	}

	@NonNull
	@Override
	public String getServiceUpdateDbTableName() {
		return OCTranspoDbHelper.T_OC_TRANSPO_SERVICE_UPDATE;
	}

	@Override
	public void cacheServiceUpdates(@NonNull ArrayList<ServiceUpdate> newServiceUpdates) {
		ServiceUpdateProvider.cacheServiceUpdatesS(this, newServiceUpdates);
	}

	@Nullable
	@Override
	public ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (!(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getCachedServiceUpdates() > no service update (poi null or not RTS)");
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		ArrayList<ServiceUpdate> serviceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, getTargetUUIDs(rts));
		enhanceRTServiceUpdateForStop(serviceUpdates, rts); // convert to stop service update
		return serviceUpdates;
	}

	private void enhanceRTServiceUpdateForStop(ArrayList<ServiceUpdate> serviceUpdates, RouteTripStop rts) {
		try {
			if (CollectionUtils.getSize(serviceUpdates) > 0) {
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					serviceUpdate.setTargetUUID(rts.getUUID()); // route trip service update targets stop
					enhanceRTServiceUpdateForStop(serviceUpdate, rts);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update for stop!");
		}
	}

	private static final String CLEAN_THAT_STOP_CODE = "(#%s \\-\\- [^\\<]*)";

	private void enhanceRTServiceUpdateForStop(ServiceUpdate serviceUpdate, RouteTripStop rts) {
		try {
			if (serviceUpdate.getText().contains(rts.getStop().getCode())) {
				if (ServiceUpdate.isSeverityWarning(serviceUpdate.getSeverity())) {
					serviceUpdate.setSeverity(ServiceUpdate.SEVERITY_WARNING_POI);
				} else {
					serviceUpdate.setSeverity(ServiceUpdate.SEVERITY_INFO_POI);
				}
				String replacement = ServiceUpdateCleaner.getReplacement(serviceUpdate.getSeverity());
				if (replacement != null) {
					serviceUpdate.setTextHTML(
							new Cleaner(
									String.format(CLEAN_THAT_STOP_CODE, rts.getStop().getCode()),
									replacement
							).clean(serviceUpdate.getTextHTML())
					);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update '%s' for stop!", serviceUpdate);
		}
	}

	@NonNull
	private HashSet<String> getTargetUUIDs(@NonNull RouteTripStop rts) {
		HashSet<String> targetUUIDs = new HashSet<>();
		targetUUIDs.add(getAgencyTargetUUID(rts.getAuthority()));
		targetUUIDs.add(getAgencyRouteShortNameTargetUUID(rts.getAuthority(), rts.getRoute().getShortName()));
		return targetUUIDs;
	}

	@NonNull
	protected static String getAgencyRouteShortNameTargetUUID(@NonNull String agencyAuthority, @NonNull String routeShortName) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName);
	}

	@NonNull
	protected static String getAgencyTargetUUID(@NonNull String agencyAuthority) {
		return POI.POIUtils.getUUID(agencyAuthority);
	}

	@Override
	public boolean purgeUselessCachedServiceUpdates() {
		return ServiceUpdateProvider.purgeUselessCachedServiceUpdates(this);
	}

	@Override
	public boolean deleteCachedServiceUpdate(@NonNull Integer serviceUpdateId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, serviceUpdateId);
	}

	@Override
	public boolean deleteCachedServiceUpdate(@NonNull String targetUUID, @NonNull String sourceId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, targetUUID, sourceId);
	}

	@SuppressWarnings("UnusedReturnValue")
	private int deleteAllAgencyServiceUpdateData() {
		int affectedRows = 0;
		try {
			String selection = SqlUtils.getWhereEqualsString(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_ID, AGENCY_SOURCE_ID);
			affectedRows = getWriteDB().delete(getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all agency service update data!");
		}
		return affectedRows;
	}

	@Nullable
	@Override
	public ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (!(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getNewServiceUpdates() > no new service update (filter null or poi null or not RTS): %s", serviceUpdateFilter);
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		updateAgencyServiceUpdateDataIfRequired(requireContextCompat(), rts.getAuthority(), serviceUpdateFilter.isInFocusOrDefault());
		ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(serviceUpdateFilter);
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			String agencyTargetUUID = getAgencyTargetUUID(rts.getAuthority());
			cachedServiceUpdates = ArrayUtils.asArrayList(getServiceUpdateNone(agencyTargetUUID));
			enhanceRTServiceUpdateForStop(cachedServiceUpdates, rts); // convert to stop service update
		}
		return cachedServiceUpdates;
	}

	@NonNull
	private ServiceUpdate getServiceUpdateNone(@NonNull String agencyTargetUUID) {
		return new ServiceUpdate(null, agencyTargetUUID, TimeUtils.currentTimeMillis(), getServiceUpdateMaxValidityInMs(), null, null,
				ServiceUpdate.SEVERITY_NONE, AGENCY_SOURCE_ID, EMPTY, getServiceUpdateLanguage());
	}

	@NonNull
	@Override
	public String getServiceUpdateLanguage() {
		return LocaleUtils.isFR() ? Locale.FRENCH.getLanguage() : Locale.ENGLISH.getLanguage();
	}

	private static final String AGENCY_SOURCE_ID = "octranspo_com_feeds_updates";

	private void updateAgencyServiceUpdateDataIfRequired(@NonNull Context context, String targetAuthority, boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS, 0L);
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyServiceUpdateDataIfRequiredSync(context, targetAuthority, lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyServiceUpdateDataIfRequiredSync(@NonNull Context context, String targetAuthority, long lastUpdateInMs, boolean inFocus) {
		if (PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS, 0L) > lastUpdateInMs) {
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		//noinspection RedundantIfStatement
		if (lastUpdateInMs + getServiceUpdateMaxValidityInMs() < nowInMs) {
			deleteAllRequired = true; // too old to display
		}
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		if (deleteAllRequired || lastUpdateInMs + minUpdateMs < nowInMs) {
			updateAllAgencyServiceUpdateDataFromWWW(context, targetAuthority, deleteAllRequired); // try to update
		}
	}

	private void updateAllAgencyServiceUpdateDataFromWWW(@NonNull Context context, String targetAuthority, boolean deleteAllRequired) {
		boolean deleteAllDone = false;
		if (deleteAllRequired) {
			deleteAllAgencyServiceUpdateData();
			deleteAllDone = true;
		}
		ArrayList<ServiceUpdate> newServiceUpdates = loadAgencyServiceUpdateDataFromWWW(targetAuthority);
		if (newServiceUpdates != null) { // empty is OK
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyServiceUpdateData();
			}
			cacheServiceUpdates(newServiceUpdates);
			PreferenceUtils.savePrefLclSync(context, PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS, nowInMs);
		} // else keep whatever we have until max validity reached
	}

	private static final String AGENCY_URL_PART_1_BEFORE_LANG = "https://www.octranspo.com/";
	private static final String AGENCY_URL_PART_2_AFTER_LANG1 = "/feeds/updates-";
	private static final String AGENCY_URL_PART_3_AFTER_LANG2 = "/";

	private static final String AGENCY_URL_LANG_DEFAULT = "en";
	private static final String AGENCY_URL_LANG_FRENCH = "fr";

	private static String getAgencyUrlString() {
		return AGENCY_URL_PART_1_BEFORE_LANG + //
				(LocaleUtils.isFR() ? AGENCY_URL_LANG_FRENCH : AGENCY_URL_LANG_DEFAULT) + // language
				AGENCY_URL_PART_2_AFTER_LANG1 + //
				(LocaleUtils.isFR() ? AGENCY_URL_LANG_FRENCH : AGENCY_URL_LANG_DEFAULT) + // language
				AGENCY_URL_PART_3_AFTER_LANG2
				;
	}

	private ArrayList<ServiceUpdate> loadAgencyServiceUpdateDataFromWWW(@SuppressWarnings("unused") String targetAuthority) {
		try {
			final String urlString = getAgencyUrlString();
			final URL url = new URL(urlString);
			MTLog.i(this, "Loading from '%s'...", url);
			final String sourceLabel = SourceUtils.getSourceLabel(AGENCY_URL_PART_1_BEFORE_LANG);
			final URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			final HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				final long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				final SAXParserFactory spf = SAXParserFactory.newInstance();
				final SAXParser sp = spf.newSAXParser();
				final XMLReader xr = sp.getXMLReader();
				final OCTranspoFeedsUpdatesDataHandler handler = new OCTranspoFeedsUpdatesDataHandler(
						httpUrlConnection.getURL(),
						getSERVICE_UPDATE_TARGET_AUTHORITY(requireContextCompat()),
						sourceLabel,
						newLastUpdateInMs,
						getServiceUpdateMaxValidityInMs(),
						getServiceUpdateLanguage()
				);
				xr.setContentHandler(handler);
				xr.parse(new InputSource(urlc.getInputStream()));
				final ArrayList<ServiceUpdate> newServiceUpdates = handler.getServiceUpdates();
				MTLog.i(this, "Found %d service updates.", newServiceUpdates.size());
				if (Constants.DEBUG) {
					for (ServiceUpdate serviceUpdate : newServiceUpdates) {
						MTLog.d(this, "- %s", serviceUpdate.toString());
					}
				}
				return newServiceUpdates;
			default:
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpUrlConnection.getResponseCode(),
						httpUrlConnection.getResponseMessage());
				return null;
			}
		} catch (UnknownHostException uhe) {
			if (MTLog.isLoggable(android.util.Log.DEBUG)) {
				MTLog.w(this, uhe, "No Internet Connection!");
			} else {
				MTLog.w(this, "No Internet Connection!");
			}
			return null;
		} catch (SocketException se) {
			MTLog.w(LOG_TAG, se, "No Internet Connection!");
			return null;
		} catch (Exception e) {
			MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
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
	private OCTranspoDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private OCTranspoDbHelper getDBHelper(@NonNull Context context) {
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
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return OCTranspoDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link OCTranspoProvider} implementations in same app.
	 */
	@NonNull
	public OCTranspoDbHelper getNewDbHelper(@NonNull Context context) {
		return new OCTranspoDbHelper(context.getApplicationContext());
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
		cursor = ServiceUpdateProvider.queryS(this, uri, selection);
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
		type = ServiceUpdateProvider.getTypeS(this, uri);
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

	private static class OCTranspoFeedsUpdatesDataHandler extends MTDefaultHandler {

		private static final String LOG_TAG = OCTranspoProvider.LOG_TAG + ">" + OCTranspoFeedsUpdatesDataHandler.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private static final String RSS = "rss";
		private static final String CHANNEL = "channel";
		private static final String TITLE = "title";
		private static final String LINK = "link";
		private static final String DESCRIPTION = "description";
		private static final String ATOM_LINK = "atom:link";
		private static final String ITEM = "item";
		private static final String PUBLICATION_DATE = "pubDate";
		private static final String CATEGORY = "category";
		private static final String GUID = "guid";

		private String currentLocalName = RSS;
		private boolean currentItem = false;
		private final StringBuilder currentTitleSb = new StringBuilder();
		@Nullable
		private String currentCategory1;
		@Nullable
		private String currentCategory2;
		private final StringBuilder currentLinkSb = new StringBuilder();
		private final StringBuilder currentDescriptionSb = new StringBuilder();

		@NonNull
		private final ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<>();

		private final URL fromURL;
		private final String targetAuthority;
		@NonNull
		private final String sourceLabel;
		private final long newLastUpdateInMs;
		private final long serviceUpdateMaxValidityInMs;
		private final String language;

		OCTranspoFeedsUpdatesDataHandler(URL fromURL,
										 String targetAuthority,
										 @NonNull String sourceLabel,
										 long newLastUpdateInMs,
										 long serviceUpdateMaxValidityInMs,
										 String language) {
			this.fromURL = fromURL;
			this.targetAuthority = targetAuthority;
			this.sourceLabel = sourceLabel;
			this.newLastUpdateInMs = newLastUpdateInMs;
			this.serviceUpdateMaxValidityInMs = serviceUpdateMaxValidityInMs;
			this.language = language;
		}

		@NonNull
		ArrayList<ServiceUpdate> getServiceUpdates() {
			return this.serviceUpdates;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			this.currentLocalName = localName;
			if (ITEM.equals(this.currentLocalName)) {
				this.currentItem = true;
				this.currentTitleSb.setLength(0); // reset
				this.currentCategory1 = null; // reset
				this.currentCategory2 = null; // reset
				this.currentLinkSb.setLength(0); // reset
				this.currentDescriptionSb.setLength(0); // reset
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);
			try {
				String string = new String(ch, start, length);
				if (TextUtils.isEmpty(string)) {
					return;
				}
				if (this.currentItem) {
					if (TITLE.equals(this.currentLocalName)) {
						this.currentTitleSb.append(string);
					} else if (PUBLICATION_DATE.equals(this.currentLocalName)) { // ignore
					} else if (CATEGORY.equals(this.currentLocalName)) {
						if (!TextUtils.isEmpty(string.trim())) {
							if (TextUtils.isEmpty(this.currentCategory1)) {
								this.currentCategory1 = string;
							} else {
								this.currentCategory2 = string;
							}
						}
					} else if (LINK.equals(this.currentLocalName)) {
						this.currentLinkSb.append(string);
					} else if (DESCRIPTION.equals(this.currentLocalName)) {
						this.currentDescriptionSb.append(string);
					} else if (GUID.equals(this.currentLocalName)) { // ignore
					} else if (ITEM.equals(this.currentLocalName)) { // ignore
					} else {
						MTLog.w(this, "characters() > Unexpected item element '%s'", this.currentLocalName);
					}
				} else if (TITLE.equals(this.currentLocalName)) { // ignore
				} else if (LINK.equals(this.currentLocalName)) { // ignore
				} else if (DESCRIPTION.equals(this.currentLocalName)) { // ignore
				} else if (ATOM_LINK.equals(this.currentLocalName)) { // ignore
				} else if (RSS.equals(this.currentLocalName)) { // ignore
				} else if (CHANNEL.equals(this.currentLocalName)) { // ignore
				} else if (GUID.equals(this.currentLocalName)) { // ignore
				} else {
					MTLog.w(this, "characters() > Unexpected element '%s'", this.currentLocalName);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing '%s' value '%s, %s, %s'!", this.currentLocalName, ch, start, length);
			}
		}

		private static final String DETOURS = "Detours";
		private static final String DELAYS = "Delays";
		private static final String CANCELLED_TRIPS = "Cancelled trips";
		private static final String TODAY_S_SERVICE = "Todays Service";
		private static final String ROUTE_SERVICE_CHANGE = "Route Service Change";
		private static final String GENERAL_SERVICE_CHANGE = "General service change";
		private static final String GENERAL_MESSAGE = "General Message"; // Escalator / Elevator
		private static final String OTHER = "Other";

		private static final String AFFECTED_ROUTES_START_WITH = "affectedRoutes-";

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			try {
				if (ITEM.equals(localName)) {
					final String title = this.currentTitleSb.toString().trim();
					String desc = this.currentDescriptionSb.toString().trim();
					desc = HtmlUtils.fixTextViewBR(desc);
					desc = HtmlUtils.removeStyle(desc);
					desc = HtmlUtils.removeTables(desc); // messy, unreadable, text too long already (not supported by Html.fromHtml())
					desc = HtmlUtils.replaceImgTagWithUrlLink(this.fromURL, desc); // replace <img /> tags w/ image URLs
					final String link = this.currentLinkSb.toString().trim();
					final HashSet<String> routeShortNames = extractRouteShortNames(this.currentCategory2);
					final int severity = extractSeverity(this.currentCategory1, routeShortNames);
					final String text = ServiceUpdateCleaner.makeText(title, HtmlUtils.fromHtml(desc));
					final String textHtml = ServiceUpdateCleaner.makeTextHTML(
							title,
							ServiceUpdateCleaner.clean(desc, ServiceUpdateCleaner.getReplacement(severity)),
							link
					);
					if (CollectionUtils.getSize(routeShortNames) == 0) { // AGENCY
						this.serviceUpdates.add(
								new ServiceUpdate(null,
										OCTranspoProvider.getAgencyTargetUUID(this.targetAuthority),
										this.newLastUpdateInMs,
										this.serviceUpdateMaxValidityInMs,
										text,
										textHtml,
										severity,
										AGENCY_SOURCE_ID,
										this.sourceLabel,
										this.language));
					} else { // AGENCY ROUTE
						for (String routeShortName : routeShortNames) {
							this.serviceUpdates.add(new ServiceUpdate(null,
									OCTranspoProvider.getAgencyRouteShortNameTargetUUID(this.targetAuthority, routeShortName),
									this.newLastUpdateInMs,
									this.serviceUpdateMaxValidityInMs,
									text,
									textHtml,
									severity,
									AGENCY_SOURCE_ID,
									this.sourceLabel,
									this.language));
						}
					}
					this.currentItem = false;
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing '%s' end element!", this.currentLocalName);
			}
		}

		@NonNull
		private static HashSet<String> extractRouteShortNames(@Nullable String category) {
			final HashSet<String> routeShortNames = new HashSet<>();
			if (category != null
					&& category.startsWith(AFFECTED_ROUTES_START_WITH)) {
				final Matcher matcher = RegexUtils.DIGITS.matcher(category);
				while (matcher.find()) {
					routeShortNames.add(matcher.group());
				}
			}
			return routeShortNames;
		}

		private static int extractSeverity(@Nullable String category, HashSet<String> routeShortNames) {
			int severity = ServiceUpdate.SEVERITY_INFO_UNKNOWN;
			if (CANCELLED_TRIPS.equals(category)) {
				if (!routeShortNames.isEmpty()) {
					severity = ServiceUpdate.SEVERITY_WARNING_RELATED_POI;
				} else {
					severity = ServiceUpdate.SEVERITY_NONE; // too general
				}
			} else if (DELAYS.equals(category)) {
				if (!routeShortNames.isEmpty()) {
					severity = ServiceUpdate.SEVERITY_INFO_RELATED_POI;
				} else {
					severity = ServiceUpdate.SEVERITY_NONE; // too general
				}
			} else if (DETOURS.equals(category)) {
				if (!routeShortNames.isEmpty()) {
					severity = ServiceUpdate.SEVERITY_INFO_RELATED_POI;
				} else {
					severity = ServiceUpdate.SEVERITY_NONE; // too general
				}
			} else if (GENERAL_SERVICE_CHANGE.equals(category)) {
				if (!routeShortNames.isEmpty()) {
					severity = ServiceUpdate.SEVERITY_INFO_RELATED_POI;
				} else {
					severity = ServiceUpdate.SEVERITY_NONE; // too general
				}
			} else if (ROUTE_SERVICE_CHANGE.equals(category)) {
				if (!routeShortNames.isEmpty()) {
					severity = ServiceUpdate.SEVERITY_INFO_RELATED_POI;
				} else {
					severity = ServiceUpdate.SEVERITY_NONE; // too general
				}
			} else if (OTHER.equals(category)) {
				if (!routeShortNames.isEmpty()) {
					severity = ServiceUpdate.SEVERITY_INFO_RELATED_POI;
				} else {
					severity = ServiceUpdate.SEVERITY_NONE; // too general
				}
			} else if (GENERAL_MESSAGE.equals(category)) {
				if (!routeShortNames.isEmpty()) {
					severity = ServiceUpdate.SEVERITY_INFO_RELATED_POI;
				} else {
					severity = ServiceUpdate.SEVERITY_NONE; // too general
				}
			} else if (TODAY_S_SERVICE.equals(category)) {
				severity = ServiceUpdate.SEVERITY_NONE; // not shown on https://www.octranspo.com/en/alerts/
			}
			return severity;
		}
	}

	protected static class JGetNextTripsForStop {

		private static final String JSON_GET_NEXT_TRIPS_FOR_STOP_RESULT = "GetNextTripsForStopResult";
		@NonNull
		private final JGetNextTripsForStopResult jGetNextTripsForStopResult;

		JGetNextTripsForStop(@NonNull JGetNextTripsForStopResult jGetNextTripsForStopResult) {
			this.jGetNextTripsForStopResult = jGetNextTripsForStopResult;
		}

		@NonNull
		static JGetNextTripsForStop parseJSON(@Nullable JSONObject jGetNextTripsForStop) {
			return new JGetNextTripsForStop(
					JGetNextTripsForStopResult.parseJSON(
							jGetNextTripsForStop == null ? null : jGetNextTripsForStop.optJSONObject(JSON_GET_NEXT_TRIPS_FOR_STOP_RESULT)
					)
			);
		}

		@NonNull
		@Override
		public String toString() {
			return JGetNextTripsForStop.class.getSimpleName() + "{" +
					"jGetNextTripsForStopResult=" + jGetNextTripsForStopResult +
					'}';
		}

		static class JGetNextTripsForStopResult {
			private static final String JSON_ROUTE = "Route";
			@NonNull
			private final JRoute jRoute;

			JGetNextTripsForStopResult(@NonNull JRoute jRoute) {
				this.jRoute = jRoute;
			}

			@NonNull
			static JGetNextTripsForStopResult parseJSON(@Nullable JSONObject jGetNextTripsForStopResult) {
				return new JGetNextTripsForStopResult(
						JRoute.parseJSON(
								jGetNextTripsForStopResult == null ? null : jGetNextTripsForStopResult.optJSONObject(JSON_ROUTE)
						)
				);
			}

			@NonNull
			@Override
			public String toString() {
				return JGetNextTripsForStopResult.class.getSimpleName() + "{" +
						"jRoute=" + jRoute +
						'}';
			}

			static class JRoute {
				private static final String JSON_ROUTE_DIRECTION = "RouteDirection";
				@NonNull
				private final List<JRouteDirection> jRouteDirections;

				JRoute(@NonNull List<JRouteDirection> jRouteDirections) {
					this.jRouteDirections = jRouteDirections;
				}

				@NonNull
				static JRoute parseJSON(@Nullable JSONObject jRoute) {
					List<JRouteDirection> routeDirections = new ArrayList<>();
					JSONArray jRouteDirections = jRoute == null ? null : jRoute.optJSONArray(JSON_ROUTE_DIRECTION);
					if (jRouteDirections != null && jRouteDirections.length() > 0) {
						for (int rd = 0; rd < jRouteDirections.length(); rd++) {
							routeDirections.add(
									JRouteDirection.parseJSON(
											jRouteDirections.optJSONObject(rd)
									)
							);
						}
					} else { // sometimes an array, sometimes an object
						JSONObject jRouteDirection = jRoute == null ? null : jRoute.optJSONObject(JSON_ROUTE_DIRECTION);
						routeDirections.add(
								JRouteDirection.parseJSON(
										jRouteDirection
								)
						);
					}
					return new JRoute(routeDirections);
				}

				@NonNull
				@Override
				public String toString() {
					return JRoute.class.getSimpleName() + "{" +
							"jRouteDirections=" + jRouteDirections +
							'}';
				}

				static class JRouteDirection {
					private static final String JSON_ROUTE_LABEL = "RouteLabel";
					@Nullable
					private final String jRouteLabel;
					private static final String JSON_REQUEST_PROCESSING_TIME = "RequestProcessingTime";
					@Nullable
					private final String jRequestProcessingTime;
					private static final String JSON_TRIPS = "Trips";
					@Nullable
					private final JTrips jTrips;

					JRouteDirection(@Nullable String jRouteLabel, @Nullable String jRequestProcessingTime, @Nullable JTrips jTrips) {
						this.jRouteLabel = jRouteLabel;
						this.jRequestProcessingTime = jRequestProcessingTime;
						this.jTrips = jTrips;
					}

					@NonNull
					List<JTrip> getJTripList() {
						return this.jTrips == null ? Collections.emptyList() : this.jTrips.jTripList;
					}

					boolean hasRealTime() {
						for (JTrip jTrip : getJTripList()) {
							if (jTrip.isRealTime()) {
								return true;
							}
						}
						return false;
					}

					@NonNull
					static JRouteDirection parseJSON(@Nullable JSONObject jRouteDirection) {
						return new JRouteDirection(
								jRouteDirection == null ? null : jRouteDirection.optString(JSON_ROUTE_LABEL),
								jRouteDirection == null ? null : jRouteDirection.optString(JSON_REQUEST_PROCESSING_TIME),
								JTrips.parseJSON(
										jRouteDirection == null ? null : jRouteDirection.optJSONObject(JSON_TRIPS)
								)
						);
					}

					@NonNull
					@Override
					public String toString() {
						return JRouteDirection.class.getSimpleName() + "{" +
								"jRouteLabel='" + jRouteLabel + '\'' +
								", jRequestProcessingTime='" + jRequestProcessingTime + '\'' +
								", jTrips=" + jTrips +
								'}';
					}

					static class JTrips {
						private static final String JSON_TRIP = "Trip";
						@NonNull
						private final List<JTrip> jTripList;

						JTrips(@NonNull List<JTrip> jTripList) {
							this.jTripList = jTripList;
						}

						@NonNull
						static JTrips parseJSON(@Nullable JSONObject jTrips) {
							List<JTrip> trips = new ArrayList<>();
							JSONArray jRouteDirections = jTrips == null ? null : jTrips.optJSONArray(JSON_TRIP);
							if (jRouteDirections != null && jRouteDirections.length() > 0) {
								for (int t = 0; t < jRouteDirections.length(); t++) {
									trips.add(
											JTrip.parseJSON(
													jRouteDirections.optJSONObject(t)
											)
									);
								}
							}
							return new JTrips(trips);
						}

						@NonNull
						@Override
						public String toString() {
							return JTrips.class.getSimpleName() + "{" +
									"jTripList=" + jTripList +
									'}';
						}

						static class JTrip {
							private static final String JSON_TRIP_DESTINATION = "TripDestination";
							@Nullable
							private final String jTripDestination;

							private static final String JSON_ADJUSTED_SCHEDULE_TIME = "AdjustedScheduleTime"; // minutes until departure
							@Nullable
							private final String jAdjustedScheduleTime;

							private static final String JSON_ADJUSTMENT_AGE = "AdjustmentAge"; // fractional minutes.
							@Nullable
							private final String jAdjustmentAge;

							JTrip(@Nullable String jTripDestination, @Nullable String jAdjustedScheduleTime, @Nullable String jAdjustmentAge) {
								this.jTripDestination = jTripDestination;
								this.jAdjustedScheduleTime = jAdjustedScheduleTime;
								this.jAdjustmentAge = jAdjustmentAge;
							}

							boolean isRealTime() {
								return this.jAdjustmentAge != null && !"-1".equals(this.jAdjustmentAge);
							}

							@NonNull
							static JTrip parseJSON(@Nullable JSONObject jTrip) {
								return new JTrip(
										jTrip == null ? null : jTrip.optString(JSON_TRIP_DESTINATION),
										jTrip == null ? null : jTrip.optString(JSON_ADJUSTED_SCHEDULE_TIME),
										jTrip == null ? null : jTrip.optString(JSON_ADJUSTMENT_AGE)
								);
							}

							@NonNull
							@Override
							public String toString() {
								return JTrip.class.getSimpleName() + "{" +
										"jTripDestination='" + jTripDestination + '\'' +
										", jAdjustedScheduleTime='" + jAdjustedScheduleTime + '\'' +
										", jAdjustmentAge='" + jAdjustmentAge + '\'' +
										'}';
							}
						}
					}
				}
			}
		}
	}

	public static class OCTranspoDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = OCTranspoDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link OCTranspoDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "octranspo.db";

		/**
		 * Override if multiple {@link OCTranspoDbHelper} implementations in same app.
		 */
		static final String PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS = "pOcTranspoServiceUpdatesLastUpdate";

		static final String T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_SQL_CREATE = //
				StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS).build();

		private static final String T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_SQL_DROP = //
				SqlUtils.getSQLDropIfExistsQuery(T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS);

		static final String T_OC_TRANSPO_SERVICE_UPDATE = ServiceUpdateProvider.ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_OC_TRANSPO_SERVICE_UPDATE_SQL_CREATE = //
				ServiceUpdateProvider.ServiceUpdateDbHelper.getSqlCreateBuilder(T_OC_TRANSPO_SERVICE_UPDATE).build();

		private static final String T_OC_TRANSPO_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_OC_TRANSPO_SERVICE_UPDATE);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link OCTranspoDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.oc_transpo_db_version);
			}
			return dbVersion;
		}

		@NonNull
		private final Context context;

		OCTranspoDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
			this.context = context;
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_SQL_DROP);
			db.execSQL(T_OC_TRANSPO_SERVICE_UPDATE_SQL_DROP);
			PreferenceUtils.savePrefLclSync(this.context, PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS, 0L);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_LIVE_NEXT_BUS_ARRIVAL_DATA_FEED_STATUS_SQL_CREATE);
			db.execSQL(T_OC_TRANSPO_SERVICE_UPDATE_SQL_CREATE);
		}
	}
}
