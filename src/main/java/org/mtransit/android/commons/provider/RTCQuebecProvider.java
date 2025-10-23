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
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.HtmlUtils;
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
import org.mtransit.android.commons.data.Direction;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.data.ServiceUpdateKtxKt;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.android.commons.helpers.MTDefaultHandler;
import org.mtransit.android.commons.provider.news.NewsTextFormatter;
import org.mtransit.commons.Cleaner;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.SourceUtils;
import org.mtransit.commons.provider.RTCQuebecProviderCommons;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@SuppressLint("Registered")
public class RTCQuebecProvider extends MTContentProvider implements StatusProviderContract, ServiceUpdateProviderContract {

	private static final String LOG_TAG = RTCQuebecProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	/**
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS = RTCQuebecDbHelper.PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS;

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
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
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
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.rtc_quebec_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
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
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
	 */
	@NonNull
	private static String getSERVICE_UPDATE_TARGET_AUTHORITY(@NonNull Context context) {
		if (serviceUpdateTargetAuthority == null) {
			serviceUpdateTargetAuthority = context.getResources().getString(R.string.rtc_quebec_service_update_for_poi_authority);
		}
		return serviceUpdateTargetAuthority;
	}

	private static final long SERVICE_UPDATE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L);
	private static final long SERVICE_UPDATE_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

	private static final long STATUS_MAX_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(5L);
	private static final long STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.SECONDS.toMillis(30L);
	private static final long STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.SECONDS.toMillis(30L);
	private static final long STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.SECONDS.toMillis(30L);

	@Override
	public long getMinDurationBetweenServiceUpdateRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public long getMinDurationBetweenRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public long getServiceUpdateMaxValidityInMs() {
		return SERVICE_UPDATE_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusMaxValidityInMs() {
		return STATUS_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getServiceUpdateValidityInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_VALIDITY_IN_MS;
	}

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return STATUS_VALIDITY_IN_MS;
	}

	@NonNull
	@Override
	public String getServiceUpdateDbTableName() {
		return RTCQuebecDbHelper.T_RTC_SERVICE_UPDATE;
	}

	@NonNull
	@Override
	public String getStatusDbTableName() {
		return RTCQuebecDbHelper.T_RTC_API_STATUS;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	@Override
	public void cacheServiceUpdates(@NonNull ArrayList<ServiceUpdate> newServiceUpdates) {
		ServiceUpdateProvider.cacheServiceUpdatesS(this, newServiceUpdates);
	}

	@Override
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
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
		final Map<String, String> targetUUIDs = getServiceUpdateTargetUUID(rds);
		ArrayList<ServiceUpdate> serviceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, targetUUIDs.values());
		enhanceServiceUpdate(serviceUpdates, rds.getRoute(), rds.getStop(), targetUUIDs);
		return serviceUpdates;
	}

	@Nullable
	private ArrayList<ServiceUpdate> getCachedServiceUpdates(@NonNull Route route) {
		final Map<String, String> targetUUIDs = getServiceUpdateTargetUUID(route);
		ArrayList<ServiceUpdate> serviceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, targetUUIDs.values());
		enhanceServiceUpdate(serviceUpdates, route, null, targetUUIDs);
		return serviceUpdates;
	}

	private void enhanceServiceUpdate(ArrayList<ServiceUpdate> serviceUpdates,
									  @Nullable Route route,
									  @Nullable Stop stop,
									  Map<String, String> targetUUIDs // different UUID from provider target UUID
	) {
		try {
			if (CollectionUtils.getSize(serviceUpdates) > 0) {
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					ServiceUpdateKtxKt.syncTargetUUID(serviceUpdate, targetUUIDs);
					enhanceServiceUpdate(serviceUpdate, route, stop);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route direction service update for stop!");
		}
	}

	private void enhanceServiceUpdate(ServiceUpdate serviceUpdate,
									  @Nullable Route route,
									  @Nullable Stop stop) {
		try {
			if (serviceUpdate.getSeverity() > ServiceUpdate.SEVERITY_NONE) {
				final String originalHtml = serviceUpdate.getTextHTML();
				if (stop != null) {
					final int severity = findStopSeverity(originalHtml, stop);
					if (severity > serviceUpdate.getSeverity()) {
						serviceUpdate.setSeverity(severity);
					}
				}
				serviceUpdate.setTextHTML(enhanceRTTextForStop(originalHtml, route, stop, serviceUpdate.getSeverity()));
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route direction service update '%s' for stop!", serviceUpdate);
		}
	}

	private String enhanceRTTextForStop(String originalHtml, @Nullable Route route, @Nullable Stop stop, int severity) {
		if (TextUtils.isEmpty(originalHtml)) {
			return originalHtml;
		}
		try {
			String html = originalHtml;
			html = enhanceHtmlRds(route, stop, html);
			html = enhanceHtmlSeverity(severity, html);
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route direction service update HTML '%s' for stop!", originalHtml);
			return originalHtml;
		}
	}

	private static final String NON_DIGIT = "\\D";
	private static final String NON_WORD = "\\W";

	private static final String CLEAN_THAT = "(^|[%s]{1})(%s)($|[%s]{1})";
	private static final String CLEAN_THAT_REPLACEMENT = "$1" + HtmlUtils.applyBold("$2") + "$3";

	private String enhanceHtmlRds(@Nullable Route route, @Nullable Stop stop, String html) {
		if (TextUtils.isEmpty(html)) {
			return html;
		}
		try {
			String code = stop == null ? null : stop.getCode();
			if (!TextUtils.isEmpty(code)) {
				String beforeCode = Character.isDigit(code.charAt(0)) ? NON_DIGIT : NON_WORD;
				String afterCode = Character.isDigit(code.charAt(code.length() - 1)) ? NON_DIGIT : NON_WORD;
				html = new Cleaner(
						String.format(CLEAN_THAT, beforeCode, code, afterCode),
						CLEAN_THAT_REPLACEMENT
				).clean(html);
			}
			String rsn = route == null ? null : route.getShortName();
			if (!TextUtils.isEmpty(rsn)) {
				String beforeRSN = Character.isDigit(rsn.charAt(0)) ? NON_DIGIT : NON_WORD;
				String afterRSN = Character.isDigit(rsn.charAt(rsn.length() - 1)) ? NON_DIGIT : NON_WORD;
				html = new Cleaner(
						String.format(CLEAN_THAT, beforeRSN, rsn, afterRSN),
						CLEAN_THAT_REPLACEMENT
				).clean(html);
			}
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while enhancing html for that stop!");
			return html;
		}
	}

	private static String enhanceHTML(String html) {
		try {
			html = HtmlUtils.fixTextViewBR(html);
			html = HtmlUtils.removeBold(html);
			html = HtmlUtils.removeSupSub(html);
			html = HtmlUtils.removeStyle(html);
			html = HtmlUtils.fixTextViewBRDuplicates(html);
			html = HtmlUtils.removeTables(html);
			return html;
		} catch (Exception e) {
			MTLog.w(LOG_TAG, e, "Error while enhancing HTML '%s'!", html);
			return html;
		}
	}

	// TODO escape HTML 1st instead of using special HTML characters
	private static final Cleaner CLEAN_BOLD_FR = ServiceUpdateCleaner.make("d&eacute;plac&eacute;s?", true);

	private String enhanceHtmlSeverity(int severity, String html) {
		if (TextUtils.isEmpty(html)) {
			return html;
		}
		final String replacement = ServiceUpdateCleaner.getReplacement(severity);
		if (replacement != null) {
			html = ServiceUpdateCleaner.clean(html, replacement);
			return CLEAN_BOLD_FR.clean(html, replacement);
		}
		return html;
	}

	private static final Cleaner NOT_SERVED = new Cleaner("(" + //
			"arr[e|ê]ts? non desservis?" //
			+ "|" //
			+ "arr&ecirc;ts? non desservis?" //
			+ ")", true);

	private static final Cleaner SUGGESTED = new Cleaner("(" //
			+ "arr[e|ê]ts? sugg[e|é]r[e|é]s?" //
			+ "|" //
			+ "arr&ecirc;ts? sugg&eacute;r&eacute;s?" //
			+ ")", true);

	private static final String STOP_CODE_FORMAT = "((^|[^0-9]){1}(%s)([^0-9]|$){1})";

	private int findStopSeverity(String originalHtml, @NonNull Stop stop) {
		if (!TextUtils.isEmpty(originalHtml)) {
			Matcher stopMatcher = new Cleaner(
					String.format(STOP_CODE_FORMAT, stop.getCode()),
					true
			).matcher(originalHtml);
			while (stopMatcher.find()) {
				if (stopMatcher.groupCount() < 3) {
					continue;
				}
				int stopStart = stopMatcher.start(3);
				int stopEnd = stopMatcher.end(3);
				Matcher notServedMatcher = NOT_SERVED.matcher(originalHtml);
				int notServedEnd = -1;
				while (notServedMatcher.find()) {
					if (notServedMatcher.start() < stopEnd) {
						notServedEnd = notServedMatcher.end();
					} else {
						break;
					}
				}
				Matcher suggestedMatcher = SUGGESTED.matcher(originalHtml);
				int suggestedEnd = -1;
				while (suggestedMatcher.find()) {
					if (suggestedMatcher.start() < stopEnd) {
						suggestedEnd = suggestedMatcher.end();
					} else {
						break;
					}
				}
				if (notServedEnd != -1) { // some stops not served
					if (suggestedEnd != -1) { // some stops suggested
						if (stopStart - notServedEnd < stopStart - suggestedEnd) {
							return ServiceUpdate.SEVERITY_WARNING_POI; // this stop not served
						}
					} else { // no stops suggested
						return ServiceUpdate.SEVERITY_WARNING_POI; // this stop not served
					}
				}
			}
		}
		MTLog.w(this, "findRDSSeverity() > Cannot find stop '%s' severity for '%s'.", stop, originalHtml);
		return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
	}

	@Nullable
	@Override
	public POIStatus getCachedStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule without schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteDirectionStop rds = scheduleStatusFilter.getRouteDirectionStop();
		if (TextUtils.isEmpty(rds.getStop().getCode())
				|| rds.getDirection().getId() < 0L
				|| TextUtils.isEmpty(rds.getRoute().getShortName())) {
			return null;
		}
		String uuid = getAgencyRouteStopTargetUUID(rds);
		POIStatus status = StatusProvider.getCachedStatusS(this, uuid);
		if (status != null) {
			status.setTargetUUID(rds.getUUID()); // target RDS UUID instead of custom tag
			// DESCENT ONLY SET BY API "descenteSeulement"
		}
		return status;
	}

	@NonNull
	private static String getAgencyRouteStopTargetUUID(@NonNull RouteDirectionStop rds) {
		return getAgencyRouteStopTargetUUID(rds.getAuthority(), rds.getRoute().getShortName(), rds.getDirection().getId(), rds.getStop().getCode());
	}

	@NonNull
	private static String getAgencyRouteStopTargetUUID(String agencyAuthority, String routeShortName, long directionId, String stopCode) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName, directionId, stopCode);
	}

	@NonNull
	private Map<String, String> getServiceUpdateTargetUUID(@NonNull RouteDirectionStop rds) {
		return getServiceUpdateTargetUUID(rds.getRoute());
	}

	@NonNull
	private Map<String, String> getServiceUpdateTargetUUID(@NonNull Route route) {
		final HashMap<String, String> targetUUIDs = new HashMap<>();
		targetUUIDs.put(getAgencyRouteShortNameTargetUUID(route.getAuthority(), route.getShortName()), route.getUUID());
		return targetUUIDs;
	}

	@NonNull
	private static String getAgencyRouteShortNameTargetUUID(@NonNull String agencyAuthority, @NonNull String routeShortName) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName);
	}

	@Override
	public boolean purgeUselessCachedServiceUpdates() {
		return ServiceUpdateProvider.purgeUselessCachedServiceUpdates(this);
	}

	@Override
	public boolean purgeUselessCachedStatuses() {
		return StatusProvider.purgeUselessCachedStatuses(this);
	}

	@Override
	public boolean deleteCachedServiceUpdate(@NonNull Integer serviceUpdateId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, serviceUpdateId);
	}

	@Override
	public boolean deleteCachedStatus(int cachedStatusId) {
		return StatusProvider.deleteCachedStatus(this, cachedStatusId);
	}

	@Override
	public boolean deleteCachedServiceUpdate(@NonNull String targetUUID, @NonNull String sourceId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, targetUUID, sourceId);
	}

	private static final String AGENCY_SOURCE_ID = "www_rtcquebec_ca_rtc_rss_aspx_type_avis_source_mobile";

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
		//noinspection deprecation // TODO fix & re-enable
		updateAgencyServiceUpdateDataIfRequired(requireContextCompat(), inFocus);
		return getCachedServiceUpdates(rds);
	}

	@Nullable
	private ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull Route route, boolean inFocus) {
		//noinspection deprecation // TODO fix & re-enable
		updateAgencyServiceUpdateDataIfRequired(requireContextCompat(), inFocus);
		return getCachedServiceUpdates(route);
	}

	@NonNull
	@Override
	public String getServiceUpdateLanguage() {
		return Locale.FRENCH.getLanguage();
	}

	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	private void updateAgencyServiceUpdateDataIfRequired(@NonNull Context context, boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS, 0L);
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyServiceUpdateDataIfRequiredSync(context, lastUpdateInMs, inFocus);
	}

	@Deprecated
	private synchronized void updateAgencyServiceUpdateDataIfRequiredSync(@NonNull Context context, long lastUpdateInMs, boolean inFocus) {
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
			updateAllAgencyServiceUpdateDataFromWWW(context, deleteAllRequired); // try to update
		}
	}

	@Deprecated
	private void updateAllAgencyServiceUpdateDataFromWWW(@NonNull Context context, boolean deleteAllRequired) {
		boolean deleteAllDone = false;
		if (deleteAllRequired) {
			deleteAllAgencyServiceUpdateData();
			deleteAllDone = true;
		}
		ArrayList<ServiceUpdate> newServiceUpdates = loadAgencyServiceUpdateDataFromWWW(context);
		if (newServiceUpdates != null) { // empty is OK
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyServiceUpdateData();
			}
			cacheServiceUpdates(newServiceUpdates);
			PreferenceUtils.savePrefLclSync(context, PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS, nowInMs);
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

	// TODO?
	// - https://www-api.rtcquebec.ca/api/notices #JSON #2019 #notUpdated
	// - https://www.rtcquebec.ca/cache/router-routes
	@Deprecated
	private static final String AGENCY_URL = "https://www.rtcquebec.ca/rtc/rss.aspx?type=avis&source=mobile";

	private static final String ENCODING = FileUtils.ISO_8859_1;

	private static final String PRIVATE_FILE_NAME = "rtcquebec.xml";

	@Deprecated
	@Nullable
	private ArrayList<ServiceUpdate> loadAgencyServiceUpdateDataFromWWW(@NonNull Context context) {
		try {
			String urlString = AGENCY_URL;
			MTLog.i(this, "Loading from '%s'...", urlString);
			final String sourceLabel = SourceUtils.getSourceLabel(AGENCY_URL);
			final Request urlRequest = new Request.Builder().url(urlString).build();
			try (Response response = getOkHttpClient(context).newCall(urlRequest).execute()) {
				switch (response.code()) {
				case HttpURLConnection.HTTP_OK:
					long newLastUpdateInMs = TimeUtils.currentTimeMillis();
					SAXParserFactory spf = SAXParserFactory.newInstance();
					SAXParser sp = spf.newSAXParser();
					XMLReader xr = sp.getXMLReader();
					RTCQuebecRSSAvisMobileDataHandler handler =
							new RTCQuebecRSSAvisMobileDataHandler(getSERVICE_UPDATE_TARGET_AUTHORITY(context), sourceLabel, newLastUpdateInMs,
									getServiceUpdateMaxValidityInMs(), getServiceUpdateLanguage());
					xr.setContentHandler(handler);
					FileUtils.copyToPrivateFile(context, PRIVATE_FILE_NAME, response.body().byteStream(), ENCODING);
					xr.parse(new InputSource(context.openFileInput(PRIVATE_FILE_NAME)));
					return handler.getServiceUpdates();
				default:
					MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", response.code(),
							response.message());
					return null;
				}
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

	@Nullable
	@Override
	public POIStatus getNewStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof Schedule.ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule without schedule filter!");
			return null;
		}
		Schedule.ScheduleStatusFilter scheduleStatusFilter = (Schedule.ScheduleStatusFilter) statusFilter;
		RouteDirectionStop rds = scheduleStatusFilter.getRouteDirectionStop();
		if (TextUtils.isEmpty(rds.getStop().getCode())
				|| TextUtils.isEmpty(rds.getRoute().getShortName())) {
			return null;
		}
		loadRealTimeStatusFromWWW(requireContextCompat(), rds);
		return getCachedStatus(statusFilter);
	}

	private static final TimeZone QUEBEC_CITY_TZ = TimeZone.getTimeZone("America/Montreal");

	private static final ThreadSafeDateFormatter DATE_FORMATTER;

	static {
		ThreadSafeDateFormatter dateFormatter = new ThreadSafeDateFormatter("yyyyMMdd", Locale.ENGLISH);
		dateFormatter.setTimeZone(QUEBEC_CITY_TZ);
		DATE_FORMATTER = dateFormatter;
	}

	// https://wssiteweb.rtcquebec.ca/api/v2/horaire/BorneVirtuelle_ArretParcours/?noParcours=1&noArret=1006&codeDirection=0&date=20200319
	private static final String REAL_TIME_URL_PART_1_BEFORE_ROUTE_NUMBER = "https://wssiteweb.rtcquebec.ca/api/v2/horaire/BorneVirtuelle_ArretParcours/?noParcours="; // rsn
	private static final String REAL_TIME_URL_PART_2_BEFORE_STOP_NUMBER = "&noArret="; // code
	private static final String REAL_TIME_URL_PART_3_BEFORE_DIRECTION_CODE = "&codeDirection="; // 0 or 1
	private static final String REAL_TIME_URL_PART_4_BEFORE_DATE = "&date="; // yyyyMMdd

	@NonNull
	private static String getRealTimeStatusUrlString(@NonNull RouteDirectionStop rds) {
		return REAL_TIME_URL_PART_1_BEFORE_ROUTE_NUMBER + //
				rds.getRoute().getShortName() +
				REAL_TIME_URL_PART_2_BEFORE_STOP_NUMBER + //
				rds.getStop().getCode() + //
				REAL_TIME_URL_PART_3_BEFORE_DIRECTION_CODE + //
				RTCQuebecProviderCommons.getDirectionCode(rds.getDirection().getId()) + //
				REAL_TIME_URL_PART_4_BEFORE_DATE + //
				DATE_FORMATTER.formatThreadSafe(TimeUtils.currentTimeMillis())
				;
	}

	private static final String APPLICATION_JSON = "application/JSON";
	private static final String ACCEPT = "accept";

	private void loadRealTimeStatusFromWWW(@NonNull Context context, @NonNull RouteDirectionStop rds) {
		try {
			String urlString = getRealTimeStatusUrlString(rds);
			MTLog.i(this, "Loading from '%s'...", urlString);
			String sourceLabel = SourceUtils.getSourceLabel(REAL_TIME_URL_PART_1_BEFORE_ROUTE_NUMBER);
			final Request urlRequest = new Request.Builder()
					.url(urlString)
					.addHeader(ACCEPT, APPLICATION_JSON)
					.build();
			try (Response response = getOkHttpClient(context).newCall(urlRequest).execute()) {
				switch (response.code()) {
				case HttpURLConnection.HTTP_OK:
					long newLastUpdateInMs = TimeUtils.currentTimeMillis();
					String jsonString = FileUtils.getString(response.body().byteStream());
					MTLog.d(this, "loadRealTimeStatusFromWWW() > jsonString: %s.", jsonString);
					JArretParcours jArretParcours = parseAgencyJSONArretParcours(jsonString);
					Collection<POIStatus> statuses = parseAgencyJSONArretParcoursHoraires(jArretParcours, rds, sourceLabel, newLastUpdateInMs);
					StatusProvider.deleteCachedStatus(this, ArrayUtils.asArrayList(getAgencyRouteStopTargetUUID(rds)));
					if (statuses != null) {
						MTLog.i(this, "Loaded %d statuses.", statuses.size());
						for (POIStatus status : statuses) {
							StatusProvider.cacheStatusS(this, status);
						}
					}
					return;
				default:
					MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", response.code(),
							response.message());
				}
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

	private static final String JSON_ARRET_NON_DESSERVI = "arretNonDesservi";
	private static final String JSON_DESCENTE_SEULEMENT = "descenteSeulement";
	private static final String JSON_HORAIRES = "horaires";
	private static final String JSON_ARRET = "arret";
	private static final String JSON_PARCOURS = "parcours";
	private static final String JSON_ACCESSIBLE = "accessible";
	private static final Boolean JSON_ACCESSIBLE_DEFAULT = null;
	private static final String JSON_DEPART = "depart";
	private static final String JSON_DEPART_MINUTES = "departMinutes";
	private static final String JSON_NTR = "ntr";
	private static final String JSON_ANNULE = "annule";
	private static final String JSON_NOM_DESTINATION = "nomDestination";

	@NonNull
	private JArretParcours parseAgencyJSONArretParcours(@Nullable String jsonString) {
		JArretParcours.JArret arret = new JArretParcours.JArret(JSON_ACCESSIBLE_DEFAULT);
		boolean arretNonDesservi = false;
		boolean descenteSeulement = false;
		List<JArretParcours.JHoraires> horaires = new ArrayList<>();
		JArretParcours.JParcours parcours = new JArretParcours.JParcours(JSON_ACCESSIBLE_DEFAULT);
		try {
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			arret = parseAgencyJSONArretParcoursArret(json);
			arretNonDesservi = json != null && json.optBoolean(JSON_ARRET_NON_DESSERVI, false);
			descenteSeulement = json != null && json.optBoolean(JSON_DESCENTE_SEULEMENT, false);
			parseAgencyJSONArretParcoursHoraires(horaires, json);
			parcours = parseAgencyJSONArretParcoursParcours(json);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
		}
		return new JArretParcours(arret, arretNonDesservi, descenteSeulement, horaires, parcours);
	}

	@NonNull
	private JArretParcours.JArret parseAgencyJSONArretParcoursArret(@Nullable JSONObject json) {
		try {
			if (json != null && json.has(JSON_ARRET)) {
				JSONObject jParcours = json.optJSONObject(JSON_ARRET);
				if (jParcours != null && jParcours.has(JSON_ACCESSIBLE)) {
					return new JArretParcours.JArret(jParcours.getBoolean(JSON_ACCESSIBLE));
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", json);
		}
		return new JArretParcours.JArret(JSON_ACCESSIBLE_DEFAULT);
	}

	@NonNull
	private JArretParcours.JParcours parseAgencyJSONArretParcoursParcours(@Nullable JSONObject json) {
		try {
			if (json != null && json.has(JSON_PARCOURS)) {
				JSONObject jParcours = json.optJSONObject(JSON_PARCOURS);
				if (jParcours != null && jParcours.has(JSON_ACCESSIBLE)) {
					return new JArretParcours.JParcours(jParcours.getBoolean(JSON_ACCESSIBLE));
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", json);
		}
		return new JArretParcours.JParcours(JSON_ACCESSIBLE_DEFAULT);
	}

	private void parseAgencyJSONArretParcoursHoraires(@NonNull List<JArretParcours.JHoraires> horaires, @Nullable JSONObject json) {
		try {
			if (json != null && json.has(JSON_HORAIRES)) {
				JSONArray jHoraires = json.getJSONArray(JSON_HORAIRES);
				if (jHoraires.length() > 0) {
					for (int r = 0; r < jHoraires.length(); r++) {
						JSONObject jHoraire = jHoraires.getJSONObject(r);
						if (jHoraire != null && jHoraire.has(JSON_DEPART)) {
							String jDepart = jHoraire.getString(JSON_DEPART);
							int jDepartMinutes = jHoraire.optInt(JSON_DEPART_MINUTES, -1);
							boolean jNTR = jHoraire.optBoolean(JSON_NTR, false);
							boolean jAnnule = jHoraire.optBoolean(JSON_ANNULE, false);
							String jNomDestination = jHoraire.optString(JSON_NOM_DESTINATION, StringUtils.EMPTY);
							horaires.add(new JArretParcours.JHoraires(jDepart, jDepartMinutes, jNTR, jAnnule, jNomDestination));
						}
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", json);
		}
	}

	private static final long PROVIDER_PRECISION_IN_MS = TimeUnit.SECONDS.toMillis(60L);

	// 2020-03-19 T 13:58:00-04:00 #ISO_8601
	// 'X' only supported API Level 24+ #ISO_8601
	private static final ThreadSafeDateFormatter PARSE_DATE_TIME = new ThreadSafeDateFormatter("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.ENGLISH);

	@Nullable
	private Collection<POIStatus> parseAgencyJSONArretParcoursHoraires(@NonNull JArretParcours jArretParcours,
																	   @NonNull RouteDirectionStop rds,
																	   @Nullable String sourceLabel,
																	   long newLastUpdateInMs) {
		try {
			// As seen on https://www.rtcquebec.ca/ :
			// - 1094 route 1 : jArretAccessible false | jParcoursAccessible true = POSSIBLE
			// - 1040 route 1 : jArretAccessible true | jParcoursAccessible true = POSSIBLE
			// - 1040 route 19 : jArretAccessible true | jParcoursAccessible false = UNKNOWN
			// - 1287 route 4: jArretAccessible true | jParcoursAccessible false = UNKNOWN
			//noinspection unused
			final Boolean jArretAccessible = jArretParcours.getArret().isAccessible();
			final Boolean jParcoursAccessible = jArretParcours.getParcours().isAccessible();
			final int accessible = Boolean.TRUE.equals(jParcoursAccessible) ? Accessibility.POSSIBLE : Accessibility.UNKNOWN;
			ArrayList<POIStatus> result = new ArrayList<>();
			List<JArretParcours.JHoraires> jHoraires = jArretParcours.getHoraires();
			if (jHoraires.isEmpty()) {
				result.add(
						new Schedule(
								null,
								getAgencyRouteStopTargetUUID(rds),
								newLastUpdateInMs,
								getStatusMaxValidityInMs(),
								newLastUpdateInMs,
								PROVIDER_PRECISION_IN_MS,
								jArretParcours.isDescenteSeulement(),
								sourceLabel,
								true // keep = no service today
						)
				);
				return result;
			}
			Schedule newSchedule = new Schedule(
					null,
					getAgencyRouteStopTargetUUID(rds),
					newLastUpdateInMs,
					getStatusMaxValidityInMs(),
					newLastUpdateInMs,
					PROVIDER_PRECISION_IN_MS,
					jArretParcours.isDescenteSeulement(),
					sourceLabel,
					false
			);
			for (int r = 0; r < jHoraires.size(); r++) {
				JArretParcours.JHoraires jHoraire = jHoraires.get(r);
				if (jHoraire == null) {
					continue;
				}
				if (jHoraire.isAnnule()) {
					continue; // SKIP cancelled scheduled
				}
				long departInMs = -1L;
				if (!jHoraire.getDepart().isEmpty()) {
					String jDepart = jHoraire.getDepart();
					try {
						final Date departDate = PARSE_DATE_TIME.parseThreadSafe(jDepart);
						if (departDate != null) {
							departInMs = departDate.getTime();
						}
					} catch (ParseException pe) {
						MTLog.w(this, pe, "Error while parsing date '%s'!", jDepart);
					}
				}
				if (departInMs < 0L) {
					int jDepartInMinutes = jHoraire.getDepartMinutes();
					if (jDepartInMinutes > 0L) {
						departInMs = newLastUpdateInMs + TimeUnit.MINUTES.toMillis(jDepartInMinutes);
					}
				}
				if (departInMs < 0L) {
					MTLog.w(this, "Skip '%s' w/o readable '%s' '%s'!", JSON_HORAIRES, JSON_DEPART, jHoraire);
					continue;
				}
				Schedule.Timestamp timestamp = new Schedule.Timestamp(TimeUtils.timeToTheMinuteMillis(departInMs), QUEBEC_CITY_TZ);
				if (jArretParcours.isDescenteSeulement()) {
					timestamp.setHeadsign(Direction.HEADSIGN_TYPE_NO_PICKUP, null);
				} else {
					String directionHeadSign = jHoraire.getNomDestination();
					if (!directionHeadSign.isEmpty()) {
						directionHeadSign = RTCQuebecProviderCommons.cleanTripHeadsign(directionHeadSign);
						String originalHeadSign = rds.getDirection().getHeadsignValue();
						if (!originalHeadSign.endsWith(directionHeadSign)) {
							timestamp.setHeadsign(Direction.HEADSIGN_TYPE_STRING, directionHeadSign);
						}
					}
				}
				timestamp.setRealTime(jHoraire.isNtr());
				if (FeatureFlags.F_ACCESSIBILITY_PRODUCER) {
					timestamp.setAccessible(accessible);
				}
				newSchedule.addTimestampWithoutSort(timestamp);
			}
			newSchedule.sortTimestamps();
			result.add(newSchedule);
			return result;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jArretParcours);
			return null;
		}
	}

	@MainThread
	@Override
	public boolean onCreateMT() {
		return true;
	}

	@Nullable
	private RTCQuebecDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private RTCQuebecDbHelper getDBHelper(@NonNull Context context) {
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
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return RTCQuebecDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
	 */
	@NonNull
	private RTCQuebecDbHelper getNewDbHelper(@NonNull Context context) {
		return new RTCQuebecDbHelper(context.getApplicationContext());
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

	@Deprecated
	private static class RTCQuebecRSSAvisMobileDataHandler extends MTDefaultHandler {

		private static final String LOG_TAG = RTCQuebecProvider.LOG_TAG + ">" + RTCQuebecRSSAvisMobileDataHandler.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private static final String RSS = "rss";
		private static final String CHANNEL = "channel";
		private static final String LANGUAGE = "language";
		private static final String TITLE = "title";
		private static final String DESCRIPTION = "description";
		private static final String ITEM = "item";
		private static final String AUTHOR = "author";
		private static final String LINK = "link";
		private static final String GUID = "guid";
		private static final String PUBLICATION_DATE = "pubDate";
		private static final String PARCOURS_IDS = "parcoursIds";
		private static final String CONTENT = "content";

		private String currentLocalName = RSS;
		private boolean currentItem = false;
		private final StringBuilder currentTitleSb = new StringBuilder();
		private final StringBuilder currentDescriptionSb = new StringBuilder();
		private final StringBuilder currentLinkSb = new StringBuilder();
		private final StringBuilder currentParcoursIdsSb = new StringBuilder();
		private final StringBuilder currentContentSb = new StringBuilder();
		private final StringBuilder currentGUIDSb = new StringBuilder();

		private final ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<>();

		private final String targetAuthority;
		@NonNull
		private final String sourceLabel;
		private final long newLastUpdateInMs;
		private final long serviceUpdateMaxValidityInMs;
		private final String language;

		RTCQuebecRSSAvisMobileDataHandler(String targetAuthority, @NonNull String sourceLabel, long newLastUpdateInMs, long serviceUpdateMaxValidityInMs, String language) {
			this.targetAuthority = targetAuthority;
			this.sourceLabel = sourceLabel;
			this.newLastUpdateInMs = newLastUpdateInMs;
			this.serviceUpdateMaxValidityInMs = serviceUpdateMaxValidityInMs;
			this.language = language;
		}

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
				this.currentLinkSb.setLength(0); // reset
				this.currentDescriptionSb.setLength(0); // reset
				this.currentGUIDSb.setLength(0); // reset
				this.currentParcoursIdsSb.setLength(0); // reset
				this.currentContentSb.setLength(0); // reset
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
					} else if (LINK.equals(this.currentLocalName)) {
						this.currentLinkSb.append(string);
					} else if (DESCRIPTION.equals(this.currentLocalName)) {
						this.currentDescriptionSb.append(string);
					} else if (GUID.equals(this.currentLocalName)) {
						this.currentGUIDSb.append(string);
					} else if (PARCOURS_IDS.equals(this.currentLocalName)) {
						this.currentParcoursIdsSb.append(string);
					} else if (CONTENT.equals(this.currentLocalName)) {
						this.currentContentSb.append(string);
					} else if (PUBLICATION_DATE.equals(this.currentLocalName)) { // ignore
					} else if (ITEM.equals(this.currentLocalName)) { // ignore
					} else if (AUTHOR.equals(this.currentLocalName)) { // ignore
					} else {
						MTLog.w(this, "characters() > Unexpected item element '%s'", this.currentLocalName);
					}
				} else if (TITLE.equals(this.currentLocalName)) { // ignore
				} else if (LINK.equals(this.currentLocalName)) { // ignore
				} else if (DESCRIPTION.equals(this.currentLocalName)) { // ignore
				} else if (RSS.equals(this.currentLocalName)) { // ignore
				} else if (LANGUAGE.equals(this.currentLocalName)) { // ignore
				} else if (CHANNEL.equals(this.currentLocalName)) { // ignore
				} else if (CONTENT.equals(this.currentLocalName)) { // ignore
				} else {
					MTLog.w(this, "characters() > Unexpected element '%s'", this.currentLocalName);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing '%s' value '%s, %s, %s'!", this.currentLocalName, ch, start, length);
			}
		}

		private static final String COLON = ": ";

		private static final String PARCOURS_IDS_SPLIT = ";";

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			try {
				if (ITEM.equals(localName)) {
					String[] parcourIds = this.currentParcoursIdsSb.toString().trim().split(PARCOURS_IDS_SPLIT);
					if (ArrayUtils.getSize(parcourIds) > 0) {
						String title = this.currentTitleSb.toString().trim();
						String desc = this.currentDescriptionSb.toString().trim();
						String link = this.currentLinkSb.toString().trim();
						String content = enhanceHTML(this.currentContentSb.toString().trim());
						StringBuilder textSb = new StringBuilder();
						StringBuilder textHTMLSb = new StringBuilder();
						if (!TextUtils.isEmpty(title)) {
							if (textSb.length() > 0) {
								textSb.append(COLON);
							}
							textSb.append(title);
							if (textHTMLSb.length() > 0) {
								textHTMLSb.append(HtmlUtils.BR);
							}
							textHTMLSb.append(NewsTextFormatter.formatHTMLTitle(title));
						}
						if (!TextUtils.isEmpty(desc)) {
							if (textSb.length() > 0) {
								textSb.append(COLON);
							}
							textSb.append(desc);
							textHTMLSb.append(NewsTextFormatter.getHTMLAfterTitleSpace(textHTMLSb.length()));
							textHTMLSb.append(HtmlUtils.applyBold(desc));
						}
						if (!TextUtils.isEmpty(content)) {
							if (textSb.length() > 0) {
								textSb.append(COLON);
							}
							textSb.append(HtmlUtils.fromHtml(content));
							if (textHTMLSb.length() > 0) {
								textHTMLSb.append(HtmlUtils.BR);
							}
							textHTMLSb.append(content);
						}
						if (!TextUtils.isEmpty(link)) {
							if (textHTMLSb.length() > 0) {
								textHTMLSb.append(HtmlUtils.BR);
							}
							textHTMLSb.append(HtmlUtils.linkify(link));
						}
						if (textSb.length() > 0) {
							Integer id = getId();
							int severity = ServiceUpdate.SEVERITY_INFO_RELATED_POI; // stop(s) on this route are concerned by some message(s)
							for (String parcourId : parcourIds) {
								if (TextUtils.isEmpty(parcourId)) {
									continue;
								}
								String targetUUID = RTCQuebecProvider.getAgencyRouteShortNameTargetUUID(this.targetAuthority, parcourId);
								ServiceUpdate serviceUpdate = new ServiceUpdate(id, targetUUID, this.newLastUpdateInMs, this.serviceUpdateMaxValidityInMs,
										textSb.toString(), textHTMLSb.toString(), severity, AGENCY_SOURCE_ID, sourceLabel, this.language);
								this.serviceUpdates.add(serviceUpdate);
							}
						}
					}
					this.currentItem = false;
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing '%s' end element!", this.currentLocalName);
			}
		}

		private Integer getId() {
			try {
				String guid = this.currentGUIDSb.toString().trim();
				if (!TextUtils.isDigitsOnly(guid)) {
					return Integer.valueOf(guid);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while generating ID from GUID '%s'!", this.currentGUIDSb);
			}
			return null;
		}
	}

	@SuppressWarnings({"unused", "WeakerAccess"})
	public static class JArretParcours {
		@NonNull
		private final JArret arret;
		private final boolean arretNonDesservi;
		private final boolean descenteSeulement;
		@NonNull
		private final List<JHoraires> horaires;
		@NonNull
		private final JParcours parcours;

		public JArretParcours(@NonNull JArret arret,
							  boolean arretNonDesservi,
							  boolean descenteSeulement,
							  @NonNull List<JHoraires> horaires,
							  @NonNull JParcours parcours) {
			this.arret = arret;
			this.arretNonDesservi = arretNonDesservi;
			this.descenteSeulement = descenteSeulement;
			this.horaires = horaires;
			this.parcours = parcours;
		}

		@NonNull
		public JArret getArret() {
			return arret;
		}

		public boolean isArretNonDesservi() {
			return arretNonDesservi;
		}

		public boolean isDescenteSeulement() {
			return descenteSeulement;
		}

		@NonNull
		public List<JHoraires> getHoraires() {
			return horaires;
		}

		@NonNull
		public JParcours getParcours() {
			return parcours;
		}

		@NonNull
		@Override
		public String toString() {
			return JArretParcours.class.getSimpleName() + "{" +
					"arret=" + arret +
					", arretNonDesservi=" + arretNonDesservi +
					", descenteSeulement=" + descenteSeulement +
					", horaires=" + horaires +
					", parcours=" + parcours +
					'}';
		}

		public static class JArret {

			@Nullable
			private final Boolean accessible;

			public JArret(@Nullable Boolean accessible) {
				this.accessible = accessible;
			}

			@Nullable
			public Boolean isAccessible() {
				return accessible;
			}

			@NonNull
			@Override
			public String toString() {
				return JArret.class.getSimpleName() + "{" +
						"accessible=" + accessible +
						'}';
			}
		}

		public static class JParcours {
			@Nullable
			private final Boolean accessible;

			public JParcours(@Nullable Boolean accessible) {
				this.accessible = accessible;
			}

			@Nullable
			public Boolean isAccessible() {
				return accessible;
			}

			@NonNull
			@Override
			public String toString() {
				return JParcours.class.getSimpleName() + "{" +
						"accessible=" + accessible +
						'}';
			}
		}

		public static class JHoraires {

			@NonNull
			private final String depart;
			private final int departMinutes;
			private final boolean ntr;
			private final boolean annule;
			@NonNull
			private final String nomDestination;

			public JHoraires(@NonNull String depart,
							 int departMinutes,
							 boolean ntr,
							 boolean annule,
							 @NonNull String nomDestination) {
				this.depart = depart;
				this.departMinutes = departMinutes;
				this.ntr = ntr;
				this.annule = annule;
				this.nomDestination = nomDestination;
			}

			@NonNull
			public String getDepart() {
				return depart;
			}

			public int getDepartMinutes() {
				return departMinutes;
			}

			public boolean isNtr() {
				return ntr;
			}

			public boolean isAnnule() {
				return annule;
			}

			@NonNull
			public String getNomDestination() {
				return nomDestination;
			}

			@NonNull
			@Override
			public String toString() {
				return JHoraires.class.getSimpleName() + "{" +
						"depart='" + depart + '\'' +
						", departMinutes=" + departMinutes +
						", ntr=" + ntr +
						", annule=" + annule +
						", nomDestination='" + nomDestination + '\'' +
						'}';
			}
		}
	}

	private static class RTCQuebecDbHelper extends MTSQLiteOpenHelper {

		private static final String LOG_TAG = RTCQuebecDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link RTCQuebecDbHelper} implementations in same app.
		 */
		static final String DB_NAME = "rtcquebec.db";

		/**
		 * Override if multiple {@link RTCQuebecDbHelper} implementations in same app.
		 */
		static final String PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS = "pRTCQuebecServiceUpdatesLastUpdate";

		static final String T_RTC_SERVICE_UPDATE = ServiceUpdateProvider.ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_RTC_SERVICE_UPDATE_SQL_CREATE = ServiceUpdateProvider.ServiceUpdateDbHelper.getSqlCreateBuilder(T_RTC_SERVICE_UPDATE)
				.build();

		private static final String T_RTC_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_RTC_SERVICE_UPDATE);

		static final String T_RTC_API_STATUS = StatusProvider.StatusDbHelper.T_STATUS;

		private static final String T_RTC_API_STATUS_SQL_CREATE = StatusProvider.StatusDbHelper.getSqlCreateBuilder(T_RTC_API_STATUS).build();

		private static final String T_RTC_API_STATUS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_RTC_API_STATUS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link RTCQuebecDbHelper} in same app.
		 */
		static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.rtc_quebec_db_version);
			}
			return dbVersion;
		}

		@NonNull
		private final Context context;

		RTCQuebecDbHelper(@NonNull Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
			this.context = context;
		}

		@Override
		public void onCreateMT(@NonNull SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_RTC_SERVICE_UPDATE_SQL_DROP);
			PreferenceUtils.savePrefLclSync(this.context, PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS, 0L);
			db.execSQL(T_RTC_API_STATUS_SQL_DROP);
			initAllDbTables(db);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_RTC_SERVICE_UPDATE_SQL_CREATE);
			db.execSQL(T_RTC_API_STATUS_SQL_CREATE);
		}
	}
}
