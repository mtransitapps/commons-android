package org.mtransit.android.commons;

import org.junit.Test;
import org.mtransit.android.commons.LocationUtils.LocationPOI;
import org.mtransit.android.commons.data.DefaultPOI;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.android.commons.data.Trip;
import org.mtransit.commons.CollectionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mtransit.android.commons.LocationUtils.EARTH_RADIUS;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

// inspired by https://github.com/googlemaps/android-maps-utils
public class LocationUtilsTests {

	private static final int latIdx = 0;
	private static final int lngIdx = 0;

	// The vertices of an octahedron, for testing
	private final double[] up = new double[]{90, 0};
	private final double[] down = new double[]{-90, 0};
	private final double[] front = new double[]{0, 0};
	private final double[] right = new double[]{0, 90};
	private final double[] back = new double[]{0, -180};
	private final double[] left = new double[]{0, -90};

	/**
	 * Tests for approximate equality.
	 */
	private static void expectLatLngApproxEquals(double[] actual, double[] expected) {
		expectNearNumber(actual[latIdx], expected[latIdx], 1e-6);
		// Account for the convergence of longitude lines at the poles
		double cosLat = Math.cos(Math.toRadians(actual[latIdx]));
		expectNearNumber(cosLat * actual[lngIdx], cosLat * expected[lngIdx], 1e-6);
	}

	private static void expectNearNumber(double actual, double expected, @SuppressWarnings("SameParameterValue") double epsilon) {
		assertTrue(String.format("Expected %g to be near %g", actual, expected),
				Math.abs(expected - actual) <= epsilon);
	}

	@Test
	public void testComputeOffset() {
		// From front
		expectLatLngApproxEquals(
				front, LocationUtils.computeOffset(front[0], front[1], 0, 0));
		expectLatLngApproxEquals(
				up, LocationUtils.computeOffset(front[0], front[1], Math.PI * EARTH_RADIUS / 2, 0));
		expectLatLngApproxEquals(
				down, LocationUtils.computeOffset(front[0], front[1], Math.PI * EARTH_RADIUS / 2, 180));
		expectLatLngApproxEquals(
				left, LocationUtils.computeOffset(front[0], front[1], Math.PI * EARTH_RADIUS / 2, -90));
		expectLatLngApproxEquals(
				right, LocationUtils.computeOffset(front[0], front[1], Math.PI * EARTH_RADIUS / 2, 90));
		expectLatLngApproxEquals(
				back, LocationUtils.computeOffset(front[0], front[1], Math.PI * EARTH_RADIUS, 0));
		expectLatLngApproxEquals(
				back, LocationUtils.computeOffset(front[0], front[1], Math.PI * EARTH_RADIUS, 90));
		// From left
		expectLatLngApproxEquals(
				left, LocationUtils.computeOffset(left[0], left[1], 0, 0));
		expectLatLngApproxEquals(
				up, LocationUtils.computeOffset(left[0], left[1], Math.PI * EARTH_RADIUS / 2, 0));
		expectLatLngApproxEquals(
				down, LocationUtils.computeOffset(left[0], left[1], Math.PI * EARTH_RADIUS / 2, 180));
		expectLatLngApproxEquals(
				front, LocationUtils.computeOffset(left[0], left[1], Math.PI * EARTH_RADIUS / 2, 90));
		expectLatLngApproxEquals(
				back, LocationUtils.computeOffset(left[0], left[1], Math.PI * EARTH_RADIUS / 2, -90));
		expectLatLngApproxEquals(
				right, LocationUtils.computeOffset(left[0], left[1], Math.PI * EARTH_RADIUS, 0));
		expectLatLngApproxEquals(
				right, LocationUtils.computeOffset(left[0], left[1], Math.PI * EARTH_RADIUS, 90));
		// NOTE(appleton): Heading is undefined at the poles, so we do not test from up/down.
	}

	@Test
	public void testPOIDistanceComparator() {
		// Arrange
		List<LocationPOI> poiList = Arrays.asList(
				makeLocationPOI(1, 3f),
				makeLocationPOI(2, 1f),
				makeLocationPOI(3, 20_000f),
				makeLocationPOI(4, 0.5f)
		);
		System.out.println(poiList);
		// Act
		CollectionUtils.sort(poiList, LocationUtils.POI_DISTANCE_COMPARATOR);
		// Assert
		System.out.println(poiList);
		assertEquals(4, poiList.size());
		assertEquals("tag4", poiList.get(0).getDistanceString());
		assertEquals("tag2", poiList.get(1).getDistanceString());
		assertEquals("tag1", poiList.get(2).getDistanceString());
		assertEquals("tag3", poiList.get(3).getDistanceString());
	}

