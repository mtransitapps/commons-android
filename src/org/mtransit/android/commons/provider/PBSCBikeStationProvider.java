package org.mtransit.android.commons.provider;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.TimeZone;

import javax.net.ssl.SSLHandshakeException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.ThreadSafeDateFormatter;
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
public class PBSCBikeStationProvider extends BikeStationProvider {

	private static final String TAG = PBSCBikeStationProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	/**
	 * Override if multiple {@link PBSCBikeStationProvider} implementations in same app.
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
		return PreferenceUtils.getPrefLcl(getContext(), PREF_KEY_LAST_UPDATE_MS, 0L);
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
		MTLog.d(this, "updateAllDataFromWWW(%s)", oldLastUpdatedInMs);
		if (getLastUpdateInMs() > oldLastUpdatedInMs) {
			return; // too late, another thread already updated
		}
		MTLog.d(this, "updateAllDataFromWWW() > loadDataFromWWW()");
		loadDataFromWWW();
		MTLog.d(this, "updateAllDataFromWWW() > DONE");
	}

	private static final String DATE_TIME_FORMAT_PATTERN = "yyyy-MM-dd' 'HH:mm:ss";
	private static ThreadSafeDateFormatter dateTimeFormat;

	public static ThreadSafeDateFormatter getDateTimeFormat(Context context) {
		if (dateTimeFormat == null) {
			dateTimeFormat = new ThreadSafeDateFormatter(DATE_TIME_FORMAT_PATTERN);
			dateTimeFormat.setTimeZone(TimeZone.getTimeZone(getTIME_ZONE(context)));
		}
		return dateTimeFormat;
	}

	private HashSet<DefaultPOI> loadDataFromWWW() {
		MTLog.d(this, "loadDataFromWWW()");
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
				String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "#ServiceUpdate json: %s", jsonString);
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
				JSONObject json = new JSONObject(jsonString);
				if (json.has("stationBeanList")) {
					JSONArray jStationBeanList = json.getJSONArray("stationBeanList");
					for (int l = 0; l < jStationBeanList.length(); l++) {
						JSONObject jStation = jStationBeanList.getJSONObject(l);
						try {
							String lastCommunicationTimeS = jStation.getString("lastCommunicationTime");
							long lastCommunicationTime = getDateTimeFormat(getContext()).parseThreadSafe(lastCommunicationTimeS).getTime();
							if (lastCommunicationTime + poiMaxValidityInMs < newLastUpdateInMs) {
								continue; // skip
							}
							DefaultPOI newBikeStation =
									new DefaultPOI(authority, dataSourceTypeId, POI.ITEM_VIEW_TYPE_BASIC_POI, POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT,
											POI.ITEM_ACTION_TYPE_FAVORITABLE);
							newBikeStation.setId(jStation.getInt("id"));
							newBikeStation.setName(cleanBikeStationName(jStation.getString("stationName")));
							newBikeStation.setLat(jStation.getDouble("latitude"));
							newBikeStation.setLng(jStation.getDouble("longitude"));
							newBikeStations.add(newBikeStation);
							BikeStationAvailabilityPercent newStatus =
									new BikeStationAvailabilityPercent(null, newLastUpdateInMs, statusMaxValidityInMs, newLastUpdateInMs, value1Color,
											value1ColorBg, value2Color, value2ColorBg);
							newStatus.setValue1(jStation.getInt("availableBikes")); // bikes
							newStatus.setValue2(jStation.getInt("availableDocks")); // docks
							newStatus.setTargetUUID(newBikeStation.getUUID());
							newBikeStationStatus.add(newStatus);
						} catch (Exception e) {
							MTLog.w(this, e, "Error while parsing stations JSON '%s'!", jStation);
						}
					}
				}
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
}


