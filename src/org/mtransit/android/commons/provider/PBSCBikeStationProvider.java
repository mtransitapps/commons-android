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
import org.json.JSONException;
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

@SuppressLint("Registered")
public class PBSCBikeStationProvider extends BikeStationProvider {

	private static final String LOG_TAG = PBSCBikeStationProvider.class.getSimpleName();

	@Override
	public String getLogTag() {
		return LOG_TAG;
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
	@Nullable
	private static ThreadSafeDateFormatter dateTimeFormat;

	@NonNull
	public static ThreadSafeDateFormatter getDateTimeFormat(Context context) {
		if (dateTimeFormat == null) {
			dateTimeFormat = new ThreadSafeDateFormatter(DATE_TIME_FORMAT_PATTERN);
			dateTimeFormat.setTimeZone(TimeZone.getTimeZone(getTIME_ZONE(context)));
		}
		return dateTimeFormat;
	}

	@Nullable
	private HashSet<DefaultPOI> loadDataFromWWW() {
		MTLog.d(this, "loadDataFromWWW()");
		try {
			Context context = getContext();
			if (context == null) {
				return null;
			}
			String urlString = getDATA_URL(context);
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
				parseAgencyJSON(context, newLastUpdateInMs, jsonString, newBikeStations, newBikeStationStatus);
				deleteAllBikeStationData();
				POIProvider.insertDefaultPOIs(this, newBikeStations);
				deleteAllBikeStationStatusData();
				StatusProvider.cacheAllStatusesBulkLockDB(this, newBikeStationStatus);
				PreferenceUtils.savePrefLcl(context, PREF_KEY_LAST_UPDATE_MS, newLastUpdateInMs, true); // sync
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

	private static final String JSON_STATION_BEAN_LIST = "stationBeanList";
	private static final String JSON_LAST_COMMUNICATION_TIME = "lastCommunicationTime";
	private static final String JSON_ID = "id";
	private static final String JSON_STATION_NAME = "stationName";
	private static final String JSON_LATITUDE = "latitude";
	private static final String JSON_LONGITUDE = "longitude";
	private static final String JSON_AVAILABLE_BIKES = "availableBikes";
	private static final String JSON_AVAILABLE_DOCKS = "availableDocks";
	private static final String JSON_STATUS = "status";

	private static final String STATUS_END_OF_LIFE = "END_OF_LIFE";
	private static final String STATUS_PLANNED = "PLANNED";
	private static final String STATUS_IN_SERVICE = "IN_SERVICE";

	private void parseAgencyJSON(@NonNull Context context, long newLastUpdateInMs, String jsonString, HashSet<DefaultPOI> newBikeStations, HashSet<POIStatus> newBikeStationStatus) {
		try {
			String authority = getAUTHORITY(context);
			int dataSourceTypeId = getAGENCY_TYPE_ID(context);
			long poiMaxValidityInMs = getPOIMaxValidityInMs();
			long statusMaxValidityInMs = getStatusMaxValidityInMs();
			int value1Color = getValue1Color(context);
			int value1ColorBg = getValue1ColorBg(context);
			int value2Color = getValue2Color(context);
			int value2ColorBg = getValue2ColorBg(context);
			JSONObject json = new JSONObject(jsonString);
			if (json.has(JSON_STATION_BEAN_LIST)) {
				JSONArray jStationBeanList = json.getJSONArray(JSON_STATION_BEAN_LIST);
				for (int l = 0; l < jStationBeanList.length(); l++) {
					JSONObject jStation = jStationBeanList.getJSONObject(l);
					try {
						String jStatus = jStation.optString(JSON_STATUS);
						if (STATUS_END_OF_LIFE.equals(jStatus)
								|| STATUS_PLANNED.equals(jStatus)) {
							continue; // skip
						}
						if (!STATUS_IN_SERVICE.equals(jStatus)) {
							MTLog.d(this, "parseAgencyJSON() > Unexpected status '%s'.", jStatus);
						}
						String lastCommunicationTimeS = jStation.getString(JSON_LAST_COMMUNICATION_TIME);
						if (TextUtils.isEmpty(lastCommunicationTimeS)) {
							continue; // skip
						}
						long lastCommunicationTime = getDateTimeFormat(context).parseThreadSafe(lastCommunicationTimeS).getTime();
						if (lastCommunicationTime + poiMaxValidityInMs < newLastUpdateInMs) {
							continue; // skip
						}
						DefaultPOI newBikeStation =
								new DefaultPOI(authority, dataSourceTypeId, POI.ITEM_VIEW_TYPE_BASIC_POI, POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT,
										POI.ITEM_ACTION_TYPE_FAVORITABLE);
						newBikeStation.setId(jStation.getInt(JSON_ID));
						newBikeStation.setName(cleanBikeStationName(jStation.getString(JSON_STATION_NAME)));
						newBikeStation.setLat(jStation.getDouble(JSON_LATITUDE));
						newBikeStation.setLng(jStation.getDouble(JSON_LONGITUDE));
						newBikeStations.add(newBikeStation);
						BikeStationAvailabilityPercent newStatus =
								new BikeStationAvailabilityPercent(null, newLastUpdateInMs, statusMaxValidityInMs, newLastUpdateInMs, value1Color,
						newStatus.setValue1(jStation.getInt(JSON_AVAILABLE_BIKES)); // bikes
						newStatus.setValue2(jStation.getInt(JSON_AVAILABLE_DOCKS)); // docks
						newStatus.setTargetUUID(newBikeStation.getUUID());
						newBikeStationStatus.add(newStatus);
					} catch (Exception e) {
						MTLog.w(this, e, "Error while parsing stations JSON '%s'!", jStation);
					}
				}
			}
		} catch (JSONException jsone) {
			MTLog.w(this, jsone, "Error while parsing JSON '%s'!", jsonString);
		}
	}
}


