package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLHandshakeException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.News;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;

@SuppressLint("Registered")
public class YouTubeNewsProvider extends NewsProvider {

	private static final String TAG = YouTubeNewsProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = YouTubeNewsDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS;

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.youtube_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String targetAuthority = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static String getTARGET_AUTHORITY(Context context) {
		if (targetAuthority == null) {
			targetAuthority = context.getResources().getString(R.string.youtube_target_for_poi_authority);
		}
		return targetAuthority;
	}

	private static String color = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static String getCOLOR(Context context) {
		if (color == null) {
			color = context.getResources().getString(R.string.youtube_color);
		}
		return color;
	}

	private static String apiKey = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static String getAPI_KEY(Context context) {
		if (apiKey == null) {
			apiKey = context.getResources().getString(R.string.youtube_api_key);
		}
		return apiKey;
	}

	private static java.util.List<String> channelsUsername = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getCHANNELS_USERNAME(Context context) {
		if (channelsUsername == null) {
			channelsUsername = Arrays.asList(context.getResources().getStringArray(R.array.youtube_channels_username));
		}
		return channelsUsername;
	}

	private static java.util.List<String> channelsId = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getCHANNELS_ID(Context context) {
		if (channelsId == null) {
			channelsId = Arrays.asList(context.getResources().getStringArray(R.array.youtube_channels_id));
		}
		return channelsId;
	}

	private static java.util.List<String> channelsUploadsPlaylistId = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getCHANNELS_UPLOADS_PLAYLIST_ID(Context context) {
		if (channelsUploadsPlaylistId == null) {
			channelsUploadsPlaylistId = Arrays.asList(context.getResources().getStringArray(R.array.youtube_channels_uploads_playlist_id));
		}
		return channelsUploadsPlaylistId;
	}

	private static java.util.List<String> channelsAuthorName = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getCHANNELS_AUTHOR_NAME(Context context) {
		if (channelsAuthorName == null) {
			channelsAuthorName = Arrays.asList(context.getResources().getStringArray(R.array.youtube_channels_author_name));
		}
		return channelsAuthorName;
	}

	private static java.util.List<String> channelsAuthorUrl = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getCHANNELS_AUTHOR_URL(Context context) {
		if (channelsAuthorUrl == null) {
			channelsAuthorUrl = Arrays.asList(context.getResources().getStringArray(R.array.youtube_channels_author_url));
		}
		return channelsAuthorUrl;
	}

	private static java.util.List<String> channelsLang = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getCHANNELS_LANG(Context context) {
		if (channelsLang == null) {
			channelsLang = Arrays.asList(context.getResources().getStringArray(R.array.youtube_channels_lang));
		}
		return channelsLang;
	}

	private static java.util.List<String> channelsColors = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getCHANNELS_COLORS(Context context) {
		if (channelsColors == null) {
			channelsColors = Arrays.asList(context.getResources().getStringArray(R.array.youtube_channels_colors));
		}
		return channelsColors;
	}

	private static java.util.List<String> channelsTargets = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getCHANNELS_TARGETS(Context context) {
		if (channelsTargets == null) {
			channelsTargets = Arrays.asList(context.getResources().getStringArray(R.array.youtube_channels_target));
		}
		return channelsTargets;
	}

	private static java.util.List<Integer> channelsSeverity = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static java.util.List<Integer> getCHANNELS_SEVERITY(Context context) {
		if (channelsSeverity == null) {
			channelsSeverity = ArrayUtils.asIntegerList(context.getResources().getIntArray(R.array.youtube_channels_severity));
		}
		return channelsSeverity;
	}

	private static java.util.List<Long> channelsNoteworthy = null;

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public static java.util.List<Long> getCHANNELS_NOTEWORTHY(Context context) {
		if (channelsNoteworthy == null) {
			channelsNoteworthy = ArrayUtils.asLongList(context.getResources().getStringArray(R.array.youtube_channels_noteworthy));
		}
		return channelsNoteworthy;
	}

	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	private static YouTubeNewsDbHelper dbHelper;
	private static int currentDbVersion = -1;

	private YouTubeNewsDbHelper getDBHelper(Context context) {
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
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return YouTubeNewsDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link YouTubeNewsProvider} implementations in same app.
	 */
	public YouTubeNewsDbHelper getNewDbHelper(Context context) {
		return new YouTubeNewsDbHelper(context.getApplicationContext());
	}

	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

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

	private static final String AGENCY_SOURCE_ID = "youtube";

