package org.mtransit.android.commons.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.News;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;

import twitter4j.User;

@SuppressLint("Registered")
public class TwitterNewsProvider extends NewsProvider {

	private static final String TAG = TwitterNewsProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = TwitterNewsDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_LANG = TwitterNewsDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_LANG;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static final String PREF_KEY_ACCESS_TOKEN = "pTwitterAccessToken";

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.twitter_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String consumerKey = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static String getCONSUMER_KEY(Context context) {
		if (consumerKey == null) {
			consumerKey = context.getResources().getString(R.string.twitter_consumer_key);
		}
		return consumerKey;
	}

	private static String consumerSecret = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static String getCONSUMER_SECRET(Context context) {
		if (consumerSecret == null) {
			consumerSecret = context.getResources().getString(R.string.twitter_consumer_secret);
		}
		return consumerSecret;
	}

	private static String color = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static String getCOLOR(Context context) {
		if (color == null) {
			color = context.getResources().getString(R.string.twitter_color);
		}
		return color;
	}

	private static java.util.List<String> screenNames = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCREEN_NAMES(Context context) {
		if (screenNames == null) {
			screenNames = Arrays.asList(context.getResources().getStringArray(R.array.twitter_screen_names));
		}
		return screenNames;
	}

	private static java.util.List<String> screenNamesLang = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCREEN_NAMES_LANG(Context context) {
		if (screenNamesLang == null) {
			screenNamesLang = Arrays.asList(context.getResources().getStringArray(R.array.twitter_screen_names_lang));
		}
		return screenNamesLang;
	}

	private static java.util.List<String> screenNamesColors = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCREEN_NAMES_COLORS(Context context) {
		if (screenNamesColors == null) {
			screenNamesColors = Arrays.asList(context.getResources().getStringArray(R.array.twitter_screen_names_colors));
		}
		return screenNamesColors;
	}

	private static java.util.List<String> screenNamesTargets = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static java.util.List<String> getSCREEN_NAMES_TARGETS(Context context) {
		if (screenNamesTargets == null) {
			screenNamesTargets = Arrays.asList(context.getResources().getStringArray(R.array.twitter_screen_names_target));
		}
		return screenNamesTargets;
	}

	private static java.util.List<Integer> screenNamesSeverity = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static java.util.List<Integer> getSCREEN_NAMES_SEVERITY(Context context) {
		if (screenNamesSeverity == null) {
			screenNamesSeverity = ArrayUtils.asIntegerList(context.getResources().getIntArray(R.array.twitter_screen_names_severity));
		}
		return screenNamesSeverity;
	}

	private static java.util.List<Long> screenNamesNoteworthy = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static java.util.List<Long> getSCREEN_NAMES_NOTEWORTHY(Context context) {
		if (screenNamesNoteworthy == null) {
			screenNamesNoteworthy = ArrayUtils.asLongList(context.getResources().getStringArray(R.array.twitter_screen_names_noteworthy));
		}
		return screenNamesNoteworthy;
	}

	private static String accessToken = null;

	private static String getACCESS_TOKEN(Context context) {
		if (accessToken == null) {
			accessToken = PreferenceUtils.getPrefLcl(context, PREF_KEY_ACCESS_TOKEN, StringUtils.EMPTY);
		}
		return accessToken;
	}

	private static void setACCESS_TOKEN(Context context, String newAccessToken) {
		accessToken = newAccessToken;
		PreferenceUtils.savePrefLcl(context, PREF_KEY_ACCESS_TOKEN, accessToken, false); // asynchronous
	}

	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	private static TwitterNewsDbHelper dbHelper;
	private static int currentDbVersion = -1;

