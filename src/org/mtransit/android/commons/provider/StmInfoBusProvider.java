package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.provider.ServiceUpdateProvider.ServiceUpdateDbHelper;
import org.mtransit.android.commons.provider.ServiceUpdateProvider.ServiceUpdateFilter;

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
			authority = context.getResources().getString(R.string.stm_info_bus_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link StmInfoBusProvider} implementations in same app.
	 */
	public static Uri getAUTHORITYURI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static final long BUS_SERVICE_UPDATE_MAX_VALIDITY_IN_MS = 1 * TimeUtils.ONE_DAY_IN_MS;

	private static final long BUS_SERVICE_UPDATE_VALIDITY_IN_MS = 1 * TimeUtils.ONE_HOUR_IN_MS;

	private static final long BUS_SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = 10 * TimeUtils.ONE_MINUTE_IN_MS;

	@Override
	public long getMinDurationBetweenServiceUpdateRefreshInMs() {
		return BUS_SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public long getServiceUpdateMaxValidityInMs() {
		return BUS_SERVICE_UPDATE_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getServiceUpdateValidityInMs() {
		return BUS_SERVICE_UPDATE_VALIDITY_IN_MS;
	}

	@Override
	public void cacheServiceUpdates(Collection<ServiceUpdate> newServiceUpdates) {
		ServiceUpdateProvider.cacheServiceUpdatesS(getContext(), this, newServiceUpdates);
	}

	@Override
	public Collection<ServiceUpdate> getCachedServiceUpdates(ServiceUpdateFilter serviceUpdateFilter) {
		return ServiceUpdateProvider.getCachedServiceUpdatesS(this, serviceUpdateFilter);
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

	@Override
	public String getServiceUpdateDbTableName() {
		return StmInfoBusDbHelper.T_STM_INFO_BUS_SERVICE_UPDATE;
	}

	@Override
	public Collection<ServiceUpdate> getNewServiceUpdates(ServiceUpdateFilter serviceUpdateFilter) {
		if (serviceUpdateFilter == null) {
			return null;
		}
		if (serviceUpdateFilter.getPoi() == null || !(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		Collection<ServiceUpdate> serviceUpdates = updateDataFromWWW(rts, serviceUpdateFilter);
		return serviceUpdates;
	}

	private HashMap<String, Long> recentlyLoadedTargetUUID = new HashMap<String, Long>();

	private synchronized Collection<ServiceUpdate> updateDataFromWWW(RouteTripStop rts, ServiceUpdateFilter serviceUpdateFilter) {
		final long nowInMs = TimeUtils.currentTimeMillis();
		Long lastTimeLoaded = this.recentlyLoadedTargetUUID.get(rts.getUUID());
		if (lastTimeLoaded != null && lastTimeLoaded.longValue() + getMinDurationBetweenServiceUpdateRefreshInMs() > nowInMs) {
			return getCachedServiceUpdates(serviceUpdateFilter);
		}
		final Collection<ServiceUpdate> result = loadDataFromWWW(rts, nowInMs);
		this.recentlyLoadedTargetUUID.put(rts.getUUID(), nowInMs);
		deleteCachedServiceUpdate(rts.getUUID(), SOURCE_ID);
		cacheServiceUpdates(result);
		return result;
	}

	public static final String SOURCE_ID = "www_stm_info_lines_stops_arrivals";
	public static final String SOURCE_LABEL = "www.stm.info";

	private static final String URL_PART_1_BEFORE_LANG = "http://i-www.stm.info/";
	private static final String URL_PART_2_BEFORE_ROUTE_ID = "/lines/";
	private static final String URL_PART_3_BEFORE_STOP_ID = "/stops/";
	private static final String URL_PART_4_BEFORE_TRIP_HEADSIGN_VALUE = "/arrivals?direction=";
	private static final String URL_PART_5_BEFORE_LIMIT = "&limit=";
	private static final String URL_PART_6_BEFORE_DATE = "&d=";
	private static final String URL_PART_7_BEFORE_TIME = "&t=";

	private static final String URL_LANG_DEFAULT = "en";
	private static final String URL_LANG_FRENCH = "fr";

	private static final String URL_DATE_FORMAT_PATTERN = "yyyyMMdd";
	private static final String URL_TIME_FORMAT_PATTERN = "HHmm";

	private static final ThreadSafeDateFormatter URL_DATE_FORMAT = new ThreadSafeDateFormatter(URL_DATE_FORMAT_PATTERN);
	private static final ThreadSafeDateFormatter URL_TIME_FORMAT = new ThreadSafeDateFormatter(URL_TIME_FORMAT_PATTERN);

	private static final Pattern STOP = Pattern.compile("(bus stop)", Pattern.CASE_INSENSITIVE);
	private static final Pattern STOP_FR = Pattern.compile("(arr[ê|e]t)", Pattern.CASE_INSENSITIVE);

	private static final Pattern YELLOW_LINE = Pattern.compile("(Yellow line)", Pattern.CASE_INSENSITIVE);
	private static final Pattern YELLOW_LINE_FR = Pattern.compile("(ligne jaune)", Pattern.CASE_INSENSITIVE);

	private Collection<ServiceUpdate> loadDataFromWWW(RouteTripStop rts, long nowInMs) {
		try {
			HashSet<ServiceUpdate> result = new HashSet<ServiceUpdate>();
			final Date nowDate = new Date(nowInMs);
			final String urlDateS = URL_DATE_FORMAT.formatThreadSafe(nowDate);
			final String urlTimeS = URL_TIME_FORMAT.formatThreadSafe(nowDate);
			final String urlString = getUrlStringWithDateAndTime(rts, urlDateS, urlTimeS);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpsUrlConnection = (HttpURLConnection) urlc;
			switch (httpsUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				String jsonString = FileUtils.getString(urlc.getInputStream());
				Collection<ServiceUpdate> parseResult = parseJson(jsonString, rts, nowInMs);
				if (parseResult != null) {
					result.addAll(parseResult);
				}
				if (CollectionUtils.getSize(result) == 0) {
					final String language = LocaleUtils.isFR() ? Locale.FRENCH.getLanguage() : StringUtils.EMPTY;
					ServiceUpdate serviceUpdateNone = new ServiceUpdate(null, rts.getUUID(), nowInMs, getServiceUpdateMaxValidityInMs(), null, null,
							ServiceUpdate.SEVERITY_NONE, SOURCE_ID, SOURCE_LABEL, language);
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

	private Collection<ServiceUpdate> parseJson(String jsonString, RouteTripStop rts, long nowInMs) {
		try {
			HashSet<ServiceUpdate> result = new HashSet<ServiceUpdate>();
			JSONObject jResponse = new JSONObject(jsonString);
			if (jResponse.has("messages")) {
				JSONArray jMessages = jResponse.getJSONArray("messages");
				Pattern stop = LocaleUtils.isFR() ? STOP_FR : STOP;
				Pattern yellowLine = LocaleUtils.isFR() ? YELLOW_LINE_FR : YELLOW_LINE;
				long maxValidityInMs = getServiceUpdateMaxValidityInMs();
				String language = LocaleUtils.isFR() ? Locale.FRENCH.getLanguage() : StringUtils.EMPTY;
				String targetUUID = rts.getUUID();
				for (int i = 0; i < jMessages.length(); i++) {
					JSONObject jMessage = jMessages.getJSONObject(i);
					ServiceUpdate serviceUpdate = parseJsonMessage(jMessage, rts, nowInMs, stop, yellowLine, maxValidityInMs, language, targetUUID);
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

	private ServiceUpdate parseJsonMessage(JSONObject jMessage, RouteTripStop rts, long nowInMs, Pattern stop, Pattern yellowLine, long maxValidityInMs,
			String language, String targetUUID) {
		try {
			final String jMessageText = jMessage.getString("text");
			if (!TextUtils.isEmpty(jMessageText)) {
				final int severity = findSeverity(jMessage, jMessageText, rts, stop, yellowLine);
				final String text = Html.fromHtml(jMessageText).toString();
				final String textHtml = enhanceHtml(jMessageText, rts, severity);
				ServiceUpdate serviceUpdate = new ServiceUpdate(null, targetUUID, nowInMs, maxValidityInMs, text, textHtml, severity, SOURCE_ID, SOURCE_LABEL,
						language);
				return serviceUpdate;
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON message '%s'!", jMessage);
		}
		return null;
	}

	private static final String POINT = "\\.";
	private static final String PARENTHESE1 = "\\(";
	private static final String PARENTHESE2 = "\\)";
	private static final String SLASH = "/";
	private static final String ANY_STOP_CODE = "[\\d]+";

	private static final Pattern CLEAN_BR = Pattern.compile("(" + PARENTHESE2 + ",|" + POINT + "|:)[\\s]+");
	private static final String CLEAN_BR_REPLACEMENT = "$1" + HtmlUtils.BR;

	private static final Pattern CLEAN_STOP_CODE_AND_NAME = Pattern.compile("(" + ANY_STOP_CODE + ")[\\s]*" + PARENTHESE1 + "([^" + SLASH + "]*)" + SLASH
			+ "([^" + PARENTHESE2 + "]*)" + PARENTHESE2 + "([" + PARENTHESE2 + "]*)" + "([,]*)([.]*)");
	private static final String CLEAN_STOP_CODE_AND_NAME_REPLACEMENT = "- $2" + SLASH + "$3$4 " + PARENTHESE1 + "$1" + PARENTHESE2 + "$5$6";

	private static final String CLEAN_THAT_STOP_CODE = "(\\-[\\s]+)" + "([^" + SLASH + "]*" + SLASH + "[^" + PARENTHESE1 + "]*" + PARENTHESE1 + "%s"
			+ PARENTHESE2 + ")";
	private static final String CLEAN_THAT_STOP_CODE_REPLACEMENT = "$1" + HtmlUtils.B1 + "$2" + HtmlUtils.B2;

	private static final Pattern CLEAN_BOLD = Pattern.compile("(bus stop|relocated)");
	private static final Pattern CLEAN_BOLD_FR = Pattern.compile("(arrêt|déplacé)");
	private static final String CLEAN_BOLD_REPLACEMENT = HtmlUtils.B1 + "$1" + HtmlUtils.B2;

	private static final Pattern CLEAN_TIME = Pattern.compile("([\\d]{1,2})[\\s]*[:|h][\\s]*([\\d]{2})([\\s]*([a|p]m))?", Pattern.CASE_INSENSITIVE);

	private static final Pattern CLEAN_DATE = Pattern.compile("([\\d]{1,2}[\\s]*[a-zA-Z]+[\\s]*[\\d]{4})");

	private static final ThreadSafeDateFormatter PARSE_TIME = new ThreadSafeDateFormatter("HH:mm");

	private static final ThreadSafeDateFormatter PARSE_TIME_AMPM = new ThreadSafeDateFormatter("hh:mm a");

	private static final ThreadSafeDateFormatter FORMAT_TIME = ThreadSafeDateFormatter.getTimeInstance(ThreadSafeDateFormatter.SHORT);

	private static final ThreadSafeDateFormatter FORMAT_DATE = ThreadSafeDateFormatter.getDateInstance(ThreadSafeDateFormatter.MEDIUM);

	private static final String PARSE_DATE_REGEX = "dd MMMM yyyy";

	private String enhanceHtml(String orginalHtml, RouteTripStop rts, int severity) {
		try {
			String html = orginalHtml;
			html = CLEAN_BR.matcher(html).replaceAll(CLEAN_BR_REPLACEMENT);
			html = CLEAN_STOP_CODE_AND_NAME.matcher(html).replaceAll(CLEAN_STOP_CODE_AND_NAME_REPLACEMENT);
			if (rts != null) {
				html = Pattern.compile(String.format(CLEAN_THAT_STOP_CODE, rts.stop.code)).matcher(html).replaceAll(CLEAN_THAT_STOP_CODE_REPLACEMENT);
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
				html = html.replace(time, fTime);
			}
			Matcher dateMatcher = CLEAN_DATE.matcher(html);
			final ThreadSafeDateFormatter parseDate = new ThreadSafeDateFormatter(PARSE_DATE_REGEX, LocaleUtils.isFR() ? Locale.FRENCH : Locale.ENGLISH);
			while (dateMatcher.find()) {
				String date = dateMatcher.group(0);
				Date dateD = parseDate.parseThreadSafe(date);
				String fDate = FORMAT_DATE.formatThreadSafe(dateD);
				html = html.replace(date, fDate);
			}
			if (ServiceUpdate.isSeverityWarning(severity))
				if (LocaleUtils.isFR()) {
					html = CLEAN_BOLD_FR.matcher(html).replaceAll(CLEAN_BOLD_REPLACEMENT);
				} else {
					html = CLEAN_BOLD.matcher(html).replaceAll(CLEAN_BOLD_REPLACEMENT);
				}
			return html;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance HTML (using original)!");
			return orginalHtml;
		}
	}

	private static final String STM_INFO_LEVEL_CORPORATIVE = "Corporative";
	private static final String STM_INFO_LEVEL_STOP_ROUTE = "StopRoute";

	private int findSeverity(JSONObject jMessage, String jMessageText, RouteTripStop rts, Pattern stop, Pattern yellowLine) {
		try {
			if (jMessage.has("level")) {
				final String level = jMessage.getString("level");
				if (STM_INFO_LEVEL_CORPORATIVE.equalsIgnoreCase(level)) {
					return ServiceUpdate.SEVERITY_INFO_AGENCY;
				}
				if (STM_INFO_LEVEL_STOP_ROUTE.equalsIgnoreCase(level)) {
					if (jMessageText.contains(rts.stop.code)) {
						return ServiceUpdate.SEVERITY_WARNING_POI;
					} else {
						return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to use custom JSON fields to find severity in '%s'!", jMessage);
		}
		if (jMessageText.contains(rts.stop.code)) {
			return ServiceUpdate.SEVERITY_WARNING_POI;
		} else if (stop.matcher(jMessageText).find()) {
			return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
		} else if (yellowLine.matcher(jMessageText).find()) {
			return ServiceUpdate.SEVERITY_INFO_AGENCY;
		}
		return ServiceUpdate.SEVERITY_INFO_UNKNOWN;
	}

	private static String getUrlStringWithDateAndTime(RouteTripStop rts, String urlDateS, String urlTimeS) {
		return new StringBuilder() //
				.append(URL_PART_1_BEFORE_LANG).append(LocaleUtils.isFR() ? URL_LANG_FRENCH : URL_LANG_DEFAULT) // language
				.append(URL_PART_2_BEFORE_ROUTE_ID).append(rts.route.id) // route ID
				.append(URL_PART_3_BEFORE_STOP_ID).append(rts.stop.id) // stop ID
				.append(URL_PART_4_BEFORE_TRIP_HEADSIGN_VALUE).append(rts.trip.headsignValue) // trip HEADSIGN (E/W/N/S)
				.append(URL_PART_5_BEFORE_LIMIT).append(100) // without limit, return all schedule for the day
				.append(URL_PART_6_BEFORE_DATE).append(urlDateS) // date 20100602
				.append(URL_PART_7_BEFORE_TIME).append(urlTimeS) // time 2359
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
	public UriMatcher getURIMATCHER() {
		return getURIMATCHER(getContext());
	}

	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITYURI(getContext());
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

	public static class StmInfoBusDbHelper extends ServiceUpdateDbHelper {

		private static final String TAG = StmInfoBusDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link StmInfoBusDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "stm_info_bus.db";

		public static final String T_STM_INFO_BUS_SERVICE_UPDATE = ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_STM_INFO_BUS_SERVICE_UPDATE_SQL_CREATE = ServiceUpdateDbHelper.getSqlCreate(T_STM_INFO_BUS_SERVICE_UPDATE);

		private static final String T_STM_INFO_BUS_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_STM_INFO_BUS_SERVICE_UPDATE);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link StmInfoBusDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.stm_info_bus_db_version);
			}
			return dbVersion;
		}

		public StmInfoBusDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
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
