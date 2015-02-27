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
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0l);
		long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + getBIKE_STATION_MAX_VALIDITY_IN_MS() < nowInMs) {
			deleteAllBikeStationData();
			updateAllDataFromWWW(lastUpdateInMs);
			return;
		}
		if (lastUpdateInMs + getBIKE_STATION_VALIDITY_IN_MS() < nowInMs) {
			updateAllDataFromWWW(lastUpdateInMs);
		}
	}

	@Override
	public Cursor getPOIBikeStations(POIFilter poiFilter) {
		updateBikeStationDataIfRequired();
		return getPOIFromDB(poiFilter);
	}

	@Override
	public void updateBikeStationStatusDataIfRequired(StatusFilter statusFilter) {
		long lastUpdateInMs = PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0l);
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
		if (PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0l) > oldLastUpdatedInMs) {
			return; // too late, another thread already updated
		}
		loadDataFromWWW(0); // 0 = 1st try
	}

	private static final int MAX_RETRY = 1;

	private static final String API_KEY_URL_PARAM = "apiKey";
	private static final String STATION_STATUS_CLOSED = "CLOSED";

	private HashSet<DefaultPOI> loadDataFromWWW(int tried) {
		try {
			String urlString = getDATA_URL(getContext());
			StringBuilder urlSb = new StringBuilder(urlString);
			urlSb.append(urlString.contains("?") ? "&" : "?").append(API_KEY_URL_PARAM).append("=").append(getJCDECAUX_API_KEY(getContext()));
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
				long poiMaxValidityInMs = getBIKE_STATION_MAX_VALIDITY_IN_MS();
				long statusMaxValidityInMs = getStatusMaxValidityInMs();
				int value1Color = getValue1Color(getContext());
				int value1ColorBg = getValue1ColorBg(getContext());
				int value2Color = getValue2Color(getContext());
				int value2ColorBg = getValue2ColorBg(getContext());
				JSONArray json = new JSONArray(jsonString);
				for (int l = 0; l < json.length(); l++) {
					JSONObject jStation = json.getJSONObject(l);
					try {
						long lastUpdateInMs = jStation.getLong("last_update");
						if (lastUpdateInMs + poiMaxValidityInMs < newLastUpdateInMs) {
							continue; // skip
						}
						DefaultPOI newBikeStation = new DefaultPOI(authority, dataSourceTypeId, POI.ITEM_VIEW_TYPE_BASIC_POI,
								POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT, POI.ITEM_ACTION_TYPE_FAVORITABLE);
						newBikeStation.setId(jStation.getInt("number"));
						newBikeStation.setName(cleanJCDecauxBikeStationName(jStation.getString("name")));
						JSONObject jStationPosition = jStation.getJSONObject("position");
						newBikeStation.setLat(jStationPosition.getDouble("lat"));
						newBikeStation.setLng(jStationPosition.getDouble("lng"));
						newBikeStations.add(newBikeStation);
						BikeStationAvailabilityPercent newStatus = new BikeStationAvailabilityPercent(null, newLastUpdateInMs, statusMaxValidityInMs,
								newLastUpdateInMs, value1Color, value1ColorBg, value2Color, value2ColorBg);
						newStatus.setStatusClosed(STATION_STATUS_CLOSED.equalsIgnoreCase(jStation.getString("status")));
						newStatus.setValue1(jStation.getInt("available_bikes")); // bikes
						newStatus.setValue2(jStation.getInt("available_bike_stands")); // docks
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
				PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, newLastUpdateInMs, true); // sync
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
