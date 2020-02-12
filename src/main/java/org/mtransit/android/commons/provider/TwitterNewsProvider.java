package org.mtransit.android.commons.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SimpleArrayMap;
import retrofit2.Response;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.BuildConfig;
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

import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.models.HashtagEntity;
import com.twitter.sdk.android.core.models.MediaEntity;
import com.twitter.sdk.android.core.models.MentionEntity;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.models.UrlEntity;
import com.twitter.sdk.android.core.models.User;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

@SuppressLint("Registered")
public class TwitterNewsProvider extends NewsProvider {

	private static final String LOG_TAG = TwitterNewsProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
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

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
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
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.twitter_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static String consumerKey = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	@NonNull
	private static String getCONSUMER_KEY(@NonNull Context context) {
		if (consumerKey == null) {
			consumerKey = context.getResources().getString(R.string.twitter_consumer_key);
		}
		return consumerKey;
	}

	@Nullable
	private static String consumerSecret = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	@NonNull
	private static String getCONSUMER_SECRET(@NonNull Context context) {
		if (consumerSecret == null) {
			consumerSecret = context.getResources().getString(R.string.twitter_consumer_secret);
		}
		return consumerSecret;
	}

	@Nullable
	private static String color = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	@NonNull
	private static String getCOLOR(@NonNull Context context) {
		if (color == null) {
			color = context.getResources().getString(R.string.twitter_color);
		}
		return color;
	}

	@Nullable
	private static java.util.List<String> screenNames = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCREEN_NAMES(@NonNull Context context) {
		if (screenNames == null) {
			screenNames = Arrays.asList(context.getResources().getStringArray(R.array.twitter_screen_names));
		}
		return screenNames;
	}

	@Nullable
	private static java.util.List<String> screenNamesLang = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCREEN_NAMES_LANG(@NonNull Context context) {
		if (screenNamesLang == null) {
			screenNamesLang = Arrays.asList(context.getResources().getStringArray(R.array.twitter_screen_names_lang));
		}
		return screenNamesLang;
	}

	@Nullable
	private static java.util.List<String> screenNamesColors = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCREEN_NAMES_COLORS(@NonNull Context context) {
		if (screenNamesColors == null) {
			screenNamesColors = Arrays.asList(context.getResources().getStringArray(R.array.twitter_screen_names_colors));
		}
		return screenNamesColors;
	}

	@Nullable
	private static java.util.List<String> screenNamesTargets = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getSCREEN_NAMES_TARGETS(@NonNull Context context) {
		if (screenNamesTargets == null) {
			screenNamesTargets = Arrays.asList(context.getResources().getStringArray(R.array.twitter_screen_names_target));
		}
		return screenNamesTargets;
	}

	@Nullable
	private static java.util.List<Integer> screenNamesSeverity = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<Integer> getSCREEN_NAMES_SEVERITY(@NonNull Context context) {
		if (screenNamesSeverity == null) {
			screenNamesSeverity = ArrayUtils.asIntegerList(context.getResources().getIntArray(R.array.twitter_screen_names_severity));
		}
		return screenNamesSeverity;
	}

	@Nullable
	private static java.util.List<Long> screenNamesNoteworthy = null;

	/**
	 * Override if multiple {@link TwitterNewsProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<Long> getSCREEN_NAMES_NOTEWORTHY(@NonNull Context context) {
		if (screenNamesNoteworthy == null) {
			screenNamesNoteworthy = ArrayUtils.asLongList(context.getResources().getStringArray(R.array.twitter_screen_names_noteworthy));
		}
		return screenNamesNoteworthy;
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		//noinspection ConstantConditions // TODO requireContext()
		return getURIMATCHER(getContext());
	}

	@Nullable
	private TwitterNewsDbHelper dbHelper;

	private static int currentDbVersion = -1;

	@NonNull
	private TwitterNewsDbHelper getDBHelper(@NonNull Context context) {
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
		//noinspection ConstantConditions // TODO requireContext()
		return TwitterNewsDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link TwitterNewsDbHelper} implementations in same app.
	 */
	@NonNull
	@Override
	public TwitterNewsDbHelper getNewDbHelper(@NonNull Context context) {
		return new TwitterNewsDbHelper(context.getApplicationContext());
	}

