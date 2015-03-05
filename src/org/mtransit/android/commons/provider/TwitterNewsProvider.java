package org.mtransit.android.commons.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.News;

import twitter4j.User;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

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
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = NewsDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	private static final String PREF_KEY_ACCESS_TOKEN = "pTwitterAccessToken";

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.twitter_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link GTFSRealTimeProvider} implementations in same app.
	 */
	public static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String targetAuthority = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	public static String getTARGET_AUTHORITY(Context context) {
		if (targetAuthority == null) {
			targetAuthority = context.getResources().getString(R.string.twitter_target_for_poi_authority);
		}
		return targetAuthority;
	}

	private static String consumerKey = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	public static String getCONSUMER_KEY(Context context) {
		if (consumerKey == null) {
			consumerKey = context.getResources().getString(R.string.twitter_consumer_key);
		}
		return consumerKey;
	}

	private static String consumerSecret = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	public static String getCONSUMER_SECRET(Context context) {
		if (consumerSecret == null) {
			consumerSecret = context.getResources().getString(R.string.twitter_consumer_secret);
		}
		return consumerSecret;
	}

	private static String color = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	public static String getCOLOR(Context context) {
		if (color == null) {
			color = context.getResources().getString(R.string.twitter_color);
		}
		return color;
	}

	private static java.util.List<String> screenNames = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getSCREEN_NAMES(Context context) {
		if (screenNames == null) {
			screenNames = Arrays.asList(context.getResources().getStringArray(R.array.twitter_screen_names));
		}
		return screenNames;
	}

	private static java.util.List<String> screenNamesColors = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getSCREEN_NAMES_COLORS(Context context) {
		if (screenNamesColors == null) {
			screenNamesColors = Arrays.asList(context.getResources().getStringArray(R.array.twitter_screen_names_colors));
		}
		return screenNamesColors;
	}

	private static String accessToken = null;

	public static String getACCESS_TOKEN(Context context) {
		if (accessToken == null) {
			accessToken = PreferenceUtils.getPrefLcl(context, PREF_KEY_ACCESS_TOKEN, StringUtils.EMPTY);
		}
		return accessToken;
	}

	public static void setACCESS_TOKEN(Context context, String newAccessToken) {
		accessToken = newAccessToken;
		PreferenceUtils.savePrefLcl(context, PREF_KEY_ACCESS_TOKEN, accessToken, false); // asynchronous
	}

	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
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
		SQLiteDatabase db = null;
		try {
			db = getDBHelper().getWritableDatabase();
			String selection = new StringBuilder() //
					.append(NewsProviderContract.Columns.T_NEWS_K_SOURCE_ID).append("=").append('\'').append(AGENCY_SOURCE_ID).append('\'') //
					.toString();
			affectedRows = db.delete(getNewsDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all agency news data!");
		} finally {
			SqlUtils.closeQuietly(db);
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
		ArrayList<News> cachedNews = new ArrayList<News>();
		String targetUUID = getTARGET_AUTHORITY(getContext());
		cachedNews.addAll(NewsProvider.getCachedNewsS(this, targetUUID));
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

	private static final String TOKEN_TYPE_BEARER = "bearer";

	private ArrayList<News> loadAgencyNewsDataFromWWW() {
		try {
			twitter4j.conf.ConfigurationBuilder builder = new twitter4j.conf.ConfigurationBuilder();
			builder.setApplicationOnlyAuthEnabled(true);
			builder.setOAuthConsumerKey(getCONSUMER_KEY(getContext()));
			builder.setOAuthConsumerSecret(getCONSUMER_SECRET(getContext()));
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
			String targetAuthority = getTARGET_AUTHORITY(getContext());
			long maxValidityInMs = getNewsMaxValidityInMs();
			String authority = getAUTHORITY(getContext());
			for (String screenName : getSCREEN_NAMES(getContext())) {
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				twitter4j.ResponseList<twitter4j.Status> statuses = twitter.getUserTimeline(screenName);
				for (twitter4j.Status status : statuses) {
					if (status.getInReplyToUserId() >= 0) {
						continue;
					}
					String textHTML = getHTMLText(status);
					News news = new News(null, authority, AGENCY_SOURCE_ID + status.getId(), newLastUpdateInMs, maxValidityInMs, status.getCreatedAt()
							.getTime(), targetAuthority, getColor(status.getUser()), status.getUser().getName(), getUserName(status.getUser()), status
							.getUser().getProfileImageURLHttps(), getAuthorProfileURL(status.getUser()), status.getText(), textHTML, getNewsWebURL(status),
							status.getLang(), AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL);
					newNews.add(news);
				}
			}
			return newNews;
		} catch (Exception e) {
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
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

	private static final String HASHTAG_AND_TAG = "#%s";
	private static final String MENTION_AND_SCREEN_NAME = "@%s";
	private static final String REGEX_AND_STRING = "(%s)";

	private String getHTMLText(twitter4j.Status status) {
		if (status == null) {
			return null;
		}
		try {
			String textHTML = status.getText();
			for (twitter4j.URLEntity urlEntity : status.getURLEntities()) {
				textHTML = textHTML.replace(urlEntity.getURL(), getURL(urlEntity.getURL(), urlEntity.getDisplayURL()));
			}
			for (twitter4j.HashtagEntity hashtagEntity : status.getHashtagEntities()) {
				String hashtag = String.format(HASHTAG_AND_TAG, hashtagEntity.getText());
				textHTML = textHTML.replace(hashtag, getURL(getHashtagURL(hashtagEntity.getText()), hashtag));
			}
			for (twitter4j.UserMentionEntity userMentionEntity : status.getUserMentionEntities()) {
				String userMention = String.format(MENTION_AND_SCREEN_NAME, userMentionEntity.getScreenName());
				textHTML = Pattern.compile(String.format(REGEX_AND_STRING, userMention), Pattern.CASE_INSENSITIVE).matcher(textHTML)
						.replaceAll(getURL(getAuthorProfileURL(userMentionEntity.getScreenName()), userMention));
			}
			HashMap<String, HashSet<String>> urlToMediaUrls = new HashMap<String, HashSet<String>>();
			for (twitter4j.MediaEntity exMediaEntity : status.getExtendedMediaEntities()) {
				if (!urlToMediaUrls.containsKey(exMediaEntity.getURL())) {
					urlToMediaUrls.put(exMediaEntity.getURL(), new HashSet<String>());
				}
				urlToMediaUrls.get(exMediaEntity.getURL()).add(exMediaEntity.getMediaURLHttps());
			}
			for (twitter4j.MediaEntity mediaEntity : status.getMediaEntities()) {
				if (!urlToMediaUrls.containsKey(mediaEntity.getURL())) {
					urlToMediaUrls.put(mediaEntity.getURL(), new HashSet<String>());
				}
				urlToMediaUrls.get(mediaEntity.getURL()).add(mediaEntity.getMediaURLHttps());
			}
			for (HashMap.Entry<String, HashSet<String>> entry : urlToMediaUrls.entrySet()) {
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
			MTLog.w(this, e, "Error while genereting HTML text for status '%s'!", status);
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
}
