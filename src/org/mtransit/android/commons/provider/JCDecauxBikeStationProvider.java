package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashSet;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.R;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.BikeStationAvailabilityPercent;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;

@SuppressLint("Registered")
public class JCDecauxBikeStationProvider extends BikeStationProvider {

	private static final String TAG = JCDecauxBikeStationProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link JCDecauxBikeStationProvider} implementations in same app.
	 */
	private static final String PREF_KEY_LAST_UPDATE_MS = BikeStationDbHelper.PREF_KEY_LAST_UPDATE_MS;

	private static String jcdecauxApiKey = null;

	/**
	 * Override if multiple {@link BikeStationProvider} implementations in same app.
	 */
	public static String getJCDECAUX_API_KEY(Context context) {
		if (jcdecauxApiKey == null) {
			jcdecauxApiKey = context.getResources().getString(R.string.jcdecaux_api_key);
		}
		return jcdecauxApiKey;
	}

	@Override
	public void updateBikeStationDataIfRequired() {
		long lastUpdateInMs = getLastUpdateInMs(); // POI
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + getPOIMaxValidityInMs() < nowInMs) {
			deleteAllBikeStationData();
			updateAllDataFromWWW(lastUpdateInMs);
			return;
		}
		if (lastUpdateInMs + getPOIValidityInMs() < nowInMs) {
			updateAllDataFromWWW(lastUpdateInMs);
		}
	}

	@Override
	public long getLastUpdateInMs() { // POI & Status
		return PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0L);
	}

	public void setLastUpdateInMs(long newLastUpdateInMs) { // POI & Status
		PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, newLastUpdateInMs, true); // sync
	}

	@Override
	public Cursor getPOIBikeStations(POIProviderContract.Filter poiFilter) {
		updateBikeStationDataIfRequired();
		return getPOIFromDB(poiFilter);
	}

	@Override
	public void updateBikeStationStatusDataIfRequired(StatusProviderContract.Filter statusFilter) {
		long lastUpdateInMs = getLastUpdateInMs(); // STATUS
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + getStatusMaxValidityInMs() < nowInMs) {
			deleteAllBikeStationStatusData();
			updateAllDataFromWWW(lastUpdateInMs);
			return;
		}
		if (lastUpdateInMs + getStatusValidityInMs(statusFilter.isInFocusOrDefault()) < nowInMs) {
			updateAllDataFromWWW(lastUpdateInMs);
		}
	}

	@Override
	public POIStatus getNewBikeStationStatus(AvailabilityPercent.AvailabilityPercentStatusFilter statusFilter) {
		updateBikeStationStatusDataIfRequired(statusFilter);
		return getCachedStatus(statusFilter);
	}

	private synchronized void updateAllDataFromWWW(long oldLastUpdatedInMs) {
		if (getLastUpdateInMs() > oldLastUpdatedInMs) { // POI & STATUS
			return; // too late, another thread already updated
		}
		loadDataFromWWW(0); // 0 = 1st try
	}

	private static final int MAX_RETRY = 1;

	private static final String AND = "&";
	private static final String QUESTION_MARK = "?";
	private static final String EQ = "=";

	private static final String API_KEY_URL_PARAM = "apiKey";
	private static final String STATION_STATUS_CLOSED = "CLOSED";

	private static final String JSON_LAST_UPDATE = "last_update";
	private static final String JSON_AVAILABLE_BIKE_STANDS = "available_bike_stands";
	private static final String JSON_AVAILABLE_BIKES = "available_bikes";
	private static final String JSON_STATUS = "status";
	private static final String JSON_POSITION = "position";
	private static final String JSON_POSITION_LNG = "lng";
	private static final String JSON_POSITION_LAT = "lat";
	private static final String JSON_NAME = "name";
	private static final String JSON_NUMBER = "number";

	private HashSet<DefaultPOI> loadDataFromWWW(int tried) {
		try {
			String urlString = getDATA_URL(getContext());
			StringBuilder urlSb = new StringBuilder(urlString);
			urlSb.append(urlString.contains(QUESTION_MARK) ? AND : QUESTION_MARK).append(API_KEY_URL_PARAM).append(EQ)
					.append(getJCDECAUX_API_KEY(getContext()));
			URL url = new URL(urlSb.toString());
			URLConnection urlc = url.openConnection();
			HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlc;
			switch (httpsUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				HashSet<DefaultPOI> newBikeStations = new HashSet<DefaultPOI>();
				HashSet<POIStatus> newBikeStationStatus = new HashSet<POIStatus>();
				String authority = getAUTHORITY(getContext());
				int dataSourceTypeId = getAGENCY_TYPE_ID(getContext());
				long poiMaxValidityInMs = getPOIMaxValidityInMs();
				long statusMaxValidityInMs = getStatusMaxValidityInMs();
				int value1Color = getValue1Color(getContext());
				int value1ColorBg = getValue1ColorBg(getContext());
				int value2Color = getValue2Color(getContext());
				int value2ColorBg = getValue2ColorBg(getContext());
				JSONArray json = new JSONArray(jsonString);
				for (int l = 0; l < json.length(); l++) {
					JSONObject jStation = json.getJSONObject(l);
					try {
						long lastUpdateInMs = jStation.getLong(JSON_LAST_UPDATE);
						if (lastUpdateInMs + poiMaxValidityInMs < newLastUpdateInMs) {
							continue; // skip
						}
						DefaultPOI newBikeStation = new DefaultPOI(authority, dataSourceTypeId, POI.ITEM_VIEW_TYPE_BASIC_POI,
								POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT, POI.ITEM_ACTION_TYPE_FAVORITABLE);
						newBikeStation.setId(jStation.getInt(JSON_NUMBER));
						newBikeStation.setName(cleanJCDecauxBikeStationName(jStation.getString(JSON_NAME)));
						JSONObject jStationPosition = jStation.getJSONObject(JSON_POSITION);
						newBikeStation.setLat(jStationPosition.getDouble(JSON_POSITION_LAT));
						newBikeStation.setLng(jStationPosition.getDouble(JSON_POSITION_LNG));
						newBikeStations.add(newBikeStation);
						BikeStationAvailabilityPercent newStatus = new BikeStationAvailabilityPercent(null, newLastUpdateInMs, statusMaxValidityInMs,
								newLastUpdateInMs, value1Color, value1ColorBg, value2Color, value2ColorBg);
						newStatus.setStatusClosed(STATION_STATUS_CLOSED.equalsIgnoreCase(jStation.getString(JSON_STATUS)));
						newStatus.setValue1(jStation.getInt(JSON_AVAILABLE_BIKES)); // bikes
						newStatus.setValue2(jStation.getInt(JSON_AVAILABLE_BIKE_STANDS)); // docks
						newStatus.setTargetUUID(newBikeStation.getUUID());
						newBikeStationStatus.add(newStatus);
					} catch (Exception e) {
						MTLog.w(this, e, "Error while parsing stations JSON '%s'!", jStation);
					}
				}
				deleteAllBikeStationData();
				POIProvider.insertDefaultPOIs(this, newBikeStations);
				deleteAllBikeStationStatusData();
				StatusProvider.cacheAllStatusesBulkLockDB(this, newBikeStationStatus);
				setLastUpdateInMs(newLastUpdateInMs); // POI & STATUS
				return newBikeStations;
			default:
				MTLog.w(this, "ERROR: HTTP URL-Connection Response Code %s (Message: %s)", httpsUrlConnection.getResponseCode(),
						httpsUrlConnection.getResponseMessage());
				if (tried < MAX_RETRY) {
					return loadDataFromWWW(++tried);
				} else {
					return null;
				}
			}
		} catch (SSLHandshakeException sslhe) {
			MTLog.w(this, sslhe, "SSL error!");
			if (tried < MAX_RETRY) {
				return loadDataFromWWW(++tried);
			} else {
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
			if (tried < MAX_RETRY) {
				return loadDataFromWWW(++tried);
			} else {
				return null;
			}
		}
	}

	private String cleanJCDecauxBikeStationName(String name) {
		if (name == null || name.length() == 0) {
			return name;
		}
		name = name.substring(8);
		return cleanBikeStationName(name);
	}
}
