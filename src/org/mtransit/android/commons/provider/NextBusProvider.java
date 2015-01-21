package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.LocaleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PackageManagerUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.SqlUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.commons.helpers.MTDefaultHandler;
import org.mtransit.android.commons.provider.ServiceUpdateProvider.ServiceUpdateColumns;
import org.mtransit.android.commons.provider.ServiceUpdateProvider.ServiceUpdateFilter;
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
import android.text.TextUtils;
import android.util.Pair;

@SuppressLint("Registered")
public class NextBusProvider extends MTContentProvider implements ServiceUpdateProviderContract {

	private static final String TAG = NextBusProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	private static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = NextBusDbHelper.PREF_KEY_AGENCY_LAST_UPDATE_MS;

	public static UriMatcher getNewUriMatcher(String authority) {
		UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		ServiceUpdateProvider.append(URI_MATCHER, authority);
		return URI_MATCHER;
	}

	private static UriMatcher uriMatcher = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	public static UriMatcher getURIMATCHER(Context context) {
		if (uriMatcher == null) {
			uriMatcher = getNewUriMatcher(getAUTHORITY(context));
		}
		return uriMatcher;
	}

	private static String authority = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	public static String getAUTHORITY(Context context) {
		if (authority == null) {
			authority = context.getResources().getString(R.string.next_bus_authority);
		}
		return authority;
	}

	private static String targetAuthority = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	public static String getTARGET_AUTHORITY(Context context) {
		if (targetAuthority == null) {
			targetAuthority = context.getResources().getString(R.string.next_bus_for_poi_authority);
		}
		return targetAuthority;
	}

	private static Uri authorityUri = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	public static Uri getAUTHORITY_URI(Context context) {
		if (authorityUri == null) {
			authorityUri = UriUtils.newContentUri(getAUTHORITY(context));
		}
		return authorityUri;
	}

	private static String agencyTag = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	public static String getAGENCY_TAG(Context context) {
		if (agencyTag == null) {
			agencyTag = context.getResources().getString(R.string.next_bus_agency_tag);
		}
		return agencyTag;
	}

	private static String textLanguageCode = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	public static String getTEXT_LANGUAGE_CODE(Context context) {
		if (textLanguageCode == null) {
			textLanguageCode = context.getResources().getString(R.string.next_bus_messages_text_language_code);
		}
		return textLanguageCode;
	}

	private static String textSecondaryLanguageCode = null;

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	public static String getTEXT_SECONDARY_LANGUAGE_CODE(Context context) {
		if (textSecondaryLanguageCode == null) {
			textSecondaryLanguageCode = context.getResources().getString(R.string.next_bus_messages_text_secondary_language_code);
		}
		return textSecondaryLanguageCode;
	}

	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS = TimeUnit.MINUTES.toMillis(10);

	private static final long SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(1);

