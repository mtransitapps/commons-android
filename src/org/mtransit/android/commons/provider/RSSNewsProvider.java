package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mtransit.android.commons.ArrayUtils;
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
import org.mtransit.android.commons.helpers.MTDefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;

@SuppressLint("Registered")
public class RSSNewsProvider extends NewsProvider {

	private static final String TAG = RSSNewsProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = RSSNewsDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS;

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.rss_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String targetAuthority = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static String getTARGET_AUTHORITY(Context context) {
		if (targetAuthority == null) {
			targetAuthority = context.getResources().getString(R.string.rss_target_for_poi_authority);
		}
		return targetAuthority;
	}

	private static String color = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static String getCOLOR(Context context) {
		if (color == null) {
			color = context.getResources().getString(R.string.rss_color);
		}
		return color;
	}

	private static java.util.List<String> feeds = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getFEEDS(Context context) {
		if (feeds == null) {
			feeds = Arrays.asList(context.getResources().getStringArray(R.array.rss_feeds));
		}
		return feeds;
	}

	private static java.util.List<String> feedsAuthorName = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getFEEDS_AUTHOR_NAME(Context context) {
		if (feedsAuthorName == null) {
			feedsAuthorName = Arrays.asList(context.getResources().getStringArray(R.array.rss_feeds_author_name));
		}
		return feedsAuthorName;
	}

	private static java.util.List<String> feedsAuthorUrl = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getFEEDS_AUTHOR_URL(Context context) {
		if (feedsAuthorUrl == null) {
			feedsAuthorUrl = Arrays.asList(context.getResources().getStringArray(R.array.rss_feeds_author_url));
		}
		return feedsAuthorUrl;
	}

	private static java.util.List<String> feedsLabel = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getFEEDS_LABEL(Context context) {
		if (feedsLabel == null) {
			feedsLabel = Arrays.asList(context.getResources().getStringArray(R.array.rss_feeds_label));
		}
		return feedsLabel;
	}

	private static java.util.List<String> feedsLang = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getFEEDS_LANG(Context context) {
		if (feedsLang == null) {
			feedsLang = Arrays.asList(context.getResources().getStringArray(R.array.rss_feeds_lang));
		}
		return feedsLang;
	}

	private static java.util.List<String> feedsColors = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getFEEDS_COLORS(Context context) {
		if (feedsColors == null) {
			feedsColors = Arrays.asList(context.getResources().getStringArray(R.array.rss_feeds_colors));
		}
		return feedsColors;
	}

	private static java.util.List<String> feedsTargets = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static java.util.List<String> getFEEDS_TARGETS(Context context) {
		if (feedsTargets == null) {
			feedsTargets = Arrays.asList(context.getResources().getStringArray(R.array.rss_feeds_target));
		}
		return feedsTargets;
	}

	private static java.util.List<Integer> feedsSeverity = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static java.util.List<Integer> getFEEDS_SEVERITY(Context context) {
		if (feedsSeverity == null) {
			feedsSeverity = ArrayUtils.asIntegerList(context.getResources().getIntArray(R.array.rss_feeds_severity));
		}
		return feedsSeverity;
	}

	private static java.util.List<Long> feedsNoteworthy = null;

	/**
	 * Override if multiple {@link RSSNewsProvider} implementations in same app.
	 */
	public static java.util.List<Long> getFEEDS_NOTEWORTHY(Context context) {
		if (feedsNoteworthy == null) {
			feedsNoteworthy = ArrayUtils.asLongList(context.getResources().getStringArray(R.array.rss_feeds_noteworthy));
		}
		return feedsNoteworthy;
	}

	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	private static RSSNewsDbHelper dbHelper;
	private static int currentDbVersion = -1;

