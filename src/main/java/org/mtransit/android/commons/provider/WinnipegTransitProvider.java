package org.mtransit.android.commons.provider;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.CleanUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.Trip;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;

@SuppressLint("Registered")
public class WinnipegTransitProvider extends MTContentProvider implements StatusProviderContract, NewsProviderContract {

	private static final String LOG_TAG = WinnipegTransitProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		NewsProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
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
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.winnipeg_transit_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String apiKey = null;

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	@NonNull
	private static String getAPI_KEY(@NonNull Context context) {
		if (apiKey == null) {
			apiKey = context.getResources().getString(R.string.winnipeg_transit_api_key);
		}
		return apiKey;
	}

	@Nullable
	private static String newsAuthorName = null;

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	@NonNull
	private static String getNEWS_AUTHOR_NAME(@NonNull Context context) {
		if (newsAuthorName == null) {
			newsAuthorName = context.getResources().getString(R.string.winnipeg_transit_news_author_name);
		}
		return newsAuthorName;
	}

	@Nullable
	private static String newsColor = null;

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	@NonNull
	private static String getNEWS_COLOR(@NonNull Context context) {
		if (newsColor == null) {
			newsColor = context.getResources().getString(R.string.winnipeg_transit_news_color);
		}
		return newsColor;
	}

	@Nullable
	private static String newsTargetAuthority = null;

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	@NonNull
	private static String getNEWS_TARGET_AUTHORITY(@NonNull Context context) {
		if (newsTargetAuthority == null) {
			newsTargetAuthority = context.getResources().getString(R.string.winnipeg_transit_news_target_for_poi_authority);
		}
		return newsTargetAuthority;
	}

