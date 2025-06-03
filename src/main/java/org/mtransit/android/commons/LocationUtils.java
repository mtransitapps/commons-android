package org.mtransit.android.commons;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.collection.ArrayMap;

import org.mtransit.android.commons.data.Area;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused", "WeakerAccess"})
public class LocationUtils implements MTLog.Loggable {

	private static final String LOG_TAG = LocationUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final long UPDATE_INTERVAL_IN_MS = TimeUnit.SECONDS.toMillis(5L);

	public static final long FASTEST_INTERVAL_IN_MS = TimeUnit.SECONDS.toMillis(1L);

	public static final long PREFER_ACCURACY_OVER_TIME_IN_MS = TimeUnit.SECONDS.toMillis(30L);

	public static final int SIGNIFICANT_ACCURACY_IN_METERS = 200; // 200 meters

	public static final int SIGNIFICANT_DISTANCE_MOVED_IN_METERS = 5; // 5 meters

	public static final int LOCATION_CHANGED_ALLOW_REFRESH_IN_METERS = 10;
	// public static final int LOCATION_CHANGED_ALLOW_REFRESH_IN_METERS = 0; // DEBUG

	public static final int LOCATION_CHANGED_NOTIFY_USER_IN_METERS = 100;
	// public static final int LOCATION_CHANGED_NOTIFY_USER_IN_METERS = 0; // DEBUG
	//

	public static final double MIN_AROUND_DIFF = 0.01;

	public static final double INC_AROUND_DIFF = 0.01;

	public static final float FEET_PER_M = 3.2808399f;

	public static final float FEET_PER_MILE = 5280;

	public static final float METER_PER_KM = 1000f;

	public static final int MIN_NEARBY_LIST = 10;

	public static final int MAX_NEARBY_LIST = 20;

	public static final int MAX_POI_NEARBY_POIS_LIST = 30;
	// public static final int MAX_POI_NEARBY_POIS_LIST = 0; // DEBUG

	public static final int MIN_NEARBY_LIST_COVERAGE_IN_METERS = 100;

	public static final int MIN_POI_NEARBY_POIS_LIST_COVERAGE_IN_METERS = 100;

	public static final double EARTH_RADIUS = 6371009;

	public static final double HEADING_NORTH = 0.0d;
	public static final double HEADING_NORTH_EAST = 45.0d;
	public static final double HEADING_NORTH_WEST = -45.0d;
	public static final double HEADING_SOUTH = 180.0d;
	public static final double HEADING_SOUTH_EAST = 135.0d;
	public static final double HEADING_SOUTH_WEST = -135.0d;
	public static final double HEADING_EAST = 90.0d;
	public static final double HEADING_WEST = -90.0d;

	@NonNull
	public static AroundDiff getNewDefaultAroundDiff() {
		return new AroundDiff(LocationUtils.MIN_AROUND_DIFF, LocationUtils.INC_AROUND_DIFF);
	}

	private LocationUtils() {
	}

	@Nullable
	public static String locationToString(@Nullable Location location) {
		if (location == null) {
			return null;
		}
		return String.format("%s > %s,%s (%s) %s seconds ago", location.getProvider(), location.getLatitude(), location.getLongitude(), location.getAccuracy(),
				TimeUtils.millisToSec(TimeUtils.currentTimeMillis() - location.getTime()));
	}

	@NonNull
	public static Location getNewLocation(double lat, double lng) {
		return getNewLocation(lat, lng, null);
	}

	@NonNull
	public static Location getNewLocation(double lat, double lng, @Nullable Float optAccuracy) {
		return getNewLocation(lat, lng, optAccuracy, "MT");
	}

	@NonNull
	public static Location getNewLocation(double lat, double lng, @Nullable Float optAccuracy, @NonNull String provider) {
		Location newLocation = new Location(provider);
		newLocation.setLatitude(lat);
		newLocation.setLongitude(lng);
		if (optAccuracy != null) {
			newLocation.setAccuracy(optAccuracy);
		}
		return newLocation;
	}

	public static float bearTo(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
		float[] results = new float[2];
		Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
		return results[1];
	}

