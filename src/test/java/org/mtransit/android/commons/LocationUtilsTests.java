package org.mtransit.android.commons;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mtransit.android.commons.LocationUtils.EARTH_RADIUS;

// inspired by https://github.com/googlemaps/android-maps-utils
public class LocationUtilsTests {

	private static final int latIdx = 0;
	private static final int lngIdx = 0;

	// The vertices of an octahedron, for testing
	private final double[] up = new double[] { 90, 0 };
	private final double[] down = new double[] { -90, 0 };
	private final double[] front = new double[] { 0, 0 };
	private final double[] right = new double[] { 0, 90 };
	private final double[] back = new double[] { 0, -180 };
	private final double[] left = new double[] { 0, -90 };

	/**
	 * Tests for approximate equality.
	 */
	private static void expectLatLngApproxEquals(double[] actual, double[] expected) {
		expectNearNumber(actual[latIdx], expected[latIdx], 1e-6);
		// Account for the convergence of longitude lines at the poles
		double cosLat = Math.cos(Math.toRadians(actual[latIdx]));
		expectNearNumber(cosLat * actual[lngIdx], cosLat * expected[lngIdx], 1e-6);
	}

	private static void expectNearNumber(double actual, double expected, double epsilon) {
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
}