	private TwitterNewsDbHelper getDBHelper(Context context) {
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
	 * Override if multiple {@link TwitterNewsDbHelper} implementations in same app.
	 */
	@Override
	public int getCurrentDbVersion() {
		return TwitterNewsDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link TwitterNewsDbHelper} implementations in same app.
	 */
	@Override
	public TwitterNewsDbHelper getNewDbHelper(Context context) {
		return new TwitterNewsDbHelper(context.getApplicationContext());
	}

	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	private static final long NEWS_MAX_VALIDITY_IN_MS = Long.MAX_VALUE; // FOREVER
	private static final long NEWS_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long NEWS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

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

	@Override
	public ArrayList<News> getNewNews(NewsProviderContract.Filter newsFilter) {
		if (newsFilter == null) {
			MTLog.w(this, "getNewNews() > no new service update (filter null)");
			return null;
		}
		updateAgencyNewsDataIfRequired(newsFilter.isInFocusOrDefault());
		ArrayList<News> cachedNews = getCachedNews(newsFilter);
		return cachedNews;
	}

	private static final String AGENCY_SOURCE_ID = "twitter";

	private static final String AGENCY_SOURCE_LABEL = "Twitter";

	private void updateAgencyNewsDataIfRequired(boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L);
		String lastUpdateLang = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_LANG, StringUtils.EMPTY);
		long minUpdateMs = Math.min(getNewsMaxValidityInMs(), getNewsValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs && LocaleUtils.getDefaultLanguage().equals(lastUpdateLang)) {
			return;
		}
		updateAgencyNewsDataIfRequiredSync(lastUpdateInMs, lastUpdateLang, inFocus);
	}

	private synchronized void updateAgencyNewsDataIfRequiredSync(long lastUpdateInMs, String lastUpdateLang, boolean inFocus) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L) > lastUpdateInMs //
				&& LocaleUtils.getDefaultLanguage().equals(lastUpdateLang)) {
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		if (lastUpdateInMs + getNewsMaxValidityInMs() < nowInMs || !LocaleUtils.getDefaultLanguage().equals(lastUpdateLang)) {
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
			PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_LANG, LocaleUtils.getDefaultLanguage(), true); // sync
		} // else keep whatever we have until max validity reached
	}

	private static final String TOKEN_TYPE_BEARER = "bearer";

	private ArrayList<News> loadAgencyNewsDataFromWWW() {
		try {
			twitter4j.conf.ConfigurationBuilder builder = new twitter4j.conf.ConfigurationBuilder();
			builder.setApplicationOnlyAuthEnabled(true);
			builder.setOAuthConsumerKey(getCONSUMER_KEY(getContext()));
			builder.setOAuthConsumerSecret(getCONSUMER_SECRET(getContext()));
			builder.setTweetModeExtended(true);
			twitter4j.Twitter twitter = new twitter4j.TwitterFactory(builder.build()).getInstance();
			String accessToken = getACCESS_TOKEN(getContext());
			if (TextUtils.isEmpty(accessToken)) {
				twitter4j.auth.OAuth2Token token = twitter.getOAuth2Token();
				if (TOKEN_TYPE_BEARER.equals(token.getTokenType())) {
					setACCESS_TOKEN(getContext(), token.getAccessToken());
				} else {
					MTLog.w(this, "Unexpected token type '%s' in token '%s'!", token.getTokenType(), token);
					return null;
				}
			} else {
				twitter.setOAuth2Token(new twitter4j.auth.OAuth2Token(TOKEN_TYPE_BEARER, accessToken));
			}
			ArrayList<News> newNews = new ArrayList<News>();
			long maxValidityInMs = getNewsMaxValidityInMs();
			String authority = getAUTHORITY(getContext());
			int i = 0;
			for (String screenName : getSCREEN_NAMES(getContext())) {
				String userLang = getSCREEN_NAMES_LANG(getContext()).get(i);
				if (!LocaleUtils.MULTIPLE.equals(userLang) && !LocaleUtils.UNKNOWN.equals(userLang) && !LocaleUtils.getDefaultLanguage().equals(userLang)) {
					i++;
					continue;
				}
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				twitter4j.ResponseList<twitter4j.Status> statuses = twitter.getUserTimeline(screenName, new twitter4j.Paging(1, 80));
				String target = getSCREEN_NAMES_TARGETS(getContext()).get(i);
				int severity = getSCREEN_NAMES_SEVERITY(getContext()).get(i);
				long noteworthyInMs = getSCREEN_NAMES_NOTEWORTHY(getContext()).get(i);
				for (twitter4j.Status status : statuses) {
					if (status.getInReplyToUserId() >= 0) {
						continue;
					}
					String link = getNewsWebURL(status);
					StringBuilder textHTMLSb = new StringBuilder(getHTMLText(status));
					if (!TextUtils.isEmpty(link)) {
						if (textHTMLSb.length() > 0) {
							textHTMLSb.append(HtmlUtils.BR).append(HtmlUtils.BR);
						}
						textHTMLSb.append(HtmlUtils.linkify(link));
					}
					String lang = getLang(status, userLang);
					News news = new News(null, authority, AGENCY_SOURCE_ID + status.getId(), severity, noteworthyInMs, newLastUpdateInMs, maxValidityInMs,
							status.getCreatedAt().getTime(), target, getColor(status.getUser()), status.getUser().getName(), getUserName(status.getUser()),
							status.getUser().getProfileImageURLHttps(), getAuthorProfileURL(status.getUser()), status.getText(), textHTMLSb.toString(), link,
							lang, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL);
					newNews.add(news);
				}
				i++;
			}
			return newNews;
		} catch (Exception e) {
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private String getLang(twitter4j.Status status, String userLang) {
		String lang = userLang;
		if (LocaleUtils.MULTIPLE.equals(lang)) {
			if (LocaleUtils.isFR(status.getLang())) {
				lang = Locale.FRENCH.getLanguage();
			} else if (LocaleUtils.isEN(status.getLang())) {
				lang = Locale.ENGLISH.getLanguage();
			}
		}
		if (TextUtils.isEmpty(lang)) {
			lang = LocaleUtils.UNKNOWN;
		}
		return lang;
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

	private String getColor(User user) {
		try {
			return getSCREEN_NAMES_COLORS(getContext()).get(getSCREEN_NAMES(getContext()).indexOf(user.getScreenName()));
		} catch (Exception e) {
			MTLog.w(this, "Error while finding user '%s' color!", user);
			return getCOLOR(getContext());
		}
	}

	private String getUserName(User user) {
		return String.format(MENTION_AND_SCREEN_NAME, user.getScreenName());
	}

	private static final String HASH_TAG_AND_TAG = "#%s";
	private static final String MENTION_AND_SCREEN_NAME = "@%s";
	private static final String REGEX_AND_STRING = "(%s)";

	private String getHTMLText(twitter4j.Status status) {
		if (status == null) {
			return null;
		}
		try {
			String textHTML = status.getText();
			try {
				if (status.isRetweet()) { // fix RT truncated at the end
					if (textHTML.length() >= 140) {
						String textRT = status.getRetweetedStatus().getText();
						int indexOf = textHTML.indexOf(textRT.substring(0, 70));
						textHTML = textHTML.substring(0, indexOf) + textRT;
					}
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Can't fix truncated RT '%s'! (using original text)", status.getText());
			}
			for (twitter4j.URLEntity urlEntity : status.getURLEntities()) {
				textHTML = textHTML.replace(urlEntity.getURL(), getURL(urlEntity.getURL(), urlEntity.getDisplayURL()));
			}
			for (twitter4j.HashtagEntity hashtagEntity : status.getHashtagEntities()) {
				String hashtag = String.format(HASH_TAG_AND_TAG, hashtagEntity.getText());
				textHTML = textHTML.replace(hashtag, getURL(getHashtagURL(hashtagEntity.getText()), hashtag));
			}
			for (twitter4j.UserMentionEntity userMentionEntity : status.getUserMentionEntities()) {
				String userMention = String.format(MENTION_AND_SCREEN_NAME, userMentionEntity.getScreenName());
				textHTML = Pattern.compile(String.format(REGEX_AND_STRING, userMention), Pattern.CASE_INSENSITIVE).matcher(textHTML)
						.replaceAll(getURL(getAuthorProfileURL(userMentionEntity.getScreenName()), userMention));
			}
			ArrayMap<String, HashSet<String>> urlToMediaUrls = new ArrayMap<String, HashSet<String>>();
			for (twitter4j.MediaEntity mediaEntity : status.getMediaEntities()) {
				if (!urlToMediaUrls.containsKey(mediaEntity.getURL())) {
					urlToMediaUrls.put(mediaEntity.getURL(), new HashSet<String>());
				}
				urlToMediaUrls.get(mediaEntity.getURL()).add(mediaEntity.getMediaURLHttps());
			}
			for (twitter4j.MediaEntity mediaEntity : status.getMediaEntities()) {
				if (!urlToMediaUrls.containsKey(mediaEntity.getURL())) {
					urlToMediaUrls.put(mediaEntity.getURL(), new HashSet<String>());
				}
				urlToMediaUrls.get(mediaEntity.getURL()).add(mediaEntity.getMediaURLHttps());
			}
			for (ArrayMap.Entry<String, HashSet<String>> entry : urlToMediaUrls.entrySet()) {
				StringBuilder sb = new StringBuilder();
				for (String mediaUrl : entry.getValue()) {
					if (sb.length() > 0) {
						sb.append(StringUtils.SPACE_CAR);
					}
					sb.append(getURL(mediaUrl, mediaUrl));
				}
				textHTML = textHTML.replace(entry.getKey(), sb.toString());
			}
			return textHTML;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while generating HTML text for status '%s'!", status);
			return status.getText();
		}
	}

	private static final String HREF_URL_AND_URL_AND_TEXT = "<A HREF=\"%s\">%s</A>";

	private String getURL(String url, String text) {
		return String.format(HREF_URL_AND_URL_AND_TEXT, url, text);
	}

	private static final String WEB_URL_AND_SCREEN_NAME_AND_ID = "https://twitter.com/%s/status/%s";

	private String getNewsWebURL(twitter4j.Status status) {
		return String.format(WEB_URL_AND_SCREEN_NAME_AND_ID, status.getUser().getScreenName(), status.getId()); // id or id_str ?
	}

	private static final String AUTHOR_PROFILE_URL_AND_SCREEN_NAME = "https://twitter.com/%s";

	private String getAuthorProfileURL(twitter4j.User user) {
		return getAuthorProfileURL(user.getScreenName());
	}

	private String getAuthorProfileURL(String userScreenName) {
		return String.format(AUTHOR_PROFILE_URL_AND_SCREEN_NAME, userScreenName);
	}

	private static final String HASHTAG_URL_AND_TAG = "https://twitter.com/hashtag/%s";

	private String getHashtagURL(String hashtag) {
		return String.format(HASHTAG_URL_AND_TAG, hashtag);
	}

	private static class TwitterNewsDbHelper extends NewsProvider.NewsDbHelper {

		private static final String TAG = TwitterNewsDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link TwitterNewsDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "news_twitter.db";

		/**
		 * Override if multiple {@link TwitterNewsDbHelper} implementations in same app.
		 */
		protected static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pTwitterNewsLastUpdate";

		/**
		 * Override if multiple {@link TwitterNewsDbHelper} implementations in same app.
		 */
		protected static final String PREF_KEY_AGENCY_LAST_UPDATE_LANG = "pTwitterNewsLastUpdateLang";

		public static final String T_TWITTER_NEWS = NewsProvider.NewsDbHelper.T_NEWS;

		private static final String T_TWITTER_NEWS_SQL_CREATE = NewsProvider.NewsDbHelper.getSqlCreateBuilder(T_TWITTER_NEWS).build();

		private static final String T_TWITTER_NEWS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_TWITTER_NEWS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link TwitterNewsDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.twitter_db_version);
			}
			return dbVersion;
		}

		private Context context;

		public TwitterNewsDbHelper(Context context) {
			this(context, DB_NAME, getDbVersion(context));
		}

		public TwitterNewsDbHelper(Context context, String dbName, int dbVersion) {
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
			db.execSQL(T_TWITTER_NEWS_SQL_DROP);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L, true);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_LAST_UPDATE_LANG, StringUtils.EMPTY, true);
			initAllDbTables(db);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_TWITTER_NEWS_SQL_CREATE);
		}
	}
}