	public static float distanceToInMeters(@Nullable Location start, @Nullable Location end) {
		if (start == null || end == null) {
			return -1f;
		}
		return distanceToInMeters(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude());
	}

	public static float distanceToInMeters(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
		float[] results = new float[2];
		Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
		return results[0];
	}

	/**
	 * @link <a href="https://developer.android.com/guide/topics/location/obtaining-user-location.html">Get the last known location</a>
	 */
	public static boolean isMoreRelevant(@Nullable String tag,
										 @Nullable Location currentLocation,
										 @Nullable Location newLocation) {
		return isMoreRelevant(
				tag,
				currentLocation,
				newLocation,
				SIGNIFICANT_ACCURACY_IN_METERS,
				SIGNIFICANT_DISTANCE_MOVED_IN_METERS,
				PREFER_ACCURACY_OVER_TIME_IN_MS
		);
	}

	public static boolean isMoreRelevant(@Nullable String tag,
										 @Nullable Location currentLocation,
										 @Nullable Location newLocation,
										 int significantAccuracyInMeters,
										 int significantDistanceMovedInMeters,
										 long preferAccuracyOverTimeInMS) {
		if (newLocation == null) {
			return false;
		}
		if (currentLocation == null) {
			return true;
		}
		if (areTheSame(currentLocation, newLocation)) {
			return false;
		}
		long timeDelta = newLocation.getTime() - currentLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > preferAccuracyOverTimeInMS;
		boolean isSignificantlyOlder = timeDelta < -preferAccuracyOverTimeInMS;
		boolean isNewer = timeDelta > 0;
		if (isSignificantlyNewer) {
			return true;
		} else if (isSignificantlyOlder) {
			return false;
		}
		int accuracyDelta = (int) (newLocation.getAccuracy() - currentLocation.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > significantAccuracyInMeters;
		boolean isSignificantlyMoreAccurate = isMoreAccurate && accuracyDelta < -significantAccuracyInMeters;
		if (isSignificantlyMoreAccurate) {
			return true;
		}
		int distanceTo = (int) distanceToInMeters(currentLocation, newLocation);
		if (distanceTo < significantDistanceMovedInMeters) {
			return false;
		}
		boolean isFromSameProvider = isSameProvider(newLocation, currentLocation);
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else // noinspection RedundantIfStatement
			if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
				return true;
			}
		return false;
	}

	private static boolean isSameProvider(Location loc1, Location loc2) {
		if (loc1.getProvider() == null) {
			return loc2.getProvider() == null;
		}
		return loc1.getProvider().equals(loc2.getProvider());
	}