	@NonNull
	@Override
	public SQLiteOpenHelper getDBHelper() {
		//noinspection ConstantConditions // TODO requireContext()
		return getDBHelper(getContext());
	}

	private static final long NEWS_MAX_VALIDITY_IN_MS = Long.MAX_VALUE; // FOREVER
	private static final long NEWS_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long NEWS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(10L);
	private static final long NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1L);

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
	public boolean deleteCachedNews(@SuppressLint("UnknownNullness") Integer serviceUpdateId) {
		return NewsProvider.deleteCachedNews(this, serviceUpdateId);
	}

	@SuppressWarnings("UnusedReturnValue")
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

	@NonNull
	@Override
	public String getAuthority() {
		//noinspection ConstantConditions // TODO requireContext()
		return getAUTHORITY(getContext());
	}

	@NonNull
	@Override
	public Uri getAuthorityUri() {
		//noinspection ConstantConditions // TODO requireContext()
		return getAUTHORITY_URI(getContext());
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
	public ArrayList<News> getNewNews(@NonNull NewsProviderContract.Filter newsFilter) {
		updateAgencyNewsDataIfRequired(newsFilter.isInFocusOrDefault());
		return getCachedNews(newsFilter);
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

	private static final int MAX_ITEM_PER_REQUESTS = 80;
	private static final boolean INCLUDE_REPLIES = false;
	private static final boolean INCLUDE_RETWEET = true;

	@Nullable
	private ArrayList<News> loadAgencyNewsDataFromWWW() {
		try {
			Context context = getContext();
			if (context == null) {
				return null;
			}
			Twitter.initialize(new TwitterConfig.Builder((Application) context.getApplicationContext())
					.twitterAuthConfig(
							new TwitterAuthConfig(
									getCONSUMER_KEY(context),
									getCONSUMER_SECRET(context)
							))
					.logger(new DefaultLogger(
							BuildConfig.DEBUG ? android.util.Log.DEBUG : android.util.Log.INFO
					))
					.debug(BuildConfig.DEBUG)
					.build());
			TwitterCore twitterCore = TwitterCore.getInstance();
			ArrayList<News> newNews = new ArrayList<>();
			long maxValidityInMs = getNewsMaxValidityInMs();
			String authority = getAUTHORITY(context);
			int i = 0;
			for (String screenName : getSCREEN_NAMES(context)) {
				loadUserTimeline(context, twitterCore, newNews, maxValidityInMs, authority, i, screenName);
				i++;
				MTLog.i(this, "loadAgencyNewsDataFromWWW() > i: %d", i);
			}
			MTLog.i(this, "Loaded %d news.", newNews.size());
			return newNews;
		} catch (IOException ioe) {
			MTLog.e(LOG_TAG, ioe, "I/O ERROR: Unknown Exception");
			return null;
		} catch (Exception e) {
			MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private void loadUserTimeline(@NonNull Context context,
								  @NonNull TwitterCore twitterCore,
								  @NonNull List<News> newNews,
								  long maxValidityInMs,
								  @NonNull String authority,
								  int i,
								  @NonNull String screenName) throws IOException {
		MTLog.i(this, "loadUserTimeline()");
		String userLang = getSCREEN_NAMES_LANG(context).get(i);
		MTLog.i(this, "loadUserTimeline() > userLang : " + userLang);
		if (!LocaleUtils.MULTIPLE.equals(userLang)
				&& !LocaleUtils.UNKNOWN.equals(userLang)
				&& !LocaleUtils.getDefaultLanguage().equals(userLang)) {
			MTLog.d(this, "SKIP loading '@%s': different language (%s).", screenName, userLang);
			return;
		}
		long newLastUpdateInMs = TimeUtils.currentTimeMillis();
		MTLog.i(this, "Loading from 'twitter.com' for '@%s'...", screenName);
		Response<List<Tweet>> response = twitterCore.getApiClient().getStatusesService().userTimeline(
				null,
				screenName,
				MAX_ITEM_PER_REQUESTS,
				null,
				null,
				false,
				!INCLUDE_REPLIES,
				null,
				INCLUDE_RETWEET
		).execute();
		MTLog.i(this, "loadUserTimeline() > response : " + response);
		String target = getSCREEN_NAMES_TARGETS(context).get(i);
		MTLog.i(this, "loadUserTimeline() > target : " + target);
		int severity = getSCREEN_NAMES_SEVERITY(context).get(i);
		long noteworthyInMs = getSCREEN_NAMES_NOTEWORTHY(context).get(i);
		MTLog.i(this, "loadUserTimeline() > noteworthyInMs : " + noteworthyInMs);
		if (response != null && response.isSuccessful()) {
			List<Tweet> statuses = response.body();
			MTLog.i(this, "loadUserTimeline() > statuses : " + statuses);
			if (statuses != null) {
				MTLog.i(this, "loadUserTimeline() > statuses: " + statuses.size());
				for (Tweet status : statuses) {
					MTLog.i(this, "loadUserTimeline() > status : " + statuses);
					News news = readNews(
							context, status,
							authority, target,
							screenName, userLang,
							maxValidityInMs, newLastUpdateInMs,
							severity, noteworthyInMs
					);
					if (news != null) {
						newNews.add(news);
					}
				}
			}
		}
	}

	@Nullable
	private News readNews(Context context, Tweet status,
						  String authority, String target,
						  String screenName, String userLang,
						  long maxValidityInMs, long newLastUpdateInMs,
						  int severity, long noteworthyInMs) {
		MTLog.i(this, "readNews() > status: %s", status);
		if (status.inReplyToUserId > 0L) {
			MTLog.i(this, "readNews() > SKIP (status.inReplyToUserId: %s)", status.inReplyToUserId);
			return null;
		}
		final User user = status.user;
		MTLog.i(this, "readNews() > user: %s", user);
		String userScreenName = user == null ? screenName : user.screenName;
		String userName = user == null ? screenName : user.name;
		String userProfileImageUrl = user == null ? null : user.profileImageUrlHttps;
		String link = getNewsWebURL(status, userScreenName);
		StringBuilder textHTMLSb = new StringBuilder();
		textHTMLSb.append(getHTMLText(status));
		if (!TextUtils.isEmpty(link)) {
			if (textHTMLSb.length() > 0) {
				textHTMLSb.append(HtmlUtils.BR).append(HtmlUtils.BR);
			}
			textHTMLSb.append(HtmlUtils.linkify(link));
		}
		String lang = getLang(status, userLang);
		long createdAtInMs = apiTimeToLong(status.createdAt);
		News news = new News(null,
				authority,
				AGENCY_SOURCE_ID + status.getId(),
				severity,
				noteworthyInMs,
				newLastUpdateInMs,
				maxValidityInMs,
				createdAtInMs,
				target,
				getColor(context, userScreenName),
				userName,
				getUserName(userScreenName),
				userProfileImageUrl,
				getAuthorProfileURL(userScreenName), //
				StringUtils.oneLineOneSpace(status.text), //
				textHTMLSb.toString(), //
				link, lang, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL);
		return news;
	}

	// https://github.com/twitter-archive/twitter-kit-android/blob/master/tweet-ui/src/main/java/com/twitter/sdk/android/tweetui/TweetDateUtils.java
	private static final SimpleDateFormat DATE_TIME_RFC822 = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.ENGLISH);

	private static final long INVALID_DATE = -1;

	private static long apiTimeToLong(@Nullable String apiTime) {
		if (apiTime == null) return INVALID_DATE;

		try {
			final Date parse = DATE_TIME_RFC822.parse(apiTime);
			return parse == null ? INVALID_DATE : parse.getTime();
		} catch (ParseException e) {
			return INVALID_DATE;
		}
	}

	private String getLang(@NonNull Tweet status, String userLang) {
		String lang = userLang;
		if (LocaleUtils.MULTIPLE.equals(lang)) {
			if (LocaleUtils.isFR(status.lang)) {
				lang = Locale.FRENCH.getLanguage();
			} else if (LocaleUtils.isEN(status.lang)) {
				lang = Locale.ENGLISH.getLanguage();
			}
		}
		if (TextUtils.isEmpty(lang)) {
			lang = LocaleUtils.UNKNOWN;
		}
		return lang;
	}

	@Nullable
	private static Collection<String> languages = null;

	@NonNull
	@Override
	public Collection<String> getNewsLanguages() {
		if (languages == null) {
			languages = new HashSet<>();
			if (LocaleUtils.isFR()) {
				languages.add(Locale.FRENCH.getLanguage());
			} else {
				languages.add(Locale.ENGLISH.getLanguage());
			}
			languages.add(LocaleUtils.UNKNOWN);
		}
		return languages;
	}

	private String getColor(@NonNull Context context, @NonNull User user) {
		return getColor(context, user.screenName);
	}

	private String getColor(@NonNull Context context, @NonNull String screenName) {
		try {
			return getSCREEN_NAMES_COLORS(context).get(
					getSCREEN_NAMES(context).indexOf(screenName)
			);
		} catch (Exception e) {
			MTLog.w(this, "Error while finding user color '%s'!", screenName);
			return getCOLOR(context);
		}
	}

	@NonNull
	private String getUserName(@NonNull User user) {
		return getUserName(user.screenName);
	}

	@NonNull
	private String getUserName(@NonNull String screenName) {
		return String.format(MENTION_AND_SCREEN_NAME, screenName);
	}

	private static final String HASH_TAG_AND_TAG = "#%s";
	private static final String MENTION_AND_SCREEN_NAME = "@%s";
	private static final String REGEX_AND_STRING = "(%s)";

	@Nullable
	private String getHTMLText(@NonNull Tweet status) {
		try {
			String textHTML = status.text;
			try {
				if (status.retweeted) { // fix RT truncated at the end
					if (textHTML.length() >= 140) {
						String textRT = status.retweetedStatus.text;
						int indexOf = textHTML.indexOf(textRT.substring(0, 70));
						textHTML = textHTML.substring(0, indexOf) + textRT;
					}
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Can't fix truncated RT '%s'! (using original text)", status.text);
			}
			if (status.entities.urls != null) {
				for (UrlEntity urlEntity : status.entities.urls) {
					textHTML = textHTML.replace(urlEntity.url, getURL(urlEntity.url, urlEntity.displayUrl));
				}
			}
			if (status.entities.hashtags != null) {
				for (HashtagEntity hashTagEntity : status.entities.hashtags) {
					String hashTag = String.format(HASH_TAG_AND_TAG, hashTagEntity.text);
					textHTML = textHTML.replace(hashTag, getURL(getHashTagURL(hashTagEntity.text), hashTag));
				}
			}
			if (status.entities.userMentions != null) {
				for (MentionEntity userMentionEntity : status.entities.userMentions) {
					String userMention = String.format(MENTION_AND_SCREEN_NAME, userMentionEntity.screenName);
					textHTML = Pattern.compile(String.format(REGEX_AND_STRING, userMention),
							Pattern.CASE_INSENSITIVE) //
							.matcher(textHTML)
							.replaceAll(
									getURL(
											getAuthorProfileURL(userMentionEntity.screenName),
											userMention
									)
							);
				}
			}
			SimpleArrayMap<String, HashSet<String>> urlToMediaUrls = getURLToMediaURLs(status);
			if (urlToMediaUrls.size() > 0) {
				for (int u = 0; u < urlToMediaUrls.size(); u++) {
					String url = urlToMediaUrls.keyAt(u);
					HashSet<String> medialUrls = urlToMediaUrls.valueAt(u);
					StringBuilder sb = new StringBuilder();
					for (String mediaUrl : medialUrls) {
						if (sb.length() > 0) {
							sb.append(StringUtils.SPACE_CAR);
						}
						sb.append(getURL(mediaUrl, mediaUrl));
					}
					textHTML = textHTML.replace(url, sb.toString());
				}
			}
			textHTML = HtmlUtils.toHTML(textHTML);
			return textHTML;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while generating HTML text for status '%s'!", status);
			return status.text;
		}
	}

	@NonNull
	private SimpleArrayMap<String, HashSet<String>> getURLToMediaURLs(@NonNull Tweet status) {
		SimpleArrayMap<String, HashSet<String>> urlToMediaUrls = new SimpleArrayMap<>();
		if (status.entities.media != null) {
			for (MediaEntity mediaEntity : status.entities.media) {
				HashSet<String> mediaUrls = urlToMediaUrls.get(mediaEntity.url);
				if (mediaUrls == null) {
					mediaUrls = new HashSet<>();
				}
				mediaUrls.add(mediaEntity.mediaUrlHttps);
				urlToMediaUrls.put(mediaEntity.url, mediaUrls);
			}
		}
		if (status.extendedEntities.media != null) {
			for (MediaEntity mediaEntity : status.extendedEntities.media) {
				HashSet<String> mediaUrls = urlToMediaUrls.get(mediaEntity.url);
				if (mediaUrls == null) {
					mediaUrls = new HashSet<>();
				}
				mediaUrls.add(mediaEntity.mediaUrlHttps);
				urlToMediaUrls.put(mediaEntity.url, mediaUrls);
			}
		}
		return urlToMediaUrls;
	}

	private static final String HREF_URL_AND_URL_AND_TEXT = "<A HREF=\"%s\">%s</A>";

	@NonNull
	private String getURL(String url, String text) {
		return String.format(HREF_URL_AND_URL_AND_TEXT, url, text);
	}

	private static final String WEB_URL_AND_SCREEN_NAME_AND_ID = "https://twitter.com/%s/status/%s";

	@NonNull
	private String getNewsWebURL(@NonNull Tweet status) {
		if (status.user == null) {
			return StringUtils.EMPTY;
		}
		return getNewsWebURL(status, status.user);
	}

	@NonNull
	private String getNewsWebURL(@NonNull Tweet status, @NonNull User user) {
		return getNewsWebURL(status, user.screenName);
	}

	@NonNull
	private String getNewsWebURL(@NonNull Tweet status, @NonNull String userScreenName) {
		return String.format(WEB_URL_AND_SCREEN_NAME_AND_ID, userScreenName, status.getId()); // id or id_str ?
	}

	private static final String AUTHOR_PROFILE_URL_AND_SCREEN_NAME = "https://twitter.com/%s";

	@NonNull
	private String getAuthorProfileURL(@NonNull User user) {
		return getAuthorProfileURL(user.screenName);
	}

	@NonNull
	private String getAuthorProfileURL(String userScreenName) {
		return String.format(AUTHOR_PROFILE_URL_AND_SCREEN_NAME, userScreenName);
	}

	private static final String HASH_TAG_URL_AND_TAG = "https://twitter.com/hashtag/%s";

	private String getHashTagURL(String hashTag) {
		return String.format(HASH_TAG_URL_AND_TAG, hashTag);
	}

	private static class TwitterNewsDbHelper extends NewsProvider.NewsDbHelper {

		private static final String LOG_TAG = TwitterNewsDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link TwitterNewsDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "news_twitter.db";

		/**
		 * Override if multiple {@link TwitterNewsDbHelper} implementations in same app.
		 */
		static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pTwitterNewsLastUpdate";

		/**
		 * Override if multiple {@link TwitterNewsDbHelper} implementations in same app.
		 */
		static final String PREF_KEY_AGENCY_LAST_UPDATE_LANG = "pTwitterNewsLastUpdateLang";

		static final String T_TWITTER_NEWS = NewsProvider.NewsDbHelper.T_NEWS;

		private static final String T_TWITTER_NEWS_SQL_CREATE = NewsProvider.NewsDbHelper.getSqlCreateBuilder(T_TWITTER_NEWS).build();

		private static final String T_TWITTER_NEWS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_TWITTER_NEWS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link TwitterNewsDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.twitter_db_version);
			}
			return dbVersion;
		}

		private Context context;

		TwitterNewsDbHelper(@NonNull Context context) {
			this(context, DB_NAME, getDbVersion(context));
		}

		TwitterNewsDbHelper(@NonNull Context context, String dbName, int dbVersion) {
			super(context, dbName, dbVersion);
			this.context = context;
		}

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
			db.execSQL(T_TWITTER_NEWS_SQL_DROP);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L, true);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_LAST_UPDATE_LANG, StringUtils.EMPTY, true);
			initAllDbTables(db);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_TWITTER_NEWS_SQL_CREATE);
		}
	}
}
