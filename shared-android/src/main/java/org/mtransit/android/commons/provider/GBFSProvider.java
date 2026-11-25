package org.mtransit.android.commons.provider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.FileUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.NetworkUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.Area;
import org.mtransit.android.commons.data.AvailabilityPercentStatusFilter;
import org.mtransit.android.commons.data.BikeStationAvailabilityPercent;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.POIStatus;
import org.mtransit.android.commons.data.POITools;
import org.mtransit.android.commons.provider.bike.BikeStationProvider;
import org.mtransit.android.commons.provider.gbfs.GBFSStorage;
import org.mtransit.android.commons.provider.poi.POIProvider;
import org.mtransit.android.commons.provider.poi.POIProviderContract;
import org.mtransit.android.commons.provider.status.StatusProvider;
import org.mtransit.android.commons.provider.status.StatusProviderContract;
import org.mtransit.commons.SourceUtils;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.net.ssl.SSLHandshakeException;

// DO NOT MOVE: referenced in modules AndroidManifest.xml
@SuppressLint("Registered")
public class GBFSProvider extends BikeStationProvider {

	protected static final String LOG_TAG = GBFSProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	@Override
	public Area getAgencyArea(@NonNull Context context) {
		final Area area = getStorage(context).getArea();
		if (area != null) return area;
		return super.getAgencyArea(context);
	}

	@Nullable
	@Override
	public Cursor getPOIBikeStations(@Nullable POIProviderContract.Filter poiFilter) {
		updateBikeStationDataIfRequired();
		return getPOIFromDB(poiFilter);
	}

	@Override
	public long getLastUpdateInMs() { // POI
		return getStorage(requireContextCompat()).getLastUpdateInMs();
	}

	@Nullable
	@Override
	public POIStatus getNewBikeStationStatus(@NonNull AvailabilityPercentStatusFilter statusFilter) {
		updateBikeStationStatusDataIfRequired(statusFilter);
		return getCachedStatus(statusFilter);
	}

	@Override
	public void updateBikeStationDataIfRequired() {
		final Context context = requireContextCompat();
		final long lastUpdateInMs = getStorage(context).getLastUpdateInMs(); // POI
		final long nowInMs = TimeUtils.currentTimeMillis();
		// MAX VALIDITY (too old to display?)
		if (lastUpdateInMs + getPoiValidityInMs() < nowInMs) { // try to update
			if (lastUpdateInMs + getPoiMaxValidityInMs() < nowInMs) { // too old to display
				deleteAllBikeStationData();
				updateBikeStationDataFromWWW(context, lastUpdateInMs);
				return;
			}
			updateBikeStationDataFromWWW(context, lastUpdateInMs);
		}
	}

	@Override
	public void updateBikeStationStatusDataIfRequired(@NonNull StatusProviderContract.Filter statusFilter) {
		final Context context = requireContextCompat();
		final long lastUpdateInMs = getStorage(context).getLastUpdateStatusInMs(); // STATUS
		final long nowInMs = TimeUtils.currentTimeMillis();
		if (lastUpdateInMs + getStatusMaxValidityInMs() < nowInMs) { // too old to display?
			deleteAllBikeStationStatusData();
			updateBikeStationStatusDataFromWWW(context, lastUpdateInMs);
			return;
		}
		if (lastUpdateInMs + getStatusValidityInMs(statusFilter.isInFocusOrDefault()) < nowInMs) { // try to refresh?
			updateBikeStationStatusDataFromWWW(context, lastUpdateInMs);
		}
	}

	private synchronized void updateBikeStationDataFromWWW(@NonNull Context context, long oldLastUpdatedInMs) { // TODO remove synchronized!
		if (getStorage(context).getLastUpdateInMs() > oldLastUpdatedInMs) { // POI
			MTLog.d(this, "updateBikeStationDataFromWWW() > SKIP (already updating/updated)");
			return; // too late, another thread already updated
		}
		loadBikeStationDataFromWWW(context);
	}

	private synchronized void updateBikeStationStatusDataFromWWW(@NonNull Context context, long oldLastUpdatedInMs) { // TODO remove synchronized!
		if (getStorage(context).getLastUpdateStatusInMs() > oldLastUpdatedInMs) { // STATUS
			MTLog.d(this, "updateBikeStationStatusDataFromWWW() > SKIP (already updating/updated)");
			return; // too late, another thread already updated
		}
		loadBikeStationStatusDataFromWWW();
	}

