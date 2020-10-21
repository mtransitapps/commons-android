package org.mtransit.android.commons.provider;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.commons.data.RouteTripStop;
import org.mtransit.android.commons.data.Stop;
import org.mtransit.android.commons.data.Trip;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class OneBusAwayProviderTests {

	private static final Route DEFAULT_ROUTE = new Route(
			1,
			"1",
			"route 1",
			"blue"
	);

	private static final Stop DEFAULT_STOP = new Stop(1, "1", "stop 1", 0, 0);

	@Test
	public void testIsSameRoute() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "50";
		long routeId = 50L;
		long tripId = -1L;
		String jRouteId = "YRT_50";
		String jRouteShortName = "50";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteShortName);
		assertTrue(result);
	}

	@Test
	public void testIsSameRouteFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "50";
		long routeId = 50L;
		long tripId = -1L;
		String jRouteId = "YRT_96";
		String jRouteShortName = "96";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteShortName);
		assertFalse(result);
	}

	@Test
	public void testIsSameRouteDoubleRouteShortName() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "98|99";
		long routeId = 9899L;
		long tripId = -1L;
		String jRouteId = "YRT_9899";
		String jRouteShortName = "98|99";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteShortName);
		assertTrue(result);
	}

	@Test
	public void testIsSameRouteDoubleRouteShortNameFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "98|99";
		long routeId = 9899L;
		long tripId = -1L;
		String jRouteId = "YRT_99";
		String jRouteShortName = "99";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteShortName);
		assertFalse(result);
	}

	@Test
	public void testIsSameRouteEndsWithAZRouteShortName() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "4A";
		long routeId = 10004L;
		long tripId = -1L;
		String jRouteId = "YRT_4001";
		String jRouteShortName = "4A";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteShortName);
		assertTrue(result);
	}

	@Test
	public void testIsSameRouteEndsWithAZRouteShortNameFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "4A";
		long routeId = 10004L;
		long tripId = -1L;
		String jRouteId = "YRT_4";
		String jRouteShortName = "4";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteShortName);
		assertFalse(result);
	}

	@Test
	public void testIsSameRouteAZRouteShortName() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "Blue-A";
		long routeId = 602L;
		long tripId = -1L;
		String jRouteId = "YRT_602";
		String jRouteShortName = "blue A";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteShortName);
		assertTrue(result);
	}

	@Test
	public void testIsSameRouteAZRouteShortNameFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String routeShortName = "Blue-A";
		long routeId = 602L;
		long tripId = -1L;
		String jRouteId = "YRT_601";
		String jRouteShortName = "blue";
		RouteTripStop rts = getRouteTripStop(routeShortName, routeId, tripId);

		boolean result = provider.isSameRoute(rts, jRouteId, jRouteShortName);
		assertFalse(result);
	}

	@Test
	public void testIsSameTripSplittedTrip() {
		OneBusAwayProvider provider = new OneBusAwayProvider();
		Route route = new Route(
				21L,
				"21",
				"route 21",
				"blue"
		);
		Trip trip = new Trip(
				2103000L,
				Trip.HEADSIGN_TYPE_STRING,
				"Vaughan Mills Terminal",
				route.getId()
		);
		RouteTripStop rts = getRouteTripStop(route, trip, DEFAULT_STOP, false);
		String jTripHeadsign = "21 Vellore Local - AF";

		boolean result = provider.isSameTrip(rts, jTripHeadsign);
		assertTrue(result);
	}

	@Test
	public void testIsSameTripSplittedTripFalse() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		Route route = new Route(
				21L,
				"21",
				"route 21",
				"blue"
		);
		Trip trip = new Trip(
				2100L,
				Trip.HEADSIGN_TYPE_STRING,
				"Vaughan Mills Terminal",
				route.getId()
		);
		RouteTripStop rts = getRouteTripStop(route, trip, DEFAULT_STOP, false);
		String jTripHeadsign = "21 Vellore Local - AF";

		boolean result = provider.isSameTrip(rts, jTripHeadsign);
		assertFalse(result);
	}

	@Test
	public void testCleanTripHeadsignRTS() {
		OneBusAwayProvider provider = new OneBusAwayProvider();

		String tripHeadsign = "Martin Grv Via Vaughan Metropolitan Ctr";
		Trip trip = new Trip(
				-1,
				Trip.HEADSIGN_TYPE_STRING,
				"Martin Grv",
				DEFAULT_ROUTE.getId()
		);
		RouteTripStop rts = getRouteTripStop(DEFAULT_ROUTE, trip, DEFAULT_STOP, false);

		String result = provider.cleanTripHeadsign(tripHeadsign, rts);

		assertEquals("Via Vaughan Metropolitan Ctr", result);
	}

	@NonNull
	private RouteTripStop getRouteTripStop(String routeShortName, long routeId, long tripId) {
		Route route = new Route(
				routeId,
				routeShortName,
				"route " + routeShortName,
				"color"
		);
		Trip trip = new Trip(
				tripId,
				Trip.HEADSIGN_TYPE_STRING,
				"trip " + tripId,
				route.getId()
		);
		boolean descentOnly = false;
		//noinspection ConstantConditions
		return getRouteTripStop(route, trip, DEFAULT_STOP, descentOnly);
	}

	@NonNull
	private RouteTripStop getRouteTripStop(Route route, Trip trip, Stop stop, boolean descentOnly) {
		return new RouteTripStop(
				"authority.test",
				POI.ITEM_VIEW_TYPE_ROUTE_TRIP_STOP,
				route,
				trip,
				stop,
				descentOnly);
	}
}