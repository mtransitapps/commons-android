package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.api.SupportFactory;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.ServiceUpdate;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import com.google.transit.realtime.GtfsRealtime;

@SuppressLint("Registered")
public class GTFSRealTimeProvider extends MTContentProvider implements ServiceUpdateProviderContract {

	private static final String TAG = GTFSRealTimeProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS = GTFSRealTimeDbHelper.PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS;

	private static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		ServiceUpdateProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.gtfs_real_time_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String agencyId = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static String getAGENCY_ID(Context context) {
		if (agencyId == null) {
			agencyId = context.getResources().getString(R.string.gtfs_real_time_agency_id);
		}
		return agencyId;
	}

	private static String agencyBoldWords = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static String getAGENCY_BOLD_WORDS(Context context) {
		if (agencyBoldWords == null) {
			agencyBoldWords = context.getResources().getString(R.string.gtfs_real_time_agency_bold_words);
		}
		return agencyBoldWords;
	}

	private static java.util.List<String> agencyExtraLanguages = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static java.util.List<String> getAGENCY_EXTRA_LANGUAGES(Context context) {
		if (agencyExtraLanguages == null) {
			agencyExtraLanguages = Arrays.asList(context.getResources().getStringArray(R.array.gtfs_real_time_agency_extra_languages));
		}
		return agencyExtraLanguages;
	}

	private static java.util.List<String> agencyExtraBoldWords = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static java.util.List<String> getAGENCY_EXTRA_BOLD_WORDS(Context context) {
		if (agencyExtraBoldWords == null) {
			agencyExtraBoldWords = Arrays.asList(context.getResources().getStringArray(R.array.gtfs_real_time_agency_extra_bold_words));
		}
		return agencyExtraBoldWords;
	}

	private static Boolean agencyStopIdIsStopCode = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static boolean isAGENCY_STOP_ID_IS_STOP_CODE(Context context) {
		if (agencyStopIdIsStopCode == null) {
			agencyStopIdIsStopCode = context.getResources().getBoolean(R.bool.gtfs_real_time_stop_id_is_stop_code);
		}
		return agencyStopIdIsStopCode;
	}

	private static String agencyUrlToken = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static String getAGENCY_URL_TOKEN(Context context) {
		if (agencyUrlToken == null) {
			agencyUrlToken = context.getResources().getString(R.string.gtfs_real_time_agency_url_token);
		}
		return agencyUrlToken;
	}

	private static String agencyServiceAlertsUrl = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static String getAGENCY_SERVICE_ALERTS_URL(Context context, String token) {
		if (agencyServiceAlertsUrl == null) {
			agencyServiceAlertsUrl = context.getResources().getString(R.string.gtfs_real_time_agency_service_alerts_url, token);
		}
		return agencyServiceAlertsUrl;
	}

	private static String agencyTimeRegex = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static String getAGENCY_TIME_REGEX(Context context) {
		if (agencyTimeRegex == null) {
			agencyTimeRegex = context.getResources().getString(R.string.gtfs_real_time_agency_time_regex);
		}
		return agencyTimeRegex;
	}

	private static String agencyTimeHourFormat = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static String getAGENCY_TIME_HOUR_FORMAT(Context context) {
		if (agencyTimeHourFormat == null) {
			agencyTimeHourFormat = context.getResources().getString(R.string.gtfs_real_time_agency_time_hour_format);
		}
		return agencyTimeHourFormat;
	}

	private static String agencyTimeMinuteFormat = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static String getAGENCY_TIME_MINUTE_FORMAT(Context context) {
		if (agencyTimeMinuteFormat == null) {
			agencyTimeMinuteFormat = context.getResources().getString(R.string.gtfs_real_time_agency_time_minute_format);
		}
		return agencyTimeMinuteFormat;
	}