	@SuppressWarnings("UnusedReturnValue")
	@Nullable
	private HashSet<DefaultPOI> loadBikeStationDataFromWWW(Context context) {
		try {
			String urlString = getDATA_URL(context);
			urlString += "en/"; // TODO support fr...
			urlString += "station_information.json";
			MTLog.i(this, "Loading from '%s'...", urlString);
			final URL url = new URL(urlString);
			final URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			final HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				final long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				final String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadBikeStationDataFromWWW() > jsonString: %s.", jsonString);
				final List<JStationInformation.JData.JStation> jStations = parseAgencyJSONStationInformation(jsonString);
				final HashSet<DefaultPOI> newBikeStations = parseAgencyJsonStations(context, jStations);
				MTLog.i(this, "Found %d stations.", newBikeStations.size());
				final Area coverageArea = POITools.getCoverageArea(newBikeStations);
				getStorage(context).saveArea(coverageArea);
				deleteAllBikeStationData();
				POIProvider.insertDefaultPOIs(this, newBikeStations);
				getStorage(context).setLastUpdateInMs(newLastUpdateInMs); // POI
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

	private volatile GBFSStorage storage = null;

	@NonNull
	private GBFSStorage getStorage(@NonNull Context context) {
		GBFSStorage storage = this.storage;
		if (storage == null) {
			synchronized (this) {
				storage = this.storage;
				if (storage == null) {
					storage = new GBFSStorage(context.getApplicationContext());
					this.storage = storage;
				}
			}
		}
		return storage;
	}

	@NonNull
	private HashSet<DefaultPOI> parseAgencyJsonStations(
			@NonNull Context context,
			@NonNull List<JStationInformation.JData.JStation> jStations
	) {
		final HashSet<DefaultPOI> newBikeStations = new HashSet<>();
		try {
			final String authority = getAUTHORITY(context);
			final int dataSourceTypeId = getAGENCY_TYPE_ID(context);
			for (JStationInformation.JData.JStation jStation : jStations) {
				final DefaultPOI newBikeStation = parseAgencyJsonStation(authority, dataSourceTypeId, jStation);
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
	private DefaultPOI parseAgencyJsonStation(String authority, int dataSourceTypeId, JStationInformation.JData.JStation jStation) {
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
			final String idString = jStation.getStationId();
			final int bikeStationId = idString == null ? -1 : Integer.parseInt(idString);
			if (bikeStationId < 0) return null;
			return new DefaultPOI(
					authority,
					bikeStationId,
					dataSourceTypeId,
					POI.ITEM_VIEW_TYPE_BASIC_POI,
					POI.ITEM_STATUS_TYPE_AVAILABILITY_PERCENT,
					POI.ITEM_ACTION_TYPE_FAVORITABLE,
					jStation.getLat(),
					jStation.getLon(),
					cleanBikeStationName(jStation.getName())
			);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing station JSON '%s'!", jStation);
			return null;
		}
	}

	@NonNull
	private List<JStationInformation.JData.JStation> parseAgencyJSONStationInformation(@NonNull String jsonString) {
		return GBFSProviderExtKt.toJStationInfoListOrDefault(jsonString);
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
			final String sourceLabel = SourceUtils.getSourceLabel(getDATA_URL(context));
			final URL url = new URL(urlString);
			final URLConnection urlc = url.openConnection();
			NetworkUtils.setupUrlConnection(urlc);
			final HttpURLConnection httpUrlConnection = (HttpURLConnection) urlc;
			switch (httpUrlConnection.getResponseCode()) {
			case HttpURLConnection.HTTP_OK:
				final long newLastUpdateInMs = TimeUtils.currentTimeMillis();
				final String jsonString = FileUtils.getString(urlc.getInputStream());
				MTLog.d(this, "loadBikeStationStatusDataFromWWW() > jsonString: %s.", jsonString);
				final List<JStationStatus.JData.JStation> jStationStatus = parseAgencyJsonStationStatus(jsonString);
				final HashSet<POIStatus> newBikeStationStatus = parseAgencyJsonStationsStatus(context, jStationStatus, sourceLabel, newLastUpdateInMs);
				MTLog.i(this, "Found %d statuses.", newBikeStationStatus.size());
				deleteAllBikeStationStatusData();
				StatusProvider.cacheAllStatusesBulkLockDB(this, newBikeStationStatus);
				getStorage(context).setLastUpdateStatusInMs(newLastUpdateInMs);
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
	private HashSet<POIStatus> parseAgencyJsonStationsStatus(
			@NonNull Context context,
			@NonNull List<JStationStatus.JData.JStation> jStations,
			@Nullable String sourceLabel,
			long newLastUpdateInMs
	) {
		final HashSet<POIStatus> newBikeStationStatuses = new HashSet<>();
		try {
			final String authority = getAUTHORITY(context);
			final long statusMaxValidityInMs = getStatusMaxValidityInMs();
			final int value1Color = getValue1Color(context);
			final int value1ColorBg = getValue1ColorBg(context);
			final int value1SubValue1Color = getValue1SubValue1Color(context);
			final int value1SubValue1ColorBg = getValue1SubValue1ColorBg(context);
			final int value2Color = getValue2Color(context);
			final int value2ColorBg = getValue2ColorBg(context);
			for (JStationStatus.JData.JStation jStation : jStations) {
				final POIStatus newBikeStationStatus = parseAgencyJsonStationStatus(
						authority, jStation, sourceLabel, newLastUpdateInMs,
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
	private POIStatus parseAgencyJsonStationStatus(
			@NonNull String authority,
			@NonNull JStationStatus.JData.JStation jStation,
			@Nullable String sourceLabel,
			long newLastUpdateInMs,
			long statusMaxValidityInMs,
			int value1Color,
			int value1ColorBg,
			int value1SubValue1Color,
			int value1SubValue1ColorBg,
			int value2Color,
			int value2ColorBg
	) {
		try {
			final String idString = jStation.getStationId();
			int bikeStationId = idString == null ? -1 : Integer.parseInt(idString);
			if (bikeStationId < 0) return null;
			final BikeStationAvailabilityPercent newBikeStationStatus = new BikeStationAvailabilityPercent(
					null,
					POI.POIUtils.makeUUID(authority, bikeStationId),
					newLastUpdateInMs,
					statusMaxValidityInMs,
					newLastUpdateInMs,
					sourceLabel,
					value1Color,
					value1ColorBg,
					value1SubValue1Color,
					value1SubValue1ColorBg,
					value2Color,
					value2ColorBg
			);
			final boolean isInstalled = Boolean.TRUE.equals(jStation.getIsInstalled());
			final boolean isRenting = Boolean.TRUE.equals(jStation.getIsRenting());
			final boolean isReturning = Boolean.TRUE.equals(jStation.getIsReturning());
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
	private List<JStationStatus.JData.JStation> parseAgencyJsonStationStatus(@NonNull String jsonString) {
		try {
			return GBFSProviderExtKt.toJStationStatusListOrDefault(jsonString);
		} catch (Exception e) {
			MTLog.w(this, e, "Error while parsing JSON '%s'!", jsonString);
			return Collections.emptyList();
		}
	}

	static class JStationInformation {
		@NonNull
		private final JData data;

		protected JStationInformation(@NonNull JData data) {
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

		static class JData {
			@NonNull
			private final List<JStation> stations;

			protected JData(@NonNull List<JStation> stations) {
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

			static class JStation {
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

				protected JStation(
						@Nullable String stationId,
						@Nullable String name,
						@Nullable String shortName,
						@Nullable Double lat,
						@Nullable Double lon,
						@Nullable Integer capacity
				) {
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

	static class JStationStatus {
		@NonNull
		private final JData data;

		private JStationStatus(@NonNull JData data) {
			this.data = data;
		}

		@NonNull
		protected JData getData() {
			return data;
		}

		@NonNull
		@Override
		public String toString() {
			return JStationStatus.class.getSimpleName() + "{" +
					"data=" + data +
					'}';
		}

		static class JData {
			@NonNull
			private final List<JStation> stations;

			protected JData(@NonNull List<JStation> stations) {
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

			static class JStation {

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

				protected JStation(
						@Nullable String stationId,
						@Nullable Integer numBikesAvailable,
						@Nullable Integer numEBikesAvailable,
						@Nullable JNumBikesAvailableTypes numBikesAvailableTypes,
						@Nullable Integer numDocksAvailable,
						@Nullable Boolean isInstalled,
						@Nullable Boolean isRenting,
						@Nullable Boolean isReturning,
						@Nullable Long lastReported
				) {
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

				static class JNumBikesAvailableTypes {
					@Nullable
					private final Integer mechanical;
					@Nullable
					private final Integer eBikes;

					protected JNumBikesAvailableTypes(
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
