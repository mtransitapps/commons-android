package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.regex.Pattern;

import javax.net.ssl.SSLHandshakeException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.BikeStationAvailabilityPercent;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

@SuppressLint("Registered")
public class SmooveBikeStationProvider extends BikeStationProvider {

	private static final String LOG_TAG = SmooveBikeStationProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	/**
	 * Override if multiple {@link SmooveBikeStationProvider} implementations in same app.
	 */
	private static final String PREF_KEY_LAST_UPDATE_MS = BikeStationDbHelper.PREF_KEY_LAST_UPDATE_MS;

	@Override
	public void updateBikeStationDataIfRequired() {
		MTLog.d(this, "updateBikeStationDataIfRequired()");
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
		return PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0L);
	}

	@Override
	public Cursor getPOIBikeStations(@Nullable POIProviderContract.Filter poiFilter) {
		MTLog.d(this, "getPOIBikeStations(%s)", poiFilter);
		updateBikeStationDataIfRequired();
		return getPOIFromDB(poiFilter);
	}

	@Override
	public void updateBikeStationStatusDataIfRequired(@NonNull StatusProviderContract.Filter statusFilter) {
		MTLog.d(this, "updateBikeStationStatusDataIfRequired(%s)", statusFilter);
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
	public POIStatus getNewBikeStationStatus(@NonNull AvailabilityPercent.AvailabilityPercentStatusFilter statusFilter) {
		MTLog.d(this, "getNewBikeStationStatus(%s)", statusFilter);
		updateBikeStationStatusDataIfRequired(statusFilter);
		return getCachedStatus(statusFilter);
	}

	private synchronized void updateAllDataFromWWW(long oldLastUpdatedInMs) {
		MTLog.d(this, "updateAllDataFromWWW(%s)", oldLastUpdatedInMs);
		if (getLastUpdateInMs() > oldLastUpdatedInMs) {
			MTLog.d(this, "updateAllDataFromWWW() > SKIP (too late, another thread already updated)");
			return; // too late, another thread already updated
		}
		MTLog.d(this, "updateAllDataFromWWW() > loadDataFromWWW()");
		loadDataFromWWW();
		MTLog.d(this, "updateAllDataFromWWW() > DONE");
	}

	public static final String JSON_RESULT = "result";
	public static final String JSON_OPERATIVE = "operative";
	public static final String JSON_COORDINATES = "coordinates";
	public static final String JSON_NAME = "name";
	public static final String JSON_AVL_BIKES = "avl_bikes";
	public static final String JSON_FREE_SLOTS = "free_slots";

	private HashSet<DefaultPOI> loadDataFromWWW() {
		MTLog.d(this, "loadDataFromWWW()");
		try {
			String urlString = getDATA_URL(requireContext());
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			urlc.addRequestProperty("Cache-Control", "no-cache"); // IMPORTANT!
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "#ServiceUpdate json: %s", jsonString);
				HashSet<DefaultPOI> newBikeStations = new HashSet<>();
				HashSet<POIStatus> newBikeStationStatus = new HashSet<>();
				parseJson(jsonString, newLastUpdateInMs, newBikeStations, newBikeStationStatus);
				deleteAllBikeStationData();
				POIProvider.insertDefaultPOIs(this, newBikeStations);
				deleteAllBikeStationStatusData();
				StatusProvider.cacheAllStatusesBulkLockDB(this, newBikeStationStatus);
				PreferenceUtils.savePrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, newLastUpdateInMs, true); // sync
				return newBikeStations;
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
			MTLog.w(this, se, "No Internet Connection!");
			return null;
		} catch (Exception e) {
			MTLog.e(this, e, "INTERNAL ERROR: Unknown Exception");
			return null;
		}
	}

	private void parseJson(String jsonString, long newLastUpdateInMs, HashSet<DefaultPOI> newBikeStations, HashSet<POIStatus> newBikeStationStatus) {
		try {
			Context context = requireContext();
			String authority = getAUTHORITY(context);
			int dataSourceTypeId = getAGENCY_TYPE_ID(context);
			long statusMaxValidityInMs = getStatusMaxValidityInMs();
			int value1Color = getValue1Color(context);
			int value1ColorBg = getValue1ColorBg(context);
			int value2Color = getValue2Color(context);
			int value2ColorBg = getValue2ColorBg(context);
			JSONObject json = new JSONObject(jsonString);
			if (json.has(JSON_RESULT)) {
				JSONArray jStationBeanList = json.getJSONArray(JSON_RESULT);
				for (int l = 0; l < jStationBeanList.length(); l++) {
					JSONObject jStation = jStationBeanList.getJSONObject(l);
					parseJsonStation(newLastUpdateInMs, newBikeStations, newBikeStationStatus, authority, dataSourceTypeId, statusMaxValidityInMs, value1Color,
							value1ColorBg, value2Color, value2ColorBg, jStation);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
		}
	}

	private static final Pattern STATION_ID = Pattern.compile("^[\\d]{4}");

	private void parseJsonStation(long newLastUpdateInMs, HashSet<DefaultPOI> newBikeStations, HashSet<POIStatus> newBikeStationStatus, String authority,
			int dataSourceTypeId, long statusMaxValidityInMs, int value1Color, int value1ColorBg, int value2Color, int value2ColorBg,
			JSONObject jStation) {
		try {
			boolean operative = jStation.optBoolean(JSON_OPERATIVE, false);
			if (!operative) {
				MTLog.d(this, "parseJsonStation() > SKIP non operative station: %s.", jStation);
				return;
			}
			DefaultPOI newBikeStation = new DefaultPOI(authority, dataSourceTypeId, POI.ITEM_VIEW_TYPE_BASIC_POI, POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT,
					POI.ITEM_ACTION_TYPE_FAVORITABLE);
			String name = jStation.getString(JSON_NAME);
			String coordinatesS = jStation.getString(JSON_COORDINATES);
			String[] coordinates = coordinatesS.split(",");
			String latS = coordinates[0].trim();
			String lngS = coordinates[1].trim();
			int bikeStationId = Integer.parseInt(name.substring(0, 4));
			newBikeStation.setId(bikeStationId);
			String bikeStationName = name.substring(5);
			newBikeStation.setName(cleanBikeStationName(bikeStationName));
			newBikeStation.setLat(Double.parseDouble(latS));
			newBikeStation.setLng(Double.parseDouble(lngS));
			newBikeStations.add(newBikeStation);
			BikeStationAvailabilityPercent newStatus =
					new BikeStationAvailabilityPercent(null, newLastUpdateInMs, statusMaxValidityInMs, newLastUpdateInMs, value1Color, value1ColorBg,
							value2Color, value2ColorBg);
			newStatus.setValue1(jStation.getInt(JSON_AVL_BIKES)); // bikes
			newStatus.setValue2(jStation.getInt(JSON_FREE_SLOTS)); // docks
			newStatus.setTargetUUID(newBikeStation.getUUID());
			newBikeStationStatus.add(newStatus);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing station JSON '%s'!", jStation);
		}
	}
}