	private static final long WEB_SERVICE_STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long WEB_SERVICE_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long WEB_SERVICE_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long WEB_SERVICE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L);
	private static final long WEB_SERVICE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	@Override
	public long getStatusMaxValidityInMs() {
		return WEB_SERVICE_STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return WEB_SERVICE_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return WEB_SERVICE_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return WEB_SERVICE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return WEB_SERVICE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Override
	public POIStatus getCachedStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule without schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteTripStop rts = scheduleStatusFilter.getRouteTripStop();
		POIStatus status = StatusProvider.getCachedStatusS(this, rts.getUUID());
		if (status != null) {
			if (status instanceof Schedule) {
				((Schedule) status).setDescentOnly(rts.isDescentOnly());
			}
		}
		return status;
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
		return WinnipegTransitDbHelper.T_WEB_SERVICE_STATUS;
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

	private static final TimeZone WINNIPEG_TZ = TimeZone.getTimeZone("America/Winnipeg");

	private static final ThreadSafeDateFormatter DATE_FORMATTER;

	static {
		ThreadSafeDateFormatter dateFormatter = new ThreadSafeDateFormatter("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
		dateFormatter.setTimeZone(WINNIPEG_TZ);
		DATE_FORMATTER = dateFormatter;
	}

	private static final String REAL_TIME_URL_PART_1_BEFORE_STOP_ID = "https://api.winnipegtransit.com/v3/stops/";
	private static final String REAL_TIME_URL_PART_2_BEFORE_ROUTE_ID = "/schedule.json?route=";
	private static final String REAL_TIME_URL_PART_3_BEFORE_START = "&start=";
	private static final String REAL_TIME_URL_PART_4_BEFORE_END = "&end=";
	private static final String REAL_TIME_URL_PART_5_BEFORE_API_KEY = "&api-key=";

	@NonNull
	private static String getRealTimeStatusUrlString(@NonNull String apiKey, @NonNull RouteTripStop rts) {
		Calendar c = Calendar.getInstance(WINNIPEG_TZ);
		c.add(Calendar.HOUR, -1);
		String start = DATE_FORMATTER.formatThreadSafe(c.getTime());
		c.add(Calendar.HOUR, +1 + 12);
		String end = DATE_FORMATTER.formatThreadSafe(c.getTime());
		return REAL_TIME_URL_PART_1_BEFORE_STOP_ID + //
				rts.getStop().getCode() + //
				REAL_TIME_URL_PART_2_BEFORE_ROUTE_ID + //
				rts.getRoute().getShortName() + //
				REAL_TIME_URL_PART_3_BEFORE_START + //
				start + //
				REAL_TIME_URL_PART_4_BEFORE_END + //
				end + //
				REAL_TIME_URL_PART_5_BEFORE_API_KEY + //
				apiKey;
	}

	private void loadRealTimeStatusFromWWW(@NonNull RouteTripStop rts) {
		try {
			final Context context = getContext();
			if (context == null) {
				return;
			}
			String urlString = getRealTimeStatusUrlString(getAPI_KEY(context), rts);
			URL url = new URL(urlString);
			MTLog.i(this, "Loading from '%s'...", getRealTimeStatusUrlString("API_KEY", rts));
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				Collection<POIStatus> statuses = parseAgencyJSON(jsonString, rts, newLastUpdateInMs);
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

	private static final String JSON_STOP_SCHEDULE = "stop-schedule";
	private static final String JSON_ROUTE_SCHEDULES = "route-schedules";
	private static final String JSON_SCHEDULED_STOPS = "scheduled-stops";
	private static final String JSON_TIMES = "times";
	private static final String JSON_DEPARTURE = "departure";
	private static final String JSON_ESTIMATED = "estimated";
	private static final String JSON_SCHEDULED = "scheduled";
	private static final String JSON_ARRIVAL = "arrival";
	private static final String JSON_VARIANT = "variant";
	private static final String JSON_NAME = "name";

	private static long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(10);

	@Nullable
	private Collection<POIStatus> parseAgencyJSON(String jsonString, @NonNull RouteTripStop rts, long newLastUpdateInMs) {
		try {
			ArrayList<POIStatus> result = new ArrayList<>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_STOP_SCHEDULE)) {
				JSONObject jStopSchedule = json.getJSONObject(JSON_STOP_SCHEDULE);
				if (jStopSchedule.has(JSON_ROUTE_SCHEDULES)) {
					JSONArray jRouteSchedules = jStopSchedule.getJSONArray(JSON_ROUTE_SCHEDULES);
					if (jRouteSchedules.length() > 0) {
						JSONObject jRouteSchedule = jRouteSchedules.getJSONObject(0);
						if (jRouteSchedule != null && jRouteSchedule.has(JSON_SCHEDULED_STOPS)) {
							JSONArray jScheduledStops = jRouteSchedule.getJSONArray(JSON_SCHEDULED_STOPS);
							if (jScheduledStops.length() > 0) {
								Schedule newSchedule = parseAgencySchedule(rts, newLastUpdateInMs, jScheduledStops);
								result.add(newSchedule);
							}
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

	@Nullable
	private Schedule parseAgencySchedule(@NonNull RouteTripStop rts, long newLastUpdateInMs, JSONArray jScheduledStops) {
		try {
			Schedule newSchedule = new Schedule(rts.getUUID(), newLastUpdateInMs, getStatusMaxValidityInMs(), newLastUpdateInMs, PROVIDER_PRECISION_IN_MS,
					false);
			for (int s = 0; s < jScheduledStops.length(); s++) {
				JSONObject jScheduledStop = jScheduledStops.getJSONObject(s);
				String variantName = parseScheduleStopVariantName(jScheduledStop);
				if (jScheduledStop != null && jScheduledStop.has(JSON_TIMES)) {
					JSONObject jTimes = jScheduledStop.getJSONObject(JSON_TIMES);
					String timeS = getTimeString(jTimes);
					boolean isRealTime = isRealTime(jTimes);
					if (!TextUtils.isEmpty(timeS)) {
						final Date date = DATE_FORMATTER.parseThreadSafe(timeS);
						if (date == null) {
							continue;
						}
						final long time = date.getTime();
						long t = TimeUtils.timeToTheTensSecondsMillis(time);
						Schedule.Timestamp newTimestamp = new Schedule.Timestamp(t);
						if (!TextUtils.isEmpty(variantName)) {
							newTimestamp.setHeadsign(Trip.HEADSIGN_TYPE_STRING, cleanTripHeadsign(variantName, rts));
						}
						newSchedule.addTimestampWithoutSort(newTimestamp);
					}
				}
			}
			newSchedule.sortTimestamps();
			return newSchedule;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing schedule JSON '%s'!", jScheduledStops);
			return null;
		}
	}

	@Nullable
	private String parseScheduleStopVariantName(JSONObject jScheduledStop) {
		try {
			if (jScheduledStop != null && jScheduledStop.has(JSON_VARIANT)) {
				JSONObject jVariant = jScheduledStop.getJSONObject(JSON_VARIANT);
				if (jVariant.has(JSON_NAME)) {
					return jVariant.getString(JSON_NAME);
				}
			}
			return null;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing schedule stop variant name %s!", jScheduledStop);
			return null;
		}
	}

	private static final Pattern UNIVERSITY_OF = Pattern.compile("(university of )", Pattern.CASE_INSENSITIVE);
	private static final String UNIVERSITY_OF_REPLACEMENT = "U of ";

	private static final Pattern MISERICORDIA_HEALTH_CTR = Pattern.compile("(Misericordia Health Centre)", Pattern.CASE_INSENSITIVE);
	private static final String MISERICORDIA_HEALTH_CTR_REPLACEMENT = "Misericordia";

	private static final Pattern AIRPORT_TERMINAL = Pattern.compile("(airport terminal)", Pattern.CASE_INSENSITIVE);
	private static final String AIRPORT_TERMINAL_REPLACEMENT = "Airport";

	private static final Pattern POINT = Pattern.compile("((^|\\S)(\\.)(\\S|$))", Pattern.CASE_INSENSITIVE);
	private static final String POINT_REPLACEMENT = "$2$3 $4";

	private static final Pattern VIA = Pattern.compile("((^|\\W)(via)(\\W|$))", Pattern.CASE_INSENSITIVE);

	private String cleanTripHeadsign(String tripHeadsign, RouteTripStop optRTS) {
		try {
			tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
			tripHeadsign = POINT.matcher(tripHeadsign).replaceAll(POINT_REPLACEMENT);
			tripHeadsign = UNIVERSITY_OF.matcher(tripHeadsign).replaceAll(UNIVERSITY_OF_REPLACEMENT);
			tripHeadsign = MISERICORDIA_HEALTH_CTR.matcher(tripHeadsign).replaceAll(MISERICORDIA_HEALTH_CTR_REPLACEMENT);
			tripHeadsign = AIRPORT_TERMINAL.matcher(tripHeadsign).replaceAll(AIRPORT_TERMINAL_REPLACEMENT);
			tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
			tripHeadsign = CleanUtils.removePoints(tripHeadsign);
			tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
			tripHeadsign = CleanUtils.cleanLabel(tripHeadsign);
			tripHeadsign = CleanUtils.keepTo(tripHeadsign);
			Matcher matcherVIA = VIA.matcher(tripHeadsign);
			if (matcherVIA.find()) {
				String tripHeadsignBeforeVIA = tripHeadsign.substring(0, matcherVIA.start());
				String tripHeadsignAfterVIA = tripHeadsign.substring(matcherVIA.end());
				if (optRTS != null) {
					String heading = getContext() == null ? optRTS.getTrip().getHeading() : optRTS.getTrip().getHeading(getContext());
					if (Trip.isSameHeadsign(tripHeadsignBeforeVIA, heading)
							|| Trip.isSameHeadsign(tripHeadsignBeforeVIA, optRTS.getRoute().getLongName())) {
						tripHeadsign = tripHeadsignAfterVIA;
					} else if (Trip.isSameHeadsign(tripHeadsignAfterVIA, heading)
							|| Trip.isSameHeadsign(tripHeadsignAfterVIA, optRTS.getRoute().getLongName())) {
						tripHeadsign = tripHeadsignBeforeVIA;
					} else {
						tripHeadsign = tripHeadsignBeforeVIA;
					}
				} else {
					tripHeadsign = tripHeadsignBeforeVIA;
				}
			}
			return tripHeadsign;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadsign);
			return tripHeadsign;
		}
	}

	private String getTimeString(JSONObject jTimes) {
		try {
			String timeS;
			if (jTimes.has(JSON_DEPARTURE)) {
				JSONObject jDeparture = jTimes.getJSONObject(JSON_DEPARTURE);
				if (jDeparture.has(JSON_ESTIMATED)) {
					timeS = jDeparture.getString(JSON_ESTIMATED);
					if (!TextUtils.isEmpty(timeS)) {
						return timeS;
					}
				}
				if (jDeparture.has(JSON_SCHEDULED)) {
					timeS = jDeparture.getString(JSON_SCHEDULED);
					if (!TextUtils.isEmpty(timeS)) {
						return timeS;
					}
				}
			}
			if (jTimes.has(JSON_ARRIVAL)) {
				JSONObject jArrival = jTimes.getJSONObject(JSON_ARRIVAL);
				if (jArrival.has(JSON_ESTIMATED)) {
					timeS = jArrival.getString(JSON_ESTIMATED);
					if (!TextUtils.isEmpty(timeS)) {
						return timeS;
					}
				}
				if (jArrival.has(JSON_SCHEDULED)) {
					timeS = jArrival.getString(JSON_SCHEDULED);
					if (!TextUtils.isEmpty(timeS)) {
						return timeS;
					}
				}
			}
			return null;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing time JSON '%s'!", jTimes);
			return null;
		}
	}

	private boolean isRealTime(JSONObject jTimes) {
		try {
			String timeS;
			if (jTimes.has(JSON_DEPARTURE)) {
				JSONObject jDeparture = jTimes.getJSONObject(JSON_DEPARTURE);
				if (jDeparture.has(JSON_ESTIMATED)) {
					timeS = jDeparture.getString(JSON_ESTIMATED);
					if (!TextUtils.isEmpty(timeS)) {
						return true;
					}
				}
				if (jDeparture.has(JSON_SCHEDULED)) {
					timeS = jDeparture.getString(JSON_SCHEDULED);
					if (!TextUtils.isEmpty(timeS)) {
						return false;
					}
				}
			}
			if (jTimes.has(JSON_ARRIVAL)) {
				JSONObject jArrival = jTimes.getJSONObject(JSON_ARRIVAL);
				if (jArrival.has(JSON_ESTIMATED)) {
					timeS = jArrival.getString(JSON_ESTIMATED);
					if (!TextUtils.isEmpty(timeS)) {
						return true;
					}
				}
				if (jArrival.has(JSON_SCHEDULED)) {
					timeS = jArrival.getString(JSON_SCHEDULED);
					if (!TextUtils.isEmpty(timeS)) {
						return false;
					}
				}
			}
			return false;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing time JSON '%s'!", jTimes);
			return false;
		}
	}

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS = WinnipegTransitDbHelper.PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS;

	private static final long NEWS_MAX_VALIDITY_IN_MS = Long.MAX_VALUE; // FOREVER
	private static final long NEWS_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1);
	private static final long NEWS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(30);
	private static final long NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10);

	@Override
	public long getMinDurationBetweenNewsRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public long getNewsMaxValidityInMs() {
		return NEWS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getNewsValidityInMs(boolean inFocus) {
		if (inFocus) {
			return NEWS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return NEWS_VALIDITY_IN_MS;
	}

	@Override
	public boolean purgeUselessCachedNews() {
		return NewsProvider.purgeUselessCachedNews(this);
	}

	@Override
	public boolean deleteCachedNews(Integer serviceUpdateId) {
		return NewsProvider.deleteCachedNews(this, serviceUpdateId);
	}

	private static final String AGENCY_SOURCE_ID = "api_winnipegtransit_com_service_advisories";

	private static final String AGENCY_SOURCE_LABEL = "winnipegtransit.com";

	@SuppressWarnings("UnusedReturnValue")
	private int deleteAllAgencyNewsData() {
		int affectedRows = 0;
		try {
			String selection = SqlUtils.getWhereEqualsString(NewsProviderContract.Columns.T_NEWS_K_SOURCE_ID, AGENCY_SOURCE_ID);
			affectedRows = getDBHelper().getWritableDatabase().delete(getNewsDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all agency news data!");
		}
		return affectedRows;
	}

	@Override
	public void cacheNews(@NonNull ArrayList<News> newNews) {
		NewsProvider.cacheNewsS(this, newNews);
	}

	@Override
	public ArrayList<News> getCachedNews(@NonNull NewsProviderContract.Filter newsFilter) {
		return NewsProvider.getCachedNewsS(this, newsFilter);
	}

	@Override
	public Cursor getNewsFromDB(NewsProviderContract.Filter newsFilter) {
		return NewsProvider.getDefaultNewsFromDB(newsFilter, this);
	}

	private static Collection<String> languages = null;

	@Override
	public Collection<String> getNewsLanguages() {
		if (languages == null) {
			languages = new HashSet<>();
			languages.add(Locale.ENGLISH.getLanguage());
			languages.add(LocaleUtils.UNKNOWN);
		}
		return languages;
	}

	@Override
	public ArrayList<News> getNewNews(@NonNull NewsProviderContract.Filter newsFilter) {
		updateAgencyNewsDataIfRequired(newsFilter.isInFocusOrDefault());
		return getCachedNews(newsFilter);
	}

	private void updateAgencyNewsDataIfRequired(boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS, 0L);
		long minUpdateMs = Math.min(getNewsMaxValidityInMs(), getNewsValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyNewsDataIfRequiredSync(lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyNewsDataIfRequiredSync(long lastUpdateInMs, boolean inFocus) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS, 0L) > lastUpdateInMs) {
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		if (lastUpdateInMs + getNewsMaxValidityInMs() < nowInMs) {
			deleteAllRequired = true; // too old to display
		}
		long minUpdateMs = Math.min(getNewsMaxValidityInMs(), getNewsValidityInMs(inFocus));
		if (deleteAllRequired || lastUpdateInMs + minUpdateMs < nowInMs) {
			updateAllAgencyNewsDataFromWWW(deleteAllRequired); // try to update
		}
	}

	private void updateAllAgencyNewsDataFromWWW(boolean deleteAllRequired) {
		boolean deleteAllDone = false;
		if (deleteAllRequired) {
			deleteAllAgencyNewsData();
			deleteAllDone = true;
		}
		ArrayList<News> newNews = loadAgencyNewsDataFromWWW();
		MTLog.d(this, "News(s) found: %s", newNews == null ? null : newNews.size());
		if (newNews != null) { // empty is OK
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyNewsData();
			}
			cacheNews(newNews);
			PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS, nowInMs, true); // sync
		} // else keep whatever we have until max validity reached
	}

	private static final String NEWS_URL_PART_1_BEFORE__API_KEY = "https://api.winnipegtransit.com/v2/service-advisories.json?api-key=";

	@NonNull
	private static String getNewsUrlString(@NonNull Context context) {
		return NEWS_URL_PART_1_BEFORE__API_KEY + //
				getAPI_KEY(context);
	}

	@Nullable
	private ArrayList<News> loadAgencyNewsDataFromWWW() {
		try {
			final Context context = getContext();
			if (context == null) {
				return null;
			}
			String urlString = getNewsUrlString(context);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				return parseAgencyNewsJSON(jsonString, newLastUpdateInMs);
			default:
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpUrlConnection.getResponseCode(),
						httpUrlConnection.getResponseMessage());
				return null;
			}
		} catch (SSLHandshakeException sslhe) {
			MTLog.w(this, sslhe, "SSL error!");
			return null;
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

	private static final String JSON_SERVICE_ADVISORIES = "service-advisories";
	private static final String JSON_KEY = "key";
	private static final String JSON_PRIORITY = "priority";
	private static final String JSON_TITLE = "title";
	private static final String JSON_BODY = "body";
	private static final String JSON_CATEGORY = "category";
	private static final String JSON_UPDATED_AT = "updated-at";

	private static final HashSet<String> TRANSIT_CATEGORIES_LC;

	static {
		HashSet<String> hashSet = new HashSet<>();
		hashSet.add("all");
		hashSet.add("transit");
		TRANSIT_CATEGORIES_LC = hashSet;
	}

	private static final String LINK_AND_KEY = "https://winnipegtransit.com/schedules-maps-tools/service-advisories/%s";

	private static final String DEFAULT_LINK = "https://winnipegtransit.com/schedules-maps-tools/service-advisories";

	private static final String COLON = ": ";

	private ArrayList<News> parseAgencyNewsJSON(String jsonString, long lastUpdateInMs) {
		try {
			Context context = getContext();
			ArrayList<News> news = new ArrayList<>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (context != null && json != null && json.has(JSON_SERVICE_ADVISORIES)) {
				JSONArray jServiceAdvisories = json.getJSONArray(JSON_SERVICE_ADVISORIES);
				long noteworthyInMs = Long.parseLong(context.getResources().getString(R.string.news_provider_noteworthy_long_term));
				int defaultPriority = context.getResources().getInteger(R.integer.news_provider_severity_info_agency);
				String target = getNEWS_TARGET_AUTHORITY(context);
				String color = getNEWS_COLOR(context);
				String authorName = getNEWS_AUTHOR_NAME(context);
				String language = Locale.ENGLISH.getLanguage();
				long maxValidityInMs = getNewsMaxValidityInMs();
				String authority = getAuthority();
				if (jServiceAdvisories.length() > 0) {
					for (int s = 0; s < jServiceAdvisories.length(); s++) {
						parseServiceAdvisory(jServiceAdvisories, s, news, lastUpdateInMs, noteworthyInMs, defaultPriority, target, color, authorName, language,
								maxValidityInMs, authority);
					}
				}
			}
			return news;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	private void parseServiceAdvisory(JSONArray jServiceAdvisories, int s, ArrayList<News> news, long lastUpdateInMs, long noteworthyInMs, int defaultPriority,
									  String target, String color, String authorName, String language, long maxValidityInMs, String authority) {
		try {
			JSONObject jServiceAdvisory = jServiceAdvisories.getJSONObject(s);
			if (jServiceAdvisory == null) {
				return;
			}
			if (jServiceAdvisory.has(JSON_CATEGORY)) {
				String category = jServiceAdvisory.optString(JSON_CATEGORY, StringUtils.EMPTY);
				if (!TRANSIT_CATEGORIES_LC.contains(category.toLowerCase(Locale.ENGLISH))) {
					return;
				}
			}
			String updatedAt = jServiceAdvisory.optString(JSON_UPDATED_AT, StringUtils.EMPTY);
			if (updatedAt.isEmpty()) {
				return;
			}
			final Date date = DATE_FORMATTER.parseThreadSafe(updatedAt);
			if (date == null) {
				return;
			}
			long updatedAtMs = date.getTime();
			String title = jServiceAdvisory.optString(JSON_TITLE, StringUtils.EMPTY);
			String body = jServiceAdvisory.optString(JSON_BODY, StringUtils.EMPTY);
			int key = jServiceAdvisory.optInt(JSON_KEY, -1);
			int priority = jServiceAdvisory.optInt(JSON_PRIORITY, -1);
			String link;
			String uuid;
			if (key < 0) {
				link = DEFAULT_LINK;
				uuid = AGENCY_SOURCE_ID + updatedAtMs;
			} else {
				link = String.format(LINK_AND_KEY, key);
				uuid = AGENCY_SOURCE_ID + key;
			}
			if (priority < 0) {
				priority = defaultPriority;
			}
			StringBuilder textSb = new StringBuilder();
			StringBuilder textHTMLSb = new StringBuilder();
			if (!title.isEmpty()) {
				textSb.append(title);
				textHTMLSb.append(HtmlUtils.applyBold(title));
			}
			if (!body.isEmpty()) {
				if (textSb.length() > 0) {
					textSb.append(COLON);
				}
				textSb.append(Html.fromHtml(body));
				if (textHTMLSb.length() > 0) {
					textHTMLSb.append(HtmlUtils.BR);
				}
				textHTMLSb.append(HtmlUtils.toHTML(body));
			}
			if (textSb.length() == 0 || textHTMLSb.length() == 0) {
				MTLog.w(this, "parseAgencyJSON() > skip (no text)");
				return;
			}
			if (!link.isEmpty()) {
				if (textHTMLSb.length() > 0) {
					textHTMLSb.append(HtmlUtils.BR).append(HtmlUtils.BR);
				}
				textHTMLSb.append(HtmlUtils.linkify(link));
			}
			news.add(new News(null, authority, uuid, priority, noteworthyInMs, lastUpdateInMs, maxValidityInMs, updatedAtMs, target, color, authorName, null,
					null, DEFAULT_LINK, textSb.toString(), textHTMLSb.toString(), link, language, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL));
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing service advisory JSON '%s'!", s);
		}
	}

	@Override
	public String getNewsDbTableName() {
		return WinnipegTransitDbHelper.T_WEB_SERVICE_NEWS;
	}

	@Override
	public String[] getNewsProjection() {
		return NewsProviderContract.PROJECTION_NEWS;
	}

	@Nullable
	private static ArrayMap<String, String> newsProjectionMap;

	@NonNull
	@Override
	public ArrayMap<String, String> getNewsProjectionMap() {
		if (newsProjectionMap == null) {
			//noinspection ConstantConditions // TODO requireContext()
			newsProjectionMap = NewsProvider.getNewNewsProjectionMap(getAUTHORITY(getContext()));
		}
		return newsProjectionMap;
	}

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
	private WinnipegTransitDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private WinnipegTransitDbHelper getDBHelper(@NonNull Context context) {
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
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		//noinspection ConstantConditions // TODO requireContext()
		return WinnipegTransitDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	@NonNull
	public WinnipegTransitDbHelper getNewDbHelper(@NonNull Context context) {
		return new WinnipegTransitDbHelper(context.getApplicationContext());
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

	@Override
	public String getAuthority() {
		//noinspection ConstantConditions // TODO requireContext()
		return getAUTHORITY(getContext());
	}

	@NonNull
	@Override
	public SQLiteOpenHelper getDBHelper() {
		//noinspection ConstantConditions // TODO requireContext()
		return getDBHelper(getContext());
	}

	@Override
	public Cursor queryMT(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor cursor = StatusProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		cursor = NewsProvider.queryS(this, uri, selection);
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
		type = NewsProvider.getTypeS(this, uri);
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

	public static class WinnipegTransitDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = WinnipegTransitDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link WinnipegTransitDbHelper} implementations in same app.
		 */
		static final String PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS = "pWinnipegTransitNewsLastUpdate";

		/**
		 * Override if multiple {@link WinnipegTransitDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "winnipegtransit.db";

		static final String T_WEB_SERVICE_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_WEB_SERVICE_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_WEB_SERVICE_STATUS).build();

		private static final String T_WEB_SERVICE_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_WEB_SERVICE_STATUS);

		static final String T_WEB_SERVICE_NEWS = NewsProvider.NewsDbHelper.T_NEWS;

		private static final String T_WEB_SERVICE_NEWS_SQL_CREATE = NewsProvider.NewsDbHelper.getSqlCreateBuilder(T_WEB_SERVICE_NEWS).build();

		private static final String T_WEB_SERVICE_NEWS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_WEB_SERVICE_NEWS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link WinnipegTransitDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.winnipeg_transit_db_version);
			}
			return dbVersion;
		}

		@NonNull
		private Context context;

		WinnipegTransitDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
			this.context = context;
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_WEB_SERVICE_STATUS_SQL_DROP);
			db.execSQL(T_WEB_SERVICE_NEWS_SQL_DROP);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS, 0L, true);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_WEB_SERVICE_STATUS_SQL_CREATE);
			db.execSQL(T_WEB_SERVICE_NEWS_SQL_CREATE);
		}
	}
}