	private static String agencyTimeAmPmFormat = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static String getAGENCY_TIME_AM_PM_FORMAT(Context context) {
		if (agencyTimeAmPmFormat == null) {
			agencyTimeAmPmFormat = context.getResources().getString(R.string.gtfs_real_time_agency_time_ampm_format);
		}
		return agencyTimeAmPmFormat;
	}

	private static String agencyTimeZone = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	private static String getAGENCY_TIME_ZONE(Context context) {
		if (agencyTimeZone == null) {
			agencyTimeZone = context.getResources().getString(R.string.gtfs_real_time_agency_time_zone);
		}
		return agencyTimeZone;
	}

	private static final long SERVICE_UPDATE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1);
	private static final long SERVICE_UPDATE_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(30);
	private static final long SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);
	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

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

	@Override
	public String getServiceUpdateDbTableName() {
		return GTFSRealTimeDbHelper.T_GTFS_REAL_TIME_SERVICE_UPDATE;
	}

	@Override
	public void cacheServiceUpdates(ArrayList<ServiceUpdate> newServiceUpdates) {
		ServiceUpdateProvider.cacheServiceUpdatesS(this, newServiceUpdates);
	}

	@Override
	public ArrayList<ServiceUpdate> getCachedServiceUpdates(ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (serviceUpdateFilter.getPoi() == null || !(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getCachedServiceUpdates() > no service update (poi null or not RTS)");
			return null;
		}
		ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<ServiceUpdate>();
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		HashSet<String> targetUUIDs = getTargetUUIDs(rts);
		for (String targetUUID : targetUUIDs) {
			Collection<ServiceUpdate> cachedServiceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, targetUUID);
			serviceUpdates.addAll(cachedServiceUpdates);
		}
		enhanceRTServiceUpdateForStop(serviceUpdates, rts);
		return serviceUpdates;
	}

	private HashSet<String> getTargetUUIDs(RouteTripStop rts) {
		HashSet<String> targetUUIDs = new HashSet<String>();
		targetUUIDs.add(getAgencyTargetUUID(getAgencyTag()));
		targetUUIDs.add(getAgencyRouteTagTargetUUID(getAgencyTag(), getRouteId(rts)));
		targetUUIDs.add(getAgencyStopTagTargetUUID(getAgencyTag(), getStopId(rts)));
		targetUUIDs.add(getAgencyRouteStopTagTargetUUID(getAgencyTag(), getRouteId(rts), getStopId(rts)));
		return targetUUIDs;
	}

	public String getAgencyTag() {
		return getAGENCY_ID(getContext());
	}

	public String getRouteId(RouteTripStop rts) {
		return String.valueOf(rts.getRoute().getId());
	}

	public String getStopId(RouteTripStop rts) {
		if (isAGENCY_STOP_ID_IS_STOP_CODE(getContext())) {
			return rts.getStop().getCode();
		}
		return String.valueOf(rts.getStop().getId());
	}

	public String cleanStopTag(String stopTag) {
		return stopTag;
	}

	protected static String getAgencyStopTagTargetUUID(String agencyTag, String stopTag) {
		return POI.POIUtils.getUUID(agencyTag, stopTag);
	}

	protected static String getAgencyRouteTagTargetUUID(String agencyTag, String routeTag) {
		return POI.POIUtils.getUUID(agencyTag, routeTag);
	}

	protected static String getAgencyRouteStopTagTargetUUID(String agencyTag, String routeTag, String stopTag) {
		return POI.POIUtils.getUUID(agencyTag, routeTag, stopTag);
	}

	protected static String getAgencyTargetUUID(String agencyTag) {
		return POI.POIUtils.getUUID(agencyTag);
	}

	@Override
	public ArrayList<ServiceUpdate> getNewServiceUpdates(ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (serviceUpdateFilter == null || serviceUpdateFilter.getPoi() == null || !(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getNewServiceUpdates() > no new service update (filter null or poi null or not RTS): %s", serviceUpdateFilter);
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		updateAgencyServiceUpdateDataIfRequired(rts.getAuthority(), serviceUpdateFilter.isInFocusOrDefault());
		ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(serviceUpdateFilter);
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			String agencyTargetUUID = getAgencyTargetUUID(rts.getAuthority());
			cachedServiceUpdates = ArrayUtils.asArrayList(getServiceUpdateNone(agencyTargetUUID));
			enhanceRTServiceUpdateForStop(cachedServiceUpdates, rts); // convert to stop service update
		}
		return cachedServiceUpdates;
	}

	private void enhanceRTServiceUpdateForStop(Collection<ServiceUpdate> serviceUpdates, RouteTripStop rts) {
		try {
			if (CollectionUtils.getSize(serviceUpdates) > 0) {
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					serviceUpdate.setTargetUUID(rts.getUUID()); // route trip service update targets stop
					serviceUpdate.setTextHTML(enhanceHtmlDateTime(serviceUpdate.getTextHTML()));
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update for stop!");
		}
	}

	public ServiceUpdate getServiceUpdateNone(String agencyTargetUUID) {
		return new ServiceUpdate(null, agencyTargetUUID, TimeUtils.currentTimeMillis(), getServiceUpdateMaxValidityInMs(), null, null,
				ServiceUpdate.SEVERITY_NONE, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, getServiceUpdateLanguage());
	}

	private static final String AGENCY_SOURCE_ID = "gtfs_real_time_service_alerts";

	private static final String AGENCY_SOURCE_LABEL = "GTFS-RealTime";

	private void updateAgencyServiceUpdateDataIfRequired(String targetAuthority, boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS, 0L);
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyServiceUpdateDataIfRequiredSync(targetAuthority, lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyServiceUpdateDataIfRequiredSync(String targetAuthority, long lastUpdateInMs, boolean inFocus) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS, 0L) > lastUpdateInMs) {
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		if (lastUpdateInMs + getServiceUpdateMaxValidityInMs() < nowInMs) {
			deleteAllRequired = true; // too old to display
		}
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		if (deleteAllRequired || lastUpdateInMs + minUpdateMs < nowInMs) {
			updateAllAgencyServiceUpdateDataFromWWW(targetAuthority, deleteAllRequired); // try to update
		}
	}

	private void updateAllAgencyServiceUpdateDataFromWWW(String targetAuthority, boolean deleteAllRequired) {
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
			PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS, nowInMs, true); // sync
		} // else keep whatever we have until max validity reached
	}

	private static String agencyAlertsUrl = null;

	private static String getAgencyServiceAlertsUrlString(Context context) {
		if (agencyAlertsUrl == null) {
			agencyAlertsUrl = getAGENCY_SERVICE_ALERTS_URL(context, getAGENCY_URL_TOKEN(context));
		}
		return agencyAlertsUrl;
	}

	private ArrayList<ServiceUpdate> loadAgencyServiceUpdateDataFromWWW(String tagetAuthority) {
		try {
			String urlString = getAgencyServiceAlertsUrlString(getContext());
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<ServiceUpdate>();
				try {
					GtfsRealtime.FeedMessage gFeedMessage = GtfsRealtime.FeedMessage.parseFrom(url.openStream());
					for (GtfsRealtime.FeedEntity gFeedEntity : gFeedMessage.getEntityList()) {
						if (gFeedEntity.hasAlert()) {
							GtfsRealtime.Alert gAlert = gFeedEntity.getAlert();
							HashSet<ServiceUpdate> alerts = processAlerts(newLastUpdateInMs, gAlert);
							if (CollectionUtils.getSize(alerts) > 0) {
								serviceUpdates.addAll(alerts);
							}
						}
					}
				} catch (Exception e) {
					MTLog.w(this, e, "loadDataFromWWW() > error while parsing GTFS Real Time data!");
				}
				return serviceUpdates;
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
			MTLog.w(TAG, se, "No Internet Connection!");
			return null;
		} catch (Exception e) { // Unknown error
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private HashSet<ServiceUpdate> processAlerts(long newLastUpdateInMs, GtfsRealtime.Alert gAlert) {
		if (gAlert == null) {
			return null;
		}
		java.util.List<GtfsRealtime.EntitySelector> gEntitySelectors = gAlert.getInformedEntityList();
		if (CollectionUtils.getSize(gEntitySelectors) == 0) {
			MTLog.w(this, "processAlerts() > no entity selectors!");
			return null;
		}
		GtfsRealtime.Alert.Cause gCause = gAlert.getCause();
		GtfsRealtime.Alert.Effect gEffect = gAlert.getEffect();
		HashSet<String> targetUUIDs = new HashSet<String>();
		ArrayMap<String, Integer> targetUUIDSeverities = new ArrayMap<String, Integer>();
		String providerAgencyId = getAGENCY_ID(getContext());
		String agencyTag = getAgencyTag();
		for (GtfsRealtime.EntitySelector gEntitySelector : gEntitySelectors) {
			if (gEntitySelector.hasAgencyId() && !providerAgencyId.equals(gEntitySelector.getAgencyId())) {
				MTLog.w(this, "processAlerts() > Alert targets another agency: %s", gEntitySelector.getAgencyId());
				continue;
			}
			String targetUUID = parseTargetUUID(agencyTag, gEntitySelector);
			if (TextUtils.isEmpty(targetUUID)) {
				continue;
			}
			targetUUIDs.add(targetUUID);
			int severity = parseSeverity(gEntitySelector, gCause, gEffect);
			targetUUIDSeverities.put(targetUUID, severity);
		}
		if (CollectionUtils.getSize(targetUUIDs) == 0) {
			MTLog.w(this, "processAlerts() > no target UUIDs!");
			return null;
		}
		ArrayMap<String, String> headerTexts = parseTranslations(gAlert.getHeaderText());
		ArrayMap<String, String> descriptionTexts = parseTranslations(gAlert.getDescriptionText());
		ArrayMap<String, String> urlTexts = parseTranslations(gAlert.getUrl());
		HashSet<String> languages = new HashSet<String>();
		languages.addAll(headerTexts.keySet());
		languages.addAll(descriptionTexts.keySet());
		languages.addAll(urlTexts.keySet());
		HashSet<ServiceUpdate> serviceUpdates = new HashSet<ServiceUpdate>();
		long serviceUpdateMaxValidityInMs = getServiceUpdateMaxValidityInMs();
		for (String targetUUID : targetUUIDs) {
			int severity = targetUUIDSeverities.get(targetUUID);
			for (String language : languages) {
				ServiceUpdate newServiceUpdate = generateNewServiceUpdate(newLastUpdateInMs, headerTexts, descriptionTexts, urlTexts,
						serviceUpdateMaxValidityInMs, targetUUID, severity, language);
				serviceUpdates.add(newServiceUpdate);
			}
		}
		return serviceUpdates;
	}

	private static Pattern boldWords = null;

	private static Pattern getBoldWords(Context context) {
		if (boldWords == null) {
			try {
				if (!TextUtils.isEmpty(getAGENCY_BOLD_WORDS(context))) {
					boldWords = Pattern.compile(getAGENCY_BOLD_WORDS(context), Pattern.CASE_INSENSITIVE);
				}
			} catch (Exception e) {
				MTLog.w(TAG, e, "Error while compiling bold words pattern!");
				boldWords = null;
			}
		}
		return boldWords;
	}

	private static ArrayMap<String, Pattern> extraBoldWords = new ArrayMap<String, Pattern>();

	private static Pattern getExtraBoldWords(Context context, String language) {
		if (!extraBoldWords.containsKey(language)) {
			try {
				int index = getAGENCY_EXTRA_LANGUAGES(context).indexOf(language);
				if (index >= 0) {
					if (index < getAGENCY_EXTRA_BOLD_WORDS(context).size()) {
						String regex = getAGENCY_EXTRA_BOLD_WORDS(context).get(index);
						if (!TextUtils.isEmpty(regex)) {
							extraBoldWords.put(language, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
						}
					}
				}
			} catch (Exception e) {
				MTLog.w(TAG, e, "Error while compiling extra bold words pattern for language '%s'!", language);
				extraBoldWords.remove(language);
			}
		}
		return extraBoldWords.get(language);
	}

	private ServiceUpdate generateNewServiceUpdate(long newLastUpdateInMs, ArrayMap<String, String> headerTexts, ArrayMap<String, String> descriptionTexts,
			ArrayMap<String, String> urlTexts, long serviceUpdateMaxValidityInMs, String targetUUID, int severity, String language) {
		StringBuilder textSb = new StringBuilder();
		StringBuilder textHTMLSb = new StringBuilder();
		String header = headerTexts.get(language);
		if (!TextUtils.isEmpty(header)) {
			textSb.append(header);
			textHTMLSb.append(HtmlUtils.applyBold(header));
		}
		String description = descriptionTexts.get(language);
		if (!TextUtils.isEmpty(description)) {
			if (textSb.length() > 0) {
				textSb.append(": ");
			}
			textSb.append(description);
			if (textHTMLSb.length() > 0) {
				textHTMLSb.append(HtmlUtils.BR);
			}
			textHTMLSb.append(description);
		}
		String url = urlTexts.get(language);
		if (!TextUtils.isEmpty(url)) {
			if (textHTMLSb.length() > 0) {
				textHTMLSb.append(HtmlUtils.BR);
			}
			textHTMLSb.append(url);
		}
		Pattern boldWords;
		if (Locale.ENGLISH.getLanguage().equals(language)) {
			boldWords = getBoldWords(getContext());
		} else {
			boldWords = getExtraBoldWords(getContext(), language);
		}
		return new ServiceUpdate(null, targetUUID, newLastUpdateInMs, serviceUpdateMaxValidityInMs, textSb.toString(), enhanceHtml(textHTMLSb.toString(),
				boldWords), severity, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, language);
	}

	private String enhanceHtml(String originalHtml, Pattern boldWords) {
		if (TextUtils.isEmpty(originalHtml)) {
			return originalHtml;
		}
		try {
			String html = originalHtml;
			html = enhanceHtmlBold(html, boldWords);
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance HTML (using original)!");
			return originalHtml;
		}
	}

	private static final String CLEAN_BOLD_REPLACEMENT = HtmlUtils.applyBold("$1");

	private String enhanceHtmlBold(String html, Pattern regex) {
		if (regex == null || TextUtils.isEmpty(html)) {
			return html;
		}
		try {
			return regex.matcher(html).replaceAll(CLEAN_BOLD_REPLACEMENT);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while making text bold!");
			return html;
		}
	}

	private static Pattern agencyCleanTime = null;

	private static Pattern getAgencyCleanTime(Context context) {
		if (TextUtils.isEmpty(getAGENCY_TIME_REGEX(context))) {
			return null;
		}
		if (agencyCleanTime == null) {
			agencyCleanTime = Pattern.compile(getAGENCY_TIME_REGEX(context));
		}
		return agencyCleanTime;
	}

	private static ThreadSafeDateFormatter timeParser = null;

	private static final String COLON = ":";

	private ThreadSafeDateFormatter getTimeParser(Context context) {
		if (timeParser == null) {
			try {
				if (TextUtils.isEmpty(getAGENCY_TIME_HOUR_FORMAT(context)) || TextUtils.isEmpty(getAGENCY_TIME_MINUTE_FORMAT(context))) {
					return null;
				}
				String pattern = getAGENCY_TIME_HOUR_FORMAT(context) + COLON + getAGENCY_TIME_MINUTE_FORMAT(context);
				if (!TextUtils.isEmpty(getAGENCY_TIME_AM_PM_FORMAT(context))) {
					pattern += StringUtils.SPACE_STRING + getAGENCY_TIME_AM_PM_FORMAT(context);
				}
				timeParser = new ThreadSafeDateFormatter(pattern);
				if (!TextUtils.isEmpty(getAGENCY_TIME_ZONE(context))) {
					timeParser.setTimeZone(TimeZone.getTimeZone(getAGENCY_TIME_ZONE(context)));
				}
			} catch (Exception e) {
				MTLog.w(TAG, e, "Error while initializing time formatter!");
				timeParser = null;
			}
		}
		return timeParser;
	}

	private String enhanceHtmlDateTime(String html) throws ParseException {
		if (TextUtils.isEmpty(html)) {
			return html;
		}
		Pattern agencyCleanTime = getAgencyCleanTime(getContext());
		ThreadSafeDateFormatter timeParser = getTimeParser(getContext());
		if (agencyCleanTime == null || timeParser == null) {
			return html;
		}
		Matcher timeMatcher = agencyCleanTime.matcher(html);
		while (timeMatcher.find()) {
			String time = timeMatcher.group(0);
			String hours = timeMatcher.group(1);
			String minutes = timeMatcher.group(2);
			String ampm = null;
			if (timeMatcher.groupCount() > 2) {
				ampm = StringUtils.trim(timeMatcher.group(3));
			}
			String timeToParse = hours + COLON + minutes;
			if (!TextUtils.isEmpty(ampm)) {
				timeToParse += StringUtils.SPACE_STRING + ampm;
			}
			Date timeD = timeParser.parseThreadSafe(timeToParse);
			String fTime = TimeUtils.formatTime(getContext(), timeD);
			html = html.replace(time, HtmlUtils.applyBold(fTime));
		}
		return html;
	}

	private ArrayMap<String, String> parseTranslations(GtfsRealtime.TranslatedString gTranslatedString) {
		ArrayMap<String, String> translations = new ArrayMap<String, String>();
		java.util.List<GtfsRealtime.TranslatedString.Translation> gTranslations = gTranslatedString.getTranslationList();
		if (CollectionUtils.getSize(gTranslations) > 0) {
			int translationsCount = gTranslations.size();
			for (GtfsRealtime.TranslatedString.Translation gTranslation : gTranslations) {
				String language = parseLanguage(translationsCount, gTranslation.getLanguage());
				if (translations.containsKey(language)) {
					MTLog.w(this, "Language '%s' translation '%s' already provided with '%s'!", language, gTranslation.getText(), translations.get(language));
				}
				translations.put(language, gTranslation.getText());
			}
		}
		return translations;
	}

	private String parseTargetUUID(String agencyTag, GtfsRealtime.EntitySelector gEntitySelector) {
		if (gEntitySelector.hasRouteId()) {
			if (gEntitySelector.hasStopId()) {
				return getAgencyRouteStopTagTargetUUID(agencyTag, gEntitySelector.getRouteId(), gEntitySelector.getStopId());
			}
			return getAgencyRouteTagTargetUUID(agencyTag, gEntitySelector.getRouteId());
		} else if (gEntitySelector.hasStopId()) {
			return getAgencyStopTagTargetUUID(agencyTag, gEntitySelector.getStopId());
		} else if (gEntitySelector.hasAgencyId()) {
			return getAgencyTargetUUID(agencyTag);
		}
		MTLog.w(this, "parseTargetUUID() > unexected entity selector: %s", gEntitySelector);
		return getAgencyTargetUUID(agencyTag); // DEFAULT
	}

	private int parseSeverity(GtfsRealtime.EntitySelector gEntitySelector, GtfsRealtime.Alert.Cause gCause, GtfsRealtime.Alert.Effect gEffect) {
		if (gEntitySelector.hasStopId()) {
			return ServiceUpdate.SEVERITY_WARNING_POI;
		} else if (gEntitySelector.hasRouteId()) {
			return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
		} else if (gEntitySelector.hasAgencyId()) {
			return ServiceUpdate.SEVERITY_INFO_AGENCY;
		}
		return ServiceUpdate.SEVERITY_INFO_UNKNOWN;
	}

	private String parseLanguage(int translationsCount, String gLanguage) {
		if (TextUtils.isEmpty(gLanguage) || translationsCount == 1) {
			return Locale.ENGLISH.getLanguage();
		}
		String providedLanguage = SupportFactory.get().localeForLanguageTag(gLanguage).getLanguage();
		if (getAGENCY_EXTRA_LANGUAGES(getContext()).contains(providedLanguage)) {
			return providedLanguage;
		} else {
			return Locale.ENGLISH.getLanguage();
		}
	}

	private int deleteAllAgencyServiceUpdateData() {
		int affectedRows = 0;
		try {
			String selection = SqlUtils.getWhereEqualsString(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_ID, AGENCY_SOURCE_ID);
			affectedRows = getDBHelper().getWritableDatabase().delete(getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all agency service update data!");
		}
		return affectedRows;
	}

	private static String serviceUpdateLanguage = null;

	@Override
	public String getServiceUpdateLanguage() {
		if (serviceUpdateLanguage == null) {
			String newServiceUpdateLanguage = Locale.ENGLISH.getLanguage();
			if (LocaleUtils.isFR()) {
				if (getAGENCY_EXTRA_LANGUAGES(getContext()).contains(Locale.FRENCH.getLanguage())) {
					newServiceUpdateLanguage = Locale.FRENCH.getLanguage();
				}
			}
			serviceUpdateLanguage = newServiceUpdateLanguage;
		}
		return serviceUpdateLanguage;
	}

	@Override
	public boolean purgeUselessCachedServiceUpdates() {
		return ServiceUpdateProvider.purgeUselessCachedServiceUpdates(this);
	}

	@Override
	public boolean deleteCachedServiceUpdate(Integer serviceUpdateId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, serviceUpdateId);
	}

	@Override
	public boolean deleteCachedServiceUpdate(String targetUUID, String sourceId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, targetUUID, sourceId);
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

	private static GTFSRealTimeDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private GTFSRealTimeDbHelper getDBHelper(Context context) {
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
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return GTFSRealTimeDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	public GTFSRealTimeDbHelper getNewDbHelper(Context context) {
		return new GTFSRealTimeDbHelper(context.getApplicationContext());
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
		Cursor cursor = ServiceUpdateProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
	}

	@Override
	public String getTypeMT(Uri uri) {
		String type = ServiceUpdateProvider.getTypeS(this, uri);
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

	public static class GTFSRealTimeDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = GTFSRealTimeDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "gtfsrealtime.db";

		/**
		 * Override if multiple {@link GTFSRealTimeDbHelper} implementations in same app.
		 */
		protected static final String PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS = "pGTFSRealTimeServiceAlertsLastUpdate";

		public static final String T_GTFS_REAL_TIME_SERVICE_UPDATE = ServiceUpdateProvider.ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_GTFS_REAL_TIME_SERVICE_UPDATE_SQL_CREATE = ServiceUpdateProvider.ServiceUpdateDbHelper.getSqlCreateBuilder(
				T_GTFS_REAL_TIME_SERVICE_UPDATE).build();

		private static final String T_GTFS_REAL_TIME_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_GTFS_REAL_TIME_SERVICE_UPDATE);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link GTFSRealTimeDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.gtfs_real_time_db_version);
			}
			return dbVersion;
		}

		private Context context;

		public GTFSRealTimeDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
			this.context = context;
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_GTFS_REAL_TIME_SERVICE_UPDATE_SQL_DROP);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_SERVICE_ALERTS_LAST_UPDATE_MS, 0L, true);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_GTFS_REAL_TIME_SERVICE_UPDATE_SQL_CREATE);
		}
	}
}
