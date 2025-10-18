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
import androidx.collection.LongSparseArray;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SecurityUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.commons.Cleaner;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.SourceUtils;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import javax.net.ssl.SSLHandshakeException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressLint("Registered")
public class StmInfoSubwayProvider extends MTContentProvider implements ServiceUpdateProviderContract {

	private static final String LOG_TAG = StmInfoSubwayProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = StmInfoSubwayDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS;

	@NonNull
	public static UriMatcher getNewUriMatcher(@NonNull String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		ServiceUpdateProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	private static UriMatcher getURI_MATCHER(@NonNull Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	@Nullable
	private static String authority = null;

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.stm_info_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String agencyColor = null;

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	private static String getAgencyColor(@NonNull Context context) {
		if (agencyColor == null) {
			agencyColor = context.getResources().getString(R.string.stm_info_agency_color);
		}
		return agencyColor;
	}

	private static final long SERVICE_UPDATE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L);
	private static final long SERVICE_UPDATE_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(30L);
	private static final long SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);
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
		return StmInfoSubwayDbHelper.T_STM_INFO_SERVICE_UPDATE;
	}

	@Override
	public void cacheServiceUpdates(@NonNull ArrayList<ServiceUpdate> newServiceUpdates) {
		ServiceUpdateProvider.cacheServiceUpdatesS(this, newServiceUpdates);
	}

	@Nullable
	@Override
	public ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if ((serviceUpdateFilter.getPoi() instanceof RouteDirectionStop)) {
			return getCachedServiceUpdates((RouteDirectionStop) serviceUpdateFilter.getPoi());
		} else if ((serviceUpdateFilter.getRoute() != null)) {
			return getCachedServiceUpdates(serviceUpdateFilter.getRoute());
		} else {
			MTLog.w(this, "getCachedServiceUpdates() > no service update (poi null or not RDS or no route)");
			return null;
		}
	}

	@Nullable
	private ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull RouteDirectionStop rds) {
		ArrayList<ServiceUpdate> routeDirectionServiceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, getAgencyTargetUUID(rds));
		enhanceRDServiceUpdateForStop(routeDirectionServiceUpdates, rds.getRoute(), rds.getUUID());
		return routeDirectionServiceUpdates;
	}

	@Nullable
	private ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull Route route) {
		ArrayList<ServiceUpdate> routeDirectionServiceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, getAgencyTargetUUID(route));
		enhanceRDServiceUpdateForStop(routeDirectionServiceUpdates, route, route.getUUID());
		return routeDirectionServiceUpdates;
	}

	private void enhanceRDServiceUpdateForStop(Collection<ServiceUpdate> serviceUpdates,
											   Route route,
											   String targetUUID // different UUID from provider target UUID
	) {
		try {
			if (CollectionUtils.getSize(serviceUpdates) > 0) {
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					serviceUpdate.setTargetUUID(targetUUID);
					enhanceRDServiceUpdateForStop(serviceUpdate, route);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route direction service update for stop!");
		}
	}

	private void enhanceRDServiceUpdateForStop(ServiceUpdate serviceUpdate, Route route) {
		try {
			String originalHtml = serviceUpdate.getTextHTML();
			serviceUpdate.setTextHTML(enhanceRTTextForStop(originalHtml, route, serviceUpdate.getSeverity()));
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route direction service update '%s' for stop!", serviceUpdate);
		}
	}

	private String enhanceRTTextForStop(String originalHtml, Route route, int severity) {
		if (TextUtils.isEmpty(originalHtml)) {
			return originalHtml;
		}
		try {
			String html = originalHtml;
			html = enhanceHtmlRds(route, html);
			html = enhanceHtmlSeverity(severity, html);
			html = enhanceHtmlDateTime(html);
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route direction service update HTML '%s' for stop!", originalHtml);
			return originalHtml;
		}
	}

	private String getAgencyTargetUUID(@NonNull RouteDirectionStop rds) {
		return getAgencyTargetUUID(rds.getRoute());
	}

	private String getAgencyTargetUUID(@NonNull Route route) {
		String targetAuthority = route.getAuthority();
		long routeId = route.getId();
		return getAgencyTargetUUID(targetAuthority, routeId);
	}

	private String getAgencyTargetUUID(String targetAuthority, long routeId) {
		return POI.POIUtils.getUUID(targetAuthority, routeId);
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
		if ((serviceUpdateFilter.getPoi() instanceof RouteDirectionStop)) {
			return getNewServiceUpdates((RouteDirectionStop) serviceUpdateFilter.getPoi(), serviceUpdateFilter.isInFocusOrDefault());
		} else if ((serviceUpdateFilter.getRoute() != null)) {
			return getNewServiceUpdates(serviceUpdateFilter.getRoute(), serviceUpdateFilter.isInFocusOrDefault());
		} else {
			MTLog.w(this, "getNewServiceUpdates() > no service update (poi null or not RDS or no route)");
			return null;
		}
	}

	@Nullable
	private ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull RouteDirectionStop rds, boolean inFocus) {
		updateAgencyServiceUpdateDataIfRequired(requireContextCompat(), rds.getAuthority(), inFocus);
		ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(rds);
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			String agencyTargetUUID = getAgencyTargetUUID(rds);
			cachedServiceUpdates = ArrayUtils.asArrayList(getServiceUpdateNone(agencyTargetUUID));
			enhanceRDServiceUpdateForStop(cachedServiceUpdates, rds.getRoute(), rds.getUUID()); // convert to stop service update
		}
		return cachedServiceUpdates;
	}

	@Nullable
	private ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull Route route, boolean inFocus) {
		updateAgencyServiceUpdateDataIfRequired(requireContextCompat(), route.getAuthority(), inFocus);
		ArrayList<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(route);
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			String agencyTargetUUID = getAgencyTargetUUID(route);
			cachedServiceUpdates = ArrayUtils.asArrayList(getServiceUpdateNone(agencyTargetUUID));
			enhanceRDServiceUpdateForStop(cachedServiceUpdates, route, route.getUUID()); // convert to stop service update
		}
		return cachedServiceUpdates;
	}

	@SuppressWarnings("WeakerAccess")
	@NonNull
	public ServiceUpdate getServiceUpdateNone(@NonNull String agencyTargetUUID) {
		return new ServiceUpdate(null, agencyTargetUUID, TimeUtils.currentTimeMillis(), getServiceUpdateMaxValidityInMs(), null, null,
				ServiceUpdate.SEVERITY_NONE, AGENCY_SOURCE_ID, EMPTY, getServiceUpdateLanguage());
	}

	@NonNull
	@Override
	public String getServiceUpdateLanguage() {
		return LocaleUtils.isFR() ? Locale.FRENCH.getLanguage() : Locale.ENGLISH.getLanguage();
	}

	public static final String AGENCY_SOURCE_ID = "www_stm_info_etats_du_service";

	private void updateAgencyServiceUpdateDataIfRequired(@NonNull Context context, String targetAuthority, boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L);
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyServiceUpdateDataIfRequiredSync(context, targetAuthority, lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyServiceUpdateDataIfRequiredSync(@NonNull Context context, String targetAuthority, long lastUpdateInMs, boolean inFocus) {
		if (PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L) > lastUpdateInMs) {
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
		ArrayList<ServiceUpdate> newServiceUpdates = loadAgencyServiceUpdateDataFromWWW(context, targetAuthority);
		if (newServiceUpdates != null) { // empty is OK
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyServiceUpdateData();
			}
			cacheServiceUpdates(newServiceUpdates);
			PreferenceUtils.savePrefLclSync(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, nowInMs);
		} // else keep whatever we have until max validity reached
	}

	private OkHttpClient okHttpClient = null;

	@NonNull
	private OkHttpClient getOkHttpClient(@NonNull Context context) {
		if (this.okHttpClient == null) {
			this.okHttpClient = NetworkUtils.makeNewOkHttpClientWithInterceptor(context);
		}
		return this.okHttpClient;
	}

	@Nullable
	private ArrayList<ServiceUpdate> loadAgencyServiceUpdateDataFromWWW(@NonNull Context context, String targetAuthority) {
		try {
			final String urlString = getAgencyUrlString();
			MTLog.i(this, "Loading from '%s'...", urlString);
			final String sourceLabel = SourceUtils.getSourceLabel(AGENCY_URL_PART_1_BEFORE_LANG);
			final Request urlRequest = new Request.Builder().url(urlString).build();
			try (Response response = getOkHttpClient(context).newCall(urlRequest).execute()) {
				switch (response.code()) {
				case HttpURLConnection.HTTP_OK:
					long newLastUpdateInMs = TimeUtils.currentTimeMillis();
					String jsonString = FileUtils.getString(response.body().byteStream());
					MTLog.d(this, "loadAgencyServiceUpdateDataFromWWW() > jsonString: %s.", jsonString);
					return parseAgencyJson(jsonString, sourceLabel, newLastUpdateInMs, targetAuthority);
				default:
					MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", response.code(),
							response.message());
					return null;
				}
			}
		} catch (SSLHandshakeException sslhe) {
			MTLog.w(this, sslhe, "SSL error!");
			SecurityUtils.logCertPathValidatorException(sslhe);
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

	private static final String JSON_METRO = "metro";

	@Nullable
	private ArrayList<ServiceUpdate> parseAgencyJson(String jsonString, @NonNull String sourceLabel, long nowInMs, String targetAuthority) {
		try {
			ArrayList<ServiceUpdate> result = new ArrayList<>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_METRO)) {
				JSONObject jMetro = json.getJSONObject(JSON_METRO);
				JSONArray jMetroNames = jMetro.names();
				long maxValidityInMs = getServiceUpdateMaxValidityInMs();
				String language = getServiceUpdateLanguage();
				if (jMetroNames != null) {
					for (int ln = 0; ln < jMetroNames.length(); ln++) {
						String jMetroName = jMetroNames.getString(ln);
						JSONObject jMetroObject = jMetro.getJSONObject(jMetroName);
						ServiceUpdate serviceUpdate = parseAgencyJsonText(jMetroObject, targetAuthority, jMetroName, sourceLabel, nowInMs, maxValidityInMs, language);
						if (serviceUpdate != null) {
							result.add(serviceUpdate);
						}
					}
				}
			}
			MTLog.i(this, "Found %d service updates.", result.size());
			if (Constants.DEBUG) {
				for (ServiceUpdate su : result) {
					MTLog.d(this, "parseAgencyJson()> - %s", su);
				}
			}
			return result;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	private static final String JSON_DATA = "data";
	private static final String JSON_TEXT = "text";

	@Nullable
	private ServiceUpdate parseAgencyJsonText(JSONObject jMetroObject, String targetAuthority, String routeId, @NonNull String sourceLabel, long nowInMs, long maxValidityInMs,
											  String language) {
		try {
			JSONObject jMetroData = jMetroObject.getJSONObject(JSON_DATA);
			String jMetroDataText = jMetroData.getString(JSON_TEXT);
			String targetUUID = getAgencyTargetUUID(targetAuthority, Integer.parseInt(routeId));
			if (jMetroDataText.isEmpty()) {
				return null;
			}
			int severity = findSeverity(jMetroObject, jMetroDataText);
			String textHtml = enhanceHtml(jMetroDataText, null, severity);
			return new ServiceUpdate(null, targetUUID, nowInMs, maxValidityInMs, jMetroDataText, textHtml, severity, AGENCY_SOURCE_ID, sourceLabel,
					language);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON message '%s'!", jMetroObject);
			return null;
		}
	}

	private static final String AGENCY_URL_PART_1_BEFORE_LANG = "https://www.stm.info/";
	private static final String AGENCY_URL_PART_2_AFTER_LANG = "/ajax/etats-du-service";
	private static final String AGENCY_URL_LANG_DEFAULT = "en";
	private static final String AGENCY_URL_LANG_FRENCH = "fr";

	private static String getAgencyUrlString() {
		return AGENCY_URL_PART_1_BEFORE_LANG +//
				(LocaleUtils.isFR() ? AGENCY_URL_LANG_FRENCH : AGENCY_URL_LANG_DEFAULT) + // language
				AGENCY_URL_PART_2_AFTER_LANG //
				;
	}

	private static final Cleaner CLEAN_STOPS = new Cleaner(
			"(between|between the stations)\\s*(\\S*)\\s*and\\s*([^\\s.,:]*)",
			"$1 " + HtmlUtils.applyBold("$2") + " and " + HtmlUtils.applyBold("$3")
	);
	private static final Cleaner CLEAN_STOPS_FR = new Cleaner(
			"(entre|entre les stations)\\s*(\\S*)\\s*et\\s*([^\\s.,:]*)",
			"$1 " + HtmlUtils.applyBold("$2") + " et " + HtmlUtils.applyBold("$3")
	);

	private static final Cleaner CLEAN_LINE = new Cleaner(
			"the\\s*(\\S*)\\s*line",
			"the " + HtmlUtils.applyBold("$1") + " line"
	);
	private static final Cleaner CLEAN_LINE_FR = new Cleaner(
			"ligne\\s*(\\S*)\\s*entre",
			"ligne " + HtmlUtils.applyBold("$1") + " entre");

	private String enhanceHtml(String originalHtml,
							   @SuppressWarnings("SameParameterValue") @Nullable RouteDirectionStop optRds,
							   Integer optSeverity) {
		if (TextUtils.isEmpty(originalHtml)) {
			return originalHtml;
		}
		try {
			String html = originalHtml;
			if (LocaleUtils.isFR()) {
				html = CLEAN_STOPS_FR.clean(html);
				html = CLEAN_LINE_FR.clean(html);
			} else {
				html = CLEAN_STOPS.clean(html);
				html = CLEAN_LINE.clean(html);
			}
			if (optRds != null) {
				html = enhanceHtmlRds(optRds.getRoute(), html);
			}
			if (optSeverity != null) {
				html = enhanceHtmlSeverity(optSeverity, html);
			}
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance HTML (using original)!");
			return originalHtml;
		}
	}

	private String enhanceHtmlSeverity(int severity, String html) {
		if (TextUtils.isEmpty(html)) {
			return html;
		}
		final String replacement = ServiceUpdateCleaner.getReplacement(severity);
		if (replacement != null) {
			html = ServiceUpdateCleaner.clean(html, replacement);
		}
		return html;
	}

	private static final LongSparseArray<String> ROUTE_LONG_NAME_FR;

	static {
		LongSparseArray<String> map = new LongSparseArray<>();
		map.put(1L, "GREEN");
		map.put(4L, "YELLOW");
		map.put(5L, "BLUE");
		ROUTE_LONG_NAME_FR = map;
	}

	private String enhanceHtmlRds(@NonNull Route route, String originalHtml) {
		if (TextUtils.isEmpty(originalHtml)) {
			return originalHtml;
		}
		String routeColor = route.getColor();
		if (TextUtils.isEmpty(routeColor)) {
			routeColor = getAgencyColor(requireContextCompat());
		}
		if (TextUtils.isEmpty(routeColor)) {
			return originalHtml;
		}
		String html = originalHtml;
		String routeLongName;
		if (!TextUtils.isEmpty(route.getLongName())
				&& html.toLowerCase(Locale.ENGLISH).contains(route.getLongName().toLowerCase(Locale.ENGLISH))) {
			routeLongName = route.getLongName();
		} else {
			routeLongName = ROUTE_LONG_NAME_FR.get(route.getId());
		}
		if (!TextUtils.isEmpty(routeLongName)) {
			html = new Cleaner(
					"(" + routeLongName + ")",
					HtmlUtils.applyFontColor(HtmlUtils.applyBold("$1"), routeColor),
					true
			).clean(html);
		}
		return html;
	}

	@NonNull
	private Calendar getNewBeginningOfTodayCal() {
		Calendar beginningOfTodayCal = Calendar.getInstance(TZ);
		beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, 0);
		beginningOfTodayCal.set(Calendar.MINUTE, 0);
		beginningOfTodayCal.set(Calendar.SECOND, 0);
		beginningOfTodayCal.set(Calendar.MILLISECOND, 0);
		return beginningOfTodayCal;
	}

	private static final Cleaner CLEAN_TIME = new Cleaner("(\\d{1,2})\\s*[:|h]\\s*(\\d{2})(\\s*([a|p]m))?", true);

	private static final Cleaner CLEAN_DATE = new Cleaner("(\\d{1,2}\\s*[a-zA-Z]+\\s*\\d{4})");

	private static final ThreadSafeDateFormatter PARSE_TIME = new ThreadSafeDateFormatter("HH:mm", Locale.ENGLISH);

	private static final ThreadSafeDateFormatter PARSE_TIME_AM_PM = new ThreadSafeDateFormatter("hh:mm a", Locale.ENGLISH);

	private static final ThreadSafeDateFormatter FORMAT_DATE = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM);

	private static final String PARSE_DATE_REGEX = "dd MMMM yyyy";

	private static final TimeZone TZ = TimeZone.getTimeZone("America/Montreal");

	private static final String COLON = ":";

	private String enhanceHtmlDateTime(String html) throws ParseException {
		if (TextUtils.isEmpty(html)) {
			return html;
		}
		Matcher timeMatcher = CLEAN_TIME.matcher(html);
		while (timeMatcher.find()) {
			String time = timeMatcher.group(0);
			if (time == null || time.isEmpty()) {
				continue;
			}
			String hours = timeMatcher.group(1);
			if (hours == null || hours.isEmpty()) {
				continue;
			}
			String minutes = timeMatcher.group(2);
			if (minutes == null || minutes.isEmpty()) {
				continue;
			}
			String amPm = StringUtils.trim(timeMatcher.group(3));
			Date timeD;
			int hoursInt = Integer.parseInt(hours);
			if (TextUtils.isEmpty(amPm)) {
				if (hoursInt > 12) {
					PARSE_TIME.setTimeZone(TZ);
					timeD = PARSE_TIME.parseThreadSafe(hours + COLON + minutes);
				} else { // check if PM missing
					Calendar timeCOriginal = getNewBeginningOfTodayCal();
					timeCOriginal.set(Calendar.HOUR_OF_DAY, hoursInt);
					timeCOriginal.set(Calendar.MINUTE, Integer.parseInt(minutes));
					Calendar timeCFixed = getNewBeginningOfTodayCal();
					timeCFixed.set(Calendar.HOUR_OF_DAY, hoursInt + 12);
					timeCFixed.set(Calendar.MINUTE, Integer.parseInt(minutes));
					long diffOriginalInMs = timeCOriginal.getTime().getTime() - TimeUtils.currentTimeMillis();
					long diffFixedInMs = timeCFixed.getTime().getTime() - TimeUtils.currentTimeMillis();
					if (Math.abs(diffOriginalInMs) > Math.abs(diffFixedInMs)) {
						timeD = timeCFixed.getTime();
					} else {
						timeD = timeCOriginal.getTime();
					}
				}
			} else {
				PARSE_TIME_AM_PM.setTimeZone(TZ);
				timeD = PARSE_TIME_AM_PM.parseThreadSafe(hours + COLON + minutes + " " + amPm);
			}
			if (timeD != null) {
				String fTime = TimeUtils.formatTime(false, requireContextCompat(), timeD);
				html = html.replace(time, HtmlUtils.applyBold(fTime));
			}
		}
		Matcher dateMatcher = CLEAN_DATE.matcher(html);
		ThreadSafeDateFormatter parseDate = new ThreadSafeDateFormatter(PARSE_DATE_REGEX, LocaleUtils.isFR() ? Locale.FRENCH : Locale.ENGLISH);
		while (dateMatcher.find()) {
			String date = dateMatcher.group(0);
			if (date == null || date.isEmpty()) {
				continue;
			}
			Date dateD = parseDate.parseThreadSafe(date);
			if (dateD == null) {
				continue;
			}
			String fDate = FORMAT_DATE.formatThreadSafe(dateD);
			html = html.replace(date, HtmlUtils.applyBold(fDate));
		}
		return html;
	}

	private static final Cleaner STATUS_NONE = new Cleaner("(normal m[e|é]tro service)", true);
	private static final Cleaner STATUS_NONE_FR = new Cleaner("(service normal du m[é|e]tro)", true);

	private static final Cleaner STATUS_INFO = new Cleaner("(service gradually)", true);
	private static final Cleaner STATUS_INFO_FR = new Cleaner("(reprise)", true);

	private static final Cleaner STATUS_WARNING = new Cleaner("(service disrupt|service interruption|closed)", true);
	private static final Cleaner STATUS_WARNING_FR = new Cleaner("(interruption de service|fermé(e)?)", true);

	private int findSeverity(@SuppressWarnings("unused") @Nullable JSONObject optJMetroObject,
							 @NonNull String jMetroDataText) {
		if (!jMetroDataText.isEmpty()) {
			if (LocaleUtils.isFR()) {
				if (STATUS_NONE_FR.find(jMetroDataText)) {
					if (Constants.DEBUG) {
						return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
					} else {
						return ServiceUpdate.SEVERITY_NONE;
					}
				} else if (STATUS_WARNING_FR.find(jMetroDataText)) {
					return ServiceUpdate.SEVERITY_WARNING_RELATED_POI;
				} else if (STATUS_INFO_FR.find(jMetroDataText)) {
					return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
				}
			} else {
				if (STATUS_NONE.find(jMetroDataText)) {
					if (Constants.DEBUG) {
						return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
					} else {
						return ServiceUpdate.SEVERITY_NONE;
					}
				} else if (STATUS_WARNING.find(jMetroDataText)) {
					return ServiceUpdate.SEVERITY_WARNING_RELATED_POI;
				} else if (STATUS_INFO.find(jMetroDataText)) {
					return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
				}
			}
		}
		MTLog.w(this, "Cannot find severity for '%s'. #ServiceUpdate", jMetroDataText);
		return ServiceUpdate.SEVERITY_INFO_UNKNOWN;
	}

	@MainThread
	@Override
	public boolean onCreateMT() {
		return true;
	}

	@Nullable
	private StmInfoSubwayDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private StmInfoSubwayDbHelper getDBHelper(@NonNull Context context) {
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
				MTLog.d(this, e, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return StmInfoSubwayDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	@NonNull
	public StmInfoSubwayDbHelper getNewDbHelper(@NonNull Context context) {
		return new StmInfoSubwayDbHelper(context.getApplicationContext());
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		return getURI_MATCHER(requireContextCompat());
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
		throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
	}

	@Nullable
	@Override
	public String getTypeMT(@NonNull Uri uri) {
		String type = ServiceUpdateProvider.getTypeS(this, uri);
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

	public static class StmInfoSubwayDbHelper extends ServiceUpdateProvider.ServiceUpdateDbHelper {

		private static final String LOG_TAG = StmInfoSubwayDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link StmInfoSubwayDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "stm_info.db";

		/**
		 * Override if multiple {@link StmInfoSubwayDbHelper} implementations in same app.
		 */
		static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pStmInfoSubwayEtatsDuServiceLastUpdate";

		static final String T_STM_INFO_SERVICE_UPDATE = ServiceUpdateProvider.ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_STM_INFO_SERVICE_UPDATE_SQL_CREATE = ServiceUpdateProvider.ServiceUpdateDbHelper.getSqlCreateBuilder(
				T_STM_INFO_SERVICE_UPDATE).build();

		private static final String T_STM_INFO_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STM_INFO_SERVICE_UPDATE);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link StmInfoSubwayDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.stm_info_db_version);
			}
			return dbVersion;
		}

		@NonNull
		private final Context context;

		StmInfoSubwayDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
			this.context = context;
		}

		@NonNull
		@Override
		public String getDbName() {
			return DB_NAME;
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_STM_INFO_SERVICE_UPDATE_SQL_DROP);
			PreferenceUtils.savePrefLclSync(this.context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L);
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_STM_INFO_SERVICE_UPDATE_SQL_CREATE);
		}
	}
}
