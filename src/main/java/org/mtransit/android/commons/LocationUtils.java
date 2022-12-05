package org.mtransit.android.commons;

import android.content.Context;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.collection.ArrayMap;

import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.android.commons.provider.AgencyProviderContract;
import org.mtransit.android.commons.task.MTCancellableAsyncTask;
import org.mtransit.commons.CollectionUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused", "WeakerAccess"})
public class LocationUtils implements MTLog.Loggable {

	private static final String TAG = LocationUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
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

	public static final double MIN_AROUND_DIFF = 0.01;

	public static final double INC_AROUND_DIFF = 0.01;

	public static final float FEET_PER_M = 3.2808399f;

	public static final float FEET_PER_MILE = 5280;

	public static final float METER_PER_KM = 1000f;

	public static final int MIN_NEARBY_LIST = 10;

	public static final int MAX_NEARBY_LIST = 20;

	public static final int MAX_POI_NEARBY_POIS_LIST = 20;

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
		Location newLocation = new Location("MT");
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
	 * @link https://developer.android.com/guide/topics/location/obtaining-user-location.html
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
		} else //noinspection RedundantIfStatement
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
				if (addresses == null || addresses.size() == 0) {
					return null; // no address found
				}
				return addresses.get(0);
			}
		} catch (IOException ioe) {
			if (MTLog.isLoggable(android.util.Log.DEBUG)) {
				MTLog.w(TAG, ioe, "getLocationAddress() > Can't find the address of the current location!");
			} else {
				MTLog.w(TAG, "getLocationAddress() > Can't find the address of the current location!");
			}
		}
		return null;
	}

	@NonNull
	public static String getLocationString(@Nullable Context context,
										   @Nullable String initialString,
										   @Nullable Address locationAddress,
										   @Nullable Float accuracy) {
		StringBuilder sb = new StringBuilder();
		if (context == null) {
			return sb.toString();
		}
		boolean hasInitialString = !TextUtils.isEmpty(initialString);
		if (hasInitialString) {
			sb.append(initialString);
		}
		if (hasInitialString) {
			sb.append(" (");
		}
		if (locationAddress != null) {
			if (locationAddress.getMaxAddressLineIndex() > 0) {
				sb.append(locationAddress.getAddressLine(0));
			} else if (locationAddress.getThoroughfare() != null) {
				sb.append(locationAddress.getThoroughfare());
			} else if (locationAddress.getLocality() != null) {
				sb.append(locationAddress.getLocality());
			} else {
				sb.append(context.getString(R.string.unknown_address));
			}
		} else {
			sb.append(context.getString(R.string.unknown_address));
		}
		if (accuracy != null && accuracy > 0.0f) {
			sb.append(" ± ").append(getDistanceStringUsingPref(context, accuracy, accuracy));
		}
		if (hasInitialString) {
			sb.append(")");
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
		float distanceToSouth = area.minLat > MIN_LAT ? distanceToInMeters(lat, lng, area.minLat, lng) : MAX_DISTANCE_ON_EARTH_IN_METERS;
		float distanceToNorth = area.maxLat < MAX_LAT ? distanceToInMeters(lat, lng, area.maxLat, lng) : MAX_DISTANCE_ON_EARTH_IN_METERS;
		float distanceToWest = area.minLng > MIN_LNG ? distanceToInMeters(lat, lng, lat, area.minLng) : MAX_DISTANCE_ON_EARTH_IN_METERS;
		float distanceToEast = area.maxLng < MAX_LNG ? distanceToInMeters(lat, lng, lat, area.maxLng) : MAX_DISTANCE_ON_EARTH_IN_METERS;
		float[] distances = new float[]{distanceToNorth, distanceToSouth, distanceToWest, distanceToEast};
		Arrays.sort(distances);
		return distances[0]; // return the closest
	}

	@NonNull
	public static Area getArea(double lat, double lng, double aroundDiff) {
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
		qb.append(SqlUtils.getBetween(latTableColumn, area.minLat, area.maxLat));
		qb.append(SqlUtils.AND);
		qb.append(SqlUtils.getBetween(lngTableColumn, area.minLng, area.maxLng));
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
			ListIterator<? extends LocationPOI> it = pois.listIterator();
			while (it.hasNext()) {
				LocationPOI poi = it.next();
				if (poi.getDistance() > maxDistanceInMeters) {
					it.remove();
				}
			}
		}
	}

	public static void removeTooMuchWhenNotInCoverage(@Nullable List<? extends LocationPOI> pois, float minCoverageInMeters, int maxSize) {
		if (pois != null) {
			CollectionUtils.sort(pois, POI_DISTANCE_COMPARATOR); // sort required
			int nbKeptInList = 0;
			ListIterator<? extends LocationPOI> it = pois.listIterator();
			while (it.hasNext()) {
				LocationPOI poi = it.next();
				if (poi.getDistance() > minCoverageInMeters && nbKeptInList >= maxSize) {
					it.remove();
				} else {
					nbKeptInList++;
				}
			}
		}
	}

	public static boolean searchComplete(double lat, double lng, double aroundDiff) {
		final Area area = getArea(lat, lng, aroundDiff);
		return searchComplete(area);
	}

	public static boolean searchComplete(@NonNull Area area) {
		if (area.minLat > MIN_LAT) {
			return false; // more places to explore in the south
		}
		if (area.maxLat < MAX_LAT) {
			return false; // more places to explore in the north
		}
		if (area.minLng > MIN_LNG) {
			return false; // more places to explore to the west
		}
		//noinspection RedundantIfStatement
		if (area.maxLng < MAX_LNG) {
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
		return isInside(lat, lng, area.minLat, area.maxLat, area.minLng, area.maxLng);
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
		if (area1.minLat >= area2.minLat && area1.maxLat <= area2.maxLat) {
			if (area2.minLng >= area1.minLng && area2.maxLng <= area1.maxLng) {
				return true; // area 1 wider than area 2 but area 2 higher than area 1
			}
		}
		if (area2.minLat >= area1.minLat && area2.maxLat <= area1.maxLat) {
			//noinspection RedundantIfStatement
			if (area1.minLng >= area2.minLng && area1.maxLng <= area2.maxLng) {
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

	// latitude = south <> north = horizontal lines
	// longitude = west <> east = vertical lines
	public static class Area {
		public double minLat;
		public double maxLat;
		public double minLng;
		public double maxLng;

		public Area(double minLat, double maxLat, double minLng, double maxLng) {
			this.minLat = minLat;
			this.maxLat = maxLat;
			this.minLng = minLng;
			this.maxLng = maxLng;
		}

		public double getNorthLat() {
			return this.maxLat;
		}

		public double getSouthLat() {
			return this.minLat;
		}

		public double getEastLng() {
			// FIXME not always: -180...0...+180 (In Pacific Ocean, E of NZ...)
			return this.maxLng;
		}

		public double getWestLng() {
			// FIXME not always: -180...0...+180 (In Pacific Ocean, E of NZ...)
			return this.minLng;
		}

		public double getCenterLat() {
			return this.minLat + (Math.abs(this.minLat - this.maxLat) / 2.0);
		}

		public double getCenterLng() {
			return this.minLng + (Math.abs(this.minLng - this.maxLng) / 2.0);
		}

		@NonNull
		public String getCenter() {
			return getCenterLat() + ", " + getCenterLng();
		}

		@NonNull
		@Override
		public String toString() {
			return Area.class.getSimpleName() + "{" +
					"minLat=" + minLat +
					", maxLat=" + maxLat +
					", minLng=" + minLng +
					", maxLng=" + maxLng +
					'}';
		}

		public boolean isEntirelyInside(@Nullable Area otherArea) {
			if (otherArea == null) {
				return false;
			}
			if (!isInside(this.minLat, this.minLng, otherArea)) {
				return false; // min lat, min lng
			}
			if (!isInside(this.minLat, this.maxLng, otherArea)) {
				return false; // min lat, max lng
			}
			if (!isInside(this.maxLat, this.minLng, otherArea)) {
				return false; // max lat, min lng
			}
			//noinspection RedundantIfStatement
			if (!isInside(this.maxLat, this.maxLng, otherArea)) {
				return false; // max lat, max lng
			}
			return true;
		}

		public static boolean areOverlapping(@Nullable Area area1, @Nullable Area area2) {
			if (area1 == null || area2 == null) {
				return false; // no data to compare
			}
			// AREA1 (at least partially) INSIDE AREA2
			if (isInside(area1.minLat, area1.minLng, area2)) {
				return true; // min lat, min lng
			}
			if (isInside(area1.minLat, area1.maxLng, area2)) {
				return true; // min lat, max lng
			}
			if (isInside(area1.maxLat, area1.minLng, area2)) {
				return true; // max lat, min lng
			}
			if (isInside(area1.maxLat, area1.maxLng, area2)) {
				return true; // max lat, max lng
			}
			// AREA2 (at least partially) INSIDE AREA1
			if (isInside(area2.minLat, area2.minLng, area1)) {
				return true; // min lat, min lng
			}
			if (isInside(area2.minLat, area2.maxLng, area1)) {
				return true; // min lat, max lng
			}
			if (isInside(area2.maxLat, area2.minLng, area1)) {
				return true; // max lat, min lng
			}
			if (isInside(area2.maxLat, area2.maxLng, area1)) {
				return true; // max lat, max lng
			}
			// OVERLAPPING
			return areCompletelyOverlapping(area1, area2);
		}

		@Nullable
		public static Area fromCursor(@Nullable Cursor cursor) {
			if (cursor == null) {
				return null;
			}
			try {
				return fromCursorNN(cursor);
			} catch (Exception e) {
				MTLog.w(TAG, e, "Error while reading cursor!");
				return null;
			}
		}

		@NonNull
		public static Area fromCursorNN(@NonNull Cursor cursor) throws IllegalArgumentException {
			double minLat = cursor.getDouble(cursor.getColumnIndexOrThrow(AgencyProviderContract.AREA_MIN_LAT));
			double maxLat = cursor.getDouble(cursor.getColumnIndexOrThrow(AgencyProviderContract.AREA_MAX_LAT));
			double minLng = cursor.getDouble(cursor.getColumnIndexOrThrow(AgencyProviderContract.AREA_MIN_LNG));
			double maxLng = cursor.getDouble(cursor.getColumnIndexOrThrow(AgencyProviderContract.AREA_MAX_LNG));
			return new Area(minLat, maxLat, minLng, maxLng);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Area area = (Area) o;

			if (Double.compare(area.minLat, minLat) != 0) return false;
			if (Double.compare(area.maxLat, maxLat) != 0) return false;
			if (Double.compare(area.minLng, minLng) != 0) return false;
			return Double.compare(area.maxLng, maxLng) == 0;
		}

		@Override
		public int hashCode() {
			int result;
			long temp;
			temp = Double.doubleToLongBits(minLat);
			result = (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(maxLat);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(minLng);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(maxLng);
			result = 31 * result + (int) (temp ^ (temp >>> 32));
			return result;
		}
	}

	public static final POIDistanceComparator POI_DISTANCE_COMPARATOR = new POIDistanceComparator();

	public static class POIDistanceComparator implements Comparator<LocationPOI> {
		@Override
		public int compare(@NonNull LocationPOI lhs, @NonNull LocationPOI rhs) {
			if (lhs instanceof RouteTripStop && rhs instanceof RouteTripStop) {
				RouteTripStop alhs = (RouteTripStop) lhs;
				RouteTripStop arhs = (RouteTripStop) rhs;
				if (alhs.getStop().getId() == arhs.getStop().getId()) { // SAME STOP = SAME LOCATION
					if (Route.SHORT_NAME_COMPARATOR.areDifferent(alhs.getRoute(), arhs.getRoute())) {
						if (Route.SHORT_NAME_COMPARATOR.areComparable(alhs.getRoute(), arhs.getRoute())) {
							return Route.SHORT_NAME_COMPARATOR.compare(alhs.getRoute(), arhs.getRoute());
						}
					}
					if (Trip.HEAD_SIGN_COMPARATOR.areDifferent(alhs.getTrip(), arhs.getTrip())) {
						if (Trip.HEAD_SIGN_COMPARATOR.areComparable(alhs.getTrip(), arhs.getTrip())) {
							return Trip.HEAD_SIGN_COMPARATOR.compare(alhs.getTrip(), arhs.getTrip());
						}
					}
				}
			}
			return Float.compare(lhs.getDistance(), rhs.getDistance());
		}
	}

	public interface LocationPOI {

		double getLat();

		double getLng();

		boolean hasLocation();

		void setDistanceString(@Nullable CharSequence distanceString);

		@Nullable
		CharSequence getDistanceString();

		void setDistance(float distance);

		float getDistance();
	}
}
