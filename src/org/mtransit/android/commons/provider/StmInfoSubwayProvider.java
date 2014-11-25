package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
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
import org.mtransit.android.commons.provider.ServiceUpdateProvider.ServiceUpdateColumns;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

@SuppressLint("Registered")
public class StmInfoSubwayProvider extends MTContentProvider implements ServiceUpdateProviderContract {

	private static final String TAG = StmInfoSubwayProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = StmInfoSubwayDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS;

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		ServiceUpdateProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.stm_info_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	public static Uri getAUTHORITYURI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static final long SERVICE_UPDATE_MAX_VALIDITY_IN_MS = 1 * TimeUtils.ONE_DAY_IN_MS;

	private static final long SERVICE_UPDATE_VALIDITY_IN_MS = 1 * TimeUtils.ONE_HOUR_IN_MS;

	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = 10 * TimeUtils.ONE_MINUTE_IN_MS;

	@Override
	public long getMinDurationBetweenServiceUpdateRefreshInMs() {
		return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public long getServiceUpdateMaxValidityInMs() {
		return SERVICE_UPDATE_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getServiceUpdateValidityInMs() {
		return SERVICE_UPDATE_VALIDITY_IN_MS;
	}

	@Override
	public String getServiceUpdateDbTableName() {
		return StmInfoSubwayDbHelper.T_STM_INFO_SERVICE_UPDATE;
	}

	@Override
	public void cacheServiceUpdates(Collection<ServiceUpdate> newServiceUpdates) {
		ServiceUpdateProvider.cacheServiceUpdatesS(getContext(), this, newServiceUpdates);
	}

	@Override
	public Collection<ServiceUpdate> getCachedServiceUpdates(ServiceUpdateProvider.ServiceUpdateFilter serviceUpdateFilter) {
		if (serviceUpdateFilter.getPoi() == null || !(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getCachedServiceUpdates() > no service update (poi null or not RTS)");
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		Collection<ServiceUpdate> routeTripServiceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, getAgencyTargetUUID(rts));
		enhanceRTServiceUpdateForStop(routeTripServiceUpdates, rts);
		return routeTripServiceUpdates;
	}

	private void enhanceRTServiceUpdateForStop(Collection<ServiceUpdate> serviceUpdates, RouteTripStop rts) {
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

	private void enhanceRTServiceUpdateForStop(ServiceUpdate serviceUpdate, RouteTripStop rts) {
		try {
			final String originalHtml = serviceUpdate.getTextHTML();
			serviceUpdate.setTextHTML(enhanceRTTextForStop(originalHtml, rts, serviceUpdate.getSeverity()));
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
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update HTML '%s' for stop!", originalHtml);
			return originalHtml;
		}
	}

	private String getAgencyTargetUUID(RouteTripStop rts) {
		final String tagetAuthority = rts.getAuthority();
		final int routeId = rts.route.id;
		return getAgencyTargetUUID(tagetAuthority, routeId);
	}

	private String getAgencyTargetUUID(String tagetAuthority, int routeId) {
		return POI.POIUtils.getUUID(tagetAuthority, routeId);
	}

	@Override
	public boolean purgeUselessCachedServiceUpdates() {
		return ServiceUpdateProvider.purgeUselessCachedServiceUpdates(getContext(), this);
	}

	@Override
	public boolean deleteCachedServiceUpdate(Integer serviceUpdateId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(getContext(), this, serviceUpdateId);
	}

	@Override
	public boolean deleteCachedServiceUpdate(String targetUUID, String sourceId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(getContext(), this, targetUUID, sourceId);
	}

	private int deleteAllAgencyServiceUpdateData() {
		int affectedRows = 0;
		SQLiteDatabase db = null;
		try {
			db = getDBHelper().getWritableDatabase();
			String selection = new StringBuilder() //
					.append(ServiceUpdateColumns.T_SERVICE_UPDATE_K_SOURCE_ID).append("=").append('\'').append(AGENCY_SOURCE_ID).append('\'') //
					.toString();
			affectedRows = db.delete(getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all agency service update data!");
		}
		return affectedRows;
	}

	@Override
	public Collection<ServiceUpdate> getNewServiceUpdates(ServiceUpdateProvider.ServiceUpdateFilter serviceUpdateFilter) {
		if (serviceUpdateFilter == null || serviceUpdateFilter.getPoi() == null || !(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getNewServiceUpdates() > no new service update (filter null or poi null or not RTS): %s", serviceUpdateFilter);
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		updateAgencyServiceUpdateDataIfRequired(rts.getAuthority());
		Collection<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(serviceUpdateFilter);
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			String agencyTargetUUID = getAgencyTargetUUID(rts);
			cachedServiceUpdates = Arrays.asList(new ServiceUpdate[] { getServiceUpdateNone(agencyTargetUUID) });
			enhanceRTServiceUpdateForStop(cachedServiceUpdates, rts); // convert to stop service update
		}
		return cachedServiceUpdates;
	}

	public ServiceUpdate getServiceUpdateNone(String agencyTargetUUID) {
		final String language = LocaleUtils.isFR() ? Locale.FRENCH.getLanguage() : StringUtils.EMPTY;
		ServiceUpdate serviceUpdateNone = new ServiceUpdate(null, agencyTargetUUID, TimeUtils.currentTimeMillis(), getServiceUpdateMaxValidityInMs(), null,
				null, ServiceUpdate.SEVERITY_NONE, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, language);
		return serviceUpdateNone;
	}

	public static final String AGENCY_SOURCE_ID = "www_stm_info_etats_du_service";

	public static final String AGENCY_SOURCE_LABEL = "www.stm.info";

	private void updateAgencyServiceUpdateDataIfRequired(String tagetAuthority) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l);
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs());
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyServiceUpdateDataIfRequiredSync(tagetAuthority, lastUpdateInMs);
	}

	private synchronized void updateAgencyServiceUpdateDataIfRequiredSync(String tagetAuthority, long lastUpdateInMs) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l) > lastUpdateInMs) {
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		if (lastUpdateInMs + getServiceUpdateMaxValidityInMs() < nowInMs) {
			deleteAllRequired = true; // too old to display
		}
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs());
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
		Collection<ServiceUpdate> newServiceUpdates = loadAgencyServiceUpdateDataFromWWW(tagetAuthority);
		if (CollectionUtils.getSize(newServiceUpdates) > 0) {
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyServiceUpdateData();
			}
			cacheServiceUpdates(newServiceUpdates);
			PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, nowInMs, true); // sync
		} // else keep whatever we have until max validity reached
	}

	private Collection<ServiceUpdate> loadAgencyServiceUpdateDataFromWWW(String tagetAuthority) {
		try {
			final String urlString = getAgencyUrlString();
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				return parseAgencyJson(jsonString, newLastUpdateInMs, tagetAuthority);
			default:
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)",
						httpUrlConnection == null ? null : httpUrlConnection.getResponseCode(),
						httpUrlConnection == null ? null : httpUrlConnection.getResponseMessage());
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

	private Collection<ServiceUpdate> parseAgencyJson(String jsonString, long nowInMs, String tagetAuthority) {
		try {
			HashSet<ServiceUpdate> result = new HashSet<ServiceUpdate>();
			JSONObject json = new JSONObject(jsonString);
			if (json.has("metro")) {
				JSONObject jMetro = json.getJSONObject("metro");
				JSONArray jMetroNames = jMetro.names();
				long maxValidityInMs = getServiceUpdateMaxValidityInMs();
				String language = LocaleUtils.isFR() ? Locale.FRENCH.getLanguage() : StringUtils.EMPTY;
				for (int ln = 0; ln < jMetroNames.length(); ln++) {
					String jMetroName = jMetroNames.getString(ln);
					JSONObject jMetroObject = jMetro.getJSONObject(jMetroName);
					ServiceUpdate serviceUpdate = parseAgencyJsonText(jMetroObject, tagetAuthority, jMetroName, nowInMs, maxValidityInMs, language);
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

	private ServiceUpdate parseAgencyJsonText(JSONObject jMetroObject, String tagetAuthority, String routeId, long nowInMs, long maxValidityInMs,
			String language) {
		try {
			JSONObject jMetroData = jMetroObject.getJSONObject("data");
			String jMetroDataText = jMetroData.getString("text");
			String targetUUID = getAgencyTargetUUID(tagetAuthority, Integer.parseInt(routeId));
			if (!TextUtils.isEmpty(jMetroDataText)) {
				int severity = findSeverity(jMetroObject, jMetroDataText);
				final String textHtml = enhanceHtml(jMetroDataText, null, severity);
				ServiceUpdate serviceUpdate = new ServiceUpdate(null, targetUUID, nowInMs, maxValidityInMs, jMetroDataText, textHtml, severity,
						AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, language);
				return serviceUpdate;
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON message '%s'!", jMetroObject);
		}
		return null;
	}

	// http://www.stm.info/en/ajax/etats-du-service
	private static final String AGENCY_URL_PART_1_BEFORE_LANG = "http://www.stm.info/";
	private static final String AGENCY_URL_PART_2_AFTER_LANG = "/ajax/etats-du-service";
	private static final String AGENCY_URL_LANG_DEFAULT = "en";
	private static final String AGENCY_URL_LANG_FRENCH = "fr";

	private static String getAgencyUrlString() {
		return new StringBuilder() //
				.append(AGENCY_URL_PART_1_BEFORE_LANG).append(LocaleUtils.isFR() ? AGENCY_URL_LANG_FRENCH : AGENCY_URL_LANG_DEFAULT) // language
				.append(AGENCY_URL_PART_2_AFTER_LANG) //
				.toString();
	}

	private static final Pattern CLEAN_STOPS = Pattern.compile("between[\\s]*([^\\s]*)[\\s]*and[\\s]*([^\\s\\.\\,\\:]*)");
	private static final Pattern CLEAN_STOPS_FR = Pattern.compile("entre[\\s]*([^\\s]*)[\\s]*et[\\s]*([^\\s\\.\\,\\:]*)");

	private static final String CLEAN_STOPS_REPLACEMENT = "between " + HtmlUtils.applyBold("$1") + " and " + HtmlUtils.applyBold("$2");
	private static final String CLEAN_STOPS_REPLACEMENT_FR = "entre " + HtmlUtils.applyBold("$1") + " et " + HtmlUtils.applyBold("$2");

	private String enhanceHtml(String orginalHtml, RouteTripStop optRts, Integer optSeverity) {
		if (TextUtils.isEmpty(orginalHtml)) {
			return orginalHtml;
		}
		try {
			String html = orginalHtml;
			if (LocaleUtils.isFR()) {
				html = CLEAN_STOPS_FR.matcher(html).replaceAll(CLEAN_STOPS_REPLACEMENT_FR);
			} else {
				html = CLEAN_STOPS.matcher(html).replaceAll(CLEAN_STOPS_REPLACEMENT);
			}
			if (optRts != null) {
				html = enhanceHtmlRts(optRts, html);
			}
			html = enhanceHtmlDateTime(html);
			if (optSeverity != null) {
				html = enhanceHtmlSeverity(optSeverity.intValue(), html);
			}
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance HTML (using original)!");
			return orginalHtml;
		}
	}

	private static final Pattern CLEAN_BOLD = Pattern.compile("(service disruption)", Pattern.CASE_INSENSITIVE);
	private static final Pattern CLEAN_BOLD_FR = Pattern.compile("(interruption de service)", Pattern.CASE_INSENSITIVE);
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

	private static final String ROUTE_LONG_NAME_REPLACEMENT = HtmlUtils.applyBold("$1");

	private String enhanceHtmlRts(RouteTripStop rts, String originalHtml) {
		if (TextUtils.isEmpty(originalHtml)) {
			return originalHtml;
		}
		String html = originalHtml;
		if (!TextUtils.isEmpty(rts.route.longName)) {
			String routeLongNameReplacement = ROUTE_LONG_NAME_REPLACEMENT;
			if (!TextUtils.isEmpty(rts.route.getColor())) {
				routeLongNameReplacement = HtmlUtils.applyFontColor(routeLongNameReplacement, rts.route.getColor());
			}
			html = Pattern.compile("(" + rts.route.longName + ")", Pattern.CASE_INSENSITIVE).matcher(html).replaceAll(routeLongNameReplacement);
		}
		return html;
	}

	private static final Pattern CLEAN_TIME = Pattern.compile("([\\d]{1,2})[\\s]*[:|h][\\s]*([\\d]{2})([\\s]*([a|p]m))?", Pattern.CASE_INSENSITIVE);

	private static final Pattern CLEAN_DATE = Pattern.compile("([\\d]{1,2}[\\s]*[a-zA-Z]+[\\s]*[\\d]{4})");

	private static final ThreadSafeDateFormatter PARSE_TIME = new ThreadSafeDateFormatter("HH:mm");

	private static final ThreadSafeDateFormatter PARSE_TIME_AMPM = new ThreadSafeDateFormatter("hh:mm a");

	private static final ThreadSafeDateFormatter FORMAT_TIME = ThreadSafeDateFormatter.getTimeInstance(ThreadSafeDateFormatter.SHORT);

	private static final ThreadSafeDateFormatter FORMAT_DATE = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM);

	private static final String PARSE_DATE_REGEX = "dd MMMM yyyy";

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
				timeD = PARSE_TIME.parseThreadSafe(hours + ":" + minutes);
			} else {
				timeD = PARSE_TIME_AMPM.parseThreadSafe(hours + ":" + minutes + " " + ampm);
			}
			final String fTime = FORMAT_TIME.formatThreadSafe(timeD);
			html = html.replace(time, HtmlUtils.applyBold(fTime));
		}
		Matcher dateMatcher = CLEAN_DATE.matcher(html);
		final ThreadSafeDateFormatter parseDate = new ThreadSafeDateFormatter(PARSE_DATE_REGEX, LocaleUtils.isFR() ? Locale.FRENCH : Locale.ENGLISH);
		while (dateMatcher.find()) {
			String date = dateMatcher.group(0);
			Date dateD = parseDate.parseThreadSafe(date);
			String fDate = FORMAT_DATE.formatThreadSafe(dateD);
			html = html.replace(date, HtmlUtils.applyBold(fDate));
		}
		return html;
	}

	private static final String STATUS_NONE = "Normal m";
	private static final String STATUS_NONE_FR = "Service normal";

	private static final String STATUS_INFO = "Service gradually";
	private static final String STATUS_INFO_FR = "Reprise";

	private static final String STATUS_WARNING = "Service disrupt";
	private static final String STATUS_WARNING_FR = "Interruption de service";

	public int findSeverity(JSONObject optJMetroObject, String jMetroDataText) {
		if (!TextUtils.isEmpty(jMetroDataText)) {
			if (LocaleUtils.isFR()) {
				if (jMetroDataText.startsWith(STATUS_NONE_FR)) {
					return ServiceUpdate.SEVERITY_NONE;
				} else if (jMetroDataText.startsWith(STATUS_INFO_FR)) {
					return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
				} else if (jMetroDataText.startsWith(STATUS_WARNING_FR)) {
					return ServiceUpdate.SEVERITY_WARNING_RELATED_POI;
				}
			} else {
				if (jMetroDataText.startsWith(STATUS_NONE)) {
					return ServiceUpdate.SEVERITY_NONE;
				} else if (jMetroDataText.startsWith(STATUS_INFO)) {
					return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
				} else if (jMetroDataText.startsWith(STATUS_WARNING)) {
					return ServiceUpdate.SEVERITY_WARNING_RELATED_POI;
				}
			}
		}
		MTLog.w(this, "Cannot find severity for '%s'. #ServiceUpdate", jMetroDataText);
		return ServiceUpdate.SEVERITY_INFO_UNKNOWN;
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

	private static StmInfoSubwayDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private StmInfoSubwayDbHelper getDBHelper(Context context) {
		if (dbHelper == null) {
			dbHelper = getNewDbHelper(context);
			currentDbVersion = getCurrentDbVersion();
		} else {
			try {
				if (currentDbVersion != getCurrentDbVersion()) {
					dbHelper.close();
					dbHelper = null;
					return getDBHelper(context);
				}
			} catch (Throwable t) {
				MTLog.d(this, t, "Can't check DB version!");
			}
		}
		return dbHelper;
	}

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return StmInfoSubwayDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link StmInfoSubwayProvider} implementations in same app.
	 */
	public StmInfoSubwayDbHelper getNewDbHelper(Context context) {
		return new StmInfoSubwayDbHelper(context.getApplicationContext());
	}

	@Override
	public UriMatcher getURIMATCHER() {
		return getURIMATCHER(getContext());
	}

	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITYURI(getContext());
	}

	@Override
	public Context getContentProviderContext() {
		return getContext();
	}

	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	@Override
	public Cursor queryMT(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor cursor = ServiceUpdateProvider.queryS(this, uri, projection, selection, selectionArgs, sortOrder);
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

	public static class StmInfoSubwayDbHelper extends ServiceUpdateProvider.ServiceUpdateDbHelper {

		private static final String TAG = StmInfoSubwayDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link StmInfoSubwayDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "stm_info.db";

		/**
		 * Override if multiple {@link StmInfoSubwayDbHelper} implementations in same app.
		 */
		protected static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pStmInfoSubwayEtatsDuServiceLastUpdate";

		public static final String T_STM_INFO_SERVICE_UPDATE = ServiceUpdateProvider.ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_STM_INFO_SERVICE_UPDATE_SQL_CREATE = ServiceUpdateProvider.ServiceUpdateDbHelper.getSqlCreate(T_STM_INFO_SERVICE_UPDATE);

		private static final String T_STM_INFO_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STM_INFO_SERVICE_UPDATE);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link StmInfoSubwayDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.stm_info_db_version);
			}
			return dbVersion;
		}

		private Context context;

		public StmInfoSubwayDbHelper(Context context) {
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
			db.execSQL(T_STM_INFO_SERVICE_UPDATE_SQL_DROP);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l, true);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_STM_INFO_SERVICE_UPDATE_SQL_CREATE);
		}
	}

}
