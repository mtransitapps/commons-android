package org.mtransit.android.commons.provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.JSONUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.PreferenceUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.AvailabilityPercent;
import org.mtransit.android.commons.data.BikeStationAvailabilityPercent;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

@SuppressLint("Registered")
public class GBFSProvider extends BikeStationProvider {

	private static final String LOG_TAG = GBFSProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	/**
	 * Override if multiple {@link GBFSProvider} implementations in same app.
	 */
	private static final String PREF_KEY_LAST_UPDATE_MS = BikeStationDbHelper.PREF_KEY_LAST_UPDATE_MS;

	/**
	 * Override if multiple {@link GBFSProvider} implementations in same app.
	 */
	private static final String PREF_KEY_STATUS_LAST_UPDATE_MS = BikeStationDbHelper.PREF_KEY_STATUS_LAST_UPDATE_MS;

	@Nullable
	@Override
	public Cursor getPOIBikeStations(@Nullable POIProviderContract.Filter poiFilter) {
		updateBikeStationDataIfRequired();
		return getPOIFromDB(poiFilter);
	}

	@Override
	public long getLastUpdateInMs() { // POI
		return PreferenceUtils.getPrefLcl(requireContextCompat(), PREF_KEY_LAST_UPDATE_MS, 0L);
	}

	public void setLastUpdateInMs(long newLastUpdateInMs) { // POI
		PreferenceUtils.savePrefLclSync(requireContextCompat(), PREF_KEY_LAST_UPDATE_MS, newLastUpdateInMs);
	}

	private long getLastUpdateStatusInMs(@NonNull Context context) {
		return PreferenceUtils.getPrefLcl(context, PREF_KEY_STATUS_LAST_UPDATE_MS, 0L);
	}

	private void setLastUpdateStatusInMs(long newLastUpdateStatusInMs) {
		PreferenceUtils.savePrefLclSync(requireContextCompat(), PREF_KEY_STATUS_LAST_UPDATE_MS, newLastUpdateStatusInMs);
	}

	@Nullable
	@Override
	public POIStatus getNewBikeStationStatus(@NonNull AvailabilityPercent.AvailabilityPercentStatusFilter statusFilter) {
		updateBikeStationStatusDataIfRequired(statusFilter);
		return getCachedStatus(statusFilter);
	}

	@Override
	public void updateBikeStationDataIfRequired() {
		final long lastUpdateInMs = getLastUpdateInMs(); // POI
		final long nowInMs = TimeUtils.currentTimeMillis();
		// MAX VALIDITY (too old to display?)
		if (lastUpdateInMs + getPOIMaxValidityInMs() < nowInMs) { // too old to display
			deleteAllBikeStationData();
			updateBikeStationDataFromWWW(lastUpdateInMs);
			return;
		}
		if (lastUpdateInMs + getPOIValidityInMs() < nowInMs) { // try to update
			updateBikeStationDataFromWWW(lastUpdateInMs);
		}
	}

