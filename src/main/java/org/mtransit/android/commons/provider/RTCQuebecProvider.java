package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mtransit.android.commons.ArrayUtils;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.helpers.MTDefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.Html;
import android.text.TextUtils;

@SuppressLint("Registered")
public class RTCQuebecProvider extends MTContentProvider implements ServiceUpdateProviderContract {

	private static final String TAG = RTCQuebecProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS = RTCQuebecDbHelper.PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS;

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		StatusProvider.append(URI_MATCHER, authority);
		ServiceUpdateProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
	 */
	private static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
	 */
	private static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.rtc_quebec_authority);
		}
		return authority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
	 */
	private static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String serviceUpdateTargetAuthority = null;

	/**
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
	 */
	private static String getSERVICE_UPDATE_TARGET_AUTHORITY(Context context) {
		if (serviceUpdateTargetAuthority == null) {
			serviceUpdateTargetAuthority = context.getResources().getString(R.string.rtc_quebec_service_update_for_poi_authority);
		}
		return serviceUpdateTargetAuthority;
	}

	private static final long SERVICE_UPDATE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1);
	private static final long SERVICE_UPDATE_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);
	private static final long SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

	@Override
	public long getMinDurationBetweenServiceUpdateRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	@Override
	public long getServiceUpdateMaxValidityInMs() {
		return SERVICE_UPDATE_MAX_VALIDITY_IN_MS;
	}

	@Override
	public long getServiceUpdateValidityInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_VALIDITY_IN_MS;
	}

	@Override
	public String getServiceUpdateDbTableName() {
		return RTCQuebecDbHelper.T_RTC_SERVICE_UPDATE;
	}

	@Override
	public void cacheServiceUpdates(ArrayList<ServiceUpdate> newServiceUpdates) {
		ServiceUpdateProvider.cacheServiceUpdatesS(this, newServiceUpdates);
	}

	@Override
	public ArrayList<ServiceUpdate> getCachedServiceUpdates(ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (serviceUpdateFilter.getPoi() == null || !(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getCachedServiceUpdates() > no service update (poi null or not RTS)");
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		ArrayList<ServiceUpdate> serviceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, getTargetUUIDs(rts));
		enhanceRTServiceUpdateForStop(serviceUpdates, rts);
		return serviceUpdates;
	}

	private void enhanceRTServiceUpdateForStop(ArrayList<ServiceUpdate> serviceUpdates, RouteTripStop rts) {
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
			if (serviceUpdate.getSeverity() > ServiceUpdate.SEVERITY_NONE) {
				String originalHtml = serviceUpdate.getTextHTML();
				int severity = findRTSSeverity(originalHtml, rts);
				if (severity > serviceUpdate.getSeverity()) {
					serviceUpdate.setSeverity(severity);
				}
				serviceUpdate.setTextHTML(enhanceRTTextForStop(originalHtml, rts, serviceUpdate.getSeverity()));
			}
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

	private static final String NON_DIGIT = "\\D";
	private static final String NON_WORD = "\\W";

	private static final String CLEAN_THAT = "(^|[%s]{1})(%s)($|[%s]{1})";
	private static final String CLEAN_THAT_REPLACEMENT = "$1" + HtmlUtils.applyBold("$2") + "$3";

	private String enhanceHtmlRts(RouteTripStop rts, String html) {
		if (TextUtils.isEmpty(html)) {
			return html;
		}
		try {
			String code = rts.getStop().getCode();
			if (!TextUtils.isEmpty(code)) {
				String beforeCode = Character.isDigit(code.charAt(0)) ? NON_DIGIT : NON_WORD;
				String afterCode = Character.isDigit(code.charAt(code.length() - 1)) ? NON_DIGIT : NON_WORD;
				String format = String.format(CLEAN_THAT, beforeCode, code, afterCode);
				html = Pattern.compile(format).matcher(html).replaceAll(CLEAN_THAT_REPLACEMENT);
			}
			String rsn = rts.getRoute().getShortName();
			if (!TextUtils.isEmpty(rsn)) {
				String beforeRSN = Character.isDigit(rsn.charAt(0)) ? NON_DIGIT : NON_WORD;
				String afterRSN = Character.isDigit(rsn.charAt(rsn.length() - 1)) ? NON_DIGIT : NON_WORD;
				String format = String.format(CLEAN_THAT, beforeRSN, rsn, afterRSN);
				html = Pattern.compile(format).matcher(html).replaceAll(CLEAN_THAT_REPLACEMENT);
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
			MTLog.w(TAG, e, "Error while enhancing HTML '%s'!", html);
			return html;
		}
	}

	private static final Pattern CLEAN_BOLD_FR = Pattern.compile("(non desservi[s]?|d[é|e]plac[é|e][s]?|d\\&eacute\\;plac\\&eacute\\;[s]?)",
			Pattern.CASE_INSENSITIVE);
	private static final String CLEAN_BOLD_REPLACEMENT = HtmlUtils.applyBold("$1");

	private String enhanceHtmlSeverity(int severity, String html) {
		if (TextUtils.isEmpty(html)) {
			return html;
		}
		if (ServiceUpdate.isSeverityWarning(severity)) {
			html = CLEAN_BOLD_FR.matcher(html).replaceAll(CLEAN_BOLD_REPLACEMENT);
		}
		return html;
	}
	private static final Pattern NOT_SERVED = Pattern.compile("(" + //
			"arr[e|ê]t[s]? non desservi[s]?" //
			+ "|" //
			+ "arr&ecirc;t[s]? non desservi[s]?" //
			+ ")", Pattern.CASE_INSENSITIVE);

	private static final Pattern SUGGESTED = Pattern.compile("(" //
			+ "arr[e|ê]t[s]? sugg[e|é]r[e|é][s]?" //
			+ "|" //
			+ "arr&ecirc;t[s]? sugg&eacute;r&eacute;[s]?" //
			+ ")", Pattern.CASE_INSENSITIVE);

	private static final String STOP_CODE_FORMAT = "((^|[^0-9]){1}(%s)([^0-9]|$){1})";

	private int findRTSSeverity(String originalHtml, RouteTripStop rts) {
		if (!TextUtils.isEmpty(originalHtml)) {
			Matcher stopMatcher = Pattern.compile(String.format(STOP_CODE_FORMAT, rts.getStop().getCode()), Pattern.CASE_INSENSITIVE).matcher(originalHtml);
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
		MTLog.w(this, "findRTSSeverity() > Cannot find RTS '%s' severity for '%s'.", rts, originalHtml);
		return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
	}

	private HashSet<String> getTargetUUIDs(RouteTripStop rts) {
		HashSet<String> targetUUIDs = new HashSet<>();
		targetUUIDs.add(getAgencyRouteShortNameTargetUUID(rts.getAuthority(), rts.getRoute().getShortName()));
		return targetUUIDs;
	}

	protected static String getAgencyRouteShortNameTargetUUID(String agencyAuthority, String routeShortName) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName);
	}

	@Override
	public boolean purgeUselessCachedServiceUpdates() {
		return ServiceUpdateProvider.purgeUselessCachedServiceUpdates(this);
	}

	@Override
	public boolean deleteCachedServiceUpdate(Integer serviceUpdateId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, serviceUpdateId);
	}

	@Override
	public boolean deleteCachedServiceUpdate(String targetUUID, String sourceId) {
		return ServiceUpdateProvider.deleteCachedServiceUpdate(this, targetUUID, sourceId);
	}

	private static final String AGENCY_SOURCE_ID = "www_rtcquebec_ca_rtc_rss_aspx_type_avis_source_mobile";

	private static final String AGENCY_SOURCE_LABEL = "rtcquebec.ca";

	private int deleteAllAgencyServiceUpdateData() {
		int affectedRows = 0;
		try {
			String selection = SqlUtils.getWhereEqualsString(ServiceUpdateProviderContract.Columns.T_SERVICE_UPDATE_K_SOURCE_ID, AGENCY_SOURCE_ID);
			affectedRows = getDBHelper().getWritableDatabase().delete(getServiceUpdateDbTableName(), selection, null);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while deleting all agency service update data!");
		}
		return affectedRows;
	}

	@Nullable
	@Override
	public ArrayList<ServiceUpdate> getNewServiceUpdates(@NonNull ServiceUpdateProviderContract.Filter serviceUpdateFilter) {
		if (!(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getNewServiceUpdates() > no new service update (filter null or poi null or not RTS): %s", serviceUpdateFilter);
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		updateAgencyServiceUpdateDataIfRequired(rts.getAuthority(), serviceUpdateFilter.isInFocusOrDefault());
		return getCachedServiceUpdates(serviceUpdateFilter);
	}

	@Override
	public String getServiceUpdateLanguage() {
		return Locale.FRENCH.getLanguage();
	}

	private void updateAgencyServiceUpdateDataIfRequired(String targetAuthority, boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS, 0L);
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyServiceUpdateDataIfRequiredSync(targetAuthority, lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyServiceUpdateDataIfRequiredSync(String targetAuthority, long lastUpdateInMs, boolean inFocus) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS, 0L) > lastUpdateInMs) {
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		if (lastUpdateInMs + getServiceUpdateMaxValidityInMs() < nowInMs) {
			deleteAllRequired = true; // too old to display
		}
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		if (deleteAllRequired || lastUpdateInMs + minUpdateMs < nowInMs) {
			updateAllAgencyServiceUpdateDataFromWWW(targetAuthority, deleteAllRequired); // try to update
		}
	}

	private void updateAllAgencyServiceUpdateDataFromWWW(String targetAuthority, boolean deleteAllRequired) {
		boolean deleteAllDone = false;
		if (deleteAllRequired) {
			deleteAllAgencyServiceUpdateData();
			deleteAllDone = true;
		}
		ArrayList<ServiceUpdate> newServiceUpdates = loadAgencyServiceUpdateDataFromWWW(targetAuthority);
		if (newServiceUpdates != null) { // empty is OK
			long nowInMs = TimeUtils.currentTimeMillis();
			if (!deleteAllDone) {
				deleteAllAgencyServiceUpdateData();
			}
			cacheServiceUpdates(newServiceUpdates);
			PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS, nowInMs, true); // sync
		} // else keep whatever we have until max validity reached
	}

	private static final String AGENCY_URL = "https://www.rtcquebec.ca/rtc/rss.aspx?type=avis&source=mobile";

	private static final String ENCODING = "iso-8859-1";

	private static final String PRIVATE_FILE_NAME = "rtcquebec.xml";

	private ArrayList<ServiceUpdate> loadAgencyServiceUpdateDataFromWWW(String targetAuthority) {
		Context context = getContext();
		if (context == null) {
			return null;
		}
		try {
			String urlString = AGENCY_URL;
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				RTCQuebecRSSAvisMobileDataHandler handler =
						new RTCQuebecRSSAvisMobileDataHandler(getSERVICE_UPDATE_TARGET_AUTHORITY(context), newLastUpdateInMs,
								getServiceUpdateMaxValidityInMs(), getServiceUpdateLanguage());
				xr.setContentHandler(handler);
				FileUtils.copyToPrivateFile(context, PRIVATE_FILE_NAME, urlc.getInputStream(), ENCODING);
				xr.parse(new InputSource(context.openFileInput(PRIVATE_FILE_NAME)));
				return handler.getServiceUpdates();
			default:
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpUrlConnection.getResponseCode(),
						httpUrlConnection.getResponseMessage());
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

	@Override
	public boolean onCreateMT() {
		ping();
		return true;
	}

	@Override
	public void ping() {
		// DO NOTHING
	}

	private RTCQuebecDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private RTCQuebecDbHelper getDBHelper(Context context) {
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
		return RTCQuebecDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link RTCQuebecProvider} implementations in same app.
	 */
	public RTCQuebecDbHelper getNewDbHelper(Context context) {
		return new RTCQuebecDbHelper(context.getApplicationContext());
	}

	@NonNull
	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITY_URI(getContext());
	}

	@NonNull
	@Override
	public SQLiteOpenHelper getDBHelper() {
		return getDBHelper(getContext());
	}

	@Override
	public Cursor queryMT(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Cursor cursor = ServiceUpdateProvider.queryS(this, uri, selection);
		if (cursor != null) {
			return cursor;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (query): '%s'", uri));
	}

	@Override
	public String getTypeMT(@NonNull Uri uri) {
		String type = ServiceUpdateProvider.getTypeS(this, uri);
		if (type != null) {
			return type;
		}
		throw new IllegalArgumentException(String.format("Unknown URI (type): '%s'", uri));
	}

	@Override
	public int deleteMT(@NonNull Uri uri, String selection, String[] selectionArgs) {
		MTLog.w(this, "The delete method is not available.");
		return 0;
	}

	@Override
	public int updateMT(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		MTLog.w(this, "The update method is not available.");
		return 0;
	}

	@Override
	public Uri insertMT(@NonNull Uri uri, ContentValues values) {
		MTLog.w(this, "The insert method is not available.");
		return null;
	}

	private static class RTCQuebecRSSAvisMobileDataHandler extends MTDefaultHandler {

		private static final String TAG = RTCQuebecProvider.TAG + ">" + RTCQuebecRSSAvisMobileDataHandler.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
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
		private StringBuilder currentTitleSb = new StringBuilder();
		private StringBuilder currentDescriptionSb = new StringBuilder();
		private StringBuilder currentLinkSb = new StringBuilder();
		private StringBuilder currentParcoursIdsSb = new StringBuilder();
		private StringBuilder currentContentSb = new StringBuilder();
		private StringBuilder currentGUIDSb = new StringBuilder();

		private ArrayList<ServiceUpdate> serviceUpdates = new ArrayList<>();

		private String targetAuthority;
		private long newLastUpdateInMs;
		private long serviceUpdateMaxValidityInMs;
		private String language;

		public RTCQuebecRSSAvisMobileDataHandler(String targetAuthority, long newLastUpdateInMs, long serviceUpdateMaxValidityInMs, String language) {
			this.targetAuthority = targetAuthority;
			this.newLastUpdateInMs = newLastUpdateInMs;
			this.serviceUpdateMaxValidityInMs = serviceUpdateMaxValidityInMs;
			this.language = language;
		}

		public ArrayList<ServiceUpdate> getServiceUpdates() {
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
							textHTMLSb.append(HtmlUtils.applyBold(title));
						}
						if (!TextUtils.isEmpty(desc)) {
							if (textSb.length() > 0) {
								textSb.append(COLON);
							}
							textSb.append(desc);
							if (textHTMLSb.length() > 0) {
								textHTMLSb.append(HtmlUtils.BR);
							}
							textHTMLSb.append(HtmlUtils.applyBold(desc));
						}
						if (!TextUtils.isEmpty(content)) {
							if (textSb.length() > 0) {
								textSb.append(COLON);
							}
							textSb.append(Html.fromHtml(content));
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
										textSb.toString(), textHTMLSb.toString(), severity, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, this.language);
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

	private static class RTCQuebecDbHelper extends MTSQLiteOpenHelper {

		private static final String TAG = RTCQuebecDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link RTCQuebecDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "rtcquebec.db";

		/**
		 * Override if multiple {@link RTCQuebecDbHelper} implementations in same app.
		 */
		protected static final String PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS = "pRTCQuebecServiceUpdatesLastUpdate";

		public static final String T_RTC_SERVICE_UPDATE = ServiceUpdateProvider.ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_RTC_SERVICE_UPDATE_SQL_CREATE = ServiceUpdateProvider.ServiceUpdateDbHelper.getSqlCreateBuilder(T_RTC_SERVICE_UPDATE)
				.build();

		private static final String T_RTC_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_RTC_SERVICE_UPDATE);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link RTCQuebecDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.rtc_quebec_db_version);
			}
			return dbVersion;
		}

		private Context context;

		public RTCQuebecDbHelper(Context context) {
			super(context, DB_NAME, null, getDbVersion(context));
			this.context = context;
		}

		@Override
		public void onCreateMT(SQLiteDatabase db) {
			initAllDbTables(db);
		}

		@Override
		public void onUpgradeMT(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL(T_RTC_SERVICE_UPDATE_SQL_DROP);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_SERVICE_UPDATE_LAST_UPDATE_MS, 0L, true);
			initAllDbTables(db);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_RTC_SERVICE_UPDATE_SQL_CREATE);
		}
	}
}
