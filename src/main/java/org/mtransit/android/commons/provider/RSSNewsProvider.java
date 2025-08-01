package org.mtransit.android.commons.provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SecurityUtils;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.StringExtKt;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.News;
import org.mtransit.android.commons.helpers.MTDefaultHandler;
import org.mtransit.android.commons.provider.news.NewsTextFormatter;
import org.mtransit.android.commons.provider.news.rss.RssNewProviderUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.CollectionUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

@SuppressLint("Registered")
public class RSSNewsProvider extends NewsProvider {

	private static final String LOG_TAG = RSSNewsProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = RSSNewsDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_LANG = RSSNewsDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_LANG;

	@Nullable
	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
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
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	@NonNull
	private static String getAUTHORITY(@NonNull Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.rss_authority);
		}
		return authority;
	}

	@Nullable
	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	@NonNull
	private static Uri getAUTHORITY_URI(@NonNull Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	@Nullable
	private static Boolean copyToFileInsteadOfStreaming = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	private static boolean isCOPY_TO_FILE_INSTEAD_OF_STREAMING(@NonNull Context context) {
		if (copyToFileInsteadOfStreaming == null) {
			copyToFileInsteadOfStreaming = context.getResources().getBoolean(R.bool.rss_copy_to_file_instead_of_streaming);
		}
		return copyToFileInsteadOfStreaming;
	}

	@Nullable
	private static Boolean useCustomSSLCertificate = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	private static boolean isUSE_CUSTOM_SSL_CERTIFICATE(@NonNull Context context) {
		if (useCustomSSLCertificate == null) {
			useCustomSSLCertificate = context.getResources().getBoolean(R.bool.rss_use_custom_ssl_certificate);
		}
		return useCustomSSLCertificate;
	}

	@Nullable
	private static java.util.List<String> fileCleaningRegex = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getFILE_CLEANING_REGEX(@NonNull Context context) {
		if (fileCleaningRegex == null) {
			fileCleaningRegex = Arrays.asList(context.getResources().getStringArray(R.array.rrs_file_cleaning_regex));
		}
		return fileCleaningRegex;
	}

	@Nullable
	private static java.util.List<String> fileCleaningReplacement = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getFILE_CLEANING_REPLACEMENT(@NonNull Context context) {
		if (fileCleaningReplacement == null) {
			fileCleaningReplacement = Arrays.asList(context.getResources().getStringArray(R.array.rrs_file_cleaning_replacement));
		}
		return fileCleaningReplacement;
	}

	@Nullable
	private static java.util.List<String> feeds = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getFEEDS(@NonNull Context context) {
		if (feeds == null) {
			feeds = Arrays.asList(context.getResources().getStringArray(R.array.rss_feeds));
		}
		return feeds;
	}

	@Nullable
	private static java.util.List<String> feedsAuthorIcon = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getFEEDS_AUTHOR_ICON(@NonNull Context context) {
		if (feedsAuthorIcon == null) {
			feedsAuthorIcon = Arrays.asList(context.getResources().getStringArray(R.array.rss_feeds_author_icon));
		}
		return feedsAuthorIcon;
	}

	@Nullable
	private static java.util.List<String> feedsAuthorUrl = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	@NonNull
	private static java.util.List<String> getFEEDS_AUTHOR_URL(@NonNull Context context) {
		if (feedsAuthorUrl == null) {
			feedsAuthorUrl = Arrays.asList(context.getResources().getStringArray(R.array.rss_feeds_author_url));
		}
		return feedsAuthorUrl;
	}

	@Nullable
	private static java.util.List<String> feedsLabel = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	// TODO remove rss_feeds_label after MTfull-description.txt.MT.sh migrated
	@NonNull
	private static java.util.List<String> getFEEDS_LABEL(@NonNull Context context) {
		if (feedsLabel == null) {
			feedsLabel = Arrays.asList(context.getResources().getStringArray(R.array.rss_feeds_label));
		}
		return feedsLabel;
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(requireContextCompat());
	}

	@Nullable
	private NewsDbHelper dbHelper;
	private static int currentDbVersion = -1;

	@NonNull
	private NewsDbHelper getDBHelper(@NonNull Context context) {
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
	 * Override if multiple {@link RSSNewsDbHelper} implementations in same app.
	 */
	@Override
	public int getCurrentDbVersion() {
		return RSSNewsDbHelper.getDbVersion(requireContextCompat());
	}

	/**
	 * Override if multiple {@link RSSNewsDbHelper} implementations in same app.
	 */
	@NonNull
	@Override
	public NewsDbHelper getNewDbHelper(@NonNull Context context) {
		return new RSSNewsDbHelper(context.getApplicationContext());
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

	private static final long NEWS_MAX_VALIDITY_IN_MS = MAX_CACHE_VALIDITY_MS;
	private static final long NEWS_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1L);
	private static final long NEWS_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.HOURS.toMillis(1L);
	private static final long NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(30L);
	private static final long NEWS_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10L);

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
	public boolean deleteCachedNews(@Nullable Integer newsId) {
		return NewsProvider.deleteCachedNews(this, newsId);
	}

	private static final String AGENCY_SOURCE_ID = "rss";

	@SuppressWarnings("UnusedReturnValue")
	private int deleteAllAgencyNewsData() {
		int affectedRows = 0;
		try {
			String selection = SqlUtils.getWhereEqualsString(Columns.T_NEWS_K_SOURCE_ID, AGENCY_SOURCE_ID);
			affectedRows = getWriteDB().delete(getNewsDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all agency news data!");
		}
		return affectedRows;
	}

	@NonNull
	@Override
	public String getAuthority() {
		return getAUTHORITY(requireContextCompat());
	}

	@NonNull
	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITY_URI(requireContextCompat());
	}

	@Override
	public void cacheNews(@NonNull ArrayList<News> newNews) {
		NewsProvider.cacheNewsS(this, newNews);
	}

	@Nullable
	@Override
	public ArrayList<News> getCachedNews(@NonNull Filter newsFilter) {
		return NewsProvider.getCachedNewsS(this, newsFilter);
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

	@Nullable
	@Override
	public ArrayList<News> getNewNews(@NonNull Filter newsFilter) {
		updateAgencyNewsDataIfRequired(requireContextCompat(), newsFilter.isInFocusOrDefault());
		return getCachedNews(newsFilter);
	}

	private void updateAgencyNewsDataIfRequired(@NonNull Context context, boolean inFocus) {
		final long lastUpdateInMs = PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L);
		final String lastUpdateLang = PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_LANG, StringUtils.EMPTY);
		final long minUpdateMs = Math.min(getNewsMaxValidityInMs(), getNewsValidityInMs(inFocus));
		final long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs
				&& LocaleUtils.getDefaultLanguage().equals(lastUpdateLang)) {
			return;
		}
		updateAgencyNewsDataIfRequiredSync(context, lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyNewsDataIfRequiredSync(@NonNull Context context, final long lastLastUpdateInMs, boolean inFocus) {
		final long lastUpdateInMs = PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L);
		final String lastUpdateLang = PreferenceUtils.getPrefLcl(context, PREF_KEY_AGENCY_LAST_UPDATE_LANG, StringUtils.EMPTY);
		if (lastUpdateInMs > lastLastUpdateInMs // IF new more recent last update DO
				&& LocaleUtils.getDefaultLanguage().equals(lastUpdateLang)) {
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		//noinspection RedundantIfStatement
		if (lastUpdateInMs + getNewsMaxValidityInMs() < nowInMs
				|| !LocaleUtils.getDefaultLanguage().equals(lastUpdateLang)) {
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
		if (newNews != null) { // empty is OK
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyNewsData();
			}
			cacheNews(newNews);
			PreferenceUtils.savePrefLclSync(context, PREF_KEY_AGENCY_LAST_UPDATE_MS, nowInMs);
			PreferenceUtils.savePrefLclSync(context, PREF_KEY_AGENCY_LAST_UPDATE_LANG, LocaleUtils.getDefaultLanguage());
		} // else keep whatever we have until max validity reached
	}

	@Nullable
	private ArrayList<News> loadAgencyNewsDataFromWWW(@NonNull Context context) {
		try {
			ArrayList<News> newNews = null;
			int i = 0;
			for (String urlString : getFEEDS(context)) {
				String language = RssNewProviderUtils.pickLang(context, i);
				if (!LocaleUtils.MULTIPLE.equals(language)
						&& !LocaleUtils.UNKNOWN.equals(language)
						&& !LocaleUtils.getDefaultLanguage().equals(language)) {
					i++;
					continue;
				}
				ArrayList<News> feedNews = loadAgencyNewsDataFromWWW(context, urlString, i++);
				if (feedNews != null) {
					if (newNews == null) {
						newNews = new ArrayList<>();
					}
					newNews.addAll(filterNews(feedNews));
				}
			}
			return newNews;
		} catch (Exception e) {
			MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception while parsing feeds");
			return null;
		}
	}

	private static final long MIN_COVERAGE_DURATION_IN_MS = TimeUnit.DAYS.toMillis(182L); // PAST
	private static final long MAX_COVERAGE_DURATION_IN_MS = TimeUnit.DAYS.toMillis(14L); // FUTURE

	private static final int MIN_SIZE_IN_THE_PAST = 10;

	@NonNull
	private ArrayList<News> filterNews(@NonNull ArrayList<News> feedNews) {
		CollectionUtils.sort(feedNews, News.NEWS_COMPARATOR);
		int nbKeptInList = 0;
		long minCoverageDateInMs = TimeUtils.currentTimeMillis() - MIN_COVERAGE_DURATION_IN_MS;
		long maxCoverageDateInMs = TimeUtils.currentTimeMillis() + MAX_COVERAGE_DURATION_IN_MS;
		final Iterator<News> it = feedNews.iterator();
		while (it.hasNext()) {
			final News news = it.next();
			if (news.getTargetUUID().isEmpty()) { // IF generic app news DO
				continue; // keep all
			}
			if (nbKeptInList > MIN_SIZE_IN_THE_PAST //
					&& news.getCreatedAtInMs() < minCoverageDateInMs) {
				it.remove(); // too old
			} else if (maxCoverageDateInMs < news.getCreatedAtInMs()) {
				it.remove(); // too far away in the future
			} else {
				nbKeptInList++;
			}
		}
		return feedNews;
	}

	private static final String PRIVATE_FILE_NAME = "rss.xml";

	@Nullable
	private ArrayList<News> loadAgencyNewsDataFromWWW(@NonNull Context context, String urlString, int i) {
		try {
			MTLog.i(this, "Loading from '%s'...", urlString);
			final String target = RssNewProviderUtils.pickTarget(context, i);
			if (target == null) {
				MTLog.w(this, "SKIP loading '%s': no target UUID!", urlString);
				return null;
			}
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			if (isUSE_CUSTOM_SSL_CERTIFICATE(context)) {
				SSLSocketFactory sslSocketFactory = SecurityUtils.getSSLSocketFactory(context, R.raw.rss_custom_ssl_certificate);
				if (sslSocketFactory != null) {
					((HttpsURLConnection) httpUrlConnection).setSSLSocketFactory(sslSocketFactory);
				}
			}
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				String authority = getAUTHORITY(context);
				final int severity = RssNewProviderUtils.pickSeverity(context, i);
				final long noteworthyInMs = RssNewProviderUtils.pickNoteworthy(context, i);
				long maxValidityInMs = getNewsMaxValidityInMs();
				final String color = RssNewProviderUtils.pickColor(context, i);
				String authorIcon = CollectionUtils.getOrNull(getFEEDS_AUTHOR_ICON(context), i);
				final String authorName = RssNewProviderUtils.pickAuthorName(context, i);
				String authorUrl = getFEEDS_AUTHOR_URL(context).get(i);
				String label = RssNewProviderUtils.pickLabel(url);
				if (TextUtils.isEmpty(label)) { // nice to have
					label = getFEEDS_LABEL(context).get(i);
				}
				final String language = RssNewProviderUtils.pickLang(context, i);
				final boolean ignoreGUID = RssNewProviderUtils.pickIgnoreGUID(context, i);
				final boolean ignoreLink = RssNewProviderUtils.pickIgnoreLink(context, i);
				RSSDataHandler handler = new RSSDataHandler(
						httpUrlConnection.getURL(),
						authority,
						severity,
						noteworthyInMs,
						newLastUpdateInMs,
						maxValidityInMs,
						target,
						color,
						authorIcon,
						authorName,
						authorUrl,
						label,
						language,
						ignoreGUID,
						ignoreLink
				);
				xr.setContentHandler(handler);
				if (isCOPY_TO_FILE_INSTEAD_OF_STREAMING(context)) { // fix leading space (invalid!) #BIXI #Montreal
					FileUtils.copyToPrivateFile(context, PRIVATE_FILE_NAME, urlc.getInputStream(), RssNewProviderUtils.pickEncoding(context));
					cleaningFile(context);
					xr.parse(new InputSource(context.openFileInput(PRIVATE_FILE_NAME)));
				} else {
					xr.parse(new InputSource(httpUrlConnection.getInputStream()));
				}
				final ArrayList<News> loadedNews = handler.getNews();
				MTLog.i(this, "Loaded %d news.", loadedNews.size());
				return loadedNews;
			default:
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpUrlConnection.getResponseCode(),
						httpUrlConnection.getResponseMessage());
				return null;
			}
		} catch (SSLHandshakeException sslhe) {
			MTLog.w(this, sslhe, "SSL error while parsing '%s'!", urlString);
			SecurityUtils.logCertPathValidatorException(sslhe);
			return null;
		} catch (UnknownHostException uhe) {
			if (MTLog.isLoggable(android.util.Log.DEBUG)) {
				MTLog.w(this, uhe, "No Internet Connection while parsing '%s'!", urlString);
			} else {
				MTLog.w(this, "No Internet Connection while parsing '%s'!", urlString);
			}
			return null;
		} catch (SocketException se) {
			MTLog.w(LOG_TAG, se, "No Internet Connection while parsing '%s'!", urlString);
			return null;
		} catch (Exception e) {
			MTLog.e(LOG_TAG, e, "INTERNAL ERROR: Unknown Exception while parsing '%s'!", urlString);
			return null;
		}
	}

	private void cleaningFile(@NonNull Context context) {
		try {
			java.util.List<String> fileCleaningRegex = getFILE_CLEANING_REGEX(context);
			int regexCount = fileCleaningRegex.size();
			java.util.List<String> fileCleaningReplacement = getFILE_CLEANING_REPLACEMENT(context);
			int replacementCount = fileCleaningReplacement.size();
			if (regexCount > 0 && replacementCount > 0) {
				if (regexCount != replacementCount) {
					MTLog.w(this, "Invalid number for file cleaning regex [%s] & replacement [%s] !", regexCount, replacementCount);
					return;
				}
				String fileContent = FileUtils.getString(context.openFileInput(PRIVATE_FILE_NAME));
				for (int i = 0; i < regexCount; i++) {
					String regex = fileCleaningRegex.get(i);
					if (TextUtils.isEmpty(regex)) {
						MTLog.w(this, "Invalid file cleaning regex! (%s)", regex);
						continue;
					}
					String replacement = fileCleaningReplacement.get(i);
					fileContent = fileContent.replaceAll(regex, replacement);
				}
				FileUtils.copyToPrivateFile(context, PRIVATE_FILE_NAME, fileContent);
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while cleaning file!");
		}
	}

	private static class RSSDataHandler extends MTDefaultHandler {

		private static final String LOG_TAG = RSSNewsProvider.LOG_TAG + ">" + RSSDataHandler.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		private static final String RSS = "rss";
		private static final String RDF = "RDF";
		private static final String CHANNEL = "channel";
		private static final String TITLE = "title";
		private static final String LINK = "link";
		private static final String LI = "li";
		private static final String ITEMS = "items";
		private static final String SEQ = "Seq";
		private static final String DESCRIPTION = "description";
		private static final String LAST_BUILD_DATE = "lastBuildDate";
		private static final String DOCS = "docs";
		private static final String GENERATOR = "generator";
		private static final String ITEM = "item";
		private static final String PUBLICATION_DATE = "pubDate";
		private static final String DATE = "date";
		private static final String UPDATED = "updated";
		private static final String COMMENTS = "comments";
		private static final String COMMENT_RSS = "commentRss";
		private static final String ENCODED = "encoded";
		private static final String CONTENT_ENCODED = "content:encoded";
		private static final String GUID = "guid";
		private static final String GUID_IS_PERMA_LINK = "isPermaLink";
		private static final String CREATOR = "creator";
		private static final String CATEGORY = "category";
		private static final String LANGUAGE = "language";
		private static final String UPDATE_PERIOD = "updatePeriod";
		private static final String UPDATE_FREQUENCY = "updateFrequency";
		private static final String MANAGING_EDITOR = "managingEditor";
		private static final String WEB_MASTER = "webMaster";
		private static final String AUTHOR = "author";
		private static final String IMAGE = "image";
		private static final String URL = "url";
		private static final String WIDTH = "width";
		private static final String HEIGHT = "height";
		private static final String COPYRIGHT = "copyright";
		private static final String TTL = "ttl";

		private String currentLocalName = RSS;
		private String currentQName = "";
		private boolean currentItem = false;
		@NonNull
		private final StringBuilder currentPubDateSb = new StringBuilder();
		@NonNull
		private final StringBuilder currentDateSb = new StringBuilder();
		@NonNull
		private final StringBuilder currentUpdatedSb = new StringBuilder();
		@NonNull
		private final StringBuilder currentTitleSb = new StringBuilder();
		@NonNull
		private final StringBuilder currentLinkSb = new StringBuilder();
		@NonNull
		private final StringBuilder currentDescriptionSb = new StringBuilder();
		@NonNull
		private final StringBuilder currentContentEncodedSb = new StringBuilder();
		@NonNull
		private final StringBuilder currentGUIDSb = new StringBuilder();
		@Nullable
		private Boolean currentGUIDIsPermaLink = null;

		@NonNull
		private final ArrayList<News> news = new ArrayList<>();

		private final URL fromURL;

		private final String authority;
		private final int severity;
		private final long noteworthyInMs;
		private final long lastUpdateInMs;
		private final long maxValidityInMs;
		private final String target;
		private final String color;
		@Nullable
		private final String authorIcon;
		private final String authorName;
		private final String authorUrl;
		private final String label;
		private final String language;
		private final boolean ignoreGuid;
		private final boolean ignoreLink;

		private long lastItemPublicationDateInMs;

		private static final TimeZone CA_BC_TZ = TimeZone.getTimeZone("America/Vancouver");

		@NonNull
		private Calendar getNewBeginningOfTodayCal() {
			Calendar beginningOfTodayCal = Calendar.getInstance(CA_BC_TZ);
			beginningOfTodayCal.set(Calendar.HOUR_OF_DAY, 0);
			beginningOfTodayCal.set(Calendar.MINUTE, 0);
			beginningOfTodayCal.set(Calendar.SECOND, 0);
			beginningOfTodayCal.set(Calendar.MILLISECOND, 0);
			return beginningOfTodayCal;
		}

		RSSDataHandler(URL fromURL,
					   String authority,
					   int severity,
					   long noteworthyInMs,
					   long lastUpdateInMs,
					   long maxValidityInMs,
					   String target,
					   String color,
					   @Nullable String authorIcon,
					   String authorName,
					   String authorUrl,
					   String label,
					   String language,
					   boolean ignoreGuid,
					   boolean ignoreLink) {
			this.fromURL = fromURL;
			this.authority = authority;
			this.severity = severity;
			this.noteworthyInMs = noteworthyInMs;
			this.lastUpdateInMs = lastUpdateInMs;
			this.maxValidityInMs = maxValidityInMs;
			this.target = target;
			this.color = color;
			this.authorIcon = authorIcon;
			this.authorName = authorName;
			this.authorUrl = authorUrl;
			this.label = label;
			this.language = language;
			this.ignoreGuid = ignoreGuid;
			this.ignoreLink = ignoreLink;
			this.lastItemPublicationDateInMs = getNewBeginningOfTodayCal().getTimeInMillis() - TimeUnit.DAYS.toMillis(1L);
		}

		@NonNull
		public ArrayList<News> getNews() {
			return this.news;
		}

		private static final String FALSE = "false";

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			this.currentLocalName = localName;
			this.currentQName = qName;
			if (ITEM.equals(this.currentLocalName)) {
				this.currentItem = true;
				this.currentTitleSb.setLength(0); // reset
				this.currentPubDateSb.setLength(0); // reset
				this.currentDateSb.setLength(0); // reset
				this.currentUpdatedSb.setLength(0); // reset
				this.currentLinkSb.setLength(0); // reset
				this.currentDescriptionSb.setLength(0); // reset
				this.currentContentEncodedSb.setLength(0); // reset
				this.currentGUIDSb.setLength(0); // reset
				this.currentGUIDIsPermaLink = null; // reset
			} else if (GUID.equals(this.currentLocalName)) {
				this.currentGUIDIsPermaLink = !FALSE.equals(attributes.getValue(GUID_IS_PERMA_LINK)); // true = default
			}
		}

		@SuppressWarnings("StatementWithEmptyBody")
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
					} else if (PUBLICATION_DATE.equals(this.currentLocalName)) {
						this.currentPubDateSb.append(string);
					} else if (DATE.equals(this.currentLocalName)) {
						this.currentDateSb.append(string);
					} else if (UPDATED.equals(this.currentLocalName)) {
						this.currentUpdatedSb.append(string);
					} else if (LINK.equals(this.currentLocalName)) {
						if (!this.ignoreLink) {
							this.currentLinkSb.append(string);
						}
					} else if (DESCRIPTION.equals(this.currentLocalName)) {
						this.currentDescriptionSb.append(string);
					} else if (GUID.equals(this.currentLocalName)) {
						if (!this.ignoreGuid) {
							this.currentGUIDSb.append(string);
						}
					} else if (ITEM.equals(this.currentLocalName)) { // ignore
					} else if (COMMENTS.equals(this.currentLocalName)) { // ignore
					} else if (COMMENT_RSS.equals(this.currentLocalName)) { // ignore
					} else if (ENCODED.equals(this.currentLocalName)) {
						if (CONTENT_ENCODED.equals(this.currentQName)) {
							this.currentContentEncodedSb.append(string);
						} // ELSE ignore
					} else if (CATEGORY.equals(this.currentLocalName)) { // ignore
					} else if (CREATOR.equals(this.currentLocalName)) { // ignore
					} else if (AUTHOR.equals(this.currentLocalName)) { // ignore
					} else {
						MTLog.d(this, "characters() > Unexpected item element '%s'", this.currentLocalName);
					}
				} else if (RDF.equals(this.currentLocalName)) { // ignore
				} else if (TITLE.equals(this.currentLocalName)) { // ignore
				} else if (PUBLICATION_DATE.equals(this.currentLocalName)) { // ignore
				} else if (DATE.equals(this.currentLocalName)) { // ignore
				} else if (LINK.equals(this.currentLocalName)) { // ignore
				} else if (LI.equals(this.currentLocalName)) { // ignore
				} else if (ITEMS.equals(this.currentLocalName)) { // ignore
				} else if (SEQ.equals(this.currentLocalName)) { // ignore
				} else if (DESCRIPTION.equals(this.currentLocalName)) { // ignore
				} else if (RSS.equals(this.currentLocalName)) { // ignore
				} else if (CHANNEL.equals(this.currentLocalName)) { // ignore
				} else if (LAST_BUILD_DATE.equals(this.currentLocalName)) { // ignore
				} else if (DOCS.equals(this.currentLocalName)) { // ignore
				} else if (GENERATOR.equals(this.currentLocalName)) { // ignore
				} else if (COMMENTS.equals(this.currentLocalName)) { // ignore
				} else if (LANGUAGE.equals(this.currentLocalName)) { // ignore
				} else if (UPDATE_PERIOD.equals(this.currentLocalName)) { // ignore
				} else if (UPDATE_FREQUENCY.equals(this.currentLocalName)) { // ignore
				} else if (MANAGING_EDITOR.equals(this.currentLocalName)) { // ignore
				} else if (WEB_MASTER.equals(this.currentLocalName)) { // ignore
				} else if (IMAGE.equals(this.currentLocalName)) { // ignore
				} else if (URL.equals(this.currentLocalName)) { // ignore
				} else if (WIDTH.equals(this.currentLocalName)) { // ignore
				} else if (HEIGHT.equals(this.currentLocalName)) { // ignore
				} else if (GUID.equals(this.currentLocalName)) { // ignore
				} else if (COPYRIGHT.equals(this.currentLocalName)) { // ignore
				} else if (TTL.equals(this.currentLocalName)) { // ignore
				} else {
					MTLog.d(this, "characters() > Unexpected element '%s'", this.currentLocalName);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing '%s' value '%s, %s, %s'!", this.currentLocalName, ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			try {
				if (ITEM.equals(localName)) {
					processItem();
					this.currentItem = false;
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing '%s' end element!", this.currentLocalName);
			}
		}

		private static final String COLON = ": ";

		private void processItem() {
			long pubDateInMs = getPublicationDateInMs();
			String uuid = getUUID(pubDateInMs);
			if (uuid == null) {
				return;
			}
			final Locale locale = Locale.FRENCH.getLanguage().equals(this.language) ? Locale.FRENCH : Locale.ENGLISH;
			final String title = StringExtKt.capitalize( // 1st character only
					CleanUtils.toLowerCaseUpperCaseWords( // some title ALL UPPER CASE!
							locale,
							this.currentTitleSb.toString().trim()
					),
					locale
			);
			String description = this.currentDescriptionSb.toString().trim();
			final String contentEncoded = this.currentContentEncodedSb.toString().trim();
			if (!contentEncoded.isEmpty()) {
				description = contentEncoded;
			}
			String link = this.currentLinkSb.toString().trim();
			StringBuilder textSb = new StringBuilder();
			StringBuilder textHTMLSb = new StringBuilder();
			if (!TextUtils.isEmpty(title)) {
				textSb.append(title);
				textHTMLSb.append(NewsTextFormatter.formatHTMLTitle(title));
			}
			if (!TextUtils.isEmpty(description)) {
				if (textSb.length() > 0) {
					textSb.append(COLON);
				}
				String textHTML = description;
				textHTML = HtmlUtils.toHTML(textHTML); // replace /n by <br />
				if (REMOVE_IMAGE_FROM_TEXT) {
					textHTML = HtmlUtils.removeImg(textHTML); // remove <img /> tags & image URLs
				} else { // keep image URLs
					textHTML = HtmlUtils.replaceImgTagWithUrlLink(this.fromURL, textHTML); // replace <img /> tags w/ image URLs
				}
				textHTML = HtmlUtils.removeStyle(textHTML);
				textHTML = HtmlUtils.removeScript(textHTML);
				textHTML = HtmlUtils.removeComments(textHTML);
				textHTML = HtmlUtils.fixTextViewBR(textHTML);
				textSb.append(HtmlUtils.fromHtmlCompact(textHTML));
				textHTMLSb.append(NewsTextFormatter.getHTMLAfterTitleSpace(textHTMLSb.length()));
				textHTMLSb.append(textHTML);
			}
			if (!TextUtils.isEmpty(link)) {
				if (textHTMLSb.length() > 0) {
					textHTMLSb.append(HtmlUtils.BR).append(HtmlUtils.BR);
				}
				textHTMLSb.append(HtmlUtils.linkify(link));
			}
			if (textSb.length() == 0 || textHTMLSb.length() == 0) {
				return;
			}
			List<String> imageUrls = HtmlUtils.fromHtmlUrls(
					HtmlUtils.extractImagesUrls(this.fromURL, description)
			);
			final News newNews = new News(
					null,
					this.authority,
					uuid,
					this.severity,
					this.noteworthyInMs,
					this.lastUpdateInMs,
					this.maxValidityInMs,
					pubDateInMs,
					this.target,
					this.color,
					this.authorName,
					null,
					this.authorIcon,
					this.authorUrl,
					StringUtils.oneLineOneSpace(textSb.toString()),
					textHTMLSb.toString(),
					link,
					this.language,
					AGENCY_SOURCE_ID,
					this.label,
					imageUrls
			);
			this.news.add(newNews);
		}

		private static final Pattern CONVERT_URL_TO_ID = Pattern.compile("[/.:]", Pattern.CASE_INSENSITIVE);
		private static final String CONVERT_URL_TO_ID_REPLACEMENT = "_";

		private String getUUID(Long pubDateInMs) {
			String guid = this.currentGUIDSb.toString().trim();
			if (!guid.isEmpty()) {
				if (this.currentGUIDIsPermaLink != null && !this.currentGUIDIsPermaLink) { // not URL
					return AGENCY_SOURCE_ID + guid;
				}
				guid = CONVERT_URL_TO_ID.matcher(guid).replaceAll(CONVERT_URL_TO_ID_REPLACEMENT);
				if (!TextUtils.isEmpty(guid)) {
					return AGENCY_SOURCE_ID + guid;
				}
			}
			String link = this.currentLinkSb.toString().trim();
			if (!link.isEmpty()) {
				link = CONVERT_URL_TO_ID.matcher(link).replaceAll(CONVERT_URL_TO_ID_REPLACEMENT);
				if (!TextUtils.isEmpty(link)) {
					return AGENCY_SOURCE_ID + link;
				}
			}
			if (pubDateInMs != null) {
				return AGENCY_SOURCE_ID + pubDateInMs;
			}
			MTLog.w(this, "getUUID() > can't find UUID! (GUID: %s, LINK: %s, DATE: %s)", this.currentGUIDSb, this.currentLinkSb, null);
			return null;
		}

		private static final ThreadSafeDateFormatter RSS_PUB_DATE_FORMATTER_X = new ThreadSafeDateFormatter("EEE, dd MMM yyyy HH:mm:ss XXX", Locale.ENGLISH);
		private static final ThreadSafeDateFormatter RSS_PUB_DATE_FORMATTER = new ThreadSafeDateFormatter("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
		private static final ThreadSafeDateFormatter ATOM_UPDATED_FORMATTER = new ThreadSafeDateFormatter("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.ENGLISH);
		private static final ThreadSafeDateFormatter DC_DATE_FORMATTER = new ThreadSafeDateFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.ENGLISH);

		private long getPublicationDateInMs() {
			try {
				if (this.currentPubDateSb.length() > 0) {
					final Date date = RSS_PUB_DATE_FORMATTER_X.parseThreadSafe(this.currentPubDateSb.toString().trim());
					if (date != null) {
						return date.getTime();
					}
				}
			} catch (Exception e) {
				MTLog.d(this, "Error while parsing pub date '%s' with '%s'!", this.currentPubDateSb, RSS_PUB_DATE_FORMATTER_X.toPattern());
			}
			try {
				if (this.currentPubDateSb.length() > 0) {
					final Date date = RSS_PUB_DATE_FORMATTER.parseThreadSafe(this.currentPubDateSb.toString().trim());
					if (date != null) {
						return date.getTime();
					}
				}
			} catch (Exception e) {
				MTLog.d(this, "Error while parsing pub date '%s' with '%s'!", this.currentPubDateSb, RSS_PUB_DATE_FORMATTER.toPattern());
			}
			try {
				if (this.currentUpdatedSb.length() > 0) {
					final Date date = ATOM_UPDATED_FORMATTER.parseThreadSafe(this.currentUpdatedSb.toString().trim());
					if (date != null) {
						return date.getTime();
					}
				}
			} catch (Exception e) {
				MTLog.d(this, "Error while parsing pub date '%s' with '%s'!", this.currentPubDateSb, ATOM_UPDATED_FORMATTER.toPattern());
			}

			try {
				if (this.currentDateSb.length() > 0) {
					final Date date = DC_DATE_FORMATTER.parseThreadSafe(this.currentDateSb.toString().trim());
					if (date != null) {
						return date.getTime();
					}
				}
			} catch (Exception e) {
				MTLog.d(this, "Error while parsing pub date '%s' with '%s'!", this.currentPubDateSb, DC_DATE_FORMATTER.toPattern());
			}
			MTLog.w(this, "Created fake date for news item!");
			this.lastItemPublicationDateInMs = this.lastItemPublicationDateInMs - TimeUnit.HOURS.toMillis(1L);
			return this.lastItemPublicationDateInMs;
		}
	}

	private static class RSSNewsDbHelper extends NewsProvider.NewsDbHelper {

		private static final String LOG_TAG = RSSNewsDbHelper.class.getSimpleName();

		@NonNull
		@Override
		public String getLogTag() {
			return LOG_TAG;
		}

		/**
		 * Override if multiple {@link RSSNewsDbHelper} implementations in same app.
		 */
		static final String DB_NAME = "rss.db";

		/**
		 * Override if multiple {@link RSSNewsDbHelper} implementations in same app.
		 */
		static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pRSSNewsLastUpdate";

		/**
		 * Override if multiple {@link RSSNewsDbHelper} implementations in same app.
		 */
		static final String PREF_KEY_AGENCY_LAST_UPDATE_LANG = "pRSSNewsLastUpdateLang";

		static final String T_RSS_NEWS = NewsProvider.NewsDbHelper.T_NEWS;

		private static final String T_RSS_NEWS_SQL_CREATE = NewsProvider.NewsDbHelper.getSqlCreateBuilder(T_RSS_NEWS).build();

		private static final String T_RSS_NEWS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_RSS_NEWS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link RSSNewsDbHelper} in same app.
		 */
		public static int getDbVersion(@NonNull Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.rss_db_version);
				dbVersion++; // add news articles images URLs do DB -> FORCE DB update
			}
			return dbVersion;
		}

		private final Context context;

		RSSNewsDbHelper(@NonNull Context context) {
			this(context, DB_NAME, getDbVersion(context));
		}

		RSSNewsDbHelper(@NonNull Context context, String dbName, int dbVersion) {
			super(context, dbName, dbVersion);
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
			db.execSQL(T_RSS_NEWS_SQL_DROP);
			PreferenceUtils.savePrefLclSync(this.context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0L);
			PreferenceUtils.savePrefLclSync(this.context, PREF_KEY_AGENCY_LAST_UPDATE_LANG, StringUtils.EMPTY);
			initAllDbTables(db);
		}

		private void initAllDbTables(@NonNull SQLiteDatabase db) {
			db.execSQL(T_RSS_NEWS_SQL_CREATE);
		}
	}
}