	private static final String AGENCY_SOURCE_LABEL = "YouTube";

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
	public String getAuthority() {
		return getAUTHORITY(getContext());
	}

	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITY_URI(getContext());
	}

	@Override
	public void cacheNews(ArrayList<News> newNews) {
		NewsProvider.cacheNewsS(this, newNews);
	}

	@Override
	public ArrayList<News> getCachedNews(NewsProviderContract.Filter newsFilter) {
		if (newsFilter == null) {
			MTLog.w(this, "getCachedNews() > skip (no news filter)");
			return null;
		}
		ArrayList<News> cachedNews = NewsProvider.getCachedNewsS(this, newsFilter);
		return cachedNews;
	}

	private static Collection<String> languages = null;

	@Override
	public Collection<String> getNewsLanguages() {
		if (languages == null) {
			languages = new HashSet<String>();
			if (LocaleUtils.isFR()) {
				languages.add(Locale.FRENCH.getLanguage());
			} else {
				languages.add(Locale.ENGLISH.getLanguage());
			}
			languages.add(LocaleUtils.UNKNOWN);
		}
		return languages;
	}

	@Override
	public ArrayList<News> getNewNews(NewsProviderContract.Filter newsFilter) {
		if (newsFilter == null) {
			return null;
		}
		updateAgencyNewsDataIfRequired(newsFilter.isInFocusOrDefault());
		ArrayList<News> cachedNews = getCachedNews(newsFilter);
		return cachedNews;
	}

	private void updateAgencyNewsDataIfRequired(boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l);
		long minUpdateMs = Math.min(getNewsMaxValidityInMs(), getNewsValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyNewsDataIfRequiredSync(lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyNewsDataIfRequiredSync(long lastUpdateInMs, boolean inFocus) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l) > lastUpdateInMs) {
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
		if (newNews != null) { // empty is OK
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyNewsData();
			}
			cacheNews(newNews);
			PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, nowInMs, true); // sync
		} // else keep whatever we have until max validity reached
	}

	private ArrayList<News> loadAgencyNewsDataFromWWW() {
		try {
			ArrayList<News> newNews = new ArrayList<News>();
			int i = 0;
			for (String channelUploadsPlaylistId : getCHANNELS_UPLOADS_PLAYLIST_ID(getContext())) {
				ArrayList<News> feedNews = loadAgencyNewsDataFromWWW(channelUploadsPlaylistId, i++);
				if (feedNews != null) {
					newNews.addAll(feedNews);
				}
			}
			return newNews;
		} catch (Exception e) {
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private static final String CHANNEL_UPLOADS_PLAYLIST_URL_PART_1_BEFORE_API_KEY = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&fields=items&maxResults=10&key=";
	private static final String CHANNEL_UPLOADS_PLAYLIST_URL_PART_2_BEFORE_PLAYLIST_ID = "&playlistId=";

	private String getChannelUploadsPlaylistUrl(String channelUploadsPlaylistId) {
		return new StringBuilder() //
				.append(CHANNEL_UPLOADS_PLAYLIST_URL_PART_1_BEFORE_API_KEY).append(getAPI_KEY(getContext())) //
				.append(CHANNEL_UPLOADS_PLAYLIST_URL_PART_2_BEFORE_PLAYLIST_ID).append(channelUploadsPlaylistId) //
				.toString();
	}

	private ArrayList<News> loadAgencyNewsDataFromWWW(String channelUploadsPlaylistId, int i) {
		try {
			String urlString = getChannelUploadsPlaylistUrl(channelUploadsPlaylistId);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				return parseAgencyNewsJSON(jsonString, newLastUpdateInMs, i);
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
			MTLog.w(TAG, se, "No Internet Connection!");
			return null;
		} catch (Exception e) {
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private static final String JSON_ITEMS = "items";
	private static final String JSON_ID = "id";
	private static final String JSON_SNIPPET = "snippet";
	private static final String JSON_PUBLISHED_AT = "publishedAt";
	private static final String JSON_TITLE = "title";
	private static final String JSON_DESCRIPTION = "description";
	private static final String JSON_RESOURCE_ID = "resourceId";
	private static final String JSON_VIDEO_ID = "videoId";

	private static final ThreadSafeDateFormatter PUBLISHED_AT_FORMATTER = new ThreadSafeDateFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.ENGLISH);

	private static final String ISO_8601_Z = "Z";
	private static final String ISO_8601_Z_REPLACEMENT = "+00:00";

	private static final String YOUTUBE_VIDEO_LINK_AND_VIDEO_ID = "https://www.youtube.com/watch?v=%s";

	private static final String COLON = ": ";

	private ArrayList<News> parseAgencyNewsJSON(String jsonString, long lastUpdateInMs, int i) {
		try {
			ArrayList<News> news = new ArrayList<News>();
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			if (json != null && json.has(JSON_ITEMS)) {
				JSONArray jItems = json.getJSONArray(JSON_ITEMS);
				String authority = getAUTHORITY(getContext());
				int severity = getCHANNELS_SEVERITY(getContext()).get(i);
				long noteworthyInMs = getCHANNELS_NOTEWORTHY(getContext()).get(i);
				long maxValidityInMs = getNewsMaxValidityInMs();
				String target = getCHANNELS_TARGETS(getContext()).get(i);
				String color = getCHANNELS_COLORS(getContext()).get(i);
				String authorName = getCHANNELS_AUTHOR_NAME(getContext()).get(i);
				String authorUrl = getCHANNELS_AUTHOR_URL(getContext()).get(i);
				String language = getCHANNELS_LANG(getContext()).get(i);
				for (int l = 0; l < jItems.length(); l++) {
					JSONObject jItem = jItems.getJSONObject(l);
					if (jItem != null && jItem.has(JSON_ID) && jItem.has(JSON_SNIPPET)) {
						try {
							String id = jItem.getString(JSON_ID);
							JSONObject jSnippet = jItem.getJSONObject(JSON_SNIPPET);
							String videoId = jSnippet.getJSONObject(JSON_RESOURCE_ID).getString(JSON_VIDEO_ID);
							String publishedAt = jSnippet.getString(JSON_PUBLISHED_AT);
							String title = jSnippet.getString(JSON_TITLE);
							String description = jSnippet.getString(JSON_DESCRIPTION);
							String uuid = AGENCY_SOURCE_ID + id;
							long pubDateInMs = PUBLISHED_AT_FORMATTER.parseThreadSafe(publishedAt.replace(ISO_8601_Z, ISO_8601_Z_REPLACEMENT)).getTime();
							String link = String.format(YOUTUBE_VIDEO_LINK_AND_VIDEO_ID, videoId);
							StringBuilder textSb = new StringBuilder();
							StringBuilder textHTMLSb = new StringBuilder();
							if (!TextUtils.isEmpty(title)) {
								textSb.append(title);
								textHTMLSb.append(HtmlUtils.applyBold(title));
							}
							if (!TextUtils.isEmpty(description)) {
								if (textSb.length() > 0) {
									textSb.append(COLON);
								}
								textSb.append(Html.fromHtml(description));
								if (textHTMLSb.length() > 0) {
									textHTMLSb.append(HtmlUtils.BR);
								}
								textHTMLSb.append(HtmlUtils.toHTML(description));
							}
							if (!TextUtils.isEmpty(link)) {
								if (textHTMLSb.length() > 0) {
									textHTMLSb.append(HtmlUtils.BR).append(HtmlUtils.BR);
								}
								textHTMLSb.append(HtmlUtils.linkify(link));
							}
							if (textSb.length() == 0 || textHTMLSb.length() == 0) {
								MTLog.w(this, "parseAgencyJSON() > skip (no text)");
								continue;
							}

							news.add(new News(null, authority, uuid, severity, noteworthyInMs, lastUpdateInMs, maxValidityInMs, pubDateInMs, target, color,
									authorName, null, null, authorUrl, textSb.toString(), textHTMLSb.toString(), link, language, AGENCY_SOURCE_ID,
									AGENCY_SOURCE_LABEL));
						} catch (Exception e) {
							MTLog.w(this, e, "Error while parsing '%s'!", jItem);
						}
					}
				}
			}
			return news;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return null;
		}
	}

	private static class YouTubeNewsDbHelper extends NewsProvider.NewsDbHelper {

		private static final String TAG = YouTubeNewsDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link YouTubeNewsDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "youtube.db";

		/**
		 * Override if multiple {@link YouTubeNewsDbHelper} implementations in same app.
		 */
		protected static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pYouTubeNewsLastUpdate";

		public static final String T_YOUTUBE_NEWS = NewsProvider.NewsDbHelper.T_NEWS;

		private static final String T_YOUTUBE_NEWS_SQL_CREATE = NewsProvider.NewsDbHelper.getSqlCreateBuilder(T_YOUTUBE_NEWS).build();

		private static final String T_YOUTUBE_NEWS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_YOUTUBE_NEWS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link YouTubeNewsDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.youtube_db_version);
			}
			return dbVersion;
		}

		private Context context;

		public YouTubeNewsDbHelper(Context context) {
			this(context, DB_NAME, getDbVersion(context));
		}

		public YouTubeNewsDbHelper(Context context, String dbName, int dbVersion) {
			super(context, dbName, dbVersion);
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
			db.execSQL(T_YOUTUBE_NEWS_SQL_DROP);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l, true);
			initAllDbTables(db);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_YOUTUBE_NEWS_SQL_CREATE);
		}
	}

}