	@Override
	public void updateBikeStationStatusDataIfRequired(@NonNull StatusProviderContract.Filter statusFilter) {
		final long lastUpdateInMs = getLastUpdateStatusInMs(requireContextCompat()); // STATUS
		final long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + getStatusMaxValidityInMs() < nowInMs) { // too old too display?
			deleteAllBikeStationStatusData();
			updateBikeStationStatusDataFromWWW(requireContextCompat(), lastUpdateInMs);
			return;
		}
		if (lastUpdateInMs + getStatusValidityInMs(statusFilter.isInFocusOrDefault()) < nowInMs) { // try to refresh?
			updateBikeStationStatusDataFromWWW(requireContextCompat(), lastUpdateInMs);
		}
	}

	private synchronized void updateBikeStationDataFromWWW(long oldLastUpdatedInMs) { // TODO remove synchronized!
		if (getLastUpdateInMs() > oldLastUpdatedInMs) { // POI
			MTLog.d(this, "updateBikeStationDataFromWWW() > SKIP (already updating/updated)");
			return; // too late, another thread already updated
		}
		loadBikeStationDataFromWWW();
	}

	private synchronized void updateBikeStationStatusDataFromWWW(@NonNull Context context, long oldLastUpdatedInMs) { // TODO remove synchronized!
		if (getLastUpdateStatusInMs(context) > oldLastUpdatedInMs) { // STATUS
			MTLog.d(this, "updateBikeStationStatusDataFromWWW() > SKIP (already updating/updated)");
			return; // too late, another thread already updated
		}
		loadBikeStationStatusDataFromWWW();
	}

	@SuppressWarnings("UnusedReturnValue")
	@Nullable
	private HashSet<DefaultPOI> loadBikeStationDataFromWWW() {
		try {
			final Context context = requireContextCompat();
			String urlString = getDATA_URL(context);
			urlString += "en/"; // TODO support fr...
			urlString += "station_information.json";
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadBikeStationDataFromWWW() > jsonString: %s.", jsonString);
				JStationInformation jStationInformation = parseAgencyJSONStationInformation(jsonString);
				HashSet<DefaultPOI> newBikeStations = parseAgencyJSONStations(context, jStationInformation.getData().getStations());
				MTLog.i(this, "Found %d stations.", newBikeStations.size());
				deleteAllBikeStationData();
				POIProvider.insertDefaultPOIs(this, newBikeStations);
				setLastUpdateInMs(newLastUpdateInMs); // POI
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

	@NonNull
	private HashSet<DefaultPOI> parseAgencyJSONStations(@NonNull Context context,
														@NonNull List<JStationInformation.JData.JStation> jStations) {
		HashSet<DefaultPOI> newBikeStations = new HashSet<>();
		try {
			String authority = getAUTHORITY(context);
			int dataSourceTypeId = getAGENCY_TYPE_ID(context);
			for (JStationInformation.JData.JStation jStation : jStations) {
				DefaultPOI newBikeStation = parseAgencyJSONStation(authority, dataSourceTypeId, jStation);
				if (newBikeStation != null) {
					newBikeStations.add(newBikeStation);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jStations);
		}
		return newBikeStations;
	}

	@Nullable
	private DefaultPOI parseAgencyJSONStation(String authority, int dataSourceTypeId, JStationInformation.JData.JStation jStation) {
		try {
			if (jStation.getCapacity() != null // capacity known
					&& jStation.getCapacity() <= 0) { // AND empty
				MTLog.d(this, "parseAgencyJSONStations() > SKIP empty station: %s.", jStation);
				return null;
			}
			if (jStation.getLat() == null || jStation.getLat() == 0.0d
					|| jStation.getLon() == null || jStation.getLon() == 0.0d) {
				return null;
			}
			String idString = jStation.getStationId();
			int bikeStationId = idString == null ? -1 : Integer.parseInt(idString);
			if (bikeStationId < 0) {
				return null;
			}
			DefaultPOI newBikeStation = new DefaultPOI(authority,
					dataSourceTypeId,
					POI.ITEM_VIEW_TYPE_BASIC_POI,
					POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT,
					POI.ITEM_ACTION_TYPE_FAVORITABLE
			);
			newBikeStation.setId(bikeStationId);
			newBikeStation.setName(cleanBikeStationName(jStation.getName()));
			newBikeStation.setLat(jStation.getLat());
			newBikeStation.setLng(jStation.getLon());
			return newBikeStation;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing station JSON '%s'!", jStation);
			return null;
		}
	}

	@NonNull
	private JStationInformation parseAgencyJSONStationInformation(@Nullable String jsonString) {
		ArrayList<JStationInformation.JData.JStation> stations = new ArrayList<>();
		try {
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			parseAgencyJSONStationInformationStations(stations, json);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
		}
		return new JStationInformation(new JStationInformation.JData(stations));
	}

	private static final String JSON_DATA = "data";
	private static final String JSON_STATIONS = "stations";
	private static final String JSON_STATION_ID = "station_id";
	private static final String JSON_NAME = "name";
	private static final String JSON_SHORT_NAME = "short_name";
	private static final String JSON_LAT = "lat";
	private static final String JSON_LON = "lon";
	private static final String JSON_CAPACITY = "capacity";
	private static final String JSON_NUM_BIKES_AVAILABLE = "num_bikes_available";
	private static final String JSON_NUM_EBIKES_AVAILABLE = "num_ebikes_available";
	private static final String JSON_NUM_BIKES_AVAILABLE_TYPES = "num_bikes_available_types";
	private static final String JSON_MECHANICAL = "mechanical";
	private static final String JSON_EBIKE = "ebike";
	private static final String JSON_NUM_DOCKS_AVAILABLE = "num_docks_available";
	private static final String JSON_IS_INSTALLED = "is_installed";
	private static final String JSON_IS_RENTING = "is_renting";
	private static final String JSON_IS_RETURNING = "is_returning";
	private static final String JSON_LAST_REPORTED = "last_reported";

	private void parseAgencyJSONStationInformationStations(@NonNull List<JStationInformation.JData.JStation> stations,
														   @Nullable JSONObject json) {
		try {
			if (json != null && json.has(JSON_DATA)) {
				JSONObject jData = json.optJSONObject(JSON_DATA);
				if (jData != null && jData.has(JSON_STATIONS)) {
					JSONArray jStations = jData.getJSONArray(JSON_STATIONS);
					for (int l = 0; l < jStations.length(); l++) {
						JSONObject jStation = jStations.getJSONObject(l);
						if (jStation != null) {
							stations.add(new JStationInformation.JData.JStation(
									jStation.optString(JSON_STATION_ID),
									jStation.optString(JSON_NAME),
									jStation.optString(JSON_SHORT_NAME),
									jStation.optDouble(JSON_LAT),
									jStation.optDouble(JSON_LON),
									jStation.optInt(JSON_CAPACITY)
							));
						}
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", json);
		}
	}

	@SuppressWarnings("UnusedReturnValue")
	@Nullable
	private HashSet<POIStatus> loadBikeStationStatusDataFromWWW() {
		try {
			final Context context = requireContextCompat();
			String urlString = getDATA_URL(context);
			urlString += "en/"; // TODO support fr...
			urlString += "station_status.json";
			MTLog.i(this, "Loading from '%s'...", urlString);
			URL url = new URL(urlString);
			URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadBikeStationStatusDataFromWWW() > jsonString: %s.", jsonString);
				JStationStatus jStationStatus = parseAgencyJSONStationStatus(jsonString);
				HashSet<POIStatus> newBikeStationStatus = parseAgencyJSONStationsStatus(context, jStationStatus.getData().getStations(), newLastUpdateInMs);
				MTLog.i(this, "Found %d statuses.", newBikeStationStatus.size());
				deleteAllBikeStationStatusData();
				StatusProvider.cacheAllStatusesBulkLockDB(this, newBikeStationStatus);
				setLastUpdateStatusInMs(newLastUpdateInMs);
				return newBikeStationStatus;
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

	@NonNull
	private HashSet<POIStatus> parseAgencyJSONStationsStatus(@NonNull Context context,
															 @NonNull List<JStationStatus.JData.JStation> jStations,
															 long newLastUpdateInMs) {
		HashSet<POIStatus> newBikeStationStatuses = new HashSet<>();
		try {
			String authority = getAUTHORITY(context);
			long statusMaxValidityInMs = getStatusMaxValidityInMs();
			int value1Color = getValue1Color(context);
			int value1ColorBg = getValue1ColorBg(context);
			int value1SubValue1Color = getValue1SubValue1Color(context);
			int value1SubValue1ColorBg = getValue1SubValue1ColorBg(context);
			int value2Color = getValue2Color(context);
			int value2ColorBg = getValue2ColorBg(context);
			for (JStationStatus.JData.JStation jStation : jStations) {
				POIStatus newBikeStationStatus = parseAgencyJSONStationStatus(
						authority, jStation, newLastUpdateInMs,
						statusMaxValidityInMs,
						value1Color, value1ColorBg,
						value1SubValue1Color, value1SubValue1ColorBg,
						value2Color, value2ColorBg);
				if (newBikeStationStatus != null) {
					newBikeStationStatuses.add(newBikeStationStatus);
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jStations);
		}
		return newBikeStationStatuses;
	}

	@Nullable
	private POIStatus parseAgencyJSONStationStatus(@NonNull String authority,
												   @NonNull JStationStatus.JData.JStation jStation,
												   long newLastUpdateInMs, long statusMaxValidityInMs,
												   int value1Color, int value1ColorBg,
												   int value1SubValue1Color, int value1SubValue1ColorBg,
												   int value2Color, int value2ColorBg) {
		try {
			String idString = jStation.getStationId();
			int bikeStationId = idString == null ? -1 : Integer.parseInt(idString);
			if (bikeStationId < 0) {
				return null;
			}
			BikeStationAvailabilityPercent newBikeStationStatus =
					new BikeStationAvailabilityPercent(
							POI.POIUtils.getUUID(authority, bikeStationId),
							newLastUpdateInMs,
							statusMaxValidityInMs,
							newLastUpdateInMs,
							value1Color,
							value1ColorBg,
							value1SubValue1Color,
							value1SubValue1ColorBg,
							value2Color,
							value2ColorBg);
			boolean isInstalled = Boolean.TRUE.equals(jStation.getIsInstalled());
			boolean isRenting = Boolean.TRUE.equals(jStation.getIsRenting());
			boolean isReturning = Boolean.TRUE.equals(jStation.getIsReturning());
			newBikeStationStatus.setStatusInstalled(isInstalled);
			int numBikesAvailable = jStation.getNumBikesAvailable() == null ? 0 : jStation.getNumBikesAvailable();
			if (!isRenting) {
				numBikesAvailable = 0; // not renting = no bikes available
			}
			Integer numEBikesAvailable = jStation.getNumEBikesAvailable();
			if (numEBikesAvailable == null) {
				if (jStation.getNumBikesAvailableTypes() != null
						&& jStation.getNumBikesAvailableTypes().getEBikes() != null) {
					numEBikesAvailable = jStation.getNumBikesAvailableTypes().getEBikes();
				}
			}
			int numDocksAvailable = jStation.getNumDocksAvailable() == null ? 0 : jStation.getNumDocksAvailable();
			if (!isReturning) {
				numDocksAvailable = 0; // not returning = no docks available
			}
			newBikeStationStatus.setValue1(numBikesAvailable);
			newBikeStationStatus.setValue1SubValue1(numEBikesAvailable);
			newBikeStationStatus.setValue2(numDocksAvailable);
			if (newBikeStationStatus.getTotalValue() == 0) {
				newBikeStationStatus.setStatusInstalled(false);
			}
			return newBikeStationStatus;
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing station JSON '%s'!", jStation);
			return null;
		}
	}

	@NonNull
	private JStationStatus parseAgencyJSONStationStatus(@Nullable String jsonString) {
		ArrayList<JStationStatus.JData.JStation> stations = new ArrayList<>();
		try {
			JSONObject json = jsonString == null ? null : new JSONObject(jsonString);
			parseAgencyJSONStationStatusStations(stations, json);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
		}
		return new JStationStatus(new JStationStatus.JData(stations));
	}

	private void parseAgencyJSONStationStatusStations(List<JStationStatus.JData.JStation> stations, JSONObject json) {
		try {
			if (json != null && json.has(JSON_DATA)) {
				JSONObject jData = json.optJSONObject(JSON_DATA);
				if (jData != null && jData.has(JSON_STATIONS)) {
					JSONArray jStations = jData.getJSONArray(JSON_STATIONS);
					for (int l = 0; l < jStations.length(); l++) {
						JSONObject jStation = jStations.getJSONObject(l);
						if (jStation != null) {
							JStationStatus.JData.JStation.JNumBikesAvailableTypes numBikesAvailableTypes = null;
							JSONObject jNumBikesAvailableTypes = jStation.optJSONObject(JSON_NUM_BIKES_AVAILABLE_TYPES);
							if (jNumBikesAvailableTypes != null) {
								numBikesAvailableTypes = new JStationStatus.JData.JStation.JNumBikesAvailableTypes(
										JSONUtils.optInt(jNumBikesAvailableTypes, JSON_MECHANICAL),
										JSONUtils.optInt(jNumBikesAvailableTypes, JSON_EBIKE)
								);
							}
							stations.add(new JStationStatus.JData.JStation(
									jStation.optString(JSON_STATION_ID),
									jStation.optInt(JSON_NUM_BIKES_AVAILABLE),
									JSONUtils.optInt(jStation, JSON_NUM_EBIKES_AVAILABLE),
									numBikesAvailableTypes,
									jStation.optInt(JSON_NUM_DOCKS_AVAILABLE),
									getIntOrBoolean(jStation, JSON_IS_INSTALLED),
									getIntOrBoolean(jStation, JSON_IS_RENTING),
									getIntOrBoolean(jStation, JSON_IS_RETURNING),
									jStation.optLong(JSON_LAST_REPORTED)
							));
						}
					}
				}
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", json);
		}
	}

	@Nullable
	private static Boolean getIntOrBoolean(@NonNull JSONObject jStation, @NonNull String key) {
		return getIntOrBoolean(jStation.opt(key), key);
	}

	@Nullable
	protected static Boolean getIntOrBoolean(@Nullable Object value, @NonNull String key) {
		if (value instanceof Boolean) {
			return (Boolean) value;
		} else if (value instanceof Integer) {
			final int intValue = (Integer) value;
			if (intValue == 0) {
				return false;
			} else if (intValue == 1) {
				return true;
			}
		}
		MTLog.w(LOG_TAG, "Unexpected int/boolean value '%s' for key '%s'!", key, value);
		return null;
	}

	@SuppressWarnings({"unused", "WeakerAccess"})
	private static class JStationInformation {
		@NonNull
		private final JData data;

		private JStationInformation(@NonNull JData data) {
			this.data = data;
		}

		@NonNull
		public JData getData() {
			return data;
		}

		@NonNull
		@Override
		public String toString() {
			return JStationInformation.class.getSimpleName() + "{" +
					"data=" + data +
					'}';
		}

		private static class JData {
			@NonNull
			private final List<JStation> stations;

			private JData(@NonNull List<JStation> stations) {
				this.stations = stations;
			}

			@NonNull
			public List<JStation> getStations() {
				return stations;
			}

			@NonNull
			@Override
			public String toString() {
				return JData.class.getSimpleName() + "{" +
						"stations=" + stations +
						'}';
			}

			private static class JStation {
				@Nullable
				private final String stationId;
				@Nullable
				private final String name;
				@Nullable
				private final String shortName;
				@Nullable
				private final Double lat;
				@Nullable
				private final Double lon;
				@Nullable
				private final Integer capacity;

				private JStation(@Nullable String stationId,
								 @Nullable String name,
								 @Nullable String shortName,
								 @Nullable Double lat,
								 @Nullable Double lon,
								 @Nullable Integer capacity) {
					this.stationId = stationId;
					this.name = name;
					this.shortName = shortName;
					this.lat = lat;
					this.lon = lon;
					this.capacity = capacity;
				}

				@Nullable
				String getStationId() {
					return stationId;
				}

				@Nullable
				public String getName() {
					return name;
				}

				@Nullable
				public String getShortName() {
					return shortName;
				}

				@Nullable
				public Double getLat() {
					return lat;
				}

				@Nullable
				Double getLon() {
					return lon;
				}

				@Nullable
				Integer getCapacity() {
					return capacity;
				}

				@NonNull
				@Override
				public String toString() {
					return JStation.class.getSimpleName() + "{" +
							"stationId='" + stationId + '\'' +
							", name='" + name + '\'' +
							", shortName='" + shortName + '\'' +
							", lat=" + lat +
							", lon=" + lon +
							", capacity=" + capacity +
							'}';
				}
			}
		}
	}

	@SuppressWarnings({"unused", "WeakerAccess"})
	private static class JStationStatus {
		@NonNull
		private final JData data;

		private JStationStatus(@NonNull JData data) {
			this.data = data;
		}

		@NonNull
		public JData getData() {
			return data;
		}

		@NonNull
		@Override
		public String toString() {
			return JStationStatus.class.getSimpleName() + "{" +
					"data=" + data +
					'}';
		}

		private static class JData {
			@NonNull
			private final List<JStation> stations;

			private JData(@NonNull List<JStation> stations) {
				this.stations = stations;
			}

			@NonNull
			public List<JStation> getStations() {
				return stations;
			}

			@NonNull
			@Override
			public String toString() {
				return JData.class.getSimpleName() + "{" +
						"stations=" + stations +
						'}';
			}

			private static class JStation {

				@Nullable
				private final String stationId;
				@Nullable
				private final Integer numBikesAvailable;
				@Nullable
				private final Integer numEBikesAvailable;
				@Nullable
				private final JNumBikesAvailableTypes numBikesAvailableTypes;
				@Nullable
				private final Integer numDocksAvailable;
				@Nullable
				private final Boolean isInstalled;
				@Nullable
				private final Boolean isRenting;
				@Nullable
				private final Boolean isReturning;
				@Nullable
				private final Long lastReported; // in seconds

				private JStation(@Nullable String stationId,
								 @Nullable Integer numBikesAvailable,
								 @Nullable Integer numEBikesAvailable,
								 @Nullable JNumBikesAvailableTypes numBikesAvailableTypes,
								 @Nullable Integer numDocksAvailable,
								 @Nullable Boolean isInstalled,
								 @Nullable Boolean isRenting,
								 @Nullable Boolean isReturning,
								 @Nullable Long lastReported) {
					this.stationId = stationId;
					this.numBikesAvailable = numBikesAvailable;
					this.numEBikesAvailable = numEBikesAvailable;
					this.numBikesAvailableTypes = numBikesAvailableTypes;
					this.numDocksAvailable = numDocksAvailable;
					this.isInstalled = isInstalled;
					this.isRenting = isRenting;
					this.isReturning = isReturning;
					this.lastReported = lastReported;
				}

				@Nullable
				String getStationId() {
					return stationId;
				}

				@Nullable
				Integer getNumBikesAvailable() {
					return numBikesAvailable;
				}

				@Nullable
				Integer getNumEBikesAvailable() {
					return numEBikesAvailable;
				}

				@Nullable
				public JNumBikesAvailableTypes getNumBikesAvailableTypes() {
					return numBikesAvailableTypes;
				}

				@Nullable
				Integer getNumDocksAvailable() {
					return numDocksAvailable;
				}

				@Nullable
				Boolean getIsInstalled() {
					return isInstalled;
				}

				@Nullable
				Boolean getIsRenting() {
					return isRenting;
				}

				@Nullable
				Boolean getIsReturning() {
					return isReturning;
				}

				@Nullable
				Long getLastReported() {
					return lastReported;
				}

				@NonNull
				@Override
				public String toString() {
					return JStation.class.getSimpleName() + "{" +
							"stationId='" + stationId + '\'' +
							", numBikesAvailable=" + numBikesAvailable +
							", numEBikesAvailable=" + numEBikesAvailable +
							", numDocksAvailable=" + numDocksAvailable +
							", isInstalled=" + isInstalled +
							", isRenting=" + isRenting +
							", isReturning=" + isReturning +
							", lastReported=" + lastReported +
							'}';
				}

				private static class JNumBikesAvailableTypes {
					@Nullable
					private final Integer mechanical;
					@Nullable
					private final Integer eBikes;

					private JNumBikesAvailableTypes(
							@Nullable Integer mechanical,
							@Nullable Integer eBikes) {
						this.mechanical = mechanical;
						this.eBikes = eBikes;
					}

					@Nullable
					public Integer getMechanical() {
						return mechanical;
					}

					@Nullable
					public Integer getEBikes() {
						return eBikes;
					}

					@NonNull
					@Override
					public String toString() {
						return JNumBikesAvailableTypes.class.getSimpleName() + "{" +
								"mechanical='" + mechanical + '\'' +
								", ebikes=" + eBikes +
								'}';
					}
				}
			}
		}
	}
}