	@Override
	public long getMinDurationBetweenServiceUpdateRefreshInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_MIN_DURATION_BETWEEN_REFRESH_IN_MS;
	}

	private static final long SERVICE_UPDATE_MAX_VALIDITY_IN_MS = TimeUnit.DAYS.toMillis(1);

	@Override
	public long getServiceUpdateMaxValidityInMs() {
		return SERVICE_UPDATE_MAX_VALIDITY_IN_MS;
	}

	private static final long SERVICE_UPDATE_VALIDITY_IN_MS = TimeUnit.HOURS.toMillis(1);

	private static final long SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS = TimeUnit.MINUTES.toMillis(10);

	@Override
	public long getServiceUpdateValidityInMs(boolean inFocus) {
		if (inFocus) {
			return SERVICE_UPDATE_VALIDITY_IN_FOCUS_IN_MS;
		}
		return SERVICE_UPDATE_VALIDITY_IN_MS;
	}

	@Override
	public String getServiceUpdateDbTableName() {
		return NextBusDbHelper.T_NEXT_BUS_SERVICE_UPDATE;
	}

	@Override
	public void cacheServiceUpdates(Collection<ServiceUpdate> newServiceUpdates) {
		ServiceUpdateProvider.cacheServiceUpdatesS(this, newServiceUpdates);
	}

	@Override
	public Collection<ServiceUpdate> getCachedServiceUpdates(ServiceUpdateFilter serviceUpdateFilter) {
		if (serviceUpdateFilter.getPoi() == null || !(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getCachedServiceUpdates() > no service update (poi null or not RTS)");
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		HashSet<ServiceUpdate> serviceUpdates = new HashSet<ServiceUpdate>();
		HashSet<String> targetUUIDs = getTargetUUIDs(rts);
		for (String targetUUID : targetUUIDs) {
			Collection<ServiceUpdate> cachedServiceUpdates = ServiceUpdateProvider.getCachedServiceUpdatesS(this, targetUUID);
			serviceUpdates.addAll(cachedServiceUpdates);
		}
		enhanceRTServiceUpdateForStop(serviceUpdates, rts);
		return serviceUpdates;
	}

	private void enhanceRTServiceUpdateForStop(Collection<ServiceUpdate> serviceUpdates, RouteTripStop rts) {
		try {
			if (CollectionUtils.getSize(serviceUpdates) > 0) {
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					serviceUpdate.setTargetUUID(rts.getUUID()); // route trip service update targets stop
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while trying to enhance route trip service update for stop!");
		}
	}

	private HashSet<String> getTargetUUIDs(RouteTripStop rts) {
		HashSet<String> targetUUIDs = new HashSet<String>();
		targetUUIDs.add(getAgencyTargetUUID(rts.getAuthority()));
		targetUUIDs.add(getAgencyRouteTargetUUID(rts.getAuthority(), rts.route.shortName));
		targetUUIDs.add(getAgencyRouteTripTargetUUID(rts.getAuthority(), rts.route.shortName, rts.trip.headsignValue));
		targetUUIDs.add(getAgencyRouteTripStopTargetUUID(rts.getAuthority(), rts.route.shortName, rts.trip.headsignValue, rts.stop.code));
		targetUUIDs.add(getAgencyRouteStopTargetUUID(rts.getAuthority(), rts.route.shortName, rts.stop.code));
		return targetUUIDs;
	}

	private static String getAgencyRouteTripStopTargetUUID(String agencyAuthority, String routeShortName, String tripHeadsignValue, String stopCode) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName, tripHeadsignValue, stopCode);
	}

	protected static String getAgencyRouteStopTargetUUID(String agencyAuthority, String routeShortName, String stopCode) {
		return getAgencyRouteTripTargetUUID(agencyAuthority, routeShortName, stopCode);
	}

	protected static String getAgencyRouteTripTargetUUID(String agencyAuthority, String routeShortName, String tripHeadsignValue) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName, tripHeadsignValue);
	}

	protected static String getAgencyRouteTargetUUID(String agencyAuthority, String routeShortName) {
		return POI.POIUtils.getUUID(agencyAuthority, routeShortName);
	}

	protected static String getAgencyTargetUUID(String agencyAuthority) {
		return POI.POIUtils.getUUID(agencyAuthority);
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

	@Override
	public Collection<ServiceUpdate> getNewServiceUpdates(ServiceUpdateFilter serviceUpdateFilter) {
		if (serviceUpdateFilter == null || serviceUpdateFilter.getPoi() == null || !(serviceUpdateFilter.getPoi() instanceof RouteTripStop)) {
			MTLog.w(this, "getNewServiceUpdates() > no new service update (filter null or poi null or not RTS): %s", serviceUpdateFilter);
			return null;
		}
		RouteTripStop rts = (RouteTripStop) serviceUpdateFilter.getPoi();
		updateAgencyServiceUpdateDataIfRequired(rts.getAuthority(), serviceUpdateFilter.isInFocusOrDefault());
		Collection<ServiceUpdate> cachedServiceUpdates = getCachedServiceUpdates(serviceUpdateFilter);
		if (CollectionUtils.getSize(cachedServiceUpdates) == 0) {
			String agencyTargetUUID = getAgencyTargetUUID(rts.getAuthority());
			cachedServiceUpdates = Arrays.asList(getServiceUpdateNone(agencyTargetUUID));
			enhanceRTServiceUpdateForStop(cachedServiceUpdates, rts); // convert to stop service update
		}
		return cachedServiceUpdates;
	}

	public ServiceUpdate getServiceUpdateNone(String agencyTargetUUID) {
		return new ServiceUpdate(null, agencyTargetUUID, TimeUtils.currentTimeMillis(), getServiceUpdateMaxValidityInMs(), null, null,
				ServiceUpdate.SEVERITY_NONE, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, getServiceUpdateLanguage());
	}

	private static final String AGENCY_SOURCE_ID = "next_bus_com_messages";

	private static final String AGENCY_SOURCE_LABEL = "NextBus";

	private void updateAgencyServiceUpdateDataIfRequired(String tagetAuthority, boolean inFocus) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l);
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + minUpdateMs > nowInMs) {
			return;
		}
		updateAgencyServiceUpdateDataIfRequiredSync(tagetAuthority, lastUpdateInMs, inFocus);
	}

	private synchronized void updateAgencyServiceUpdateDataIfRequiredSync(String tagetAuthority, long lastUpdateInMs, boolean inFocus) {
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l) > lastUpdateInMs) {
			return; // too late, another thread already updated
		}
		long nowInMs = TimeUtils.currentTimeMillis();
		boolean deleteAllRequired = false;
		if (lastUpdateInMs + getServiceUpdateMaxValidityInMs() < nowInMs) {
			deleteAllRequired = true; // too old to display
		}
		long minUpdateMs = Math.min(getServiceUpdateMaxValidityInMs(), getServiceUpdateValidityInMs(inFocus));
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

	// http://webservices.nextbus.com/service/publicXMLFeed?command=messages&a=<agency_tag>
	private static final String AGENCY_URL_PART_1_BEFORE_AGENCY_TAG = "http://webservices.nextbus.com/service/publicXMLFeed?command=messages&a=";

	private static String getAgencyUrlString(Context context) {
		return new StringBuilder() //
				.append(AGENCY_URL_PART_1_BEFORE_AGENCY_TAG) //
				.append(getAGENCY_TAG(context)) //
				.toString();
	}

	private Collection<ServiceUpdate> loadAgencyServiceUpdateDataFromWWW(String tagetAuthority) {
		try {
			String urlString = getAgencyUrlString(getContext());
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
				NextBusMessagesDataHandler handler = new NextBusMessagesDataHandler(newLastUpdateInMs, getTARGET_AUTHORITY(getContext()),
						getServiceUpdateMaxValidityInMs(), getTEXT_LANGUAGE_CODE(getContext()), getTEXT_SECONDARY_LANGUAGE_CODE(getContext()));
				xr.setContentHandler(handler);
				xr.parse(new InputSource(urlc.getInputStream()));
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
		} catch (Exception e) { // Unknown error
			MTLog.e(TAG, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private int deleteAllAgencyServiceUpdateData() {
		int affectedRows = 0;
		SQLiteDatabase db;
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
	public String getServiceUpdateLanguage() {
		return LocaleUtils.isFR() ? Locale.FRENCH.getLanguage() : Locale.ENGLISH.getLanguage();
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

	private static NextBusDbHelper dbHelper;

	private static int currentDbVersion = -1;

	private NextBusDbHelper getDBHelper(Context context) {
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
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	public int getCurrentDbVersion() {
		return NextBusDbHelper.getDbVersion(getContext());
	}

	/**
	 * Override if multiple {@link NextBusProvider} implementations in same app.
	 */
	public NextBusDbHelper getNewDbHelper(Context context) {
		return new NextBusDbHelper(context.getApplicationContext());
	}

	@Override
	public UriMatcher getURI_MATCHER() {
		return getURIMATCHER(getContext());
	}

	@Override
	public Uri getAuthorityUri() {
		return getAUTHORITY_URI(getContext());
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
		Cursor cursor = ServiceUpdateProvider.queryS(this, uri, selection);
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

	private static class NextBusMessagesDataHandler extends MTDefaultHandler {

		private static final String TAG = NextBusProvider.TAG + ">" + NextBusMessagesDataHandler.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * XML tags.
		 */
		private static final String BODY = "body";
		private static final String ROUTE = "route";
		private static final String ROUTE_TAG = "tag";
		private static final String ROUTE_TAG_ALL = "all";
		private static final String MESSAGE = "message";
		private static final String MESSAGE_ID = "id";
		private static final String MESSAGE_PRIORITY = "priority";
		private static final String MESSAGE_PRIORITY_NORMAL = "Normal";
		private static final String MESSAGE_PRIORITY_LOW = "Low";
		private static final String ROUTE_CONFIGURED_FOR_MESSAGE = "routeConfiguredForMessage";
		private static final String ROUTE_CONFIGURED_FOR_MESSAGE_TAG = "tag";
		private static final String STOP = "stop";
		private static final String STOP_TAG = "tag";
		private static final String TEXT = "text";
		private static final String TEXT_SECONDARY_LANGUAGE = "textSecondaryLanguage";

		private String currentLocalName = BODY;

		private boolean currentRouteAll = false;

		private long newLastUpdateInMs;

		private long serviceUpdateMaxValidityInMs;

		private HashSet<ServiceUpdate> serviceUpdates = new HashSet<ServiceUpdate>();

		private String authority;

		private Pair<String, String> currentRSNAndTripHS = null;

		private Pair<String, String> currentRouteConfiguredForMessageRSNAndTripHS = null;
		private HashMap<Pair<String, String>, HashSet<String>> currentRouteConfiguredForMessage = new HashMap<Pair<String, String>, HashSet<String>>();

		private StringBuilder currentTextSb = new StringBuilder();
		private StringBuilder currentTextSecondaryLanguageSb = new StringBuilder();

		private String textLanguageCode;
		private String textSecondaryLanguageCode;

		private HashMap<String, HashSet<String>> textMessageIdTargetUUID = new HashMap<String, HashSet<String>>();
		private HashMap<String, HashSet<String>> textSecondaryMessageIdTargetUUID = new HashMap<String, HashSet<String>>();

		private String currentMessageId;
		private String currentMessagePriority;

		public NextBusMessagesDataHandler(long newLastUpdateInMs, String authority, long serviceUpdateMaxValidityInMs, String textLanguageCode,
				String textSecondaryLanguageCode) {
			this.newLastUpdateInMs = newLastUpdateInMs;
			this.authority = authority;
			this.serviceUpdateMaxValidityInMs = serviceUpdateMaxValidityInMs;
			this.textLanguageCode = textLanguageCode;
			this.textSecondaryLanguageCode = textSecondaryLanguageCode;
		}

		public HashSet<ServiceUpdate> getServiceUpdates() {
			return this.serviceUpdates;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			this.currentLocalName = localName;
			if (BODY.equals(this.currentLocalName)) {
			} else if (ROUTE.equals(this.currentLocalName)) {
				String routeTag = attributes.getValue(ROUTE_TAG);
				if (ROUTE_TAG_ALL.equals(routeTag)) {
					this.currentRSNAndTripHS = null;
					this.currentRouteAll = true;
				} else {
					String currentRouteShortName = routeTag.substring(0, routeTag.length() - 1); // STL only
					String currentTripHeadsignValue = routeTag.substring(routeTag.length() - 1, routeTag.length()); // STL only
					this.currentRSNAndTripHS = new Pair<String, String>(currentRouteShortName, currentTripHeadsignValue);
					this.currentRouteAll = false;
				}
				this.currentMessagePriority = null;
				this.currentMessageId = null;
			} else if (MESSAGE.equals(this.currentLocalName)) {
				this.currentMessagePriority = attributes.getValue(MESSAGE_PRIORITY);
				this.currentMessageId = attributes.getValue(MESSAGE_ID);
				if (!this.textMessageIdTargetUUID.containsKey(this.currentMessageId)) {
					this.textMessageIdTargetUUID.put(this.currentMessageId, new HashSet<String>());
				}
				if (!this.textSecondaryMessageIdTargetUUID.containsKey(this.currentMessageId)) {
					this.textSecondaryMessageIdTargetUUID.put(this.currentMessageId, new HashSet<String>());
				}
				this.currentRouteConfiguredForMessageRSNAndTripHS = null;
				this.currentRouteConfiguredForMessage.clear();
				this.currentTextSb = new StringBuilder();
				this.currentTextSecondaryLanguageSb = new StringBuilder();
			} else if (ROUTE_CONFIGURED_FOR_MESSAGE.equals(this.currentLocalName)) {
				String routeTag = attributes.getValue(ROUTE_CONFIGURED_FOR_MESSAGE_TAG);
				String routeShortName = routeTag.substring(0, routeTag.length() - 1); // STL only
				String tripHeadsignValue = routeTag.substring(routeTag.length() - 1, routeTag.length()); // STL only
				this.currentRouteConfiguredForMessageRSNAndTripHS = new Pair<String, String>(routeShortName, tripHeadsignValue);
				if (!this.currentRouteConfiguredForMessage.containsKey(this.currentRouteConfiguredForMessageRSNAndTripHS)) {
					this.currentRouteConfiguredForMessage.put(this.currentRouteConfiguredForMessageRSNAndTripHS, new HashSet<String>());
				}
			} else if (STOP.equals(this.currentLocalName)) {
				String stopTag = attributes.getValue(STOP_TAG);
				if (stopTag.startsWith("CP")) { // STL only
					stopTag = stopTag.substring(2); // STL only
				} // STL only
				this.currentRouteConfiguredForMessage.get(this.currentRouteConfiguredForMessageRSNAndTripHS).add(stopTag);
			} else if (TEXT.equals(this.currentLocalName)) { // ignore
			} else if (TEXT_SECONDARY_LANGUAGE.equals(this.currentLocalName)) { // ignore
			} else {
				MTLog.w(this, "startElement() > Unexpected element '%s'", this.currentLocalName);
			}

		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);
			try {
				String string = new String(ch, start, length).trim();
				if (TextUtils.isEmpty(string)) {
					return;
				}
				if (TEXT.equals(this.currentLocalName)) {
					this.currentTextSb.append(string);
				} else if (TEXT_SECONDARY_LANGUAGE.equals(this.currentLocalName)) {
					this.currentTextSecondaryLanguageSb.append(string);
				} else if (STOP.equals(this.currentLocalName)) { // ignore
				} else {
					MTLog.w(this, "Unexpected name '%s'! while parsing '%s'", this.currentLocalName, string);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing '%s' value '%s, %s, %s'!", this.currentLocalName, ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (MESSAGE.equals(localName)) {
				if (this.currentTextSb.length() == 0 && this.currentTextSecondaryLanguageSb.length() == 0) {
					return; // no message
				}
				if (this.currentRouteConfiguredForMessage.size() > 0) { // ROUTE(s)
					for (Pair<String, String> routeAndTrip : this.currentRouteConfiguredForMessage.keySet()) {
						if (this.currentRouteConfiguredForMessage.get(routeAndTrip).size() == 0) {
							String targetUUID;
							if (TextUtils.isEmpty(routeAndTrip.second)) { // ROUTE
								targetUUID = NextBusProvider.getAgencyRouteTargetUUID(this.authority, routeAndTrip.first);
							} else { // ROUTE TRIP
								targetUUID = NextBusProvider.getAgencyRouteTripTargetUUID(this.authority, routeAndTrip.first, routeAndTrip.second);
							}
							int severity = findRouteSeverity();
							addServiceUpdates(targetUUID, severity);
						} else {
							for (String stopCode : this.currentRouteConfiguredForMessage.get(routeAndTrip)) {
								String targetUUID;
								if (TextUtils.isEmpty(routeAndTrip.second)) { // ROUTE STOP
									targetUUID = NextBusProvider.getAgencyRouteStopTargetUUID(this.authority, routeAndTrip.first, stopCode);
								} else { // ROUTE TRIP STOP
									targetUUID = NextBusProvider.getAgencyRouteTripStopTargetUUID(this.authority, routeAndTrip.first, routeAndTrip.second,
											stopCode);
								}
								int severity = findStopPriority();
								addServiceUpdates(targetUUID, severity);
							}
						}
					}
				} else if (this.currentRSNAndTripHS != null) {
					String targetUUID;
					if (TextUtils.isEmpty(this.currentRSNAndTripHS.second)) { // ROUTE
						targetUUID = NextBusProvider.getAgencyRouteTargetUUID(this.authority, this.currentRSNAndTripHS.first);
					} else { // ROUTE TRIP
						targetUUID = NextBusProvider.getAgencyRouteTripTargetUUID(this.authority, this.currentRSNAndTripHS.first,
								this.currentRSNAndTripHS.second);
					}
					int severity = findAgencySeverity();
					addServiceUpdates(targetUUID, severity);
				} else if (this.currentRouteAll) { // AGENCY
					String targetUUID = NextBusProvider.getAgencyTargetUUID(this.authority);
					int severity = findAgencySeverity();
					addServiceUpdates(targetUUID, severity);
				} else {
					MTLog.w(this, "Unexpected combination of tags!");
				}
			}
		}

		private int findStopPriority() {
			if (MESSAGE_PRIORITY_NORMAL.equals(this.currentMessagePriority)) {
				return ServiceUpdate.SEVERITY_WARNING_POI;
			} else if (MESSAGE_PRIORITY_LOW.equals(this.currentMessagePriority)) {
				return ServiceUpdate.SEVERITY_INFO_POI;
			}
			MTLog.w(this, "endElement() > unexpected message priority: %s", this.currentMessagePriority);
			return ServiceUpdate.SEVERITY_WARNING_UNKNOWN; // default
		}

		private int findRouteSeverity() {
			if (MESSAGE_PRIORITY_NORMAL.equals(this.currentMessagePriority)) {
				return ServiceUpdate.SEVERITY_WARNING_RELATED_POI;
			} else if (MESSAGE_PRIORITY_LOW.equals(this.currentMessagePriority)) {
				return ServiceUpdate.SEVERITY_INFO_RELATED_POI;
			}
			MTLog.w(this, "endElement() > unexpected message priority: %s", this.currentMessagePriority);
			return ServiceUpdate.SEVERITY_WARNING_UNKNOWN; // default
		}

		private int findAgencySeverity() {
			if (MESSAGE_PRIORITY_NORMAL.equals(this.currentMessagePriority)) {
				return ServiceUpdate.SEVERITY_WARNING_AGENCY;
			} else if (MESSAGE_PRIORITY_LOW.equals(this.currentMessagePriority)) {
				return ServiceUpdate.SEVERITY_INFO_AGENCY;
			}
			MTLog.w(this, "endElement() > unexpected message priority: %s", this.currentMessagePriority);
			return ServiceUpdate.SEVERITY_WARNING_UNKNOWN; // default
		}

		private void addServiceUpdates(String targetUUID, int severity) {
			if (this.currentTextSb.length() > 0) {
				if (!this.textMessageIdTargetUUID.get(this.currentMessageId).contains(targetUUID)) {
					this.serviceUpdates.add(new ServiceUpdate(null, targetUUID, this.newLastUpdateInMs, this.serviceUpdateMaxValidityInMs, this.currentTextSb
							.toString(), null, severity, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL, this.textLanguageCode));
					this.textMessageIdTargetUUID.get(this.currentMessageId).add(targetUUID);
				}
			}
			if (this.currentTextSecondaryLanguageSb.length() > 0) {
				if (!this.textSecondaryMessageIdTargetUUID.get(this.currentMessageId).contains(targetUUID)) {
					this.serviceUpdates.add(new ServiceUpdate(null, targetUUID, this.newLastUpdateInMs, this.serviceUpdateMaxValidityInMs,
							this.currentTextSecondaryLanguageSb.toString(), null, severity, AGENCY_SOURCE_ID, AGENCY_SOURCE_LABEL,
							this.textSecondaryLanguageCode));
					this.textSecondaryMessageIdTargetUUID.get(this.currentMessageId).add(targetUUID);
				}
			}
		}
	}

	public static class NextBusDbHelper extends ServiceUpdateProvider.ServiceUpdateDbHelper {

		private static final String TAG = NextBusDbHelper.class.getSimpleName();

		@Override
		public String getLogTag() {
			return TAG;
		}

		/**
		 * Override if multiple {@link NextBusDbHelper} implementations in same app.
		 */
		protected static final String DB_NAME = "nextbus.db";

		/**
		 * Override if multiple {@link NextBusDbHelper} implementations in same app.
		 */
		protected static final String PREF_KEY_AGENCY_LAST_UPDATE_MS = "pNextBusMessagesLastUpdate";

		public static final String T_NEXT_BUS_SERVICE_UPDATE = ServiceUpdateProvider.ServiceUpdateDbHelper.T_SERVICE_UPDATE;

		private static final String T_NEXT_BUS_SERVICE_UPDATE_SQL_CREATE = ServiceUpdateProvider.ServiceUpdateDbHelper.getSqlCreate(T_NEXT_BUS_SERVICE_UPDATE);

		private static final String T_NEXT_BUS_SERVICE_UPDATE_SQL_DROP = SqlUtils.getSQLDropIfExistsQuery(T_NEXT_BUS_SERVICE_UPDATE);

		private static int dbVersion = -1;

		/**
		 * Override if multiple {@link NextBusDbHelper} in same app.
		 */
		public static int getDbVersion(Context context) {
			if (dbVersion < 0) {
				dbVersion = context.getResources().getInteger(R.integer.next_bus_db_version);
			}
			return dbVersion;
		}

		private Context context;

		public NextBusDbHelper(Context context) {
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
			db.execSQL(T_NEXT_BUS_SERVICE_UPDATE_SQL_DROP);
			PreferenceUtils.savePrefLcl(this.context, PREF_KEY_AGENCY_LAST_UPDATE_MS, 0l, true);
			initAllDbTables(db);
		}

		public boolean isDbExist(Context context) {
			return SqlUtils.isDbExist(context, DB_NAME);
		}

		private void initAllDbTables(SQLiteDatabase db) {
			db.execSQL(T_NEXT_BUS_SERVICE_UPDATE_SQL_CREATE);
		}
	}

}