	@WorkerThread
	@Nullable
	public static Address getLocationAddress(@NonNull Context context, @NonNull Location location) {
		try {
			if (Geocoder.isPresent()) {
				Geocoder geocoder = new Geocoder(context);
				int maxResults = 1;
				java.util.List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), maxResults);
				if (addresses == null || addresses.isEmpty()) {
					return null; // no address found
				}
				return addresses.get(0);
			}
		} catch (IOException ioe) {
			if (MTLog.isLoggable(android.util.Log.DEBUG)) {
				MTLog.w(LOG_TAG, ioe, "getLocationAddress() > Can't find the address of the current location!");
			} else {
				MTLog.w(LOG_TAG, "getLocationAddress() > Can't find the address of the current location!");
			}
		}
		return null;
	}

	@NonNull
	public static String getLocationString(@NonNull Context context,
										   @Nullable Address locationAddress,
										   float accuracyInMeters) {
		final StringBuilder sb = new StringBuilder();
		final boolean isAccurate = accuracyInMeters < 5_000.0F;
		if (locationAddress != null) {
			if (isAccurate && locationAddress.getMaxAddressLineIndex() > 0) {
				sb.append(locationAddress.getAddressLine(0));
			} else if (isAccurate && locationAddress.getThoroughfare() != null) {
				sb.append(locationAddress.getThoroughfare());
			} else if (locationAddress.getLocality() != null) {
				sb.append(locationAddress.getLocality());
			} else if (!isAccurate && locationAddress.getSubAdminArea() != null) {
				sb.append(locationAddress.getSubAdminArea());
			} else if (isAccurate) {
				sb.append(context.getString(R.string.unknown_address));
			}
		} else if (isAccurate) {
			sb.append(context.getString(R.string.unknown_address));
		}
		if (isAccurate && accuracyInMeters > 0.0f) {
			sb.append(" ± ").append(getDistanceStringUsingPref(context, accuracyInMeters, accuracyInMeters));
		}
		return sb.toString();
	}

	public static double truncAround(@NonNull String loc) {
		return Double.parseDouble(truncAround(Double.parseDouble(loc)));
	}

	private static final String AROUND_TRUNC = "%.2f";

	@NonNull
	public static String truncAround(double loc) {
		return String.format(Locale.US, AROUND_TRUNC, loc);
	}

	@NonNull
	public static String getDistanceStringUsingPref(@NonNull Context context, float distanceInMeters, float accuracyInMeters) {
		String distanceUnit = PreferenceUtils.getPrefDefault(context, PreferenceUtils.PREFS_UNITS, PreferenceUtils.PREFS_UNITS_DEFAULT);
		return getDistanceString(distanceInMeters, accuracyInMeters, distanceUnit);
	}

	@NonNull
	private static String getDistanceString(float distanceInMeters, float accuracyInMeters, @Nullable String distanceUnit) {
		if (PreferenceUtils.PREFS_UNITS_IMPERIAL.equals(distanceUnit)) {
			float distanceInSmall = distanceInMeters * FEET_PER_M;
			float accuracyInSmall = accuracyInMeters * FEET_PER_M;
			return getDistance(distanceInSmall, accuracyInSmall, FEET_PER_MILE, 10, "ft", "mi");
		} else { // use Metric (default)
			return getDistance(distanceInMeters, accuracyInMeters, METER_PER_KM, 1, "m", "km");
		}
	}

	@NonNull
	private static String getDistance(float distance, float accuracy, float smallPerBig, int threshold, String smallUnit, String bigUnit) {
		StringBuilder sb = new StringBuilder();
		if (accuracy > distance) {
			if (accuracy > (smallPerBig / threshold)) {
				float accuracyInBigUnit = accuracy / smallPerBig;
				float niceAccuracyInBigUnit = Integer.valueOf(Math.round(accuracyInBigUnit * 10)).floatValue() / 10;
				sb.append("< ").append(niceAccuracyInBigUnit).append(" ").append(bigUnit);
			} else {
				int niceAccuracyInSmallUnit = Math.round(accuracy);
				sb.append("< ").append(getSimplerDistance(niceAccuracyInSmallUnit, accuracy)).append(" ").append(smallUnit);
			}
		} else {
			if (distance > (smallPerBig / threshold)) {
				float distanceInBigUnit = distance / smallPerBig;
				float niceDistanceInBigUnit = Integer.valueOf(Math.round(distanceInBigUnit * 10)).floatValue() / 10;
				sb.append(niceDistanceInBigUnit).append(" ").append(bigUnit);
			} else {
				int niceDistanceInSmallUnit = Math.round(distance);
				sb.append(getSimplerDistance(niceDistanceInSmallUnit, accuracy)).append(" ").append(smallUnit);
			}
		}
		return sb.toString();
	}

	public static int getSimplerDistance(int distance, float accuracyF) {
		int accuracy = Math.round(accuracyF);
		int simplerDistance = Math.round(distance / 10f) * 10;
		if (Math.abs(simplerDistance - distance) < accuracy) {
			return simplerDistance;
		} else {
			return distance; // accuracy too good, have to keep real data
		}
	}

	private static final float MAX_DISTANCE_ON_EARTH_IN_METERS = 40075017f / 2f;

	public static float getAroundCoveredDistanceInMeters(double lat, double lng, double aroundDiff) {
		Area area = getArea(lat, lng, aroundDiff);
		float distanceToSouth = area.getMinLat() > MIN_LAT ? distanceToInMeters(lat, lng, area.getMinLat(), lng) : MAX_DISTANCE_ON_EARTH_IN_METERS;
		float distanceToNorth = area.getMaxLat() < MAX_LAT ? distanceToInMeters(lat, lng, area.getMaxLat(), lng) : MAX_DISTANCE_ON_EARTH_IN_METERS;
		float distanceToWest = area.getMinLng() > MIN_LNG ? distanceToInMeters(lat, lng, lat, area.getMinLng()) : MAX_DISTANCE_ON_EARTH_IN_METERS;
		float distanceToEast = area.getMaxLng() < MAX_LNG ? distanceToInMeters(lat, lng, lat, area.getMaxLng()) : MAX_DISTANCE_ON_EARTH_IN_METERS;
		float[] distances = new float[]{distanceToNorth, distanceToSouth, distanceToWest, distanceToEast};
		Arrays.sort(distances);
		return distances[0]; // return the closest
	}

	@NonNull
	private static Area getArea(double lat, double lng, double aroundDiff) {
		double latTrunc = Math.abs(lat);
		double latBefore = Math.signum(lat) * Double.parseDouble(truncAround(latTrunc - aroundDiff));
		double latAfter = Math.signum(lat) * Double.parseDouble(truncAround(latTrunc + aroundDiff));
		double lngTrunc = Math.abs(lng);
		double lngBefore = Math.signum(lng) * Double.parseDouble(truncAround(lngTrunc - aroundDiff));
		double lngAfter = Math.signum(lng) * Double.parseDouble(truncAround(lngTrunc + aroundDiff));
		double minLat = Math.min(latBefore, latAfter);
		if (minLat < MIN_LAT) {
			minLat = MIN_LAT;
		}
		double maxLat = Math.max(latBefore, latAfter);
		if (maxLat > MAX_LAT) {
			maxLat = MAX_LAT;
		}
		double minLng = Math.min(lngBefore, lngAfter);
		if (minLng < MIN_LNG) {
			minLng = MIN_LNG;
		}
		double maxLng = Math.max(lngBefore, lngAfter);
		if (maxLng > MAX_LNG) {
			maxLng = MAX_LNG;
		}
		return new Area(minLat, maxLat, minLng, maxLng);
	}

	public static final double MAX_LAT = 90.0f;
	public static final double MIN_LAT = -90.0f;
	public static final double MAX_LNG = 180.0f;
	public static final double MIN_LNG = -180.0f;
	@NonNull
	public static final Area THE_WORLD = new Area(MIN_LAT, MAX_LAT, MIN_LNG, MAX_LNG);

	@NonNull
	public static String genAroundWhere(@NonNull String lat, @NonNull String lng, @NonNull String latTableColumn, @NonNull String lngTableColumn, double aroundDiff) {
		StringBuilder qb = new StringBuilder();
		Area area = getArea(truncAround(lat), truncAround(lng), aroundDiff);
		qb.append(SqlUtils.getBetween(latTableColumn, area.getMinLat(), area.getMaxLat()));
		qb.append(SqlUtils.AND);
		qb.append(SqlUtils.getBetween(lngTableColumn, area.getMinLng(), area.getMaxLng()));
		return qb.toString();
	}

	@NonNull
	public static String genAroundWhere(double lat, double lng, @NonNull String latTableColumn, @NonNull String lngTableColumn, double aroundDiff) {
		return genAroundWhere(String.valueOf(lat), String.valueOf(lng), latTableColumn, lngTableColumn, aroundDiff);
	}

	@NonNull
	public static String genAroundWhere(@NonNull Location location, @NonNull String latTableColumn, @NonNull String lngTableColumn, double aroundDiff) {
		return genAroundWhere(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), latTableColumn, lngTableColumn, aroundDiff);
	}

	@NonNull
	public static List<SimpleLocationPOI> toSimplePOIListClone(@NonNull Iterable<? extends LocationPOI> locationPOIList) {
		List<SimpleLocationPOI> result = new ArrayList<>();
		for (LocationPOI locationPOI : locationPOIList) {
			result.add(
					new SimpleLocationPOI(locationPOI.getPOI())
							.setDistance(locationPOI.getDistance())
							.setDistanceString(locationPOI.getDistanceString())
			);
		}
		return result;
	}

	public static void updateDistance(@Nullable ArrayMap<?, ? extends LocationPOI> pois, @Nullable Location location) {
		if (location == null) {
			return;
		}
		updateDistance(pois, location.getLatitude(), location.getLongitude());
	}

	public static void updateDistance(@Nullable ArrayMap<?, ? extends LocationPOI> pois, double lat, double lng) {
		if (pois == null) {
			return;
		}
		for (LocationPOI poi : pois.values()) {
			if (!poi.hasLocation()) {
				continue;
			}
			poi.setDistance(distanceToInMeters(lat, lng, poi.getLat(), poi.getLng()));
		}
	}

	public static void updateDistanceWithString(@NonNull Context context,
												@Nullable Collection<? extends LocationPOI> pois,
												@Nullable Location currentLocation,
												@SuppressWarnings("deprecation") @Nullable MTCancellableAsyncTask<?, ?, ?> task) {
		if (pois == null || currentLocation == null) {
			return;
		}
		String distanceUnit = PreferenceUtils.getPrefDefault(context, PreferenceUtils.PREFS_UNITS, PreferenceUtils.PREFS_UNITS_DEFAULT);
		float accuracyInMeters = currentLocation.getAccuracy();
		float newDistance;
		for (LocationPOI poi : pois) {
			if (!poi.hasLocation()) {
				continue;
			}
			newDistance = distanceToInMeters(currentLocation.getLatitude(), currentLocation.getLongitude(), poi.getLat(), poi.getLng());
			if (poi.getDistance() > 1 && newDistance == poi.getDistance() && poi.getDistanceString() != null) {
				continue;
			}
			poi.setDistance(newDistance);
			poi.setDistanceString(getDistanceString(poi.getDistance(), accuracyInMeters, distanceUnit));
			// noinspection deprecation
			if (task != null && task.isCancelled()) {
				break;
			}
		}
	}

	public static void updateDistance(@Nullable List<? extends LocationPOI> pois, @Nullable Location location) {
		if (location == null) {
			return;
		}
		updateDistance(pois, location.getLatitude(), location.getLongitude());
	}

	public static void updateDistance(@Nullable List<? extends LocationPOI> pois, double lat, double lng) {
		if (pois == null) {
			return;
		}
		for (LocationPOI poi : pois) {
			if (!poi.hasLocation()) {
				continue;
			}
			poi.setDistance(distanceToInMeters(lat, lng, poi.getLat(), poi.getLng()));
		}
	}

	public static void updateDistanceWithString(@NonNull Context context, @Nullable LocationPOI poi, @Nullable Location currentLocation) {
		if (poi == null || currentLocation == null) {
			return;
		}
		final String distanceUnit = PreferenceUtils.getPrefDefault(context, PreferenceUtils.PREFS_UNITS, PreferenceUtils.PREFS_UNITS_DEFAULT);
		updateDistanceWithString(distanceUnit, poi, currentLocation);
	}

	public static void updateDistanceWithString(@Nullable String distanceUnit, @Nullable LocationPOI poi, @Nullable Location currentLocation) {
		if (poi == null || currentLocation == null) {
			return;
		}
		final float accuracyInMeters = currentLocation.getAccuracy();
		if (!poi.hasLocation()) {
			return;
		}
		final float newDistance = distanceToInMeters(currentLocation.getLatitude(), currentLocation.getLongitude(), poi.getLat(), poi.getLng());
		if (poi.getDistance() > 1 && newDistance == poi.getDistance() && poi.getDistanceString() != null) {
			return;
		}
		poi.setDistance(newDistance);
		poi.setDistanceString(getDistanceString(poi.getDistance(), accuracyInMeters, distanceUnit));
	}

	public static boolean areAlmostTheSame(@Nullable Location loc1, @Nullable Location loc2, int distanceInMeters) {
		return loc1 != null && loc2 != null //
				&& distanceToInMeters(loc1, loc2) < distanceInMeters;
	}

	public static boolean areTheSame(@Nullable Location loc1, @Nullable Location loc2) {
		if (loc1 == null) {
			return loc2 == null;
		}
		return loc2 != null //
				&& areTheSame(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(), loc2.getLongitude());
	}

	public static boolean areTheSame(@Nullable Location loc1, double lat2, double lng2) {
		return loc1 != null //
				&& areTheSame(loc1.getLatitude(), loc1.getLongitude(), lat2, lng2);
	}

	public static boolean areTheSame(double lat1, double lng1, double lat2, double lng2) {
		return lat1 == lat2 && lng1 == lng2;
	}

	public static void removeTooFar(@Nullable List<? extends LocationPOI> pois, float maxDistanceInMeters) {
		if (pois != null) {
			pois.removeIf(poi -> poi.getDistance() > maxDistanceInMeters);
		}
	}

	public static boolean searchComplete(double lat, double lng, double aroundDiff) {
		final Area area = getArea(lat, lng, aroundDiff);
		return searchComplete(area);
	}

	public static boolean searchComplete(@NonNull Area area) {
		if (area.getMinLat() > MIN_LAT) {
			return false; // more places to explore in the south
		}
		if (area.getMaxLat() < MAX_LAT) {
			return false; // more places to explore in the north
		}
		if (area.getMinLng() > MIN_LNG) {
			return false; // more places to explore to the west
		}
		// noinspection RedundantIfStatement
		if (area.getMaxLng() < MAX_LNG) {
			return false; // more places to explore to the east
		}
		return true; // planet search completed!
	}

	@NonNull
	public static AroundDiff incAroundDiff(@NonNull AroundDiff ad) {
		ad.aroundDiff += ad.incAroundDiff;
		ad.incAroundDiff *= 2; // warning, might return a huge chunk of data if far away (all POIs or none)
		return ad;
	}

	public static boolean isInside(double lat, double lng, @Nullable Area area) {
		if (area == null) {
			return false;
		}
		return isInside(lat, lng, area.getMinLat(), area.getMaxLat(), area.getMinLng(), area.getMaxLng());
	}

	public static boolean isInside(double lat, double lng, double minLat, double maxLat, double minLng, double maxLng) {
		return lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng;
	}

	/**
	 * <pre>
	 *        +--+
	 *        |  |
	 *   +----+--+----+
	 *   |    |  |    |
	 *   +----+--+----+
	 *        |  |
	 *        +--+
	 * </pre>
	 */
	private static boolean areCompletelyOverlapping(Area area1, Area area2) {
		if (area1.getMinLat() >= area2.getMinLat() && area1.getMaxLat() <= area2.getMaxLat()) {
			if (area2.getMinLng() >= area1.getMinLng() && area2.getMaxLng() <= area1.getMaxLng()) {
				return true; // area 1 wider than area 2 but area 2 higher than area 1
			}
		}
		if (area2.getMinLat() >= area1.getMinLat() && area2.getMaxLat() <= area1.getMaxLat()) {
			// noinspection RedundantIfStatement
			if (area1.getMinLng() >= area2.getMinLng() && area1.getMaxLng() <= area2.getMaxLng()) {
				return true; // area 2 wider than area 1 but area 1 higher than area 2
			}
		}
		return false;
	}

	@NonNull
	public static Location computeOffset(@NonNull Location from, double distance, double heading) {
		double[] result = computeOffset(from.getLatitude(), from.getLongitude(), distance, heading);
		return getNewLocation(result[0], result[1]);
	}

	// inspired by https://github.com/googlemaps/android-maps-utils
	@NonNull
	public static double[] computeOffset(double fromLatitude, double fromLongitude, double distance, double heading) {
		distance /= EARTH_RADIUS;
		heading = Math.toRadians(heading);
		// https://williams.best.vwh.net/avform.htm#LL
		double fromLat = Math.toRadians(fromLatitude);
		double fromLng = Math.toRadians(fromLongitude);
		double cosDistance = Math.cos(distance);
		double sinDistance = Math.sin(distance);
		double sinFromLat = Math.sin(fromLat);
		double cosFromLat = Math.cos(fromLat);
		double sinLat = cosDistance * sinFromLat + sinDistance * cosFromLat * Math.cos(heading);
		double dLng = Math.atan2(
				sinDistance * cosFromLat * Math.sin(heading),
				cosDistance - sinFromLat * sinLat);
		return new double[]{Math.toDegrees(Math.asin(sinLat)), Math.toDegrees(fromLng + dLng)};
	}

	public static class AroundDiff {

		public double aroundDiff = LocationUtils.MIN_AROUND_DIFF;
		public double incAroundDiff = LocationUtils.INC_AROUND_DIFF;

		public AroundDiff() {
		}

		public AroundDiff(double aroundDiff, double incAroundDiff) {
			this.aroundDiff = aroundDiff;
			this.incAroundDiff = incAroundDiff;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			AroundDiff that = (AroundDiff) o;

			if (Double.compare(that.aroundDiff, aroundDiff) != 0) return false;
			return Double.compare(that.incAroundDiff, incAroundDiff) == 0;
		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			temp = Double.doubleToLongBits(aroundDiff);
			result = (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(incAroundDiff);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			return result;
		}

		@NonNull
		@Override
		public String toString() {
			return AroundDiff.class.getSimpleName() + '[' +
					this.aroundDiff + ',' +
					this.incAroundDiff +
					']';
		}
	}

	public static final POIDistanceComparator POI_DISTANCE_COMPARATOR = new POIDistanceComparator();

	public static class POIDistanceComparator implements Comparator<LocationPOI> {
		@Override
		public int compare(@NonNull LocationPOI lhs, @NonNull LocationPOI rhs) {
			if (lhs.getPOI() instanceof RouteTripStop && rhs.getPOI() instanceof RouteTripStop) {
				final RouteTripStop lRTS = (RouteTripStop) lhs.getPOI();
				final RouteTripStop rRTS = (RouteTripStop) rhs.getPOI();
				if (lRTS.getStop().getId() == rRTS.getStop().getId()) { // SAME STOP = SAME LOCATION
					if (Route.SHORT_NAME_COMPARATOR.areDifferent(lRTS.getRoute(), rRTS.getRoute())) {
						if (Route.SHORT_NAME_COMPARATOR.areComparable(lRTS.getRoute(), rRTS.getRoute())) {
							return Route.SHORT_NAME_COMPARATOR.compare(lRTS.getRoute(), rRTS.getRoute());
						}
					}
					if (Trip.HEAD_SIGN_COMPARATOR.areDifferent(lRTS.getTrip(), rRTS.getTrip())) {
						if (Trip.HEAD_SIGN_COMPARATOR.areComparable(lRTS.getTrip(), rRTS.getTrip())) {
							return Trip.HEAD_SIGN_COMPARATOR.compare(lRTS.getTrip(), rRTS.getTrip());
						}
					}
				}
			}
			// FIXME IllegalArgumentException: Comparison method violates its general contract!
			// FIXME => distance can be updated from another thread
			return Float.compare(lhs.getDistance(), rhs.getDistance());
		}
	}

	public static class SimpleLocationPOI implements LocationPOI {

		@NonNull
		private final POI poi;
		@Nullable
		private CharSequence distanceString = null;
		private float distance = -1;

		public SimpleLocationPOI(@NonNull POI poi) {
			this.poi = poi;
		}

		@NonNull
		@Override
		public POI getPOI() {
			return this.poi;
		}

		@Override
		public double getLat() {
			return this.poi.getLat();
		}

		@Override
		public double getLng() {
			return this.poi.getLng();
		}

		@Override
		public boolean hasLocation() {
			return this.poi.hasLocation();
		}

		@NonNull
		@Override
		public SimpleLocationPOI setDistanceString(@Nullable CharSequence distanceString) {
			this.distanceString = distanceString;
			return this;
		}

		@Nullable
		@Override
		public CharSequence getDistanceString() {
			return this.distanceString;
		}

		@NonNull
		@Override
		public SimpleLocationPOI setDistance(float distance) {
			this.distance = distance;
			return this;
		}

		@Override
		public float getDistance() {
			return this.distance;
		}
	}

	public interface LocationPOI {

		@NonNull
		POI getPOI();

		double getLat();

		double getLng();

		boolean hasLocation();

		@NonNull
		LocationPOI setDistanceString(@Nullable CharSequence distanceString);

		@Nullable
		CharSequence getDistanceString();

		@NonNull
		LocationPOI setDistance(float distance);

		float getDistance();
	}
}
