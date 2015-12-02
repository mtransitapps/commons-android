package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.BikeStationAvailabilityPercent;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.helpers.MTDefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

@SuppressLint("Registered")
public class BixiBikeStationProvider extends BikeStationProvider {

	private static final String TAG = BixiBikeStationProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link BixiBikeStationProvider} implementations in same app.
	 */
	private static final String PREF_KEY_LAST_UPDATE_MS = BikeStationDbHelper.PREF_KEY_LAST_UPDATE_MS;

	@Override
	public void updateBikeStationDataIfRequired() {
		long lastUpdateInMs = getLastUpdateInMs();
		long nowInMs = TimeUtils.currentTimeMillis();
		// MAX VALIDITY (too old to display?)
		if (lastUpdateInMs + getPOIMaxValidityInMs() < nowInMs) { // too old to display
			deleteAllBikeStationData();
			updateAllDataFromWWW(lastUpdateInMs);
			return;
		}
		if (lastUpdateInMs + getPOIValidityInMs() < nowInMs) { // try to update
			updateAllDataFromWWW(lastUpdateInMs);
		}
	}

	@Override
	public long getLastUpdateInMs() {
		return PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0l);
	}

	@Override
	public Cursor getPOIBikeStations(POIProviderContract.Filter poiFilter) {
		updateBikeStationDataIfRequired();
		return getPOIFromDB(poiFilter);
	}

	@Override
	public void updateBikeStationStatusDataIfRequired(StatusProviderContract.Filter statusFilter) {
		long lastUpdateInMs = getLastUpdateInMs();
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + getStatusMaxValidityInMs() < nowInMs) { // too old too display?
			deleteAllBikeStationStatusData();
			updateAllDataFromWWW(lastUpdateInMs);
			return;
		}
		if (lastUpdateInMs + getStatusValidityInMs(statusFilter.isInFocusOrDefault()) < nowInMs) { // try to refresh?
			updateAllDataFromWWW(lastUpdateInMs);
		}
	}

	@Override
	public POIStatus getNewBikeStationStatus(AvailabilityPercent.AvailabilityPercentStatusFilter statusFilter) {
		updateBikeStationStatusDataIfRequired(statusFilter);
		return getCachedStatus(statusFilter);
	}

	private synchronized void updateAllDataFromWWW(long oldLastUpdatedInMs) {
		if (getLastUpdateInMs() > oldLastUpdatedInMs) {
			return; // too late, another thread already updated
		}
		loadDataFromWWW();
	}

	private static final String PRIVATE_FILE_NAME = "bixi_bike_stations.xml";

	private HashSet<DefaultPOI> loadDataFromWWW() {
		try {
			String urlString = getDATA_URL(getContext());
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			urlc.addRequestProperty("Cache-Control", "no-cache"); // IMPORTANT!
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				FileUtils.copyToPrivateFile(getContext(), PRIVATE_FILE_NAME, urlc.getInputStream());
				SAXParserFactory spf = SAXParserFactory.newInstance();
				SAXParser sp = spf.newSAXParser();
				XMLReader xr = sp.getXMLReader();
				BixiBikeStationsDataHandler handler = new BixiBikeStationsDataHandler(getContext(), newLastUpdateInMs, getStatusMaxValidityInMs(),
						getPOIMaxValidityInMs(), getValue1Color(getContext()), getValue1ColorBg(getContext()), getValue2Color(getContext()),
						getValue2ColorBg(getContext()));
				xr.setContentHandler(handler);
				xr.parse(new InputSource(getContext().openFileInput(PRIVATE_FILE_NAME)));
				deleteAllBikeStationData();
				POIProvider.insertDefaultPOIs(this, handler.getBikeStations());
				deleteAllBikeStationStatusData();
				StatusProvider.cacheAllStatusesBulkLockDB(this, handler.getBikeStationsStatus());
				PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, newLastUpdateInMs, true); // sync
				return handler.getBikeStations();
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

	private static final String PLACE_CHAR_DE_L = "de l'";
	private static final String PLACE_CHAR_DE_LA = "de la ";
	private static final String PLACE_CHAR_D = "d'";
	private static final String PLACE_CHAR_DE = "de ";
	private static final String PLACE_CHAR_DES = "des ";
	private static final String PLACE_CHAR_DU = "du ";
	private static final String PLACE_CHAR_LA = "la ";
	private static final String PLACE_CHAR_LE = "le ";
	private static final String PLACE_CHAR_LES = "les ";
	private static final String PLACE_CHAR_L = "l'";

	private static final String[] START_WITH_CHARS = new String[] { //
	PLACE_CHAR_DE_L,//
			PLACE_CHAR_DE_LA, //
			PLACE_CHAR_D, //
			PLACE_CHAR_DE, //
			PLACE_CHAR_DES, //
			PLACE_CHAR_DU, //
			PLACE_CHAR_LA,//
			PLACE_CHAR_LE,//
			PLACE_CHAR_LES,//
			PLACE_CHAR_L //
	};

	public static final String SLASH_SPACE = "/ ";
	private static final String[] SLASH_CHARS = new String[] { //
	SLASH_SPACE + PLACE_CHAR_DE_L,//
			SLASH_SPACE + PLACE_CHAR_DE_LA,//
			SLASH_SPACE + PLACE_CHAR_D,//
			SLASH_SPACE + PLACE_CHAR_DE,//
			SLASH_SPACE + PLACE_CHAR_DES,//
			SLASH_SPACE + PLACE_CHAR_DU,//
			SLASH_SPACE + PLACE_CHAR_LA,//
			SLASH_SPACE + PLACE_CHAR_LE,//
			SLASH_SPACE + PLACE_CHAR_LES, //
			SLASH_SPACE + PLACE_CHAR_L //
	};

	private static final String PLACE_CHAR_SAINT = "saint";
	private static final String PLACE_CHAR_SAINT_REPLACEMENT = "st";

	private static final String PLACE_CHAR_AVE = "ave ";
	private static final String PLACE_CHAR_AVENUE = "avenue ";
	private static final String PLACE_CHAR_BOUL = "boul ";
	private static final String PLACE_CHAR_CH = "ch. ";
	private static final String PLACE_CHAR_METRO = "métro ";

	private static final String[] START_WITH_ST = new String[] { //
	PLACE_CHAR_AVE, //
			PLACE_CHAR_AVENUE, //
			PLACE_CHAR_BOUL, //
			PLACE_CHAR_CH, //
			PLACE_CHAR_METRO //
	};

	private static final String[] SPACE_ST = new String[] {//
	StringUtils.SPACE_STRING + PLACE_CHAR_AVE, //
			StringUtils.SPACE_STRING + PLACE_CHAR_AVENUE, //
			StringUtils.SPACE_STRING + PLACE_CHAR_BOUL,//
			StringUtils.SPACE_STRING + PLACE_CHAR_CH,//
			StringUtils.SPACE_STRING + PLACE_CHAR_METRO//
	};

	private static final String SLASH = "/";
	private static final Pattern CLEAN_SUBWAY = Pattern.compile("(m[é|e]tro)([^" + PARENTHESE1 + "]*)" + PARENTHESE1 + "([^" + SLASH + "]*)" + SLASH + "([^"
			+ PARENTHESE2 + "]*)" + PARENTHESE2);
	private static final String CLEAN_SUBWAY_REPLACEMENT = "$3 " + SLASH + " $4 " + PARENTHESE1 + "$2" + PARENTHESE2 + "";

	private static String cleanBixiBikeStationName(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		name = CLEAN_SLASHES.matcher(name).replaceAll(CLEAN_SLASHES_REPLACEMENT);
		name = name.toLowerCase(Locale.ENGLISH);
		name = CLEAN_SUBWAY.matcher(name).replaceAll(CLEAN_SUBWAY_REPLACEMENT);
		name = StringUtils.removeStartWith(name, START_WITH_CHARS, 0);
		name = StringUtils.replaceAll(name, SLASH_CHARS, SLASH_SPACE);
		name = StringUtils.removeStartWith(name, START_WITH_ST, 0);
		name = StringUtils.replaceAll(name, SPACE_ST, StringUtils.SPACE_STRING);
		name = name.replace(PLACE_CHAR_SAINT, PLACE_CHAR_SAINT_REPLACEMENT);
		return cleanBikeStationName(name);
	}

	private static class BixiBikeStationsDataHandler extends MTDefaultHandler {

		@Override
		public String getLogTag() {
			return TAG;
		}

		private static final String STATIONS = "stations";
		private static final String STATIONS_VERSION = "version"; // 2.0
		private static final String STATION = "station";
		private static final String ID = "id";
		private static final String NAME = "name";
		private static final String TERMINAL_NAME = "terminalName";
		private static final String LAST_COMM_WITH_SERVER = "lastCommWithServer";
		private static final String LAT = "lat";
		private static final String LONG = "long";
		private static final String INSTALLED = "installed";
		private static final String LOCKED = "locked";
		private static final String INSTALL_DATE = "installDate";
		private static final String REMOVAL_DATE = "removalDate";
		private static final String TEMPORARY = "temporary";
		private static final String PUBLIC = "public";
		private static final String NB_BIKES = "nbBikes";
		private static final String NB_EMPTY_DOCKS = "nbEmptyDocks";
		private static final String LATEST_UPDATE_TIME = "latestUpdateTime";

		private static final String SUPPORTED_VERSIONS = "2.0";

		private String currentLocalName = STATIONS;

		private Context context;
		private long newLastUpdateInMs;
		private long statusMaxValidityInMs;
		private long poiMaxValidityInMs;

		private HashSet<DefaultPOI> bikeStations = new HashSet<DefaultPOI>();

		private HashSet<POIStatus> bikeStationsStatus = new HashSet<POIStatus>();

		private StringBuilder currentBikeStationNameSb = new StringBuilder();
		private StringBuilder currentBikeStationTerminalNameSb = new StringBuilder();
		private StringBuilder currentBikeStationLastCommWithServerSb = new StringBuilder();
		private StringBuilder currentBikeStationLatSb = new StringBuilder();
		private StringBuilder currentBikeStationLngSb = new StringBuilder();
		private StringBuilder currentBikeStationInstalledSb = new StringBuilder();
		private StringBuilder currentBikeStationLockedSb = new StringBuilder();
		private StringBuilder currentBikeStationPublicSb = new StringBuilder();
		private StringBuilder currentBikeStationNbBikesSb = new StringBuilder();
		private StringBuilder currentBikeStationNbEmptyDocksSb = new StringBuilder();
		private StringBuilder currentBikeStationLatestUpdateTimeSb = new StringBuilder();
		private int value1Color;
		private int value1ColorBg;
		private int value2Color;
		private int value2ColorBg;

		public BixiBikeStationsDataHandler(Context context, long newLastUpdateInMs, long statusMaxValidityInMs, long poiMaxValidityInMs, int value1Color,
				int value1ColorBg, int value2Color, int value2ColorBg) {
			this.context = context;
			this.newLastUpdateInMs = newLastUpdateInMs;
			this.statusMaxValidityInMs = statusMaxValidityInMs;
			this.poiMaxValidityInMs = poiMaxValidityInMs;
			this.value1Color = value1Color;
			this.value1ColorBg = value1ColorBg;
			this.value2Color = value2Color;
			this.value2ColorBg = value2ColorBg;
		}

		public HashSet<DefaultPOI> getBikeStations() {
			return this.bikeStations;
		}

		public Collection<POIStatus> getBikeStationsStatus() {
			return bikeStationsStatus;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			this.currentLocalName = localName;
			if (STATIONS.equals(localName)) {
				String version = attributes.getValue(STATIONS_VERSION);
				if (version == null || !SUPPORTED_VERSIONS.equals(version)) {
					MTLog.w(this, "XML version '%s' not supported!", version);
				}
			} else if (STATION.equals(localName)) {
				this.currentBikeStationNameSb.setLength(0); // reset
				this.currentBikeStationTerminalNameSb.setLength(0); // reset
				this.currentBikeStationLastCommWithServerSb.setLength(0); // reset
				this.currentBikeStationLatSb.setLength(0); // reset
				this.currentBikeStationLngSb.setLength(0); // reset
				this.currentBikeStationInstalledSb.setLength(0); // reset
				this.currentBikeStationLockedSb.setLength(0); // reset
				this.currentBikeStationPublicSb.setLength(0); // reset
				this.currentBikeStationNbBikesSb.setLength(0); // reset
				this.currentBikeStationNbEmptyDocksSb.setLength(0); // reset
				this.currentBikeStationLatestUpdateTimeSb.setLength(0); // reset
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);
			try {
				String string = new String(ch, start, length);
				try {
					if (NAME.equals(this.currentLocalName)) {
						this.currentBikeStationNameSb.append(string);
					} else if (TERMINAL_NAME.equals(this.currentLocalName)) {
						this.currentBikeStationTerminalNameSb.append(string);
					} else if (LAST_COMM_WITH_SERVER.equals(this.currentLocalName)) {
						this.currentBikeStationLastCommWithServerSb.append(string);
					} else if (LAT.equals(this.currentLocalName)) {
						this.currentBikeStationLatSb.append(string);
					} else if (LONG.equals(this.currentLocalName)) {
						this.currentBikeStationLngSb.append(string);
					} else if (INSTALLED.equals(this.currentLocalName)) {
						this.currentBikeStationInstalledSb.append(string);
					} else if (LOCKED.equals(this.currentLocalName)) {
						this.currentBikeStationLockedSb.append(string);
					} else if (PUBLIC.equals(this.currentLocalName)) {
						this.currentBikeStationPublicSb.append(string);
					} else if (NB_BIKES.equals(this.currentLocalName)) {
						this.currentBikeStationNbBikesSb.append(string);
					} else if (NB_EMPTY_DOCKS.equals(this.currentLocalName)) {
						this.currentBikeStationNbEmptyDocksSb.append(string);
					} else if (LATEST_UPDATE_TIME.equals(this.currentLocalName)) {
						this.currentBikeStationLatestUpdateTimeSb.append(string);
					} else if (ID.equals(this.currentLocalName)) { // no used
					} else if (INSTALL_DATE.equals(this.currentLocalName)) { // no used
					} else if (REMOVAL_DATE.equals(this.currentLocalName)) { // no used
					} else if (TEMPORARY.equals(this.currentLocalName)) { // no used
					} else {
						MTLog.w(this, "Unexpected name '%s'! while parsing '%s'", this.currentLocalName, string);
					}
				} catch (Exception e) {
					MTLog.w(this, e, "Error while parsing '%s' value '%s'!", this.currentLocalName, string);
				}
			} catch (Exception e) {
				MTLog.w(this, e, "Error while parsing '%s' value '%s, %s, %s' !", this.currentLocalName, ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (STATION.equals(localName)) {
				try {
					if (!TextUtils.isEmpty(this.currentBikeStationLastCommWithServerSb)) {
						long lastComWithServerInMs = Long.valueOf(this.currentBikeStationLastCommWithServerSb.toString());
						if (lastComWithServerInMs + this.poiMaxValidityInMs < this.newLastUpdateInMs) {
							return;
						}
					}
					if (!TextUtils.isEmpty(this.currentBikeStationLatestUpdateTimeSb)) {
						long latestUpdateTimeInMs = Long.valueOf(this.currentBikeStationLatestUpdateTimeSb.toString());
						if (latestUpdateTimeInMs + this.poiMaxValidityInMs < this.newLastUpdateInMs) {
							return;
						}
					}
					if (TextUtils.isEmpty(this.currentBikeStationTerminalNameSb) || !TextUtils.isDigitsOnly(this.currentBikeStationTerminalNameSb)) {
						return;
					}
					DefaultPOI newBikeStation = new DefaultPOI(getAUTHORITY(this.context), getAGENCY_TYPE_ID(this.context), POI.ITEM_VIEW_TYPE_BASIC_POI,
							POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT, POI.ITEM_ACTION_TYPE_FAVORITABLE);
					BikeStationAvailabilityPercent newBikeStationStatus = new BikeStationAvailabilityPercent(null, this.newLastUpdateInMs,
							this.statusMaxValidityInMs, this.newLastUpdateInMs, this.value1Color, this.value1ColorBg, this.value2Color, this.value2ColorBg);
					newBikeStation.setName(cleanBixiBikeStationName(this.currentBikeStationNameSb.toString()));
					newBikeStation.setId(Integer.parseInt(this.currentBikeStationTerminalNameSb.toString()));
					newBikeStation.setLat(Double.valueOf(this.currentBikeStationLatSb.toString()));
					newBikeStation.setLng(Double.valueOf(this.currentBikeStationLngSb.toString()));
					newBikeStationStatus.setStatusInstalled(Boolean.parseBoolean(this.currentBikeStationInstalledSb.toString()));
					newBikeStationStatus.setStatusLocked(Boolean.parseBoolean(this.currentBikeStationLockedSb.toString()));
					newBikeStationStatus.setStatusPublic(Boolean.parseBoolean(this.currentBikeStationPublicSb.toString()));
					newBikeStationStatus.setValue1(Integer.parseInt(this.currentBikeStationNbBikesSb.toString()));
					newBikeStationStatus.setValue2(Integer.parseInt(this.currentBikeStationNbEmptyDocksSb.toString()));
					if (newBikeStationStatus.getTotalValue() == 0) {
						newBikeStationStatus.setStatusInstalled(false);
					}
					this.bikeStations.add(newBikeStation);
					if (newBikeStationStatus != null) {
						newBikeStationStatus.setTargetUUID(newBikeStation.getUUID());
						this.bikeStationsStatus.add(newBikeStationStatus);
					}
				} catch (Exception e) {
					MTLog.w(this, e, "Error while parsing data!");
				}
			}
		}
	}
}
