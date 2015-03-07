package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.FileUtils;
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
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.data.Trip;

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

@SuppressLint("Registered")
public class StmInfoBusProvider extends MTContentProvider implements ServiceUpdateProviderContract {

	private static final String TAG = StmInfoBusProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link StmInfoBusProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = StmInfoBusDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS;

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		ServiceUpdateProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link StmInfoBusProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link StmInfoBusProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.stm_info_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link StmInfoBusProvider} implementations in same app.
	 */
	public static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String agencyColor = null;

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	public static String getAgencyColor(Context context) {
		if (agencyColor == null) {
			agencyColor = context.getResources().getString(R.string.stm_info_agency_color);
		}
		return agencyColor;
	}

	private static final long SERVICE_UPDATE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1);
	private static final long SERVICE_UPDATE_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10);
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
		return StmInfoBusDbHelper.T_STM_INFO_BUS_SERVICE_UPDATE;
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
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		ArrayList<ServiceUpdate> serviceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, getAgencyTargetUUID(rts));
		enhanceRTServiceUpdateForStop(serviceUpdates, rts);
		return serviceUpdates;
	}

	private void enhanceRTServiceUpdateForStop(ArrayList<ServiceUpdate> serviceUpdates, RouteTripStop rts) {
		try {
			if (CollectionUtils.getSize(serviceUpdates) > 0) {
				Pattern stop = LocaleUtils.isFR() ? STOP_FR : STOP;
				Pattern yellowLine = LocaleUtils.isFR() ? YELLOW_LINE_FR : YELLOW_LINE;
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					serviceUpdate.setTargetUUID(rts.getUUID()); // route trip service update targets stop
					enhanceRTServiceUpdateForStop(serviceUpdate, rts, stop, yellowLine);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update for stop!");
		}
	}

	private void enhanceRTServiceUpdateForStop(ServiceUpdate serviceUpdate, RouteTripStop rts, Pattern stop, Pattern yellowLine) {
		try {
			if (serviceUpdate.getSeverity() > ServiceUpdate.SEVERITY_NONE) {
				String originalHtml = serviceUpdate.getTextHTML();
				int severity = findRTSSeverity(null, originalHtml, rts, stop, yellowLine);
				if (severity > serviceUpdate.getSeverity()) {
					serviceUpdate.setSeverity(severity);
				}
				serviceUpdate.setTextHTML(enhanceRTTextForStop(originalHtml, rts, serviceUpdate.getSeverity()));
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update '%s' for stop!", serviceUpdate);
		}
	}

	private String enhanceRTTextForStop(String originalHtml, RouteTripStop rts, int severity) {
		if (TextUtils.isEmpty(originalHtml)) {
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

	private String getAgencyTargetUUID(RouteTripStop rts) {
		String tagetAuthority = rts.getAuthority();
		long routeId = rts.getRoute().getId();
		String tripHeadsignValue = rts.getTrip().getHeadsignValue();
		return getAgencyTargetUUID(tagetAuthority, routeId, tripHeadsignValue);
	}

	private String getAgencyTargetUUID(String tagetAuthority, long routeId, String tripHeadsignValue) {
		return POI.POIUtils.getUUID(tagetAuthority, routeId, tripHeadsignValue);
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
			String agencyTargetUUID = getAgencyTargetUUID(rts);
			cachedServiceUpdates = ArrayUtils.asArrayList(getServiceUpdateNone(agencyTargetUUID));
			enhanceRTServiceUpdateForStop(cachedServiceUpdates, rts); // convert to stop service update
		}
		return cachedServiceUpdates;
	}

	public ServiceUpdate getServiceUpdateNone(String agencyTargetUUID) {
		return new ServiceUpdate(null, agencyTargetUUID, TimeUtils.currentTimeMillis(), getServiceUpdateMaxValidityInMs(), null, null,
				ServiceUpdate.SEVERITY_NONE, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, getServiceUpdateLanguage());
	}

	@Override
	public String getServiceUpdateLanguage() {
		return LocaleUtils.isFR() ? Locale.FRENCH.getLanguage() : Locale.ENGLISH.getLanguage();
	}

	private static final String AGENCY_SOURCE_ID = "www_stm_info_etats_du_service";

	private static final String AGENCY_SOURCE_LABEL = "www.stm.info";

	private void updateAgencyServiceUpdateDataIfRequired(String tagetAuthority, boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l);
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyServiceUpdateDataIfRequiredSync(tagetAuthority, lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyServiceUpdateDataIfRequiredSync(String tagetAuthority, long lastUpdateInMs, boolean inFocus) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l) > lastUpdateInMs) {
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		if (lastUpdateInMs + getServiceUpdateMaxValidityInMs() < nowInMs) {
			deleteAllRequired = true; // too old to display
		}
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		if (deleteAllRequired || lastUpdateInMs + minUpdateMs < nowInMs) {
			updateAllAgencyServiceUpdateDataFromWWW(tagetAuthority, deleteAllRequired); // try to update
		}
	}

	private void updateAllAgencyServiceUpdateDataFromWWW(String tagetAuthority, boolean deleteAllRequired) {
		boolean deleteAllDone = false;
		if (deleteAllRequired) {
			deleteAllAgencyServiceUpdateData();
			deleteAllDone = true;
		}
		ArrayList<ServiceUpdate> newServiceUpdates = loadAgencyServiceUpdateDataFromWWW(tagetAuthority);
		if (newServiceUpdates != null) { // empty is OK
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyServiceUpdateData();
			}
			cacheServiceUpdates(newServiceUpdates);
			PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, nowInMs, true); // sync
		} // else keep whatever we have until max validity reached
	}


	private ArrayList<ServiceUpdate> loadAgencyServiceUpdateDataFromWWW(String tagetAuthority) {
		try {
			String urlString = getAgencyUrlString();
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				return parseAgencyJson(jsonString, newLastUpdateInMs, tagetAuthority);
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
		} catch (Exception e) {
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private static final String JSON_BUS_INTERNE = "bus-interne";
	private static final String JSON_LIGNES = "lignes";

	private ArrayList<ServiceUpdate> parseAgencyJson(String jsonString, long nowInMs, String targetAuthority) {
		try {
			ArrayList<ServiceUpdate> result = new ArrayList<ServiceUpdate>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_BUS_INTERNE)) {
				JSONObject jBusInterne = json.getJSONObject(JSON_BUS_INTERNE);
				if (jBusInterne.has(JSON_LIGNES)) {
					JSONArray jLignes = jBusInterne.getJSONArray(JSON_LIGNES);
					for (int l = 0; l < jLignes.length(); l++) {
						JSONObject jLigne = jLignes.getJSONObject(l);
						JSONArray jLigneNames = jLigne.names();
						if (jLigneNames != null) {
							for (int ln = 0; ln < jLigneNames.length(); ln++) {
								String jLigneName = jLigneNames.getString(ln);
								JSONArray jLigneArray = jLigne.getJSONArray(jLigneName);
								long maxValidityInMs = getServiceUpdateMaxValidityInMs();
								String language = getServiceUpdateLanguage();
								for (int la = 0; la < jLigneArray.length(); la++) {
									JSONObject jLigneObject = jLigneArray.getJSONObject(la);
									ServiceUpdate serviceUpdate = parseAgencyJsonText(jLigneObject, targetAuthority, jLigneName, nowInMs, maxValidityInMs,
											language);
									if (serviceUpdate != null) {
										result.add(serviceUpdate);
									}
								}
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

	private static final String JSON_DIRECTION_NAME = "direction_name";
	private static final String JSON_TEXT = "text";

	private ServiceUpdate parseAgencyJsonText(JSONObject jLigneObject, String targetAuthority, String routeId, long nowInMs, long maxValidityInMs,
			String language) {
		try {
			String directionName = jLigneObject.getString(JSON_DIRECTION_NAME);
			String text = jLigneObject.getString(JSON_TEXT);
			int routeIdInt = Integer.parseInt(routeId);
			String tripHeadsignValue = parseAgencyTripHeadsignValue(directionName, routeIdInt);
			String targetUUID = getAgencyTargetUUID(targetAuthority, routeIdInt, tripHeadsignValue);
			if (!TextUtils.isEmpty(text)) {
				int severity = ServiceUpdate.SEVERITY_INFO_RELATED_POI;
				String textHtml = enhanceHtml(text, null, ServiceUpdate.SEVERITY_NONE); // no severity based enhancement here
				return new ServiceUpdate(null, targetUUID, nowInMs, maxValidityInMs, text, textHtml, severity, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, language);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON message '%s'!", jLigneObject);
		}
		return null;
	}

	private String parseAgencyTripHeadsignValue(String directionName, int routeId) {
		if (!TextUtils.isEmpty(directionName)) {
			if (directionName.startsWith("N")) { // North / Nord
				return Trip.HEADING_NORTH;
			} else if (directionName.startsWith("S")) { // South / Sud
				return Trip.HEADING_SOUTH;
			} else if (directionName.startsWith("E")) { // Est / East
				return Trip.HEADING_EAST;
			} else if (directionName.startsWith("W") | directionName.startsWith("O")) { // West / Ouest
				return Trip.HEADING_WEST;
			}
			if (routeId == 37) {
				if (directionName.toLowerCase(Locale.ENGLISH).contains("angrignon")) {
					return Trip.HEADING_SOUTH;
				} else if (directionName.toLowerCase(Locale.ENGLISH).contains("vendôme")) {
					return Trip.HEADING_NORTH;
				}
			} else if (routeId == 747) {
				if (directionName.toLowerCase(Locale.ENGLISH).contains("airport")) {
					return Trip.HEADING_WEST;
				} else if (directionName.toLowerCase(Locale.ENGLISH).contains("downtown")) {
					return Trip.HEADING_EAST;
				}
			}
		}
		MTLog.w(this, "parseAgencyTripHeadsignValue() > unexpected direction '%s'!", directionName);
		return null;
	}

	private static final String AGENCY_URL_PART_1_BEFORE_LANG = "http://www.stm.info/";
	private static final String AGENCY_URL_PART_2_AFTER_LANG = "/ajax/etats-du-service";
	private static final String AGENCY_URL_LANG_DEFAULT = "en";
	private static final String AGENCY_URL_LANG_FRENCH = "fr";

	private static String getAgencyUrlString() {
		return new StringBuilder() //
				.append(AGENCY_URL_PART_1_BEFORE_LANG) //
				.append(LocaleUtils.isFR() ? AGENCY_URL_LANG_FRENCH : AGENCY_URL_LANG_DEFAULT) // language
				.append(AGENCY_URL_PART_2_AFTER_LANG) //
				.toString();
	}

	private HashMap<String, Long> recentlyLoadedTargetUUID = new HashMap<String, Long>();

	@SuppressWarnings("unused")
	private synchronized ArrayList<ServiceUpdate> updateRTSDataFromWWW(RouteTripStop rts, ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		long nowInMs = TimeUtils.currentTimeMillis();
		Long lastTimeLoaded = this.recentlyLoadedTargetUUID.get(rts.getUUID());
		boolean inFocus = serviceUpdateFilter.isInFocusOrDefault();
		if (lastTimeLoaded != null && lastTimeLoaded + getServiceUpdateValidityInMs(inFocus) > nowInMs) {
			return getCachedServiceUpdates(serviceUpdateFilter);
		}
		ArrayList<ServiceUpdate> result = loadRTSDataFromWWW(rts, nowInMs);
		this.recentlyLoadedTargetUUID.put(rts.getUUID(), nowInMs);
		deleteCachedServiceUpdate(rts.getUUID(), RTS_SOURCE_ID);
		cacheServiceUpdates(result);
		return result;
	}

	private static final String RTS_SOURCE_ID = "www_stm_info_lines_stops_arrivals";
	private static final String RTS_SOURCE_LABEL = "www.stm.info";

	private ArrayList<ServiceUpdate> loadRTSDataFromWWW(RouteTripStop rts, long nowInMs) {
		try {
			String urlString = getRTSUrlStringWithDateAndTime(rts, nowInMs);
			URL url = new URL(urlString);
			URLConnection urlConnection = url.openConnection();
			HttpURLConnection httpsUrlConnection = (HttpURLConnection) urlConnection;
			switch (httpsUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				ArrayList<ServiceUpdate> result = new ArrayList<ServiceUpdate>();
				String jsonString = FileUtils.getString(urlConnection.getInputStream());
				ArrayList<ServiceUpdate> parseResult = parseRTSJson(jsonString, rts, nowInMs);
				if (parseResult != null) {
					result.addAll(parseResult);
				}
				if (CollectionUtils.getSize(result) == 0) {
					ServiceUpdate serviceUpdateNone = new ServiceUpdate(null, rts.getUUID(), nowInMs, getServiceUpdateMaxValidityInMs(), null, null,
							ServiceUpdate.SEVERITY_NONE, RTS_SOURCE_ID, RTS_SOURCE_LABEL, getServiceUpdateLanguage());
					result.add(serviceUpdateNone);
				}
				return result;
			default:
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpsUrlConnection.getResponseCode(),
						httpsUrlConnection.getResponseMessage());
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
		} catch (Exception e) {
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private static final String BUS_STOP = "bus stop[\\S]*";
	private static final String BUS_STOP_FR = "arr[ê|e]t[\\S]*";

	private static final Pattern STOP = Pattern.compile("(" + BUS_STOP + ")", Pattern.CASE_INSENSITIVE);
	private static final Pattern STOP_FR = Pattern.compile("(" + BUS_STOP_FR + ")", Pattern.CASE_INSENSITIVE);

	private static final Pattern YELLOW_LINE = Pattern.compile("(Yellow line)", Pattern.CASE_INSENSITIVE);
	private static final Pattern YELLOW_LINE_FR = Pattern.compile("(ligne jaune)", Pattern.CASE_INSENSITIVE);

	private static final String JSON_MESSAGES = "messages";

	private ArrayList<ServiceUpdate> parseRTSJson(String jsonString, RouteTripStop rts, long nowInMs) {
		try {
			ArrayList<ServiceUpdate> result = new ArrayList<ServiceUpdate>();
			JSONObject jResponse = jsonString == null ? null : new JSONObject(jsonString);
			if (jResponse != null && jResponse.has(JSON_MESSAGES)) {
				JSONArray jMessages = jResponse.getJSONArray(JSON_MESSAGES);
				Pattern stop = LocaleUtils.isFR() ? STOP_FR : STOP;
				Pattern yellowLine = LocaleUtils.isFR() ? YELLOW_LINE_FR : YELLOW_LINE;
				long maxValidityInMs = getServiceUpdateMaxValidityInMs();
				String language = getServiceUpdateLanguage();
				String targetUUID = rts.getUUID();
				for (int i = 0; i < jMessages.length(); i++) {
					JSONObject jMessage = jMessages.getJSONObject(i);
					ServiceUpdate serviceUpdate = parseRTSJsonMessage(jMessage, rts, nowInMs, stop, yellowLine, maxValidityInMs, language, targetUUID);
					if (serviceUpdate != null) {
						result.add(serviceUpdate);
					}
				}
			}
			return result;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	private ServiceUpdate parseRTSJsonMessage(JSONObject jMessage, RouteTripStop rts, long nowInMs, Pattern stop, Pattern yellowLine, long maxValidityInMs,
			String language, String targetUUID) {
		try {
			String jMessageText = jMessage.getString("text");
			if (!TextUtils.isEmpty(jMessageText)) {
				int severity = findRTSSeverity(jMessage, jMessageText, rts, stop, yellowLine);
				String text = Html.fromHtml(jMessageText).toString();
				String textHtml = enhanceHtml(jMessageText, rts, severity);
				return new ServiceUpdate(null, targetUUID, nowInMs, maxValidityInMs, text, textHtml, severity, RTS_SOURCE_ID, RTS_SOURCE_LABEL, language);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON message '%s'!", jMessage);
		}
		return null;
	}

	private static final String POINT = "\\.";
	private static final String PARENTHESES1 = "\\(";
	private static final String PARENTHESES2 = "\\)";
	private static final String SLASH = "/";
	private static final String ANY_STOP_CODE = "[\\d]+";

	private static final Pattern CLEAN_BR = Pattern.compile("(" + PARENTHESES2 + ",|" + POINT + "|:)[\\s]+");
	private static final String CLEAN_BR_REPLACEMENT = "$1" + HtmlUtils.BR;

	private static final Pattern CLEAN_STOP_CODE_AND_NAME = Pattern.compile("(" + ANY_STOP_CODE + ")[\\s]*" + PARENTHESES1 + "([^" + SLASH + "]*)" + SLASH
			+ "([^" + PARENTHESES2 + "]*)" + PARENTHESES2 + "([" + PARENTHESES2 + "]*)" + "([,]*)([.]*)");
	private static final String CLEAN_STOP_CODE_AND_NAME_REPLACEMENT = "- $2" + SLASH + "$3$4 " + PARENTHESES1 + "$1" + PARENTHESES2 + "$5$6";

	private static final String CLEAN_THAT_STOP_CODE = "(\\-[\\s]+)" + "([^" + SLASH + "]*" + SLASH + "[^" + PARENTHESES1 + "]*" + PARENTHESES1 + "%s"
			+ PARENTHESES2 + ")";
	private static final String CLEAN_THAT_STOP_CODE_REPLACEMENT = "$1" + HtmlUtils.applyBold("$2");

	private String enhanceHtml(String originalHtml, RouteTripStop optRts, Integer optSeverity) {
		if (TextUtils.isEmpty(originalHtml)) {
			return originalHtml;
		}
		try {
			String html = originalHtml;
			html = CLEAN_BR.matcher(html).replaceAll(CLEAN_BR_REPLACEMENT);
			html = CLEAN_STOP_CODE_AND_NAME.matcher(html).replaceAll(CLEAN_STOP_CODE_AND_NAME_REPLACEMENT);
			if (optRts != null) {
				html = enhanceHtmlRts(optRts, html);
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

	private static final Pattern CLEAN_BOLD = Pattern.compile("(" + BUS_STOP + "|relocat[\\S]*|cancel[\\S]*)");
	private static final Pattern CLEAN_BOLD_FR = Pattern.compile("(" + BUS_STOP_FR + "|déplac[\\S]*|annul[\\S]*)");
	private static final String CLEAN_BOLD_REPLACEMENT = HtmlUtils.applyBold("$1");

	private String enhanceHtmlSeverity(int severity, String html) {
		if (TextUtils.isEmpty(html)) {
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

	private String enhanceHtmlRts(RouteTripStop rts, String html) {
		// MTLog.v(this, "enhanceHtmlRts(%s,%s) #ServiceUpdate", rts, html);
		if (TextUtils.isEmpty(html)) {
			return html;
		}
		return Pattern.compile(String.format(CLEAN_THAT_STOP_CODE, rts.getStop().getCode())).matcher(html).replaceAll(CLEAN_THAT_STOP_CODE_REPLACEMENT);
	}

	private static final Pattern CLEAN_TIME = Pattern.compile("([\\d]{1,2})[\\s]*[:|h][\\s]*([\\d]{2})([\\s]*([a|p]m))?", Pattern.CASE_INSENSITIVE);

	private static final Pattern CLEAN_DATE = Pattern.compile("([\\d]{1,2}[\\s]*[a-zA-Z]+[\\s]*[\\d]{4})");

	private static final ThreadSafeDateFormatter PARSE_TIME = new ThreadSafeDateFormatter("HH:mm");

	private static final ThreadSafeDateFormatter PARSE_TIME_AMPM = new ThreadSafeDateFormatter("hh:mm a");

	private static final ThreadSafeDateFormatter FORMAT_DATE = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM);

	private static final String PARSE_DATE_REGEX = "dd MMMM yyyy";

	private static final TimeZone TZ = TimeZone.getTimeZone("America/Montreal");

	private String enhanceHtmlDateTime(String html) throws ParseException {
		if (TextUtils.isEmpty(html)) {
			return html;
		}
		Matcher timeMatcher = CLEAN_TIME.matcher(html);
		while (timeMatcher.find()) {
			String time = timeMatcher.group(0);
			String hours = timeMatcher.group(1);
			String minutes = timeMatcher.group(2);
			String ampm = StringUtils.trim(timeMatcher.group(3));
			Date timeD;
			if (TextUtils.isEmpty(ampm)) {
				PARSE_TIME.setTimeZone(TZ);
				timeD = PARSE_TIME.parseThreadSafe(hours + ":" + minutes);
			} else {
				PARSE_TIME_AMPM.setTimeZone(TZ);
				timeD = PARSE_TIME_AMPM.parseThreadSafe(hours + ":" + minutes + " " + ampm);
			}
			String fTime = TimeUtils.formatTime(getContext(), timeD);
			html = html.replace(time, fTime);
		}
		Matcher dateMatcher = CLEAN_DATE.matcher(html);
		ThreadSafeDateFormatter parseDate = new ThreadSafeDateFormatter(PARSE_DATE_REGEX, LocaleUtils.isFR() ? Locale.FRENCH : Locale.ENGLISH);
		while (dateMatcher.find()) {
			String date = dateMatcher.group(0);
			Date dateD = parseDate.parseThreadSafe(date);
			String fDate = FORMAT_DATE.formatThreadSafe(dateD);
			html = html.replace(date, fDate);
		}
		return html;
	}

	private static final String STM_INFO_LEVEL_CORPORATIVE = "Corporative";
	private static final String STM_INFO_LEVEL_STOP_ROUTE = "StopRoute";

	private int findRTSSeverity(JSONObject optJMessage, String jMessageText, RouteTripStop rts, Pattern stop, Pattern yellowLine) {
		try {
			if (optJMessage != null && optJMessage.has("level")) {
				String level = optJMessage.getString("level");
				if (STM_INFO_LEVEL_CORPORATIVE.equalsIgnoreCase(level)) {
					return ServiceUpdate.SEVERITY_INFO_AGENCY;
				}
				if (STM_INFO_LEVEL_STOP_ROUTE.equalsIgnoreCase(level)) {
					if (jMessageText.contains(rts.getStop().getCode())) {
						return ServiceUpdate.SEVERITY_WARNING_POI;
					} else {
						return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to use custom JSON fields to find RTS severity in '%s'!", optJMessage);
		}
		if (!TextUtils.isEmpty(jMessageText)) {
			if (jMessageText.contains(rts.getStop().getCode())) {
				return ServiceUpdate.SEVERITY_WARNING_POI;
			} else if (stop.matcher(jMessageText).find()) {
				return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
			} else if (yellowLine.matcher(jMessageText).find()) {
				return ServiceUpdate.SEVERITY_INFO_AGENCY;
			}
		}
		MTLog.w(this, "Cannot find RTS severity for '%s'. (%s)", jMessageText, optJMessage);
		return ServiceUpdate.SEVERITY_INFO_UNKNOWN;
	}

	private static final String RTS_URL_PART_1_BEFORE_LANG = "http://i-www.stm.info/";
	private static final String RTS_URL_PART_2_BEFORE_ROUTE_ID = "/lines/";
	private static final String RTS_URL_PART_3_BEFORE_STOP_ID = "/stops/";
	private static final String RTS_URL_PART_4_BEFORE_TRIP_HEADSIGN_VALUE = "/arrivals?direction=";
	private static final String RTS_URL_PART_5_BEFORE_LIMIT = "&limit=";
	private static final String RTS_URL_PART_6_BEFORE_DATE = "&d=";
	private static final String RTS_URL_PART_7_BEFORE_TIME = "&t=";
	private static final String RTS_URL_LANG_DEFAULT = "en";
	private static final String RTS_URL_LANG_FRENCH = "fr";
	private static final ThreadSafeDateFormatter RTS_URL_DATE_FORMAT = new ThreadSafeDateFormatter("yyyyMMdd");
	private static final ThreadSafeDateFormatter RTS_URL_TIME_FORMAT = new ThreadSafeDateFormatter("HHmm");

	private static String getRTSUrlStringWithDateAndTime(RouteTripStop rts, long nowInMs) {
		Date nowDate = new Date(nowInMs);
		return new StringBuilder() //
				.append(RTS_URL_PART_1_BEFORE_LANG).append(LocaleUtils.isFR() ? RTS_URL_LANG_FRENCH : RTS_URL_LANG_DEFAULT) // language
				.append(RTS_URL_PART_2_BEFORE_ROUTE_ID).append(rts.getRoute().getId()) // route ID
				.append(RTS_URL_PART_3_BEFORE_STOP_ID).append(rts.getStop().getId()) // stop ID
				.append(RTS_URL_PART_4_BEFORE_TRIP_HEADSIGN_VALUE).append(rts.getTrip().getHeadsignValue()) // trip HEADSIGN (E/W/N/S)
				.append(RTS_URL_PART_5_BEFORE_LIMIT).append(50) // without limit, return all schedule for the day
				.append(RTS_URL_PART_6_BEFORE_DATE).append(RTS_URL_DATE_FORMAT.formatThreadSafe(nowDate)) // date 20100602
				.append(RTS_URL_PART_7_BEFORE_TIME).append(RTS_URL_TIME_FORMAT.formatThreadSafe(nowDate)) // time 2359
				.toString();
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

	private static StmInfoBusDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private StmInfoBusDbHelper getDBHelper(Context context) {
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
	 * Override if multiple {@link StmInfoBusProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return StmInfoBusDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link StmInfoBusProvider} implementations in same app.
	 */
	public StmInfoBusDbHelper getNewDbHelper(Context context) {
		return new StmInfoBusDbHelper(context.getApplicationContext());
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

	public static class StmInfoBusDbHelper extends ServiceUpdateProvider.ServiceUpdateDbHelper {

		private static final String TAG = StmInfoBusDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link StmInfoBusDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "stm_info.db";

		/**
		 * Override if multiple {@link StmInfoBusDbHelper} implementations in same app.
		 */
		protected static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pStmInfoBusEtatsDuServiceLastUpdate";

		public static final String T_STM_INFO_BUS_SERVICE_UPDATE = ServiceUpdateProvider.ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_STM_INFO_BUS_SERVICE_UPDATE_SQL_CREATE = ServiceUpdateProvider.ServiceUpdateDbHelper.getSqlCreateBuilder(
				T_STM_INFO_BUS_SERVICE_UPDATE).build();

		private static final String T_STM_INFO_BUS_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STM_INFO_BUS_SERVICE_UPDATE);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link StmInfoBusDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.stm_info_db_version);
			}
			return dbVersion;
		}

		private Context context;

		public StmInfoBusDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
			this.context = context;
		}

		@Override
		public String getDbName() {
			return DB_NAME;
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_STM_INFO_BUS_SERVICE_UPDATE_SQL_DROP);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l, true);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_STM_INFO_BUS_SERVICE_UPDATE_SQL_CREATE);
		}
	}
}
