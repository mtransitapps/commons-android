package org.mtransit.android.commons.provider;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.KeysIds;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SecureStringUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.Direction;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.RouteDirectionStop;
import org.mtransit.android.commons.data.Schedule;
import org.mtransit.android.commons.data.ScheduleStatusFilter;
import org.mtransit.android.commons.provider.common.MTContentProvider;
import org.mtransit.android.commons.provider.common.MTSQLiteOpenHelper;
import org.mtransit.android.commons.provider.news.NewsProvider;
import org.mtransit.android.commons.provider.news.NewsProviderContract;
import org.mtransit.android.commons.provider.status.StatusProvider;
import org.mtransit.android.commons.provider.status.StatusProviderContract;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.SourceUtils;
import org.mtransit.commons.provider.WinnipegTransitProviderCommons;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLHandshakeException;

// DO NOT MOVE: referenced in modules AndroidManifest.xml
@SuppressLint("Registered")
public class WinnipegTransitProvider extends MTContentProvider implements StatusProviderContract, NewsProviderContract {

	protected static final String LOG_TAG = WinnipegTransitProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	public static UriMatcher getNewUriMatcher(@NonNull String authority) {
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
	private String providedApiKey = null;

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

	private static final long WEB_SERVICE_STATUS_VALIDITY_IN_MS = TimeUnit.MINUTES.toMillis(10L); // API key
	private static final long WEB_SERVICE_STATUS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L); // API key
	private static final long WEB_SERVICE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(1L); // API key
	private static final long WEB_SERVICE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L); // API key

	@Override
	public long getStatusValidityInMs(boolean inFocus) {
		if (inFocus) {
			return WEB_SERVICE_STATUS_VALIDITY_IN_FOCUS_IN_MS;
		}
		return WEB_SERVICE_STATUS_VALIDITY_IN_MS;
	}

	@Override
	public long getMinDurationBetweenStatusRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return WEB_SERVICE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return WEB_SERVICE_STATUS_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public void cacheStatus(@NonNull POIStatus newStatusToCache) {
		StatusProvider.cacheStatusS(this, newStatusToCache);
	}

	@Nullable
	@Override
	public POIStatus getCachedStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule without schedule filter!");
			return null;
		}
		ScheduleStatusFilter scheduleStatusFilter = (ScheduleStatusFilter) statusFilter;
		RouteDirectionStop rds = scheduleStatusFilter.getRouteDirectionStop();
		POIStatus cachedStatus = StatusProvider.getCachedStatusS(this, rds.getUUID());
		if (cachedStatus != null) {
			if (rds.isNoPickup()) {
				if (cachedStatus instanceof Schedule) {
					Schedule schedule = (Schedule) cachedStatus;
					schedule.setNoPickup(true); // API doesn't know about "descent only"
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
		return WinnipegTransitDbHelper.T_WEB_SERVICE_STATUS;
	}

	@Override
	public int getStatusType() {
		return POI.ITEM_STATUS_TYPE_SCHEDULE;
	}

	@Nullable
	@Override
	public POIStatus getNewStatus(@NonNull StatusProviderContract.Filter statusFilter) {
		if (!(statusFilter instanceof ScheduleStatusFilter)) {
			MTLog.w(this, "getNewStatus() > Can't find new schedule without schedule filter!");
			return null;
		}
		ScheduleStatusFilter scheduleStatusFilter = (ScheduleStatusFilter) statusFilter;
		RouteDirectionStop rds = scheduleStatusFilter.getRouteDirectionStop();
		this.providedApiKey = SecureStringUtils.dec(statusFilter.getProvidedEncryptKey(KeysIds.CA_WINNIPEG_TRANSIT_API_KEY));
		loadRealTimeStatusFromWWW(requireContextCompat(), rds);
		return getCachedStatus(statusFilter);
	}

	protected static final String WINNIPEG_TZ_ID = "America/Cancun"; // "America/Winnipeg"; // UTC-5 forever starting Nov 2026, compat w/ old Android devices

	private static final TimeZone WINNIPEG_TZ = TimeZone.getTimeZone(WINNIPEG_TZ_ID);

	protected static final ThreadSafeDateFormatter DATE_FORMATTER;

	static {
		ThreadSafeDateFormatter dateFormatter = new ThreadSafeDateFormatter("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);
		dateFormatter.setTimeZone(WINNIPEG_TZ);
		DATE_FORMATTER = dateFormatter;
	}

	// https://api.winnipegtransit.com/home/api/v4
	// https://api.winnipegtransit.com/v4/stops/STOP_CODE/schedule.json?api-key=API_KEY
	private static final String REAL_TIME_URL_PART_1_BEFORE_STOP_ID = "https://api.winnipegtransit.com/v4/stops/";
	private static final String REAL_TIME_URL_PART_2_BEFORE_ROUTE_ID = "/schedule.json?route=";
	private static final String REAL_TIME_URL_PART_3_BEFORE_START = "&start=";
	private static final String REAL_TIME_URL_PART_4_BEFORE_END = "&end=";
	private static final String REAL_TIME_URL_PART_5_BEFORE_API_KEY = "&api-key=";

	@NonNull
	private static String getRealTimeStatusUrlString(@NonNull String apiKey, @NonNull RouteDirectionStop rds) {
		Calendar c = Calendar.getInstance(WINNIPEG_TZ);
		c.add(Calendar.HOUR, -1);
		String start = DATE_FORMATTER.formatThreadSafe(c);
		c.add(Calendar.HOUR, 1 + 12);
		String end = DATE_FORMATTER.formatThreadSafe(c);
		return REAL_TIME_URL_PART_1_BEFORE_STOP_ID + //
				rds.getStop().getCode() + //
				REAL_TIME_URL_PART_2_BEFORE_ROUTE_ID + //
				rds.getRoute().getShortName() + //
				REAL_TIME_URL_PART_3_BEFORE_START + //
				start + //
				REAL_TIME_URL_PART_4_BEFORE_END + //
				end + //
				REAL_TIME_URL_PART_5_BEFORE_API_KEY + //
				apiKey;
	}

	private void loadRealTimeStatusFromWWW(@NonNull Context context, @NonNull RouteDirectionStop rds) {
		try {
			final String urlString = getRealTimeStatusUrlString(
					(this.providedApiKey != null ? this.providedApiKey : getAPI_KEY(context)),
					rds
			);
			final URL url = new URL(urlString);
			MTLog.i(this, "Loading from '%s'...", getRealTimeStatusUrlString("API_KEY", rds));
			final String sourceLabel = SourceUtils.getSourceLabel(REAL_TIME_URL_PART_1_BEFORE_STOP_ID);
			final URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			final HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				final long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				final String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadRealTimeStatusFromWWW() > jsonString: %s.", jsonString);
				final Collection<POIStatus> statuses = WinnipegTransitProviderExtKt.toStatusesOrNull(
						jsonString,
						rds.getDirection().getId(),
						() -> new Schedule(
								null,
								rds.getUUID(),
								newLastUpdateInMs,
								getStatusMaxValidityInMs(),
								newLastUpdateInMs,
								PROVIDER_PRECISION_IN_MS,
								false,
								WINNIPEG_TZ_ID,
								sourceLabel,
								false
						),
						(tripHeadsign) -> cleanTripHeadsign(context, tripHeadsign, rds)
				);
				StatusProvider.deleteCachedStatus(this, Collections.singleton(rds.getUUID()));
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

	private String cleanTripHeadsign(@NonNull Context context, @NonNull String tripHeadsign, @NonNull RouteDirectionStop rds) {
		try {
			tripHeadsign = WinnipegTransitProviderCommons.cleanTripHeadsign(tripHeadsign);
			final String tripHeading = rds.getDirection().getHeading(context);
			final String routeLongName = rds.getRoute().getLongName();
			tripHeadsign = CleanUtils.keepOrRemoveVia(tripHeadsign, string ->
					Direction.isSameHeadsign(string, tripHeading)
							|| Direction.isSameHeadsign(string, routeLongName)
			);
			return tripHeadsign;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning trip head sign '%s'!", tripHeadsign);
			return tripHeadsign;
		}
	}

	/**
	 * Override if multiple {@link WinnipegTransitProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS = WinnipegTransitDbHelper.PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS;

	@Override
	public boolean purgeUselessCachedNews() {
		return NewsProvider.purgeUselessCachedNews(this);
	}

	@Override
	public boolean deleteCachedNews(@Nullable Integer serviceUpdateId) {
		return NewsProvider.deleteCachedNews(this, serviceUpdateId);
	}

	protected static final String AGENCY_SOURCE_ID = "api_winnipegtransit_com_service_advisories";

	@SuppressWarnings("UnusedReturnValue")
	private int deleteAllAgencyNewsData() {
		int affectedRows = 0;
		try {
			String selection = SqlUtils.getWhereEqualsString(NewsProviderContract.Columns.T_NEWS_K_SOURCE_ID, AGENCY_SOURCE_ID);
			affectedRows = getWriteDB().delete(getNewsDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all agency news data!");
		}
		return affectedRows;
	}

	@Override
	public void cacheNews(@NonNull ArrayList<News> newNews) {
		NewsProvider.cacheNewsS(this, newNews);
	}

	@Nullable
	@Override
	public ArrayList<News> getCachedNews(@NonNull NewsProviderContract.Filter newsFilter) {
		return NewsProvider.getCachedNewsS(this, newsFilter);
	}

	@Nullable
	@Override
	public Cursor getNewsFromDB(@NonNull NewsProviderContract.Filter newsFilter) {
		return NewsProvider.getDefaultNewsFromDB(newsFilter, this);
	}

	@Nullable
	private static Collection<String> languages = null;

	@NonNull
	@Override
	public Collection<String> getNewsLanguages() {
		if (languages == null) {
			languages = new HashSet<>();
			languages.add(Locale.ENGLISH.getLanguage());
			languages.add(LocaleUtils.UNKNOWN);
		}
		return languages;
	}

	@Nullable
	@Override
	public ArrayList<News> getNewNews(@NonNull NewsProviderContract.Filter newsFilter) {
		this.providedApiKey = SecureStringUtils.dec(newsFilter.getProvidedEncryptKey(KeysIds.CA_WINNIPEG_TRANSIT_API_KEY));
		updateAgencyNewsDataIfRequired(requireContextCompat(), newsFilter.isInFocusOrDefault());
		return getCachedNews(newsFilter);
	}

	private void updateAgencyNewsDataIfRequired(@NonNull Context context, boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(context).getLong(PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS, 0L);
		long minUpdateMs = Math.min(getNewsMaxValidityInMs(), getNewsValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyNewsDataIfRequiredSync(context, lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyNewsDataIfRequiredSync(@NonNull Context context, long lastLastUpdateInMs, boolean inFocus) {
		final long lastUpdateInMs = PreferenceUtils.getPrefLcl(context).getLong(PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS, 0L);
		if (lastUpdateInMs > lastLastUpdateInMs) { // IF new more recent last update DO
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		//noinspection RedundantIfStatement
		if (lastUpdateInMs + getNewsMaxValidityInMs() < nowInMs) {
			deleteAllRequired = true; // too old to display
		}
		long minUpdateMs = Math.min(getNewsMaxValidityInMs(), getNewsValidityInMs(inFocus));
		if (deleteAllRequired
				|| lastUpdateInMs + minUpdateMs < nowInMs) {
			updateAllAgencyNewsDataFromWWW(context, deleteAllRequired); // try to update
		}
	}

	private void updateAllAgencyNewsDataFromWWW(@NonNull Context context, boolean deleteAllRequired) {
		boolean deleteAllDone = false;
		if (deleteAllRequired) {
			deleteAllAgencyNewsData();
			deleteAllDone = true;
		}
		ArrayList<News> newNews = loadAgencyNewsDataFromWWW(context);
		MTLog.d(this, "News(s) found: %s", newNews == null ? null : newNews.size());
		if (newNews != null) { // empty is OK
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyNewsData();
			}
			cacheNews(newNews);
			PreferenceUtils.getPrefLcl(context).edit().putLong(PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS, nowInMs).apply();
		} // else keep whatever we have until max validity reached
	}

	private static final String NEWS_URL_PART_1_BEFORE_API_KEY = "https://api.winnipegtransit.com/v4/service-advisories.json?api-key=";

	@NonNull
	private String getNewsUrlString(@NonNull Context context) {
		return NEWS_URL_PART_1_BEFORE_API_KEY +
				(this.providedApiKey != null ? this.providedApiKey : getAPI_KEY(context));
	}

	@Nullable
	private ArrayList<News> loadAgencyNewsDataFromWWW(@NonNull Context context) {
		try {
			MTLog.i(this, "Loading from '%s'...", NEWS_URL_PART_1_BEFORE_API_KEY);
			final String sourceLabel = SourceUtils.getSourceLabel(NEWS_URL_PART_1_BEFORE_API_KEY);
			final String urlString = getNewsUrlString(context);
			final URL url = new URL(urlString);
			final URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			final HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				final long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				final String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadAgencyNewsDataFromWWW() > jsonString: %s.", jsonString);
				final long noteworthyInMs = Long.parseLong(context.getResources().getString(R.string.news_provider_noteworthy_long_term));
				final int defaultPriority = context.getResources().getInteger(R.integer.news_provider_severity_info_agency);
				final String language = Locale.ENGLISH.getLanguage();
				final URL formURL = httpUrlConnection.getURL();
				return WinnipegTransitProviderExtKt.toNews(
						jsonString,
						(uuid, priority, updatedAtMs, text, textHTML, link) -> new News(
								null,
								getAuthority(),
								uuid,
								priority != null ? priority : defaultPriority,
								noteworthyInMs,
								newLastUpdateInMs,
								getNewsMaxValidityInMs(),
								updatedAtMs,
								getNEWS_TARGET_AUTHORITY(context),
								getNEWS_COLOR(context),
								getNEWS_AUTHOR_NAME(context),
								null,
								AUTHOR_ICON,
								DEFAULT_LINK,
								text,
								textHTML,
								link,
								language,
								AGENCY_SOURCE_ID,
								sourceLabel,
								HtmlUtils.extractImagesUrls(formURL, textHTML)
						)
				);
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

	protected static final String DEFAULT_LINK = "https://info.winnipegtransit.com/en/schedules-maps-tools/service-advisories";

	private static final String AUTHOR_ICON = "https://winnipegtransit.com/favicon.ico";

	@NonNull
	@Override
	public String getNewsDbTableName() {
		return WinnipegTransitDbHelper.T_WEB_SERVICE_NEWS;
	}

	@NonNull
	@Override
	public String[] getNewsProjection() {
		return NewsProviderContract.getPROJECTION_NEWS();
	}

	@Nullable
	private static ArrayMap<String, String> newsProjectionMap;

	@NonNull
	@Override
	public ArrayMap<String, String> getNewsProjectionMap() {
		if (newsProjectionMap == null) {
			newsProjectionMap = NewsProvider.getNewNewsProjectionMap(getAUTHORITY(requireContextCompat()));
		}
		return newsProjectionMap;
	}

	@MainThread
	@Override
	public boolean onCreateMT() {
		return true;
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
		return WinnipegTransitDbHelper.getDbVersion(requireContextCompat());
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
		return getURIMATCHER(requireContextCompat());
	}

	@NonNull
	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITY_URI(requireContextCompat());
	}

	@NonNull
	@Override
	public String getAuthority() {
		return getAUTHORITY(requireContextCompat());
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
		cursor = NewsProvider.queryS(this, uri, selection);
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
				dbVersion++; // add news articles images URLs do DB -> FORCE DB update
			}
			return dbVersion;
		}

		@NonNull
		private final Context context;

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
			initAllDbTables(db);
		}

		public boolean isDbExist(@NonNull Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_WEB_SERVICE_STATUS_SQL_CREATE);
			db.execSQL(T_WEB_SERVICE_NEWS_SQL_CREATE);
			PreferenceUtils.getPrefLcl(this.context).edit().remove(PREF_KEY_AGENCY_NEWS_LAST_UPDATE_MS).apply();
		}
	}
}