	private RSSNewsDbHelper getDBHelper(Context context) {
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
	public int getCurrentDbVersion() {
		return RSSNewsDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link RSSNewsDbHelper} implementations in same app.
	 */
	public RSSNewsDbHelper getNewDbHelper(Context context) {
		return new RSSNewsDbHelper(context.getApplicationContext());
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

	private static final String AGENCY_SOURCE_ID = "rss";

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
			MTLog.w(this, "getNewNews() > no new service update (filter null)");
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
			for (String urlString : getFEEDS(getContext())) {
				newNews.addAll(loadAgencyNewsDataFromWWW(urlString, i++));
			}
			return newNews;
		} catch (Exception e) {
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private ArrayList<News> loadAgencyNewsDataFromWWW(String urlString, int i) {
		try {
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				String authority = getAUTHORITY(getContext());
				int severity = getFEEDS_SEVERITY(getContext()).get(i);
				long noteworthyInMs = getFEEDS_NOTEWORTHY(getContext()).get(i);
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				long maxValidityInMs = getNewsMaxValidityInMs();
				String target = getFEEDS_TARGETS(getContext()).get(i);
				String color = getFEEDS_COLORS(getContext()).get(i);
				String authorName = getFEEDS_AUTHOR_NAME(getContext()).get(i);
				String authorUrl = getFEEDS_AUTHOR_URL(getContext()).get(i);
				String label = getFEEDS_LABEL(getContext()).get(i);
				String language = getFEEDS_LANG(getContext()).get(i);
				RSSDataHandler handler = new RSSDataHandler(authority, severity, noteworthyInMs, newLastUpdateInMs, maxValidityInMs, target, color, authorName,
						authorUrl, label, language);
				xr.setContentHandler(handler);
				xr.parse(new InputSource(httpUrlConnection.getInputStream()));
				return handler.getNews();
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

	private static class RSSDataHandler extends MTDefaultHandler {

		private static final String TAG = RSSNewsProvider.TAG + ">" + RSSDataHandler.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		private static final String RSS = "rss";
		private static final String CHANNEL = "channel";
		private static final String TITLE = "title";
		private static final String LINK = "link";
		private static final String DESCRIPTION = "description";
		private static final String LAST_BUILD_DATE = "lastBuildDate";
		private static final String DOCS = "docs";
		private static final String GENERATOR = "generator";
		private static final String ITEM = "item";
		private static final String PUBLICATION_DATE = "pubDate";
		private static final String UPDATED = "updated";
		private static final String COMMENTS = "comments";
		private static final String COMMENT_RSS = "commentRss";
		private static final String ENCODED = "encoded";
		private static final String GUID = "guid";
		private static final String CREATOR = "creator";
		private static final String CATEGORY = "category";
		private static final String LANGUAGE = "language";
		private static final String UPDATE_PERIOD = "updatePeriod";
		private static final String UPDATE_FREQUENCY = "updateFrequency";

		private String currentLocalName = RSS;
		private boolean currentItem = false;
		private StringBuilder currentPubDateSb = new StringBuilder();
		private StringBuilder currentUpdatedSb = new StringBuilder();
		private StringBuilder currentTitleSb = new StringBuilder();
		private StringBuilder currentLinkSb = new StringBuilder();
		private StringBuilder currentDescriptionSb = new StringBuilder();
		private StringBuilder currentGUIDSb = new StringBuilder();

		private ArrayList<News> news = new ArrayList<News>();

		private String authority;
		private int severity;
		private long noteworthyInMs;
		private long lastUpdateInMs;
		private long maxValidityInMs;
		private String target;
		private String color;
		private String authorName;
		private String authorUrl;
		private String label;
		private String language;

		public RSSDataHandler(String authority, int severity, long noteworthyInMs, long lastUpdateInMs, long maxValidityInMs, String target, String color,
				String authorName, String authorUrl, String label, String language) {
			this.authority = authority;
			this.severity = severity;
			this.noteworthyInMs = noteworthyInMs;
			this.lastUpdateInMs = lastUpdateInMs;
			this.maxValidityInMs = maxValidityInMs;
			this.target = target;
			this.color = color;
			this.authorName = authorName;
			this.authorUrl = authorUrl;
			this.label = label;
			this.language = language;
		}

		public ArrayList<News> getNews() {
			return this.news;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			this.currentLocalName = localName;
			if (ITEM.equals(this.currentLocalName)) {
				this.currentItem = true;
				this.currentTitleSb.setLength(0); // reset
				this.currentPubDateSb.setLength(0); // reset
				this.currentUpdatedSb.setLength(0); // reset
				this.currentLinkSb.setLength(0); // reset
				this.currentDescriptionSb.setLength(0); // reset
				this.currentGUIDSb.setLength(0); // reset
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
					} else if (PUBLICATION_DATE.equals(this.currentLocalName)) {
						this.currentPubDateSb.append(string);
					} else if (UPDATED.equals(this.currentLocalName)) {
						this.currentUpdatedSb.append(string);
					} else if (LINK.equals(this.currentLocalName)) {
						this.currentLinkSb.append(string);
					} else if (DESCRIPTION.equals(this.currentLocalName)) {
						this.currentDescriptionSb.append(string);
					} else if (GUID.equals(this.currentLocalName)) {
						this.currentGUIDSb.append(string);
					} else if (ITEM.equals(this.currentLocalName)) { // ignore
					} else if (COMMENTS.equals(this.currentLocalName)) { // ignore
					} else if (COMMENT_RSS.equals(this.currentLocalName)) { // ignore
					} else if (ENCODED.equals(this.currentLocalName)) { // ignore
					} else if (CATEGORY.equals(this.currentLocalName)) { // ignore
					} else if (CREATOR.equals(this.currentLocalName)) { // ignore
					} else {
						MTLog.w(this, "characters() > Unexpected item element '%s'", this.currentLocalName);
					}
				} else if (TITLE.equals(this.currentLocalName)) { // ignore
				} else if (PUBLICATION_DATE.equals(this.currentLocalName)) { // ignore
				} else if (LINK.equals(this.currentLocalName)) { // ignore
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
				} else {
					MTLog.w(this, "characters() > Unexpected element '%s'", this.currentLocalName);
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

		private void processItem() throws ParseException {
			Long pubDateInMs = getPublicationDateInMs();
			if (pubDateInMs == null) {
				return;
			}
			String uuid = getUUID();
			if (uuid == null) {
				return;
			}
			String title = this.currentTitleSb.toString().trim();
			String desc = this.currentDescriptionSb.toString().trim();
			String link = this.currentLinkSb.toString().trim();
			String text = title + COLON + Html.fromHtml(desc);
			String textHTML = HtmlUtils.applyBold(title) + HtmlUtils.BR + desc + HtmlUtils.BR + HtmlUtils.BR + HtmlUtils.linkify(link);
			this.news.add(new News(null, this.authority, uuid, this.severity, this.noteworthyInMs, this.lastUpdateInMs, this.maxValidityInMs, pubDateInMs,
					this.target, this.color, this.authorName, null, null, this.authorUrl, text, textHTML, link, this.language, AGENCY_SOURCE_ID, this.label));
		}

		private static final String CONVERT_URL_TO_ID = "\\/|\\.|\\:";
		private static final String CONVERT_URL_TO_ID_REPLACEMENT = "_";

		private String getUUID() {
			if (this.currentGUIDSb.length() > 0) {
				return AGENCY_SOURCE_ID + this.currentGUIDSb.toString().trim();
			}
			if (this.currentLinkSb.length() > 0) {
				return AGENCY_SOURCE_ID + this.currentLinkSb.toString().trim().replaceAll(CONVERT_URL_TO_ID, CONVERT_URL_TO_ID_REPLACEMENT);
			}
			return null;
		}

		private static final ThreadSafeDateFormatter RSS_PUB_DATE_FORMATTER = new ThreadSafeDateFormatter("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
		private static final ThreadSafeDateFormatter ATOM_UPDATED_FORMATTER = new ThreadSafeDateFormatter("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.ENGLISH);
		private Long getPublicationDateInMs() throws ParseException {
			if (this.currentUpdatedSb.length() > 0) {
				return ATOM_UPDATED_FORMATTER.parseThreadSafe(this.currentUpdatedSb.toString().trim()).getTime();
			}
			if (this.currentPubDateSb.length() > 0) {
				return RSS_PUB_DATE_FORMATTER.parseThreadSafe(this.currentPubDateSb.toString().trim()).getTime();
			}
			return null;
		}
	}

	private static class RSSNewsDbHelper extends NewsProvider.NewsDbHelper {

		private static final String TAG = RSSNewsDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link RSSNewsDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "rss.db";

		/**
		 * Override if multiple {@link RSSNewsDbHelper} implementations in same app.
		 */
		protected static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pRSSNewsLastUpdate";

		public static final String T_RSS_NEWS = NewsProvider.NewsDbHelper.T_NEWS;

		private static final String T_RSS_NEWS_SQL_CREATE = NewsProvider.NewsDbHelper.getSqlCreateBuilder(T_RSS_NEWS).build();

		private static final String T_RSS_NEWS_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_RSS_NEWS);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link RSSNewsDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.rss_db_version);
			}
			return dbVersion;
		}

		private Context context;

		public RSSNewsDbHelper(Context context) {
			this(context, DB_NAME, getDbVersion(context));
		}

		public RSSNewsDbHelper(Context context, String dbName, int dbVersion) {
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
			db.execSQL(T_RSS_NEWS_SQL_DROP);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l, true);
			initAllDbTables(db);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_RSS_NEWS_SQL_CREATE);
		}
	}
}