	@Test
	public void testPOIDistanceComparatorRTS_SameStop_DistinctRoute() {
		// Arrange
		List<LocationPOI> poiList = Arrays.asList(
				makeLocationPOI(1, 3f),
				makeLocationPOI(2, 1f, 2L, 0L, 100),
				makeLocationPOI(3, 1f, 1L, 0L, 100),
				makeLocationPOI(4, 0.5f)
		);
		System.out.println(poiList);
		// Act
		CollectionUtils.sort(poiList, LocationUtils.POI_DISTANCE_COMPARATOR);
		// Assert
		System.out.println(poiList);
		assertEquals(4, poiList.size());
		assertEquals("tag4", poiList.get(0).getDistanceString());
		assertEquals("tag3", poiList.get(1).getDistanceString());
		assertEquals("tag2", poiList.get(2).getDistanceString());
		assertEquals("tag1", poiList.get(3).getDistanceString());
	}

	@Test
	public void testPOIDistanceComparatorRTS_SameStop_DistinctTrip() {
		// Arrange
		List<LocationPOI> poiList = Arrays.asList(
				makeLocationPOI(1, 3f),
				makeLocationPOI(2, 1f, 1L, 1L, 100),
				makeLocationPOI(3, 1f, 1L, 0L, 100),
				makeLocationPOI(4, 0.5f)
		);
		System.out.println(poiList);
		// Act
		CollectionUtils.sort(poiList, LocationUtils.POI_DISTANCE_COMPARATOR);
		// Assert
		System.out.println(poiList);
		assertEquals(4, poiList.size());
		assertEquals("tag4", poiList.get(0).getDistanceString());
		assertEquals("tag3", poiList.get(1).getDistanceString());
		assertEquals("tag2", poiList.get(2).getDistanceString());
		assertEquals("tag1", poiList.get(3).getDistanceString());
	}

	@NonNull
	private LocationPOI makeLocationPOI(int intTag, float distance) {
		return makeLocationPOI(intTag, distance, null, null, null);
	}

	@NonNull
	private LocationPOI makeLocationPOI(int intTag, float distance,
										@Nullable Long rtsRouteTag, @Nullable Long rtsTripTag, @Nullable Integer rtsStopTag) {
		//noinspection ConstantConditions
		return new LocationPOI() {

			@NonNull
			@Override
			public POI getPOI() {
				if (rtsRouteTag != null && rtsTripTag != null && rtsStopTag != null) {
					return new RouteTripStop(
							"authority" + intTag,
							1,
							new Route(rtsRouteTag, "R" + rtsRouteTag, "Route " + rtsRouteTag, "000000", rtsRouteTag.hashCode()),
							new Trip(rtsTripTag, Trip.HEADSIGN_TYPE_NONE, "head-sign " + rtsTripTag, rtsRouteTag),
							new Stop(rtsStopTag, String.valueOf(rtsStopTag), "Stop #" + rtsStopTag, 0.0d, 0.0d, 0, rtsStopTag.hashCode()),
							false
					);
				}
				return new DefaultPOI("authority" + intTag, 1, POI.ITEM_VIEW_TYPE_BASIC_POI, POI.ITEM_STATUS_TYPE_NONE, POI.ITEM_ACTION_TYPE_NONE);
			}

			@Override
			public double getLat() {
				return 0d;
			}

			@Override
			public double getLng() {
				return 0d;
			}

			@Override
			public boolean hasLocation() {
				return true;
			}

			@NonNull
			@Override
			public LocationPOI setDistanceString(@Nullable CharSequence distanceString) {
				throw new RuntimeException("This object is not compatible with distance string");
			}

			@Nullable
			@Override
			public CharSequence getDistanceString() {
				return "tag" + intTag;
			}

			@NonNull
			@Override
			public LocationPOI setDistance(float distance) {
				throw new RuntimeException("This object is not compatible with distance string");
			}

			@Override
			public float getDistance() {
				return distance;
			}

			@NonNull
			@Override
			public String toString() {
				return "POI{" +
						"tag=" + intTag +
						", distance=" + distance +
						'}';
			}
		};
	}
}
